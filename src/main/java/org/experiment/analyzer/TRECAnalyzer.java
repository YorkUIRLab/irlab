package org.experiment.analyzer;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.core.LowerCaseFilter;
import org.apache.lucene.analysis.core.StopFilter;
import org.apache.lucene.analysis.en.PorterStemFilter;
import org.apache.lucene.analysis.standard.StandardTokenizer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.util.CharArraySet;
import org.utils.Utilities;

import java.io.IOException;
import java.util.ArrayList;

/**
 * Created by sonic on 18/10/16.
 *
 * @Author Fanghong
 */
public class TRECAnalyzer extends Analyzer  {

    private int maxTokenLength  = 255;

    private static CharArraySet stopWords;

    static {
        stopWords = Utilities.initStopWords();
    }

    @Override
    protected TokenStreamComponents createComponents(String fieldName) {
        final StandardTokenizer tokenizer = new StandardTokenizer();
        tokenizer.setMaxTokenLength(maxTokenLength);
        TokenFilter lowerCaseFilter = new LowerCaseFilter( tokenizer);
        TokenFilter stopFilter = new StopFilter(lowerCaseFilter, stopWords);
        TokenFilter stemFilter = new PorterStemFilter(stopFilter);

        return new TokenStreamComponents(tokenizer, stemFilter);
    }


    public static ArrayList<String> getTermList(Analyzer analyzer, String text) throws IOException {

        ArrayList<String> result = new ArrayList<String>();

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
        System.out.println(getTermList(new TRECAnalyzer(), "despite opposition from radicals _ because the country's fast-growing population is imposing strains on a struggling economy."));
    }

}
