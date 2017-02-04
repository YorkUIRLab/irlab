package org.experiment.word2vec;

import org.deeplearning4j.models.embeddings.reader.impl.BasicModelUtils;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.ops.transforms.Transforms;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by sonic on 01/01/17.
 */
public class MyModelUtils extends BasicModelUtils {

    private static final Logger log = LoggerFactory.getLogger(BasicModelUtils.class);

    /**
     * Returns the similarity of 2 words. Result value will be in range [-1,1], where -1.0 is exact opposite similarity, i.e. NO similarity, and 1.0 is total match of two word vectors.
     * However, most of time you'll see values in range [0,1], but that's something depends of training corpus.
     *
     * Returns NaN if any of labels not exists in vocab, or any label is null
     *
     * @param label1 the first word
     * @param label2 the second word
     * @return a normalized similarity (cosine similarity)
     */
    @Override
    public double similarity( String label1, String label2) {
        if (label1 == null || label2 == null) {
            //log.debug("LABELS: " + label1 + ": " + (label1 == null ? "null": EXISTS)+ ";" + label2 +" vec2:" + (label2 == null ? "null": EXISTS));
            return Double.NaN;
        }

        if (lookupTable.vector(label1) == null || lookupTable.vector(label2) == null) {
            //log.debug("LABELS: " + label1 + ": " + (lookupTable.vector(label1) == null ? "null": EXISTS)+ ";" + label2 +" vec2:" + (lookupTable.vector(label2) == null ? "null": EXISTS));
            return Double.NaN;
        }

        INDArray vec1 = lookupTable.vector(label1).dup();
        INDArray vec2 = lookupTable.vector(label2).dup();


        if (vec1 == null || vec2 == null) {
            log.debug(label1 + ": " + (vec1 == null ? "null": EXISTS)+ ";" + label2 +" vec2:" + (vec2 == null ? "null": EXISTS));
            return Double.NaN;
        }

        if (label1.equals(label2)) return 1.0;

        return Transforms.cosineSim(vec1, vec2);
    }

}
