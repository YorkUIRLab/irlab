package org;

import org.experiment.wikipedia.model.WikiPage;
import org.experiment.wikipedia.wikiclean.Util;
import org.repository.WikiRepo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.utils.Utilities;

import java.io.BufferedReader;
import java.util.Arrays;

@SpringBootApplication
@ComponentScan ({"org.controller" , "org.service"})
public class Application {

    private static Logger log = LoggerFactory.getLogger(Util.class);

    @Autowired
    private WikiRepo wikiRepo;

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    public void run(String... args) throws Exception {

        BufferedReader is = Utilities.readGZip("/media/sonic/Windows/TREC/wikipedia-clean1.bin");
        WikiPage wikiPage;

        String line;
        int count =0;
//        while ((line = is.readLine()) != null) {
//            //Id, Title, Content
//            if (line.split("\t").length == 3) {
//                wikiPage = new WikiPage(line.split("\t")[0], line.split("\t")[1],line.split("\t")[2]);
//                //log.info(wikiPage.toString());
//                wikiRepo.save(wikiPage);
//                count++;
//                if (count == 1000){
//                    log.info(wikiPage.toString());
//                    count = 0;
//                }
//
//            }
//        }

        log.info(wikiRepo.findById("22228064").toString());

        for (WikiPage wikiPage1 : wikiRepo.findAll(Arrays.asList("52741835","4079"))) {
            log.info(wikiPage1.toString());
        }

    }

}
