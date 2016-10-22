package org.experiment.word2vec;

import com.sampullara.cli.Args;
import com.sampullara.cli.Argument;
import org.deeplearning4j.models.embeddings.loader.WordVectorSerializer;
import org.deeplearning4j.models.word2vec.*;
import java.io.FileNotFoundException;
import java.util.Collection;

/**
 * Created by sonic on 22/10/16.
 */
public class Word2VecLoader {

    @Argument(alias = "v", description = "Word2VecLoader model location", required = true)
    private static String vectorModel;

    public static void main(String[] args) {

        try {
            Args.parse(TRECWord2Vec.class, args);
        } catch (IllegalArgumentException e) {
            Args.usage(TRECWord2Vec.class);
            System.exit(1);
        }

        try {
            Word2Vec word2Vec = WordVectorSerializer.loadFullModel(vectorModel);


            Collection<String> lst3 = word2Vec.wordsNearest("man", 10);
            System.out.println(lst3);

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

    }
}
