package org.utils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.core.StopAnalyzer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.util.CharArraySet;
import org.apache.lucene.search.similarities.*;
import org.experiment.benchmark.EntityQualityBenchmark;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tagme4j.model.Annotation;

import java.io.*;
import java.util.*;
import java.util.zip.GZIPInputStream;

/**
 * Created by sonic on 07/12/16.
 */
public class Utilities {

    private static final Logger logger = LoggerFactory.getLogger(Utilities.class);

    public static String listToString(List<String> stringList)
    {
        String text = "";
        for(String k: stringList)
            text += " " + k;

        return text;
    }

    public static Similarity getSimilarity(String simstring) {
        float k1=1.2f;
        float b=0.35f;
        float mu=1000.0f;
        float lambda=0.1f;

        Similarity simfn = null;
        if ("default".equals(simstring)) {
            simfn = new ClassicSimilarity();
        } else if ("bm25".equals(simstring)) {
            simfn = new BM25Similarity(k1, b); // k1, b
        } else if ("dfr".equals(simstring)) {
            simfn = new DFRSimilarity(new BasicModelP(), new AfterEffectL(), new NormalizationH2());
        } else if ("lm".equals(simstring)) {
            simfn = new LMDirichletSimilarity(mu);
        } else if ("LMJelinekMercerSimilarity".equals(simstring)) {
            simfn = new LMJelinekMercerSimilarity(lambda);
        }

        logger.info("Similarity function is : " + simstring);
        return simfn;
    }

    /**
     * append = true
     * @param fileName
     * @param content
     */
    public static void writeToFile (String fileName, String content) {
        try{
            PrintWriter writer = new PrintWriter(new FileOutputStream(new File(fileName), true));
            writer.println(content);
            writer.close();
        } catch (IOException e) {
            logger.error("failed to write to file: " + fileName + e.getMessage());
        }
    }

    public static BufferedReader readGZip (String FILENAME) throws IOException {
        FileInputStream fin = new FileInputStream(FILENAME);
        GZIPInputStream gzis = new GZIPInputStream(fin);
        InputStreamReader xover = new InputStreamReader(gzis);
        BufferedReader is = new BufferedReader(xover);
//        String line;
//        while ((line = is.readLine()) != null)
//            System.out.println("Read: " + line);
        return is;
    }

    public static String getTermList(TokenStream ts) throws IOException {

        ArrayList<String> result = new ArrayList<String>();
        try {
            ts.reset();
            while (ts.incrementToken()) {
                CharTermAttribute ta = ts.getAttribute(CharTermAttribute.class);
                result.add(ta.toString());
            }
            ts.reset();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return listToString(result);
    }

    public static CharArraySet initStopWords() {

        CharArraySet stopWords;
        LineNumberReader reader = null;
        Set<String> set = new HashSet<String>();
        try {
            reader = new LineNumberReader(new FileReader("stopwords/stopwords.txt"));
            String stopWord = null;
            while ((stopWord = reader.readLine()) != null) {
                set.add(stopWord.trim());
            }
            stopWords = new CharArraySet(set, true);
            reader.close();

            //http://www.lextek.com/manuals/onix/stopwords2.html
            logger.info("Loaded SMART stop word list");
        } catch (IOException e) {
            logger.error("There is no stopwords: " + e.getMessage());
            stopWords = StopAnalyzer.ENGLISH_STOP_WORDS_SET;
        }

        return stopWords;

    }



    public static HashMap<String, List<Annotation>> populateCollectionAnnotationMap(String docAnnotationfileName) {
        // Load document annotations
        ObjectMapper mapper= new ObjectMapper();
        HashMap <String, List <Annotation> > docAnnMap = new HashMap<>();
        int entityCount = 0;
        try (BufferedReader br = new BufferedReader(new FileReader(docAnnotationfileName))) {
            String line;
            while ((line = br.readLine()) != null) {
                String docID = line.split("\t")[0];
                String json = line.split("\t")[1];
                List<Annotation> docAnnList = mapper.readValue(json, new TypeReference<List<Annotation>>(){});
                docAnnMap.put(docID, docAnnList);
                entityCount += docAnnList.size();
                //logger.error(docAnnList.toString());
            }
        } catch (Exception e) {
            logger.error("unable to load annotation catch");
            logger.error(e.getMessage());
        }
        logger.info("Loaded docs: " + docAnnMap.size() + " total annotations: " + entityCount +  " annotations for " + docAnnotationfileName);
        return docAnnMap;
    }

    public static void getWinTieLoss (String baselineReport, String modelReport) {
    }

    public static HashMap<String, List<Annotation>> populateDocTitleAnnotationMap(String documentTitleAnnFileName) {
        // Load document annotations
        ObjectMapper mapper= new ObjectMapper();
        HashMap <String, List <Annotation> > docAnnMap = new HashMap<>();
        try (BufferedReader br = new BufferedReader(new FileReader(documentTitleAnnFileName))) {
            String line;
            while ((line = br.readLine()) != null) {
                String docID = line.split("\t")[0];
                String json = line.split("\t")[1];
                List<Annotation> docAnnList = mapper.readValue(json, new TypeReference<List<Annotation>>(){});
                docAnnMap.put(docID, docAnnList);
                //logger.error(docAnnList.toString());
            }
        } catch (Exception e) {
            logger.error("unable to load annotation catch");
            logger.error(e.getMessage());
        }
        return docAnnMap;

    }
}
