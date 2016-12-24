package org.experiment.word2vec;

import org.deeplearning4j.models.embeddings.loader.WordVectorSerializer;
import org.deeplearning4j.models.word2vec.Word2Vec;
import org.deeplearning4j.text.sentenceiterator.LineSentenceIterator;
import org.deeplearning4j.text.sentenceiterator.SentenceIterator;
import org.deeplearning4j.text.sentenceiterator.SentencePreProcessor;
import org.deeplearning4j.text.tokenization.tokenizer.preprocessor.CommonPreprocessor;
import org.deeplearning4j.text.tokenization.tokenizerfactory.DefaultTokenizerFactory;
import org.deeplearning4j.text.tokenization.tokenizerfactory.TokenizerFactory;
import org.experiment.wikipedia.processor.TagMeWikiHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;

/**
 * Created by sonic on 07/12/16.
 */
public class WikiWord2Vec {

    private static Logger log = LoggerFactory.getLogger(WikiWord2Vec.class);

    public static void main(String[] args) {

        File dir = new File(TagMeWikiHelper.WIKI_QUERY_EXPANSION_BASE_LOCATION);
        File[] directoryListing = dir.listFiles();
        if (directoryListing != null) {
            for (File child : directoryListing) {
                try {
                    processWord2Vec(child);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } else {

        }

    }

    /**
     *
     * @param filePath
     * @throws IOException
     */
    public static void processWord2Vec (File filePath) throws IOException {

        log.info("Load & Vectorize Sentences....");
        // Strip white space before and after for each line
        // SentenceIterator iter = new BasicLineIterator(filePath);
        // Split on white spaces in the line to get words
        TokenizerFactory t = new DefaultTokenizerFactory();
        t.setTokenPreProcessor(new CommonPreprocessor());


        SentenceIterator iter = new LineSentenceIterator(filePath);
        iter.setPreProcessor(new SentencePreProcessor() {
            @Override
            public String preProcess(String sentence) {
                return sentence.toLowerCase();
            }
        });

        log.info("Building model...." + filePath.getAbsolutePath() );
        Word2Vec vec = new Word2Vec.Builder()
                .minWordFrequency(3)
                .iterations(1)
                .layerSize(100)
                .seed(42)
                .windowSize(5)
                .iterate(iter)
                .tokenizerFactory(t)
                .build();

        log.info("Fitting Word2Vec model....");
        vec.fit();

        log.info("Writing word vectors to text file....");

        // Write word vectors
        WordVectorSerializer.writeWordVectors(vec, TagMeWikiHelper.WIKI_QUERY_EXPANSION_BASE_LOCATION + filePath.getName() + "_WordVectors.txt");

        // Write Full Model
        WordVectorSerializer.writeFullModel(vec, TagMeWikiHelper.WIKI_QUERY_EXPANSION_BASE_LOCATION + filePath.getName() + "_Word2Vec-full.txt");
    }
}
