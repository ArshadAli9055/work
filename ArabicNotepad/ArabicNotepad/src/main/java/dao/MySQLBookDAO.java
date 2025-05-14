package dao;

import dto.Book;
import dto.Page;
import config.DBConfig;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class MySQLBookDAO implements BookDAO {

    private Connection connection;
    private static final Logger logger = LoggerFactory.getLogger(MySQLBookDAO.class);

    public MySQLBookDAO(DBConfig dbConfig) {
        boolean result = connect(dbConfig);
        if (!result) {
            int count = 0;
            while (count < 3) {
                result = connect(dbConfig);
                count++;
                if (!result) {
                    break;
                }
            }
        }
    }

    @Override
    public boolean isDatabaseConnected() {
        boolean isConnected = false;
        String testQuery = "SELECT 1";

        try (PreparedStatement pstmt = connection.prepareStatement(testQuery); ResultSet rs = pstmt.executeQuery()) {
            if (rs.next()) {
                isConnected = true;
            }
        } catch (SQLException e) {
            logger.warn("Database connection is not active.", e);
        }

        return isConnected;
    }

    @Override
    public boolean connect(DBConfig dbConfig) {
        boolean result = true;
        try {
            String url = dbConfig.getProperty("url");
            String user = dbConfig.getProperty("username");
            String password = dbConfig.getProperty("password");
            connection = DriverManager.getConnection(url, user, password);
            logger.info("Successfully connected to the database.");
        } catch (SQLException e) {
            logger.error("Failed to connect to the database", e);
            throw new RuntimeException("Failed to connect to the database", e);
        }
        return result;
    }

    @Override
    public boolean addBook(Book book, boolean isDbDown) {
        String sql = "INSERT INTO book (title, hash, idauthor) VALUES (?, ?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, book.getTitle());
            pstmt.setString(2, book.getHash());
            pstmt.setString(3, book.getIdauthor());
            pstmt.executeUpdate();

            try (ResultSet rs = pstmt.getGeneratedKeys()) {
                if (rs.next()) {
                    int generatedId = rs.getInt(1);
                    book.setId(generatedId);
                } else {
                    logger.error("Failed to retrieve generated book ID for: {}", book.getTitle());
                    return false;
                }
            }

            if (book.getPages() != null && !book.getPages().isEmpty()) {
                for (Page page : book.getPages()) {
                    page.setBookId(book.getId());
                    if (!addPage(book.getId(), page)) {
                        logger.warn("Failed to add page {} for book: {}", page.getPageNumber(), book.getTitle());
                    }
                }
            }
            return true;
        } catch (SQLException e) {
            logger.error("Error adding book: {}", book.getTitle(), e);
            return false;
        }
    }

    @Override
    public List<Book> getAllBooks(String path) {
        List<Book> bookList = new ArrayList<>();
        String sql = "SELECT idbook, title, hash, idauthor FROM book";
        try (PreparedStatement pstmt = connection.prepareStatement(sql); ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                Book book = new Book();
                book.setId(rs.getInt("idbook"));
                book.setTitle(rs.getString("title"));
                book.setHash(rs.getString("hash"));
                book.setIdauthor(rs.getString("idauthor"));
                bookList.add(book);
            }
        } catch (SQLException e) {
            logger.error("Error retrieving all books", e);
        }
        return bookList;
    }

    @Override
    public Book getBookByName(String title) {
        String sql = "SELECT * FROM book WHERE title = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, title);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                Book book = new Book();
                book.setId(rs.getInt("idbook"));
                book.setTitle(rs.getString("title"));
                book.setHash(rs.getString("hash"));
                book.setIdauthor(rs.getString("idauthor"));

                List<Page> pages = getPagesByBookTitle(book.getTitle());
                book.setPages(pages);
                return book;
            }
        } catch (SQLException e) {
            logger.error("Error retrieving book with title: {}", title, e);
        }
        return null;
    }

    @Override
    public boolean updateBook(Book book) {
        String updateSql = "UPDATE book SET title = ?, idauthor = ? WHERE idbook = ?";
        String callUpdatePageSql = "{CALL UpdatePageContent(?, ?)}"; // Call the stored procedure

        try {
           
            try (PreparedStatement pstmtUpdate = connection.prepareStatement(updateSql)) {
                pstmtUpdate.setString(1, book.getTitle());
                pstmtUpdate.setString(2, book.getIdauthor());
                pstmtUpdate.setInt(3, book.getId());

                int rowsAffected = pstmtUpdate.executeUpdate();
                if (rowsAffected > 0) {
                    logger.info("Book updated successfully in DB: {}", book.getTitle());
                } else {
                    logger.warn("No book found with id: {}", book.getId());
                    return false;
                }
            }

            
            for (Page page : book.getPages()) {
                try (PreparedStatement pstmtCallUpdatePage = connection.prepareStatement(callUpdatePageSql)) {
                    pstmtCallUpdatePage.setInt(1, page.getId()); 
                    pstmtCallUpdatePage.setString(2, page.getContent());
                    pstmtCallUpdatePage.execute();
                    logger.info("Updated content of page {} in book '{}'", page.getPageNumber(), book.getTitle());
                } catch (SQLException e) {
                    logger.error("Error updating page with ID: {} for book: {}", page.getId(), book.getTitle(), e);
                    return false;                 }
            }

            return true;

        } catch (SQLException e) {
            logger.error("Error updating book: {}", book.getTitle(), e);
            return false;
        }
    }

    @Override
    public boolean deleteBook(String title) {
        
        deletePagesByBookTitle(title);

        String deleteSql = "DELETE FROM book WHERE title = ?";

        try (PreparedStatement pstmtDelete = connection.prepareStatement(deleteSql)) {
            pstmtDelete.setString(1, title);
            int rowsAffected = pstmtDelete.executeUpdate();
            if (rowsAffected > 0) {
                logger.info("Successfully deleted book with title: {}", title);
                return true;
            } else {
                logger.warn("No book found with title: {}", title);
                return false;
            }
        } catch (SQLException e) {
            logger.error("Error deleting book: {}", title, e);
            return false; // Return false on error
        }
    }

    @Override
    public void deletePagesByBookTitle(String title) {
        String sqlGetBookId = "SELECT idbook FROM book WHERE title = ?";
        String deleteSql = "DELETE FROM book_pages WHERE idbook = ?";

        try (PreparedStatement pstmtGetBookId = connection.prepareStatement(sqlGetBookId)) {
            pstmtGetBookId.setString(1, title);
            ResultSet rsBookId = pstmtGetBookId.executeQuery();

            if (rsBookId.next()) {
                int bookId = rsBookId.getInt("idbook");
            
                try (PreparedStatement pstmtDelete = connection.prepareStatement(deleteSql)) {
                    pstmtDelete.setInt(1, bookId);
                    pstmtDelete.executeUpdate();
                    logger.info("Successfully deleted pages for book title: {}", title);
                }
            } else {
                logger.warn("No book found with title: {}", title);
            }
        } catch (SQLException e) {
            logger.error("Error deleting pages for book title: {}", title, e);
        }
    }

    @Override
    public boolean isHashExists(String hash) {
        String sql = "SELECT COUNT(*) FROM book WHERE hash = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, hash);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            logger.error("Error checking if hash exists: {}", hash, e);
        }
        return false;
    }

    @Override
    public boolean addPage(int bookId, Page page) {
        String sql = "INSERT INTO book_pages (idbook, page_number, content) VALUES (?, ?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, bookId);
            pstmt.setInt(2, page.getPageNumber());
            pstmt.setString(3, page.getContent());
            pstmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            logger.error("Error adding page to book ID: {}", bookId, e);
            return false;
        }
    }

    @Override
    public List<Page> getPagesByBookTitle(String title) {
        List<Page> pageList = new ArrayList<>();
        String sqlGetBookId = "SELECT idbook FROM book WHERE title = ?";
        String sqlGetPages = "SELECT * FROM book_pages WHERE idbook = ? ORDER BY page_number ASC";

        try (PreparedStatement pstmtGetBookId = connection.prepareStatement(sqlGetBookId)) {
            pstmtGetBookId.setString(1, title);
            ResultSet rsBookId = pstmtGetBookId.executeQuery();

            if (rsBookId.next()) {
                int bookId = rsBookId.getInt("idbook");
    
                try (PreparedStatement pstmtGetPages = connection.prepareStatement(sqlGetPages)) {
                    pstmtGetPages.setInt(1, bookId);
                    ResultSet rsPages = pstmtGetPages.executeQuery();

                    while (rsPages.next()) {
                        Page page = new Page();
                        page.setId(rsPages.getInt("idpage"));
                        page.setBookId(rsPages.getInt("idbook"));
                        page.setPageNumber(rsPages.getInt("page_number"));
                        page.setContent(rsPages.getString("content"));
                        pageList.add(page);
                    }
                }
            } else {
                logger.warn("No book found with title: {}", title);
            }
        } catch (SQLException e) {
            logger.error("Error retrieving pages for book title: {}", title, e);
        }

        return pageList;
    }

    @Override
    public List<String> searchBooksByContent(String searchText) {
        List<String> searchResults = new ArrayList<>();
        String sql = "SELECT DISTINCT b.title, bp.content "
                + "FROM book b "
                + "JOIN book_pages bp ON b.idbook = bp.idbook "
                + "WHERE bp.content LIKE ?";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, "%" + searchText + "%");
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                String title = rs.getString("title");
                String content = rs.getString("content");

                // Split the content into sentences
                String[] sentences = content.split("\\. "); // Split sentences by period and space

                for (String sentence : sentences) {
                    if (sentence.toLowerCase().contains(searchText.toLowerCase())) {
                        // Format the result as "Title: <title>, Sentence: <matching sentence>"
                        searchResults.add("Title: " + title + ", Sentence: " + sentence);
                    }
                }
            }
        } catch (SQLException e) {
            logger.error("Error searching for books by content: {}", searchText, e);
        }
        return searchResults;
    }
}
