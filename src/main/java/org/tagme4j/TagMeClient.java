/**
 * Copyright (c) 2016 Enrico Candino
 * <p>
 * Distributed under the MIT License.
 */
package org.tagme4j;

import com.google.gson.Gson;
import org.tagme4j.request.RelRequest;
import org.tagme4j.request.SpotRequest;
import org.tagme4j.request.TagRequest;
import okhttp3.*;

import java.util.concurrent.TimeUnit;

public class TagMeClient {

    private final static String scheme = "https";
    private final static String host   = "tagme.d4science.org";

    private String apikey;
    private OkHttpClient client;
    private Gson gson;

    /**
     *
     * @param apikey the D4Science Service Authorization Token
     */
    public TagMeClient(String apikey) {
        System.setProperty("javax.net.ssl.trustStore", "jssecacerts");
        this.apikey = apikey;
        this.client = new OkHttpClient.Builder()
                .connectTimeout(5, TimeUnit.MINUTES)
                .writeTimeout(5, TimeUnit.MINUTES)
                .readTimeout(5, TimeUnit.MINUTES)
                .build();
        this.gson = new Gson();
    }

    public TagRequest tag() {
        return new TagRequest(this);
    }

    public SpotRequest spot() {
        return new SpotRequest(this);
    }

    public RelRequest rel() {
        return new RelRequest(this);
    }

    public static String getScheme() {
        return scheme;
    }

    public static String getHost() {
        return host;
    }

    public String getApikey() {
        return apikey;
    }

    public OkHttpClient getClient() {
        return client;
    }

    public Gson getGson() {
        return gson;
    }
}
