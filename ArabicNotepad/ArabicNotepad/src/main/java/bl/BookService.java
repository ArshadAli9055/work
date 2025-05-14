package bl;

import config.ConfigurationManager;
import config.Environment;
import config.UserConfig;
import dao.BookDAO;
import dao.LocalStorageBookDAO;
import dto.Book;
import dto.Page;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import util.*;

import java.io.File;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

public class BookService {

    private static final int BATCH_SIZE = 50;
    private static final Logger logger = LoggerFactory.getLogger(BookService.class);

    private final BookDAO bookDAO;
    private final LocalStorageBookDAO localStorageBookDAO;
    private final ConfigurationManager configManager;
    private UserConfig userConfig;
    private final String userId;
    private Environment currentEnvironment;

    private QualityPhrasesMiner qualityPhrasesMiner;
    private PMIAnalyzer pmiAnalyzer;
    private PKLAnalyzer pklAnalyzer;
    private TFIDFAnalyzer tfidfAnalyzer;
    private WordAnalyzer wordAnalyzer;
    private TransliterationUtil transliterationUtil;

    public BookService(BookDAO bookDAO) {
        this.bookDAO = bookDAO;
        this.localStorageBookDAO = new LocalStorageBookDAO();
        this.configManager = ConfigurationManager.getInstance();
        try {
            this.currentEnvironment = configManager.getCurrentEnvironment();
        } catch (RemoteException ex) {
            java.util.logging.Logger.getLogger(BookService.class.getName()).log(Level.SEVERE, null, ex);
        }
        try {
            this.userConfig = configManager.getUserConfig();
        } catch (RemoteException ex) {
            java.util.logging.Logger.getLogger(BookService.class.getName()).log(Level.SEVERE, null, ex);
        }
        this.userId = userConfig.getUserId();

        logger.info("Initializing BookService in {} environment.", currentEnvironment);
    }
    
     private QualityPhrasesMiner getQualityPhrasesMiner() {
        if (qualityPhrasesMiner == null) {
            qualityPhrasesMiner = QualityPhrasesMiner.getInstance();
        }
        return qualityPhrasesMiner;
    }

    private PMIAnalyzer getPmiAnalyzer() {
        if (pmiAnalyzer == null) {
            pmiAnalyzer = PMIAnalyzer.getInstance();
        }
        return pmiAnalyzer;
    }

    private PKLAnalyzer getPklAnalyzer() {
        if (pklAnalyzer == null) {
            pklAnalyzer = PKLAnalyzer.getInstance();
        }
        return pklAnalyzer;
    }

    private TFIDFAnalyzer getTfidfAnalyzer() {
        if (tfidfAnalyzer == null) {
            tfidfAnalyzer = TFIDFAnalyzer.getInstance();
        }
        return tfidfAnalyzer;
    }

    private WordAnalyzer getWordAnalyzer() {
        if (wordAnalyzer == null) {
            wordAnalyzer = WordAnalyzer.getInstance();
        }
        return wordAnalyzer;
    }

    private TransliterationUtil getTransliterationUtil() {
        if (transliterationUtil == null) {
            transliterationUtil = TransliterationUtil.getInstance();
        }
        return transliterationUtil;
    }


    private void addBookWithLogging(Book book) {
        if (bookDAO.addBook(book, true)) {
            logger.info("Successfully added book to DB: {}", book.getTitle());
        } else {
            logger.warn("Failed to add book to DB: {}", book.getTitle());
        }
    }

    public boolean hasWritePrivileges(String bookTitle) {
        Book book = bookDAO.getBookByName(bookTitle);

        if (book == null) {
            logger.warn("No book found with title: {}", bookTitle);
            return false;
        }

        boolean hasPrivileges = userId.equals(book.getIdauthor());
        if (hasPrivileges) {
            logger.info("User '{}' has write privileges for the book '{}'.", userId, bookTitle);
        } else {
            logger.warn("User '{}' does NOT have write privileges for the book '{}'.", userId, bookTitle);
        }
        return hasPrivileges;
    }

    public void importBook(String path) {
        File file = new File(path);

        if (file.isDirectory()) {
            List<Book> books = localStorageBookDAO.getAllBooks(path);
            if (books == null || books.isEmpty()) {
                logger.warn("No books found in local storage for the directory: {}", path);
                return;
            }
            importBooksInBatches(books);
        } else if (file.isFile()) {
            Book book = localStorageBookDAO.getBookByName(file.getAbsolutePath());
            if (book != null) {
                processSingleBook(book);
            } else {
                logger.warn("No book found in local storage with name: {}", file.getName());
            }
        } else {
            logger.warn("Invalid path provided: {}", path);
        }
    }

    
    private void importBooksInBatches(List<Book> books) {
        List<Book> batch = new ArrayList<>();

        for (Book book : books) {
            try {
                setAuthorIdIfNecessary(book);
                batch.add(book);

                if (batch.size() == BATCH_SIZE) {
                    processBatch(batch);
                    batch.clear();
                }
            } catch (Exception e) {
                logger.error("Error processing book: {}", book.getTitle(), e);
            }
        }

        if (!batch.isEmpty()) {
            processBatch(batch);
        }
    }

    
    private void processBatch(List<Book> batch) {
        logger.info("Processing batch of size: {}", batch.size());

        for (Book book : batch) {
            if (!bookDAO.isHashExists(book.getHash())) {
                addBookWithLogging(book);
            } else {
                logger.info("Book already exists in DB, skipping: {}", book.getTitle());
            }
        }
    }

    
    private void processSingleBook(Book book) {
        try {
            setAuthorIdIfNecessary(book);
            if (!bookDAO.isHashExists(book.getHash())) {
                addBookWithLogging(book);
            } else {
                logger.info("Book already exists in DB, skipping: {}", book.getTitle());
            }
        } catch (Exception e) {
            logger.error("Failed to import book: {}", book.getTitle(), e);
        }
    }

    
    private void setAuthorIdIfNecessary(Book book) {
        if (book.getIdauthor() == null || book.getIdauthor().isEmpty()) {
            book.setIdauthor(userId);
            logger.debug("Set author ID for book '{}': {}", book.getTitle(), userId);
        }
    }

    
    public void insertEmptyBookIntoDB(Book book) {
        setAuthorIdIfNecessary(book);
        addBookWithLogging(book);
    }

    
    public List<Book> getBookListFromDB() {
        List<Book> books = bookDAO.getAllBooks(null);
        if (books.isEmpty()) {
            logger.info("No books found in the database.");
        } else {
            logger.info("Retrieved {} books from the database.", books.size());
        }
        return books;
    }

    
    public Book getBookByName(String title) {
        Book book = bookDAO.getBookByName(title);
        if (book == null) {
            logger.warn("No book found with title: {}", title);
        } else {
            logger.info("Retrieved book from DB: {}", book.getTitle());
        }
        return book;
    }

    
     public boolean deleteBook(String value) {
        File file = new File(value);

        if (file.exists() && file.isFile()) {
            boolean deleted = localStorageBookDAO.deleteBook(value);
            if (deleted) {
                logger.info("Deleted book from local storage: {}", value);
            } else {
                logger.warn("Failed to delete book from local storage: {}", value);
            }
            return deleted;
        } else {
            boolean deleted = bookDAO.deleteBook(value);
            if (deleted) {
                logger.info("Deleted book from DB: {}", value);
            } else {
                logger.warn("Failed to delete book from DB: {}", value);
            }
            return deleted;
        }
    }
    
    
    public boolean exportBook(String bookTitle) {
        Book book = bookDAO.getBookByName(bookTitle);
        if (book == null) {
            logger.warn("No book found in SQL DB with title: {}", bookTitle);
            return false;
        }

        boolean exported = localStorageBookDAO.addBook(book, !isDatabaseConnected());
        if (exported) {
            logger.info("Successfully exported book to local storage: {}", book.getTitle());
        } else {
            logger.warn("Failed to export book to local storage: {}", book.getTitle());
        }
        return exported;
    }

    
    public boolean exportBook(Book book) {
        boolean exported = localStorageBookDAO.addBook(book, !isDatabaseConnected());
        if (exported) {
            logger.info("Successfully exported book to local storage: {}", book.getTitle());
        } else {
            logger.warn("Failed to export book to local storage: {}", book.getTitle());
        }
        return exported;
    }

    
    public boolean updateBook(Book book) {
        boolean updated = bookDAO.updateBook(book);
        if (updated) {
            logger.info("Book '{}' was updated successfully.", book.getTitle());
        } else {
            logger.error("Failed to update book '{}'.", book.getTitle());
        }
        return updated;
    }

   
    public boolean isDatabaseConnected() {
        boolean connected = bookDAO.isDatabaseConnected();
        if (connected) {
            logger.info("Database connection is active.");
        } else {
            logger.warn("Database connection is inactive.");
        }
        return connected;
    }

    
     public String translateToRomanEnglish(String arabicText) {
        String translated = getTransliterationUtil().translateToRomanEnglish(arabicText);
        logger.debug("Translated Arabic to Roman English: {}", translated);
        return translated;
    }

    
    public List<String> searchBooksByContent(String searchText) {
        List<String> results = bookDAO.searchBooksByContent(searchText);
        logger.info("Found {} books matching the search text '{}'.", results.size(), searchText);
        return results;
    }
    
    public List<String> searchBooksByTitle(String searchText)
    {
        List<String> results = bookDAO.searchBooksByContent(searchText);
        logger.info("Found {} books matchning the search text '{}.", results.size(), searchText);
        return results;
    }
    
    public boolean addPage(String title, Page page) {
        Book book = getBookByName(title);
        if (book != null) {
            boolean added = bookDAO.addPage(book.getId(), page);
            if (added) {
                logger.info("Added page to book '{}'.", title);
            } else {
                logger.warn("Failed to add page to book '{}'.", title);
            }
            return added;
        } else {
            logger.warn("Cannot add page. Book '{}' does not exist.", title);
            return false;
        }
    }

    
    public String getBookPath() {
        String currentPath = null;
        try {
            currentPath = configManager.getLocalConfig().getCurrentPath();
        } catch (RemoteException ex) {
            java.util.logging.Logger.getLogger(BookService.class.getName()).log(Level.SEVERE, null, ex);
        }
        logger.debug("Retrieved current path from config: {}", currentPath);
        return currentPath;
    }

     public String performAnalysis(Book book, String analysisMethod) {
        logger.info("Starting analysis '{}' for book '{}'.", analysisMethod, book.getTitle());
        String result;

        switch (analysisMethod) {
            case "Paper" -> result = getQualityPhrasesMiner().mineQualityPhrases(book);
            case "PMI" -> result = getPmiAnalyzer().calculatePMI(book);
            case "PKL" -> result = getPklAnalyzer().calculatePKL(book);
            case "TF-IDF" -> result = getTfidfAnalyzer().calculateTFIDF(book);
            default -> {
                logger.error("Unknown analysis method: {}", analysisMethod);
                throw new IllegalArgumentException("Unknown analysis method: " + analysisMethod);
            }
        }
        logger.info("Completed analysis '{}' for book '{}'.", analysisMethod, book.getTitle());
        return result;
    }

    String analyzeWord(String word) {
        return getWordAnalyzer().analyzeWord(word);
    }
}
