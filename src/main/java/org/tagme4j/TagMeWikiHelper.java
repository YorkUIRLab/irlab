package org.tagme4j;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.benchmark.quality.QualityQuery;
import org.apache.lucene.benchmark.quality.utils.DocNameExtractor;
import org.apache.lucene.document.Document;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.ScoreDoc;
import org.experiment.analyzer.TRECAnalyzer;
import org.experiment.analyzer.WikiPediaAnalyzer;
import org.experiment.benchmark.EntityQualityBenchmark;
import org.experiment.preprocessing.StanfordLemmatizer;
import org.experiment.wikipedia.model.WikiPage;
import org.json.JSONException;
import org.json.JSONObject;
import org.repository.WikiRepo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tagme4j.model.Annotation;
import org.tagme4j.response.TagResponse;
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
@Component
public class TagMeWikiHelper {

    @Autowired
    private WikiRepo wikiRepo;

    public static String tagMeToken = "b1eac658-d8a7-49ac-8596-a212d7bf3c92-843339462";
    public static final String PAGEIDS = "{PAGEIDS}";
    public static final String MAXLINK = "{MAXLINK}";
    public static int      wikiMaxLink = 50;
    public static String wikiPageIdURL = "https://en.wikipedia.org/w/api.php?action=query&format=json&pageids=" + PAGEIDS + "&rvprop=content&prop=revisions";
    public static String wikiPageLinks = "https://en.wikipedia.org/w/api.php?action=query&format=json&pageids=" + PAGEIDS+ "&generator=links&gpllimit=" + MAXLINK;

    //Change based on environment
    public static final String WIKI_QUERY_EXPANSION_BASE_LOCATION = "dataset/TREC/WikiQueryExpansion/";

    private static Logger log = LoggerFactory.getLogger(TagMeWikiHelper.class);
    static BufferedReader is = null;

    public TagMeWikiHelper() {

        try {
            is = Utilities.readGZip("/media/sonic/Windows/TREC/wikipedia-clean1.bin");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws Exception {

        //List<String> wikiTextList = getWikiPages(sanitizeIDs("18717338|20903754|"));
        //getWikiLinkPages(sanitizeIDs("18717338|20903754|"));
        //getWikiDataIds ();
        TagMeWikiHelper tagMeHelper = new TagMeWikiHelper();
        for (String wikiText : tagMeHelper.loadWikiPages("18717338|20903754|"))
            System.out.println(WikiPediaAnalyzer.getTermList(new WikiPediaAnalyzer(), wikiText));

    }





    /**
     *
     * @param ids
     * @return return string of ids
     * @throws IOException
     */
    private static String getWikiLinkPages(String ids) throws IOException {

        ids = sanitizeIDs(ids);
        List <String> idList = Arrays.asList(ids.split("\\|"));
        for (String id : idList ) {
            String wikiURL = wikiPageLinks.replace(PAGEIDS, id).replace(MAXLINK, Integer.toString(wikiMaxLink));
            //log.info("Wikipedia URL: " + wikiURL);

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
                    //log.error(e.getMessage());
                }

            }
        }
        //log.info( ids );
        return ids;
    }

    /**
     * Wikipedia page ids
     * @param ids
     * @return
     * @throws IOException
     */
    public static List<String> getWikiPages(String ids) throws IOException {

        ids = sanitizeIDs(ids);
        String wikiURL = wikiPageIdURL.replace(PAGEIDS, ids);
        //log.info("Wikipedia URL: " + wikiURL);

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
     * @param qqs
     * @throws IOException
     */
    public void processWikiEntities(QualityQuery[] qqs) throws IOException {
        System.setProperty("javax.net.ssl.trustStore", "jssecacerts");
        TagMeClient tagMeClient = new TagMeClient(TagMeWikiHelper.tagMeToken);

        List<String> wikiTextList;
        TagResponse tagResponse;
        for (QualityQuery qq : qqs) {
            String queryTitle = qq.getValue("title").toLowerCase().trim();
            String queryID = qq.getQueryID().toLowerCase().trim();
            log.info("Query id: " + queryID + " QueryTitle: " + queryTitle);

            tagResponse = tagMeClient
                    .tag()
                    .text(StanfordLemmatizer.getInstance().lemmatizeToString(queryTitle))
                    .execute();

            String ids = "";
            for (Annotation a : tagResponse.getAnnotations()) {
                ids += +a.getId() + "|"; // original WikiPag
            }

            //Expand wiki pages to internal links - Get WikiPages of internal links
            ids = getWikiLinkPages(ids);

            //Expand wiki pages to internal links - Get WikiPages of internal links
            ids = getWikiLinkPages(ids);
            log.info("Query ID: "+ queryID + " Ids:" + ids);

            // Load from data set
            wikiTextList = loadWikiPages (ids);

            log.info("Query ID: " + queryID + " # of WikiPages: " + wikiTextList.size() +  " Wiki IDs: " + ids );

            for (String wikiText : wikiTextList) {
                //System.out.println(wikiText);
                Utilities.writeToFile(WIKI_QUERY_EXPANSION_BASE_LOCATION + queryID, wikiText);
            }
        }
    }

    private  List<String> loadWikiPages(String ids) {
        ids = sanitizeIDs(ids);
        List <String> idList = Arrays.asList(ids.split("\\|"));
        // Maximum 200 WikiPages
        if (idList.size() > 300)
            idList = idList.subList(0, 300);

        TRECAnalyzer trecAnalyzer = new TRECAnalyzer();
        List<String> wikiText = new ArrayList<>();
        try {
            for (WikiPage wikiPage : wikiRepo.findAll(idList)){
                wikiText.add (TRECAnalyzer.getTermList(trecAnalyzer, wikiPage.getTitle()) + " " + wikiPage.getContent());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        log.info("found # WikiPage: " + wikiText.size());
        return wikiText;
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
