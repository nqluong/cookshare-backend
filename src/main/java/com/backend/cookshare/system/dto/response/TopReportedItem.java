package com.backend.cookshare.system.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TopReportedItem {
    UUID itemId;
    String itemName;
    Long reportCount;
}
