package org.experiment.word2vec;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.LineIterator;
import org.deeplearning4j.text.sentenceiterator.BaseSentenceIterator;

import java.io.IOException;
import java.io.InputStream;

public  class CustomLineSentenceIterator extends BaseSentenceIterator {

    private InputStream file;
    private LineIterator iter;
    private InputStream inputStreamTemp;

    public CustomLineSentenceIterator(InputStream gzipStream ) {
        try {
            this.file = gzipStream;
            this.inputStreamTemp = gzipStream;
            iter = IOUtils.lineIterator(this.file, "UTF-8");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String nextSentence() {
        String line = iter.nextLine();
        if (preProcessor != null) {
            line = preProcessor.preProcess(line);
        }
        return line;
    }

    @Override
    public boolean hasNext() {
        return iter.hasNext();
    }

    @Override
    public void reset() {
        try {
            if(file != null)
            //    file.close();
            if(iter != null)
             //   iter.close();
            iter = IOUtils.lineIterator(this.file, "UTF-8");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

}
