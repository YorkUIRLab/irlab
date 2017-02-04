package org.repository;

import org.experiment.wikipedia.model.WikiPage;
import org.springframework.data.mongodb.repository.MongoRepository;

/**
 * Created by sonic on 24/01/17.
 */
public interface WikiRepo extends MongoRepository<WikiPage, String> {

    public WikiPage findById(String id);

}
