package org.experiment.analyzer;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.core.LowerCaseFilter;
import org.apache.lucene.analysis.core.StopAnalyzer;
import org.apache.lucene.analysis.core.StopFilter;
import org.apache.lucene.analysis.en.PorterStemFilter;
import org.apache.lucene.analysis.miscellaneous.LengthFilter;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.util.CharArraySet;
import org.apache.lucene.analysis.wikipedia.WikipediaTokenizer;
import org.utils.Utilities;

import java.io.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by sonic on 07/12/16.
 */
public class WikiPediaAnalyzer extends Analyzer {

    private static CharArraySet stopWords;

    static {
        stopWords = Utilities.initStopWords();
    }

    @Override
    protected Analyzer.TokenStreamComponents createComponents(String fieldName) {
        final WikipediaTokenizer tokenizer = new WikipediaTokenizer();

        TokenFilter lowerCaseFilter = new LowerCaseFilter( tokenizer);
        //TokenFilter LengthFilter = new LengthFilter(lowerCaseFilter, 2, 20); is needed?
        TokenFilter stopFilter = new StopFilter( lowerCaseFilter, stopWords);
        TokenFilter stemFilter = new PorterStemFilter(stopFilter);

        return new Analyzer.TokenStreamComponents(tokenizer, stemFilter);
    }



    public static String getTermList(Analyzer analyzer, String text) throws IOException {

        String result = "";
        CharTermAttribute ta;
        try {
            TokenStream ts = analyzer.tokenStream(null, text);
            ts.reset();

            while(ts.incrementToken()) {
                ta = ts.getAttribute(CharTermAttribute.class);
                result += ta.toString().trim().replaceAll("(\\r|\\n|\\t)", " ") + " ";
            }
            ts.reset();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return result;
    }

    public static void main(String[] args) throws IOException {
        System.out.println(getTermList(new WikiPediaAnalyzer(), "j sadsda dasdasd dsdadasd ssss dssdsds"));
    }
}
