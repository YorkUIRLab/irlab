package org.experiment.wikipedia.processor;

import org.apache.lucene.benchmark.quality.QualityQuery;
import org.apache.lucene.benchmark.quality.utils.DocNameExtractor;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.experiment.analyzer.WikiPediaAnalyzer;
import org.experiment.word2vec.WikiWord2Vec;
import org.jcp.xml.dsig.internal.dom.Utils;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tagme4j.model.Annotation;
import org.utils.Utilities;

import java.io.*;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Created by sonic on 06/12/16.
 * <p>
 * WikiPedia API reference
 * https://www.mediawiki.org/wiki/API:Query
 */
public class TagMeWikiHelper {

    public static final String PAGEIDS = "{PAGEIDS}";
    public static final String MAXLINK = "{MAXLINK}";
    public static int      wikiMaxLink = 50;
    public static String wikiPageIdURL = "https://en.wikipedia.org/w/api.php?action=query&format=json&pageids=" + PAGEIDS + "&rvprop=content&prop=revisions";
    public static String wikiPageLinks = "https://en.wikipedia.org/w/api.php?action=query&format=json&pageids=" + PAGEIDS+ "&generator=links&gpllimit=" + MAXLINK;

    //Change based on environment
    public static final String WIKI_QUERY_EXPANSION_BASE_LOCATION = "dataset/TREC/WikiQueryExpansion/";

    private static Logger log = LoggerFactory.getLogger(TagMeWikiHelper.class);

    public static void main(String[] args) throws Exception {

        //List<String> wikiTextList = getWikiPages(sanitizeIDs("18717338|20903754|"));
        getWikiLinkPages(sanitizeIDs("18717338|20903754|"));

//        for (String wikiText : wikiTextList)
//            System.out.println(WikiPediaAnalyzer.getTermList(new WikiPediaAnalyzer(), wikiText));
    }

    /**
     *
     * @param ids
     * @return return string of ids
     * @throws IOException
     */
    private static String getWikiLinkPages(String ids) throws IOException {

        //TODO
        if (ids.contains("|")) {
            for (String id : ids.split(Pattern.quote("|")) ) {
                String wikiURL = wikiPageLinks.replace(PAGEIDS, id).replace(MAXLINK, Integer.toString(wikiMaxLink));
                log.info("Wikipedia URL: " + wikiURL);

                JSONObject obj = readJsonFromUrl(wikiURL);
                obj = obj.getJSONObject("query").getJSONObject("pages");

                Iterator<?> keys = obj.keys();
                while (keys.hasNext()) {
                    String key = (String) keys.next();
                    try {
                        if (obj.get(key) instanceof JSONObject && ((JSONObject) obj.get(key)).get("pageid") != null) {
                            ids += "|" + ((JSONObject) obj.get(key)).get("pageid").toString();
                        }
                    } catch (JSONException e) {
                        log.error(e.getMessage());
                    }

                }
            }
        }
        //log.info( ids );
        return ids;
    }

    public static List<String> getWikiPages(String ids) throws IOException {

        String wikiURL = wikiPageIdURL.replace(PAGEIDS, ids);
        log.info("Wikipedia URL: " + wikiURL);

        JSONObject obj = readJsonFromUrl(wikiURL);
        //System.out.println(obj.getJSONObject("query").getJSONObject("pages").toString());

        obj = obj.getJSONObject("query").getJSONObject("pages");

        Iterator<?> keys = obj.keys();

        List<String> wikiText = new ArrayList<>();
        while (keys.hasNext()) {
            String key = (String) keys.next();
            if (obj.get(key) instanceof JSONObject) {

                // System.out.println(((JSONObject) obj.get(key)).get("pageid"));
                JSONObject revisions = (JSONObject) ((JSONObject) obj.get(key)).getJSONArray("revisions").get(0);
                if (revisions != null) {
                    wikiText.add(revisions.get("*").toString());
                    // System.out.println(wikiText);
                }
            }
        }

        return wikiText;
    }


    /**
     *
     *
     * Write to file
     * FileName: QualityQuery #
     * FileContent: lines of WikiPeida page, processes by TrecAnalyzer equivalent.
     * @param entityMap
     * @throws IOException
     */
    public static void processWikiEntities(HashMap<QualityQuery, List<Annotation>> entityMap) throws IOException {

        Iterator it = entityMap.entrySet().iterator();
        while (it.hasNext()) {
            String ids = "";
            Map.Entry pair = (Map.Entry) it.next();

            //System.out.println(pair.getKey() + " = " + pair.getValue());
            QualityQuery qq = (QualityQuery) pair.getKey();
            String queryID = qq.getQueryID().toLowerCase().trim();


            for (Annotation a : (List<Annotation>) pair.getValue()) {
                //System.out.println(a.toString());
                ids += +a.getId() + "|"; // original WikiPage
            }

            //Expand wiki pages to internal links - Get WikiPages of internal links
            ids = getWikiLinkPages(sanitizeIDs(ids));
            log.info( ids );

            List<String> wikiTextList = getWikiPages(sanitizeIDs(ids));

            for (String wikiText : wikiTextList) {
                String processedText = WikiPediaAnalyzer.getTermList(new WikiPediaAnalyzer(), wikiText);
                //System.out.println(processedText);
                Utilities.writeToFile(WIKI_QUERY_EXPANSION_BASE_LOCATION + queryID, processedText);
            }
            it.remove(); // avoids a ConcurrentModificationException
        }
    }

    /**
     * Replace last "|" in pageIDs
     *
     * @param str
     * @return
     */
    public static String sanitizeIDs(String str) {
        if (str != null && str.length() > 0 && str.charAt(str.length() - 1) == '|') {
            str = str.substring(0, str.length() - 1);
        }
        return str;
    }



    private static String readAll(Reader rd) throws IOException {
        StringBuilder sb = new StringBuilder();
        int cp;
        while ((cp = rd.read()) != -1) {
            sb.append((char) cp);
        }
        return sb.toString();
    }

    public static JSONObject readJsonFromUrl(String url) throws IOException, JSONException {
        InputStream is = new URL(url).openStream();
        try {
            BufferedReader rd = new BufferedReader(new InputStreamReader(is, Charset.forName("UTF-8")));
            String jsonText = readAll(rd);
            JSONObject json = new JSONObject(jsonText);
            return json;
        } finally {
            is.close();
        }
    }


}
