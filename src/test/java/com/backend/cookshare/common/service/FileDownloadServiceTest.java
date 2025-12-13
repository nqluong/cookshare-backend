package com.backend.cookshare.common.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class FileDownloadServiceTest {

    @InjectMocks
    private FileDownloadService service;

    private UUID userId;

    private Path avatarDir;

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);
        ReflectionTestUtils.setField(service, "uploadDir", "target/test-uploads");
        userId = UUID.randomUUID();
        avatarDir = Path.of("target/test-uploads/avatar");
        if (!Files.exists(avatarDir)) {
            Files.createDirectories(avatarDir);
        }
    }

    @Test
    void downloadImageToAvatar_ShouldReturnNull_WhenUrlIsNull() {
        String result = service.downloadImageToAvatar(null, userId);
        assertNull(result);
    }

    @Test
    void downloadImageToAvatar_ShouldReturnSame_WhenUrlIsLocal() {
        String url = "/avatar/test.jpg";
        String result = service.downloadImageToAvatar(url, userId);
        assertEquals(url, result);
    }

    @Test
    void downloadImageToAvatar_ShouldDownloadFile_WhenUrlIsValid() throws Exception {
        // Dùng file local làm giả URL để test
        Path tempFile = Files.createTempFile("test-image", ".jpg");
        String imageUrl = tempFile.toUri().toString();

        String result = service.downloadImageToAvatar(imageUrl, userId);

        assertNotNull(result);
        assertTrue(result.startsWith("/avatar/"));

        Path filePath = avatarDir.resolve(result.replace("/avatar/", ""));
        assertTrue(Files.exists(filePath));

        // Cleanup
        Files.deleteIfExists(filePath);
        Files.deleteIfExists(tempFile);
    }

    @Test
    void deleteOldAvatar_ShouldDeleteFile_WhenFileExists() throws Exception {
        Path testFile = avatarDir.resolve("oldfile.jpg");
        Files.createFile(testFile);

        service.deleteOldAvatar("/avatar/oldfile.jpg");

        assertFalse(Files.exists(testFile));
    }

    @Test
    void deleteOldAvatar_ShouldDoNothing_WhenFileNotExist() {
        service.deleteOldAvatar("/avatar/nonexistent.jpg");
        // Không ném exception → passed
    }
}
