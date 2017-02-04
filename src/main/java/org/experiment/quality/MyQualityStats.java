package org.experiment.quality;

import org.apache.lucene.benchmark.quality.QualityStats;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.NumberFormat;
import java.util.Locale;

/**
 * Created by sonic on 03/01/17.
 */
public class MyQualityStats extends QualityStats {

    /**
     * Construct a QualityStats object with anticipated maximal number of relevant hits.
     *
     * @param maxGoodPoints maximal possible relevant hits.
     * @param searchTime
     */
    public MyQualityStats(double maxGoodPoints, long searchTime) {
        super(maxGoodPoints, searchTime);
    }

    public JSONObject toJSON () {
        JSONObject obj = new JSONObject();
        obj.put("name", "mkyong.com");
        obj.put("age", new Integer(100));

        JSONArray list = new JSONArray();
        list.put("msg 1");
        list.put("msg 2");
        list.put("msg 3");

        obj.put("messages", list);

        try {

            FileWriter file = new FileWriter("c:\\test.json");
            file.write(obj.toString());
            file.flush();
            file.close();

        } catch (IOException e) {
            e.printStackTrace();
        }

        System.out.print(obj);

        return obj;

    }

    /**
     * Log information on this QualityStats object.
     * @param logger Logger.
     * @param prefix prefix before each log line.
     */
    public void log(String title, int paddLines, PrintWriter logger, String prefix) {

//        prefix = prefix==null ? "" : prefix;
//        NumberFormat nf = NumberFormat.getInstance(Locale.ROOT);
//        nf.setMaximumFractionDigits(3);
//        nf.setMinimumFractionDigits(3);
//        nf.setGroupingUsed(true);
//        int M = 19;
//        logger.println(prefix+"Average Precision: " + nf.format(getAvp()));
//        logger.println(prefix+"MRR: " + nf.format(getMRR()));
//        logger.println(prefix+"Recall: " + nf.format(getRecall())));
//        for (int i=1; i<(int)numPoints && i<pAt.length; i++) {
//
//            logger.println (prefix+"Precision At "+i+": " + nf.format(getPrecisionAt(i)));
//        }
//        for (int i=0; i<paddLines; i++) {
//            logger.println();
//        }
    }

}
