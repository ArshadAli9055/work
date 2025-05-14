package util;

import net.oujda_nlp_team.AlKhalil2Analyzer;

import java.util.*;
import net.oujda_nlp_team.entity.Result;
import net.oujda_nlp_team.entity.ResultList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WordAnalyzer {

    private static volatile WordAnalyzer instance;
    private static final Logger logger = LoggerFactory.getLogger(WordAnalyzer.class);

    private WordAnalyzer() {
        // Initialization logic here (if any)
    }
    
    public static WordAnalyzer getInstance() {
        WordAnalyzer instance;
        instance = WordAnalyzer.instance;
        if (instance == null) {
            synchronized (WordAnalyzer.class) {
                instance = WordAnalyzer.instance;
                if (instance == null) {
                    WordAnalyzer.instance = instance = new WordAnalyzer();
                }
            }
        }
        return instance;
    }
    
    public String analyzeWord(String word) {
        try {
            AlKhalil2Analyzer analyzer = AlKhalil2Analyzer.getInstance();
            if (analyzer != null) {
	            ResultList result = analyzer.processToken(word);            
	            String lemma = result.getAllLemmasString();
	            String root = result.getAllRootString();
	            List<Result> allResults = result.getAllResults();
	            
	            if (!allResults.isEmpty()) {
	                String pos = allResults.get(0).getPartOfSpeech();
	                return "Root: " + root + "\nPOS: " + pos + "\nLemma: " + lemma;
	            }
            }
        } catch (Exception e) {
            logger.error("Error during analysis of word: ", word, e);     
        }
        return "Error during analysis of word: " + word;
    }
}
