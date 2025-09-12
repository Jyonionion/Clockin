package com.example.attendance.controller;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import com.example.attendance.dao.AttendanceDAO;
import com.example.attendance.dto.Attendance;
import com.example.attendance.dto.User;

@WebServlet("/attendance")
public class AttendanceServlet extends HttpServlet {
    private final AttendanceDAO attendanceDAO = new AttendanceDAO();

    // 設定ファイルから読み込む値
    private LocalTime START_TIME;
    private LocalTime END_TIME;
    private LocalTime REMINDER_TIME;

    private static final DateTimeFormatter INPUT_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");

    @Override
    public void init() throws ServletException {
        super.init();
        Properties props = new Properties();
        try (InputStream in = getServletContext().getResourceAsStream("/WEB-INF/config.properties")) {
            if (in == null) throw new ServletException("config.properties が見つかりません");
            props.load(in);

            START_TIME = LocalTime.parse(props.getProperty("work.start"));
            END_TIME = LocalTime.parse(props.getProperty("work.end"));
            REMINDER_TIME = LocalTime.parse(props.getProperty("reminder.time"));
        } catch (IOException e) {
            throw new ServletException("設定ファイルの読み込みに失敗しました", e);
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        HttpSession session = req.getSession(false);
        User user = (User) session.getAttribute("user");
        if (user == null) {
            resp.sendRedirect("login.jsp");
            return;
        }

        // メッセージ設定
        String message = (String) session.getAttribute("successMessage");
        if (message != null) {
            req.setAttribute("successMessage", message);
            session.removeAttribute("successMessage");
        }
        String error = (String) session.getAttribute("errorMessage");
        if (error != null) {
            req.setAttribute("errorMessage", error);
            session.removeAttribute("errorMessage");
        }

        String action = req.getParameter("action");

        if ("admin".equals(user.getRole())) {
            if ("export_csv".equals(action)) {
                exportCsv(req, resp);
            } else if ("filter".equals(action)) {
                handleAdminFilter(req, resp);
            } else {
                handleAdminDefault(req, resp);
            }
        } else {
            handleEmployee(req, resp, user);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        HttpSession session = req.getSession(false);
        User user = (User) session.getAttribute("user");
        if (user == null) {
            resp.sendRedirect("login.jsp");
            return;
        }

        String action = req.getParameter("action");

        switch (action) {
            case "check_in":
                attendanceDAO.checkIn(user.getUsername());
                session.setAttribute("successMessage", "出勤を記録しました。");
                break;
            case "check_out":
                attendanceDAO.checkOut(user.getUsername());
                session.setAttribute("successMessage", "退勤を記録しました。");
                break;
            case "add_manual":
                if ("admin".equals(user.getRole())) handleAddManual(req, session);
                break;
            case "update_manual":
                if ("admin".equals(user.getRole())) handleUpdateManual(req, session);
                break;
            case "delete_manual":
                if ("admin".equals(user.getRole())) handleDeleteManual(req, session);
                break;
        }

        if ("admin".equals(user.getRole())) {
            resp.sendRedirect("attendance?action=filter" +
                    "&filterUserId=" + (req.getParameter("filterUserId") != null ? req.getParameter("filterUserId") : "") +
                    "&startDate=" + (req.getParameter("startDate") != null ? req.getParameter("startDate") : "") +
                    "&endDate=" + (req.getParameter("endDate") != null ? req.getParameter("endDate") : ""));
        } else {
            resp.sendRedirect("attendance");
        }
    }

    private void handleEmployee(HttpServletRequest req, HttpServletResponse resp, User user) throws ServletException, IOException {
        List<Attendance> records = attendanceDAO.findByUserId(user.getUsername());
        req.setAttribute("attendanceRecords", records);

        // ★ 残業時間の月合計を計算
        long monthlyOvertimeMinutes = records.stream()
                .mapToLong(Attendance::getOvertimeMinutes)
                .sum();
        req.setAttribute("monthlyOvertimeHours", String.format("%.1f", monthlyOvertimeMinutes / 60.0));

        // 打刻忘れチェック
        boolean hasCheckedInToday = records.stream().anyMatch(att ->
                att.getCheckInTime() != null &&
                        att.getCheckInTime().toLocalDate().isEqual(LocalDate.now())
        );

        if (LocalTime.now().isAfter(REMINDER_TIME) && !hasCheckedInToday) {
            req.setAttribute("notificationMessage", "⚠ 本日の出勤打刻がまだです！");
        }

        RequestDispatcher rd = req.getRequestDispatcher("/jsp/employee_menu.jsp");
        rd.forward(req, resp);
    }

    // --- Admin関連メソッド（省略せずに維持） ---
    private void handleAdminFilter(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String filterUserId = req.getParameter("filterUserId");
        String startDateStr = req.getParameter("startDate");
        String endDateStr = req.getParameter("endDate");
        LocalDate startDate = null;
        LocalDate endDate = null;
        try {
            if (startDateStr != null && !startDateStr.isEmpty()) startDate = LocalDate.parse(startDateStr);
            if (endDateStr != null && !endDateStr.isEmpty()) endDate = LocalDate.parse(endDateStr);
        } catch (DateTimeParseException e) {
            req.setAttribute("errorMessage", "日付の形式が不正です。");
        }

        List<Attendance> filteredRecords = attendanceDAO.findFilteredRecords(filterUserId, startDate, endDate);
        req.setAttribute("allAttendanceRecords", filteredRecords);

        Map<String, Long> totalHoursByUser = filteredRecords.stream()
                .collect(Collectors.groupingBy(Attendance::getUserId,
                        Collectors.summingLong(att -> {
                            if (att.getCheckInTime() != null && att.getCheckOutTime() != null) {
                                return java.time.temporal.ChronoUnit.HOURS.between(att.getCheckInTime(), att.getCheckOutTime());
                            }
                            return 0L;
                        })));

        Map<String, Long> overtimeHoursByUser = filteredRecords.stream()
                .collect(Collectors.groupingBy(Attendance::getUserId,
                        Collectors.summingLong(Attendance::getOvertimeMinutes)));

        req.setAttribute("totalHoursByUser", totalHoursByUser);
        req.setAttribute("overtimeHoursByUser", overtimeHoursByUser);
        req.setAttribute("monthlyWorkingHours", attendanceDAO.getMonthlyWorkingHours(filterUserId));
        req.setAttribute("monthlyCheckInCounts", attendanceDAO.getMonthlyCheckInCounts(filterUserId));

        req.getRequestDispatcher("/jsp/admin_menu.jsp").forward(req, resp);
    }

    private void handleAdminDefault(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.setAttribute("allAttendanceRecords", attendanceDAO.findAll());
        req.setAttribute("totalHoursByUser", null);
        req.getRequestDispatcher("/jsp/admin_menu.jsp").forward(req, resp);
    }

    private void handleAddManual(HttpServletRequest req, HttpSession session) {
        try {
            String userId = req.getParameter("userId");
            LocalDateTime checkIn = LocalDateTime.parse(req.getParameter("checkInTime"), INPUT_FORMATTER);
            LocalDateTime checkOut = req.getParameter("checkOutTime") != null && !req.getParameter("checkOutTime").isEmpty()
                    ? LocalDateTime.parse(req.getParameter("checkOutTime"), INPUT_FORMATTER) : null;
            attendanceDAO.addManualAttendance(userId, checkIn, checkOut);
            session.setAttribute("successMessage", "勤怠記録を手動で追加しました。");
        } catch (Exception e) {
            session.setAttribute("errorMessage", "日付/時刻の形式が不正です。");
        }
    }

    private void handleUpdateManual(HttpServletRequest req, HttpSession session) {
        try {
            long id = Long.parseLong(req.getParameter("attendanceId"));
            LocalDateTime checkIn = LocalDateTime.parse(req.getParameter("newCheckInTime"), INPUT_FORMATTER);
            LocalDateTime checkOut = req.getParameter("newCheckOutTime") != null && !req.getParameter("newCheckOutTime").isEmpty()
                    ? LocalDateTime.parse(req.getParameter("newCheckOutTime"), INPUT_FORMATTER) : null;
            boolean success = attendanceDAO.updateManualAttendance(id, checkIn, checkOut);
            session.setAttribute(success ? "successMessage" : "errorMessage",
                    success ? "勤怠記録を更新しました。" : "勤怠記録の更新に失敗しました。");
        } catch (Exception e) {
            session.setAttribute("errorMessage", "日付/時刻の形式が不正です。");
        }
    }

    private void handleDeleteManual(HttpServletRequest req, HttpSession session) {
        try {
            long id = Long.parseLong(req.getParameter("attendanceId"));
            boolean success = attendanceDAO.deleteManualAttendanceById(id);
            session.setAttribute(success ? "successMessage" : "errorMessage",
                    success ? "勤怠記録を削除しました。" : "勤怠記録の削除に失敗しました。");
        } catch (NumberFormatException e) {
            session.setAttribute("errorMessage", "不正なIDです。");
        }
    }

    private void exportCsv(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("text/csv; charset=UTF-8");
        resp.setHeader("Content-Disposition", "attachment; filename=attendance.csv");
        PrintWriter out = resp.getWriter();
        out.println("ユーザーID,出勤時刻,退勤時刻");
        List<Attendance> all = attendanceDAO.findAll();
        for (Attendance att : all) {
            out.println(att.getUserId() + "," + att.getCheckInStr() + "," + att.getCheckOutStr());
        }
        out.flush();
        out.close();
    }
}
