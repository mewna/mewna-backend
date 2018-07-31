package com.mewna.plugin.plugins;

import com.mewna.plugin.BasePlugin;
import com.mewna.plugin.Plugin;
import com.mewna.plugin.plugins.settings.MusicSettings;

/**
 * @author amy
 * @since 5/19/18.
 */
@Plugin(name = "Music", desc = "Control the way music is played in your server.", settings = MusicSettings.class,
        enabled = false)
public class PluginMusic extends BasePlugin {
    /*
    @Event(EventType.AUDIO_TRACK_QUEUE)
    public void handleTrackQueue(final AudioTrackEvent event) {
        final Channel channel = event.getChannel();
        final EmbedBuilder builder = new EmbedBuilder();
        builder.setTitle(Emotes.YES + " Song queued")
                .addField("Title", event.getInfo().getTitle(), true)
                .addBlankField(true)
                .addField("Artist", event.getInfo().getAuthor(), true)
                .addField("Length", Time.toHumanReadableDuration(event.getInfo().getLength()), true);
        getRestJDA().sendMessage(channel, builder.build()).queue();
    }
    
    @Event(EventType.AUDIO_TRACK_START)
    public void handleTrackStart(final AudioTrackEvent event) {
        final Channel channel = event.getChannel();
        final EmbedBuilder builder = new EmbedBuilder();
        builder.setTitle(Emotes.YES + " Song started")
                .addField("Title", event.getInfo().getTitle(), true)
                .addBlankField(true)
                .addField("Artist", event.getInfo().getAuthor(), true)
                .addField("Length", Time.toHumanReadableDuration(event.getInfo().getLength()), true);
        getRestJDA().sendMessage(channel, builder.build()).queue();
    }
    
    @Event(EventType.AUDIO_QUEUE_END)
    public void handleQueueEnd(final AudioTrackEvent event) {
        final Channel channel = event.getChannel();
        final EmbedBuilder builder = new EmbedBuilder();
        builder.setTitle(Emotes.YES + " Queue ended");
        getRestJDA().sendMessage(channel, builder.build()).queue();
    }
    
    @Event(EventType.AUDIO_TRACK_NOW_PLAYING)
    public void handleNowPlaying(final AudioTrackEvent event) {
        final Channel channel = event.getChannel();
        final EmbedBuilder builder = new EmbedBuilder();
        builder.setTitle(Emotes.YES + " Now playing")
                .addField("Title", event.getInfo().getTitle(), true)
                .addBlankField(true)
                .addField("Artist", event.getInfo().getAuthor(), true)
                .addField("Length", Time.toHumanReadableDuration(event.getInfo().getLength()), true);
        getRestJDA().sendMessage(channel, builder.build()).queue();
    }
    
    @Command(names = "join", desc = "Bring Mewna into a voice channel with you.", usage = "join", examples = "join")
    public void join(final CommandContext ctx) {
        final VoiceCheck check = checkState(ctx.getGuild(), ctx.getUser());
        if(check == VoiceCheck.USER_NOT_IN_VOICE) {
            getRestJDA().sendMessage(ctx.getChannel(), "You're not in a voice channel!").queue();
        } else if(check == VoiceCheck.USER_IN_DIFFERENT_VOICE || check == VoiceCheck.SELF_AND_USER_IN_SAME_VOICE) {
            getRestJDA().sendMessage(ctx.getChannel(), "I'm already in a voice channel! If this isn't right, try doing `"
                    + ctx.getPrefix() + "leave --force`.").queue();
        } else {
            final VoiceState state = getMewna().getCache().getVoiceState(ctx.getUser().getId());
            getRestJDA().sendMessage(ctx.getChannel(), "Connecting to voice channel #"
                    + ctx.getChannel().getName()).queue();
            getLogger().info("Attempting join -> voice channel {}#{}", ctx.getGuild().getId(),
                    state.getChannel().getId());
            // Tell shards to join, which will then tell audio server to connect
            getMewna().getNats().pushShardEvent("AUDIO_CONNECT", new JSONObject()
                    .put("guild_id", ctx.getGuild().getId())
                    .put("channel_id", state.getChannel().getId()));
        }
    }
    
    @Command(names = "leave", desc = "Make Mewna leave the voice channel she's in.", usage = "leave [-f|--force]",
            examples = {"leave -f", "leave --force"})
    public void leave(final CommandContext ctx) {
        if(!ctx.getArgs().isEmpty()) {
            final String arg = ctx.getArgs().get(0);
            if(arg.equalsIgnoreCase("-f") || arg.equalsIgnoreCase("--force")) {
                getLogger().info("Forcing leave -> guild voice {}", ctx.getGuild().getId());
                getMewna().getCache().deleteSelfVoiceState(ctx.getGuild().getId());
                // Tell audio server to disconnect, which will then tell shards to leave
                getMewna().getNats().pushAudioEvent("AUDIO_DISCONNECT", new JSONObject()
                        .put("guild_id", ctx.getGuild().getId()));
                return;
            }
        }
        final VoiceCheck check = checkState(ctx.getGuild(), ctx.getUser());
        if(check == VoiceCheck.USER_NOT_IN_VOICE) {
            getRestJDA().sendMessage(ctx.getChannel(), "You're not in a voice channel!").queue();
        } else if(check == VoiceCheck.USER_IN_DIFFERENT_VOICE) {
            getRestJDA().sendMessage(ctx.getChannel(), "You're not in this voice channel!").queue();
        } else if(check == VoiceCheck.SELF_NOT_IN_VOICE) {
            getRestJDA().sendMessage(ctx.getChannel(), "I'm not in a voice channel! If this isn't right, try doing `"
                    + ctx.getPrefix() + "leave --force`.").queue();
        } else {
            final VoiceState state = getMewna().getCache().getSelfVoiceState(ctx.getGuild().getId());
            getLogger().info("Attempting leave -> voice channel {}#{}", ctx.getGuild().getId(),
                    state.getChannel().getId());
            // Tell audio server to disconnect, which will then tell shards to leave
            getMewna().getNats().pushAudioEvent("AUDIO_DISCONNECT", new JSONObject()
                    .put("guild_id", ctx.getGuild().getId()));
        }
    }
    
    @Command(names = {"queue", "q"}, desc = "Queue up a song for Mewna to play.", usage = "queue <song>",
            examples = {"q rick astley", "queue darude sandstorm"})
    public void queue(final CommandContext ctx) {
        final VoiceCheck check = checkState(ctx.getGuild(), ctx.getUser());
        if(check == VoiceCheck.USER_NOT_IN_VOICE) {
            getRestJDA().sendMessage(ctx.getChannel(), "You're not in a voice channel!").queue();
        } else if(check == VoiceCheck.USER_IN_DIFFERENT_VOICE) {
            getRestJDA().sendMessage(ctx.getChannel(), "You're not in this voice channel!").queue();
        } else if(check == VoiceCheck.SELF_NOT_IN_VOICE) {
            getRestJDA().sendMessage(ctx.getChannel(), "I'm not in a voice channel!").queue();
        } else {
            if(ctx.getArgs().isEmpty()) {
                getRestJDA().sendMessage(ctx.getChannel(), "You need to give me something to queue!").queue();
            } else {
                getMewna().getNats().pushAudioEvent("AUDIO_QUEUE", new JSONObject()
                        .put("ctx", ctxToAudioCtx(ctx)).put("track", String.join(" ", ctx.getArgs())));
            }
        }
    }
    
    @Command(names = "skip", desc = "Skip the currently-playing song", usage = "skip", examples = "skip")
    public void skip(final CommandContext ctx) {
        final VoiceCheck check = checkState(ctx.getGuild(), ctx.getUser());
        if(check == VoiceCheck.USER_NOT_IN_VOICE) {
            getRestJDA().sendMessage(ctx.getChannel(), "You're not in a voice channel!").queue();
        } else if(check == VoiceCheck.USER_IN_DIFFERENT_VOICE) {
            getRestJDA().sendMessage(ctx.getChannel(), "You're not in this voice channel!").queue();
        } else if(check == VoiceCheck.SELF_NOT_IN_VOICE) {
            getRestJDA().sendMessage(ctx.getChannel(), "I'm not in a voice channel!").queue();
        } else {
            getMewna().getNats().pushAudioEvent("AUDIO_PLAY", new JSONObject()
                    .put("ctx", ctxToAudioCtx(ctx)).put("track", String.join(" ", ctx.getArgs())));
        }
    }
    
    @Command(names = "play", desc = "Start playing the songs that have been queued up.", usage = "play", examples = "play")
    public void play(final CommandContext ctx) {
        final VoiceCheck check = checkState(ctx.getGuild(), ctx.getUser());
        if(check == VoiceCheck.USER_NOT_IN_VOICE) {
            getRestJDA().sendMessage(ctx.getChannel(), "You're not in a voice channel!").queue();
        } else if(check == VoiceCheck.USER_IN_DIFFERENT_VOICE) {
            getRestJDA().sendMessage(ctx.getChannel(), "You're not in this voice channel!").queue();
        } else if(check == VoiceCheck.SELF_NOT_IN_VOICE) {
            getRestJDA().sendMessage(ctx.getChannel(), "I'm not in a voice channel!").queue();
        } else {
            getMewna().getNats().pushAudioEvent("AUDIO_PLAY", new JSONObject()
                    .put("ctx", ctxToAudioCtx(ctx)).put("track", String.join(" ", ctx.getArgs())));
        }
    }
    
    @Command(names = {"np", "nowplaying"}, desc = "Show the currently-playing song", usage = "np", examples = "np")
    public void np(final CommandContext ctx) {
        getMewna().getNats().pushAudioEvent("AUDIO_NOW_PLAYING", new JSONObject()
                .put("ctx", ctxToAudioCtx(ctx)).put("track", String.join(" ", ctx.getArgs())));
    }
    
    private JSONObject ctxToAudioCtx(final CommandContext ctx) {
        return new JSONObject().put("guild_id", ctx.getGuild().getId())
                .put("channel_id", ctx.getChannel().getId())
                .put("user_id", ctx.getUser().getId());
    }
    
    private VoiceCheck checkState(final Guild guild, final User user) {
        final VoiceState selfState = getMewna().getCache().getSelfVoiceState(guild.getId());
        final VoiceState userState = getMewna().getCache().getVoiceState(user.getId());
        if(userState == null || userState.getChannel() == null) {
            return VoiceCheck.USER_NOT_IN_VOICE;
        }
        if(selfState == null || selfState.getChannel() == null) {
            return VoiceCheck.SELF_NOT_IN_VOICE;
        }
        if(userState.getChannel().getId().equalsIgnoreCase(selfState.getChannel().getId())) {
            return VoiceCheck.SELF_AND_USER_IN_SAME_VOICE;
        } else {
            return VoiceCheck.USER_IN_DIFFERENT_VOICE;
        }
    }
    
    private enum VoiceCheck {
        USER_NOT_IN_VOICE,
        SELF_NOT_IN_VOICE,
        USER_IN_DIFFERENT_VOICE,
        SELF_AND_USER_IN_SAME_VOICE,
    }
    */
}
