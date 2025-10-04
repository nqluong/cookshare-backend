package com.backend.cookshare.common.dto;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ValidationResponse {
    String field;
    String message;
    Object rejectedValue;
}
