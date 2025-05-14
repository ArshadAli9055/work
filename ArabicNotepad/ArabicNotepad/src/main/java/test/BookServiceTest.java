package test; 

import bl.BookService;
import dao.InMemoryBookDAO;
import dto.Book;
import dto.Page;
import org.junit.jupiter.api.*;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class BookServiceTest {

    private BookService bookService;
    private InMemoryBookDAO bookDAO;

    @BeforeEach
    void setUp() {
        bookDAO = new InMemoryBookDAO();
        bookService = new BookService(bookDAO);
    }

    @AfterEach
    void tearDown() {
        bookDAO.clear(); // Clear data after each test
    }

    @Test
    void testAddBook() {
        Book book = new Book(1, "Book Title", "hash123", "author1", null);
        bookService.insertEmptyBookIntoDB(book);
        List<Book> books = bookService.getBookListFromDB();
        
        assertEquals(1, books.size(), "Service should return all added books");
    }

    @Test
    void testAddDuplicateBook() {
        Book book = new Book(1, "Book Title", "hash123", "author1", null);
        bookService.insertEmptyBookIntoDB(book);
        bookService.insertEmptyBookIntoDB(book);
        
        List<Book> books = bookService.getBookListFromDB();
        
        assertEquals(1, books.size(), "Service should return all added books");
    }

    @Test
    void testGetBookByTitle() {
        Book book = new Book(1, "Book Title", "hash123", "author1", null);
        bookService.insertEmptyBookIntoDB(book);

        Book retrievedBook = bookService.getBookByName("Book Title");
        assertNotNull(retrievedBook, "Book should be retrieved by title");
        assertEquals(book, retrievedBook, "Retrieved book should match the added book");
    }

    @Test
    void testGetAllBooks() {
        bookService.insertEmptyBookIntoDB(new Book(1, "Book One", "hash1", "author1", null));
        bookService.insertEmptyBookIntoDB(new Book(2, "Book Two", "hash2", "author2", null));

        List<Book> books = bookService.getBookListFromDB();
        assertEquals(2, books.size(), "Service should return all added books");
    }

    @Test
    void testDeleteBook() {
        bookService.insertEmptyBookIntoDB(new Book(1, "Book One", "hash1", "author1", null));
        bookService.deleteBook("Book One");

        List<Book> books = bookService.getBookListFromDB();
        assertEquals(0, books.size(), "Service should return all added books");
        assertNull(bookService.getBookByName("Book One"), "Deleted book should not be retrievable");
    }

    @Test
    void testAddPage() {
        Book book = new Book(1, "Book Title", "hash123", "author1", null);
        bookService.insertEmptyBookIntoDB(book);

        Page page = new Page(1, book.getId(), 1, "Page content");
        assertTrue(bookService.addPage(book.getTitle(), page), "Page should be added successfully");
    }

    @Test
    void testSearchBooksByContent() {
        Book book = new Book(1, "Book Title", "hash123", "author1", null);
        bookService.insertEmptyBookIntoDB(book);

        Page page = new Page(1, book.getId(), 1, "This is a sample page content.");
        bookService.addPage(book.getTitle(), page);

        List<String> results = bookService.searchBooksByContent("sample");
        assertEquals(1, results.size(), "Search should return one matching book");
        assertEquals("Book Title", results.get(0), "Matching book title should be returned");
    }
}