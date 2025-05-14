package util;

import dto.Book;
import dto.Page;
import java.util.*;
import java.util.stream.Collectors;

public class QualityPhrasesMiner {

    private static volatile QualityPhrasesMiner instance;
    private static final int MIN_PHRASE_LENGTH = 2;
    private static final int MAX_PHRASE_LENGTH = 5;
    private static final int MIN_OCCURRENCES = 2;
    private static final int MAX_TOP_PHRASES = 20;
    private static final Set<String> ARABIC_STOPWORDS = Set.of("و", "في", "على", "من", "إلى", "عن", "ما", "مع");

    private QualityPhrasesMiner() {
        // Initialization logic here (if any)
    }
    
    public static QualityPhrasesMiner getInstance() {
        QualityPhrasesMiner instance = QualityPhrasesMiner.instance;
        if (instance == null) {
            synchronized (QualityPhrasesMiner.class) {
                instance = QualityPhrasesMiner.instance;
                if (instance == null) {
                    QualityPhrasesMiner.instance = instance = new QualityPhrasesMiner();
                }
            }
        }
        return instance;
    }
    
    public String mineQualityPhrases(Book book) {
        Map<String, Integer> phraseFrequency = new HashMap<>();

        for (Page page : book.getPages()) {
            List<String> tokens = tokenize(page.getContent());
            extractPhrases(tokens, phraseFrequency);
        }

        List<Map.Entry<String, Integer>> sortedPhrases = phraseFrequency.entrySet().stream()
                .filter(entry -> entry.getValue() >= MIN_OCCURRENCES)
                .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
                .limit(MAX_TOP_PHRASES)
                .collect(Collectors.toList());

        return formatResults(sortedPhrases);
    }

    private List<String> tokenize(String content) {
        return Arrays.stream(normalize(content).split("\\s+"))
                .filter(token -> !token.isBlank() && !ARABIC_STOPWORDS.contains(token))
                .collect(Collectors.toList());
    }

    private String normalize(String content) {
        return content.replaceAll("[\u0610-\u061A\u064B-\u065F]", "")
                  .replaceAll("[^\\p{InArabic}]", " ");
    } 

    private void extractPhrases(List<String> tokens, Map<String, Integer> phraseFrequency) {
        for (int length = MIN_PHRASE_LENGTH; length <= MAX_PHRASE_LENGTH; length++) {
            for (int i = 0; i <= tokens.size() - length; i++) {
                String phrase = String.join(" ", tokens.subList(i, i + length));
                phraseFrequency.put(phrase, phraseFrequency.getOrDefault(phrase, 0) + 1);
            }
        }
    }

    private String formatResults(List<Map.Entry<String, Integer>> sortedPhrases) {
        if (sortedPhrases.isEmpty()) {
            return "No high-quality phrases found.";
        }

        StringBuilder result = new StringBuilder("Top Quality Phrases:\n");
        for (int i = 0; i < sortedPhrases.size(); i++) {
            Map.Entry<String, Integer> entry = sortedPhrases.get(i);
            result.append(String.format("%d. %s (Frequency: %d)\n", i + 1, entry.getKey(), entry.getValue()));
        }
        return result.toString();
    }
}
