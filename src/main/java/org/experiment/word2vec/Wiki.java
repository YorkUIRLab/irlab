package org.experiment.word2vec;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.benchmark.byTask.feeds.DocMaker;
import org.apache.lucene.benchmark.byTask.feeds.EnwikiContentSource;
import org.apache.lucene.benchmark.byTask.utils.Config;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.FSDirectory;
import org.deeplearning4j.models.embeddings.loader.WordVectorSerializer;
import org.deeplearning4j.models.word2vec.Word2Vec;
import org.deeplearning4j.text.sentenceiterator.FileSentenceIterator;
import org.deeplearning4j.text.sentenceiterator.LineSentenceIterator;
import org.deeplearning4j.text.sentenceiterator.SentenceIterator;
import org.deeplearning4j.text.sentenceiterator.SentencePreProcessor;
import org.deeplearning4j.text.tokenization.tokenizer.preprocessor.CommonPreprocessor;
import org.deeplearning4j.text.tokenization.tokenizer.preprocessor.StemmingPreprocessor;
import org.deeplearning4j.text.tokenization.tokenizerfactory.DefaultTokenizerFactory;
import org.deeplearning4j.text.tokenization.tokenizerfactory.TokenizerFactory;
import org.deeplearning4j.util.SerializationUtils;
import org.nd4j.linalg.factory.Nd4j;

import java.io.File;
import java.nio.file.Paths;
import java.util.Properties;

/**
 * Created by sonic on 08/10/16.
 * https://medium.com/@klintcho/training-a-word2vec-model-for-swedish-e14b15be6cb#.3e7hylvta
 */
public class Wiki {

    public static void main(String[] args) throws Exception {

        if (args.length <= 1) {
            System.out.println("Param WikiExtract location missing");
            return;
        }

        //String wikiText = "wikitext.txt";
        String wikiText = args[0];
        int layerSize = 300;

        System.out.println("Load & Vectorize Sentences....");
        // Strip white space before and after for each line
        //SentenceIterator iter = new BasicLineIterator(filePath);
        // Split on white spaces in the line to get words
        TokenizerFactory t = new DefaultTokenizerFactory();
       // t.setTokenPreProcessor(new CommonPreprocessor());

        t.setTokenPreProcessor(new StemmingPreprocessor()); //Porter

        SentenceIterator iter = new FileSentenceIterator(new File(wikiText));
        iter.setPreProcessor(new SentencePreProcessor() {
            @Override
            public String preProcess(String sentence) {
                return sentence.toLowerCase();
            }
        });

        Word2Vec vec = new Word2Vec.Builder()
                .sampling(1e-5)
                .minWordFrequency(5)
                .batchSize(1000)
                .useAdaGrad(false)
                .layerSize(layerSize)
                .iterations(3)
                .learningRate(0.025)
                .minLearningRate(1e-2)
                .negativeSample(10)
                .iterate(iter)
                .tokenizerFactory(t)
                .build();

        System.out.println("Fitting...");
        long start = System.currentTimeMillis();
        vec.fit();
        long finish = System.currentTimeMillis();
        System.out.println("Fitting Word2Vec took " + (finish - start) + " ms");

        Nd4j.ENFORCE_NUMERICAL_STABILITY = true;

        System.out.println("Saving model");
        SerializationUtils.saveObject(vec, new File("/home/datasets/wikipedia/w2v_model.ser"));
        WordVectorSerializer.writeWordVectors(vec, "/home/datasets/wikipedia/w2v_vectors.txt");
    }

}
