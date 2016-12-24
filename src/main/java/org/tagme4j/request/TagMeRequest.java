/**
 * Copyright (c) 2016 Enrico Candino
 * <p>
 * Distributed under the MIT License.
 */
package org.tagme4j.request;

import com.google.gson.Gson;
import org.tagme4j.TagMeClient;
import org.tagme4j.TagMeException;
import org.tagme4j.response.TagMeResponse;
import okhttp3.*;

public abstract class TagMeRequest<T extends TagMeResponse> {

    private TagMeClient tagMeClient;
    protected HttpUrl.Builder builder;
    private String path;
    private Class<T> clazz;

    public TagMeRequest(TagMeClient tagMeClient, String path, Class<T> clazz) {
        this.tagMeClient = tagMeClient;
        this.path = path;
        this.clazz = clazz;
        this.builder = new HttpUrl.Builder();
    }

    protected HttpUrl getUrl() {
        return builder.scheme(TagMeClient.getScheme())
                .host(TagMeClient.getHost())
                .addPathSegments(path)
                .setQueryParameter("gcube-token", tagMeClient.getApikey())
                .build();
    }

    protected abstract Request getRequest();

    public T execute() {
        T tagMeResponse = null;

        try {
            OkHttpClient client = tagMeClient.getClient();
            Gson gson = tagMeClient.getGson();

            Response response = client.newCall(getRequest()).execute();
            if (response.code() != 200)
                throw new TagMeException(
                    String.format("Request to TagMeQueryExpander failed with HTTP code %d, message: %s",
                                  response.code(),
                                  response.body().string()));

            String json = response.body().string();
            tagMeResponse = gson.fromJson(json, clazz);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return tagMeResponse;
    }

}
