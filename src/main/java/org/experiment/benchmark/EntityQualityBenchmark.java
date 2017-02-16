package org.experiment.benchmark;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.experiment.TREC.BagOfEntitiesEx;
import org.experiment.TREC.TrecDocIterator;
import org.experiment.analyzer.TRECAnalyzer;
import org.experiment.preprocessing.StanfordLemmatizer;
import org.experiment.similarities.ByWeightComparator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tagme4j.TagMeClient;
import org.tagme4j.TagMeWikiHelper;
import org.tagme4j.model.Annotation;
import org.tagme4j.response.TagResponse;
import org.utils.Utilities;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Created by sonic on 18/12/16.
 */
public class EntityQualityBenchmark extends QualityBenchmark {

    private static final Logger logger = LoggerFactory.getLogger(EntityQualityBenchmark.class);
    //public static final int numOfResults = 100;

    /**
     * maximal number of queries that this quality benchmark runs. Default: maxint. Useful for debugging.
     */
    private int maxQueries = Integer.MAX_VALUE;
    /**
     * maximal number of results to collect for each query. Default: 1000.
     */
    private int maxResults = 500;

    double tetaVal;

    public static String collectionAnnFileName = "dataset/WT10GDOCAnnotation.bin";
//    public static String collectionAnnFileName = "dataset/APDOCAnnotation.bin";
//    public static String collectionAnnFileName = "dataset/documentAnnotation.txt";
    public static String documentTitleAnnFileName = "dataset/documentTitleAnnotation.txt";
    private static HashMap<String, List<Annotation>> collectionAnnMap = new HashMap<>();

    private static HashMap<String, List<Annotation>> docTitleAnnMap = new HashMap<>();
    private static HashMap<String, List<Annotation>> queryAnnMap = new HashMap<>();
    List<Annotation> qqAnnotation;

    Word2Vec word2Vec;
    TRECAnalyzer trecAnalyzer = new TRECAnalyzer();

    HashMap<String, Long> term_total_freq_map = new HashMap<String, Long>();// term frequency in collection
    HashMap<String, Integer> term_doc_freq_map = new HashMap<String, Integer>();// term frequency in a document

    TagMeClient tagMeClient;
    ObjectMapper mapper = new ObjectMapper();
    TagResponse tagResponse;

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
     * @param docNameField name of field containing the document name.
     *                     This allows to extract the doc name for search results,
     */
    public EntityQualityBenchmark(QualityQuery[] qqs, QualityQueryParser qqParser, IndexSearcher searcher, String docNameField) {
        super(qqs, qqParser, searcher, docNameField);

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

        Directory directory;  //
        try {
            directory = FSDirectory.open(Paths.get(BagOfEntitiesEx.index));
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

        TagResponse tagResponse;
        Document d;
        double PCLength;
        double PCdocument;
        TRECAnalyzer trecAnalyzer = new TRECAnalyzer();

        String queryTitle = qq.getValue("title").toLowerCase().trim();
        List<Annotation> docAnnotation = new ArrayList<>();

        logger.info("Start Query update, queryID : " + qq.getQueryID() + " : " + StanfordLemmatizer.getInstance().lemmatizeToString(queryTitle) + " teta: "  + tetaVal);

        try {
            qqAnnotation = queryAnnMap.get(qq.getQueryID());
            logger.info(qqAnnotation.toString());
            // Set number of Top documents
            for (int i = 0; i < td.scoreDocs.length; i++) {
                PCLength = 0.0;
                PCdocument = 0.0; //pc (t, x)
                d = searcher.doc(td.scoreDocs[i].doc);
                String docNO = d.getField(TrecDocIterator.DOCNO).stringValue().trim();
                String contents = d.getField(TrecDocIterator.CONTENTS).stringValue();
                contents = (contents.length() > 2000) ? contents.substring(0, 2000) : contents;

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
                    } else {
                        docAnnotation = collectionAnnMap.get(docNO);
                    }

                } catch (Exception e) {
                    logger.error("Time out on the tag response");
                }

                // Calculate Pseudo Count Document - pc (t, x)
                for (Annotation a : docAnnotation) {
                    for (Annotation qa : qqAnnotation) {
                        if (a.getId() == qa.getId()) { // check probability
                            double termIDF = 0d;
                            String spotTitle = qa.getTitle() + " " + qa.getSpot();
                            for (String qterm : TRECAnalyzer.getTermList(trecAnalyzer, spotTitle)) {
                                termIDF += idf(qterm);
                            }
                            PCdocument += (tetaVal * (termIDF)) + ((1 - tetaVal) * (a.getLink_probability() + qa.getLink_probability()));
                        }
                    }
                }

                // Calculate Pseudo Length - pl (x)
                for (Annotation qa : qqAnnotation) {
                    int count = 0;
                    for (List<Annotation> docAnnList : collectionAnnMap.values()) {
                        for (Annotation docAnn : docAnnList) {
                            if (qa.getId() == docAnn.getId()) {
                                count++;
                            }
                        }
                    }
                    double termIDF = 0d;
                    String spotTitle = qa.getTitle() + " " + qa.getSpot();
                    for (String qterm : TRECAnalyzer.getTermList(trecAnalyzer, spotTitle)) {
                        termIDF += idf(qterm);
                    }
                    PCLength += (tetaVal * (termIDF)) + ((1 - tetaVal) * (count * qa.getLink_probability()));
                }

                // (MLE) Maximum likelihood estimate
//                double MLE = PCdocument / ( (PCLength == 0) ? 1 : PCLength );
                double MLE = PCdocument / PCLength ;

                //double symmetricSIM = symmetricSIMScore(queryTitle, d, word2Vec);
                //double newScore = tetaVal * (symmetricSIM) + ((1 - tetaVal) * MLE);

                double newScore = (tetaVal * (td.scoreDocs[i].score)) + ((1 - tetaVal) * MLE);

                logger.info(" document " + i + " MLE: " + MLE +  " score from: " + td.scoreDocs[i].score + " to: " + newScore);
                td.scoreDocs[i].score = (float) newScore;
            }
        } catch (IOException e) {
            e.printStackTrace();
            logger.error(e.getMessage());
        }

        return td;
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
