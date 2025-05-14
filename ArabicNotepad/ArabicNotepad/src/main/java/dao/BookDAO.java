package dao;

import dto.Book;
import config.DBConfig;
import java.util.List;
import dto.Page;

public interface BookDAO {

    List<Book> getAllBooks(String path);

    Book getBookByName(String name);

    public boolean addBook(Book book, boolean isDbDown);

    boolean updateBook(Book book);

    boolean deleteBook(String title);

    boolean isHashExists(String hash);

    boolean connect(DBConfig dbConfig);

    public boolean isDatabaseConnected();

    public List<String> searchBooksByContent(String searchText);
            
    public boolean addPage(int bookId, Page page);

    public List<Page> getPagesByBookTitle(String title);

    public void deletePagesByBookTitle(String title);
}
