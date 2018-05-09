package com.mewna.plugin.impl.audio;

import com.mewna.cache.entity.Channel;
import com.mewna.cache.entity.Guild;
import com.mewna.cache.entity.User;
import com.mewna.cache.entity.VoiceState;
import com.mewna.plugin.BasePlugin;
import com.mewna.plugin.Command;
import com.mewna.plugin.CommandContext;
import com.mewna.plugin.Plugin;
import com.mewna.plugin.event.Event;
import com.mewna.plugin.event.EventType;
import com.mewna.plugin.event.audio.AudioTrackEvent;
import com.mewna.plugin.util.Emotes;
import com.mewna.util.Time;
import net.dv8tion.jda.core.EmbedBuilder;
import org.json.JSONObject;

/**
 * @author amy
 * @since 4/18/18.
 */
@Plugin("audio")
public class PluginAudio extends BasePlugin {
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
    
    @Command(names = {"music", "m"}, desc = "Do all things music", usage = {"music join", "music leave", "music np"},
            examples = {"music join", "music leave", "music queue darude sandstorm", "music np"})
    public void music(final CommandContext ctx) {
        if(!ctx.getArgs().isEmpty()) {
            final String sub = ctx.getArgs().remove(0);
            switch(sub.toLowerCase()) {
                case "j":
                case "join": {
                    final VoiceCheck check = checkState(ctx.getGuild(), ctx.getUser());
                    if(check == VoiceCheck.USER_NOT_IN_VOICE) {
                        getRestJDA().sendMessage(ctx.getChannel(), "You're not in a voice channel!").queue();
                    } else if(check == VoiceCheck.USER_IN_DIFFERENT_VOICE || check == VoiceCheck.SELF_AND_USER_IN_SAME_VOICE) {
                        getRestJDA().sendMessage(ctx.getChannel(), "I'm already in a voice channel!").queue();
                    } else {
                        final VoiceState state = this.getMewna().getCache().getVoiceState(ctx.getUser().getId());
                        getRestJDA().sendMessage(ctx.getChannel(), "Connecting to voice channel #"
                                + ctx.getChannel().getName()).queue();
                        getLogger().info("Attempting join -> voice channel {}#{}", ctx.getGuild().getId(),
                                state.getChannel().getId());
                        // Tell shards to join, which will then tell audio server to connect
                        this.getMewna().getNats().pushShardEvent("AUDIO_CONNECT", new JSONObject()
                                .put("guild_id", ctx.getGuild().getId())
                                .put("channel_id", state.getChannel().getId()));
                    }
                    break;
                }
                case "l":
                case "leave": {
                    if(!ctx.getArgs().isEmpty()) {
                        final String arg = ctx.getArgs().get(0);
                        if(arg.equalsIgnoreCase("-f") || arg.equalsIgnoreCase("--force")) {
                            getLogger().info("Forcing leave -> guild voice {}", ctx.getGuild().getId());
                            this.getMewna().getCache().deleteSelfVoiceState(ctx.getGuild().getId());
                            // Tell audio server to disconnect, which will then tell shards to leave
                            this.getMewna().getNats().pushAudioEvent("AUDIO_DISCONNECT", new JSONObject()
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
                        getRestJDA().sendMessage(ctx.getChannel(), "I'm not in a voice channel! If this isn't correct, " +
                                "run this command again, but put `--force` at the end").queue();
                    } else {
                        final VoiceState state = this.getMewna().getCache().getSelfVoiceState(ctx.getGuild().getId());
                        getLogger().info("Attempting leave -> voice channel {}#{}", ctx.getGuild().getId(),
                                state.getChannel().getId());
                        // Tell audio server to disconnect, which will then tell shards to leave
                        this.getMewna().getNats().pushAudioEvent("AUDIO_DISCONNECT", new JSONObject()
                                .put("guild_id", ctx.getGuild().getId()));
                    }
                    break;
                }
                case "q":
                case "queue": {
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
                            this.getMewna().getNats().pushAudioEvent("AUDIO_QUEUE", new JSONObject()
                                    .put("ctx", ctxToAudioCtx(ctx)).put("track", String.join(" ", ctx.getArgs())));
                        }
                    }
                    break;
                }
                case "s":
                case "p":
                case "skip":
                case "play": {
                    final VoiceCheck check = checkState(ctx.getGuild(), ctx.getUser());
                    if(check == VoiceCheck.USER_NOT_IN_VOICE) {
                        getRestJDA().sendMessage(ctx.getChannel(), "You're not in a voice channel!").queue();
                    } else if(check == VoiceCheck.USER_IN_DIFFERENT_VOICE) {
                        getRestJDA().sendMessage(ctx.getChannel(), "You're not in this voice channel!").queue();
                    } else if(check == VoiceCheck.SELF_NOT_IN_VOICE) {
                        getRestJDA().sendMessage(ctx.getChannel(), "I'm not in a voice channel!").queue();
                    } else {
                        this.getMewna().getNats().pushAudioEvent("AUDIO_PLAY", new JSONObject()
                                .put("ctx", ctxToAudioCtx(ctx)).put("track", String.join(" ", ctx.getArgs())));
                    }
                    break;
                }
            }
        }
    }
    
    private JSONObject ctxToAudioCtx(final CommandContext ctx) {
        return new JSONObject().put("guild_id", ctx.getGuild().getId())
                .put("channel_id", ctx.getChannel().getId())
                .put("user_id", ctx.getUser().getId());
    }
    
    private VoiceCheck checkState(final Guild guild, final User user) {
        final VoiceState selfState = this.getMewna().getCache().getSelfVoiceState(guild.getId());
        final VoiceState userState = this.getMewna().getCache().getVoiceState(user.getId());
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
}
