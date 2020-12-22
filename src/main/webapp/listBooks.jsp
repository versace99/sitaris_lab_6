
<%@ page contentType="text/html;charset=UTF-8" %>
<%@ page isELIgnored="false" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>

<c:forEach items="${requestScope.bookList}" var="book">
    <tr>
        <td><c:out value="${book.idBook}"></c:out></td>
        <td><c:out value="${book.bookAuthor}"></c:out></td>
        <td><c:out value="${book.nameBook}"></c:out></td>

        <td>
            <a href="#" onclick="jsGetBookDetails(${book.idBook})"><c:out value="${book.ISBNBook}"></c:out></a>
        </td>

        <c:choose>
            <c:when test="${book.bookTaker==sesCurUser}">
                <td>
                    <input type="button" value="Вернуть" onclick="jsChangeTaker(${book.idBook}, 0, '${sesCurUser}')">
                </td>
            </c:when>

            <c:when test="${book.bookTaker==null && not empty sesCurUser}">
                <td>
                    <input type="button" value="Взять" onclick="jsChangeTaker(${book.idBook}, 1, '${sesCurUser}')">
                </td>
            </c:when>

            <c:otherwise>
                <td>
                    ${book.bookTaker}
                </td>
            </c:otherwise>
        </c:choose>

        <td><input type="button" value="Удалить" onclick="jsDeleteBook(${book.idBook})"></td>
    </tr>
</c:forEach>