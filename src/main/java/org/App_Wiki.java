package org;

import org.apache.lucene.benchmark.quality.QualityQuery;
import org.apache.lucene.benchmark.quality.trec.TrecTopicsReader;
import org.experiment.wikipedia.model.WikiPage;
import org.experiment.wikipedia.wikiclean.Util;
import org.experiment.word2vec.Wiki;
import org.experiment.word2vec.WikiWord2Vec;
import org.repository.WikiRepo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.tagme4j.TagMeWikiHelper;
import org.utils.Utilities;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.Arrays;

@SpringBootApplication
@ComponentScan ({"org.experiment", "org.repository", "org.tagme4j", "org.utils"})
public class App_Wiki  {

    private static Logger log = LoggerFactory.getLogger(App_Wiki.class);

    @Autowired
    private WikiRepo wikiRepo;

    @Autowired
    private WikiWord2Vec wikiWord2Vec;

    @Autowired
    TagMeWikiHelper tagMeWikiHelper;

    public static void main(String[] args) {
        SpringApplication.run(App_Wiki.class, args);
    }

    @Bean
    public CommandLineRunner commandLineRunner(ApplicationContext ctx) {
        return args -> {

            File topicsFile = new File("/media/sonic/Windows/TREC/WT2G/topics/topics.wt2g");
            TrecTopicsReader qReader = new TrecTopicsReader();
            QualityQuery qqs[] = qReader.readQueries(new BufferedReader(new FileReader(topicsFile)));

            boolean isDone = true;
            if (!isDone)
                tagMeWikiHelper.processWikiEntities(qqs);

            File dir = new File(TagMeWikiHelper.WIKI_QUERY_EXPANSION_BASE_LOCATION);
            wikiWord2Vec.processWikiEmbedding(dir);

        };
    }
}
