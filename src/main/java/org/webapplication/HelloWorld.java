package org.webapplication;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;


public class HelloWorld extends HttpServlet {

    private static final String DB_DRIVER = "org.h2.Driver";
    private static final String DB_CONNECTION = "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1";
    private static final String DB_USER = "";
    private static final String DB_PASSWORD = "";

    private static final int numBooks = 20;

    private static int numRecords;

    final static Logger logger = LoggerFactory.getLogger(HelloWorld.class);

    public void init() {
        initDb();
    }

    public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("text/html");
        response.setCharacterEncoding("UTF-8");
        request.setCharacterEncoding("UTF-8");

        HttpSession session = request.getSession(true);
        String param = (String) session.getAttribute("sesCurUser");
        if (param == null) {
            session.setAttribute("sesCurUser", "");
        }

        // проверяем и устанавливаем параметр сессии - столбец по которому сортируется вывод книг
        param = (String) session.getAttribute("sesCurOrder");
        if (param == null) {
            session.setAttribute("sesCurOrder", "BookAuthor");
            logger.debug("HelloWorld class: session param sesCurOrder is null");
        } else {
            logger.debug("HelloWorld class: session param sesCurOrder is not null: "+((String) session.getAttribute("sesCurOrder")));
        }

        // проверяем и устанавливаем параметр сессии - порядок отображения столбца авторов или наименований
        param = (String) session.getAttribute("sesOrder");
        if (param == null) {
            session.setAttribute("sesOrder", "ASC");
        }

        if (request.getPathInfo().equals("/initdb")) {
            //инициализация бд
            initDb();
            response.sendRedirect(request.getContextPath() + "/index.jsp");

        } else if (request.getPathInfo().equals("/getbooks")) {
            //вывод списка книг
            int page = 1;
            int recPerPage = 5;
            String Order = (String) session.getAttribute("sesOrder");
            String CurOrder = (String) session.getAttribute("sesCurOrder");
            if (request.getParameter("page") != null) {
                page = Integer.parseInt(request.getParameter("page"));
            }
            if (request.getParameter("recPerPage") != null) {
                recPerPage = Integer.parseInt(request.getParameter("recPerPage"));
            }
            // добавляем атрибут со списком пользователей
            request.setAttribute("bookList", getBooks((page - 1) * recPerPage, recPerPage, CurOrder, Order));
            RequestDispatcher rd = getServletContext().getRequestDispatcher("/listBooks.jsp");
            rd.forward(request, response);

        } else if (request.getPathInfo().equals("/delbook")) {
            //удаляем книгу
            int idDelBook;
            if (request.getParameter("idDelBook") != null) {
                idDelBook = Integer.parseInt(request.getParameter("idDelBook"));
                delBooks(idDelBook);
            } else {
                logger.error("!!! Exec /delbook without a parameter!");
            }

        } else if (request.getPathInfo().equals("/addbook")) {
            // добавляем книгу в бд
            String newISBN;
            String newAuthor;
            String newName;

            if (request.getParameter("newISBN") != null && request.getParameter("newAuthor") != null && request.getParameter("newName") != null) {
                newISBN = request.getParameter("newISBN");
                newAuthor = request.getParameter("newAuthor");
                newName = request.getParameter("newName");
                if (!newISBN.isEmpty() && !newAuthor.isEmpty() && !newName.isEmpty()) {
                    response.setContentType("application/json");
                    response.getWriter().write(addBook(newISBN, newAuthor, newName));
                } else {
                    logger.error("!!! addBook received at least one empty string as a parameter!");
                }
            } else {
                logger.error("!!! Exec addBook without a parameter!");
            }

        } else if (request.getPathInfo().equals("/getusers")) {
            //вызов jsp с шаблонами
            request.setAttribute("userList", getUsers());
            RequestDispatcher rd = getServletContext().getRequestDispatcher("/users.jsp");
            rd.forward(request, response);

        } else if (request.getPathInfo().equals("/deluser")) {
            String userIdParam = request.getParameter("id");
            if (userIdParam != null && !userIdParam.isEmpty()) {
                delUsers(Integer.parseInt(userIdParam));
            } else {
                logger.error("Exec /deluser without a parameter!");
            }
            response.sendRedirect(request.getContextPath() + "/hw/getusers");

        } else if (request.getPathInfo().equals("/adduser")) {
            String addUser = "";
            String addPass = "";
            if (request.getParameter("addUser") != null) {
                addUser = request.getParameter("addUser");
            }
            if (request.getParameter("addPass") != null) {
                addPass = request.getParameter("addPass");
            }
            if (addUser != null && !addUser.isEmpty() && addPass != null) {
                //addUser(addUser, addPass);
                response.setContentType("application/json");
                response.getWriter().write(addUser(addUser, addPass));
            } else {
                logger.error("!!! Error: user or pass is null or user is empty string");
            }

        } else if (request.getPathInfo().equals("/getuserdetails")) {
            int userId = 0;
            if (request.getParameter("userid") != null) {
                userId = Integer.parseInt(request.getParameter("userid"));
                logger.debug("/getuserdetails?userid=" + Integer.toString(userId));
            }
            if (userId != 0) {
                response.setContentType("application/json");
                response.getWriter().write(getUserDetails(userId));
            } else {
                logger.error("Error: have not received user id (userId = 0");
            }

        } else if (request.getPathInfo().equals("/getbookdetails")) {
            int bookId = 0;
            if (request.getParameter("bookid") != null) {
                bookId = Integer.parseInt(request.getParameter("bookid"));
                logger.debug("/getbookdetails?bookid=" + Integer.toString(bookId));
            }
            if (bookId != 0) {
                response.setContentType("application/json");
                response.getWriter().write(getBookDetails(bookId));
            } else {
                logger.error("Error: have not received book id (bookId = 0");
            }

        } else if (request.getPathInfo().equals("/updateuserpass")) {
            //апдейтим пароль пользователя
            int userId = 0;
            String newPass = "";

            if (request.getParameter("userid") != null) {
                userId = Integer.parseInt(request.getParameter("userid"));
            }
            if (request.getParameter("newpass") != null) {
                newPass = request.getParameter("newpass");
            }
            logger.debug("Changing password for userid=" + userId + ". New password is '" + newPass + "'.");

            //if (userId != 0 && newPass != "") {
            if (userId != 0 && !newPass.equals("")) {
                response.setContentType("application/json");
                response.getWriter().write(updateUserPass(userId, newPass));
            } else {
                logger.error("Error: have not received user id (userId = 0");
            }

        } else if (request.getPathInfo().equals("/updatebookdetails")) {
            // апдейтим детали (автор, название) книги
            // задаем начальные значения переменных - параметров функции,
            int bookId = 0;
            String newAuthor = "";
            String newName = "";

            if (request.getParameter("bookid")!=null && request.getParameter("newAuthor")!=null && request.getParameter("newName")!=null) {
                bookId = Integer.parseInt(request.getParameter("bookid"));
                newAuthor = request.getParameter("newAuthor");
                newName = request.getParameter("newName");
                if (bookId!=0 && !newAuthor.isEmpty() && !newName.isEmpty()) {
                    response.setContentType("application/json");
                    response.getWriter().write(updateBookDetails(bookId, newAuthor, newName));
                } else {
                    logger.error("Error: received empty parameters for /updatebookdetails.");
                }
            } else {
                logger.error("Error: not enough parameters for /updatebookdetails.");
            }

        } else if (request.getPathInfo().equals("/setuser")) {
            // определяем переменную текущего пользователя
            if (request.getParameter("username")!=null) {
                session.setAttribute("sesCurUser", request.getParameter("username"));
                logger.debug("Set curUser to '" + session.getAttribute("sesCurUser") + "'.");
                RequestDispatcher rd = getServletContext().getRequestDispatcher("/usersdebug.jsp");
                rd.forward(request, response);
            }

        } else if (request.getPathInfo().equals("/changetaker")) {
            int bookid;
            int action;
            String username;
            if (request.getParameter("bookid") != null && request.getParameter("action") != null && request.getParameter("username")!=null) {
                bookid = Integer.parseInt(request.getParameter("bookid"));
                action = Integer.parseInt(request.getParameter("action"));
                username = request.getParameter("username");
                logger.debug("Changing taker, parameters: bookid/action/username"+bookid+"/"+action+"/"+username);
                switch (action) {
                    case 0: changeTaker(bookid, action, username);
                            break;
                    case 1: changeTaker(bookid, action, username);
                            break;
                    default:    logger.error("Error: wrong action in /changetaker. Should be 0 or 1.");
                                break;
                }
            } else {
                logger.error("Error: undefined parameters in /changetaker");
            }

        } else if (request.getPathInfo().equals("/setauthororder")) {
            if (((String) session.getAttribute("sesCurOrder")).equals("BookAuthor")) {
                logger.debug("Sorting column is Author, sesOrder is '" + ((String) session.getAttribute("sesOrder")) + "'");
                switch ((String) session.getAttribute("sesOrder")) {
                    case "ASC":
                        session.setAttribute("sesOrder", "DESC");
                        logger.debug("Set column = Author sesOrder to DESC.");
                        break;
                    case "DESC":
                        session.setAttribute("sesOrder", "ASC");
                        logger.debug("Set column = Author sesOrder to ASC.");
                        break;
                    default:
                        session.setAttribute("sesOrder", "ASC");
                        logger.debug("Set column=Author sesAuthorOrder to ASC.");
                        break;
                }
            } else {
                session.setAttribute("sesCurOrder", "BookAuthor");
                session.setAttribute("sesOrder", "ASC");
            }

        } else if (request.getPathInfo().equals("/setnameorder")) {
            if (((String) session.getAttribute("sesCurOrder")).equals("BookName")) {
                logger.debug("Sorting column is Name, sesOrder is '" + ((String) session.getAttribute("sesNameOrder")) + "'");
                switch ((String) session.getAttribute("sesOrder")) {
                    case "ASC":
                        session.setAttribute("sesOrder", "DESC");
                        logger.debug("Set column=Name sesOrder to DESC.");
                        break;
                    case "DESC":
                        session.setAttribute("sesOrder", "ASC");
                        logger.debug("Set column=Name sesOrder to ASC.");
                        break;
                    default:
                        session.setAttribute("sesOrder", "ASC");
                        logger.debug("Dummy has worked! Set column=Name sesOrder to ASC.");
                        break;
                }
            } else {
                session.setAttribute("sesCurOrder", "BookName");
                session.setAttribute("sesOrder", "ASC");
            }
        }
    }

    private void initDb() {
        Connection con = null;
        Statement stmt = null;
        int min = 345;
        int max = 970;
        String strISBN;
        int initBookTaker;
        String initSQLstBookTaker;
        String strSQLstmt;

        try {
            con = getConnection();
            logger.debug("DB created. Start of filling.");

            con.setAutoCommit(false);
            stmt = con.createStatement();

            stmt.executeUpdate("CREATE TABLE users(id INT NOT NULL AUTO_INCREMENT primary key, name varchar(255) NOT NULL UNIQUE, password varchar(255))");
            stmt.executeUpdate("INSERT INTO users (name, password) VALUES ('Иванов','xxx')");
            stmt.executeUpdate("INSERT INTO users (name, password) VALUES ('Петров','xxx')");
            stmt.executeUpdate("INSERT INTO users (name, password) VALUES ('Сидоров','xxx')");
            logger.debug("Finished initial filling of users");

            stmt.executeUpdate("CREATE TABLE books(id INT NOT NULL AUTO_INCREMENT primary key, isbn varchar(17) " +
                    "NOT NULL, author varchar(50) NOT NULL, name varchar(50) NOT NULL, takerid int REFERENCES users(id) ON DELETE SET NULL)");
            for (int i=0; i<numBooks; i++) {
                //init vars at the beginning of every iteration
                // Starting ISBN string
                strISBN = "-3-16-148410-0";
                // Starting SQL statement
                strSQLstmt = "INSERT INTO books(ISBN, author, name";
                //Defining the random part of the ISBN
                int partISBN = ThreadLocalRandom.current().nextInt(min, max + 1);
                //Concat
                strISBN = Integer.toString(partISBN) + strISBN;

                //Defining temporarily owner of a book
                initBookTaker = ThreadLocalRandom.current().nextInt(0, 4);
                if (initBookTaker == 0) {
                    initSQLstBookTaker = "";
                    strSQLstmt = strSQLstmt + ") VALUES('" + strISBN + "', 'А.С. Пушкин', 'Евгений Онегин')";
                } else {
                    initSQLstBookTaker = Integer.toString(initBookTaker);
                    strSQLstmt = strSQLstmt + ", takerid) VALUES('" + strISBN + "', 'А.С. Пушкин', 'Евгений Онегин', " + initSQLstBookTaker + ")";
                }

                //Learn how many records returns query
                ResultSet rs = stmt.executeQuery("SELECT B.id AS BookID, B.ISBN AS BookISBN, B.author AS BookAuthor, B.name AS BookName, " +
                        "U.name AS UserName FROM books B JOIN users U ON U.id = B.takerid ORDER BY BookISBN");
                if (rs.next()) {
                    numRecords = rs.getInt(1);
                }

                stmt.executeUpdate(strSQLstmt);
                logger.debug(strSQLstmt);
            }

            stmt.close();
            con.commit();
            logger.debug("Records were inserted. End of filling");
        } catch (Exception e) {
            logger.error("Init db error", e);
        } finally {
            closeQuiet(stmt);
            closeQuiet(con);
        }
    }

    private String addUser(String addUser, String addPass) {
        Connection con = null;
        Statement stmt = null;
        int numUsers = 0;
        String res = "{\"Result\":1}";

        try {
            con = getConnection();
            con.setAutoCommit(false);

            stmt = con.createStatement();

            ResultSet rs = stmt.executeQuery("SELECT COUNT(name) FROM users WHERE name='"+addUser+"'");
            rs.first();
            numUsers = rs.getInt(1);
            logger.debug("Number of users '"+addUser+"' in DB: "+String.valueOf(numUsers));

            if (numUsers==0) {
                //пользователи с таким именем отсутствуют, добавляем
                stmt.executeUpdate("INSERT INTO users(name, password) VALUES ('" + addUser + "', '" + addPass + "')");
                logger.debug("Added user with passwd: " + addUser + ":" + addPass);
                stmt.close();
                con.commit();
                res = "{\"Result\":0}";
            } else {
                //пользователи существуют, пропускаем
                logger.debug("User "+addUser+" already exists. Skipping.");
            }
        } catch (Exception e) {
            logger.error("Error adding a user", e);
        } finally {
            closeQuiet(stmt);
            closeQuiet(con);
        }
        return res;
    }

    private String addBook(String newISBN, String newAuthor, String newName) {
        Connection con = null;
        PreparedStatement stmt = null;
        int numBooks = 0;
        String res = "{\"Result\":1}";
        String selectSQL = "SELECT COUNT(ISBN) FROM books WHERE ISBN = ?";
        String insertSQL = "INSERT INTO books(ISBN, author, name) VALUES(?, ?, ?)";

        try {
            con = getConnection();
            con.setAutoCommit(false);
            stmt = con.prepareStatement(selectSQL);
            stmt.setString(1, newISBN);
            ResultSet rs = stmt.executeQuery();
            rs.first();
            numBooks = rs.getInt(1);
            logger.debug("Number of books with ISBN '"+ newISBN +"' in DB: "+String.valueOf(numBooks));

            if (numBooks==0) {
                stmt = con.prepareStatement(insertSQL);
                stmt.setString(1, newISBN);
                stmt.setString(2, newAuthor);
                stmt.setString(3, newName);
                stmt.executeUpdate();
                //stmt.executeUpdate("INSERT INTO users(name, password) VALUES ('" + addUser + "', '" + addPass + "')");
                logger.debug("Added book with ISBN:" + newISBN+ ", author:" + newAuthor + ", name: " + newName);
                stmt.close();
                con.commit();
                res = "{\"Result\":0}";
            } else {
                logger.debug("Book ISBN: "+ newISBN +" already exists. Skipping.");
            }

        } catch (Exception e) {
            logger.error("Error adding a book", e);
        } finally {
            closeQuiet(stmt);
            closeQuiet(con);
        }

        return res;
    }

    private String getUserDetails(int userId) {
        Connection con = null;
        Statement stmt = null;
        String res = "{\"Result\":0}";

        try {
            con = getConnection();
            //con.setAutoCommit(false);

            stmt = con.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT name AS username, password AS pass FROM users WHERE id="+userId);
            rs.first();
            res = "{\"user\":\""+rs.getString("username")+"\", \"pass\":\""+rs.getString("pass")+"\", \"Result\":1}";
            logger.debug("JSON user details:"+res);
            stmt.close();
        } catch (Exception e) {
                logger.error("Error getting user details", e);
        } finally {
                closeQuiet(stmt);
                closeQuiet(con);
        }
        return res;
    }

    public String getBookDetails(int bookId) {
        Connection con = null;
        PreparedStatement stmt = null;
        String res = "{\"Result\":0}";
        String selectSQL = "SELECT ISBN AS isbn, author AS author, name AS name FROM books WHERE id = ?";

        try {
            con = getConnection();
            stmt = con.prepareStatement(selectSQL);
            stmt.setInt(1, bookId);
            ResultSet rs = stmt.executeQuery();
            rs.first();
            res = "{\"ISBN\":\""+rs.getString("isbn")+"\", \"author\":\""+rs.getString("author")+"\", \"name\":\"" + rs.getString("name") + "\",\"Result\":1}";
            logger.debug("JSON book details:"+res);
            stmt.close();
        } catch (Exception e) {
            logger.error("Error getting book details", e);
        } finally {
            closeQuiet(stmt);
            closeQuiet(con);
        }

        return res;

    }

    private String updateUserPass(int userId, String newPass) {
        Connection con = null;
        Statement stmt = null;
        String res = "{\"Result\":0}";

        try {
            con = getConnection();
            con.setAutoCommit(false);

            stmt = con.createStatement();
            stmt.executeUpdate("UPDATE users SET password='"+newPass+"' WHERE id="+userId);
            res = "{\"Result\":1}";
            logger.debug("Password for userid="+userId+" has been successfully changed.");

            stmt.close();
            con.commit();

        } catch (Exception e) {
            logger.error("Error updating password", e);
        } finally {
            closeQuiet(stmt);
            closeQuiet(con);
        }

        return res;
    }

    public String updateBookDetails(int bookId, String newAuthor, String newName) {
        Connection con = null;
        PreparedStatement stmt = null;
        String res = "{\"Result\":0}";
        String updateSQL = "UPDATE books SET author = ?, name = ? WHERE id = ?";

        try {
            con = getConnection();
            stmt = con.prepareStatement(updateSQL);
            stmt.setString(1, newAuthor);
            stmt.setString(2, newName);
            stmt.setInt(3, bookId);
            stmt.executeUpdate();
            res = "{\"Result\":1}";
            logger.debug("Book id=" + bookId + " details have been successfully changed.");
            stmt.close();
            con.commit();
        } catch (Exception e) {
            logger.error("Error updating book details", e);
        } finally {
            closeQuiet(stmt);
            closeQuiet(con);
        }

        return res;

    }

    public void changeTaker(int bookId, int action, String username) {
        Connection con = null;
        Statement stmt = null;

        try {
            con = getConnection();
            con.setAutoCommit(false);

            stmt = con.createStatement();

            if (action == 0) {
                stmt.executeUpdate("UPDATE books SET takerid=NULL WHERE id=" + bookId);
            } else if (action == 1) {
                stmt.executeUpdate("UPDATE books SET takerid=(SELECT id FROM users WHERE name = '" + username + "') WHERE id=" + bookId);
            }

            stmt.close();
            con.commit();

        } catch (Exception e) {
            logger.error("Error changing book owner", e);
        } finally {
            closeQuiet(stmt);
            closeQuiet(con);
        }
    }

    private List<librarianUser> getUsers() {
        Connection con = null;
        Statement stmt = null;

        List<librarianUser> users = new ArrayList<>();

        try {
            con = getConnection();

            con.setAutoCommit(false);
            stmt = con.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT id AS UserID, name AS UserName, password AS UserPassword FROM users ORDER by UserName");

            while (rs.next()) {
                librarianUser user = new librarianUser();
                user.setUserId(rs.getInt("UserID"));
                user.setUserName(rs.getString("UserName"));
                user.setUserPass(rs.getString("UserPassword"));
                users.add(user);
            }

            stmt.close();
            con.commit();

        } catch (Exception e) {
            logger.error("Get users error", e);
        } finally {
            closeQuiet(stmt);
            closeQuiet(con);
        }

        return users;
    }

    private List<LibrarianBook> getBooks(int offset, int recPerPage, String CurOrder, String Order) {

        if (CurOrder.equals(null) || Order.equals("")) {
            CurOrder = "BookAuthor";
            logger.debug("Warning: empty CurOrder. Set it to BookAuthor.");
        } else {
            logger.debug("Non-empty CurOrder: " + CurOrder);
        }

        if (Order.equals(null) || Order.equals("")) {
            Order = "ASC";
            logger.debug("Warning: empty Order. Set it to ASC.");
        } else {
            logger.debug("Non-empty Order: " + Order);
        }

        Connection con = null;
        PreparedStatement stmt = null;
        String selectSQL = "SELECT B.id AS BookID, B.ISBN AS BookISBN, B.author AS BookAuthor, B.name AS BookName, U.name AS UserName FROM books AS B LEFT JOIN users AS U ON B.takerid = U.id ORDER BY " + CurOrder +" "+ Order +", BookISBN LIMIT ? OFFSET ?";

        List<LibrarianBook> books = new ArrayList<>();
        try {
            con = getConnection();
            con.setAutoCommit(false);

            stmt = con.prepareStatement(selectSQL);
            stmt.setInt(1, recPerPage);
            stmt.setInt(2, offset);

            //stmt = con.createStatement();

            /* ResultSet rs = stmt.executeQuery("SELECT B.id AS BookID, B.ISBN AS BookISBN, B.author AS BookAuthor, B.name AS BookName, U.name " +
                    "AS UserName FROM books AS B LEFT JOIN users AS U ON B.takerid = U.id ORDER BY BookISBN LIMIT " +
                    Integer.toString(recPerPage) + " OFFSET " + Integer.toString(offset)); */

            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                LibrarianBook book = new LibrarianBook();
                book.setIdBook(rs.getInt("BookID"));
                book.setISBNBook(rs.getString("BookISBN"));
                book.setBookAuthor(rs.getString("BookAuthor"));
                book.setNameBook(rs.getString("BookName"));
                book.setBookTaker(rs.getString("UserName"));
                books.add(book);
            }

            stmt.close();
            con.commit();
        } catch (Exception e) {
            logger.error("Get books error", e);
        } finally {
            closeQuiet(stmt);
            closeQuiet(con);
        }
        return books;
    }

    private void delUsers(int idDelUser) {
        Connection con = null;
        Statement stmt = null;

        try {
            con = getConnection();
            con.setAutoCommit(false);

            stmt = con.createStatement();
            stmt.executeUpdate("DELETE FROM users WHERE id= " + Integer.toString(idDelUser));
            logger.debug("Deleted user record with id=" + Integer.toString(idDelUser));
            stmt.close();
            con.commit();
        } catch (Exception e) {
            logger.error("Del users error", e);
        } finally {
            closeQuiet(stmt);
            closeQuiet(con);
        }
    }

    private void delBooks(int idDelBook) {
        Connection con = null;
        Statement stmt = null;

        try {
            con = getConnection();
            con.setAutoCommit(false);

            stmt = con.createStatement();
            stmt.executeUpdate("DELETE FROM books WHERE id= " + Integer.toString(idDelBook));
            logger.debug("Deleted book record with id=" + Integer.toString(idDelBook));
            stmt.close();
            con.commit();
        } catch (Exception e) {
            logger.error("Del books error", e);
        } finally {
            closeQuiet(stmt);
            closeQuiet(con);
        }
    }

    private Connection getConnection() throws ClassNotFoundException, SQLException {
        Class.forName(DB_DRIVER);
        return DriverManager.getConnection(DB_CONNECTION, DB_USER, DB_PASSWORD);
    }

    public void destroy() {
    }

    private void closeQuiet(AutoCloseable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (Exception e) {
                //e.printStackTrace();
            }
        }
    }

}