package com.backend.cookshare.system.service;

import com.backend.cookshare.common.dto.PageResponse;
import com.backend.cookshare.system.dto.response.ReportGroupDetailResponse;
import com.backend.cookshare.system.dto.response.ReportGroupResponse;

import java.util.UUID;


public interface ReportGroupService {

    /**
     * Lấy danh sách phân trang các báo cáo được nhóm với thống kê đã tính toán.
     * Các báo cáo được nhóm theo mục tiêu và làm giàu với:
     * - Điểm trọng số dựa trên mức độ nghiêm trọng
     * - Mức độ ưu tiên (CRITICAL, HIGH, MEDIUM, LOW)
     * - Phân loại theo loại báo cáo
     * - Người báo cáo hàng đầu
     *
     * @param page Số trang (bắt đầu từ 0)
     * @param size Kích thước trang
     * @return Phản hồi phân trang của các báo cáo được nhóm, sắp xếp theo độ ưu tiên
     */
    PageResponse<ReportGroupResponse> getGroupedReports(int page, int size);

    /**
     * Lấy thông tin chi tiết về các báo cáo của một mục tiêu cụ thể.
     * Bao gồm tất cả các báo cáo riêng lẻ và thống kê tổng hợp.
     *
     * @param targetType Loại mục tiêu ("USER" hoặc "RECIPE")
     * @param targetId   ID của mục tiêu
     * @return Phản hồi chi tiết với tất cả báo cáo cho mục tiêu
     * @throws IllegalArgumentException nếu không tìm thấy báo cáo cho mục tiêu
     */
    ReportGroupDetailResponse getGroupDetail(String targetType, UUID targetId);
}
