package com.mewna.data.account;

import com.fasterxml.jackson.annotation.JsonProperty;
import gg.amy.pgorm.annotations.GIndex;
import gg.amy.pgorm.annotations.PrimaryKey;
import gg.amy.pgorm.annotations.Table;
import lombok.*;
import lombok.experimental.Accessors;

import java.util.ArrayList;
import java.util.List;

/**
 * @author amy
 * @since 6/3/19.
 */
@Getter
@Setter
@Accessors(fluent = true)
@AllArgsConstructor
@RequiredArgsConstructor
@Table("premium_accounts")
@GIndex({"id", "personalPrefixes", "uploadedBackground"})
@Builder(toBuilder = true)
@SuppressWarnings("unused")
public class PremiumAccount {
    @PrimaryKey
    @JsonProperty("id")
    private String id;
    @JsonProperty("personalPrefixes")
    private List<String> personalPrefixes = new ArrayList<>();
    @JsonProperty("uploadedBackground")
    private String uploadedBackground;
}
