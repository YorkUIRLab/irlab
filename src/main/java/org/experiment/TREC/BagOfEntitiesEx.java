package org.experiment.TREC;

/**
 * Created by sonic on 06/09/16.
 */

import org.apache.lucene.benchmark.byTask.feeds.DocMaker;
import org.apache.lucene.benchmark.quality.*;
import org.apache.lucene.benchmark.quality.trec.TrecJudge;
import org.apache.lucene.benchmark.quality.trec.TrecTopicsReader;
import org.apache.lucene.benchmark.quality.utils.SubmissionReport;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.similarities.*;
import org.apache.lucene.store.FSDirectory;
import org.experiment.benchmark.EntityQualityBenchmark;
import org.experiment.query.TRECQQParser;
import org.experiment.tagme4j.TagMeQueryExpander;
import org.utils.Utilities;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.PrintWriter;
import java.nio.file.Paths;
import java.text.NumberFormat;
import java.util.Locale;

public class BagOfEntitiesEx {

    public static void main(String[] args) throws Throwable {
        String index = "index";
        String simstring = "bm25";
        String field = "contents";
        String docNameField = "docno";
        File topicsFile = new File("/media/sonic/Windows/TREC/WT2G/topics/topics.wt2g");
        File qrelsFile = new File("/media/sonic/Windows/TREC/WT2G/Golden Standard/qrels.wt2g");

        Similarity simfn = Utilities.getSimilarity(simstring);

        IndexReader reader = DirectoryReader.open(FSDirectory.open(Paths.get(index)));
        IndexSearcher searcher = new IndexSearcher(reader);
        searcher.setSimilarity(simfn);

        PrintWriter logger  = new PrintWriter("test-data/1-" + simstring + "-wiki-entity-report1.out", "UTF-8");
        PrintWriter logger2 = new PrintWriter("test-data/1-" + simstring + "-wiki-entity-report2.out", "UTF-8");
        PrintWriter logger3 = new PrintWriter("test-data/1-" + simstring + "-wiki-entity-report3.out", "UTF-8");
        PrintWriter logger4 = new PrintWriter("test-data/1-" + simstring + "-wiki-entity-report4.out", "UTF-8");

        TrecTopicsReader qReader = new TrecTopicsReader();
        QualityQuery qqs[] = qReader.readQueries(new BufferedReader(new FileReader(topicsFile)));

        // Update queries using TagMeQueryExpander entity linking to Wiki and Word2Vec
        TagMeQueryExpander tagMeQueryExpander = new TagMeQueryExpander();
        //qqs = tagMeQueryExpander.updateQuery(qqs);

        Judge judge = new TrecJudge(new BufferedReader(new FileReader(qrelsFile)));

        judge.validateData(qqs, logger);

        QualityQueryParser qqParser = new TRECQQParser("title", field);

        //QualityBenchmark qrun = new QualityBenchmark(qqs, qqParser, searcher, docNameField);
        EntityQualityBenchmark qrun = new EntityQualityBenchmark(qqs, qqParser, searcher, docNameField);
        SubmissionReport submitLog = new SubmissionReport(logger2, simstring);

        //TODO
        QualityStats stats[] = qrun.execute(judge, submitLog, logger);

        NumberFormat nf = NumberFormat.getInstance(Locale.ROOT);
        nf.setMaximumFractionDigits(4);
        nf.setMinimumFractionDigits(4);
        nf.setGroupingUsed(true);


        QualityStats qualityStats = QualityStats.average(stats);
        logger3.println("MAP : "  + nf.format( qualityStats.getAvp()));
        logger3.println("Average Precision @ 1: "  + nf.format( qualityStats.getPrecisionAt(1)));
        logger3.println("Average Precision @ 5: "  + nf.format( qualityStats.getPrecisionAt(5)));
        logger3.println("Average Precision @ 10: " + nf.format( qualityStats.getPrecisionAt(10)));
        logger3.println("Average Precision @ 20: " + nf.format( qualityStats.getPrecisionAt(20)));

        logger3.println ("==========================");
        int qualityQuery = 1;
        for (QualityStats qualityStat : stats) {
            logger3.println("Quality Stat: " + qualityQuery);
            logger3.println("MAP:  "  + nf.format( qualityStat.getAvp()) );
            logger3.println("P@1:  "  + nf.format( qualityStat.getPrecisionAt(1)));
            logger3.println("P@5:  "  + nf.format( qualityStat.getPrecisionAt(5)));
            logger3.println("P@10: " + nf.format( qualityStat.getPrecisionAt(10)));
            logger3.println("P@20: " + nf.format( qualityStat.getPrecisionAt(20)));
            logger3.println("=========================");
            qualityQuery++;
        }

        QualityStats avg = QualityStats.average(stats);		// #6 Print precision and recall measures
        QualityStats.RecallPoint[] rp = avg.getRecallPoints();
        avg.log("SUMMARY -- " + simstring, 2, logger4, "  ");

	    logger.close();
        logger2.close();
        logger3.close();
        logger4.close();

//        for ( i = 0; i <hits.length; i++) {
            int docId = 164859;
            Document d = searcher.doc(docId);
            System.err.println( d.get("contents") );
//        }

        //todo
        // get
    }
}
