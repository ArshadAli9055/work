package dao;

import dto.Book;
import dto.Page;
import config.ConfigurationManager;
import config.LocalConfig;
import config.DBConfig;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import util.FileUtil;

public class LocalStorageBookDAO implements BookDAO {

    private static final int MAX_LINES_PER_PAGE = 20;
    private static final Logger logger = LoggerFactory.getLogger(LocalStorageBookDAO.class);
    
    private LocalConfig localConfig;

    public LocalStorageBookDAO() {
        
        ConfigurationManager configManager = ConfigurationManager.getInstance();
        try {
            this.localConfig = configManager.getLocalConfig();
        } catch (RemoteException ex) {
            java.util.logging.Logger.getLogger(LocalStorageBookDAO.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public List<Book> getAllBooks(String path) {
        File folder = new File(path);
        File[] files = folder.listFiles();
        List<Book> books = new ArrayList<>();

        if (files != null) {
            for (File file : files) {
                if (file.isFile()) {
                    Book book = new Book();
                    List<Page> pages = new ArrayList<>();
                    String firstLine;

                    try (BufferedReader reader = new BufferedReader(new java.io.FileReader(file))) {
                        firstLine = reader.readLine();

                        if (isIdAuthor(firstLine)) {
                            book.setIdauthor(firstLine.substring("**idauthor**: ".length()).trim());
                        } else {
                            logger.info("First line does not indicate idauthor: {}", firstLine);
                            book.setIdauthor(null);
                        }

                        String line;
                        int pageNumber = 1;
                        StringBuilder contentBuilder = new StringBuilder();
                        int lineCount = 0;

                        while ((line = reader.readLine()) != null) {
                            if (!line.isEmpty()) {
                                contentBuilder.append(line).append("\n");
                                lineCount++;

                                if (lineCount >= MAX_LINES_PER_PAGE) {
                                    Page page = new Page();
                                    page.setPageNumber(pageNumber++);
                                    page.setContent(contentBuilder.toString().trim());
                                    pages.add(page);
                                    contentBuilder.setLength(0);
                                    lineCount = 0;
                                }
                            } else {
                                if (contentBuilder.length() > 0) {
                                    contentBuilder.append("\n");
                                }
                            }
                        }

                        if (contentBuilder.length() > 0) {
                            Page page = new Page();
                            page.setPageNumber(pageNumber++);
                            page.setContent(contentBuilder.toString().trim());
                            pages.add(page);
                        }

                        book.setPages(pages);
                    } catch (IOException e) {
                        logger.error("Error reading book from file: {}", file.getAbsolutePath(), e);
                        continue;
                    } catch (IllegalArgumentException e) {
                        logger.warn("Invalid date format in file: {}", file.getAbsolutePath(), e);
                        continue;
                    }

                    book.setTitle(file.getName().replace(".txt", ""));
                    book.setHash(FileUtil.calculateSHA256(getAllPagesContent(pages)));
                    books.add(book);
                }
            }
        } else {
            logger.warn("No files found in the directory: {}", path);
        }

        return books;
    }

    @Override
    public Book getBookByName(String path) {
        File file = new File(path);
        if (!file.exists() || !file.isFile()) {
            logger.warn("File does not exist or is not a file: {}", path);
            return null;
        }

        Book book = new Book();
        List<Page> pages = new ArrayList<>();
        StringBuilder contentBuilder = new StringBuilder();
        String firstLine;

        try (BufferedReader reader = new BufferedReader(new java.io.FileReader(file))) {
            firstLine = reader.readLine();

            if (isIdAuthor(firstLine)) {
                book.setIdauthor(firstLine.substring("**idauthor**: ".length()).trim());
            } else {
                logger.info("First line does not indicate idauthor: {}", firstLine);
                book.setIdauthor(null);
            }

            String line;
            int pageNumber = 1;
            int lineCount = 0;

            while ((line = reader.readLine()) != null) {
                if (!line.isEmpty()) {
                    contentBuilder.append(line).append(System.lineSeparator());
                    lineCount++;

                    if (lineCount >= MAX_LINES_PER_PAGE) {
                        Page page = new Page();
                        page.setPageNumber(pageNumber++);
                        page.setContent(contentBuilder.toString().trim());
                        pages.add(page);
                        contentBuilder.setLength(0);
                        lineCount = 0;
                    }
                } else {
                    if (contentBuilder.length() > 0) {
                        contentBuilder.append(System.lineSeparator());
                    }
                }
            }

            if (contentBuilder.length() > 0) {
                Page page = new Page();
                page.setPageNumber(pageNumber++);
                page.setContent(contentBuilder.toString().trim());
                pages.add(page);
            }

            book.setPages(pages);
        } catch (IOException e) {
            logger.error("Error reading book from file: {}", path, e);
            return null;
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid date format in file: {}", path, e);
            return null;
        }

        book.setTitle(file.getName().replace(".txt", ""));
        book.setHash(FileUtil.calculateSHA256(getAllPagesContent(pages)));

        return book;
    }

    @Override
    public boolean addBook(Book book, boolean isDbDown) {
        if (book == null || book.getTitle() == null || book.getIdauthor() == null || book.getIdauthor().isEmpty()) {
            logger.warn("Invalid book details provided for storage.");
            return false;
        }

        String storagePath;
        if (isDbDown) {
            storagePath = localConfig.getCurrentPath();
            if (storagePath == null || storagePath.isEmpty()) {
                storagePath = localConfig.getStoragePath();
            }
        } else {
            storagePath = localConfig.getStoragePath();
        }

        File directory = new File(storagePath);
        if (!directory.exists()) {
            directory.mkdirs();
        }

        File bookFile = new File(directory, book.getTitle() + ".md");
        try (FileWriter writer = new FileWriter(bookFile, false)) {
            writer.write("**idauthor**: " + book.getIdauthor() + "\n");

            for (Page page : book.getPages()) {
                writer.write(page.getContent() + "\n\n");
            }
        } catch (IOException e) {
            logger.error("Error writing book to local storage: {}", book.getTitle(), e);
            return false;
        }
        return true;
    }

    @Override
    public boolean updateBook(Book book) {
        File bookFile = new File(localConfig.getStoragePath(), book.getTitle() + ".md");
        if (bookFile.exists()) {
            try (FileWriter writer = new FileWriter(bookFile, false)) {
                writer.write("**idauthor**: " + book.getIdauthor() + "\n");

                for (Page page : book.getPages()) {
                    writer.write(page.getContent() + "\n\n");
                }

                logger.info("Successfully updated book in local storage: {}", book.getTitle());
                return true;
            } catch (IOException e) {
                logger.error("Error updating book in local storage: {}", book.getTitle(), e);
                return false;
            }
        } else {
            logger.warn("Book file does not exist. Attempting to add book: {}", book.getTitle());
            boolean added = addBook(book, true);
            if (added) {
                logger.info("Book added successfully as it did not exist: {}", book.getTitle());
            } else {
                logger.error("Failed to add book as it did not exist: {}", book.getTitle());
            }
            return added;
        }
    }

    @Override
    public boolean deleteBook(String path) {
        File bookFile = new File(path);
        if (bookFile.exists()) {
            if (bookFile.delete()) {
                logger.info("Book deleted successfully at path: {}", path);
                return true;
            } else {
                logger.error("Failed to delete book at path: {}", path);
                return false;
            }
        } else {
            logger.warn("Book file does not exist for deletion at path: {}", path);
            return false;
        }
    }

    @Override
    public boolean isHashExists(String hash) {
        return false;
    }

    @Override
    public boolean connect(DBConfig dbConfig) {
        return true;
    }

    @Override
    public boolean isDatabaseConnected() {
        return false;
    }

    private boolean isIdAuthor(String line) {
        return line.startsWith("**idauthor**: ");
    }

    @Override
    public List<String> searchBooksByContent(String searchText) {
        return null;
    }

    @Override
    public boolean addPage(int bookId, Page page) {
        return false;
    }

    @Override
    public List<Page> getPagesByBookTitle(String title) {
        return null;
    }

    @Override
    public void deletePagesByBookTitle(String title) {
        
    }

    private String getAllPagesContent(List<Page> pages) {
        StringBuilder sb = new StringBuilder();
        for (Page page : pages) {
            sb.append(page.getContent()).append("\n");
        }
        return sb.toString();
    }
}
