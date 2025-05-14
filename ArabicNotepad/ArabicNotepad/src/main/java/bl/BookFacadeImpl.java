package bl;

import dao.BookDAO;
import dto.Book;

import java.util.List;
import dto.Page;

public class BookFacadeImpl implements BookFacade {

    private final BookService bookService;

    public BookFacadeImpl(BookDAO bookDAO) {
        this.bookService = new BookService(bookDAO);
    }

    @Override
    public void importBook(String path) {
        bookService.importBook(path);
    }

    @Override
    public void insertBook(Book book) {
        bookService.insertEmptyBookIntoDB(book);
    }

    @Override
    public List<Book> getBookList(String path) {
        return bookService.getBookListFromDB();
    }

    @Override
    public Book getBookByName(String value) {
        return bookService.getBookByName(value);
    }

    @Override
    public void updateBook(Book book) {
        if (bookService.isDatabaseConnected()) {
            bookService.updateBook(book);
        } else {
            bookService.exportBook(book);
        }

    }

    @Override
    public void deleteBook(String value) {
        if (bookService.isDatabaseConnected()) {
            bookService.deleteBook(value);
        } else if (bookService.getBookPath() != null) {
            bookService.deleteBook(bookService.getBookPath());
        } else {
            System.err.println("DB Disconnected, Book cannot be deleted.");
        }
    }

    @Override
    public boolean exportBook(Book book) {
        return bookService.exportBook(book);
    }

    @Override
    public boolean exportBook(String title) {
        return bookService.exportBook(title);
    }

    @Override
    public String transliterate(String arabictext) {
        return bookService.translateToRomanEnglish(arabictext);
    }
    
    @Override
    public String analyzeWord(String selectedWord) {
        return bookService.analyzeWord(selectedWord);
    }

    @Override
    public List<String> searchBooksByContent(String searchText) {
        return bookService.searchBooksByContent(searchText);
    }

    @Override
    public void addPageByBookTitle(String title, Page page) {
        Book book = bookService.getBookByName(title);
        if (book != null) {
            bookService.addPage(title, page);
        } else {
            System.err.println("Book not found with title: " + title);
        }
    }
    
    @Override
    public String performAnalysis(Book book, String analysisMethod) {
        return bookService.performAnalysis(book, analysisMethod);
    }

    @Override
    public boolean isDatabaseConnected() {
        return bookService.isDatabaseConnected();
    }

}
