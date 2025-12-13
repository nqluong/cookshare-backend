package com.backend.cookshare;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;

class CookshareApplicationTest {

    @Test
    void main_ShouldCallSpringApplicationRun() {
        // Mock SpringApplication.run() to prevent actual application startup
        try (MockedStatic<SpringApplication> springApplicationMock = mockStatic(SpringApplication.class)) {
            springApplicationMock.when(() -> SpringApplication.run(
                    eq(CookshareApplication.class),
                    any(String[].class)
            )).thenReturn(null);

            // Act - Call main method
            CookshareApplication.main(new String[]{});

            // Assert - Verify SpringApplication.run was called
            springApplicationMock.verify(
                    () -> SpringApplication.run(eq(CookshareApplication.class), any(String[].class)),
                    times(1)
            );
        }
    }

    @Test
    void main_WithArguments_ShouldPassArgumentsToSpringApplication() {
        String[] args = {"--server.port=8081", "--spring.profiles.active=test"};

        try (MockedStatic<SpringApplication> springApplicationMock = mockStatic(SpringApplication.class)) {
            springApplicationMock.when(() -> SpringApplication.run(
                    eq(CookshareApplication.class),
                    eq(args)
            )).thenReturn(null);

            // Act
            CookshareApplication.main(args);

            // Assert
            springApplicationMock.verify(
                    () -> SpringApplication.run(eq(CookshareApplication.class), eq(args)),
                    times(1)
            );
        }
    }

    @Test
    void main_ShouldExist() {
        // Verify the main method exists and is accessible
        assertDoesNotThrow(() -> {
            CookshareApplication.class.getDeclaredMethod("main", String[].class);
        });
    }

    @Test
    void application_ShouldHaveSpringBootAnnotation() {
        // Verify that the class has @SpringBootApplication annotation
        assertTrue(CookshareApplication.class.isAnnotationPresent(
                org.springframework.boot.autoconfigure.SpringBootApplication.class
        ));
    }

    @Test
    void main_ShouldHaveCorrectSignature() {
        // This test ensures the main method signature is correct
        assertDoesNotThrow(() -> {
            var method = CookshareApplication.class.getMethod("main", String[].class);
            assertNotNull(method);
            assertEquals("main", method.getName());
            assertTrue(java.lang.reflect.Modifier.isStatic(method.getModifiers()));
            assertTrue(java.lang.reflect.Modifier.isPublic(method.getModifiers()));
            assertEquals(void.class, method.getReturnType());
        });
    }

    @Test
    void application_ShouldBePublicClass() {
        // Verify the class is public
        assertTrue(java.lang.reflect.Modifier.isPublic(
                CookshareApplication.class.getModifiers()
        ));
    }

    @Test
    void application_ShouldHavePublicConstructor() {
        // Verify that the class can be instantiated
        assertDoesNotThrow(() -> {
            CookshareApplication application = new CookshareApplication();
            assertNotNull(application);
        });
    }

    @Test
    void springBootApplication_ShouldHaveCorrectConfiguration() {
        // Verify @SpringBootApplication annotation configuration
        var annotation = CookshareApplication.class.getAnnotation(
                org.springframework.boot.autoconfigure.SpringBootApplication.class
        );

        assertNotNull(annotation);
        // @SpringBootApplication is a combination of:
        // @Configuration, @EnableAutoConfiguration, and @ComponentScan
    }

    @Test
    void constructor_ShouldCreateInstanceSuccessfully() {
        // Test constructor execution
        CookshareApplication application = new CookshareApplication();
        assertNotNull(application);
        assertInstanceOf(CookshareApplication.class, application);
    }

    @Test
    void main_ShouldAcceptStringArrayParameter() {
        // Verify main method accepts String[] parameter
        assertDoesNotThrow(() -> {
            var method = CookshareApplication.class.getMethod("main", String[].class);
            assertEquals(1, method.getParameterCount());
            assertEquals(String[].class, method.getParameterTypes()[0]);
        });
    }
}