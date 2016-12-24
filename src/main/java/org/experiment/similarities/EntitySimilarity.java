package org.experiment.similarities;

import org.apache.lucene.index.FieldInvertState;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.search.CollectionStatistics;
import org.apache.lucene.search.TermStatistics;
import org.apache.lucene.search.similarities.Similarity;

import java.io.IOException;

/**
 * Created by sonic on 18/12/16.
 */
public class EntitySimilarity  extends Similarity {



    @Override
    public long computeNorm(FieldInvertState state) {
        return 0;
    }

    @Override
    public SimWeight computeWeight(CollectionStatistics collectionStats, TermStatistics... termStats) {
        return null;
    }

    @Override
    public SimScorer simScorer(SimWeight weight, LeafReaderContext context) throws IOException {
        return null;
    }

    @Override
    public String toString() {
        return "EntitySimilarity{}";
    }

}
