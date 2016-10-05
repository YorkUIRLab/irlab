package org.experiment;

import java.io.*;

/**
 * Created by sonic on 28/05/16.
 */
public class TrecUtils {

    public static void main(String[] args) throws Exception {
        TrecUtils trecUtils = new TrecUtils();
        trecUtils.generateTitleQueries();

    }

    public void generateTitleQueries () throws FileNotFoundException, UnsupportedEncodingException {

        String titleOutput = "";
        try (BufferedReader br = new BufferedReader(new FileReader("/home/sonic/Dev/WT2G/topics/topics.wt2g"))) {
            String line;

            while ((line = br.readLine()) != null) {
                if (line.contains("<num>")){
                    titleOutput += line.replace("<num> Number: ", "").trim();
                } else if (line.contains("<title>")){
                    titleOutput += " " + line.replace("<title> ", "").trim() + "\n";
                }

                // process the line.
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }


        PrintWriter writer = new PrintWriter("/home/sonic/Dev/WT2G/topics/topics.title", "UTF-8");
        writer.println(titleOutput);
        writer.close();


    }
}
