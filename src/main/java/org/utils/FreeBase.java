package org.utils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.experiment.benchmark.EntityQualityBenchmark;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tagme4j.TagMeWikiHelper;
import org.tagme4j.model.Annotation;


import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

/**
 * Created by sonic on 29/12/16.
 */
public class FreeBase {

    private static final String FREEBASEFILENAME = "dataset/FreeBase/fb2w.nt.gz";
    private static Logger log = LoggerFactory.getLogger(FreeBase.class);
    //https://en.wikipedia.org/w/api.php?action=query&prop=pageprops&format=json&pageids=18717338|20903754
    public static String wikiDataLink = "https://en.wikipedia.org/w/api.php?action=query&prop=pageprops&format=json&pageids=" + TagMeWikiHelper.PAGEIDS;
    private static String WIKIDATAMAPFILENAME = "dataset/wikiDataMap.txt";;


    public static void main(String[] args) throws Exception {
        /** TODO
         * Read FreeBase ()
         * Read Annotated Dataset (dataset/documentAnnotation.txt)
         *  - Save to File
         * Create Vector
         */
        readFreeBase();
       // getWikiDataIds ();

    }
    public static void readFreeBase () throws IOException {

        FileInputStream fin = new FileInputStream(FREEBASEFILENAME);
        GZIPInputStream gzis = new GZIPInputStream(fin);
        InputStreamReader xover = new InputStreamReader(gzis);
        BufferedReader is = new BufferedReader(xover);

        int counter = 0;
        int freeBaseEntityCounter = 0;
        String line;
        String wikiDataID;
        ArrayList <String> wikiDataList = new ArrayList<>();
        while ((line = is.readLine()) != null) {
            Pattern pattern = Pattern.compile("<(.+?)>");
            Matcher matcher = pattern.matcher(line);
            while (matcher.find()) {
                counter++;
                if ( counter == 3) { // WikiData
                    freeBaseEntityCounter++;
                    wikiDataID = matcher.group(1).replace("http://www.wikidata.org/entity/", "");
                    //log.info(wikiDataID);
                    wikiDataList.add(wikiDataID);
                    counter =0;

                }
            }
        }

        log.info("Finished Loading FreeBase...");

        HashMap <String , int[]> freebaseMap = new HashMap<>();

        HashMap<String, List<String>> docAnnMap = getWikiDataIds();

        int i = 0;
        int docFreeBaseAnnotationMath = 0;
        String docID = "";
        List <String> docWikiDataIDs;
        Iterator it = docAnnMap.entrySet().iterator();
        //TODO unsign char
        int[] entityFrequency = new int[wikiDataList.size()];
        while (it.hasNext()) { // Per document

            // Entity frequency vector set to - 0
            Arrays.fill(entityFrequency, 0);

            Map.Entry pair = (Map.Entry)it.next();
            docID = pair.getKey().toString();

            docWikiDataIDs = (List <String>) pair.getValue();
            for (String docWikiDataAnn : docWikiDataIDs) { // per document annotation
                if (wikiDataList.contains(docWikiDataAnn)) {
                    entityFrequency[wikiDataList.indexOf(docWikiDataAnn)]++;
                    docFreeBaseAnnotationMath++;
                }
            }

            it.remove();
            freebaseMap.put(docID, entityFrequency);
            log.info("DocID: " + docID + " number of entities: " + docWikiDataIDs.size() + " Matched: " + docFreeBaseAnnotationMath );
            docFreeBaseAnnotationMath = 0;
            //log.info(docID + " = " + Arrays.toString(entityFrequency));
        }

        log.info("Number of Tuples: " + freeBaseEntityCounter);
    }

    public static HashMap<String, List<String>> getWikiDataIds () {

        ObjectMapper mapper = new ObjectMapper();
        HashMap <String, List<String>> docAnnMap = new HashMap<>();
        List <String> wikiDataList = null;

        // Read WikiDataID and load:
        // FORMAT ==> 17289 : [Q490513, Q17052147, Q49389, Q977]
        try (BufferedReader br = new BufferedReader(new FileReader(WIKIDATAMAPFILENAME))) {
            String line;
            while ((line = br.readLine()) != null) {

                String docID = line.split(" : ")[0];
                String ids = line.split(" : ")[1];
                ids = ids.replace("[", "").replace("]", "");
                wikiDataList = Arrays.asList ( ids.split(",") );

                docAnnMap.put(docID, wikiDataList);

            }

        } catch (IOException e) {
            log.error("Could not read WikiDataMap.txt" + e.toString());
        }
        //
        try (BufferedReader br = new BufferedReader(new FileReader(EntityQualityBenchmark.collectionAnnFileName))) {
            String line;
            while ((line = br.readLine()) != null) {
                String docID = line.split("\t")[0];
                String json = line.split("\t")[1];
                List<Annotation> docAnnList = mapper.readValue(json, new TypeReference<List<Annotation>>(){});

                //check if Cached
                if (docAnnMap.containsKey(docID))
                    continue;

                String ids = "";
                for (Annotation a : docAnnList) {
                    ids += +a.getId() + "|"; // original WikiPage
                }

                String wikiURL = wikiDataLink.replace(TagMeWikiHelper.PAGEIDS, TagMeWikiHelper.sanitizeIDs (ids));
                //log.info("docID: " + docID + " - Wikipedia URL: " + wikiURL);
                JSONObject obj = TagMeWikiHelper.readJsonFromUrl(wikiURL);
                obj = obj.getJSONObject("query").getJSONObject("pages");

                Iterator<?> keys = obj.keys();

                wikiDataList = new ArrayList<>();
                while (keys.hasNext()) {
                    String key = (String) keys.next();
                    if (obj.get(key) instanceof JSONObject) {
                        try {
                            JSONObject pageprops = ((JSONObject) obj.get(key)).getJSONObject("pageprops");
                            if (pageprops != null) {
                                //log.info(pageprops.get("wikibase_item").toString());
                                wikiDataList.add(pageprops.get("wikibase_item").toString());
                            }
                        } catch (Exception e) {
                            log.error("Move On!");
                        }
                    }
                }

                // Save to File...
                log.info("DocID: " + docID +
                        " Number of Annotations: " + docAnnList.size() +
                        " WikiDataList: " + wikiDataList.toString()  );
                docAnnMap.put(docID, wikiDataList);
                String s = docID + " : " + wikiDataList.toString();
                Utilities.writeToFile(WIKIDATAMAPFILENAME, s);
            }

            log.info("Dataset Stat: " );
            return docAnnMap;

        } catch (Exception e) {
            log.error("unable to load annotation catch");
            log.error(e.toString());

        }
        return docAnnMap;
    }

}
