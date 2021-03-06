package org.experiment.TREC;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.experiment.analyzer.TRECAnalyzer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tagme4j.TagMeQueryExpander;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Date;
import java.util.concurrent.TimeUnit;


public class IndexTREC {

    private static Logger log = LoggerFactory.getLogger(TagMeQueryExpander.class);

    private IndexTREC() {

    }

    public static void index () {
        String datasetName = "WT2G";
        String indexPath, docsPath;
        indexPath = "index/" + datasetName;
        docsPath = "/media/sonic/Windows/TREC/";// /home/datasets/TREC/WT2G/dataset
        final File docDir = new File(docsPath);
        if (!docDir.exists() || !docDir.canRead()) {
            log.info("Document directory '" + docDir.getAbsolutePath() + "' does not exist or is not readable, please check the path");
            System.exit(1);
        }
        Date start = new Date();
        try {
            log.info("Indexing to directory '" + indexPath + "'...");
            Directory dir = FSDirectory.open(Paths.get(indexPath));
            Analyzer analyzer = new TRECAnalyzer();
            IndexWriterConfig iwc = new IndexWriterConfig(analyzer);
            // Create a new index in the directory, removing any previously indexed documents:
            iwc.setOpenMode(OpenMode.CREATE);
            iwc.setRAMBufferSizeMB(5000.0);
            IndexWriter writer = new IndexWriter(dir, iwc);
            indexDocs(writer, docDir);

            log.info ("Number of Document indexed (numDocs): " + writer.numDocs() + " maxDoc: " + writer.maxDoc());
            writer.close();

            Date end = new Date();
            log.info ("Time to index dataset : " + TimeUnit.MILLISECONDS.toMinutes(end.getTime() - start.getTime()) + " Minutes");

        } catch (IOException e) {
            System.out.println(" caught a " + e.getClass() +
                    "\n with message: " + e.getMessage());
        }



    }

    public static void main(String[] args) {


        String message = "nohup mvn compile && nohup mvn -DargLine=\"-Xmx1524m\" -e exec:java " +
                "-Dexec.mainClass=\"org.experiment.TREC.IndexTREC\"  " +
                "-Dexec.args=\"index /home/datasets/TREC/WT2G/dataset\" &> trecIndex.log";

        String datasetName = "WT2G";
        String indexPath, docsPath;
        if (args.length == 2) {
            indexPath = args[0];
            docsPath = args[1];
        } else {
            indexPath = "/media/sonic/Windows/TREC/index/" + datasetName;
            docsPath = "/media/sonic/Windows/TREC/WT2G/dataset";// /home/datasets/TREC/WT2G/dataset
        }


        boolean create = true;
        for (int i = 0; i < args.length; i++) {
            if ("-index".equals(args[i])) {
                indexPath = args[i + 1];
                i++;
            } else if ("-docs".equals(args[i])) {
                docsPath = args[i + 1];
                i++;
            } else if ("-update".equals(args[i])) {
                create = false;
            }
        }

        final File docDir = new File(docsPath);
        if (!docDir.exists() || !docDir.canRead()) {
            System.out.println("Document directory '" + docDir.getAbsolutePath() + "' does not exist or is not readable, please check the path");
            System.exit(1);
        }

        Date start = new Date();
        try {
            System.out.println("Indexing to directory '" + indexPath + "'...");

            Directory dir = FSDirectory.open(Paths.get(indexPath));
            Analyzer analyzer = new TRECAnalyzer();
            // add analyzers...
            IndexWriterConfig iwc = new IndexWriterConfig(analyzer);

            if (create) {
                // Create a new index in the directory, removing any
                // previously indexed documents:
                iwc.setOpenMode(OpenMode.CREATE);
            } else {
                // Add new documents to an existing index:
                iwc.setOpenMode(OpenMode.CREATE_OR_APPEND);
            }

            // Optional: for better indexing performance, if you
            // are indexing many documents, increase the RAM
            // buffer.  But if you do this, increase the max heap
            // size to the JVM (eg add -Xmx512m or -Xmx1g):
            iwc.setRAMBufferSizeMB(4000.0);

            IndexWriter writer = new IndexWriter(dir, iwc);
            indexDocs(writer, docDir);

            // NOTE: if you want to maximize search performance,
            // you can optionally call forceMerge here.  This can be
            // a terribly costly operation, so generally it's only
            // worth it when your index is relatively static (ie
            // you're done adding documents to it):
            //
            // writer.forceMerge(1);

            System.out.println("Number of Document indexed (numDocs): " + writer.numDocs() + " maxDoc: " + writer.maxDoc());
            writer.close();

            Date end = new Date();
            System.out.println("Time to index dataset : " + TimeUnit.MILLISECONDS.toMinutes(end.getTime() - start.getTime()) + " Minutes");

        } catch (IOException e) {
            System.out.println(" caught a " + e.getClass() +
                    "\n with message: " + e.getMessage());
        }
    }

    /**
     * Indexes the given file using the given writer, or if a directory is given,
     * recurses over files and directories found under the given directory.
     * <p>
     * NOTE: This method indexes one document per input file.  This is slow.  For good
     * throughput, put multiple documents into your input file(s).  An example of this is
     * in the benchmark module, which can create "line doc" files, one document per line,
     * using the
     * <a href="../../../../../contrib-benchmark/org/apache/lucene/benchmark/byTask/tasks/WriteLineDocTask.html"
     * >WriteLineDocTask</a>.
     *
     * @param writer Writer to the index where the given file/dir info will be stored
     * @param file   The file to index, or the directory to recurse into to find files to index
     * @throws IOException If there is a low-level I/O error
     */
    private static void indexDocs(IndexWriter writer, File file) throws IOException {
        int counter = 0;

        // do not try to index files that cannot be read
        if (file.canRead()) {
            if (file.isDirectory()) {
                String[] files = file.list();
                // an IO error could occur
                if (files != null) {
                    for (String file1 : files) {
                        indexDocs(writer, new File(file, file1));
                    }
                }
            } else {
                TrecDocIterator docs = new TrecDocIterator(file);
                Document doc;
                while (docs.hasNext()) {
                    doc = docs.next();
                    if (doc != null && doc.getField("contents") != null) {
                        writer.addDocument(doc);
                        counter++;
                    }
                }
            }
        }
        System.out.println("Number of document: " + counter);
    }


}


