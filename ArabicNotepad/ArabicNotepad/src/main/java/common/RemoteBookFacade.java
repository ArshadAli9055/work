package common;
import java.rmi.Remote;
import java.rmi.RemoteException;
import dto.Book;
import dto.Page;
import java.util.List;
import ui.ArabicNotepadClient;

public interface RemoteBookFacade extends Remote {
    List<Book> getBookList(String filepath) throws RemoteException;
    Book getBookByName(String value) throws RemoteException;
    void insertBook(Book book) throws RemoteException;
    void updateBook(Book book) throws RemoteException;
    void deleteBook(String value) throws RemoteException;
    void importBook(String path) throws RemoteException;
    boolean exportBook(String title) throws RemoteException;
    boolean exportBook(Book book) throws RemoteException;
    String transliterate(String arabictext) throws RemoteException;
    List<String> searchBooksByContent(String searchText) throws RemoteException;
    void addPageByBookTitle(String title, Page page) throws RemoteException;
    String performAnalysis(Book book, String analysisMethod) throws RemoteException;
    String analyzeWord(String selectedWord) throws RemoteException;
    boolean isDatabaseConnected() throws RemoteException;
    void registerClient(ArabicNotepadClient client) throws RemoteException;
    boolean ping() throws RemoteException;
}
