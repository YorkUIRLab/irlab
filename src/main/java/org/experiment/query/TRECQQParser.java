package org.experiment.query;

import org.apache.lucene.benchmark.quality.QualityQuery;
import org.apache.lucene.benchmark.quality.QualityQueryParser;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.queryparser.classic.QueryParserBase;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;
import org.experiment.analyzer.TRECAnalyzer;

/**
 * Created by sonic on 19/10/16.
 *
 * @Author Fanghong
 */
public class TRECQQParser implements QualityQueryParser {

    private String qqNames[];
    private String indexField;
    ThreadLocal<QueryParser> queryParser = new ThreadLocal<QueryParser>();

    /**
     * Constructor of a simple qq parser.
     *
     * @param qqNames    name-value pairs of quality query to use for creating the query
     * @param indexField corresponding index field
     */
    public TRECQQParser(String qqNames[], String indexField) {
        this.qqNames = qqNames;
        this.indexField = indexField;
    }

    /**
     * Constructor of a simple qq parser.
     *
     * @param qqName     name-value pair of quality query to use for creating the query
     * @param indexField corresponding index field
     */
    public TRECQQParser(String qqName, String indexField) {
        this(new String[]{qqName}, indexField);
    }

    /* (non-Javadoc)
     * @see org.apache.lucene.benchmark.quality.QualityQueryParser#parse(org.apache.lucene.benchmark.quality.QualityQuery)
     */
    @Override
    public Query parse(QualityQuery qq) throws ParseException {
        QueryParser qp = queryParser.get();
        if (qp==null) {
            qp = new QueryParser(indexField, new TRECAnalyzer());
            queryParser.set(qp);
        }

        BooleanQuery.Builder bq = new BooleanQuery.Builder();

        for (String qqName : qqNames)
            bq.add(qp.parse(QueryParserBase.escape(qq.getValue(qqName))), BooleanClause.Occur.SHOULD);

        return bq.build();
    }
}
