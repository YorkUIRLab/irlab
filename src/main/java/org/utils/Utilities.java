package org.utils;

import org.apache.lucene.search.similarities.*;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

/**
 * Created by sonic on 07/12/16.
 */
public class Utilities {


    public static String listToString(List<String> stringList)
    {
        String text = "";
        for(String k: stringList)
            text += " " + k;

        return text;
    }

    public static Similarity getSimilarity(String simstring) {
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
            System.err.println("failed to write to file: " + fileName + e.getMessage());
        }
    }

}
