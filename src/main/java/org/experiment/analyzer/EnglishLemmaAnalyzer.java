/*
 * Lemmatizing library for Lucene
 * Copyright (C) 2010 Lars Buitinck
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.experiment.analyzer;

import java.io.*;
import java.util.ArrayList;

import edu.stanford.nlp.tagger.maxent.MaxentTagger;
import edu.stanford.nlp.tagger.maxent.TaggerConfig;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;

/**
 * An analyzer that uses an {@link EnglishLemmaTokenizer}.
 *
 * @author  Lars Buitinck
 * @version 2010.1006
 */
public class EnglishLemmaAnalyzer extends Analyzer {
    private MaxentTagger posTagger;

    public static void main(String[] args) throws IOException {
        try {
            getTermList(new EnglishLemmaAnalyzer("model "), "Thomas Jefferson (April 13 [O.S. April 2] 1743 – July 4, 1826) was an American Founding Father and the principal author of the Declaration of Independence (1776). He was elected the second Vice President of the United States (1797–1801), serving under John Adams and in 1800 was elected the third President (1801–09). Jefferson was a proponent of democracy, republicanism, and individual rights, which motivated American colonists to break from Great Britain and form a new nation. He produced formative documents and decisions at both the state and national level.\n" +
                    "Primarily of English ancestry, Jefferson was born and educated in Virginia. He graduated from the College of William & Mary and briefly practiced law, at times defending slaves seeking their freedom. During the American Revolution, he represented Virginia in the Continental Congress that adopted the Declaration, drafted the law for religious freedom as a Virginia legislator, and served as a wartime governor (1779–1781). He became the United States Minister to France in May 1785, and subsequently the nation's first Secretary of State in 1790–1793 under President George Washington. Jefferson and James Madison organized the Democratic-Republican Party to oppose the Federalist Party during the formation of the First Party System. With Madison, he anonymously wrote the Kentucky and Virginia Resolutions in 1798–1799, which sought to embolden states' rights in opposition to the national government by nullifying the Alien and Sedition Acts.\n" +
                    "As President Jefferson pursued the nation's shipping and trade interests against Barbary pirates and aggressive British trade policies respectively. He also organized the Louisiana Purchase almost doubling the country's territory. As a result of peace negotiations with France, his administration reduced military forces. He was reelected in 1804. Jefferson's second term was beset with difficulties at home, including the trial of former Vice President Aaron Burr. American foreign trade was diminished when Jefferson implemented the Embargo Act of 1807, responding to British threats to U.S. shipping. In 1803, Jefferson began a controversial process of Indian tribe removal to the newly organized Louisiana Territory, and, in 1807, signed the Act Prohibiting Importation of Slaves.\n" +
                    "Jefferson mastered many disciplines which ranged from surveying and mathematics to horticulture and mechanics. He was a proven architect in the classical tradition. Jefferson's keen interest in religion and philosophy earned him the presidency of the American Philosophical Society. He shunned organized religion, but was influenced by both Christianity and deism. He was well versed in linguistics and spoke several languages. He founded the University of Virginia after retiring from public office. He was a prolific letter writer and corresponded with many prominent and important people throughout his adult life. His only full-length book, Notes on the State of Virginia (1785), is considered the most important American book published before 1800.\n" +
                    "Jefferson owned several plantations which were worked by hundreds of slaves. Most historians now believe that after the death of his wife in 1782, he had a relationship with his slave Sally Hemings and fathered at least one of her children. Historians have lauded Jefferson's public life, noting his primary authorship of the Declaration of Independence during the Revolutionary War, his advocacy of religious freedom and tolerance in Virginia, and the Louisiana Purchase while he was president. Various modern scholars are more critical of Jefferson's private life, pointing out for example the discrepancy between his ownership of slaves and his liberal political principles. Presidential scholars consistently rank Jefferson among the greatest presidents.");

            System.out.println();
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println();
    }

    public static ArrayList<String> getTermList(Analyzer analyzer, String text) throws IOException {

        ArrayList<String> result = new ArrayList<String>();

        Reader reader = new StringReader(text);

        try {
            TokenStream ts = analyzer.tokenStream(null, reader);
            ts.reset();

            while(ts.incrementToken()) {
                CharTermAttribute ta = ts.getAttribute(CharTermAttribute.class);
                result.add(ta.toString());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return result;
    }

    /**
     * Construct an analyzer with a tagger using the given model file.
     */
    public EnglishLemmaAnalyzer(String posModelFile) throws Exception {
        this(makeTagger(posModelFile));
    }

    /**
     * Construct an analyzer using the given tagger.
     */
    public EnglishLemmaAnalyzer(MaxentTagger tagger) {
        posTagger = tagger;
    }

    /**
     * Factory method for loading a POS tagger.
     */
    public static MaxentTagger makeTagger(String modelFile) throws Exception {
        TaggerConfig config = new TaggerConfig("-model", modelFile);
        // The final argument suppresses a "loading" message on stderr.
        return new MaxentTagger(modelFile, config, true);
    }

    @Override
    protected TokenStreamComponents createComponents(String fieldName) {
        return null;
    }

//    @Override
//    public TokenStream tokenStream(String fieldName, Reader input) {
//        return new EnglishLemmaTokenizer(input, posTagger);
//    }
}
