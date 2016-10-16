package org.experiment.TREC;

/**
 * Created by sonic on 06/09/16.
 */
import java.io.File;
import java.io.PrintWriter;
import java.io.BufferedReader;
import java.io.FileReader;
import java.nio.file.Paths;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.*;
import org.apache.lucene.search.similarities.*;
import org.apache.lucene.store.*;
import org.apache.lucene.benchmark.quality.*;
import org.apache.lucene.benchmark.quality.utils.*;
import org.apache.lucene.benchmark.quality.trec.*;

public class PrecisionRecall {

    public static void main(String[] args) throws Throwable {
        String index = "index";
        String simstring = "bm25";
        String field = "contents";
        String docNameField = "docno";
        File topicsFile = new File("/home/sonic/Dev/WT2G/topics/topics.wt2g");
        File qrelsFile = new File("/home/sonic/Dev/WT2G/Golden Standard/qrels.wt2g");
     //   Directory dir = FSDirectory.open(Paths.get("home/sonic/Dev/WT2G/out.txt"));


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

        PrintWriter logger = new PrintWriter("test-data/precision-recall.out", "UTF-8");
        PrintWriter logger2 = new PrintWriter("test-data/precision-recall2.out", "UTF-8");

        TrecTopicsReader qReader = new TrecTopicsReader();
        QualityQuery qqs[] = qReader.readQueries(new BufferedReader(new FileReader(topicsFile)));


        Judge judge = new TrecJudge(new BufferedReader(new FileReader(qrelsFile)));

        judge.validateData(qqs, logger);


        QualityQueryParser qqParser = new SimpleQQParser("title", "contents");

        QualityBenchmark qrun = new QualityBenchmark(qqs, qqParser, searcher, docNameField);
        SubmissionReport submitLog = new SubmissionReport(logger2, "myRun");
        QualityStats stats[] = qrun.execute(judge, submitLog, logger);

        QualityStats qualityStats = QualityStats.average(stats);
        qualityStats.log("SUMMARY", 2,logger2, "  ");
    //    dir.close();
    }
}