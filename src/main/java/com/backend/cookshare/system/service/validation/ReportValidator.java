package com.backend.cookshare.system.service.validation;

import com.backend.cookshare.authentication.repository.UserRepository;
import com.backend.cookshare.common.exception.CustomException;
import com.backend.cookshare.common.exception.ErrorCode;
import com.backend.cookshare.recipe_management.repository.RecipeRepository;
import com.backend.cookshare.system.dto.request.CreateReportRequest;
import com.backend.cookshare.system.repository.ReportRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ReportValidator {
    private final UserRepository userRepository;
    private final RecipeRepository recipeRepository;
    private final ReportRepository reportRepository;

    public void validateCreateRequest(CreateReportRequest request, UUID reporterId) {
        validateTarget(request, reporterId);
        validateNoDuplicate(request, reporterId);
        validateEntitiesExist(request);
    }

    private void validateTarget(CreateReportRequest request, UUID reporterId) {
        if (request.getReportedId() == null && request.getRecipeId() == null) {
            throw new CustomException(ErrorCode.REPORT_TARGET_REQUIRED);
        }

        if (request.getReportedId() != null && request.getReportedId().equals(reporterId)) {
            throw new CustomException(ErrorCode.CANNOT_REPORT_YOURSELF);
        }
    }

    private void validateNoDuplicate(CreateReportRequest request, UUID reporterId) {
        if (reportRepository.existsPendingReportByReporter(
                reporterId,
                request.getReportedId(),
                request.getRecipeId())) {
            throw new CustomException(ErrorCode.REPORT_ALREADY_EXISTS);
        }
    }

    private void validateEntitiesExist(CreateReportRequest request) {
        if (request.getReportedId() != null) {
            userRepository.findById(request.getReportedId())
                    .orElseThrow(() -> new CustomException(ErrorCode.REPORTED_USER_NOT_FOUND));
        }

        if (request.getRecipeId() != null) {
            recipeRepository.findById(request.getRecipeId())
                    .orElseThrow(() -> new CustomException(ErrorCode.REPORTED_RECIPE_NOT_FOUND));
        }
    }
}
