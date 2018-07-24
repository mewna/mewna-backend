package com.mewna;

import com.google.common.collect.Lists;
import com.mewna.accounts.Account;
import com.mewna.data.Player;
import com.mewna.plugin.plugins.settings.LevelsSettings;
import org.json.JSONObject;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * @author amy
 * @since 7/23/18.
 */
@SuppressWarnings("unused")
final class ImportOldDB {
    private static final long timestampBits = 41L;
    private static final long datacenterIdBits = 5L;
    private static final long workerIdBits = 5L;
    private static final long sequenceBits = 12L;
    private static final long timestampShift = sequenceBits + datacenterIdBits + workerIdBits;
    private static final long datacenterIdShift = sequenceBits + workerIdBits;
    private static final long workerIdShift = sequenceBits;
    private static final long epoch = 1518566400000L;
    private static final AtomicLong waitCount = new AtomicLong(0);
    private static long increment;
    private static long lastTimestamp;
    
    private ImportOldDB() {
    }
    
    private static long getWaitCount() {
        return waitCount.get();
    }
    
    private static long waitNextMillis(long currTimestamp) {
        waitCount.incrementAndGet();
        while(currTimestamp <= lastTimestamp) {
            currTimestamp = System.currentTimeMillis();
        }
        return currTimestamp;
    }
    
    private static synchronized long nextSnowflake() {
        long currTimestamp = System.currentTimeMillis();
        
        if(currTimestamp == lastTimestamp) {
            final long maxSequence = 4096;
            increment = increment + 1 & maxSequence;
            if(increment == 0) { // overflow: greater than max sequence
                currTimestamp = waitNextMillis(currTimestamp);
            }
        } else { // reset to 0 for next period/millisecond
            increment = 0L;
        }
        
        // track and memo the time stamp last snowflake ID generated
        lastTimestamp = currTimestamp;
        
        final long workerId = 1;
        return currTimestamp - epoch << timestampShift | //
                workerId << datacenterIdShift | //
                workerId << workerIdShift | // new line for nice looking
                increment;
    }
    
    /*
discord-user-balances
donors
guild:%ID%:allow-chat-levels
guild:%ID%:allow-level-up-messages
guild:%ID%:chat-avatars
guild:%ID%:chat-levels
guild:%ID%:chat-names
guild:%ID%:owner
guild:%ID%:role-rewards
guild:%ID%:roles
guild:%ID%:text-channels
guild:%ID%:voice-channels
user:%ID%:cache
     */
    
    static void importDb(final Mewna mewna) {
        try {
            final long loadStart = System.currentTimeMillis();
            System.out.println("### Loading data...");
            final List<String> lines = Files.lines(Paths.get("dump.json")).collect(Collectors.toList());
            System.out.println("### Initial data loaded.");
            
            final List<JSONObject> userCache;
            {
                final long start = System.currentTimeMillis();
                userCache = lines.stream().filter(e -> e.matches("(.*)user:(\\d{16,22}):cache(.*)"))
                        .map(JSONObject::new).collect(Collectors.toList());
                final long end = System.currentTimeMillis();
                System.out.println("### User cache loaded (" + userCache.size() + " entities) in " + (end - start) + "ms.");
            }
            
            final JSONObject userBalances;
            {
                final long start = System.currentTimeMillis();
                //noinspection ConstantConditions
                userBalances = lines.stream().filter(e -> e.contains("discord-user-balances"))
                        .map(JSONObject::new).findFirst().get();
                final long end = System.currentTimeMillis();
                System.out.println("### User balance data loaded (" + userBalances.getJSONObject("value").keySet().size() + " entities) in " + (end - start) + "ms.");
            }
            
            final List<JSONObject> chatLevelsEnabled;
            {
                final long start = System.currentTimeMillis();
                chatLevelsEnabled = lines.stream().filter(e -> e.matches("(.*)guild:(\\d{16,22}):allow-chat-levels(.*)"))
                        .map(JSONObject::new).collect(Collectors.toList());
                final long end = System.currentTimeMillis();
                System.out.println("### Chat levels toggle data loaded (" + chatLevelsEnabled.size() + " entities) in " + (end - start) + "ms.");
            }
            
            final List<JSONObject> roleRewards;
            {
                final long start = System.currentTimeMillis();
                roleRewards = lines.stream().filter(e -> e.matches("(.*)guild:(\\d{16,22}):role-rewards(.*)"))
                        .map(JSONObject::new).collect(Collectors.toList());
                final long end = System.currentTimeMillis();
                System.out.println("### Chat levels role rewards data loaded (" + roleRewards.size() + " entities) in " + (end - start) + "ms.");
            }
            
            final List<JSONObject> chatLevels;
            {
                final long start = System.currentTimeMillis();
                chatLevels = lines.stream().filter(e -> e.matches("(.*)guild:(\\d{16,22}):chat-levels(.*)"))
                        .map(JSONObject::new).collect(Collectors.toList());
                final long end = System.currentTimeMillis();
                System.out.println("### Chat levels XP data loaded (" + chatLevels.size() + " entities) in " + (end - start) + "ms.");
            }
            
            final long loadEnd = System.currentTimeMillis();
            System.out.println("### Finished loading data (took " + (loadEnd - loadStart) + "ms).");
            
            System.out.println("### ");
            System.out.println("### ");
            System.out.println("### ");
            
            final long dataStart = System.currentTimeMillis();
            System.out.println("### Converting old cache and data...");
            final Map<String, Player> playerCache = new HashMap<>();
            {
                final long start = System.currentTimeMillis();
                System.out.println("### Converting user cache...");
                final int[] counter = {0};
                
                // structure:
                // -----------------
                // {
                //   "db": 0,
                //   "key": "user:128316294742147072:cache",
                //   "ttl": -1,
                //   "type": "hash",
                //   "value": {
                //     "name": "amy",
                //     "discrim": "0001",
                //     "avatarUrl": "https://cdn.discordapp.com/avatars/128316294742147072/a_4f7762b810e3b934fa4abec9918143d5.gif"
                //   },
                //   "size": 119
                // }
                Lists.partition(userCache, 10000).forEach(users -> {
                    {
                        final long astart = System.currentTimeMillis();
                        mewna.getDatabase().getStore().sql("BEGIN TRANSACTION;");
                        users.forEach(data -> {
                            final String key = data.getString("key");
                            final String id = key
                                    .replace("user:", "")
                                    .replace(":cache", "");
                            final JSONObject user = data.getJSONObject("value");
                            final String avatar = user.getString("avatarUrl");
                            final String name = user.getString("name");
                            
                            // Create an account with minimal data
                            final String snowflake = nextSnowflake() + ""; // Snowflakes.getNewSnowflake();
                            final Account account = new Account(snowflake);
                            
                            account.setDiscordAccountId(id);
                            account.setDisplayName(name);
                            account.setAvatar(avatar);
                            
                            mewna.getDatabase().saveAccount(account);
                            
                            /////////////////////////////////////
                            
                            final String avatarNoURL;
                            if(!user.getString("avatarUrl").startsWith("https://discordapp.com/assets/")) {
                                avatarNoURL = user.getString("avatarUrl")
                                        .replace("https://cdn.discordapp.com/avatars/" + id + '/', "")
                                        .replace(".png", "")
                                        .replace(".gif", "");
                            } else {
                                avatarNoURL = null;
                            }
                            final JSONObject cache = new JSONObject()
                                    .put("id", id)
                                    .put("bot", false)
                                    .put("discriminator", user.getString("discrim"))
                                    .put("username", user.getString("name"))
                                    .put("avatar", avatarNoURL);
                            mewna.getCache().cacheUser(cache);
                            final Player player = Player.base(id); //mewna.getDatabase().getPlayer(id);
                            player.setBalance(userBalances.optInt(id, 0));
                            //mewna.getDatabase().savePlayer(player);
                            playerCache.putIfAbsent(id, player);
                        });
                        mewna.getDatabase().getStore().sql("COMMIT;");
                        final long aend = System.currentTimeMillis();
                        System.out.println("### Processed 10000 more accounts and player-caches (took " + (aend - astart) + "ms).");
                        counter[0] += 10000;
                    }
                });
                final long end = System.currentTimeMillis();
                System.out.println("### User cache converted (" + counter[0] + " users converted) in " + (end - start) + "ms.");
            }
            {
                System.out.println("### Converting levels data...");
                final long start = System.currentTimeMillis();
                final int[] counter = {0};
                Lists.partition(chatLevelsEnabled, 500).forEach(list -> {
                    mewna.getDatabase().getStore().sql("BEGIN TRANSACTION;");
                    list.forEach(config -> {
                        // {
                        //   "db": 0,
                        //   "key": "guild:267500017260953601:allow-chat-levels",
                        //   "ttl": -1,
                        //   "type": "string",
                        //   "value": "true",
                        //   "size": 4
                        // }
                        final boolean value = Boolean.parseBoolean(config.getString("value"));
                        final String id = config.getString("key")
                                .replace("guild:", "")
                                .replace(":allow-chat-levels", "");
                        final LevelsSettings settings = mewna.getDatabase().getOrBaseSettings(LevelsSettings.class, id);
                        mewna.getDatabase().saveSettings(settings.toBuilder().levelsEnabled(value).build());
                        counter[0] = counter[0] + 1;
                    });
                    mewna.getDatabase().getStore().sql("COMMIT;");
                });
                roleRewards.forEach(config -> {
                    // {
                    //   "db": 0,
                    //   "key": "guild:449838893647527937:role-rewards",
                    //   "ttl": -1,
                    //   "type": "hash",
                    //   "value": {
                    //     "449840248264982529": "1",
                    //     "449840363935629322": "1",
                    //     "449840851137462274": "-1"
                    //   },
                    //   "size": 58
                    // }
                    final JSONObject value = config.getJSONObject("value");
                    final String id = config.getString("key")
                            .replace("guild:", "")
                            .replace(":role-rewards", "");
                    final LevelsSettings settings = mewna.getDatabase().getOrBaseSettings(LevelsSettings.class, id);
                    final Map<String, Long> rewards = settings.getLevelRoleRewards();
                    value.keySet().forEach(key -> {
                        final int level = Integer.parseInt(value.getString(key));
                        if(level > 0) {
                            rewards.put(key, (long) level);
                        }
                    });
                    mewna.getDatabase().saveSettings(settings.toBuilder().levelRoleRewards(rewards).build());
                    counter[0] = counter[0] + 1;
                });
                Lists.partition(chatLevels, 1000).forEach(list -> list.forEach(data -> {
                    // {
                    //   "db": 0,
                    //   "key": "guild:267500017260953601:chat-levels",
                    //   "ttl": -1,
                    //   "type": "hash",
                    //   "value": {
                    //     "455478116828053506:user-xp": "15",
                    //     "317323140164222977:user-xp": "102",
                    //     "218866717676273666:user-xp": "20",
                    //     "317345857538228235:user-xp": "37",
                    //     "277637311024594956:user-xp": "17",
                    //     "429625104104947713:user-xp": "37",
                    //     "365339663612772354:user-xp": "1811",
                    //     "192756779573051392:user-xp": "20",
                    //     "345000254963449857:user-xp": "33",
                    //     "214839179509628930:user-xp": "191",
                    //     "372437423164227586:user-xp": "15"
                    //   },
                    //   "size": 82188
                    // }
                    
                    final String guildId = data.getString("key")
                            .replace("guild:", "")
                            .replace(":chat-levels", "");
                    final JSONObject levels = data.getJSONObject("value");
                    levels.keySet().forEach(key -> {
                        final String playerId = key.replace(":user-xp", "");
                        try {
                            long totalXp;
                            try {
                                totalXp = Long.parseLong(levels.getString(key));
                                if(totalXp > 10_000_000) {
                                    throw new IllegalStateException();
                                }
                            } catch(final Exception e) {
                                totalXp = 0;
                            }
                            /*
                            final Player player = mewna.getDatabase().getPlayer(playerId);
                            player.incrementLocalXp(guildId, totalXp);
                            mewna.getDatabase().savePlayer(player);
                            */
                            final long finalTotalXp = totalXp;
                            Optional.ofNullable(playerCache.get(playerId))
                                    .ifPresent(player -> player.incrementLocalXp(guildId, finalTotalXp));
                        } catch(final Exception ignored) {
                            System.err.println("### Bad user, id:" + playerId);
                        }
                    });
                    counter[0] = counter[0] + 1;
                }));
                {
                    final int[] c = {0};
                    Lists.partition(new ArrayList<>(playerCache.values()), 10000).forEach(list -> {
                        mewna.getDatabase().getStore().sql("BEGIN TRANSACTION;");
                        list.forEach(p -> mewna.getDatabase().savePlayer(p));
                        mewna.getDatabase().getStore().sql("COMMIT;");
                        c[0] += 10000;
                        System.out.println("### Updated 10000 more players");
                    });
                    System.out.println("### Updated ~" + c[0] + " players.");
                }
                final long end = System.currentTimeMillis();
                System.out.println("### Levels data converted (" + counter[0] + " entities converted) in " + (end - start) + "ms.");
            }
            final long dataEnd = System.currentTimeMillis();
            System.out.println("### Finished converting data (took " + (dataEnd - dataStart) + "ms).");
        } catch(final IOException e) {
            throw new RuntimeException(e);
        }
    }
}
