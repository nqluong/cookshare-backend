package com.backend.cookshare.authentication.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FacebookUserInfo {
    @JsonProperty("id")
    private String facebookId;

    @JsonProperty("email")
    private String email;

    @JsonProperty("name")
    private String name;

    @JsonProperty("picture")
    private Picture picture;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Picture {
        @JsonProperty("data")
        private PictureData data;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PictureData {
        @JsonProperty("url")
        private String url;

        @JsonProperty("height")
        private Integer height;

        @JsonProperty("width")
        private Integer width;
    }

    public String getPictureUrl() {
        if (picture != null && picture.getData() != null) {
            return picture.getData().getUrl();
        }
        return null;
    }
}

