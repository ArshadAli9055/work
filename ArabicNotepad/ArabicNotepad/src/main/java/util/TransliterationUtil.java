package util;

import com.ibm.icu.text.Transliterator;
import java.text.Normalizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TransliterationUtil {

    private static volatile TransliterationUtil instance;
    private static final Logger logger = LoggerFactory.getLogger(TransliterationUtil.class);

    private TransliterationUtil() {
        // Initialization logic here (if any)
    }
    
    public static TransliterationUtil getInstance() {
        TransliterationUtil instance = TransliterationUtil.instance;
        if (instance == null) {
            synchronized (TransliterationUtil.class) {
                instance = TransliterationUtil.instance;
                if (instance == null) {
                    TransliterationUtil.instance = instance = new TransliterationUtil();
                }
            }
        }
        return instance;
    }

    
    private static final String BASIC_RULES = ""
            + "ا > a; "
            + "ب > b; "
            + "ت > t; "
            + "ث > th; "
            + "ج > j; "
            + "ح > h; "
            + "خ > kh; "
            + "د > d; "
            + "ذ > dh; "
            + "ر > r; "
            + "ز > z; "
            + "س > s; "
            + "ش > sh; "
            + "ص > s; "
            + "ض > d; "
            + "ط > t; "
            + "ظ > z; "
            + "ع > e; "
            + "غ > gh; "
            + "ف > f; "
            + "ق > q; "
            + "ك > k; "
            + "ل > l; "
            + "م > m; "
            + "ن > n; "
            + "ه > h; "
            + "و > w; "
            + "ي > y; "
            + "ء > ''; "
            + "ة > h; "
            + "ى > a; "
            + "ؤ > o; "
            + "ئ > e;";

    private static final String DIACRITIC_RULES = ""
            + "اَ > a; "
            + "اُ > u; "
            + "اِ > i; "
            + "ىَ > a; "
            + "ىُ > u; "
            + "ىِ > i; "
            + "ؤَ > o; "
            + "ؤُ > u; "
            + "ؤِ > i; "
            + "ئَ > e; "
            + "ئُ > u; "
            + "ئِ > i;";

    private static final String CUSTOM_RULES = DIACRITIC_RULES + BASIC_RULES;

    private static final Transliterator ARABIC_TO_LATIN_TRANSLITERATOR =
            Transliterator.createFromRules("Arabic-Latin-Custom", CUSTOM_RULES, Transliterator.FORWARD);

    public String translateToRomanEnglish(String arabicText) {
        if (arabicText == null || arabicText.isEmpty()) {
            logger.error("Input Arabic text is null or empty.");
            return "";
        }

        String normalizedText = Normalizer.normalize(arabicText, Normalizer.Form.NFC);

        try {
            String transliteratedText = ARABIC_TO_LATIN_TRANSLITERATOR.transliterate(normalizedText);
            logger.debug("Transliterated text: {}", transliteratedText);
            return transliteratedText;
        } catch (Exception e) {
            logger.error("Error during transliteration of text: {}", arabicText, e);
            return "";
        }
    }
}
