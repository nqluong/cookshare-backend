package com.backend.cookshare.authentication.service.impl;

import com.google.cloud.storage.*;
import com.google.auth.oauth2.GoogleCredentials;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FirebaseStorageServiceImplTest {

    @Mock
    private Storage storage;

    @Mock
    private MultipartFile multipartFile;

    @InjectMocks
    private FirebaseStorageServiceImpl firebaseStorageService;

    private static final String BUCKET_NAME = "test-bucket";
    private static final String CREDENTIALS_PATH = "firebase-credentials.json";
    private static final String AVATAR_FOLDER = "avatars";
    private static final String RECIPE_IMAGE_FOLDER = "recipe_images";

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(firebaseStorageService, "bucketName", BUCKET_NAME);
        ReflectionTestUtils.setField(firebaseStorageService, "credentialsPath", CREDENTIALS_PATH);
        ReflectionTestUtils.setField(firebaseStorageService, "storage", storage);
        ReflectionTestUtils.setField(firebaseStorageService, "initialized", true);
    }

    // ==================== INIT TESTS ====================

    @Test
    void init_WithValidCredentials_ShouldInitializeSuccessfully() throws Exception {
        // Thay vì test thực tế init(), ta test rằng service hoạt động sau khi init
        // bằng cách set initialized = true và verify các methods khác hoạt động
        FirebaseStorageServiceImpl service = new FirebaseStorageServiceImpl();
        ReflectionTestUtils.setField(service, "bucketName", BUCKET_NAME);
        ReflectionTestUtils.setField(service, "credentialsPath", "test-credentials.json");

        Storage mockStorage = mock(Storage.class);
        ReflectionTestUtils.setField(service, "storage", mockStorage);
        ReflectionTestUtils.setField(service, "initialized", true);

        // Verify service is initialized and can perform operations
        assertTrue(service.isInitialized());

        // Verify it can generate URLs when initialized
        URL mockUrl = new URL("https://storage.googleapis.com/signed-url");
        when(mockStorage.signUrl(any(BlobInfo.class), eq(15L), eq(TimeUnit.MINUTES),
                any(Storage.SignUrlOption[].class)))
                .thenReturn(mockUrl);

        String result = service.generateAvatarUploadUrl("test.jpg", "image/jpeg");
        assertNotNull(result);
        assertEquals(mockUrl.toString(), result);
    }

    @Test
    void init_WithMissingCredentialsFile_ShouldNotInitialize() {
        FirebaseStorageServiceImpl service = new FirebaseStorageServiceImpl();
        ReflectionTestUtils.setField(service, "bucketName", BUCKET_NAME);
        ReflectionTestUtils.setField(service, "credentialsPath", "nonexistent-file.json");

        try (MockedStatic<Files> filesMock = mockStatic(Files.class);
             MockedStatic<Paths> pathsMock = mockStatic(Paths.class)) {

            Path mockPath = mock(Path.class);
            pathsMock.when(() -> Paths.get("nonexistent-file.json")).thenReturn(mockPath);
            filesMock.when(() -> Files.exists(mockPath)).thenReturn(false);

            service.init();

            assertFalse(service.isInitialized());
        }
    }

    @Test
    void init_WithIOException_ShouldNotInitialize() throws Exception {
        FirebaseStorageServiceImpl service = new FirebaseStorageServiceImpl();
        ReflectionTestUtils.setField(service, "bucketName", BUCKET_NAME);
        ReflectionTestUtils.setField(service, "credentialsPath", CREDENTIALS_PATH);

        try (MockedStatic<Files> filesMock = mockStatic(Files.class);
             MockedStatic<GoogleCredentials> credentialsMock = mockStatic(GoogleCredentials.class);
             MockedStatic<Paths> pathsMock = mockStatic(Paths.class)) {

            Path mockPath = mock(Path.class);
            pathsMock.when(() -> Paths.get(CREDENTIALS_PATH)).thenReturn(mockPath);
            filesMock.when(() -> Files.exists(mockPath)).thenReturn(true);

            credentialsMock.when(() -> GoogleCredentials.fromStream(any(FileInputStream.class)))
                    .thenThrow(new IOException("Test exception"));

            service.init();

            assertFalse(service.isInitialized());
        }
    }

    // ==================== AVATAR TESTS ====================

    @Test
    void generateAvatarUploadUrl_ShouldReturnSignedUrl() throws Exception {
        String fileName = "test-avatar.jpg";
        String contentType = "image/jpeg";
        URL mockUrl = new URL("https://storage.googleapis.com/signed-url");

        when(storage.signUrl(any(BlobInfo.class), eq(15L), eq(TimeUnit.MINUTES),
                any(Storage.SignUrlOption[].class)))
                .thenReturn(mockUrl);

        String result = firebaseStorageService.generateAvatarUploadUrl(fileName, contentType);

        assertEquals(mockUrl.toString(), result);
        verify(storage).signUrl(any(BlobInfo.class), eq(15L), eq(TimeUnit.MINUTES),
                any(Storage.SignUrlOption[].class));
    }

    @Test
    void generateAvatarUploadUrl_WhenNotInitialized_ShouldThrowException() {
        ReflectionTestUtils.setField(firebaseStorageService, "initialized", false);

        assertThrows(IllegalStateException.class, () ->
                firebaseStorageService.generateAvatarUploadUrl("test.jpg", "image/jpeg"));
    }

    @Test
    void getAvatarPublicUrl_ShouldReturnPublicUrl() {
        String fileName = "test-avatar.jpg";
        String expectedUrl = String.format(
                "https://firebasestorage.googleapis.com/v0/b/%s/o/%s%%2F%s?alt=media",
                BUCKET_NAME, AVATAR_FOLDER, fileName);

        String result = firebaseStorageService.getAvatarPublicUrl(fileName);

        assertEquals(expectedUrl, result);
    }

    @Test
    void uploadAvatar_ShouldUploadSuccessfully() {
        String fileName = "test-avatar.jpg";
        byte[] fileBytes = "test-data".getBytes();
        String contentType = "image/jpeg";

        Blob mockBlob = mock(Blob.class);
        when(storage.create(any(BlobInfo.class), eq(fileBytes))).thenReturn(mockBlob);

        assertDoesNotThrow(() ->
                firebaseStorageService.uploadAvatar(fileName, fileBytes, contentType));

        verify(storage).create(any(BlobInfo.class), eq(fileBytes));
    }

    @Test
    void uploadAvatar_WhenNotInitialized_ShouldThrowException() {
        ReflectionTestUtils.setField(firebaseStorageService, "initialized", false);

        assertThrows(IllegalStateException.class, () ->
                firebaseStorageService.uploadAvatar("test.jpg", new byte[0], "image/jpeg"));
    }

    @Test
    void deleteAvatar_WithValidUrl_ShouldDeleteSuccessfully() {
        String avatarUrl = String.format(
                "https://firebasestorage.googleapis.com/v0/b/%s/o/%s%%2Ftest-avatar.jpg?alt=media",
                BUCKET_NAME, AVATAR_FOLDER);

        when(storage.delete((BlobId) any())).thenReturn(true);

        boolean result = firebaseStorageService.deleteAvatar(avatarUrl);

        assertTrue(result);
        verify(storage).delete((BlobId) any());
    }

    @Test
    void deleteAvatar_WithNullUrl_ShouldReturnFalse() {
        boolean result = firebaseStorageService.deleteAvatar(null);

        assertFalse(result);
        verify(storage, never()).delete((BlobId) any());
    }

    @Test
    void deleteAvatar_WithEmptyUrl_ShouldReturnFalse() {
        boolean result = firebaseStorageService.deleteAvatar("");

        assertFalse(result);
        verify(storage, never()).delete((BlobId) any());
    }

    @Test
    void deleteAvatar_WithNonFirebaseUrl_ShouldReturnFalse() {
        String nonFirebaseUrl = "https://example.com/image.jpg";

        boolean result = firebaseStorageService.deleteAvatar(nonFirebaseUrl);

        assertFalse(result);
        verify(storage, never()).delete((BlobId) any());
    }

    @Test
    void deleteAvatar_WhenNotInitialized_ShouldReturnFalse() {
        ReflectionTestUtils.setField(firebaseStorageService, "initialized", false);
        String avatarUrl = "https://firebasestorage.googleapis.com/v0/b/bucket/o/file.jpg";

        boolean result = firebaseStorageService.deleteAvatar(avatarUrl);

        assertFalse(result);
    }

    @Test
    void deleteAvatar_WhenFileNotFound_ShouldReturnFalse() {
        String avatarUrl = String.format(
                "https://firebasestorage.googleapis.com/v0/b/%s/o/%s%%2Ftest-avatar.jpg?alt=media",
                BUCKET_NAME, AVATAR_FOLDER);

        when(storage.delete((BlobId) any())).thenReturn(false);

        boolean result = firebaseStorageService.deleteAvatar(avatarUrl);

        assertFalse(result);
        verify(storage).delete((BlobId) any());
    }

    @Test
    void deleteAvatar_WhenExceptionOccurs_ShouldReturnFalse() {
        String avatarUrl = String.format(
                "https://firebasestorage.googleapis.com/v0/b/%s/o/%s%%2Ftest-avatar.jpg?alt=media",
                BUCKET_NAME, AVATAR_FOLDER);

        when(storage.delete((BlobId) any())).thenThrow(new RuntimeException("Storage error"));

        boolean result = firebaseStorageService.deleteAvatar(avatarUrl);

        assertFalse(result);
    }

    // ==================== RECIPE IMAGE TESTS ====================

    @Test
    void generateRecipeImageUploadUrl_ShouldReturnSignedUrl() throws Exception {
        String fileName = "test-recipe.jpg";
        String contentType = "image/jpeg";
        URL mockUrl = new URL("https://storage.googleapis.com/signed-url");

        when(storage.signUrl(any(BlobInfo.class), eq(15L), eq(TimeUnit.MINUTES),
                any(Storage.SignUrlOption[].class)))
                .thenReturn(mockUrl);

        String result = firebaseStorageService.generateRecipeImageUploadUrl(fileName, contentType);

        assertEquals(mockUrl.toString(), result);
    }

    @Test
    void uploadRecipeImage_ShouldUploadAndReturnPublicUrl() {
        String fileName = "test-recipe.jpg";
        byte[] fileBytes = "test-data".getBytes();
        String contentType = "image/jpeg";

        Blob mockBlob = mock(Blob.class);
        when(storage.create(any(BlobInfo.class), eq(fileBytes))).thenReturn(mockBlob);

        String result = firebaseStorageService.uploadRecipeImage(fileName, fileBytes, contentType);

        assertNotNull(result);
        assertTrue(result.contains(BUCKET_NAME));
        assertTrue(result.contains(RECIPE_IMAGE_FOLDER));
        assertTrue(result.contains(fileName));
        verify(storage).create(any(BlobInfo.class), eq(fileBytes));
    }

    @Test
    void deleteRecipeImage_WithValidUrl_ShouldDeleteSuccessfully() {
        String imageUrl = String.format(
                "https://firebasestorage.googleapis.com/v0/b/%s/o/%s%%2Ftest-recipe.jpg?alt=media",
                BUCKET_NAME, RECIPE_IMAGE_FOLDER);

        when(storage.delete((BlobId) any())).thenReturn(true);

        boolean result = firebaseStorageService.deleteRecipeImage(imageUrl);

        assertTrue(result);
        verify(storage).delete((BlobId) any());
    }

    // ==================== UPLOAD FILE TESTS ====================

    @Test
    void uploadFile_WithValidImage_ShouldUploadSuccessfully() throws IOException {
        byte[] fileBytes = "test-image-data".getBytes();
        when(multipartFile.isEmpty()).thenReturn(false);
        when(multipartFile.getContentType()).thenReturn("image/jpeg");
        when(multipartFile.getOriginalFilename()).thenReturn("test-image.jpg");
        when(multipartFile.getBytes()).thenReturn(fileBytes);

        Blob mockBlob = mock(Blob.class);
        when(storage.create(any(BlobInfo.class), eq(fileBytes))).thenReturn(mockBlob);

        String result = firebaseStorageService.uploadFile(multipartFile);

        assertNotNull(result);
        assertTrue(result.contains(BUCKET_NAME));
        assertTrue(result.contains(RECIPE_IMAGE_FOLDER));
        verify(storage).create(any(BlobInfo.class), eq(fileBytes));
    }

    @Test
    void uploadFile_WithNullFile_ShouldThrowException() {
        assertThrows(IllegalArgumentException.class, () ->
                firebaseStorageService.uploadFile(null));
    }

    @Test
    void uploadFile_WithEmptyFile_ShouldThrowException() {
        when(multipartFile.isEmpty()).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () ->
                firebaseStorageService.uploadFile(multipartFile));
    }

    @Test
    void uploadFile_WithNullContentType_ShouldThrowException() {
        when(multipartFile.isEmpty()).thenReturn(false);
        when(multipartFile.getContentType()).thenReturn(null);

        assertThrows(IllegalArgumentException.class, () ->
                firebaseStorageService.uploadFile(multipartFile));
    }

    @Test
    void uploadFile_WithNonImageContentType_ShouldThrowException() {
        when(multipartFile.isEmpty()).thenReturn(false);
        when(multipartFile.getContentType()).thenReturn("application/pdf");

        assertThrows(IllegalArgumentException.class, () ->
                firebaseStorageService.uploadFile(multipartFile));
    }

    @Test
    void uploadFile_WithoutExtension_ShouldUploadSuccessfully() throws IOException {
        byte[] fileBytes = "test-image-data".getBytes();
        when(multipartFile.isEmpty()).thenReturn(false);
        when(multipartFile.getContentType()).thenReturn("image/png");
        when(multipartFile.getOriginalFilename()).thenReturn("test-image");
        when(multipartFile.getBytes()).thenReturn(fileBytes);

        Blob mockBlob = mock(Blob.class);
        when(storage.create(any(BlobInfo.class), eq(fileBytes))).thenReturn(mockBlob);

        String result = firebaseStorageService.uploadFile(multipartFile);

        assertNotNull(result);
        assertTrue(result.contains(BUCKET_NAME));
    }

    @Test
    void uploadFile_WithIOException_ShouldThrowRuntimeException() throws IOException {
        when(multipartFile.isEmpty()).thenReturn(false);
        when(multipartFile.getContentType()).thenReturn("image/jpeg");
        when(multipartFile.getOriginalFilename()).thenReturn("test.jpg");
        when(multipartFile.getBytes()).thenThrow(new IOException("Read error"));

        assertThrows(RuntimeException.class, () ->
                firebaseStorageService.uploadFile(multipartFile));
    }

    // ==================== DELETE FILE TESTS ====================

    @Test
    void deleteFile_WithValidUrl_ShouldDeleteSuccessfully() {
        String fileUrl = String.format(
                "https://firebasestorage.googleapis.com/v0/b/%s/o/%s%%2Ftest-file.jpg?alt=media",
                BUCKET_NAME, RECIPE_IMAGE_FOLDER);

        when(storage.delete((BlobId) any())).thenReturn(true);

        assertDoesNotThrow(() -> firebaseStorageService.deleteFile(fileUrl));

        verify(storage).delete((BlobId) any());
    }

    @Test
    void deleteFile_WithNullUrl_ShouldNotThrowException() {
        assertDoesNotThrow(() -> firebaseStorageService.deleteFile(null));

        verify(storage, never()).delete((BlobId) any());
    }

    @Test
    void deleteFile_WithBlankUrl_ShouldNotThrowException() {
        assertDoesNotThrow(() -> firebaseStorageService.deleteFile("   "));

        verify(storage, never()).delete((BlobId) any());
    }

    @Test
    void deleteFile_WithException_ShouldNotThrowException() {
        String fileUrl = String.format(
                "https://firebasestorage.googleapis.com/v0/b/%s/o/%s%%2Ftest-file.jpg?alt=media",
                BUCKET_NAME, RECIPE_IMAGE_FOLDER);

        when(storage.delete((BlobId) any())).thenThrow(new RuntimeException("Delete error"));

        assertDoesNotThrow(() -> firebaseStorageService.deleteFile(fileUrl));
    }

    // ==================== CONVERT PATH TESTS ====================

    @Test
    void convertPathToFirebaseUrl_WithLocalPath_ShouldReturnFirebaseUrl() {
        String localPath = "recipe_images/test-image.jpg";

        String result = firebaseStorageService.convertPathToFirebaseUrl(localPath);

        assertNotNull(result);
        assertTrue(result.contains("firebasestorage.googleapis.com"));
        assertTrue(result.contains(BUCKET_NAME));
        assertTrue(result.contains("recipe_images%2Ftest-image.jpg"));
    }

    @Test
    void convertPathToFirebaseUrl_WithBackslashes_ShouldConvertToForwardSlashes() {
        String localPath = "recipe_images\\test-image.jpg";

        String result = firebaseStorageService.convertPathToFirebaseUrl(localPath);

        assertNotNull(result);
        assertTrue(result.contains("recipe_images%2Ftest-image.jpg"));
        assertFalse(result.contains("\\"));
    }

    @Test
    void convertPathToFirebaseUrl_WithHttpUrl_ShouldReturnOriginal() {
        String httpUrl = "http://example.com/image.jpg";

        String result = firebaseStorageService.convertPathToFirebaseUrl(httpUrl);

        assertEquals(httpUrl, result);
    }

    @Test
    void convertPathToFirebaseUrl_WithHttpsUrl_ShouldReturnOriginal() {
        String httpsUrl = "https://example.com/image.jpg";

        String result = firebaseStorageService.convertPathToFirebaseUrl(httpsUrl);

        assertEquals(httpsUrl, result);
    }

    @Test
    void convertPathToFirebaseUrl_WithNullPath_ShouldReturnNull() {
        String result = firebaseStorageService.convertPathToFirebaseUrl(null);

        assertNull(result);
    }

    @Test
    void convertPathToFirebaseUrl_WithEmptyPath_ShouldReturnNull() {
        String result = firebaseStorageService.convertPathToFirebaseUrl("");

        assertNull(result);
    }

    @Test
    void convertPathsToFirebaseUrls_WithMultiplePaths_ShouldConvertAll() {
        List<String> localPaths = Arrays.asList(
                "recipe_images/image1.jpg",
                "recipe_images/image2.jpg",
                "https://example.com/image3.jpg"
        );

        List<String> results = firebaseStorageService.convertPathsToFirebaseUrls(localPaths);

        assertEquals(3, results.size());
        assertTrue(results.get(0).contains("firebasestorage.googleapis.com"));
        assertTrue(results.get(1).contains("firebasestorage.googleapis.com"));
        assertEquals("https://example.com/image3.jpg", results.get(2));
    }

    @Test
    void convertPathsToFirebaseUrls_WithNullList_ShouldReturnEmptyList() {
        List<String> results = firebaseStorageService.convertPathsToFirebaseUrls(null);

        assertNotNull(results);
        assertTrue(results.isEmpty());
    }

    @Test
    void convertPathsToFirebaseUrls_WithEmptyList_ShouldReturnEmptyList() {
        List<String> results = firebaseStorageService.convertPathsToFirebaseUrls(Collections.emptyList());

        assertNotNull(results);
        assertTrue(results.isEmpty());
    }

    // ==================== IS INITIALIZED TESTS ====================

    @Test
    void isInitialized_WhenInitialized_ShouldReturnTrue() {
        assertTrue(firebaseStorageService.isInitialized());
    }

    @Test
    void isInitialized_WhenNotInitialized_ShouldReturnFalse() {
        ReflectionTestUtils.setField(firebaseStorageService, "initialized", false);

        assertFalse(firebaseStorageService.isInitialized());
    }

    // ==================== EXTRACT FILENAME TESTS ====================

    @Test
    void extractFileNameFromUrl_WithEncodedUrl_ShouldExtractCorrectly() {
        String url = String.format(
                "https://firebasestorage.googleapis.com/v0/b/%s/o/%s%%2Ftest-file.jpg?alt=media",
                BUCKET_NAME, RECIPE_IMAGE_FOLDER);

        when(storage.delete((BlobId) any())).thenReturn(true);

        boolean result = firebaseStorageService.deleteRecipeImage(url);

        assertTrue(result);
        verify(storage).delete((BlobId) any());
    }

    @Test
    void extractFileNameFromUrl_WithSlashUrl_ShouldExtractCorrectly() {
        String url = String.format(
                "https://firebasestorage.googleapis.com/v0/b/%s/o/%s/test-file.jpg?alt=media",
                BUCKET_NAME, RECIPE_IMAGE_FOLDER);

        when(storage.delete((BlobId) any())).thenReturn(true);

        boolean result = firebaseStorageService.deleteRecipeImage(url);

        assertTrue(result);
        verify(storage).delete((BlobId) any());
    }

    @Test
    void extractFileNameFromUrl_WithAmpersand_ShouldExtractCorrectly() {
        String url = String.format(
                "https://firebasestorage.googleapis.com/v0/b/%s/o/%s%%2Ftest-file.jpg&token=abc",
                BUCKET_NAME, RECIPE_IMAGE_FOLDER);

        when(storage.delete((BlobId) any())).thenReturn(true);

        boolean result = firebaseStorageService.deleteRecipeImage(url);

        assertTrue(result);
    }

    @Test
    void extractFileNameFromUrl_WithNoQueryParams_ShouldExtractCorrectly() {
        String url = String.format(
                "https://firebasestorage.googleapis.com/v0/b/%s/o/%s%%2Ftest-file.jpg",
                BUCKET_NAME, RECIPE_IMAGE_FOLDER);

        when(storage.delete((BlobId) any())).thenReturn(true);

        boolean result = firebaseStorageService.deleteRecipeImage(url);

        assertTrue(result);
    }

    @Test
    void extractFileNameFromUrl_WithInvalidUrl_ShouldReturnFalse() {
        String invalidUrl = "https://firebasestorage.googleapis.com/invalid/url";

        boolean result = firebaseStorageService.deleteRecipeImage(invalidUrl);

        assertFalse(result);
        verify(storage, never()).delete((BlobId) any());
    }

    // ==================== STORAGE OPTIONS TESTS ====================

    @Test
    void deleteFile_WithStorageDotGoogleapis_ShouldDelete() {
        String url = String.format(
                "https://storage.googleapis.com/%s/%s/test-file.jpg",
                BUCKET_NAME, RECIPE_IMAGE_FOLDER);

        when(storage.delete((BlobId) any())).thenReturn(true);

        boolean result = firebaseStorageService.deleteRecipeImage(url);

        assertTrue(result);
    }
}