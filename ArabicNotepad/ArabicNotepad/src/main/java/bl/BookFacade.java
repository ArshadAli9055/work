package bl;

import dto.Book;
import java.util.List;
import dto.Page;

public interface BookFacade {
    List<Book> getBookList(String filepath);
    Book getBookByName(String value);
    void insertBook(Book book);
    void updateBook(Book book);
    void deleteBook(String value);
    void importBook(String path);
    boolean exportBook(String title);
    boolean exportBook(Book book);
    String transliterate(String arabictext);
    List<String> searchBooksByContent(String searchText);
    void addPageByBookTitle(String title, Page page);   
    String performAnalysis(Book book, String analysisMethod);
    String analyzeWord(String selectedWord);   
    boolean isDatabaseConnected();
}
