package com.backend.cookshare.admin_report.dto;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RecipeContentAnalysisDTO {
     Double avgCookTime;
     Double avgPrepTime;
     Double avgTotalTime;
     Double avgIngredientCount;
     Double avgStepCount;
     Long recipesWithImage;
     Long recipesWithVideo;
     Double imagePercentage;
     Double videoPercentage;
     Double avgDescriptionLength;
     Double avgInstructionLength;
}
