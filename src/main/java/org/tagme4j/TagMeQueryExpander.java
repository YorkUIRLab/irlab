package org.tagme4j;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.benchmark.quality.QualityQuery;
import org.deeplearning4j.models.embeddings.loader.WordVectorSerializer;
import org.deeplearning4j.models.word2vec.Word2Vec;
import org.experiment.analyzer.TRECAnalyzer;
import org.experiment.preprocessing.StanfordLemmatizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tagme4j.model.Annotation;
import org.tagme4j.model.Relatedness;
import org.tagme4j.response.RelResponse;
import org.tagme4j.response.TagResponse;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

/**
 * Created by sonic on 27/11/16.
 * TagMe4J
 * https://github.com/marcocor/tagme4j
 */
public class TagMeQueryExpander {

    static String tagMeToken = "b1eac658-d8a7-49ac-8596-a212d7bf3c92-843339462";
    public static String  tagMeURL = "https://tagme.d4science.org/tagme/tag?lang=en&gcube-token={TOKEN}&text={TEXT}&include_abstract=true";

    //set number of close word
    int nearestWord = 1;

    Word2Vec word2Vec ;
    Analyzer trecAnalyzer ;
    private static Logger log = LoggerFactory.getLogger(TagMeQueryExpander.class);

    TagMeClient tagMeClient;
    public TagMeQueryExpander() {
        System.setProperty("javax.net.ssl.trustStore", "jssecacerts");
        tagMeClient = new TagMeClient(tagMeToken);
        this.trecAnalyzer = new TRECAnalyzer();
    }

    public QualityQuery[] updateQuery (QualityQuery qqs[]) throws IOException {

        ArrayList<QualityQuery> qualityQueryList = new ArrayList<>();

        HashMap<QualityQuery, List<Annotation>> entityMap = new HashMap<>();

        for (QualityQuery qq : qqs) {
            String queryTitle = qq.getValue("title").toLowerCase().trim();

            TagResponse tagResponse = tagMeClient
                    .tag()
                    .text(StanfordLemmatizer.getInstance().lemmatizeToString(queryTitle))
                    .execute();

            for (Annotation a : tagResponse.getAnnotations()) {
                //log.info(queryTitle + a.toString());
            }
            entityMap.put(qq, tagResponse.getAnnotations());
        }

        log.info ("Start Wiki Entities Training");
        // Turn-On Turn-OFF
        //TagMeWikiHelper.processWikiEntities (entityMap);
        log.info ("Ended Wiki Entities Training");


        log.info ("Start Query update");
        for (QualityQuery qq : qqs) {
            HashMap<String,String> fields = new HashMap<>();
            String queryTitle = qq.getValue("title").toLowerCase().trim();

            File dir = new File(TagMeWikiHelper.WIKI_QUERY_EXPANSION_BASE_LOCATION);
            File[] directoryListing = dir.listFiles();

            String word2vecModelFile;
            if (directoryListing != null) {
                for (File child : directoryListing) {
                    try {
                        word2vecModelFile = qq.getQueryID() + "_Word2Vec-full.txt";
                        //log.info("fileName: " + child.getName());
                        if (word2vecModelFile.equals(child.getName())) {
                            //log.info("loading Word2Vec model: " + child.getAbsolutePath());
                            word2Vec = loadWord2VecModel (child.getAbsolutePath());

                            for (String query : TRECAnalyzer.getTermList(trecAnalyzer, queryTitle)) {
                                Collection<String> lst4 = word2Vec.wordsNearest(query, nearestWord);
                                queryTitle = queryTitle.concat(" " + lst4.toString());
                            }

                            queryTitle = queryTitle.replace("[", "").replace("]", "");
                            log.info("query title: "  + queryTitle);
                            fields.put("title", queryTitle);
                            fields.put("description", qq.getValue("description"));
                            fields.put("narrative", qq.getValue("narrative"));
                            QualityQuery topic = new QualityQuery(qq.getQueryID(), fields);
                            qualityQueryList.add(topic);
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            } else {
                log.error("Could not find the directory: " + dir.getAbsolutePath());
            }
        }
        return qualityQueryList.toArray(new QualityQuery[0]);
    }

    public static void main(String[] args) throws TagMeException {

        System.setProperty("javax.net.ssl.trustStore", "jssecacerts");
//        System.setProperty("javax.net.ssl.trustStorePassword", "sonic");

//        String query = "Obama to visit UK";
//
//        tagMeURL = tagMeURL.replace("{TOKEN}", tagMeToken);
//        tagMeURL = tagMeURL.replace("{TEXT}", query);
//
//
//        RestTemplate restTemplate = new RestTemplate();
//        TagResponse tagResponse = restTemplate.getForObject(tagMeURL, TagResponse.class);
//
//        for (Annotation a : tagResponse.getAnnotations()){
//            System.out.printf("%s -> %s (rho=%f, lp=%f, abstract=%s)%n", a.getSpot(), a.getTitle(), a.getRho(), a.getLink_probability(), a.getWiki_abstract());
//        }

        TagMeClient tagMeClient = new TagMeClient(tagMeToken);

        TagResponse tagResponse = tagMeClient
                .tag()
                .longText(30)
                .text("a call for healingwarren c trenchard (70711.3366@compuserve.com)16 feb 93 09:24:32 est messages sorted by: [ date ][ thread ][ subject ][ author ] next message: woody baker: \"re: thinking man's church?  or \"the church with the truth\"\" previous message: daniel turk: \"thinking [person's] church\"a call for healingcheryl and sue are absolutely right.  we continue to practice thepatriarchalism of biblical times, claiming that we have biblical authority forthe exclusion of women from full participation in the adventist ministry.one problem with this kind of reasoning is that it is selective.  we do notpractice everything that the bible describes as practiced by the earliestchristians, e.g., common property holding and the kind treatment of ourslaves.  on the flip side, we practice all kinds of things in the church thatthe bible does not provide for, e.g., a professional clergy and a politicalinfrastructure.  the point is, while church praxis may be generally basedon certain biblical principles, it must be specifically designed to meetcontemporary needs in the context of contemporary societal and domesticrealities.  in this respect, the church of the new testament period was nodifferent from us.  their particular organization and policies wereconditioned by the expectations and realities of their time and place.  wemust allow ours to be so conditioned as well.  for them, the holding ofslaves and the subordination of women were realities in the context ofwhich they lived their christian lives and conducted the business of theirchurch.  we should no more feel compelled to emulate their subordinationof women than we should their holding of slaves.we have reached a strange position.  on the one hand, we have officially,if not always evidenced in local practice, given women the right to performcertain ecclesiastical duties.  for example, they may serve as ordainedlocal elders, in which role they may share in the officiation at communionservices.  this is an office that is sanctioned by the new testament,although we have no record in the text of any elder being ordained or anydiscussion of such a practice.  (curiously, our admission of women to theoffice of elder would seem to exceed certain pauline injunctions.)  on theother hand, we refuse to ordain women to the professional clergy, a callingnot specifically identified in the new testament.the bottom line is that we need to recognize that the praxis of the newtestament church is not normative for the praxis of the church at othertimes and places.  just as the praxis of the former was conditioned by itshistorical context, so also ours must be permitted to develop in the contextof our own time and expectations.  the timeless role of the bible in thisregard is to provide principles, such as love, service, commitment, andjustice, that will shape the praxis of the church in any time or place, nomatter what its specific characteristics may be.we must not allow this issue to remain like that strange miracle of jesuswhich, upon his first attempt at healing, left the man incompletely cured ofblindness.  just as he completed the healing task, let us together, as womenand men, quickly rectify this injustice by including women in the arena offull ecclesiastical rights--not only the rights of ordination to theprofessional clergy but also those of church leadership at the highest levels.warren c. trenchardinternet: 70711.3366@compuserve.comcis: 70711,3366\u001A next message: woody baker: \"re: thinking man's church?  or \"the church with the truth\"\" previous message: daniel turk: \"thinking [person's] church\"\n")
                .execute();
//
        for (Annotation a : tagResponse.getAnnotations()){
            System.out.println(a.toString());
        }
//
//        SpotResponse spotResponse = tagMeClient
//                .spot()
//                .text("Ice cream is good because it contains sugar")
//                .execute();
//        for (Mention m : spotResponse.getSpots()){
//            System.out.printf("%s (lp=%f)%n", m.getSpot(), m.getLp());
//        }
//
        RelResponse relResponse = tagMeClient
                .rel()
                .tt("Linked_data Semantic_Web")
                .tt("University_of_Pisa Massachusetts_Institute_of_Technology")
                .tt("Hussein_of_Jordan Peace")
                .tt("James_Cameron Non_Existing_Entity_ZXCASD")
                .execute();


        for (Relatedness r : relResponse.getResult())
            if (r.entitiesExist())
                log.info(r.toString());
            else
                log.info("Could not compute relatedness for entities error: " + r.toString());

    }

    public static Word2Vec loadWord2VecModel (String fullVectorModel) {
        try {
            return WordVectorSerializer.loadFullModel(fullVectorModel);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }
}
