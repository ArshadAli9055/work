package util;

import dto.Book;
import dto.Page;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.*;
import org.apache.lucene.index.*;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.MMapDirectory;
import org.apache.lucene.util.BytesRef;
import java.nio.file.Files;
import java.nio.file.Path;

public class TFIDFAnalyzer {

    private static volatile TFIDFAnalyzer instance;
    
    private TFIDFAnalyzer() {
        // Initialization logic here (if any)
    }
    
    public static TFIDFAnalyzer getInstance() {
        TFIDFAnalyzer instance = TFIDFAnalyzer.instance;
        if (instance == null) {
            synchronized (TFIDFAnalyzer.class) {
                instance = TFIDFAnalyzer.instance;
                if (instance == null) {
                    TFIDFAnalyzer.instance = instance = new TFIDFAnalyzer();
                }
            }
        }
        return instance;
    }
    
    public String calculateTFIDF(Book book) {
        try {
            Path tempDir = Files.createTempDirectory("lucene-tfidf");
            Directory directory = new MMapDirectory(tempDir);
            Analyzer analyzer = new StandardAnalyzer();
            IndexWriterConfig config = new IndexWriterConfig(analyzer);
            
            try (IndexWriter writer = new IndexWriter(directory, config)) {
                for (Page page : book.getPages()) {
                    Document doc = new Document();
                    doc.add(new TextField("content", page.getContent(), Field.Store.YES));
                    writer.addDocument(doc);
                }
            }

            try (DirectoryReader reader = DirectoryReader.open(directory)) {
                List<TermScore> termScores = new ArrayList<>();

                for (LeafReaderContext leafContext : reader.leaves()) {
                    LeafReader leafReader = leafContext.reader();
                    Terms terms = leafReader.terms("content");

                    if (terms != null) {
                        TermsEnum termsEnum = terms.iterator();
                        BytesRef term;

                        while ((term = termsEnum.next()) != null) {
                            String termText = term.utf8ToString();
                            long docFreq = termsEnum.docFreq();
                            long totalDocs = reader.numDocs();
                            double idf = Math.log((double) totalDocs / (1 + docFreq));

                            int tf = 0;
                            PostingsEnum postings = termsEnum.postings(null, PostingsEnum.FREQS);
                            while (postings.nextDoc() != PostingsEnum.NO_MORE_DOCS) {
                                tf += postings.freq();
                            }

                            double tfidf = tf * idf;
                            termScores.add(new TermScore(termText, tfidf));
                        }
                    }
                }

                termScores.sort((a, b) -> Double.compare(b.score, a.score));

                StringBuilder result = new StringBuilder();
                result.append("Top 20 terms by TF-IDF:\n");
                for (int i = 0; i < Math.min(20, termScores.size()); i++) {
                    TermScore ts = termScores.get(i);
                    result.append(String.format("%d. %s (TF-IDF: %.4f)\n", i + 1, ts.term, ts.score));
                }

                return result.toString();
            } finally {
                directory.close();
                Files.walk(tempDir)
                     .map(Path::toFile)
                     .forEach(file -> {
                         if (!file.delete()) {
                             file.deleteOnExit();
                         }
                     });
            }
        } catch (IOException e) {
            return "Error during TF-IDF analysis: " + e.getMessage();
        }
    }

    private static class TermScore {
        String term;
        double score;

        TermScore(String term, double score) {
            this.term = term;
            this.score = score;
        }
    }
}
