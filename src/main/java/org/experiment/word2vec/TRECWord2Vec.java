package org.experiment.word2vec;

import com.sampullara.cli.Args;
import com.sampullara.cli.Argument;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.document.Document;
import org.deeplearning4j.berkeley.StringUtils;
import org.deeplearning4j.models.embeddings.loader.WordVectorSerializer;
import org.deeplearning4j.models.word2vec.Word2Vec;
import org.experiment.TREC.TrecDocIterator;
import org.experiment.analyzer.TRECAnalyzer;
import org.utils.Utilities;

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

        try {
            Args.parse(TRECWord2Vec.class, args);
        } catch (IllegalArgumentException e) {
            Args.usage(TRECWord2Vec.class);
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
                        writer.append( Utilities.getTermList(doc.getField("contents").tokenStream(analyzer, ts)) );
                        writer.newLine();
                        counter++;
                    }
                }
            }
            System.out.println("Number of document: " + counter);
        }
    }

    public static Word2Vec loadWord2VecModel (String fullVectorModel) {
        try {
            return WordVectorSerializer.loadFullModel(fullVectorModel);
        } catch (Exception e) {
            System.err.println("Could not load fullVector file: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }



}
