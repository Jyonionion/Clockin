package com.example.attendance.dao;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import com.example.attendance.dto.Attendance;
import com.example.attendance.dto.User;

public class AttendanceDAO {

    private static final List<Attendance> attendanceRecords = new CopyOnWriteArrayList<>();
    private static final AtomicLong idGenerator = new AtomicLong(1); // ID自動採番
    private final UserDAO userDAO = new UserDAO(); // ユーザー存在チェック用

    public void checkIn(String userId) {
        Attendance attendance = new Attendance(userId);
        attendance.setId(idGenerator.getAndIncrement());
        attendance.setCheckInTime(LocalDateTime.now());
        attendanceRecords.add(attendance);
    }

    public void checkOut(String userId) {
        attendanceRecords.stream()
                .filter(att -> userId.equals(att.getUserId()) && att.getCheckOutTime() == null)
                .findFirst()
                .ifPresent(att -> att.setCheckOutTime(LocalDateTime.now()));
    }

    public List<Attendance> findByUserId(String userId) {
        return attendanceRecords.stream()
                .filter(att -> userId.equals(att.getUserId()))
                .collect(Collectors.toList());
    }

    public List<Attendance> findAll() {
        return new ArrayList<>(attendanceRecords);
    }

    public List<Attendance> findFilteredRecords(String userId, LocalDate startDate, LocalDate endDate) {
        return attendanceRecords.stream()
                .filter(att -> (userId == null || userId.isEmpty() || att.getUserId().equals(userId)))
                .filter(att -> (startDate == null || (att.getCheckInTime() != null
                        && !att.getCheckInTime().toLocalDate().isBefore(startDate))))
                .filter(att -> (endDate == null || (att.getCheckInTime() != null
                        && !att.getCheckInTime().toLocalDate().isAfter(endDate))))
                .collect(Collectors.toList());
    }

    public Map<YearMonth, Long> getMonthlyWorkingHours(String userId) {
        return attendanceRecords.stream()
                .filter(att -> userId == null || userId.isEmpty() || att.getUserId().equals(userId))
                .filter(att -> att.getCheckInTime() != null && att.getCheckOutTime() != null)
                .collect(Collectors.groupingBy(
                        att -> YearMonth.from(att.getCheckInTime()),
                        Collectors.summingLong(att -> ChronoUnit.HOURS.between(
                                att.getCheckInTime(), att.getCheckOutTime()))));
    }

    public Map<YearMonth, Long> getMonthlyCheckInCounts(String userId) {
        return attendanceRecords.stream()
                .filter(att -> userId == null || userId.isEmpty() || att.getUserId().equals(userId))
                .filter(att -> att.getCheckInTime() != null)
                .collect(Collectors.groupingBy(
                        att -> YearMonth.from(att.getCheckInTime()),
                        Collectors.counting()));
    }

    /**
     * 手動追加
     * @return true=追加成功、false=ユーザー存在しない場合
     */
    public boolean addManualAttendance(String userId, LocalDateTime checkIn, LocalDateTime checkOut) {
        User user = userDAO.findByUsername(userId);
        if (user == null) return false; // ユーザー存在チェック

        Attendance newRecord = new Attendance(userId);
        newRecord.setId(idGenerator.getAndIncrement());
        newRecord.setCheckInTime(checkIn);
        newRecord.setCheckOutTime(checkOut);
        attendanceRecords.add(newRecord);
        return true;
    }

    public boolean updateManualAttendance(long id, LocalDateTime newCheckIn, LocalDateTime newCheckOut) {
        for (Attendance att : attendanceRecords) {
            if (att.getId() == id) {
                att.setCheckInTime(newCheckIn);
                att.setCheckOutTime(newCheckOut);
                return true;
            }
        }
        return false;
    }

    public boolean deleteManualAttendanceById(long id) {
        return attendanceRecords.removeIf(att -> att.getId() == id);
    }
}
