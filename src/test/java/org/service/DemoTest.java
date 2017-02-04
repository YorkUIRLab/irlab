package org.service;

import org.Application;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * Created by sonic on 3/10/16.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(Application.class)
public class DemoTest {

    private static final Logger logger = LoggerFactory.getLogger(DemoTest.class);


    @Test
    public void DemoTest () throws Exception {
        logger.info("hello");

    }



}
