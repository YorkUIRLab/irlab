package org.experiment.benchmark;

import com.fasterxml.jackson.databind.ObjectMapper;
import info.debatty.java.stringsimilarity.JaroWinkler;
import org.apache.commons.math3.ml.distance.DistanceMeasure;
import org.apache.commons.math3.ml.distance.EarthMoversDistance;
import org.apache.commons.math3.ml.distance.ManhattanDistance;
import org.apache.commons.math3.stat.descriptive.moment.StandardDeviation;
import org.apache.lucene.benchmark.quality.*;
import org.apache.lucene.benchmark.quality.utils.DocNameExtractor;
import org.apache.lucene.benchmark.quality.utils.SubmissionReport;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.*;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.BytesRef;
import org.deeplearning4j.models.word2vec.Word2Vec;
import org.experiment.TREC.BOEEx;
import org.experiment.TREC.BagOfEntitiesEx;
import org.experiment.TREC.TrecDocIterator;
import org.experiment.analyzer.TRECAnalyzer;
import org.experiment.preprocessing.StanfordLemmatizer;
import org.experiment.similarities.ByWeightComparator;
import org.experiment.word2vec.MyModelUtils;
import org.experiment.word2vec.TRECWord2Vec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tagme4j.TagMeClient;
import org.tagme4j.TagMeWikiHelper;
import org.tagme4j.model.Annotation;
import org.tagme4j.response.TagResponse;
import org.utils.Utilities;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Created by sonic on 18/12/16.
 */
public class BOEQualityBenchmark extends QualityBenchmark {

    private static final Logger logger = LoggerFactory.getLogger(BOEQualityBenchmark.class);


    //maximal number of queries that this quality benchmark runs. Default: maxint. Useful for debugging.
    private int maxQueries = Integer.MAX_VALUE;
    // maximal number of results to collect for each query. Default: 1000.
    private int maxResults = 500;

    Word2Vec word2Vec;

    double tetaVal;

    String collectionAnnFileName;

    public static String documentTitleAnnFileName = "dataset/documentTitleAnnotation.txt";
    private HashMap<String, List<Annotation>> collectionAnnMap = new HashMap<>();

    private HashMap<String, List<Annotation>> docTitleAnnMap = new HashMap<>();
    private HashMap<String, List<Annotation>> queryAnnMap = new HashMap<>();
    List<Annotation> qqAnnotation;

    TRECAnalyzer trecAnalyzer = new TRECAnalyzer();

    HashMap<String, Long> term_total_freq_map = new HashMap<String, Long>();// term frequency in collection
    HashMap<String, Integer> term_doc_freq_map = new HashMap<String, Integer>();// term frequency in a document

    TagMeClient tagMeClient;
    TagResponse tagResponse;
    ObjectMapper mapper = new ObjectMapper();
    JaroWinkler jw;
    StandardDeviation sd2;

    IndexReader reader;
    /**
     * Number of Documents in index
     */
    int numberOfDocsIndex;

    /**
     * Create a QualityBenchmark.
     *
     * @param qqs          quality queries to run.
     * @param qqParser     HTMLParser for turning QualityQueries into Lucene Queries.
     * @param searcher     index to be searched.
     *                     This allows to extract the doc name for search results,
     */
    public BOEQualityBenchmark(QualityQuery[] qqs, QualityQueryParser qqParser, IndexSearcher searcher, String collectionAnnFileName) {
        super(qqs, qqParser, searcher, TrecDocIterator.DOCNO);

        this.collectionAnnFileName = collectionAnnFileName;

        jw = new JaroWinkler();
        sd2 = new StandardDeviation();

        collectionAnnMap = Utilities.populateCollectionAnnotationMap(collectionAnnFileName);
        docTitleAnnMap = Utilities.populateDocTitleAnnotationMap(documentTitleAnnFileName);

        tagMeClient = new TagMeClient(TagMeWikiHelper.tagMeToken);

        // Load annotations into memory.
        for (int i = 0; i < qualityQueries.length; i++) {
            QualityQuery qq = qualityQueries[i];
            String queryTitle = qq.getValue("title").toLowerCase().trim();
           // List<Annotation> docAnnotation = new ArrayList<>();
            tagResponse = tagMeClient
                    .tag()
                    .text(StanfordLemmatizer.getInstance().lemmatizeToString(queryTitle))
                    .execute();

            qqAnnotation = tagResponse.getAnnotations();
            logger.info("==================================");
            logger.info("Processing quality query: " + qq.getQueryID() +
                    " - query title: " + queryTitle +
                    " - " + qqAnnotation.toString());
            queryAnnMap.put(qq.getQueryID(), qqAnnotation);
        }

        try {
            Directory directory = FSDirectory.open(Paths.get(BOEEx.index));
            reader = DirectoryReader.open(directory);
            termStats(reader);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void setTeta(double teta) {
        tetaVal = teta;
    }

    /**
     * Run the quality benchmark.
     *
     * @param judge      the judge that can tell if a certain result doc is relevant for a certain quality query.
     *                   If null, no judgements would be made. Usually null for a submission run.
     * @param submitRep  submission report is created if non null.
     * @param qualityLog If not null, quality run data would be printed for each query.
     * @return QualityStats of each quality query that was executed.
     * @throws Exception if quality benchmark failed to run.
     */
    public QualityStats[] execute(Judge judge, SubmissionReport submitRep,
                                  PrintWriter qualityLog) throws Exception {
        int nQueries = Math.min(maxQueries, qualityQueries.length);
        QualityStats stats[] = new QualityStats[nQueries];

        for (int i = 0; i < nQueries; i++) {
            QualityQuery qq = qualityQueries[i];
            // generate query
            Query q = qqParser.parse(qq);
            // search with this query
            long t1 = System.currentTimeMillis();
            TopDocs td = searcher.search(q, maxResults);

            if (tetaVal != 1.0) { // if not baseline
                td = updateScore(qq, td, searcher);
                // Sort scores
                Arrays.sort(td.scoreDocs, new ByWeightComparator());
            }

            long searchTime = System.currentTimeMillis() - t1;
            logger.info("Search time: " + TimeUnit.MILLISECONDS.toSeconds(searchTime));

            //most likely we either submit or judge, but check both
            if (judge != null) {
                stats[i] = analyzeQueryResults(qq, q, td, judge, qualityLog, searchTime);
            }
            if (submitRep != null) {
                submitRep.report(qq, td, docNameField, searcher);
            }
        }
        if (submitRep != null) {
            submitRep.flush();
        }
        return stats;
    }

    public TopDocs updateScore(QualityQuery qq, TopDocs td, IndexSearcher searcher) throws IOException {

        Document d;
        String queryTitle = qq.getValue("title").toLowerCase().trim();
        List<Annotation> docAnnotation;

        logger.info("Start Query update, queryID : " +
                qq.getQueryID() + " : " +
                StanfordLemmatizer.getInstance().lemmatizeToString(queryTitle) +
                " teta: "  + tetaVal);

        // Get Word2Vec model
        File dir = new File(TagMeWikiHelper.WIKI_QUERY_EXPANSION_BASE_LOCATION);
        File[] directoryListing = dir.listFiles();
        String word2vecModelFile;
        if (directoryListing != null) {
            for (File child : directoryListing) {
                word2vecModelFile = qq.getQueryID() + "_Word2Vec-full.bin";
                //logger.info("fileName: " + child.getName());
                if (word2vecModelFile.equals(child.getName())) {
                    logger.info("loading Word2Vec model: " + child.getAbsolutePath());
                    word2Vec = TRECWord2Vec.loadWord2VecModel(child.getAbsolutePath());
                    // Override the nullpointerException thrown if term not found
                    word2Vec.setModelUtils(new MyModelUtils());
                }
            }
        } else {
            logger.error("Could not find the directory: " + dir.getAbsolutePath());
        }

        try {
            qqAnnotation = queryAnnMap.get(qq.getQueryID());
            logger.info(qqAnnotation.toString());

            List <Annotation> collectionVector = new ArrayList<>();
            // Gather the entity space of the query results...
            for (int i = 0; i < td.scoreDocs.length; i++) {
                d = searcher.doc(td.scoreDocs[i].doc);
                String docNO = d.getField(TrecDocIterator.DOCNO).stringValue().trim();
                String contents = d.getField(TrecDocIterator.CONTENTS).stringValue();
                contents = (contents.length() > 2000) ? contents.substring(0, 2000) : contents;

                // Prepare collection entity space - collectionVector
                getCollectionSpace(collectionVector, docNO, contents);
            }

            logger.info("Finished getting collection entity space, size: " + collectionVector.size());

            Annotation[] entitySpace = collectionVector.toArray(new Annotation[collectionVector.size()]);
            double[] docSpace = new double[entitySpace.length];
            //Arrays.fill(docSpace, 0);
            double[] queryVector = new double[entitySpace.length];
            Arrays.fill(queryVector, 0);


            //Calculate Query Vector
            qqAnnotation = queryAnnMap.get(qq.getQueryID());
//            for (int j = 0; j < entitySpace.length; j++) {
//                for (Annotation qqAnn : qqAnnotation) {
//                    if (entitySpace[j].equalTo(qqAnn)) {
//                        queryVector[j] += 1;
//                    }
//                }
//            }

            //logger.info("Query vector calculated");

            for (int i = 0; i < td.scoreDocs.length; i++) {
                Arrays.fill(docSpace, 0);
                d = searcher.doc(td.scoreDocs[i].doc);
                String docNO = d.getField(TrecDocIterator.DOCNO).stringValue().trim();
                docAnnotation = collectionAnnMap.get(docNO);
                // Calculate document Vector
//                for (int j = 0; j < entitySpace.length; j++) {
//                    for (Annotation docAnn : docAnnotation) {
//                        if (entitySpace[j].equalTo(docAnn)) {
//                            docSpace[j] += 1;
//                        }
//                    }
//                }

                // calculate spot distances
                double entitySpotSigma = 0.0;
                List <Double> similarityScores = new ArrayList<>();
                for (Annotation qqAnn : qqAnnotation) {
                    for (Annotation docAnn : docAnnotation) {
                        for (String qqToken : TRECAnalyzer.getTermList(trecAnalyzer, qqAnn.getSpot())) {
                            for (String spaceToken : TRECAnalyzer.getTermList(trecAnalyzer, docAnn.getSpot())) {
                                entitySpotSigma += jw.similarity(qqToken, spaceToken);
                                similarityScores.add(jw.similarity(qqToken, spaceToken));
                            }
                        }
                    }
                }

                // Symetrics similarity
                double symmetricSIM = symmetricSIMScore(queryTitle, d, word2Vec);

                // q and d distance similarity
//                DistanceMeasure earthMoversDistance = new ManhattanDistance();
//                double cosine = earthMoversDistance.compute(queryVector, docSpace);


                /**
                 * X_{changed} = \frac{X - \mu}{\sigma}
                 */
                double[] simArray = new double[similarityScores.size()];
                for(int p = 0; i < similarityScores.size(); i++) simArray[p] = similarityScores.get(p);
                 sd2.evaluate(simArray);


                double newScore = (tetaVal * (symmetricSIM)) + ((1 - tetaVal) * entitySpotSigma);

                logger.info(" document " + i + " entitySpotSigma similarity: " + entitySpotSigma + " Symmetric: " + symmetricSIM + " score from: " + td.scoreDocs[i].score + " to: " + newScore);
                td.scoreDocs[i].score = (float) newScore;
//                td.scoreDocs[i].score = (float) cosine;
            }
        } catch (IOException e) {
            e.printStackTrace();
            logger.error(e.getMessage());
        }

        return td;
    }

    private void getCollectionSpace(List<Annotation> collectionVector, String docNO, String contents) {
        TagResponse tagResponse;
        List<Annotation> docAnnotation = new ArrayList<>();
        try {
            // if the document is not already cached
            if (!collectionAnnMap.containsKey(docNO)) {
                tagResponse = tagMeClient
                        .tag()
                        .longText(30) // variable!!!
                        .text(StanfordLemmatizer.getInstance().lemmatizeToString(contents))
                        .execute();

                if (tagResponse != null) {
                    docAnnotation = tagResponse.getAnnotations();
                    // Cache annotations for Document
                    String documentAnnotation = mapper.writeValueAsString(docAnnotation);


                    // Update the map
                    collectionAnnMap.put(docNO, docAnnotation);
                    //Write to file
                    documentAnnotation = docNO+ "\t" + documentAnnotation;
                    Utilities.writeToFile(collectionAnnFileName, documentAnnotation);
                    //logger.info(documentAnnotation);
                }
            }

            //Populate collection vector
            for (Annotation annotation : collectionAnnMap.get(docNO)) {
                if (!collectionVector.contains(annotation))
                    collectionVector.add(annotation);
            }
        } catch (Exception e) {
            logger.error("Time out on the tag response");
        }
    }

    /**
     * Implementation
     * http://dl.acm.org/citation.cfm?id=2884862&dl=ACM&coll=DL&CFID=865546466&CFTOKEN=67374010
     *
     * @return
     * @throws IOException
     */
    public double symmetricSIMScore(String queryTitle, Document d, Word2Vec word2Vec) throws IOException {
        double symmetricSIM = 0d;
        try {

            double sim_T_S;
            double sim_S_T;
            // Go through top scored docs
            String contents = d.getField(TrecDocIterator.CONTENTS).stringValue();
            contents = (contents.length() > 2000) ? contents.substring(0, 2000) : contents;

            sim_T_S = sim_BOW(TRECAnalyzer.getTermList(trecAnalyzer, queryTitle),
                    TRECAnalyzer.getTermList(trecAnalyzer, contents),
                    word2Vec);
            sim_S_T = sim_BOW(TRECAnalyzer.getTermList(trecAnalyzer, contents),
                    TRECAnalyzer.getTermList(trecAnalyzer, queryTitle),
                    word2Vec);
            symmetricSIM = sim_T_S + sim_S_T;
//            logger.info(" Symmetric: " + symmetricSIM);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return symmetricSIM;
    }




    public double sim_BOW(List<String> BOW_T, List<String> BOW_S, Word2Vec word2Vec) {
        double simSigma = 0d, IDFSigma = 0d;

        for (String w_T : BOW_T) {
            simSigma += (getScore(sim(w_T, BOW_S, word2Vec)) * idf(w_T));
            IDFSigma += idf(w_T);
        }
        return simSigma / IDFSigma;
    }

    /**
     * Compute Maximum Similarity word w and bag-of-words T
     *
     * @param term_w
     * @param BOW_T
     * @param word2Vec
     * @return
     */
    public double sim(String term_w, List<String> BOW_T, Word2Vec word2Vec) {
        double max = 0d;
        double sim;
        for (String w_p : BOW_T) {
            sim = getScore(word2Vec.similarity(term_w, w_p));
            max = Math.max(sim, max);
        }
        return max;
    }

    private List<Annotation> getTitleAnnotation(String documentTitle, String docNO) {
        List<Annotation> titleAnnotation = new ArrayList<>();

        try {
            // if the document is not already cached
            if (!docTitleAnnMap.containsKey(docNO)) {
                TagResponse tagResponse = tagMeClient
                        .tag()
                        .text(StanfordLemmatizer.getInstance().lemmatizeToString(documentTitle))
                        .execute();

                if (tagResponse != null) {
                    titleAnnotation = tagResponse.getAnnotations();
                    // Cache annotations for Document
                    String titleAnn = mapper.writeValueAsString(titleAnnotation);
                    // Update the map
                    docTitleAnnMap.put(docNO, titleAnnotation);
                    //Write to file
                    titleAnn = docNO+ "\t" + titleAnn;
                    Utilities.writeToFile(documentTitleAnnFileName, titleAnn);
                    logger.info("document title: " + documentTitle + " " + titleAnn);
                }
            } else {
                titleAnnotation = docTitleAnnMap.get(docNO);
            }

        } catch (Exception e) {
            logger.error("Time out on the tag response");
        }

        return titleAnnotation;
    }

    /* Analyze/judge results for a single quality query; optionally log them. */
    private QualityStats analyzeQueryResults(QualityQuery qq, Query q, TopDocs td, Judge judge, PrintWriter logger, long searchTime) throws IOException {
        QualityStats stts = new QualityStats(judge.maxRecall(qq), searchTime);
        ScoreDoc sd[] = td.scoreDocs;
        long t1 = System.currentTimeMillis(); // extraction of first doc name we measure also construction of doc name extractor, just in case.
        DocNameExtractor xt = new DocNameExtractor(docNameField);
        for (int i = 0; i < sd.length; i++) {
            String docName = xt.docName(searcher, sd[i].doc);
            long docNameExtractTime = System.currentTimeMillis() - t1;
            t1 = System.currentTimeMillis();
            boolean isRelevant = judge.isRelevant(docName, qq);
            stts.addResult(i + 1, isRelevant, docNameExtractTime);
        }
        if (logger != null) {
            logger.println(qq.getQueryID() + "  -  " + q);
            stts.log(qq.getQueryID() + " Stats:", 1, logger, "  ");
        }
        return stts;
    }


    public void termStats(IndexReader reader) throws Exception {
        Fields fields = MultiFields.getFields(reader);
        Terms terms = fields.terms(TrecDocIterator.CONTENTS);
        TermsEnum iterator = terms.iterator();
        BytesRef byteRef;
        String term;

        numberOfDocsIndex = reader.numDocs();
        while ((byteRef = iterator.next()) != null) {
            term = new String(byteRef.bytes, byteRef.offset, byteRef.length);
            term_total_freq_map.put(term, iterator.totalTermFreq());
            term_doc_freq_map.put(term, iterator.docFreq());

        }
    }

    /**
     * Implemented as <code>log((docCount+1)/(docFreq+1)) + 1</code>.
     */
    public float idf(String term) {
        long docFreq = term_doc_freq_map.containsKey(term) ? term_doc_freq_map.get(term) : 0;
        return (float) (Math.log((numberOfDocsIndex + 1) / (double) (docFreq + 1)) + 1.0);
    }

    public float tf(float freq) {
        return (float)Math.sqrt(freq);
    }

    //TODO FIXMEEEEEE
    public float tf_idf (String term) {
        float tf = term_doc_freq_map.containsKey(term) ? term_doc_freq_map.get(term) : 0;
        return tf * idf(term);
    }


    public double getScore(double score) {
        return (Double.isNaN(score)) ? 0d : score;
    }


    public void TFIDFscore(IndexReader reader,String field,String term) throws IOException
    {
//        float tf = 1;
//        float idf = 0;
//        float tfidf_score;
//        float[] tfidf = null;
//
//        /** GET TERM FREQUENCY & IDF **/
//        TFIDFSimilarity tfidfSIM = new ClassicSimilarity();
//        Bits liveDocs = MultiFields.getLiveDocs(reader);
//        TermsEnum termEnum = MultiFields.getTerms(reader, field).iterator(null);
//        BytesRef bytesRef;
//        while ((bytesRef = termEnum.next()) != null)
//        {
//            if(bytesRef.utf8ToString().trim() == term.trim())
//            {
//                if (termEnum.seekExact(bytesRef, true)) {
//                    idf = tfidfSIM.idf(termEnum.docFreq(), reader.numDocs());
//
//                    DocsEnum docsEnum = termEnum.docs(liveDocs, null);
//
//                    termEnum.d
//                    if (docsEnum != null)
//                    {
//                        int doc;
//                        while((doc = docsEnum.nextDoc())!= DocIdSetIterator.NO_MORE_DOCS)
//                        {
//                            tf = tfidfSIM.tf(docsEnum.freq());
//                            tfidf_score = tf*idf;
//                            System.out.println(" -tfidf_score- " + tfidf_score);
//                        }
//                    }
//                }
//            }
//        }
    }

}
