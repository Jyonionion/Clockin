package com.example.attendance.dto;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Attendance {
    private String userId;
    private LocalDateTime checkInTime;
    private LocalDateTime checkOutTime;

    // 共通フォーマッタ
    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm");

    public Attendance(String userId) { this.userId = userId; }

    public String getUserId() { return userId; }

    public LocalDateTime getCheckInTime() { return checkInTime; }
    public void setCheckInTime(LocalDateTime checkInTime) { this.checkInTime = checkInTime; }

    public LocalDateTime getCheckOutTime() { return checkOutTime; }
    public void setCheckOutTime(LocalDateTime checkOutTime) { this.checkOutTime = checkOutTime; }

    // ★ 表示用（ここでフォーマット）
    public String getCheckInStr() {
        return checkInTime != null ? checkInTime.format(FORMATTER) : "";
    }
    public String getCheckOutStr() {
        return checkOutTime != null ? checkOutTime.format(FORMATTER) : "";
    }
}
