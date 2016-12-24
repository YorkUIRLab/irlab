package org.experiment.analyzer;

import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.KeywordAttribute;
import org.experiment.preprocessing.StanfordLemmatizer;

import java.io.IOException;

/**
 * Created by sonic on 18/10/16.
 */
public class LemmaFilter extends TokenFilter {

    private final CharTermAttribute termAtt = addAttribute(CharTermAttribute.class);
    private final KeywordAttribute keywordAttr = addAttribute(KeywordAttribute.class);

    /**
     * Construct a token stream filtering the given input.
     *
     * @param input
     */
    protected LemmaFilter(TokenStream input) {
        super(input);
    }

    @Override
    public boolean incrementToken() throws IOException {
        if (input.incrementToken()) {
          //  StanfordLemmatizer.getInstance().lemmatize(termAtt.buffer(), 0, termAtt.length());
            return true;
        } else
            return false;
    }
}
