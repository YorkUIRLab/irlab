package org.experiment.TREC;

/**
 * Created by sonic on 06/09/16.
 */

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
import org.experiment.benchmark.EntityQualityBenchmark;
import org.experiment.query.TRECQQParser;
import org.utils.Utilities;

import java.io.*;
import java.nio.file.Paths;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class BagOfEntitiesEx {

    private static final int MYTHREADS = 30;
    public static String index;
    private static String topicPath;
    private static String qrelsPath;

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

        if (args.length == 0) {
            // WT10G
//            index = "/media/sonic/Windows/TREC/index/WT10G";
//            topicPath = "/media/sonic/Windows/TREC/topics/topics.wt10g";
//            qrelsPath = "/media/sonic/Windows/TREC/qrels/qrels.wt10g";
            // disk
//            index = "/media/sonic/Windows/TREC/index/disk45/disk45noCR";
//            topicPath = "/media/sonic/Windows/TREC/topics/topics.disk45.301-450";
//            qrelsPath = "/media/sonic/Windows/TREC/qrels/qrels.disk45.301-450";
            // AP
            index = "/media/sonic/Windows/TREC/index/AP";
            topicPath = "/media/sonic/Windows/TREC/topics/topics.AP.51-150";
            qrelsPath = "/media/sonic/Windows/TREC/qrels/qrels.AP.51-150";
        }

        List<String> similarityList = new ArrayList<>();
        similarityList.add("bm25");
        similarityList.add("LMJelinekMercerSimilarity");
        similarityList.add("lm");
        List<Double> tetaList = new ArrayList<>();
        tetaList.add(1.0); // Baseline
        tetaList.add(0.1);
        tetaList.add(0.2);
        tetaList.add(0.3);
        tetaList.add(0.4);
        tetaList.add(0.5);
        tetaList.add(0.6);
        tetaList.add(0.7);
        tetaList.add(0.8);
        tetaList.add(0.9);
        tetaList.add(0.0);

        String description = "AP"+ "-ENTLM-";
//        String description = "WT10G"+ "-Baseline-";
        File topicsFile = new File(topicPath);
        File qrelsFile = new File(qrelsPath);

        IndexReader reader = DirectoryReader.open(FSDirectory.open(Paths.get(index)));
        IndexSearcher searcher = new IndexSearcher(reader);


        TrecTopicsReader qReader = new TrecTopicsReader();
        QualityQuery qqs[] = qReader.readQueries(new BufferedReader(new FileReader(topicsFile)));
        Judge judge = new TrecJudge(new BufferedReader(new FileReader(qrelsFile)));
        QualityQueryParser qqParser = new TRECQQParser(TrecDocIterator.TITLE, TrecDocIterator.CONTENTS);

        ExecutorService executor = Executors.newFixedThreadPool(MYTHREADS);

        for (String simstring : similarityList) {
            Similarity simfn = Utilities.getSimilarity(simstring);
            searcher.setSimilarity(simfn);

            EntityQualityBenchmark qrun = new EntityQualityBenchmark(qqs, qqParser, searcher, TrecDocIterator.DOCNO);

            for (double teta : tetaList) {
                qrun.setTeta(teta);
                Runnable worker = new MyRunnable(description, qrun, qqs, judge, qqParser, simstring, teta);
                executor.execute(worker);
                worker.run();
            }
        }

        executor.shutdown();

        // Wait until all threads are finish
        while (!executor.isTerminated()) {

        }
        System.out.println("\nFinished all threads");

    }


    public static class MyRunnable implements Runnable {
        private String description;
        private String simstring;
        private double teta;
        private Judge judge;
        private QualityQuery[] qqs;
        private QualityQueryParser qqParser;
        private EntityQualityBenchmark qrun;

        public MyRunnable(String description, EntityQualityBenchmark qrun, QualityQuery[] qqs, Judge judge, QualityQueryParser qqParser, String simstring, double teta) {
            this.description = description;
            this.qrun = qrun;
            this.simstring = simstring;
            this.teta = teta;
            this.judge = judge;
            this.qqs = qqs;
            this.qqParser = qqParser;
        }


        @Override
        public void run() {
            try {
                PrintWriter logger  = new PrintWriter("test-data/Entity/final/" + description + teta + "-" + simstring + "-report1.out", "UTF-8");
                PrintWriter logger2 = new PrintWriter("test-data/Entity/final/" + description + teta + "-" + simstring + "-report2.out", "UTF-8");
                PrintWriter logger3 = new PrintWriter("test-data/Entity/final/" + description + teta + "-" + simstring + "-report3.out", "UTF-8");
                PrintWriter logger4 = new PrintWriter("test-data/Entity/final/" + description + teta + "-" + simstring + "-report4.out", "UTF-8");

                judge.validateData(qqs, logger);

                SubmissionReport submitLog = new SubmissionReport(logger2, simstring+teta);
                QualityStats stats[] = qrun.execute(judge, submitLog, logger);

                printReport(logger3, stats);

                QualityStats avg = QualityStats.average(stats);        // #6 Print precision and recall measures
                QualityStats.RecallPoint[] rp = avg.getRecallPoints();
                avg.log("SUMMARY -- " + simstring, 2, logger4, "  ");

                logger.close();
                logger2.close();
                logger3.close();
                logger4.close();
            } catch (Exception e) {
                e.printStackTrace();
            }

        }

        private void printReport(PrintWriter logger3, QualityStats[] stats) {
            NumberFormat nf = NumberFormat.getInstance(Locale.ROOT);
            nf.setMaximumFractionDigits(4);
            nf.setMinimumFractionDigits(4);
            nf.setGroupingUsed(true);

            QualityStats qualityStats = QualityStats.average(stats);

            logger3.println("SUMMARY -- " + simstring);
            logger3.println("MAP : " + nf.format(qualityStats.getAvp()));
            logger3.println("Average Precision @ 1: " + nf.format(qualityStats.getPrecisionAt(1)));
            logger3.println("Average Precision @ 5: " + nf.format(qualityStats.getPrecisionAt(5)));
            logger3.println("Average Precision @ 10: " + nf.format(qualityStats.getPrecisionAt(10)));
            logger3.println("Average Precision @ 20: " + nf.format(qualityStats.getPrecisionAt(20)));
            logger3.println("==========================");
            int qualityQuery = 1;
            for (QualityStats qualityStat : stats) {
                logger3.println("Quality Stat: " + qualityQuery);
                logger3.println("MAP:  " + nf.format(qualityStat.getAvp()));
                logger3.println("P@1:  " + nf.format(qualityStat.getPrecisionAt(1)));
                logger3.println("P@5:  " + nf.format(qualityStat.getPrecisionAt(5)));
                logger3.println("P@10: " + nf.format(qualityStat.getPrecisionAt(10)));
                logger3.println("P@20: " + nf.format(qualityStat.getPrecisionAt(20)));
                logger3.println("=========================");
                qualityQuery++;
            }
        }
    }
}
