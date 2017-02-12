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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.PrintWriter;
import java.nio.file.Paths;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class BagOfEntitiesEx {

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
            index = "/media/sonic/Windows/TREC/index/WT10G";
            topicPath = "/media/sonic/Windows/TREC/topics/topics.wt10g";
            qrelsPath = "/media/sonic/Windows/TREC/qrels/qrels.wt10g";
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

        String description = "WT10G"+ "-ENTLM-";
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

            for (double teta : tetaList) {
                PrintWriter logger =  new PrintWriter("test-data/Entity/final/" + description + teta + "-" + simstring + "-report1.out", "UTF-8");
                PrintWriter logger2 = new PrintWriter("test-data/Entity/final/" + description + teta + "-" + simstring + "-report2.out", "UTF-8");
                PrintWriter logger3 = new PrintWriter("test-data/Entity/final/" + description + teta + "-" + simstring + "-report3.out", "UTF-8");
                PrintWriter logger4 = new PrintWriter("test-data/Entity/final/" + description + teta + "-" + simstring + "-report4.out", "UTF-8");

                judge.validateData(qqs, logger);

                EntityQualityBenchmark qrun = new EntityQualityBenchmark(qqs, qqParser, searcher, TrecDocIterator.DOCNO);
                qrun.setTeta(teta);
                SubmissionReport submitLog = new SubmissionReport(logger2, simstring+teta);

                QualityStats stats[] = qrun.execute(judge, submitLog, logger);

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

                QualityStats avg = QualityStats.average(stats);        // #6 Print precision and recall measures
                QualityStats.RecallPoint[] rp = avg.getRecallPoints();
                avg.log("SUMMARY -- " + simstring, 2, logger4, "  ");

                logger.close();
                logger2.close();
                logger3.close();
                logger4.close();
            }
        }



    }
}
