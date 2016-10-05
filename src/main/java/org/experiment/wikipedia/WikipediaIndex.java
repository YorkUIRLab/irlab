package org.experiment.wikipedia;

import org.apache.lucene.benchmark.byTask.feeds.ContentSource;
import org.apache.lucene.benchmark.byTask.feeds.DocMaker;
import org.apache.lucene.benchmark.byTask.feeds.EnwikiContentSource;
import org.apache.lucene.benchmark.byTask.utils.Config;
import org.apache.lucene.benchmark.utils.ExtractWikipedia;

import java.io.File;
import java.nio.file.Paths;
import java.util.Properties;

/**
 * Created by sonic on 04/10/16.
 */
public class WikipediaIndex {

    public static void main(String[] args) throws Exception {

        File wikipedia = new File("/media/sonic/Windows/Wikipedia/enwiki-latest-pages-articles.xml.bz2");
        String outputDir = "/media/sonic/Windows/Wikipedia/enwiki";
        boolean keepImageOnlyDocs = false;
//        for (int i = 0; i < args.length; i++) {
//            String arg = args[i];
//            if (arg.equals("--input") || arg.equals("-i")) {
//                //wikipedia = new File(args[i + 1]);
//                i++;
//            } else if (arg.equals("--output") || arg.equals("-o")) {
//               // outputDir = new File(args[i + 1]);
//                i++;
//            } else if (arg.equals("--discardImageOnlyDocs") || arg.equals("-d")) {
//                keepImageOnlyDocs = false;
//            }
//        }

        Properties properties = new Properties();
        properties.setProperty("docs.file", wikipedia.getAbsolutePath());
        properties.setProperty("content.source.forever", "false");
        properties.setProperty("keep.image.only.docs", String.valueOf(keepImageOnlyDocs));
        Config config = new Config(properties);

        ContentSource source = new EnwikiContentSource();
        source.setConfig(config);

        DocMaker docMaker = new DocMaker();
        docMaker.setConfig(config, source);
        docMaker.resetInputs();
        if (wikipedia.exists()) {
            System.out.println("Extracting Wikipedia to: " + outputDir + " using EnwikiContentSource");
            ExtractWikipedia extractor = new ExtractWikipedia(docMaker, Paths.get(outputDir));
            extractor.extract();
        } else {
            System.out.println("something not right");
        }
    }

    private static void printUsage() {
        System.err.println("Usage: java -cp <...> org.apache.lucene.benchmark.utils.ExtractWikipedia --input|-i <Path to Wikipedia XML file> " +
                "[--output|-o <Output Path>] [--discardImageOnlyDocs|-d]");

        System.err.println("--discardImageOnlyDocs tells the extractor to skip Wiki docs that contain only images");
    }
}
