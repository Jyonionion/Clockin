<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<!DOCTYPE html>
<html lang="ja">
<head>
<meta charset="UTF-8">
<title>管理者メニュー</title>
<link rel="stylesheet" href="${pageContext.request.contextPath}/style.css">
</head>
<body>
<div class="container">
    <h1>管理者メニュー</h1>
    <p>ようこそ, ${user.username}さん (管理者)</p>

    <div class="main-nav">
        <a href="attendance?action=filter">勤怠履歴管理</a>
        <a href="users?action=list">ユーザー管理</a>
        <a href="logout">ログアウト</a>
    </div>

    <c:if test="${not empty sessionScope.successMessage}">
        <p class="success-message">
            <c:out value="${sessionScope.successMessage}" />
        </p>
        <c:remove var="successMessage" scope="session" />
    </c:if>

    <c:if test="${not empty errorMessage}">
        <p class="error-message">
            <c:out value="${errorMessage}" />
        </p>
    </c:if>

    <h2>勤怠履歴フィルタ</h2>
    <form action="attendance" method="get" class="filter-form">
        <input type="hidden" name="action" value="filter">
        <div>
            <label for="filterUserId">ユーザーID:</label>
            <input type="text" id="filterUserId" name="filterUserId"
                   value="<c:out value='${param.filterUserId}'/>">
        </div>
        <div>
            <label for="startDate">開始日:</label>
            <input type="date" id="startDate" name="startDate"
                   value="<c:out value='${param.startDate}'/>">
        </div>
        <div>
            <label for="endDate">終了日:</label>
            <input type="date" id="endDate" name="endDate"
                   value="<c:out value='${param.endDate}'/>">
        </div>
        <button type="submit" class="button">フィルタ</button>
    </form>

    <a href="attendance?action=export_csv&filterUserId=${param.filterUserId}&startDate=${param.startDate}&endDate=${param.endDate}"
       class="button">勤怠履歴を CSV エクスポート</a>

    <h2>詳細勤怠履歴</h2>
    <table>
        <thead>
            <tr>
                <th>従業員ID</th>
                <th>出勤時刻</th>
                <th>退勤時刻</th>
                <th>操作</th>
            </tr>
        </thead>
        <tbody>
        <c:forEach var="att" items="${allAttendanceRecords}">
            <tr>
                <td>${att.userId}</td>
                <td>${att.checkInStr}</td>
                <td>${att.checkOutStr}</td>
                <td>
                    <form action="attendance" method="post"
                          onsubmit="return confirm('本当にこの勤怠記録を削除しますか？');">
                        <input type="hidden" name="action" value="delete_manual">
                        <input type="hidden" name="attendanceId" value="${att.id}">
                        <input type="submit" value="削除" class="button danger">
                    </form>
                </td>
            </tr>
        </c:forEach>
        <c:if test="${empty allAttendanceRecords}">
            <tr>
                <td colspan="4">データがありません。</td>
            </tr>
        </c:if>
        </tbody>
    </table>

    <h2>勤怠記録の手動追加</h2>
    <form action="attendance" method="post">
        <input type="hidden" name="action" value="add_manual">
        <p>
            <label for="manualUserId">ユーザーID:</label>
            <input type="text" id="manualUserId" name="userId" required>
        </p>
        <p>
            <label for="manualCheckInTime">出勤時刻:</label>
            <input type="datetime-local" id="manualCheckInTime" name="checkInTime" required>
        </p>
        <p>
            <label for="manualCheckOutTime">退勤時刻 (任意):</label>
            <input type="datetime-local" id="manualCheckOutTime" name="checkOutTime">
        </p>
        <div class="button-group">
            <input type="submit" value="追加">
        </div>
    </form>
</div>
</body>
</html>
