package com.mewna.plugin.plugins.levels;

import com.mewna.Mewna;
import com.mewna.data.player.Player;
import io.sentry.Sentry;
import io.vertx.core.json.JsonObject;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import static com.mewna.util.MewnaFutures.block;

/**
 * Yeah yeah don't extend Thread blah blah etc etc etc
 * <br/>
 * This was the easiest way to do it, okay? Stop getting angery ;w;
 *
 * @author amy
 * @since 6/3/19.
 */
@SuppressWarnings("WeakerAccess")
@RequiredArgsConstructor
public class LevelsImportQueue extends Thread {
    public static final String IMPORT_QUEUE = "mewna:discord:import:levels:queue";
    
    private final Mewna mewna;
    
    @Getter
    @Setter
    private boolean run = true;
    
    public void queue(final JsonObject importData) {
        mewna.database().redis(r -> r.rpush(IMPORT_QUEUE, importData.encode()));
    }
    
    @Override
    public void run() {
        while(run) {
            // Grab next thing in the queue, decode it, and apply it to the
            // corresponding player object.
            // For a queue of size N, this will issue 2N queries at worst -- it's
            // more likely that it'll just heavily abuse Redis, unless it's a case
            // where the majority of users aren't able to be cached for some
            // reason -- think VERY large guilds, that kind of use-case.
            mewna.database().redis(r -> {
                final var popped = r.blpop(0, IMPORT_QUEUE);
                // [0] is the key it was popped from -- IMPORT_QUEUE
                // [1] is the actual value that was popped
                final var value = new JsonObject(popped.get(1));
                /*
                .put("id", player.getId())
                .put("username", player.getUsername())
                .put("avatar", player.getAvatar())
                .put("guild", player.getGuildId())
                .put("xp", player.getXp())
                */
    
                // Will also fill in the account, not just the player.
                final Player player = block(mewna.database().getPlayerForImport(
                        value.getString("id"),
                        value.getString("username"),
                        value.getString("avatar"))
                );
                player.setLocalXp(value.getString("guild"), Long.parseLong(value.getString("xp")));
                mewna.database().savePlayer(player);
                mewna.statsClient().gauge("levels.import-queue.length", r.llen(IMPORT_QUEUE));
            });
            try {
                // Don't just constantly hit up the DB
                Thread.sleep(5L);
            } catch(final InterruptedException e) {
                Sentry.capture(e);
            }
        }
    }
}
