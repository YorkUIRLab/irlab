package org.experiment.benchmark;

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
import org.experiment.TREC.BagOfEntitiesEmbeddingEx;
import org.experiment.TREC.TrecDocIterator;
import org.experiment.analyzer.TRECAnalyzer;
import org.experiment.similarities.ByWeightComparator;
import org.experiment.word2vec.MyModelUtils;
import org.experiment.word2vec.TRECWord2Vec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tagme4j.TagMeWikiHelper;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Created by sonic on 18/12/16.
 */
public class EmbeddingQualityBenchmark extends QualityBenchmark {

    private static final Logger logger = LoggerFactory.getLogger(EmbeddingQualityBenchmark.class);

    IndexReader reader;
    Word2Vec word2Vec;

    /**
     * maximal number of queries that this quality benchmark runs. Default: maxint. Useful for debugging.
     */
    private int maxQueries = Integer.MAX_VALUE;
    /**
     * Number of Documents in index
     */
    int numberOfDocsIndex;


    /**
     * maximal number of results to collect for each query. Default: 1000.
     */
    private int maxResults = 500;

    private double teta;

    public void setTeta(double teta) {
        this.teta = teta;
    }

    HashMap<String, Long> term_total_freq_map = new HashMap<String, Long>();// term frequency in collection
    HashMap<String, Integer> term_doc_freq_map = new HashMap<String, Integer>();// term frequency in a document
    HashMap<Integer, Integer> doc_length_map = new HashMap<Integer, Integer>();// document  length
    HashMap<Integer, Double> doc_avg_tf_map = new HashMap<Integer, Double>();// average of term frequency in a document


    /**
     * Create a QualityBenchmark.
     *
     * @param qqs          quality queries to run.
     * @param qqParser     HTMLParser for turning QualityQueries into Lucene Queries.
     * @param searcher     index to be searched.
     * @param docNameField name of field containing the document name.
     *                     This allows to extract the doc name for search results,
     */
    public EmbeddingQualityBenchmark(QualityQuery[] qqs, QualityQueryParser qqParser, IndexSearcher searcher, String docNameField) {
        super(qqs, qqParser, searcher, docNameField);

        Directory directory = null;  //
        try {
            directory = FSDirectory.open(Paths.get(BagOfEntitiesEmbeddingEx.index));
            reader = DirectoryReader.open(directory);
            termStats(reader);
        } catch (Exception e) {
            e.printStackTrace();
        }
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

            //
            updateScore(qq, td, searcher);
            // Sort scores
            Arrays.sort(td.scoreDocs, new ByWeightComparator());

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

    /**
     * Implementation
     * http://dl.acm.org/citation.cfm?id=2884862&dl=ACM&coll=DL&CFID=865546466&CFTOKEN=67374010
     *
     * @param qq
     * @param td
     * @param searcher
     * @return
     * @throws IOException
     */
    public TopDocs updateScore(QualityQuery qq, TopDocs td, IndexSearcher searcher) throws IOException {

        Document d;
        TRECAnalyzer trecAnalyzer = new TRECAnalyzer();
        String queryTitle = qq.getValue("title").toLowerCase().trim();

        logger.info("Start Query update");
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

            double sim_T_S;
            double sim_S_T;
            // Go through top scored docs
            for (int i = 0; i < td.scoreDocs.length; i++) {
                d = searcher.doc(td.scoreDocs[i].doc);
                String contents = d.getField(TrecDocIterator.CONTENTS).stringValue();
                contents = (contents.length() > 2000) ? contents.substring(0, 2000) : contents;

                sim_T_S = sim_BOW(TRECAnalyzer.getTermList(trecAnalyzer, queryTitle),
                        TRECAnalyzer.getTermList(trecAnalyzer, contents),
                        word2Vec);
                sim_S_T = sim_BOW(TRECAnalyzer.getTermList(trecAnalyzer, contents),
                        TRECAnalyzer.getTermList(trecAnalyzer, queryTitle),
                        word2Vec);

                double symmetricSIM = sim_T_S + sim_S_T;
                double newScore = (teta * td.scoreDocs[i].score) + ((1 - teta) * symmetricSIM);
                logger.info(" document " + i + " score from: " + td.scoreDocs[i].score + " to: " + newScore +
                        " Symmetric: " + symmetricSIM);
                td.scoreDocs[i].score = (float) newScore;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return td;
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

    public TopDocs updateScore(QualityQuery qq, TopDocs td, String docNameField, IndexSearcher searcher) throws IOException {

        Document d;
        double scoreBoost;
        TRECAnalyzer trecAnalyzer = new TRECAnalyzer();

        String queryTitle = qq.getValue("title").toLowerCase().trim();

        logger.info("Start Query update");

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
                    word2Vec.setModelUtils(new MyModelUtils());
                }
            }
        } else {
            logger.error("Could not find the directory: " + dir.getAbsolutePath());
        }

        try {

            /* TODO
             *  3- Function - foreach (t in Q) -> sum (tWeight) * IDF (t)
             */

            // Set number of Top documents
            for (int i = 0; i < td.scoreDocs.length; i++) {
                d = searcher.doc(td.scoreDocs[i].doc);

                String contents = d.getField(TrecDocIterator.CONTENTS).stringValue();
                contents = (contents.length() > 1500) ? contents.substring(0, 1500) : contents;

                double tran = 0.0d;
                double sigmaDocQuery = 0d;
                double s = 0d;
                double sigmaQueryDocSimilarity = 0d;
                teta = 0.25;
                double k = 0.15;
                int KNN = 5;

                // Word Embedding Query Expansion Similarity
//                List <String> queryTitleList = TRECAnalyzer.getTermList (trecAnalyzer, queryTitle);
//                meanCosineSimilarity(queryTitleList, KNN);


                // tran (t_i, t_j) = (sim (t_i, t_j) sum )
                // s (j) = (1 - tetaVal) + k * sum (tran(t_i, t_j))
                for (String qterm : TRECAnalyzer.getTermList(trecAnalyzer, queryTitle)) {
                    sigmaDocQuery = 0d;
                    tran = 0;

                    for (String dterm : TRECAnalyzer.getTermList(trecAnalyzer, contents)) {
                        sigmaDocQuery += getScore(word2Vec.similarity(qterm, dterm));
                    }

                    for (String dterm : TRECAnalyzer.getTermList(trecAnalyzer, contents)) {
                        if (sigmaDocQuery != 0)
                            tran += getScore(word2Vec.similarity(qterm, dterm));
                    }

                    tran = tran / sigmaDocQuery;
                    //s (j)
                    s = (1 - k) + k * tran;
                    sigmaQueryDocSimilarity += s * idf(qterm);
                    //logger.info("s: " + s + " tran: " + tran + " idf: " + idf(qterm));
                }


                double newScore = td.scoreDocs[i].score;
//                newScore = (tetaVal * (sigmaQueryDocSimilarity / (1 + sigmaQueryDocSimilarity))) +
//                        ((1 - tetaVal) * (td.scoreDocs[i].score / (1 + td.scoreDocs[i].score)));

                newScore = (teta * td.scoreDocs[i].score) + ((1 - teta) * sigmaQueryDocSimilarity);

                logger.info(" document " + i + " score from: " + td.scoreDocs[i].score + " to: " + newScore +
                        " sigmaQueryDocSimilarity: " + sigmaQueryDocSimilarity + " sigmaDocQuery: " + sigmaDocQuery);
                td.scoreDocs[i].score = (float) newScore;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return td;
    }

    private double meanCosineSimilarity(List<String> queryTitleTerms, int KNN) throws IOException {
        double sigmaQueryTerm = 1;

        for (String qTerm : queryTitleTerms) {
            Collection<String> qTermKNN = word2Vec.wordsNearest(qTerm, KNN);

            for (String qTermExp : qTermKNN) {
                for (String q : qTermKNN)
                    sigmaQueryTerm *= word2Vec.similarity(qTermExp, q);
            }
        }
        return sigmaQueryTerm;
    }

    public double getScore(double score) {
        return (Double.isNaN(score)) ? 0d : score;
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


    public void termStats(IndexReader reader) throws Exception //
    {
        Fields fields = MultiFields.getFields(reader);
        Terms terms = fields.terms(BagOfEntitiesEmbeddingEx.field);
        TermsEnum iterator = terms.iterator();
        BytesRef byteRef = null;
        String term;
        numberOfDocsIndex = reader.numDocs();
        while ((byteRef = iterator.next()) != null) {
            term = new String(byteRef.bytes, byteRef.offset, byteRef.length);
            term_total_freq_map.put(term, iterator.totalTermFreq());
            term_doc_freq_map.put(term, iterator.docFreq());
            //total_term_freq += iterator.totalTermFreq();
        }
    }

    /**
     * Implemented as <code>log((docCount+1)/(docFreq+1)) + 1</code>.
     */
    public float idf(String term) {
        long docFreq = term_doc_freq_map.containsKey(term) ? term_doc_freq_map.get(term) : 0;
        return (float) (Math.log((numberOfDocsIndex + 1) / (double) (docFreq + 1)) + 1.0);
    }


}
