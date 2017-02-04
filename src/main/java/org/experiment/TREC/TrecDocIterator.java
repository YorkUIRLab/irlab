package org.experiment.TREC;

import org.apache.lucene.benchmark.byTask.feeds.TrecContentSource;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.utils.HTMLParser;
import org.xml.sax.SAXException;

import java.io.*;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

public class TrecDocIterator implements Iterator<Document> {

    public static final String DOCNO = "docno";
    public static final String CONTENTS = "contents";
    public static final String TITLE = "title";
    protected BufferedReader rdr;
    protected boolean at_eof = false;
    protected HTMLParser HTMLParser;
    protected TrecContentSource trecContentSource;


    public TrecDocIterator(File file) throws IOException {
        InputStream fileStream = new FileInputStream(file.getAbsolutePath());
        InputStream gzipStream = new GZIPInputStream(fileStream);
        Reader decoder = new InputStreamReader(gzipStream, "UTF-8");
        rdr = new BufferedReader(decoder);
        System.out.println("Reading " + file.toString());
        trecContentSource = new TrecContentSource();
    }

    @Override
    public boolean hasNext() {
        return !at_eof;
    }

    @Override
    public Document next() {
        Document doc = new Document();
        StringBuffer sb = new StringBuffer();
        try {
            String line;
            Pattern docno_tag = Pattern.compile("<DOCNO>\\s*(\\S+)\\s*<");
            boolean in_doc = false;
            while (true) {
                line = rdr.readLine();
                if (line == null) {
                    at_eof = true;
                    break;
                }
                if (!in_doc) {
                    if (line.startsWith("<DOC>"))
                        in_doc = true;
                    else
                        continue;
                }
                if (line.startsWith("</DOC>")) {
                    in_doc = false;
                    sb.append(line);
                    break;
                }

                Matcher m = docno_tag.matcher(line);
                if (m.find()) {
                    String docno = m.group(1);
                    doc.add(new StringField(DOCNO, docno, Field.Store.YES));
                }

                sb.append(line);
            }
            if (sb.length() > 0) {
                String result = sb.substring(sb.indexOf("</DOCHDR>") + 9, sb.indexOf("</DOC>"));
                HTMLParser = new HTMLParser(result.toLowerCase().trim());
                // http://stackoverflow.com/questions/12727868/lucene-how-to-store-file-content
                doc.add(new TextField(TITLE, HTMLParser.title, Field.Store.YES));
                doc.add(new TextField(CONTENTS, HTMLParser.body, Field.Store.YES));
            }
        } catch (IOException | SAXException e) {
            System.out.println("...missed..." + e.getMessage());
            doc = null;
        }
        return doc;
    }

    @Override
    public void remove() {
        // Do nothing, but don't complain
    }

}
