package org.experiment.TREC;

import org.apache.commons.io.FileUtils;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.experiment.analyzer.TRECAnalyzer;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Date;
import java.util.EnumSet;
import java.util.concurrent.TimeUnit;

public class IndexTRECAP {

    private IndexTRECAP() {
    }

    public static void main(String[] args) {
//        String collection = "AP/AP90";
//        String collection = "AP/AP8889";
//        String collection = "AP"; //AP90 + AP8889 (242,918)
//        String collection = "disk45/disk45noCR"; // ROBUST (528,021)
        String collection = "WT2G"; // WT2G (247,489)
        String indexPath = "/media/sonic/Windows/TREC/index/" + collection;
        String docsPath = "/media/sonic/Windows/TREC/" + collection;

        final Path docDir = Paths.get(docsPath);
        if (!Files.isReadable(docDir)) {
            System.out.println("Document directory '" + docDir.toAbsolutePath() + "' does not exist or is not readable, please check the path");
            System.exit(1);
        }

        try {
            Date start = new Date();
            System.out.println("Indexing collection: " + docsPath +  "Indexing to directory '" + indexPath + "'...");
            Directory dir = FSDirectory.open(Paths.get(indexPath));
            IndexWriterConfig iwc = new IndexWriterConfig(new TRECAnalyzer());
            iwc.setOpenMode(OpenMode.CREATE);
            iwc.setRAMBufferSizeMB(4096.0);
            iwc.setUseCompoundFile(false);
            IndexWriter writer = new IndexWriter(dir, iwc);
            indexDocs(writer, docDir);

            System.out.println("Number of Document indexed (numDocs): " + writer.numDocs() + " maxDoc: " + writer.maxDoc());
            writer.close();

            Date end = new Date();
            System.out.println("Time to index dataset : " + TimeUnit.MILLISECONDS.toMinutes(end.getTime() - start.getTime()) + " Minutes");

            writer.close();
        } catch (IOException e) {
            System.out.println(" caught a " + e.getClass() +
                    "\n with message: " + e.getMessage());
        }
    }

    public static EnumSet<FileVisitOption> visitor_opts = EnumSet.of(FileVisitOption.FOLLOW_LINKS);

    public static class DocVisitor extends SimpleFileVisitor<Path> {
        IndexWriter writer;

        DocVisitor(IndexWriter writer_) {
            writer = writer_;
        }

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
                throws IOException {
            try {
                parseDoc(writer, file);
            } catch (IOException ignore) {
                // don't index files that can't be read.
            }
            return FileVisitResult.CONTINUE;
        }
    }

    static void indexDocs(final IndexWriter writer, Path path) throws IOException {
        DocVisitor docVisitor = new DocVisitor(writer);
        if (Files.isDirectory(path)) {
            Files.walkFileTree(path, visitor_opts, Integer.MAX_VALUE, docVisitor);
        } else {
            parseDoc(writer, path);
        }
    }

    static void parseDoc(IndexWriter writer, Path file) throws IOException {
        org.jsoup.nodes.Document soup;
        String str, docno, txt;
        str = FileUtils.readFileToString(new File(file.toString()), "UTF-8");
        soup = Jsoup.parse(str);
        int counter = 0;
        for (Element elm : soup.select("DOC")) {
            docno = "x";
            txt = "x";
            for (Element elm_ : elm.children()) {
                if (elm_.tagName().equals("docno")) {
                    docno = elm_.text().trim();
                    elm_.remove();
                }
//                else if (elm_.tagName().equals("")) {
//                    docno = elm_.text().trim();
//                    elm_.remove();
//                }
            }
            txt = elm.text().toLowerCase().trim();
            Document doc = new Document();
            doc.add(new StringField(TrecDocIterator.DOCNO, docno, Field.Store.YES));
            doc.add(new TextField(TrecDocIterator.CONTENTS, txt, Field.Store.YES));
            writer.addDocument(doc);
            counter++;
        }
        System.out.println("# docs read: " + counter);
    }
}
