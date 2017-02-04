package org.utils;

import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.List;

/**
 * Created by sonic on 23/01/17.
 */
public class CSVUtils {

    FileWriter writer;
    String csvFile;
    public CSVUtils(String csvFileName) throws IOException {
        csvFile = csvFileName;
        writer = new FileWriter(csvFile);
    }

    private static final char DEFAULT_SEPARATOR = ',';

    public void writeLine(List<String> values) throws IOException {
        writeLine(values, DEFAULT_SEPARATOR, ' ');
    }

    public void writeLine(List<String> values, char separators) throws IOException {
        writeLine(values, separators, ' ');
    }

    //https://tools.ietf.org/html/rfc4180
    private static String followCVSformat(String value) {

        String result = value;
        if (result.contains("\"")) {
            result = result.replace("\"", "\"\"");
        }
        return result;

    }

    public void writeLine(List<String> values, char separators, char customQuote) throws IOException {

        boolean first = true;
        if (separators == ' ') {
            separators = DEFAULT_SEPARATOR;
        }

        StringBuilder sb = new StringBuilder();
        for (String value : values) {
            if (!first) {
                sb.append(separators);
            }
            if (customQuote == ' ') {
                sb.append(followCVSformat(value));
            } else {
                sb.append(customQuote).append(followCVSformat(value)).append(customQuote);
            }
            first = false;
        }
        sb.append("\n");
        writer.append(sb.toString());

    }

    public void close () throws IOException {
        writer.flush();
        writer.close();
    }

}