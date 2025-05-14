package common;

import bl.BookFacade;
import java.rmi.server.UnicastRemoteObject;
import dto.Book;
import dto.Page;
import java.rmi.RemoteException;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ui.ArabicNotepadClient;

public class RemoteBookFacadeImpl extends UnicastRemoteObject implements RemoteBookFacade {
    
    private final BookFacade bookFacade;
    private ArabicNotepadClient client;
    private static final Logger logger = LoggerFactory.getLogger(RemoteBookFacadeImpl.class);

    public RemoteBookFacadeImpl(BookFacade bookFacade) throws RemoteException {
        super();
        this.bookFacade = bookFacade;
    }

    @Override
    public List<Book> getBookList(String filepath) throws RemoteException {
        return bookFacade.getBookList(filepath);
    }

    @Override
    public Book getBookByName(String value) throws RemoteException {
        return bookFacade.getBookByName(value);
    }

    @Override
    public void insertBook(Book book) throws RemoteException {
        bookFacade.insertBook(book);
        
    }

    @Override
    public void updateBook(Book book) throws RemoteException {
        bookFacade.updateBook(book);
    }

    @Override
    public void deleteBook(String title) throws RemoteException {
        bookFacade.deleteBook(title);
    }

    @Override
    public void importBook(String path) throws RemoteException {
        bookFacade.importBook(path);
    }

    @Override
    public boolean exportBook(String title) throws RemoteException {
        return bookFacade.exportBook(title);
    }

    @Override
    public boolean exportBook(Book book) throws RemoteException {
        return bookFacade.exportBook(book);
    }

    @Override
    public String transliterate(String arabictext) throws RemoteException {
        return bookFacade.transliterate(arabictext);
    }

    @Override
    public List<String> searchBooksByContent(String searchText) throws RemoteException {
        return bookFacade.searchBooksByContent(searchText);
    }

    @Override
    public void addPageByBookTitle(String title, Page page) throws RemoteException {
        bookFacade.addPageByBookTitle(title, page);
    }

    @Override
    public String performAnalysis(Book book, String analysisMethod) throws RemoteException {
        return bookFacade.performAnalysis(book, analysisMethod);
    }

    @Override
    public String analyzeWord(String selectedWord) throws RemoteException {
        return bookFacade.analyzeWord(selectedWord);
    }
     
    @Override
    public boolean isDatabaseConnected() throws RemoteException {
        return bookFacade.isDatabaseConnected();
    }

    @Override
    public void registerClient(ArabicNotepadClient client) throws RemoteException {
        try {
            this.client = client;
            client.onRegisterClient(true);
            logger.info("Client registered successfully");
        } catch (RemoteException e) {
            logger.error("Error registering client", e);
            client.onRegisterClient(false);
        }
    }
    
     @Override
    public boolean ping() throws RemoteException {
        logger.info("Ping received from client");
        return true;
    }
}
