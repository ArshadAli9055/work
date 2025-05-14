package ui;

import dto.Book;
import dto.Page;
import bl.BookFacade;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BookUI extends JFrame {

    private static final Logger logger = LoggerFactory.getLogger(BookUI.class);
    
    private final Book book;
    private final BookFacade bookFacade;
    private JTextArea textArea;
    private JButton leftButton;
    private JButton rightButton;
    private JTextField searchField;
    private int currentPageIndex;
    private JLabel pageNumberLabel;

    public BookUI(Book book, BookFacade bookFacade) {
        this.bookFacade = bookFacade;
        this.book = book;

        this.currentPageIndex = 0;
        initializeUI();
        loadContent();
        logger.info("BookUI initialized for book: {}", book.getTitle());
    }

    private void initializeUI() {
        setTitle(book.getTitle());
        setLayout(new BorderLayout());
        setSize(800, 600);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        JPanel topPanel = createTopPanel();
        add(topPanel, BorderLayout.NORTH);

        JPanel contentPanel = createContentPanel();
        add(contentPanel, BorderLayout.CENTER);

        JPanel navigationPanel = createNavigationPanel();
        add(navigationPanel, BorderLayout.SOUTH);

        loadContent();
        updateNavigationState();
    }

    private JPanel createTopPanel() {
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));

        searchField = new JTextField(20);
        JButton searchButton = new JButton("Search");
        searchButton.addActionListener(e -> performSearch());

        JButton exportButton = new JButton("Export");
        JButton transliterateButton = new JButton("Transliterate");
        JButton analyzeWordButton = new JButton("Analyze");
        

        exportButton.addActionListener(this::handleExport);
        transliterateButton.addActionListener(this::handleTransliterate);
        analyzeWordButton.addActionListener(this::handleAnalyzeWord); 

        topPanel.add(searchField);
        topPanel.add(searchButton);
        topPanel.add(exportButton);
        topPanel.add(transliterateButton);
        topPanel.add(analyzeWordButton);

        return topPanel;
    }

    private JPanel createContentPanel() {
        JPanel contentPanel = new JPanel(new BorderLayout());

        textArea = new JTextArea();
        textArea.setFont(new Font("Arial", Font.PLAIN, 14));
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        textArea.setMargin(new Insets(10, 10, 10, 10));

        textArea.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                handleRealTimeContentUpdate();
            }
        });

        contentPanel.add(textArea, BorderLayout.CENTER);
        return contentPanel;
    }

    private JPanel createNavigationPanel() {
        JPanel navigationPanel = new JPanel(new FlowLayout());

        leftButton = new JButton("←");
        rightButton = new JButton("→");
        pageNumberLabel = new JLabel("Page 1 of " + (book.getPages() != null ? book.getPages().size() : 0));

        leftButton.addActionListener(e -> navigatePages(-1));
        rightButton.addActionListener(e -> navigatePages(1));

        navigationPanel.add(leftButton);
        navigationPanel.add(pageNumberLabel);
        navigationPanel.add(rightButton);

        return navigationPanel;
    }

    private void loadContent() {
        List<Page> pages = book.getPages();
        if (pages != null && !pages.isEmpty() && currentPageIndex < pages.size()) {
            Page currentPage = pages.get(currentPageIndex);
            textArea.setText(currentPage.getContent());
            pageNumberLabel.setText(String.format("Page %d of %d", currentPageIndex + 1, pages.size()));
            logger.info("Loaded content for page {} of book '{}'", currentPageIndex + 1, book.getTitle());
        } else {
            textArea.setText("");
            pageNumberLabel.setText("No pages available.");
            logger.warn("No content available to load for book '{}'", book.getTitle());
        }
        updateNavigationState();
    }

    private void navigatePages(int direction) {
        currentPageIndex += direction;
        loadContent();
        logger.info("Navigated to page {} of book '{}'", currentPageIndex + 1, book.getTitle());
    }

    private void updateNavigationState() {
        List<Page> pages = book.getPages();
        leftButton.setEnabled(currentPageIndex > 0);
        rightButton.setEnabled(pages != null && currentPageIndex < pages.size() - 1);
    }

    private void performSearch() {
        String searchTerm = searchField.getText().trim();
        List<Page> pages = book.getPages();
        if (!searchTerm.isEmpty() && pages != null) {
            for (int i = 0; i < pages.size(); i++) {
                if (pages.get(i).getContent().contains(searchTerm)) {
                    currentPageIndex = i;
                    loadContent();
                    logger.info("Search term '{}' found on page {} of book '{}'", searchTerm, currentPageIndex + 1, book.getTitle());
                    return;
                }
            }
            logger.warn("Search term '{}' not found in book '{}'", searchTerm, book.getTitle());
            JOptionPane.showMessageDialog(this, "No matches found.", "Search", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private void handleExport(ActionEvent e) {
        if(bookFacade.isDatabaseConnected())
        {
            bookFacade.exportBook(book.getTitle());
        }
        else
        {
            bookFacade.exportBook(book);
        }
        
        logger.info("Exported book '{}'", book.getTitle());
    }

    private void handleTransliterate(ActionEvent e) {
        String currentText = textArea.getSelectedText();
        if (currentText != null && !currentText.isEmpty()) {
            String transliterated = bookFacade.transliterate(currentText);
            if (transliterated != null) {
                textArea.replaceSelection(transliterated);
                logger.info("Transliterated selected text in book '{}'", book.getTitle());
            }
        } else {
            JOptionPane.showMessageDialog(this, "Please select text to transliterate.", "Transliteration", JOptionPane.INFORMATION_MESSAGE);
            logger.warn("No text selected for transliteration in book '{}'", book.getTitle());
        }
    }
    
    private void handleAnalyzeWord(ActionEvent e) {
    String selectedWord = textArea.getSelectedText();
    if (selectedWord != null && !selectedWord.isEmpty()) {
        String analysisResult = bookFacade.analyzeWord(selectedWord);
        if (analysisResult != null) {
            JOptionPane.showMessageDialog(this, analysisResult, "Word Analysis", JOptionPane.INFORMATION_MESSAGE);
            logger.info("Analyzed word '{}' in book '{}'", selectedWord, book.getTitle());
        }
    } else {
        JOptionPane.showMessageDialog(this, "Please select a word to analyze.", "Word Analysis", JOptionPane.INFORMATION_MESSAGE);
        logger.warn("No word selected for analysis in book '{}'", book.getTitle());
    }
}

    private void handleRealTimeContentUpdate() {
        List<Page> pages = book.getPages();
        if (pages == null) {
            pages = new ArrayList<>();
            book.setPages(pages);
        }

        if (pages.isEmpty()) {
            Page newPage = new Page();
            newPage.setContent(textArea.getText());
            pages.add(newPage);
            bookFacade.addPageByBookTitle(book.getTitle(), newPage);
            logger.info("Added new page to book '{}'", book.getTitle());
        } else if (currentPageIndex < pages.size()) {
            Page currentPage = pages.get(currentPageIndex);
            currentPage.setContent(textArea.getText());
            bookFacade.updateBook(book);
            logger.info("Updated content of page {} in book '{}'", currentPageIndex + 1, book.getTitle());
        }
    }

    public static void showBook(Book book, BookFacade bookFacade) {
        if (book != null) {
            SwingUtilities.invokeLater(() -> {
                BookUI bookUI = new BookUI(book, bookFacade);
                bookUI.setVisible(true);
            });
        }
    }
}
