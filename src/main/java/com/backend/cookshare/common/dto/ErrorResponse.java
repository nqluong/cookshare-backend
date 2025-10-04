package com.backend.cookshare.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ErrorResponse {
    boolean succes = false;
    int code;
    String message;
    String details;
    String path;
    @Builder.Default
    LocalDateTime timestamp = LocalDateTime.now();
    List<ValidationResponse> validationErrors;
}
