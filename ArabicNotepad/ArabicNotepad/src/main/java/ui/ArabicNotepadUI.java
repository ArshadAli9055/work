package ui;

import config.ConfigurationManager;
import config.Environment;
import dao.BookDAO;
import dao.BookDAOFactory;
import dto.Book;
import bl.BookFacade;
import bl.BookFacadeImpl;
import dto.Page;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

public class ArabicNotepadUI extends JFrame {

    private static final Logger logger = LoggerFactory.getLogger(ArabicNotepadUI.class);
    private final JPanel mainPanel;
    private final JTextField searchBar;
    private final JTable bookTable;
    private final BookFacade bookFacade;
    private final BookTableModel bookTableModel;
    private final JLabel statusLabel;
    private JProgressBar progressBar;

    private final ConfigurationManager configManager;
    private Environment currentEnvironment;

    private boolean isRefreshing = false, isRowAlreadySelected = false;
    int rowAlreadySelected;
    

    public ArabicNotepadUI() {
        this.configManager = ConfigurationManager.getInstance();
        try {
            this.currentEnvironment = configManager.getCurrentEnvironment();
        } catch (RemoteException ex) {
            java.util.logging.Logger.getLogger(ArabicNotepadUI.class.getName()).log(Level.SEVERE, null, ex);
        }

        logger.info("Initializing ArabicNotepadUI in {} environment.", currentEnvironment);

        BookDAO bookDAO = BookDAOFactory.createBookDAO();
        this.bookFacade = new BookFacadeImpl(bookDAO);

        setTitle("Arabic Notepad");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1200, 800);
        setMinimumSize(new Dimension(800, 600));

        mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

        statusLabel = createStatusLabel();
        bookTableModel = new BookTableModel();
        bookTable = createBookTable();
        searchBar = createSearchBar();

        searchBar.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                handleSearchUpdate();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                handleSearchUpdate();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                handleSearchUpdate();
            }
        });

        mainPanel.add(createTopPanel(), BorderLayout.NORTH);
        mainPanel.add(createCenterPanel(), BorderLayout.CENTER);
        assembleStatusPanel();

        setContentPane(mainPanel);
        helperRefreshBookList();
        pack();
        setVisible(true);
    }

    private void handleSearchUpdate() {
        String searchText = searchBar.getText().trim();

        if (searchText.isEmpty()) {
            if (!isRefreshing) {
                helperRefreshBookList();
            }
        } else {
            performSearch(searchText);
        }
    }

    private void performSearch(String searchText) {
        setStatus("Searching...");

        SwingWorker<List<String>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<String> doInBackground() {
                return bookFacade.searchBooksByContent(searchText);
            }

            @Override
            protected void done() {
                try {
                    List<String> searchResults = get();
                    bookTableModel.setSearchResults(searchResults);
                    setStatus("Search completed. " + searchResults.size() + " result(s) found.");
                } catch (InterruptedException | ExecutionException ex) {
                    setStatus("Error during search: " + ex.getMessage());
                    logger.error("Error during search", ex);
                }
            }
        };
        worker.execute();
    }

    private void assembleStatusPanel() {
        progressBar = new JProgressBar();
        progressBar.setStringPainted(true);
        progressBar.setIndeterminate(false);

        JPanel statusPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        statusPanel.add(statusLabel);
        statusPanel.add(progressBar);

        mainPanel.add(statusPanel, BorderLayout.SOUTH);
    }


    private JPanel createTopPanel() {
        JPanel panel = new JPanel(new BorderLayout());
     
        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        searchPanel.add(new JLabel("Search: "));
        searchPanel.add(searchBar);
 
        JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonsPanel.add(createButton("Import", this::onImportBookAction));
        buttonsPanel.add(createButton("Create", this::onCreateBookAction));
        buttonsPanel.add(createButton("Analyze", this::onAnalyzeBookAction));
        buttonsPanel.add(createButton("Delete", this::onDeleteBookAction));
        buttonsPanel.add(createButton("Export", this::onExportBookAction));

        panel.add(searchPanel, BorderLayout.WEST);
        panel.add(buttonsPanel, BorderLayout.EAST);

        return panel;
    }

    private JPanel createCenterPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        JScrollPane scrollPane = new JScrollPane(bookTable);
        panel.add(scrollPane, BorderLayout.CENTER);
        return panel;
    }

    private JLabel createStatusLabel() {
        JLabel label = new JLabel("Ready");
        label.setBorder(new EmptyBorder(5, 5, 5, 5));
        return label;
    }

    private JTable createBookTable() {
        JTable table = new JTable(bookTableModel);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setRowHeight(25);
        table.getTableHeader().setReorderingAllowed(false);

        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int newSelectedRow = table.rowAtPoint(e.getPoint());
                if (e.getClickCount() == 2 && newSelectedRow == rowAlreadySelected) {
                    openSelectedBook();
                }
                               
                if (newSelectedRow >= 0) {
                    if (isRowAlreadySelected) {
                    	if(newSelectedRow == rowAlreadySelected)
                    	{
                            table.getSelectionModel().removeSelectionInterval(newSelectedRow, newSelectedRow);
                            rowAlreadySelected = -1;
                    	}
                    	else
                    	{
                            table.getSelectionModel().removeSelectionInterval(rowAlreadySelected, rowAlreadySelected);
                            table.getSelectionModel().setSelectionInterval(newSelectedRow, newSelectedRow);
                            rowAlreadySelected = newSelectedRow;
                    	}
                        
                    } else {
                        table.getSelectionModel().setSelectionInterval(newSelectedRow, newSelectedRow);
                        rowAlreadySelected = newSelectedRow;
                    }
                    isRowAlreadySelected = !isRowAlreadySelected;
                }
            }
        });
        return table;
    }

    private JButton createButton(String text, ActionListener listener) {
        JButton button = new JButton(text);
        button.addActionListener(listener);
        return button;
    }

    private JTextField createSearchBar() {
        JTextField textField = new JTextField(20);
        return textField;
    }

    private void onImportBookAction(ActionEvent e) {
        String path = openFileChooser();
        if (path != null) {
            setStatus("Importing book...");
            progressBar.setIndeterminate(true);

            SwingWorker<Void, String> worker = new SwingWorker<>() {
                @Override
                protected Void doInBackground() {
                    try {
                        bookFacade.importBook(path);
                        publish("Book imported successfully");
                    } catch (Exception ex) {
                        publish("Error importing book: " + ex.getMessage());
                        logger.error("Error importing book", ex);
                    }
                    return null;
                }

                @Override
                protected void process(List<String> messages) {
                    for (String message : messages) {
                        setStatus(message);
                    }
                    helperRefreshBookList();
                }

                @Override
                protected void done() {
                    progressBar.setIndeterminate(false);
                    progressBar.setValue(0);
                }
            };
            worker.execute();
        }
    }

    private void onCreateBookAction(ActionEvent e) {
        String title = JOptionPane.showInputDialog(this, "Enter the book title:");

        if (title == null || title.trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Book title cannot be empty!", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        Book newBook = new Book();
        newBook.setTitle(title);
        newBook.setHash(null);

        List<Page> pages = new ArrayList<>();
        Page firstPage = new Page();
        firstPage.setContent("Feel Free to Dive Straight In!");
        pages.add(firstPage);
        newBook.setPages(pages);

        bookFacade.insertBook(newBook);
        helperRefreshBookList();
        BookUI.showBook(newBook, bookFacade);
    }


    private void onAnalyzeBookAction(ActionEvent e) {
        int[] selectedRows = bookTable.getSelectedRows();
        if (selectedRows.length == 0) {
            JOptionPane.showMessageDialog(this, "Please select at least one book to analyze.", "No Book Selected", JOptionPane.WARNING_MESSAGE);
            return;
        }

        List<String> bookTitles = new ArrayList<>();
        for (int row : selectedRows) {
            bookTitles.add((String) bookTable.getValueAt(row, 0));
        }

        List<Book> foundBooks = new ArrayList<>();
        List<String> notFoundBooks = new ArrayList<>();
        for (String bookTitle : bookTitles) {
            Book book = bookFacade.getBookByName(bookTitle);
            if (book != null) {
                foundBooks.add(book);
            } else {
                notFoundBooks.add(bookTitle);
            }
        }

        if (foundBooks.isEmpty()) {
            JOptionPane.showMessageDialog(this, "None of the selected books were found.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (notFoundBooks.isEmpty()) {
            JOptionPane.showMessageDialog(this, "All selected books were found. Starting analysis.", "Books Found", JOptionPane.INFORMATION_MESSAGE);
        } else {
            StringBuilder notFoundMessage = new StringBuilder("The following books were not found:\n");
            for (String title : notFoundBooks) {
                notFoundMessage.append(title).append("\n");
            }
            JOptionPane.showMessageDialog(this, notFoundMessage.toString(), "Some Books Not Found", JOptionPane.INFORMATION_MESSAGE);
        }

        String[] analysisOptions = {
            "Paper: Mining Quality Phrases from Massive Text Corpora",
            "PMI: Pointwise Mutual Information",
            "PKL: Point-wise Kullback-Leibler Divergence",
            "TF-IDF: Term Frequency-Inverse Document Frequency"
        };
        String selectedOption = (String) JOptionPane.showInputDialog(
            this,
            "Choose an analysis method:",
            "Analyze Books",
            JOptionPane.QUESTION_MESSAGE,
            null,
            analysisOptions,
            analysisOptions[0]
        );

        if (selectedOption != null) {
            String analysisMethod = selectedOption.split(":")[0].trim();
            performBatchAnalysis(foundBooks, analysisMethod);
        }
    }

    private void performBatchAnalysis(List<Book> foundBooks, String analysisMethod) {
        setStatus("Performing analysis: " + analysisMethod);
        progressBar.setIndeterminate(true);

        SwingWorker<List<String>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<String> doInBackground() {
                List<String> analysisResults = new ArrayList<>();
                for (Book book : foundBooks) {
                    String result = bookFacade.performAnalysis(book, analysisMethod);
                    analysisResults.add(result);
                }
                return analysisResults;
            }

            @Override
            protected void done() {
                try {
                    List<String> analysisResults = get();
                    progressBar.setIndeterminate(false);
                    progressBar.setValue(0);
                    showAnalysisResult(analysisResults, foundBooks);
                } catch (InterruptedException | ExecutionException ex) {
                    setStatus("Error during analysis: " + ex.getMessage());
                    progressBar.setIndeterminate(false);
                    progressBar.setValue(0);
                    logger.error("Error performing analysis", ex);
                    JOptionPane.showMessageDialog(
                        ArabicNotepadUI.this,
                        "An error occurred during analysis: " + ex.getMessage(),
                        "Analysis Error",
                        JOptionPane.ERROR_MESSAGE
                    );
                }
            }
        };
        worker.execute();
    }

    private void showAnalysisResult(List<String> analysisResults, List<Book> foundBooks) {
        int currentIndex = 0;
        while (currentIndex < analysisResults.size()) {
            String result = analysisResults.get(currentIndex);
            Book book = foundBooks.get(currentIndex);

            String message = result == null ? "Analysis failed for " + book.getTitle() : result;
            int option = JOptionPane.showOptionDialog(
                this,
                message,
                "Analysis Result for '" + book.getTitle() + "'",
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.INFORMATION_MESSAGE,
                null,
                new String[] { "Previous", "Next", "Finish" },
                "Next"
            );

            if (option == 0 && currentIndex > 0) {
                currentIndex--;
            } else if (option == 1 && currentIndex < analysisResults.size() - 1) {
                currentIndex++;
            } else {
                break;
            }
        }

        exportAnalysisResults(foundBooks, analysisResults);
    }

    private void exportAnalysisResults(List<Book> foundBooks, List<String> analysisResults) {
        Book exportBook = new Book();
        exportBook.setTitle("Book Analysis Results");
        exportBook.setHash(null);
        exportBook.setId(0);
        exportBook.setIdauthor("0");

        List<Page> pages = new ArrayList<>();
     
        for (int i = 0; i < foundBooks.size(); i++) {
            String analysisResult = analysisResults.get(i);
            Page analysisPage = new Page(0, 0, i + 1, foundBooks.get(i) + "\n\n" + analysisResult);
            pages.add(analysisPage);
        }
        exportBook.setPages(pages);
        bookFacade.exportBook(exportBook);
        JOptionPane.showMessageDialog(this, "Analysis results exported successfully.", "Export Complete", JOptionPane.INFORMATION_MESSAGE);
    }

    private void openSelectedBook() {
        int selectedRow = bookTable.getSelectedRow();

        if (selectedRow != -1) {
            String bookTitle = (String) bookTable.getValueAt(selectedRow, 0);

            Book book = bookFacade.getBookByName(bookTitle);

            if (book != null) {
                BookUI.showBook(book, bookFacade);
            } else {
                JOptionPane.showMessageDialog(this, "Book not found.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        } else {
            JOptionPane.showMessageDialog(this, "Please select a book to open.", "No Book Selected", JOptionPane.WARNING_MESSAGE);
        }
    }


    private String openFileChooser() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Select Book File or Folder");
        fileChooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);

        if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            return fileChooser.getSelectedFile().getAbsolutePath();
        }
        return null;
    }

    public final void helperRefreshBookList() {
        if (!isRefreshing) {
            refreshBookList();
        }
    }

    private void refreshBookList() {
        isRefreshing = true;
        setStatus("Refreshing book list...");
        progressBar.setIndeterminate(true);

        SwingWorker<List<Book>, String> worker = new SwingWorker<>() {
            @Override
            protected List<Book> doInBackground() {
                return bookFacade.getBookList(null);
            }

            @Override
            protected void done() {
                try {
                    List<Book> books = get();
                    bookTableModel.setBooks(books);
                    setStatus("Ready");
                } catch (InterruptedException | ExecutionException ex) {
                    setStatus("Error refreshing book list: " + ex.getMessage());
                    logger.error("Error refreshing book list", ex);
                } finally {
                    isRefreshing = false;
                    progressBar.setIndeterminate(false);
                    progressBar.setValue(0);
                }
            }
        };
        worker.execute();
    }

    private void setStatus(String message) {
        SwingUtilities.invokeLater(() -> statusLabel.setText(message));
    }

    private void onDeleteBookAction(ActionEvent e) {
    int[] selectedRows = bookTable.getSelectedRows();
    if (selectedRows.length == 0) {
        JOptionPane.showMessageDialog(this, "Please select at least one book to delete.", "No Book Selected", JOptionPane.WARNING_MESSAGE);
    } else {
        StringBuilder bookTitles = new StringBuilder();
        for (int row : selectedRows) {
            String bookTitle = (String) bookTable.getValueAt(row, 0);
            bookTitles.append(bookTitle).append("\n");
        }

        int confirmed = JOptionPane.showConfirmDialog(this, "Are you sure you want to delete the following books?\n" + bookTitles.toString(), "Confirm Deletion", JOptionPane.YES_NO_OPTION);
        if (confirmed == JOptionPane.YES_OPTION) {
            for (int row : selectedRows) {
                String bookTitle = (String) bookTable.getValueAt(row, 0);
                bookFacade.deleteBook(bookTitle);
                logger.info("Deleted book '{}'", bookTitle);
            }
        }
        helperRefreshBookList();
    }
    }

    private void onExportBookAction(ActionEvent e) {
    int[] selectedRows = bookTable.getSelectedRows();
    if (selectedRows.length == 0) {
        JOptionPane.showMessageDialog(this, "Please select at least one book to export.", "No Book Selected", JOptionPane.WARNING_MESSAGE);
    } else {
        StringBuilder bookTitles = new StringBuilder();
        for (int row : selectedRows) {
            String bookTitle = (String) bookTable.getValueAt(row, 0);
            bookTitles.append(bookTitle).append("\n");
        }

        for (int row : selectedRows) {
            String bookTitle = (String) bookTable.getValueAt(row, 0);
            bookFacade.exportBook(bookTitle);
            logger.info("Exported book '{}'", bookTitle);
        }
    }
    }

    private class BookTableModel extends AbstractTableModel {

        private List<Book> books;
        private List<String> searchResults;
        private final String[] bookColumnNames = {"Title", "Author ID"};
        private final String[] searchColumnNames = {"Title", "Matching Sentence"};
        private boolean isSearchMode = false;

        public BookTableModel() {
            this.books = new ArrayList<>();
            this.searchResults = new ArrayList<>();
        }

        public void setBooks(List<Book> books) {
            this.books = books != null ? books : new ArrayList<>();
            this.isSearchMode = false;
            fireTableStructureChanged();
        }

        public void setSearchResults(List<String> searchResults) {
            this.searchResults = searchResults != null ? searchResults : new ArrayList<>();
            this.isSearchMode = true;
            fireTableStructureChanged();
        }

        @Override
        public int getRowCount() {
            if (isSearchMode) {
                return searchResults.size();
            } else {
                return books.size();
            }
        }

        @Override
        public int getColumnCount() {
            if (isSearchMode) {
                return searchColumnNames.length;
            } else {
                return bookColumnNames.length;
            }
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            if (isSearchMode) {
                if (rowIndex < 0 || rowIndex >= searchResults.size()) {
                    return null;
                }
                String result = searchResults.get(rowIndex);
                String[] parts = result.split(", ", 2);
                if (parts.length < 2) {
                    return null;
                }
                return switch (columnIndex) {
                    case 0 -> parts[0].replace("Title: ", "").trim();
                    case 1 -> parts[1].replace("Sentence: ", "").trim();
                    default -> null;
                };
            } else {
                if (rowIndex < 0 || rowIndex >= books.size()) {
                    return null;
                }
                Book book = books.get(rowIndex);
                return switch (columnIndex) {
                    case 0 -> book.getTitle();
                    case 1 -> book.getIdauthor();
                    default -> null;
                };
            }
        }

        @Override
        public String getColumnName(int column) {
            if (isSearchMode) {
                return searchColumnNames[column];
            } else {
                return bookColumnNames[column];
            }
        }
    }

}
