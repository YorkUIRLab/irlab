package org.experiment.TREC;

/**
 * Created by sonic on 06/09/16.
 */
import java.io.File;
import java.io.PrintWriter;
import java.io.BufferedReader;
import java.io.FileReader;
import java.nio.file.Paths;
import java.text.NumberFormat;
import java.util.Locale;

import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.*;
import org.apache.lucene.search.similarities.*;
import org.apache.lucene.store.*;
import org.apache.lucene.benchmark.quality.*;
import org.apache.lucene.benchmark.quality.utils.*;
import org.apache.lucene.benchmark.quality.trec.*;
import org.experiment.query.TRECQQParser;

public class PrecisionRecall {

    public static final String WORD2VEC_MODEL_FULL_TXT = "dataset/TREC/Word2Vec-full.txt";

    public static void main(String[] args) throws Throwable {
        String index = "index";
        String simstring = "bm25";
        String field = "contents";
        String docNameField = "docno";
        File topicsFile = new File("/media/sonic/Windows/TREC/WT2G/topics/topics.wt2g");
        File qrelsFile = new File("/media/sonic/Windows/TREC/WT2G/Golden Standard/qrels.wt2g");

        Similarity simfn = null;
        if ("default".equals(simstring)) {
            simfn = new ClassicSimilarity();
        } else if ("bm25".equals(simstring)) {
            simfn = new BM25Similarity();
        } else if ("dfr".equals(simstring)) {
            simfn = new DFRSimilarity(new BasicModelP(), new AfterEffectL(), new NormalizationH2());
        } else if ("lm".equals(simstring)) {
            simfn = new LMDirichletSimilarity();
        }
        IndexReader reader = DirectoryReader.open(FSDirectory.open(Paths.get(index)));
        IndexSearcher searcher = new IndexSearcher(reader);
        searcher.setSimilarity(simfn);

        PrintWriter logger  = new PrintWriter("test-data/word2vec-precision-recall1.out",  "UTF-8");
        PrintWriter logger2 = new PrintWriter("test-data/word2vec-precision-recall2.out", "UTF-8");
        PrintWriter logger3 = new PrintWriter("test-data/word2vec-precision-recall3.out", "UTF-8");
        PrintWriter logger4 = new PrintWriter("test-data/word2vec-precision-recall4.out", "UTF-8");



        TrecTopicsReader qReader = new TrecTopicsReader();
        QualityQuery updatedQuery[] = qReader.readQueries(new BufferedReader(new FileReader(topicsFile)));


        // WORD2VEC Query Expansion
        Word2VecQuery word2VecQuery = new Word2VecQuery(WORD2VEC_MODEL_FULL_TXT);
        updatedQuery = word2VecQuery.updateQuery(updatedQuery);

        Judge judge = new TrecJudge(new BufferedReader(new FileReader(qrelsFile)));

        judge.validateData(updatedQuery, logger);

        QualityQueryParser qqParser = new TRECQQParser("title", "contents");

        QualityBenchmark qrun = new QualityBenchmark(updatedQuery, qqParser, searcher, docNameField);
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
            logger3.println("P@10: "  + nf.format( qualityStat.getPrecisionAt(10)));
            logger3.println("P@20: "  + nf.format( qualityStat.getPrecisionAt(20)));
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
        //    dir.close();
    }
}
