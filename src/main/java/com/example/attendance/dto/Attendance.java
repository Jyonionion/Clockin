package com.example.attendance.dto;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Attendance {
    private long id;
    private String userId;
    private LocalDateTime checkInTime;
    private LocalDateTime checkOutTime;

    private static final DateTimeFormatter DISPLAY_FORMATTER = 
            DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm");

    public Attendance() {}

    public Attendance(long id, String userId, LocalDateTime checkInTime, LocalDateTime checkOutTime) {
        this.id = id;
        this.userId = userId;
        this.checkInTime = checkInTime;
        this.checkOutTime = checkOutTime;
    }

    public Attendance(String userId) {
        this.userId = userId;
    }

    // getter/setter
    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public LocalDateTime getCheckInTime() { return checkInTime; }
    public void setCheckInTime(LocalDateTime checkInTime) { this.checkInTime = checkInTime; }
    public LocalDateTime getCheckOutTime() { return checkOutTime; }
    public void setCheckOutTime(LocalDateTime checkOutTime) { this.checkOutTime = checkOutTime; }

    // JSP表示用（yyyy/MM/dd HH:mm）
    public String getCheckInStr() {
        return checkInTime != null ? checkInTime.format(DISPLAY_FORMATTER) : "";
    }

    public String getCheckOutStr() {
        return checkOutTime != null ? checkOutTime.format(DISPLAY_FORMATTER) : "";
    }
}
