package com.mewna.plugin.plugins.levels;

import com.mewna.Mewna;
import com.mewna.data.Player;
import com.mewna.plugin.plugins.levels.mee6.MEE6Player;
import com.mewna.plugin.plugins.levels.mee6.MEE6RoleReward;
import com.mewna.plugin.plugins.settings.LevelsSettings;
import io.sentry.Sentry;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * @author amy
 * @since 1/28/19.
 */
@SuppressWarnings("unused")
public final class LevelsImporter {
    private static final Logger LOGGER = LoggerFactory.getLogger(LevelsImporter.class);
    
    private static final String MEE6_IMPORTER = "https://mee6.xyz/api/plugins/levels/leaderboard/$guild?page=$page&limit=999";
    
    private LevelsImporter() {
    }
    
    public static void importMEE6Levels(final String guild) {
        new Thread(() -> {
            final OkHttpClient client = new OkHttpClient();
            Thread.currentThread().setName("MEE6 importer " + guild);
            final List<JsonObject> pages = new ArrayList<>();
            boolean exception = false;
            int currentPage = 0;
            while(true) {
                try {
                    // Don't care about the possible npe b/c we catch it anyway
                    @SuppressWarnings({"UnnecessarilyQualifiedInnerClassAccess", "ConstantConditions"})
                    final String res = client.newCall(new Request.Builder()
                            .url(MEE6_IMPORTER.replace("$guild", guild).replace("$page", currentPage + ""))
                            .get()
                            .build()).execute().body().string();
                    final JsonObject json = new JsonObject(res);
                    pages.add(json);
                    if(json.getJsonArray("players").isEmpty()) {
                        // Ran out of pages
                        break;
                    } else {
                        // Be nice to their API
                        Thread.sleep(100L);
                        ++currentPage;
                    }
                } catch(Exception e) {
                    Sentry.capture(e);
                    e.printStackTrace();
                    exception = true;
                    break;
                }
            }
            if(!exception) {
                if(!pages.isEmpty()) {
                    LOGGER.info("Importing {} pages of MEE6 users for guild {}", pages.size(), guild);
                    // Import role rewards
                    final JsonObject firstPage = pages.get(0);
                    final JsonArray roleRewards = firstPage.getJsonArray("role_rewards");
                    Mewna.getInstance().database().getOrBaseSettings(LevelsSettings.class, guild)
                            .thenAccept(settings -> {
                                for(final Object r : roleRewards) {
                                    final JsonObject reward = (JsonObject) r;
                                    final MEE6RoleReward rr = reward.mapTo(MEE6RoleReward.class);
                                    settings.getLevelRoleRewards().put(rr.getRole().getId(), (long) rr.getRank());
                                }
                                Mewna.getInstance().database().saveSettings(settings);
                            });
                    // Import levels
                    // God this is gonna suck...
                    pages.forEach(page -> {
                        // Convert players
                        Mewna.getInstance().database().getStore().sql("BEGIN TRANSACTION;");
                        page.getJsonArray("players").forEach(o -> {
                            final MEE6Player player = ((JsonObject) o).mapTo(MEE6Player.class);
                            
                            Mewna.getInstance().database().getStore().sql("INSERT INTO players (id, data) VALUES (?, to_jsonb(?::jsonb)) " +
                                            "ON CONFLICT (id) DO UPDATE " +
                                            "SET data = jsonb_insert(players.data, '{guildXp, " + guild + "}', '" + player.getXp() + "');",
                                    c -> {
                                        c.setString(1, player.getId());
                                        final Player p = new Player();
                                        p.setId(player.getId());
                                        c.setString(2, JsonObject.mapFrom(p).encode());
                                        c.execute();
                                    });
                        });
                        Mewna.getInstance().database().getStore().sql("COMMIT;");
                        try {
                            Thread.sleep(1000L);
                        } catch(final InterruptedException e) {
                            Sentry.capture(e);
                        }
                    });
                    LOGGER.info("Finished importing MEE6 levels for guild {}.", guild);
                } else {
                    LOGGER.warn("No pages for MEE6 levels for guild {}!", guild);
                }
            } else {
                LOGGER.warn("Couldn't finish MEE6 levels import for guild {} (Check Sentry)", guild);
            }
        }).start();
    }
}
