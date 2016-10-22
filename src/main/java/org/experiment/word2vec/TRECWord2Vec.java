package org.experiment.word2vec;

import com.sampullara.cli.Args;
import com.sampullara.cli.Argument;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.document.Document;
import org.deeplearning4j.berkeley.StringUtils;
import org.experiment.TREC.TrecDocIterator;
import org.experiment.analyzer.TRECAnalyzer;

import java.io.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.zip.GZIPOutputStream;

/**
 * Created by sonic on 21/10/16.
 */
public class TRECWord2Vec {

    @Argument(alias = "t", description = "TREC dataset location", required = true)
    private static String docsPath;// --/media/sonic/Windows/TREC/WT2G/dataset"; /home/datasets/TREC/WT2G/dataset
    @Argument(alias = "o", description = "Line dataset location", required = true)
    private static String lineOutput;// "/home/sonic/Dev/irlab/dataset/TREC/word2vec/WT2GLine.gz"

    public static void main(String[] args) {

        // arguments
        try {
            Args.parse(WordEmbedding.class, args);
        } catch (IllegalArgumentException e) {
            Args.usage(WordEmbedding.class);
            System.exit(1);
        }

        final File docDir = new File(docsPath);
        if (!docDir.exists() || !docDir.canRead()) {
            System.out.println("Document directory '" + docDir.getAbsolutePath() + "' does not exist or is not readable, please check the path");
            System.exit(1);
        }

        try {
            Date start = new Date();

            GZIPOutputStream zip = new GZIPOutputStream(new FileOutputStream(new File(lineOutput)));
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(zip, "UTF-8"));

            processDocs(docDir, writer);

            writer.close();

            Date end = new Date();
            System.out.println(end.getTime() - start.getTime() + " total milliseconds");

        } catch (IOException e) {
            System.out.println(" caught a " + e.getClass() +
                    "\n with message: " + e.getMessage());
        }
    }

    private static void processDocs(File file, BufferedWriter writer) throws IOException {

        Analyzer analyzer = new TRECAnalyzer();

        TokenStream ts = null;
        int counter = 0;

        if (file.canRead()) {
            if (file.isDirectory()) {
                String[] files = file.list();
                // an IO error could occur
                if (files != null) {
                    for (String file1 : files) {
                        processDocs(new File(file, file1), writer);
                    }
                }
            } else {
                TrecDocIterator docs = new TrecDocIterator(file);
                Document doc;
                while (docs.hasNext()) {
                    doc = docs.next();
                    if (doc != null && doc.getField("contents") != null) {
                        ArrayList<String> termList = getTermList(doc.getField("contents").tokenStream(analyzer, ts));
                        writer.append(StringUtils.join(termList).replaceAll(("[^A-Za-z0-9 ]"), ""));
                        writer.newLine();
                        counter++;
                    }
                }
            }
            System.out.println("Number of document: " + counter);
        }
    }

    public static ArrayList<String> getTermList(TokenStream ts) throws IOException {

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
        return result;
    }

}
