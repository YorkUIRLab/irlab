package org.experiment.wikipedia.wikiclean;

import com.mongodb.Mongo;
import edu.stanford.nlp.ling.HasWord;
import edu.stanford.nlp.ling.Sentence;
import edu.stanford.nlp.process.DocumentPreprocessor;
import org.apache.commons.io.FileUtils;
import org.experiment.analyzer.WikiPediaAnalyzer;
import org.experiment.wikipedia.model.WikiPage;
import org.experiment.word2vec.Wiki;
import org.repository.WikiRepo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;
import org.tagme4j.TagMeQueryExpander;
import org.utils.Utilities;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import java.io.*;
import java.util.List;

/**
 * Created by sonic on 08/01/17.
 */
@SpringBootApplication
@ComponentScan()
public class Util  {

    private static Logger log = LoggerFactory.getLogger(Util.class);

    @Autowired
    private WikiRepo wikiRepo;

    public static void main(String[] args) {
        SpringApplication.run(Util.class, args);
    }


    public void run(String... args) throws Exception {


        BufferedReader is = Utilities.readGZip("/media/sonic/Windows/TREC/wikipedia-clean1.bin");
        WikiPage wikiPage;

        String line;
        while ((line = is.readLine()) != null) {
            //Id, Title, Content
            wikiPage = new WikiPage(line.split("\t")[0], line.split("\t")[1],line.split("\t")[2]);
            log.info(wikiPage.toString());
            wikiRepo.save(wikiPage);
        }
    }

//    public static void main(String[] argv) throws Exception {

//        String raw = FileUtils.readFileToString(new File("index/enwiki-20120104-id12.xml"), "UTF-8");
//        WikiClean cleaner = new WikiCleanBuilder().build();
//        String content = cleaner.clean(raw);
//
//        System.out.println (content.replaceAll("(\\r|\\n|\\t)", " ") );
//        System.out.println (WikiPediaAnalyzer.getTermList(new WikiPediaAnalyzer(), content));
//
//        Reader reader = new StringReader(content.replaceAll("(\\r|\\n|\\t)", " ") );
//        DocumentPreprocessor dp = new DocumentPreprocessor(reader);
//        for (List<HasWord> sentence : dp) {
//            System.out.print (Sentence.listToString(sentence));
//        }
//    }

}


