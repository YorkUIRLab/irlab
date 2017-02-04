/**
 * WikiClean: A Java Wikipedia markup to plain text converter
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.experiment.wikipedia.wikiclean;

import org.apache.tools.bzip2.CBZip2InputStream;
import org.experiment.analyzer.WikiPediaAnalyzer;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.kohsuke.args4j.ParserProperties;

import java.io.*;
import java.util.zip.GZIPOutputStream;

public class WikipediaBz2DumpInputStream {
    public static final String WIKIPEDIA_CLEAN_BIN = "/media/sonic/Windows/TREC/wikipedia-clean1.bin";
    private static final int DEFAULT_STRINGBUFFER_CAPACITY = 1024;

    private BufferedReader br;
    private FileInputStream fis;

    /**
     * Creates an input stream for reading Wikipedia articles from a bz2-compressed dump file.
     *
     * @param file path to dump file
     * @throws IOException
     */
    public WikipediaBz2DumpInputStream(String file) throws IOException {
        br = null;
        fis = new FileInputStream(file);
        byte[] ignoreBytes = new byte[2];
        fis.read(ignoreBytes); // "B", "Z" bytes from commandline tools
        br = new BufferedReader(new InputStreamReader(new CBZip2InputStream(fis), "UTF8"));
    }

    public String readNext() throws IOException {
        String s;
        StringBuffer sb = new StringBuffer(DEFAULT_STRINGBUFFER_CAPACITY);

        while ((s = br.readLine()) != null) {
            if (s.endsWith("<page>"))
                break;
        }

        if (s == null) {
            fis.close();
            br.close();
            return null;
        }

        sb.append(s + "\n");

        while ((s = br.readLine()) != null) {
            sb.append(s + "\n");

            if (s.endsWith("</page>"))
                break;
        }

        return sb.toString();
    }

    private static final class Args {
        @Option(name = "-input", metaVar = "[path]", required = true, usage = "input path")
        String input;

        @Option(name = "-lang", metaVar = "[lang]", usage = "two-letter language code")
        String lang = "en";
    }

    /**
     * Format:
     * ID\tTitile\tPageText
     *
     * @param argv
     * @throws Exception
     */
    public static void main(String[] argv) throws Exception {
        final Args args = new Args();
        CmdLineParser parser = new CmdLineParser(args, ParserProperties.defaults().withUsageWidth(100));
        WikiPediaAnalyzer wikiPediaAnalyzer = new WikiPediaAnalyzer();
        int counter = 0;

        try {
            parser.parseArgument(argv);
        } catch (CmdLineException e) {
            System.err.println(e.getMessage());
            parser.printUsage(System.err);
            System.exit(-1);
        }

        WikiClean.WikiLanguage lang = WikiClean.WikiLanguage.EN;
        if (args.lang.equalsIgnoreCase("de")) {
            lang = WikiClean.WikiLanguage.DE;
        }

        WikiClean cleaner = new WikiCleanBuilder().withLanguage(lang).build();

        WikipediaBz2DumpInputStream stream = new WikipediaBz2DumpInputStream(args.input);
        String page;

        BufferedWriter writer = null;
        try {
            GZIPOutputStream zip = new GZIPOutputStream(new FileOutputStream(new File(WIKIPEDIA_CLEAN_BIN)));

            writer = new BufferedWriter(new OutputStreamWriter(zip, "UTF-8"));
            while ((page = stream.readNext()) != null) {
                if (page.contains("<ns>") && !page.contains("<ns>0</ns>"))
                    continue;

                writer.append(cleaner.getId(page).trim() + "\t");
                writer.append(cleaner.getTitle(page).trim() + "\t");
                writer.append( WikiPediaAnalyzer.getTermList( wikiPediaAnalyzer, cleaner.clean(page).trim()) );
                writer.newLine();

                counter++;
                if (counter % 100 == 0)
                    System.out.println("Processed pages so far: " + counter);
            }
        } finally {
            if (writer != null)
                writer.close();
        }
    }
}
