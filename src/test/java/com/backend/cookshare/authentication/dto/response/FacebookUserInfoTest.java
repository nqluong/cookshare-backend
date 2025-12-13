package com.backend.cookshare.authentication.dto.response;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class FacebookUserInfoTest {

    @Test
    void testGetterAndSetter() {
        FacebookUserInfo info = new FacebookUserInfo();

        FacebookUserInfo.PictureData data = new FacebookUserInfo.PictureData();
        data.setUrl("http://example.com/avatar.jpg");

        FacebookUserInfo.Picture picture = new FacebookUserInfo.Picture();
        picture.setData(data);

        info.setFacebookId("12345");
        info.setEmail("test@example.com");
        info.setName("John Doe");
        info.setPicture(picture);

        assertEquals("12345", info.getFacebookId());
        assertEquals("test@example.com", info.getEmail());
        assertEquals("John Doe", info.getName());
        assertEquals("http://example.com/avatar.jpg", info.getPicture().getData().getUrl());
    }

    @Test
    void testBuilder() {
        FacebookUserInfo.PictureData data = new FacebookUserInfo.PictureData("http://img.com/pic.png");
        FacebookUserInfo.Picture picture = new FacebookUserInfo.Picture(data);

        FacebookUserInfo info = FacebookUserInfo.builder()
                .facebookId("fb001")
                .email("builder@test.com")
                .name("Builder User")
                .picture(picture)
                .build();

        assertEquals("fb001", info.getFacebookId());
        assertEquals("builder@test.com", info.getEmail());
        assertEquals("Builder User", info.getName());
        assertEquals("http://img.com/pic.png", info.getPicture().getData().getUrl());
    }

    @Test
    void testGetPictureUrl_ReturnsCorrectUrl() {
        FacebookUserInfo.PictureData data = new FacebookUserInfo.PictureData("http://pic.com/a.jpg");
        FacebookUserInfo.Picture picture = new FacebookUserInfo.Picture(data);

        FacebookUserInfo info = new FacebookUserInfo();
        info.setPicture(picture);

        assertEquals("http://pic.com/a.jpg", info.getPictureUrl());
    }

    @Test
    void testGetPictureUrl_ReturnsNull_WhenPictureIsNull() {
        FacebookUserInfo info = new FacebookUserInfo();
        info.setPicture(null);
        assertNull(info.getPictureUrl());
    }

    @Test
    void testGetPictureUrl_ReturnsNull_WhenPictureDataIsNull() {
        FacebookUserInfo.Picture picture = new FacebookUserInfo.Picture(null);
        FacebookUserInfo info = new FacebookUserInfo();
        info.setPicture(picture);

        assertNull(info.getPictureUrl());
    }
}
