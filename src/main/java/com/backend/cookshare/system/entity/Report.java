package com.backend.cookshare.system.entity;
import com.backend.cookshare.system.enums.ReportActionType;
import com.backend.cookshare.system.enums.ReportStatus;
import com.backend.cookshare.system.enums.ReportType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.GenericGenerator;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "reports")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Report {

    @Id
    @GeneratedValue(generator = "uuid2")
    @GenericGenerator(name = "uuid2", strategy = "uuid2")
    @Column(name = "report_id", columnDefinition = "uuid")
    private UUID reportId;

    @Column(name = "reporter_id", nullable = false, columnDefinition = "uuid")
    private UUID reporterId;

    @Column(name = "reported_id", columnDefinition = "uuid")
    private UUID reportedId;

    @Column(name = "recipe_id", columnDefinition = "uuid")
    private UUID recipeId;

    @Enumerated(EnumType.STRING)
    @Column(name = "report_type", nullable = false, length = 50)
    private ReportType reportType;

    @Column(name = "reason", length = 255)
    private String reason;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 50)
    @Builder.Default
    private ReportStatus status = ReportStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(name = "action_taken")
    private ReportActionType actionTaken;

    @Column(name = "action_description", columnDefinition = "TEXT")
    private String actionDescription;

    @Column(name = "reporters_notified")
    private Boolean reportersNotified = false;

    @Column(name = "admin_note", columnDefinition = "TEXT")
    private String adminNote;

    @Column(name = "reviewed_by", columnDefinition = "uuid")
    private UUID reviewedBy;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
