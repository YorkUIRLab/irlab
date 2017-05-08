package org.experiment.TREC;

import org.apache.lucene.benchmark.quality.Judge;
import org.apache.lucene.benchmark.quality.QualityQuery;
import org.apache.lucene.benchmark.quality.QualityQueryParser;
import org.apache.lucene.benchmark.quality.QualityStats;
import org.apache.lucene.benchmark.quality.trec.TrecJudge;
import org.apache.lucene.benchmark.quality.trec.TrecTopicsReader;
import org.apache.lucene.benchmark.quality.utils.SubmissionReport;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.similarities.Similarity;
import org.apache.lucene.store.FSDirectory;
import org.experiment.benchmark.BOEQualityBenchmark;
import org.experiment.benchmark.EntityQualityBenchmark;
import org.experiment.query.TRECQQParser;
import org.utils.ExUtils;
import org.utils.Utilities;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.PrintWriter;
import java.nio.file.Paths;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by sonic on 06/09/16.
 */
public class BOEEx {

    public static String index;
    private static String topicPath;
    private static String qrelsPath;
    private static String collectionAnn;

    public static void main(String[] args) throws Throwable {

        for(int i = 0;i < args.length;i++) {
            if ("-index".equals(args[i])) {
                index = args[i + 1];
                i++;
            } else if ("-topic".equals(args[i])) {
                topicPath = args[i + 1];
                i++;
            } else if ("-qrels".equals(args[i])) {
                qrelsPath = args[i + 1];
                i++;
            }
        }

        //    "dataset/APDOCAnnotation.bin"; "dataset/documentAnnotation.txt";
        if (args.length == 0) {
            // WT2G
            index = "/media/sonic/Windows/TREC/index/WT2G";
            topicPath = "/media/sonic/Windows/TREC/topics/topics.wt2g";
            qrelsPath = "/media/sonic/Windows/TREC/qrels/qrels.wt2g";
            collectionAnn = "dataset/WT2GDOCAnnotation.bin";
            // WT10G
//            index = "/media/sonic/Windows/TREC/index/WT10G";
//            topicPath = "/media/sonic/Windows/TREC/topics/topics.wt10g";
//            qrelsPath = "/media/sonic/Windows/TREC/qrels/qrels.wt10g";
            // disk
//            index = "/media/sonic/Windows/TREC/index/disk45/disk45noCR";
//            topicPath = "/media/sonic/Windows/TREC/topics/topics.disk45.301-450";
//            qrelsPath = "/media/sonic/Windows/TREC/qrels/qrels.disk45.301-450";
//            // AP
//            index = "/media/sonic/Windows/TREC/index/AP";
//            topicPath = "/media/sonic/Windows/TREC/topics/topics.AP.51-150";
//            qrelsPath = "/media/sonic/Windows/TREC/qrels/qrels.AP.51-150";
        }

        List<String> similarityList = new ArrayList<>();
        similarityList.add("bm25");
//        similarityList.add("LMJelinekMercerSimilarity");
//        similarityList.add("lm");
        List<Double> tetaList = new ArrayList<>();
//        tetaList.add(1.0); // Baseline
        tetaList.add(0.1);
//        tetaList.add(0.2);
//        tetaList.add(0.3);
//        tetaList.add(0.4);
//        tetaList.add(0.5);
//        tetaList.add(0.6);
//        tetaList.add(0.7);
//        tetaList.add(0.8);
//        tetaList.add(0.9);
//        tetaList.add(0.0);
//        tetaList.add(0.05);
//        tetaList.add(0.15);
//        tetaList.add(0.25);
//        tetaList.add(0.35);
//        tetaList.add(0.35);
//        tetaList.add(0.45);
//        tetaList.add(0.55);
//        tetaList.add(0.65);
//        tetaList.add(0.75);
//        tetaList.add(0.85);
//        tetaList.add(0.95);

        String description = "WT2G"+ "-EMBBOE-";
//        String description = "WT10G"+ "-Baseline-";
        File topicsFile = new File(topicPath);
        File qrelsFile = new File(qrelsPath);

        IndexReader reader = DirectoryReader.open(FSDirectory.open(Paths.get(index)));
        IndexSearcher searcher = new IndexSearcher(reader);

        TrecTopicsReader qReader = new TrecTopicsReader();
        QualityQuery qqs[] = qReader.readQueries(new BufferedReader(new FileReader(topicsFile)));
        Judge judge = new TrecJudge(new BufferedReader(new FileReader(qrelsFile)));
        QualityQueryParser qqParser = new TRECQQParser(TrecDocIterator.TITLE, TrecDocIterator.CONTENTS);

        for (String simstring : similarityList) {
            Similarity simfn = Utilities.getSimilarity(simstring);
            searcher.setSimilarity(simfn);

            BOEQualityBenchmark qrun = new BOEQualityBenchmark(qqs, qqParser, searcher, collectionAnn);

            for (double teta : tetaList) {
                qrun.setTeta(teta);

                try {
                    PrintWriter logger  = new PrintWriter("test-data/Entity/BOE/" + description + teta + "-" + simstring + "-report1.out", "UTF-8");
                    PrintWriter logger2 = new PrintWriter("test-data/Entity/BOE/" + description + teta + "-" + simstring + "-report2.out", "UTF-8");
                    PrintWriter logger3 = new PrintWriter("test-data/Entity/BOE/" + description + teta + "-" + simstring + "-report3.out", "UTF-8");
                    PrintWriter logger4 = new PrintWriter("test-data/Entity/BOE/" + description + teta + "-" + simstring + "-report4.out", "UTF-8");

                    judge.validateData(qqs, logger);

                    SubmissionReport submitLog = new SubmissionReport(logger2, simstring+teta);
                    QualityStats stats[] = qrun.execute(judge, submitLog, logger);

                    ExUtils.printResults(simstring, logger3, logger4, stats);

                    logger.close();
                    logger2.close();
                    logger3.close();
                    logger4.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        System.out.println("\nFinished all threads");
    }
}
