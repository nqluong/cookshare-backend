package com.backend.cookshare.system.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;
import org.checkerframework.checker.units.qual.N;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TargetStatisticsResponse {
    // Số lượng targets bị report
    Long totalReportedRecipes;
    Long totalReportedUsers;

    // Targets vượt ngưỡng auto-moderation
    Long recipesExceedingThreshold;
    Long usersExceedingThreshold;

    // Targets đã bị auto-action
    Long autoUnpublishedRecipes;
    Long autoDisabledUsers;

    // Avg reports per target
    Double avgReportsPerRecipe;
    Double avgReportsPerUser;

    // Priority distribution
    Long criticalPriorityTargets;
    Long highPriorityTargets;
    Long mediumPriorityTargets;
    Long lowPriorityTargets;
}
