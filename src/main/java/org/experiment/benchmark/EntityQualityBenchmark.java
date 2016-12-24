package org.experiment.benchmark;

import org.apache.lucene.benchmark.quality.*;
import org.apache.lucene.benchmark.quality.utils.DocNameExtractor;
import org.apache.lucene.benchmark.quality.utils.SubmissionReport;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.experiment.wikipedia.processor.TagMeWikiHelper;
import org.utils.Utilities;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;

/**
 * Created by sonic on 18/12/16.
 */
public class EntityQualityBenchmark extends QualityBenchmark {

    public static final int numOfResults = 10;
    /** maximal number of queries that this quality benchmark runs. Default: maxint. Useful for debugging. */
    private int maxQueries = Integer.MAX_VALUE;

    /** maximal number of results to collect for each query. Default: 1000. */
    private int maxResults = 1000;

    /**
     * Create a QualityBenchmark.
     *
     * @param qqs          quality queries to run.
     * @param qqParser     parser for turning QualityQueries into Lucene Queries.
     * @param searcher     index to be searched.
     * @param docNameField name of field containing the document name.
     *                     This allows to extract the doc name for search results,
     */
    public EntityQualityBenchmark(QualityQuery[] qqs, QualityQueryParser qqParser, IndexSearcher searcher, String docNameField) {
        super(qqs, qqParser, searcher, docNameField);
    }

    /**
     * Run the quality benchmark.
     * @param judge the judge that can tell if a certain result doc is relevant for a certain quality query.
     *        If null, no judgements would be made. Usually null for a submission run.
     * @param submitRep submission report is created if non null.
     * @param qualityLog If not null, quality run data would be printed for each query.
     * @return QualityStats of each quality query that was executed.
     * @throws Exception if quality benchmark failed to run.
     */
    public  QualityStats[] execute(Judge judge, SubmissionReport submitRep,
                                   PrintWriter qualityLog) throws Exception {
        int nQueries = Math.min(maxQueries, qualityQueries.length);
        QualityStats stats[] = new QualityStats[nQueries];
        for (int i=0; i<nQueries; i++) {
            QualityQuery qq = qualityQueries[i];
            // generate query
            Query q = qqParser.parse(qq);
            // search with this query
            long t1 = System.currentTimeMillis();
            TopDocs td = searcher.search(q,maxResults);
            long searchTime = System.currentTimeMillis()-t1;

            // Get top 100 in the list
            ScoreDoc sd[] = td.scoreDocs;
            long t2 = System.currentTimeMillis(); // extraction of first doc name we measure also construction of doc name extractor, just in case.
            DocNameExtractor xt = new DocNameExtractor(docNameField);
            ArrayList <Integer> documentIDs = new ArrayList<>();
            ArrayList <Float> documentScore = new ArrayList<>();
            for (int j = 0; j < numOfResults; j++) {
                String docName = xt.docName(searcher,sd[j].doc);

                long docNameExtractTime = System.currentTimeMillis() - t2;
                t2 = System.currentTimeMillis();
                //boolean isRelevant = judge.isRelevant(docName,qq);
                //stts.addResult(i+1,isRelevant, docNameExtractTime);
                documentIDs.add(sd[j].doc);
                documentScore.add(sd[j].score);
            }
            System.out.println(documentIDs);
            System.out.println(documentScore);
            //

            //most likely we either submit or judge, but check both
            if (judge!=null) {
                stats[i] = analyzeQueryResults(qq, q, td, judge, qualityLog, searchTime);
            }
            if (submitRep!=null) {
                submitRep.report(qq,td,docNameField,searcher);
            }





            //Utilities.writeToFile("test-data/query-results.txt" , Utilities.listToString(documentIDs));


        }
        if (submitRep!=null) {
            submitRep.flush();
        }
        return stats;
    }

    /* Analyze/judge results for a single quality query; optionally log them. */
    private QualityStats analyzeQueryResults(QualityQuery qq, Query q, TopDocs td, Judge judge, PrintWriter logger, long searchTime) throws IOException {
        QualityStats stts = new QualityStats(judge.maxRecall(qq),searchTime);
        ScoreDoc sd[] = td.scoreDocs;
        long t1 = System.currentTimeMillis(); // extraction of first doc name we measure also construction of doc name extractor, just in case.
        DocNameExtractor xt = new DocNameExtractor(docNameField);
        for (int i=0; i<sd.length; i++) {
            String docName = xt.docName(searcher,sd[i].doc);
            long docNameExtractTime = System.currentTimeMillis() - t1;
            t1 = System.currentTimeMillis();
            boolean isRelevant = judge.isRelevant(docName,qq);
            stts.addResult(i+1,isRelevant, docNameExtractTime);
        }
        if (logger!=null) {
            logger.println(qq.getQueryID()+"  -  "+q);
            stts.log(qq.getQueryID()+" Stats:",1,logger,"  ");
        }
        return stts;
    }


}
