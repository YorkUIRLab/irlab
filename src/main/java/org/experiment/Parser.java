package org.experiment;

import org.cyberneko.html.parsers.SAXParser;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.*;

/**
 * Created by sonic on 16/10/16.
 */
public final class Parser {

    public final Properties metaTags = new Properties();
    public final String title, body;

    public Parser(String source) throws IOException, SAXException {
        final SAXParser parser = new SAXParser();
        parser.setFeature("http://xml.org/sax/features/namespaces", true);
        parser.setFeature("http://cyberneko.org/html/features/balance-tags", true);
        parser.setFeature("http://cyberneko.org/html/features/report-errors", false);
        parser.setProperty("http://cyberneko.org/html/properties/names/elems", "lower");
        parser.setProperty("http://cyberneko.org/html/properties/names/attrs", "lower");

        final StringBuilder title = new StringBuilder(), body = new StringBuilder();
        final DefaultHandler handler = new DefaultHandler() {
            private int inBODY = 0, inHEAD = 0, inTITLE = 0, suppressed = 0;

            @Override
            public void startElement(String namespaceURI, String localName, String qName, Attributes atts) throws SAXException {
                if (inHEAD > 0) {
                    if ("title".equals(localName)) {
                        inTITLE++;
                    } else {
                        if ("meta".equals(localName)) {
                            String name = atts.getValue("name");
                            if (name == null) {
                                name = atts.getValue("http-equiv");
                            }
                            final String val = atts.getValue("content");
                            if (name != null && val != null) {
                                metaTags.setProperty(name.toLowerCase(Locale.ROOT), val);
                            }
                        }
                    }
                } else if (inBODY > 0) {
                    if (SUPPRESS_ELEMENTS.contains(localName)) {
                        suppressed++;
                    } else if ("img".equals(localName)) {
                        // the original javacc-based parser preserved <IMG alt="..."/>
                        // attribute as body text in [] parenthesis:
                        final String alt = atts.getValue("alt");
                        if (alt != null) {
                            body.append('[').append(alt).append(']');
                        }
                    }
                } else if ("body".equals(localName)) {
                    inBODY++;
                } else if ("head".equals(localName)) {
                    inHEAD++;
                } else if ("frameset".equals(localName)) {
                    throw new SAXException("This parser does not support HTML framesets.");
                }
            }

            @Override
            public void endElement(String namespaceURI, String localName, String qName) throws SAXException {
                if (inBODY > 0) {
                    if ("body".equals(localName)) {
                        inBODY--;
                    } else if (ENDLINE_ELEMENTS.contains(localName)) {
                        body.append('\n');
                    } else if (SUPPRESS_ELEMENTS.contains(localName)) {
                        suppressed--;
                    }
                } else if (inHEAD > 0) {
                    if ("head".equals(localName)) {
                        inHEAD--;
                    } else if (inTITLE > 0 && "title".equals(localName)) {
                        inTITLE--;
                    }
                }
            }

            @Override
            public void characters(char[] ch, int start, int length) throws SAXException {
                if (inBODY > 0 && suppressed == 0) {
                    body.append(ch, start, length);
                } else if (inTITLE > 0) {
                    title.append(ch, start, length);
                }
            }

            @Override
            public InputSource resolveEntity(String publicId, String systemId) {
                // disable network access caused by DTDs
                return new InputSource(new StringReader(""));
            }
        };

        parser.setContentHandler(handler);
        parser.setErrorHandler(handler);
        InputSource inputSource = new InputSource(new StringReader(source));
        parser.parse(inputSource);

        // the javacc-based parser trimmed title (which should be done for HTML in all cases):
        this.title = title.toString().trim().replaceAll("(\\r|\\n|\\t)", "");

        // assign body text
        this.body = body.toString().trim().replaceAll("(\\r|\\n|\\t)", "");
    }

    private static final Set<String> createElementNameSet(String... names) {
        return Collections.unmodifiableSet(new HashSet<>(Arrays.asList(names)));
    }

    /**
     * HTML elements that cause a line break (they are block-elements)
     */
    static final Set<String> ENDLINE_ELEMENTS = createElementNameSet(
            "p", "h1", "h2", "h3", "h4", "h5", "h6", "div", "ul", "ol", "dl",
            "pre", "hr", "blockquote", "address", "fieldset", "table", "form",
            "noscript", "li", "dt", "dd", "noframes", "br", "tr", "select", "option"
    );

    /**
     * HTML elements with contents that are ignored
     */
    static final Set<String> SUPPRESS_ELEMENTS = createElementNameSet(
            "style", "script"
    );

}
