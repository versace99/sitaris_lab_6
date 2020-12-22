<%@ page contentType="text/html;charset=UTF-8" %>
<%@ page isELIgnored="false" %>

<table>
    <tr>
        <td><a href="${pageContext.request.contextPath}/books.jsp">Книги</a></td>
        <td><a href="${pageContext.request.contextPath}/hw/getusers">Пользователи</a></td>
        <td><a href="${pageContext.request.contextPath}/usersdebug.jsp">Users debug</a></td>
    </tr>
</table>

<div style="margin: 10px 0">
    Debug: Current user is ${empty sesCurUser? 'not set yet': sesCurUser} | ${sesCurOrder} - ${sesOrder}.
</div>