package org.experiment.preprocessing;

import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

import edu.stanford.nlp.ling.CoreAnnotations.LemmaAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.util.CoreMap;

/**
 * Created by sonic on 14/10/16.
 */
public class StanfordLemmatizer {

    protected StanfordCoreNLP pipeline;
    static StanfordLemmatizer stanfordLemmatizer;
    Properties props;


    private StanfordLemmatizer() {
        props = new Properties();
        props.put("annotators", "tokenize, ssplit, pos, lemma");

        this.pipeline = new StanfordCoreNLP(props);
    }

    public static StanfordLemmatizer getInstance() {
        if (stanfordLemmatizer == null) {
            stanfordLemmatizer = new StanfordLemmatizer();
        }
        return stanfordLemmatizer;
    }

    public List<String> lemmatize(String documentText)
    {
        List<String> lemmas = new LinkedList<String>();
        // Create an empty Annotation just with the given text
        Annotation document = new Annotation(documentText);
        // run all Annotators on this text
        this.pipeline.annotate(document);
        // Iterate over all of the sentences found
        List<CoreMap> sentences = document.get(SentencesAnnotation.class);
        for(CoreMap sentence: sentences) {
            // Iterate over all tokens in a sentence
            for (CoreLabel token: sentence.get(TokensAnnotation.class)) {
                // Retrieve and add the lemma for each word into the
                // list of lemmas
                lemmas.add(token.get(LemmaAnnotation.class));
            }
        }
        return lemmas;
    }


    public static void main(String[] args) {
        System.out.println("Starting Stanford Lemmatizer");
        String text = "For grammatical reasons, documents are going to use different forms of a word, such as organize, organizes, and organizing. " +
                "Additionally, there are families of derivationally related words with similar meanings, such as democracy, democratic, and democratization. " +
                "In many situations, it seems as if it would be useful for a search for one of these words to return documents that contain another word in the set.\n";
        System.out.println(StanfordLemmatizer.getInstance().lemmatize(text));
    }
}
