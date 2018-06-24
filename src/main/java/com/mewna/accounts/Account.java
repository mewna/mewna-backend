package com.mewna.accounts;

import gg.amy.pgorm.annotations.GIndex;
import gg.amy.pgorm.annotations.PrimaryKey;
import gg.amy.pgorm.annotations.Table;
import lombok.Value;

/**
 * @author amy
 * @since 6/23/18.
 */
@Value
@Table("accounts")
@GIndex({"id", "email", "username", "discordAccountId"})
public class Account {
    @PrimaryKey
    private String id;
    private String email;
    private String username;
    private String displayName;
    private String discordAccountId;
    private String avatar;
}
