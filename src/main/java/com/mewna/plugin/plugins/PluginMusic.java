package com.mewna.plugin.plugins;

import com.mewna.catnip.entity.builder.EmbedBuilder;
import com.mewna.catnip.entity.guild.Guild;
import com.mewna.catnip.entity.user.User;
import com.mewna.data.DiscordCache;
import com.mewna.plugin.BasePlugin;
import com.mewna.plugin.Command;
import com.mewna.plugin.CommandContext;
import com.mewna.plugin.Plugin;
import com.mewna.plugin.event.Event;
import com.mewna.plugin.event.EventType;
import com.mewna.plugin.event.audio.NekoTrackEvent;
import com.mewna.plugin.plugins.music.NekoTrackContext;
import com.mewna.plugin.plugins.settings.MusicSettings;
import com.mewna.plugin.util.Emotes;
import com.mewna.util.Time;
import gg.amy.singyeong.QueryBuilder;
import io.sentry.Sentry;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import me.escoffier.vertx.completablefuture.VertxCompletableFuture;

import java.util.concurrent.CompletionStage;

/**
 * @author amy
 * @since 5/19/18.
 */
@Plugin(name = "Music", desc = "Control the way music is played in your server.", settings = MusicSettings.class)
public class PluginMusic extends BasePlugin {
    // TODO: Fill out shit with localization
    
    @Command(names = "join", desc = "Bring Mewna into a voice channel with you.", usage = "join", examples = "join")
    public void join(final CommandContext ctx) {
        final Guild guild = ctx.getGuild();
        final String guildId = guild.id();
        checkState(guild, ctx.getUser()).thenAccept(check -> {
            if(check == VoiceCheck.USER_NOT_IN_VOICE) {
                catnip().rest().channel().sendMessage(ctx.getMessage().channelId(), "You're not in a voice channel!");
            } else if(check == VoiceCheck.USER_IN_DIFFERENT_VOICE || check == VoiceCheck.SELF_AND_USER_IN_SAME_VOICE) {
                catnip().rest().channel().sendMessage(ctx.getMessage().channelId(), "I'm already in a voice channel!");
            } else {
                DiscordCache.voiceState(guildId, ctx.getUser().id())
                        .thenAccept(state -> {
                            mewna().singyeong().send("mewna-shard",
                                    new QueryBuilder().contains("guilds", guildId).build(),
                                    new JsonObject().put("type", "VOICE_JOIN")
                                            .put("guild_id", guildId)
                                            .put("channel_id", state.channelId()));
                            DiscordCache.voiceChannel(guildId, state.channelId())
                                    .thenAccept(ch -> catnip().rest().channel()
                                            .sendMessage(ctx.getMessage().channelId(),
                                                    "Joined \uD83D\uDD0A" + ch.name()));
                        });
            }
        });
    }
    
    @Command(names = "leave", desc = "Make Mewna leave the voice channel she's in.", usage = "leave", examples = "leave")
    public void leave(final CommandContext ctx) {
        final Guild guild = ctx.getGuild();
        final String guildId = guild.id();
        checkState(guild, ctx.getUser()).thenAccept(check -> {
            if(check == VoiceCheck.USER_NOT_IN_VOICE) {
                catnip().rest().channel().sendMessage(ctx.getMessage().channelId(), "You're not in a voice channel!");
            } else if(check == VoiceCheck.USER_IN_DIFFERENT_VOICE) {
                catnip().rest().channel().sendMessage(ctx.getMessage().channelId(), "You're not in this voice channel!");
            } else if(check == VoiceCheck.SELF_NOT_IN_VOICE) {
                catnip().rest().channel().sendMessage(ctx.getMessage().channelId(), "I'm not in a voice channel!");
            } else {
                DiscordCache.voiceState(guildId, ctx.getUser().id())
                        .thenAccept(state -> {
                            mewna().singyeong().send("mewna-shard",
                                    new QueryBuilder().contains("guilds", guildId).build(),
                                    new JsonObject().put("type", "VOICE_JOIN")
                                            .put("guild_id", guildId)
                                            .put("channel_id", state.channelId()));
                            DiscordCache.voiceChannel(guildId, state.channelId())
                                    .thenAccept(ch -> {
                                        catnip().rest().channel()
                                                .sendMessage(ctx.getMessage().channelId(),
                                                        "Left \uD83D\uDD0A" + ch.name());
                                        mewna().singyeong().send("mewna-shard", new QueryBuilder().contains("guilds", guildId).build(),
                                                new JsonObject().put("type", "VOICE_LEAVE").put("guild_id", guildId));
                                    });
                        });
            }
        });
    }
    
    @Command(names = {"queue", "q"}, desc = "", usage = "", examples = "")
    public void queue(final CommandContext ctx) {
        final NekoTrackContext context = new NekoTrackContext(
                ctx.getUser().id(),
                ctx.getMessage().channelId(),
                ctx.getMessage().guildId()
        );
        mewna().singyeong().send("nekomimi", new QueryBuilder().contains("guilds", ctx.getGuild().id()).build(),
                new JsonObject().put("type", "VOICE_QUEUE")
                        .put("url", ctx.getArgstr())
                        .put("context", JsonObject.mapFrom(context)));
    }
    
    @Command(names = {"play", "p"}, desc = "", usage = "", examples = "")
    public void play(final CommandContext ctx) {
        mewna().singyeong().send("nekomimi", new QueryBuilder().contains("guilds", ctx.getGuild().id()).build(),
                new JsonObject().put("type", "VOICE_PLAY")
                        .put("guild_id", ctx.getGuild().id()));
    }
    
    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Event(EventType.AUDIO_TRACK_QUEUE)
    public void handleTrackQueue(final NekoTrackEvent event) {
        final EmbedBuilder builder = new EmbedBuilder();
        builder.title(Emotes.YES + " Song queued")
                .field("Title", event.track().title(), true)
                .field("\u200B", "\u200B", true)
                .field("Artist", event.track().author(), true)
                .field("Length", Time.toHumanReadableDuration(event.track().length()), true);
        catnip().rest().channel().sendMessage(event.track().context().channel(), builder.build());
    }
    
    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Event(EventType.AUDIO_TRACK_START)
    public void handleTrackStart(final NekoTrackEvent event) {
        final EmbedBuilder builder = new EmbedBuilder();
        builder.title(Emotes.YES + " Song started")
                .field("Title", event.track().title(), true)
                .field("\u200B", "\u200B", true)
                .field("Artist", event.track().author(), true)
                .field("Length", Time.toHumanReadableDuration(event.track().length()), true);
        catnip().rest().channel().sendMessage(event.track().context().channel(), builder.build());
    }
    
    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Event(EventType.AUDIO_QUEUE_END)
    public void handleQueueEnd(final NekoTrackEvent event) {
        final EmbedBuilder builder = new EmbedBuilder();
        builder.title(Emotes.YES + " Queue ended");
        catnip().rest().channel().sendMessage(event.track().context().channel(), builder.build());
    }
    
    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Event(EventType.AUDIO_TRACK_NOW_PLAYING)
    public void handleNowPlaying(final NekoTrackEvent event) {
        final EmbedBuilder builder = new EmbedBuilder();
        builder.title(Emotes.YES + " Now playing")
                .field("Title", event.track().title(), true)
                .field("\u200B", "\u200B", true)
                .field("Artist", event.track().author(), true)
                .field("Length", Time.toHumanReadableDuration(event.track().length()), true);
        catnip().rest().channel().sendMessage(event.track().context().channel(), builder.build());
    }
    
    @SuppressWarnings("ConstantConditions")
    private CompletionStage<VoiceCheck> checkState(final Guild guild, final User user) {
        final Future<VoiceCheck> future = Future.future();
        
        final var selfStateFuture = DiscordCache.voiceState(guild.id(), System.getenv("CLIENT_ID")).exceptionally(e -> null);
        final var userStateFuture = DiscordCache.voiceState(guild.id(), user.id()).exceptionally(e -> null);
        
        selfStateFuture.thenAcceptBoth(userStateFuture, (selfState, userState) -> {
            if(userState == null || userState.channelId() == null) {
                future.complete(VoiceCheck.USER_NOT_IN_VOICE);
            } else if(selfState == null || selfState.channelId() == null) {
                future.complete(VoiceCheck.SELF_NOT_IN_VOICE);
            } else if(userState.channelId().equals(selfState.channelId())) {
                // This ^ Should:tm: be safe, but yeahhhh...
                future.complete(VoiceCheck.SELF_AND_USER_IN_SAME_VOICE);
            } else {
                future.complete(VoiceCheck.USER_IN_DIFFERENT_VOICE);
            }
        }).exceptionally(e -> {
            e.printStackTrace();
            Sentry.capture(e);
            return null;
        });
        
        return VertxCompletableFuture.from(mewna().vertx(), future);
    }
    
    private enum VoiceCheck {
        USER_NOT_IN_VOICE,
        SELF_NOT_IN_VOICE,
        USER_IN_DIFFERENT_VOICE,
        SELF_AND_USER_IN_SAME_VOICE,
    }
}