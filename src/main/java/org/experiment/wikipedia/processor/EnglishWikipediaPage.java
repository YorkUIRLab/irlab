package org.experiment.wikipedia.processor;

/**
 * Created by sonic on 06/12/16.
 */
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringEscapeUtils;

/**
 * An English page from Wikipedia.
 *
 * @author Peter Exner
 * @author Ferhan Ture
 */
public class EnglishWikipediaPage extends WikipediaPage {
    /**
     * Language dependent identifiers of disambiguation, redirection, and stub pages.
     */
    private static final String IDENTIFIER_REDIRECTION_UPPERCASE = "#REDIRECT";
    private static final String IDENTIFIER_REDIRECTION_LOWERCASE = "#redirect";
    private static final String IDENTIFIER_STUB_TEMPLATE = "stub}}";
    private static final String IDENTIFIER_STUB_WIKIPEDIA_NAMESPACE = "Wikipedia:Stub";
    private static final Pattern disambPattern = Pattern.compile("\\{\\{disambig\\w*\\}\\}", Pattern.CASE_INSENSITIVE);
    private static final String LANGUAGE_CODE = "en";

    /**
     * Creates an empty <code>EnglishWikipediaPage</code> object.
     */
    public EnglishWikipediaPage() {
        super();
    }



    @Override
    protected void processPage(String s) {
        this.language = LANGUAGE_CODE;

        // parse out title
        int start = s.indexOf(XML_START_TAG_TITLE);
        int end = s.indexOf(XML_END_TAG_TITLE, start);
     //   this.title = StringEscapeUtils.unescapeHtml(s.substring(start + 7, end));

        // determine if article belongs to the article namespace
        start = s.indexOf(XML_START_TAG_NAMESPACE);
        end = s.indexOf(XML_END_TAG_NAMESPACE);
        this.isArticle = start == -1 ? true : s.substring(start + 4, end).trim().equals("0");
        // add check because namespace tag not present in older dumps

        // parse out the document id
        start = s.indexOf(XML_START_TAG_ID);
        end = s.indexOf(XML_END_TAG_ID);
//        this.mId = s.substring(start + 4, end);

        // parse out actual text of article
        this.textStart = s.indexOf(XML_START_TAG_TEXT);
        this.textEnd = s.indexOf(XML_END_TAG_TEXT, this.textStart);

        // determine if article is a disambiguation, redirection, and/or stub page.
      //  Matcher matcher = disambPattern.matcher(page);
      //  this.isDisambig = matcher.find();
        this.isRedirect = s.substring(this.textStart + XML_START_TAG_TEXT.length(), this.textStart + XML_START_TAG_TEXT.length() + IDENTIFIER_REDIRECTION_UPPERCASE.length()).compareTo(IDENTIFIER_REDIRECTION_UPPERCASE) == 0 ||
                s.substring(this.textStart + XML_START_TAG_TEXT.length(), this.textStart + XML_START_TAG_TEXT.length() + IDENTIFIER_REDIRECTION_LOWERCASE.length()).compareTo(IDENTIFIER_REDIRECTION_LOWERCASE) == 0;
        this.isStub = s.indexOf(IDENTIFIER_STUB_TEMPLATE, this.textStart) != -1 ||
                s.indexOf(IDENTIFIER_STUB_WIKIPEDIA_NAMESPACE) != -1;
    }
}