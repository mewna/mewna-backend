package com.mewna.plugin.plugins;

import com.google.common.base.Strings;
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
import gg.amy.singyeong.SafeVertxCompletableFuture;
import io.sentry.Sentry;
import io.sentry.event.EventBuilder;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;
import java.util.concurrent.CompletionStage;

import static com.mewna.util.Translator.$;

/**
 * @author amy
 * @since 5/19/18.
 */
@Plugin(name = "Music", desc = "Control the way music is played in your server.", settings = MusicSettings.class)
public class PluginMusic extends BasePlugin {
    // TODO: Fill out shit with localization
    
    private static String getDomainName(final String url) throws URISyntaxException {
        try {
            final URI uri = new URI(url);
            return uri.getHost().replaceFirst("www\\.", "");
        } catch(final NullPointerException ignored) {
            throw new URISyntaxException("null", "invalid uri");
        }
    }
    
    @Command(names = "join", desc = "commands.music.join", usage = "join", examples = "join")
    public void join(final CommandContext ctx) {
        final Guild guild = ctx.getGuild();
        final String guildId = guild.id();
        checkState(guild, ctx.getUser()).thenAccept(check -> {
            if(check == VoiceCheck.USER_NOT_IN_VOICE) {
                ctx.sendMessage(
                        $(ctx.getLanguage(), "plugins.music.user-not-in-voice"));
            } else if(check == VoiceCheck.USER_IN_DIFFERENT_VOICE || check == VoiceCheck.SELF_AND_USER_IN_SAME_VOICE) {
                ctx.sendMessage(
                        $(ctx.getLanguage(), "plugins.music.bot-already-in-voice"));
            } else {
                DiscordCache.voiceState(guildId, ctx.getUser().id())
                        .thenAccept(state -> {
                            mewna().singyeong().send("shards",
                                    new QueryBuilder().contains("guilds", guildId).build(),
                                    new JsonObject().put("type", "VOICE_JOIN")
                                            .put("guild_id", guildId)
                                            .put("channel_id", state.channelId()));
                            DiscordCache.voiceChannel(guildId, state.channelId())
                                    .thenAccept(ch -> catnip().rest().channel()
                                            .sendMessage(ctx.getMessage().channelId(),
                                                    $(ctx.getLanguage(), "plugins.music.commands.join.joined")
                                                            + " \uD83D\uDD0A" + ch.name()));
                        });
            }
        });
    }
    
    @Command(names = "leave", desc = "commands.music.leave", usage = "leave", examples = "leave")
    public void leave(final CommandContext ctx) {
        final Guild guild = ctx.getGuild();
        final String guildId = guild.id();
        checkState(guild, ctx.getUser()).thenAccept(check -> {
            if(check == VoiceCheck.USER_NOT_IN_VOICE) {
                ctx.sendMessage(
                        $(ctx.getLanguage(), "plugins.music.user-not-in-voice"));
            } else if(check == VoiceCheck.USER_IN_DIFFERENT_VOICE) {
                ctx.sendMessage(
                        $(ctx.getLanguage(), "plugins.music.user-not-in-same-voice"));
            } else if(check == VoiceCheck.SELF_NOT_IN_VOICE) {
                ctx.sendMessage(
                        $(ctx.getLanguage(), "plugins.music.bot-not-in-voice"));
            } else {
                DiscordCache.voiceState(guildId, ctx.getUser().id())
                        .thenAccept(state ->
                                DiscordCache.voiceChannel(guildId, state.channelId())
                                        .thenAccept(ch -> {
                                            catnip().rest().channel()
                                                    .sendMessage(ctx.getMessage().channelId(),
                                                            $(ctx.getLanguage(), "plugins.music.commands.leave.left") +
                                                                    " \uD83D\uDD0A" + ch.name());
                                            mewna().singyeong().send("shards", new QueryBuilder().contains("guilds", guildId).build(),
                                                    new JsonObject().put("type", "VOICE_LEAVE").put("guild_id", guildId));
                                        }));
            }
        });
    }
    
    @Command(names = {"queue", "q"}, desc = "commands.music.queue",
            usage = {"queue <song name>", "queue <youtube url>"},
            examples = {"queue Rick Astley", "q https://www.youtube.com/watch?v=fUOVQ4KsX9U"})
    public void queue(final CommandContext ctx) {
        final Guild guild = ctx.getGuild();
        checkState(guild, ctx.getUser()).thenAccept(check -> {
            if(check == VoiceCheck.USER_NOT_IN_VOICE) {
                ctx.sendMessage(
                        $(ctx.getLanguage(), "plugins.music.user-not-in-voice"));
            } else if(check == VoiceCheck.USER_IN_DIFFERENT_VOICE) {
                ctx.sendMessage(
                        $(ctx.getLanguage(), "plugins.music.user-not-in-same-voice"));
            } else if(check == VoiceCheck.SELF_NOT_IN_VOICE) {
                ctx.sendMessage(
                        $(ctx.getLanguage(), "plugins.music.bot-not-in-voice"));
            } else {
                final NekoTrackContext context = new NekoTrackContext(
                        ctx.getUser().id(),
                        ctx.getMessage().channelId(),
                        ctx.getMessage().guildId()
                );
                
                queueTrack(ctx, context);
            }
        });
    }
    
    private void queueTrack(final CommandContext ctx, final NekoTrackContext context) {
        try {
            final String domainName = getDomainName(ctx.getArgstr()).toLowerCase();
            switch(domainName) {
                case "youtube.com":
                case "youtu.be": {
                    // YT URLs are ok, so just send it directly
                    mewna().singyeong().send("nekomimi", new QueryBuilder().contains("guilds", ctx.getGuild().id()).build(),
                            new JsonObject().put("type", "VOICE_QUEUE")
                                    .put("url", ctx.getArgstr())
                                    .put("context", JsonObject.mapFrom(context)));
                    break;
                }
                case "open.spotify.com":
                case "spotify.com": {
                    ctx.sendMessage(
                            $(ctx.getLanguage(), "plugins.music.commands.queue.no-spotify"));
                    break;
                }
                default: {
                    ctx.sendMessage(
                            $(ctx.getLanguage(), "plugins.music.commands.queue.invalid-url"));
                    break;
                }
            }
        } catch(final Exception e) {
            // Not a valid url, search it
            mewna().singyeong().send("nekomimi",
                    new QueryBuilder().contains("guilds", ctx.getGuild().id()).build(),
                    new JsonObject().put("type", "VOICE_QUEUE")
                            .put("search", ctx.getArgstr())
                            .put("context", JsonObject.mapFrom(context)));
        }
    }
    
    @Command(names = {"play", "p"}, desc = "commands.music.play", usage = "play", examples = "play")
    public void play(final CommandContext ctx) {
        final Guild guild = ctx.getGuild();
        checkState(guild, ctx.getUser()).thenAccept(check -> {
            if(check == VoiceCheck.USER_NOT_IN_VOICE) {
                ctx.sendMessage(
                        $(ctx.getLanguage(), "plugins.music.user-not-in-voice"));
            } else if(check == VoiceCheck.USER_IN_DIFFERENT_VOICE) {
                ctx.sendMessage(
                        $(ctx.getLanguage(), "plugins.music.user-not-in-same-voice"));
            } else if(check == VoiceCheck.SELF_NOT_IN_VOICE) {
                ctx.sendMessage(
                        $(ctx.getLanguage(), "plugins.music.bot-not-in-voice"));
            } else {
                if(ctx.getArgs().isEmpty()) {
                    mewna().singyeong().send("nekomimi", new QueryBuilder().contains("guilds", ctx.getGuild().id()).build(),
                            new JsonObject().put("type", "VOICE_PLAY")
                                    .put("guild_id", ctx.getGuild().id()));
                } else {
                    final NekoTrackContext context = new NekoTrackContext(
                            ctx.getUser().id(),
                            ctx.getMessage().channelId(),
                            ctx.getMessage().guildId()
                    );
                    queueTrack(ctx, context);
                }
            }
        });
    }
    
    @Command(names = "skip", desc = "commands.music.skip", usage = "skip", examples = "skip")
    public void skip(final CommandContext ctx) {
        final Guild guild = ctx.getGuild();
        checkState(guild, ctx.getUser()).thenAccept(check -> {
            if(check == VoiceCheck.USER_NOT_IN_VOICE) {
                ctx.sendMessage(
                        $(ctx.getLanguage(), "plugins.music.user-not-in-voice"));
            } else if(check == VoiceCheck.USER_IN_DIFFERENT_VOICE) {
                ctx.sendMessage(
                        $(ctx.getLanguage(), "plugins.music.user-not-in-same-voice"));
            } else if(check == VoiceCheck.SELF_NOT_IN_VOICE) {
                ctx.sendMessage(
                        $(ctx.getLanguage(), "plugins.music.bot-not-in-voice"));
            } else {
                mewna().singyeong().send("nekomimi", new QueryBuilder().contains("guilds", ctx.getGuild().id()).build(),
                        new JsonObject().put("type", "VOICE_SKIP")
                                .put("guild_id", ctx.getGuild().id()));
            }
        });
    }
    
    @Command(names = {"np", "nowplaying"}, desc = "commands.music.np", usage = "np", examples = "np")
    public void np(final CommandContext ctx) {
        final NekoTrackContext context = new NekoTrackContext(
                ctx.getUser().id(),
                ctx.getMessage().channelId(),
                ctx.getMessage().guildId()
        );
        mewna().singyeong().send("nekomimi", new QueryBuilder().contains("guilds", ctx.getGuild().id()).build(),
                new JsonObject().put("type", "VOICE_NOW_PLAYING")
                        .put("guild_id", ctx.getGuild().id())
                        .put("context", JsonObject.mapFrom(context)));
    }
    
    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Event(EventType.AUDIO_TRACK_QUEUE)
    public void handleTrackQueue(final NekoTrackEvent event) {
        final EmbedBuilder builder = new EmbedBuilder();
        // lol
        
        if(event.track() == null) {
            // TODO: Dealing with no-tracks-found event
            return;
            // Sentry.capture(new EventBuilder().withMessage("Captured null event track! Data:\n" + JsonObject.mapFrom(event).encodePrettily()).build());
        }
        // back to srs bsns
        builder.title(Emotes.YES + ' ' + $(database().language(event.track().context().guild()), "plugins.music.events.song-queued"))
                .url(event.track().url())
                .field($(database().language(event.track().context().guild()), "plugins.music.events.title"),
                        event.track().title(), true)
                .field("\u200B", "\u200B", true)
                .field($(database().language(event.track().context().guild()), "plugins.music.events.artist"),
                        event.track().author(), true)
                .field($(database().language(event.track().context().guild()), "plugins.music.events.length"),
                        Time.toHumanReadableDuration(event.track().length()), true);
        catnip().rest().channel().sendMessage(event.track().context().channel(), builder.build());
    }
    
    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Event(EventType.AUDIO_TRACK_START)
    public void handleTrackStart(final NekoTrackEvent event) {
        final EmbedBuilder builder = new EmbedBuilder();
        builder.title(Emotes.YES + ' ' + $(database().language(event.track().context().guild()), "plugins.music.events.song-started"))
                .url(event.track().url())
                .field($(database().language(event.track().context().guild()), "plugins.music.events.title"),
                        event.track().title(), true)
                .field("\u200B", "\u200B", true)
                .field($(database().language(event.track().context().guild()), "plugins.music.events.artist"),
                        event.track().author(), true)
                .field($(database().language(event.track().context().guild()), "plugins.music.events.length"),
                        Time.toHumanReadableDuration(event.track().length()), true);
        catnip().rest().channel().sendMessage(event.track().context().channel(), builder.build());
    }
    
    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Event(EventType.AUDIO_QUEUE_END)
    public void handleQueueEnd(final NekoTrackEvent event) {
        final EmbedBuilder builder = new EmbedBuilder();
        builder.title(Emotes.YES + ' ' + $(database().language(event.track().context().guild()), "plugins.music.events.queue-ended"));
        catnip().rest().channel().sendMessage(event.track().context().channel(), builder.build());
    }
    
    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Event(EventType.AUDIO_TRACK_NOW_PLAYING)
    public void handleNowPlaying(final NekoTrackEvent event) {
        if(event.track() == null || event.track().title() == null) {
            final EmbedBuilder builder = new EmbedBuilder();
            builder.title(Emotes.NO + ' ' + $(database().language(event.track().context().guild()), "plugins.music.events.now-playing.nothing"));
            catnip().rest().channel().sendMessage(event.track().context().channel(), builder.build());
        } else {
            final EmbedBuilder builder = new EmbedBuilder();
            builder.title(Emotes.YES + ' ' + $(database().language(event.track().context().guild()), "plugins.music.events.now-playing.now-playing"))
                    .url(event.track().url())
                    .field($(database().language(event.track().context().guild()), "plugins.music.events.title"),
                            event.track().title(), true)
                    .field("\u200B", "\u200B", true)
                    .field($(database().language(event.track().context().guild()), "plugins.music.events.artist"),
                            event.track().author(), true)
                    .field($(database().language(event.track().context().guild()), "plugins.music.events.length"),
                            Time.toHumanReadableDuration(event.track().length()), true);
            if(event.track().position() > 0) {
                // I want these parens to make it more obvious to myself
                //noinspection UnnecessaryParentheses
                final int pos = (int) (((double) event.track().position() / event.track().length()) * 10);
                final StringBuilder bar = new StringBuilder();
                for(int i = 0; i < 10; i++) {
                    if(i < pos) {
                        bar.append(Emotes.FULL_BAR);
                    } else if(i == pos) {
                        bar.append("\uD83D\uDD18");
                    } else {
                        bar.append(Emotes.EMPTY_BAR);
                    }
                }
                final Duration position = Duration.ofMillis(event.track().position());
                final Duration length = Duration.ofMillis(event.track().length());
                builder.field($(database().language(event.track().context().guild()), "plugins.music.events.time"),
                        String.format("%s:%s / %s:%s\n%s",
                                Strings.padStart("" + position.toMinutesPart(), 2, '0'),
                                Strings.padStart("" + position.toSecondsPart(), 2, '0'),
                                Strings.padStart("" + length.toMinutesPart(), 2, '0'),
                                Strings.padStart("" + length.toSecondsPart(), 2, '0'),
                                bar.toString()),
                        false);
            }
            catnip().rest().channel().sendMessage(event.track().context().channel(), builder.build());
        }
    }
    
    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Event(EventType.AUDIO_TRACK_QUEUE_MANY)
    public void handleQueueMany(final NekoTrackEvent event) {
        final EmbedBuilder builder = new EmbedBuilder();
        builder.title(Emotes.YES + ' ' + $(database().language(event.track().context().guild()), "plugins.music.events.queue.many")
                .replace("$amount", event.track().title()));
        catnip().rest().channel().sendMessage(event.track().context().channel(), builder.build());
    }
    
    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Event(EventType.AUDIO_TRACK_NO_MATCHES)
    public void handleNoMatches(final NekoTrackEvent event) {
        final EmbedBuilder builder = new EmbedBuilder();
        builder.title(Emotes.NO + ' ' + $(database().language(event.track().context().guild()), "plugins.music.events.queue.no-matches"));
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
        
        return SafeVertxCompletableFuture.from(mewna().vertx(), future);
    }
    
    private enum VoiceCheck {
        USER_NOT_IN_VOICE,
        SELF_NOT_IN_VOICE,
        USER_IN_DIFFERENT_VOICE,
        SELF_AND_USER_IN_SAME_VOICE,
    }
}