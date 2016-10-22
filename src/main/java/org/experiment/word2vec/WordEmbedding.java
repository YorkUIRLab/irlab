package org.experiment.word2vec;

import com.sampullara.cli.Args;
import com.sampullara.cli.Argument;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.LineIterator;
import org.deeplearning4j.models.embeddings.loader.WordVectorSerializer;
import org.deeplearning4j.models.word2vec.Word2Vec;
import org.deeplearning4j.text.sentenceiterator.BaseSentenceIterator;
import org.deeplearning4j.text.sentenceiterator.LineSentenceIterator;
import org.deeplearning4j.text.sentenceiterator.SentenceIterator;
import org.deeplearning4j.text.sentenceiterator.SentencePreProcessor;
import org.deeplearning4j.text.tokenization.tokenizer.preprocessor.CommonPreprocessor;
import org.deeplearning4j.text.tokenization.tokenizerfactory.DefaultTokenizerFactory;
import org.deeplearning4j.text.tokenization.tokenizerfactory.TokenizerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.zip.GZIPInputStream;


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

        log.info("Load & Vectorize Sentences....");
        // Strip white space before and after for each line
        // SentenceIterator iter = new BasicLineIterator(filePath);
        // Split on white spaces in the line to get words
        TokenizerFactory t = new DefaultTokenizerFactory();
        t.setTokenPreProcessor(new CommonPreprocessor());

//        InputStream fileStream = new FileInputStream(filePath);
//        InputStream gzipStream = new GZIPInputStream(fileStream);
//        Reader decoder = new InputStreamReader(gzipStream, "UTF-8");
//        BufferedReader rdr = new BufferedReader(decoder);

        SentenceIterator iter = new LineSentenceIterator(new File(filePath));
        iter.setPreProcessor(new SentencePreProcessor() {
            @Override
            public String preProcess(String sentence) {
                return sentence.toLowerCase();
            }
        });

        log.info("Building model....");
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
        WordVectorSerializer.writeWordVectors(vec, outputPath + "WordVectors.txt");

        WordVectorSerializer.writeFullModel(vec, outputPath + "Word2Vec-full.txt");
    }
}

