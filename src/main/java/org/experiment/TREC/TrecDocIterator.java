package org.experiment.TREC;

import java.io.*;
import java.util.Date;
import java.util.Iterator;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

import org.apache.lucene.benchmark.byTask.feeds.DemoHTMLParser;
import org.apache.lucene.benchmark.byTask.feeds.DocData;
import org.apache.lucene.benchmark.byTask.feeds.TrecContentSource;
import org.apache.lucene.benchmark.byTask.feeds.TrecDocParser;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.experiment.Parser;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class TrecDocIterator implements Iterator<Document> {

	protected BufferedReader rdr;
	protected boolean at_eof = false;
	protected Parser parser;
    protected TrecContentSource trecContentSource;

	
	public TrecDocIterator(File file) throws IOException {

        InputStream fileStream = new FileInputStream(file.getAbsolutePath());
        InputStream gzipStream = new GZIPInputStream(fileStream);
        Reader decoder = new InputStreamReader(gzipStream, "UTF-8");
        rdr = new BufferedReader(decoder);
		//rdr = new BufferedReader(new FileReader(file));

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
					doc.add(new StringField("docno", docno, Field.Store.YES));
				}

				sb.append(line);
			}
			if (sb.length() > 0) {
				String result = sb.substring(sb.indexOf("</DOCHDR>") + 9, sb.indexOf("</DOC>"));
				//System.out.println(result);
				parser = new Parser (result.toLowerCase().trim());
                // Add more pre-processing...
				doc.add(new TextField("contents", parser.body, Field.Store.NO));
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
