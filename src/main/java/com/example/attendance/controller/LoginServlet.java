package com.example.attendance.controller;

import java.io.IOException;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import com.example.attendance.dao.UserDAO;
import com.example.attendance.dto.User;

@WebServlet("/login")
public class LoginServlet extends HttpServlet {
    private final UserDAO userDAO = new UserDAO();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) 
            throws ServletException, IOException {
        String username = req.getParameter("username");
        String password = req.getParameter("password");
        User user = userDAO.findByUsername(username);

        if (user != null && user.isEnabled() && userDAO.verifyPassword(username, password)) {
            HttpSession session = req.getSession();
            session.setAttribute("user", user);
            session.setAttribute("successMessage", "ログインしました。");

            if ("admin".equals(user.getRole())) {
                RequestDispatcher rd = req.getRequestDispatcher("/jsp/admin_menu.jsp");
                rd.forward(req, resp);
            } else {
                RequestDispatcher rd = req.getRequestDispatcher("/jsp/employee_menu.jsp");
                rd.forward(req, resp);
            }
        } else {
            req.setAttribute("errorMessage", "ユーザーID またはパスワードが不正です。またはアカウントが無効です。");
            RequestDispatcher rd = req.getRequestDispatcher("/login.jsp");
            rd.forward(req, resp);
        }
    }
}
