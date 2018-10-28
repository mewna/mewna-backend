package com.mewna.event.discord;

import com.mewna.catnip.entity.guild.Guild;
import com.mewna.catnip.entity.guild.Member;
import com.mewna.catnip.entity.user.User;
import lombok.*;
import lombok.experimental.Accessors;

/**
 * @author amy
 * @since 10/27/18.
 */
@Getter
@Setter
@Builder
@Accessors(fluent = true)
@NoArgsConstructor
@AllArgsConstructor
public class DiscordGuildMemberAdd {
    private Guild guild;
    private User user;
    private Member member;
}
