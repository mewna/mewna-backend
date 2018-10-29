package com.mewna.event;

import com.mewna.Mewna;
import com.mewna.catnip.entity.Entity;
import com.mewna.catnip.entity.impl.GuildImpl;
import com.mewna.catnip.entity.impl.MemberImpl;
import com.mewna.catnip.entity.impl.MessageImpl;
import com.mewna.catnip.entity.impl.UserImpl;
import com.mewna.catnip.shard.DiscordEvent.Raw;
import com.mewna.event.discord.DiscordGuildMemberAdd;
import com.mewna.event.discord.DiscordGuildMemberRemove;
import com.mewna.event.discord.DiscordMessageCreate;
import gg.amy.singyeong.Dispatch;
import io.vertx.core.json.JsonObject;
import lombok.RequiredArgsConstructor;

/**
 * @author amy
 * @since 10/27/18.
 */
@RequiredArgsConstructor
public class SingyeongEventManager {
    private final Mewna mewna;
    
    public void handle(final Dispatch dispatch) {
        final JsonObject data = dispatch.data();
        switch(data.getString("type")) {
            case Raw.MESSAGE_CREATE: {
                final var event = DiscordMessageCreate.builder()
                        .guild(Entity.fromJson(mewna.catnip(), GuildImpl.class, data.getJsonObject("guild")))
                        .member(Entity.fromJson(mewna.catnip(), MemberImpl.class, data.getJsonObject("member")))
                        .user(Entity.fromJson(mewna.catnip(), UserImpl.class, data.getJsonObject("user")))
                        .message(Entity.fromJson(mewna.catnip(), MessageImpl.class, data.getJsonObject("message")))
                        .build();
                // mewna.getPluginManager().processEvent(data.getString("type"), event);
                mewna.commandManager().tryExecCommand(event);
                break;
            }
            case Raw.GUILD_MEMBER_ADD: {
                final var event = DiscordGuildMemberAdd.builder()
                        .guild(Entity.fromJson(mewna.catnip(), GuildImpl.class, data.getJsonObject("guild")))
                        .member(Entity.fromJson(mewna.catnip(), MemberImpl.class, data.getJsonObject("member")))
                        .user(Entity.fromJson(mewna.catnip(), UserImpl.class, data.getJsonObject("user")))
                        .build();
                mewna.pluginManager().processEvent(data.getString("type"), event);
                break;
            }
            case Raw.GUILD_MEMBER_REMOVE: {
                final var event = DiscordGuildMemberRemove.builder()
                        .guild(Entity.fromJson(mewna.catnip(), GuildImpl.class, data.getJsonObject("guild")))
                        .member(Entity.fromJson(mewna.catnip(), MemberImpl.class, data.getJsonObject("member")))
                        .user(Entity.fromJson(mewna.catnip(), UserImpl.class, data.getJsonObject("user")))
                        .build();
                mewna.pluginManager().processEvent(data.getString("type"), event);
                break;
            }
        }
    }
}
