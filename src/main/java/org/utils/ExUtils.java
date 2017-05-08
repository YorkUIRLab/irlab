package org.utils;

import org.apache.commons.io.FileUtils;
import org.apache.lucene.benchmark.quality.QualityQuery;
import org.apache.lucene.benchmark.quality.QualityStats;
import org.apache.lucene.benchmark.quality.trec.TrecTopicsReader;
import org.experiment.preprocessing.StanfordLemmatizer;
import org.tagme4j.TagMeClient;
import org.tagme4j.TagMeWikiHelper;
import org.tagme4j.model.Annotation;
import org.tagme4j.response.TagResponse;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

/**
 * Created by sonic on 18/02/17.
 */
public class ExUtils {

    public static void main(String[] arg) throws IOException {
        //launch(arg);
        resultToCSV();

//        processCSVToWLT();
//        getQueryAnnotationStats();
    }

    private static void processCSVToWLT() {
        List<String> collectionList = new ArrayList<>();
        collectionList.add("AP");
        collectionList.add("WT2G");
        collectionList.add("WT10G");

        for (String dataset : collectionList) {

            System.out.println("=================" + dataset + "=================");
            try (Stream<Path> paths = Files.walk(Paths.get("test-data/Entity/final/" + dataset + "/csv"))) {
                paths.forEach(filePath -> {
                    if (Files.isRegularFile(filePath)) {
                        calculateWLT (filePath.toString());
                    }
                });
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void getQueryAnnotationStats () throws IOException {

        TrecTopicsReader qReader = new TrecTopicsReader();
        List <String> collectionList = new ArrayList<>();
        collectionList.add("/media/sonic/Windows/TREC/topics/topics.wt2g");
        collectionList.add("/media/sonic/Windows/TREC/topics/topics.wt10g");
        collectionList.add("/media/sonic/Windows/TREC/topics/topics.AP.51-150");
        TagMeClient tagMeClient = new TagMeClient(TagMeWikiHelper.tagMeToken);
        TagResponse tagResponse;
        List<Annotation> qqAnnotation;
        for (String topicFile : collectionList) {
            int noEntityCount = 0;
            double entityCount = 0;
            QualityQuery qqs[] = qReader.readQueries(new BufferedReader(new FileReader(topicFile)));
            // Load annotations into memory.
            for (int i = 0; i < qqs.length; i++) {
                QualityQuery qq = qqs[i];
                String queryTitle = qq.getValue("title").toLowerCase().trim();
                // List<Annotation> docAnnotation = new ArrayList<>();
                tagResponse = tagMeClient
                        .tag()
                        .text(StanfordLemmatizer.getInstance().lemmatizeToString(queryTitle))
                        .execute();

                qqAnnotation = tagResponse.getAnnotations();
                if (qqAnnotation.size() == 0)
                    noEntityCount++;

                entityCount += qqAnnotation.size();
//                System.out.println("Processing quality query: " + qq.getQueryID() +
//                        " - query title: " + queryTitle +
//                        " - " + qqAnnotation.toString());
            }

            System.out.println("Collection: " + topicFile +
                    "\n total number of queries: " + qqs.length +
                    "\n queries without entity: " + noEntityCount +
                    "\n total number of entities: " + entityCount +
                    "\n average entities: " + (entityCount / qqs.length) );
        }


    }

    public static void resultToCSV() {
//        String evalDir = "test-data/Entity/final/WT2G";
//        String evalDir = "test-data/Entity/final/WT10G";
        String evalDir = "test-data/Entity/final/AP";


        List <String> metricsList = new ArrayList<>();
        metricsList.add("MAP:");
        metricsList.add("P@1:");
        metricsList.add("P@10:");


        for (String regEx : metricsList) {
            try (Stream<Path> paths = Files.walk(Paths.get(evalDir))) {
                CSVUtils csvUtils = new CSVUtils(evalDir + "/csv/" + regEx + ".csv");
                paths.forEach(filePath -> {
                    if (Files.isRegularFile(filePath)) {
                        if (filePath.toString().endsWith("-report3.out")) {
                            List<String> mapList = new ArrayList<>();
                            mapList.add(filePath.toString());

                            try {
                                String raw = FileUtils.readFileToString(filePath.toFile(), "UTF-8");
                                System.out.println(filePath);
                                for (String line : raw.split("\n")) {
                                    if (line.contains(regEx)) {
                                        String value = line.split(regEx)[1];
                                        mapList.add(Double.toString(Double.parseDouble(value.trim())));
                                        System.out.println(regEx + " " + value);
                                    }
                                }
                                csvUtils.writeLine(mapList);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                });
                csvUtils.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void calculateWLT (String csvFile) {
        String line;
        String cvsSplitBy = ",";
        List<String[]> runs = new ArrayList<>();
        String[] baseLine = new String[0];
        try (BufferedReader br = new BufferedReader(new FileReader(csvFile))) {
            while ((line = br.readLine()) != null) {
                // use comma as separator
                String[] results = line.split(cvsSplitBy);
                if (results[0].contains("1.0"))
                    baseLine = results;
                else
                    runs.add(results);
                //System.out.println(results);
            }

//            System.out.println("baseline::::::" + baseLine.length);
            
            for (String [] result : runs){
                int win = 0, tie= 0, loss = 0;
                for (int i = 1; i < result.length; i++) {
                    if (Double.parseDouble(result[i]) > Double.parseDouble(baseLine[i]))
                        win++;
                    else if (Double.parseDouble(result[i]) < Double.parseDouble(baseLine[i]))
                        loss++;
                    else
                        tie++;
                }
                System.out.println(result[0] + "\tWin/Tie/Loss \t" + win + "/" + tie + "/" + loss);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void printResults(String simstring, PrintWriter logger3, PrintWriter logger4, QualityStats[] stats) {
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
    }
}
