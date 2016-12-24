/**
 * Copyright (c) 2016 Enrico Candino
 * <p>
 * Distributed under the MIT License.
 */
package org.tagme4j.response;

import org.tagme4j.model.Relatedness;
import java.util.List;

public class RelResponse extends TagMeResponse {

    private List<Relatedness> result;

    public List<Relatedness> getResult() {
        return result;
    }

    public void setResult(List<Relatedness> result) {
        this.result = result;
    }
}
