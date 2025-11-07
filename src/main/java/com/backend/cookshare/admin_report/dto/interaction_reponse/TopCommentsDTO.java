package com.backend.cookshare.admin_report.dto.interaction_reponse;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TopCommentsDTO {
    List<CommentDetailDTO> topComments;
    Integer totalCount;
    LocalDateTime periodStart;
    LocalDateTime periodEnd;
}
