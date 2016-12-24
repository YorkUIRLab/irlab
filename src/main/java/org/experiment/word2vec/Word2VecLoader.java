package org.experiment.word2vec;

import com.sampullara.cli.Args;
import com.sampullara.cli.Argument;
import org.deeplearning4j.models.embeddings.loader.WordVectorSerializer;
import org.deeplearning4j.models.embeddings.wordvectors.WordVectors;
import org.deeplearning4j.models.word2vec.*;
import org.deeplearning4j.plot.BarnesHutTsne;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.UnsupportedEncodingException;
import java.util.Collection;

/**
 * Created by sonic on 22/10/16.
 */
public class Word2VecLoader {

    @Argument(alias = "v", description = "Word2VecLoader model location", required = true)
    private static String fullVectorModel;

    public static void main(String[] args) {

        try {
            Args.parse(Word2VecLoader.class, args);
        } catch (IllegalArgumentException e) {
            Args.usage(Word2VecLoader.class);
            System.exit(1);
        }

        try {
            //WordVectors wordVectors = WordVectorSerializer.loadTxtVectors(new File(vectorModel));

            String text = "wast water treatment high blood pressur thereaft charg";

            // System.out.println(wordVectors.similarity("wast", "water"));

            Word2Vec word2Vec = WordVectorSerializer.loadFullModel(fullVectorModel);

            for (String x : text.split(" ")) {
                Collection<String> lst4 = word2Vec.wordsNearest(x, 10);
                System.out.println(lst4);
            }

//            System.out.println("Plot TSNE....");
//            BarnesHutTsne tsne = new BarnesHutTsne.Builder()
//                    .setMaxIter(100)
//                    .stopLyingIteration(250)
//                    .learningRate(500)
//                    .useAdaGrad(true)
//                    .theta(0.5)
//                    .setMomentum(0.5)
//                    .normalize(true)
//                    .usePca(false)
//                    .build();
//
//            wordVectors.lookupTable().plotVocab(tsne, 100, new File ("dataset/TREC/Word2VecPlot") );

        } catch (FileNotFoundException  e) {
            e.printStackTrace();
        }

    }
}
