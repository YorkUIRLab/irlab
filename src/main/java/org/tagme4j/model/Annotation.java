/**
 * Copyright (c) 2016 Enrico Candino
 * <p>
 * Distributed under the MIT License.
 */
package org.tagme4j.model;

import java.util.List;

public class Annotation {

    private int id;
    private String title;
    private String wiki_abstract;
    private int start;

    public String getWiki_abstract() {
        return wiki_abstract;
    }

    public void setWiki_abstract(String wiki_abstract) {
        this.wiki_abstract = wiki_abstract;
    }

    private int end;
    private double rho;
    private String spot;
    private double link_probability;
    private List<String> dbpedia_categories;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public int getStart() {
        return start;
    }

    public void setStart(int start) {
        this.start = start;
    }

    public int getEnd() {
        return end;
    }

    public void setEnd(int end) {
        this.end = end;
    }

    public double getRho() {
        return rho;
    }

    public void setRho(double rho) {
        this.rho = rho;
    }

    public String getSpot() {
        return spot;
    }

    public void setSpot(String spot) {
        this.spot = spot;
    }

    public double getLink_probability() {
        return link_probability;
    }

    public void setLink_probability(double link_probability) {
        this.link_probability = link_probability;
    }

    public List<String> getDbpedia_categories() {
        return dbpedia_categories;
    }

    public void setDbpedia_categories(List<String> dbpedia_categories) {
        this.dbpedia_categories = dbpedia_categories;
    }

    @Override
    public String toString() {
        return "Annotation{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", wiki_abstract='" + wiki_abstract + '\'' +
                ", start=" + start +
                ", end=" + end +
                ", rho=" + rho +
                ", spot='" + spot + '\'' +
                ", link_probability=" + link_probability +
                ", dbpedia_categories=" + dbpedia_categories +
                '}';
    }
}
