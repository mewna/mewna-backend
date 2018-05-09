package com.mewna.plugin.impl.audio;

import lombok.Value;
import org.json.JSONObject;

/**
 * @author amy
 * @since 4/22/18.
 */
@Value
public class AudioTrackInfo {
    private String identifier;
    private String author;
    private int length;
    private boolean stream;
    private String title;
    private String uri;
    
    public static AudioTrackInfo fromJson(JSONObject o) {
        return new AudioTrackInfo(o.getString("identifier"), o.getString("author"), o.getInt("length"),
                o.getBoolean("isStream"), o.getString("title"), o.getString("uri"));
    }
}
