package com.backend.cookshare.system.service;

import com.backend.cookshare.common.dto.PageResponse;
import com.backend.cookshare.system.dto.response.ReportGroupDetailResponse;
import com.backend.cookshare.system.dto.response.ReportGroupResponse;
import com.backend.cookshare.system.enums.ReportActionType;
import com.backend.cookshare.system.enums.ReportStatus;
import com.backend.cookshare.system.enums.ReportType;

import java.util.UUID;


public interface ReportGroupService {

    /**
     * Lấy danh sách phân trang các báo cáo công thức được nhóm với thống kê đã tính toán.
     * Các báo cáo được nhóm theo công thức và làm giàu với:
     * - Điểm trọng số dựa trên mức độ nghiêm trọng
     * - Mức độ ưu tiên (CRITICAL, HIGH, MEDIUM, LOW)
     * - Phân loại theo loại báo cáo
     * - Người báo cáo hàng đầu
     *
     * @param page Số trang (bắt đầu từ 0)
     * @param size Kích thước trang
     * @param status Lọc theo trạng thái báo cáo (tùy chọn)
     * @param actionType Lọc theo loại hành động admin đã xử lý (tùy chọn)
     * @param reportType Lọc theo loại báo cáo (tùy chọn)
     * @return Phản hồi phân trang của các báo cáo được nhóm, sắp xếp theo độ ưu tiên
     */
    PageResponse<ReportGroupResponse> getGroupedReports(int page, int size, ReportStatus status, ReportActionType actionType, ReportType reportType);

    /**
     * Lấy thông tin chi tiết về các báo cáo của một công thức cụ thể.
     * Bao gồm tất cả các báo cáo riêng lẻ và thống kê tổng hợp.
     *
     * @param recipeId ID của công thức
     * @return Phản hồi chi tiết với tất cả báo cáo cho công thức
     * @throws IllegalArgumentException nếu không tìm thấy báo cáo cho công thức
     */
    ReportGroupDetailResponse getGroupDetail(UUID recipeId);
}
