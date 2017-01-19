package uk.ac.ebi.subs.submissiongeneration.olsSearch;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import com.mashape.unirest.request.HttpRequest;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

@Component
public class OlsSearchServiceImpl implements OlsSearchService {

    private static final Logger logger = LoggerFactory.getLogger(OlsSearchServiceImpl.class);

    private String olsApiSearchUrl = "http://www.ebi.ac.uk/ols/api/search";


    @Override
    @Cacheable("olsUri")
    public String fetchUriForQuery(String query) {
        logger.info("Looking up term URI for {}",query);

        HttpRequest req = formQueryUrl(query);
        HttpResponse<JsonNode> resp = null;

        String uri = null;

        try {
            resp = req.asJson();
        } catch (UnirestException e) {

            throw new RuntimeException(e);
        }

        if (resp.getStatus() == 200) {
            uri = findTermUri(resp.getBody());
        }
        logger.info("Found term URI {} for {}",uri, query);
        return uri;
    }

    protected HttpRequest formQueryUrl(String query) {
        return Unirest.get(olsApiSearchUrl)
                .queryString("q", query);
    }

    protected String findTermUri(JsonNode node) {

        Assert.notNull(node);
        Assert.isTrue(!node.isArray());

        JSONObject jsonObject = node.getObject();

        JSONObject response = jsonObject.getJSONObject("response");

        Assert.notNull(response);

        Assert.isTrue(response.has("numFound"));

        int numFound = response.getInt("numFound");

        if (numFound < 1) {
            return null;
        }

        Assert.isTrue(response.has("docs"));

        JSONArray docs = response.getJSONArray("docs");

        Assert.notNull(docs);
        JSONObject doc = docs.getJSONObject(0);

        return doc.getString("iri");
    }

}
