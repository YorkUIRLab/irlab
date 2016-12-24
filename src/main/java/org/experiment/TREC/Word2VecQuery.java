package org.experiment.TREC;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.benchmark.quality.QualityQuery;
import org.apache.lucene.benchmark.quality.trec.TrecTopicsReader;
import org.deeplearning4j.models.embeddings.loader.WordVectorSerializer;
import org.deeplearning4j.models.word2vec.Word2Vec;
import org.experiment.analyzer.TRECAnalyzer;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;

/**
 * Created by sonic on 23/10/16.
 */
public class Word2VecQuery {

    protected static Word2Vec word2Vec ;
    Analyzer trecAnalyzer ;

    public Word2VecQuery(String fullVectorModel) {
        try {
            this.word2Vec =  WordVectorSerializer.loadFullModel(fullVectorModel);
            this.trecAnalyzer = new TRECAnalyzer();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {

        try {

            File topicsFile = new File("/media/sonic/Windows/TREC/WT2G/topics/topics.wt2g");
            TrecTopicsReader qReader = new TrecTopicsReader();

            QualityQuery qqs[] = qReader.readQueries(new BufferedReader(new FileReader(topicsFile)));

            Word2VecQuery word2VecQuery = new Word2VecQuery("dataset/TREC/Word2Vec-full.txt");

            QualityQuery updatedQuery[] = word2VecQuery.updateQuery(qqs);

            for (QualityQuery qq : updatedQuery) {
              System.out.println("query title: "  + qq.getValue("title"));
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public QualityQuery[] updateQuery (QualityQuery qqs[]) throws IOException {

        ArrayList<QualityQuery> qualityQueryList = new ArrayList<>();
        //set number of close word
        int nearestWord = 1;

        for (QualityQuery qq : qqs) {
            HashMap<String,String> fields = new HashMap<>();
            String queryTitle = qq.getValue("title");

            for (String query : TRECAnalyzer.getTermList(trecAnalyzer, queryTitle)) {
                Collection<String> lst4 = word2Vec.wordsNearest(query, nearestWord);
                queryTitle = queryTitle.concat(" " + lst4.toString());
            }

            queryTitle = queryTitle.replace("[", "").replace("]", "");
            fields.put("title", queryTitle);
            fields.put("description", qq.getValue("description"));
            fields.put("narrative", qq.getValue("narrative"));
            QualityQuery topic = new QualityQuery(qq.getQueryID(), fields);
            qualityQueryList.add(topic);
        }
        return qualityQueryList.toArray(new QualityQuery[0]);
    }


}








