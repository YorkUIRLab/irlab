package org.experiment.analyzer;

import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.core.LowerCaseFilter;
import org.apache.lucene.analysis.core.StopAnalyzer;
import org.apache.lucene.analysis.core.StopFilter;
import org.apache.lucene.analysis.en.PorterStemFilter;
import org.apache.lucene.analysis.standard.StandardTokenizer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.util.CharArraySet;

/**
 * Created by sonic on 18/10/16.
 *
 * @Author Fanghong
 */
public class TRECAnalyzer extends Analyzer  {

    private int maxTokenLength  = 255;

    private static CharArraySet stopWords;

    static {
        initStopWords();
    }

    @Override
    protected TokenStreamComponents createComponents(String fieldName) {
        final StandardTokenizer tokenizer = new StandardTokenizer();
        tokenizer.setMaxTokenLength(maxTokenLength);

        TokenFilter lowerCaseFilter = new LowerCaseFilter( tokenizer);
        TokenFilter stopFilter = new StopFilter( lowerCaseFilter, stopWords);
        //TokenFilter lemmaFilter = new LemmaFilter (stopFilter);
        TokenFilter stemFilter = new PorterStemFilter(stopFilter);

        return new TokenStreamComponents(tokenizer, stemFilter);
    }

    private static final void initStopWords() {

        LineNumberReader reader = null;
        Set<String> set = new HashSet<String>();
        try {
            reader = new LineNumberReader(new FileReader("stopWords/stopwords.txt"));
            String stopWord = null;
            while ((stopWord = reader.readLine()) != null) {
                set.add(stopWord.trim());
            }
            stopWords = new CharArraySet(set, true);
            reader.close();
        } catch (IOException e) {
            System.err.println("There is no stopwords");
            stopWords = StopAnalyzer.ENGLISH_STOP_WORDS_SET;
        }
    }


    public static ArrayList<String> getTermList(Analyzer analyzer, String text) throws IOException {

        ArrayList<String> result = new ArrayList<String>();

        Reader reader = new StringReader(text);

        try {
            TokenStream ts = analyzer.tokenStream(null, text);
            ts.reset();

            while(ts.incrementToken()) {
                CharTermAttribute ta = ts.getAttribute(CharTermAttribute.class);
                result.add(ta.toString());
            }
            ts.reset();

        } catch (IOException e) {
            e.printStackTrace();
        }

        return result;
    }

    public static void main(String[] args) throws IOException {
        System.out.println(getTermList(new TRECAnalyzer(), "Iran's government is intensifying a birth control program _ despite opposition from radicals _ because the country's fast-growing population is imposing strains on a struggling economy."));
    }

}
