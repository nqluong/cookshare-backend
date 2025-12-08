package com.backend.cookshare.system.dto.request;

import lombok.*;
import lombok.experimental.FieldDefaults;
import org.checkerframework.checker.units.qual.N;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class NotificationMessage {
    String type; // NEW_REPORT, REPORT_REVIEWED, USER_WARNING, etc.
    String title;
    String message;
    Map<String, Object> data;
    LocalDateTime timestamp;
}
