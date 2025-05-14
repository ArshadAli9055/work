package util;

import dto.Book;
import dto.Page;
import java.util.*;
import java.util.stream.Collectors;

public class PKLAnalyzer {

    private static volatile PKLAnalyzer instance;
    private static final int TOP_PAIR_LIMIT = 20;
    private static final Set<String> ARABIC_STOPWORDS = Set.of("و", "في", "على", "من", "إلى", "عن", "ما", "مع");

    private PKLAnalyzer() {
        // Initialization logic here (if any)
    }
    
    public static PKLAnalyzer getInstance() {
        PKLAnalyzer instance = PKLAnalyzer.instance;
        if (instance == null) {
            synchronized (PKLAnalyzer.class) {
                instance = PKLAnalyzer.instance;
                if (instance == null) {
                    PKLAnalyzer.instance = instance = new PKLAnalyzer();
                }
            }
        }
        return instance;
    }
    
    public String calculatePKL(Book book) {
        if (book == null || book.getPages() == null || book.getPages().isEmpty()) {
            return "No content available in the book to calculate PKL.";
        }

        Map<String, Integer> wordCounts = new HashMap<>();
        Map<String, Integer> pairCounts = new HashMap<>();
        int totalWords = 0;

        for (Page page : book.getPages()) {
            String content = page.getContent();
            if (content != null && !content.isEmpty()) {
                List<String> words = tokenize(content);

                for (String word : words) {
                    wordCounts.put(word, wordCounts.getOrDefault(word, 0) + 1);
                }
                totalWords += words.size();

                for (int i = 0; i < words.size(); i++) {
                    for (int j = i + 1; j < words.size(); j++) {
                        String pair = makePair(words.get(i), words.get(j));
                        pairCounts.put(pair, pairCounts.getOrDefault(pair, 0) + 1);
                    }
                }
            }
        }

        if (totalWords == 0) {
            return "No valid words found in the book content to calculate PKL.";
        }

        Map<String, Double> pklScores = new HashMap<>();
        for (Map.Entry<String, Integer> entry : pairCounts.entrySet()) {
            String pair = entry.getKey();
            int pairCount = entry.getValue();

            String[] words = pair.split("\\|");
            int countA = wordCounts.getOrDefault(words[0], 0);
            int countB = wordCounts.getOrDefault(words[1], 0);

            if (countA > 0 && countB > 0) {
                double pPair = (double) pairCount / totalWords;
                double pA = (double) countA / totalWords;
                double pB = (double) countB / totalWords;

                double pkl = pPair * Math.log(pPair / (pA * pB));
                pklScores.put(pair, pkl);
            }
        }

        List<Map.Entry<String, Double>> sortedPKLScores = pklScores.entrySet().stream()
            .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
            .collect(Collectors.toList());

        StringBuilder result = new StringBuilder();
        result.append("Top ").append(TOP_PAIR_LIMIT).append(" word pairs by PKL:\n");
        for (int i = 0; i < Math.min(TOP_PAIR_LIMIT, sortedPKLScores.size()); i++) {
            Map.Entry<String, Double> entry = sortedPKLScores.get(i);
            String[] words = entry.getKey().split("\\|");
            result.append(String.format("%d. (%s, %s) - PKL: %.4f\n", i + 1, words[0], words[1], entry.getValue()));
        }

        return result.toString();
    }

    private List<String> tokenize(String content) {
        return Arrays.stream(normalize(content).split("\\s+"))
                     .filter(word -> word.length() > 1 && !ARABIC_STOPWORDS.contains(word))
                     .collect(Collectors.toList());
    }

    private String normalize(String content) {
        return content.replaceAll("[\u0610-\u061A\u064B-\u065F]", "")
                  .replaceAll("[^\\p{InArabic}]", " ");
    }
    
    private String makePair(String word1, String word2) {
        return word1.compareTo(word2) < 0 ? word1 + "|" + word2 : word2 + "|" + word1;
    }
}
