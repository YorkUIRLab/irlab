package org.experiment.word2vec;

import com.sampullara.cli.Args;
import com.sampullara.cli.Argument;
import org.deeplearning4j.models.embeddings.loader.WordVectorSerializer;
import org.deeplearning4j.models.word2vec.Word2Vec;
import org.deeplearning4j.text.sentenceiterator.LineSentenceIterator;
import org.deeplearning4j.text.sentenceiterator.SentenceIterator;
import org.deeplearning4j.text.sentenceiterator.SentencePreProcessor;
import org.deeplearning4j.text.tokenization.tokenizer.preprocessor.CommonPreprocessor;
import org.deeplearning4j.text.tokenization.tokenizerfactory.DefaultTokenizerFactory;
import org.deeplearning4j.text.tokenization.tokenizerfactory.TokenizerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;



/**
 * Created by sonic on 21/09/16.
 */
public class WordEmbedding {

    private static Logger log = LoggerFactory.getLogger(WordEmbedding.class);

    @Argument(alias = "f", description = "File containing TREC documents line format", required = true)
    private static String filePath;

    @Argument(alias = "v", description = "Vector File", required = true)
    private static String outputPath;


    public static void main(String[] args) throws Exception {

        // arguments
        try {
            Args.parse(WordEmbedding.class, args);
        } catch (IllegalArgumentException e) {
            Args.usage(WordEmbedding.class);
            System.exit(1);
        }

       // String filePath = "dataset/TREC/word2vec/WT2GLine.gz";
       // String outputPath = "dataset/TREC/word2vec/WT2G-Word2Vec.gz";

        log.info("Load & Vectorize Sentences....");
        // Strip white space before and after for each line
        //SentenceIterator iter = new BasicLineIterator(filePath);
        // Split on white spaces in the line to get words
        TokenizerFactory t = new DefaultTokenizerFactory();
        t.setTokenPreProcessor(new CommonPreprocessor());

        SentenceIterator iter = new LineSentenceIterator(new File(filePath));
        iter.setPreProcessor(new SentencePreProcessor() {
            @Override
            public String preProcess(String sentence) {
                return sentence.toLowerCase();
            }
        });

        log.info("Building model....");
        Word2Vec vec = new Word2Vec.Builder()
                .minWordFrequency(5)
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
        WordVectorSerializer.writeWordVectors(vec, outputPath);

    }


}
