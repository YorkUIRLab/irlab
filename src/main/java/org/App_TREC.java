package org;

import org.apache.lucene.benchmark.quality.QualityQuery;
import org.apache.lucene.benchmark.quality.trec.TrecTopicsReader;
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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

@SpringBootApplication
@ComponentScan ({"org.experiment", "org.repository", "org.tagme4j", "org.utils"})
public class App_TREC {

    private static Logger log = LoggerFactory.getLogger(App_TREC.class);

    @Autowired
    private WikiRepo wikiRepo;

    @Autowired
    private WikiWord2Vec wikiWord2Vec;

    @Autowired
    TagMeWikiHelper tagMeWikiHelper;

    public static void main(String[] args) {
        SpringApplication.run(App_TREC.class, args);
    }

    @Bean
    public CommandLineRunner commandLineRunner(ApplicationContext ctx) {

        log.info("hello");



        return args -> {};
    }
}
