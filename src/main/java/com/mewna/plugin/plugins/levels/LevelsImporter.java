package com.mewna.plugin.plugins.levels;

import com.mewna.Mewna;
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
    
    // tfw the limit is supposed to be 1000 but it errors if you give 1000
    // feels bad
    // We solve this by specifying 999 as the limit~
    private static final String MEE6_IMPORTER = "https://mee6.xyz/api/plugins/levels/leaderboard/$guild?page=$page&limit=999";
    
    private LevelsImporter() {
    }
    
    public static void importMEE6Levels(final String guild) {
        new Thread(() -> {
            final OkHttpClient client = new OkHttpClient();
            Thread.currentThread().setName(guild);
            final List<JsonObject> pages = new ArrayList<>();
            boolean exception = false;
            int currentPage = 0;
            while(true) {
                JsonObject json = null;
                try {
                    // Don't care about the possible npe b/c we catch it anyway
                    // TODO: Replace this w/ JDK HTTP client...
                    @SuppressWarnings({"UnnecessarilyQualifiedInnerClassAccess", "ConstantConditions"})
                    final String res = client.newCall(new Request.Builder()
                            .url(MEE6_IMPORTER.replace("$guild", guild).replace("$page", currentPage + ""))
                            .get()
                            .build()).execute().body().string();
                    json = new JsonObject(res);
                    pages.add(json);
                    if(json.containsKey("error")) {
                        if(json.getInteger("status_code", 200) == 404) {
                            // Guild not found, just skip
                            return;
                        } else {
                            throw new IllegalStateException("Unknown MEE6 levels JSON error!");
                        }
                    } else if(json.getJsonArray("players").isEmpty()) {
                        // Ran out of pages
                        break;
                    } else {
                        // Be nice to their API
                        Thread.sleep(100L);
                        ++currentPage;
                    }
                } catch(final Exception e) {
                    Sentry.capture(new RuntimeException(json != null ? json.encode() : "<no json available>", e));
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
                    // Actually do the import~
                    pages.forEach(page -> {
                        // Convert players
                        page.getJsonArray("players").stream()
                                .map(e -> ((JsonObject) e).mapTo(MEE6Player.class))
                                .forEach(player -> {
                                    final var queuedPlayer = new JsonObject()
                                            .put("id", player.getId())
                                            .put("username", player.getUsername())
                                            .put("avatar", player.getAvatar())
                                            .put("guild", player.getGuildId())
                                            .put("xp", player.getXp())
                                            ;
                                    Mewna.getInstance().levelsImportQueue().queue(queuedPlayer);
                                });
                    });
                    LOGGER.info("Finished importing MEE6 levels for guild {}.", guild);
                } else {
                    LOGGER.warn("No pages for MEE6 levels for guild {}!", guild);
                }
            } else {
                LOGGER.error("Couldn't finish MEE6 levels import for guild {} (Check Sentry)", guild);
            }
        }).start();
    }
}
