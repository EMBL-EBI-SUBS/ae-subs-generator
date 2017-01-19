package uk.ac.ebi.subs.submissiongeneration.olsSearch;


import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.request.HttpRequest;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.util.List;

import static org.junit.Assert.assertThat;
import static org.hamcrest.Matchers.*;


public class OlsSearchServiceTest {

    private OlsSearchServiceImpl olsSearchService;

    @Before
    public void setUp() {
        olsSearchService = new OlsSearchServiceImpl();
    }

    @Test
    public void testQueryUrl() throws MalformedURLException, UnsupportedEncodingException {
        String query = "bob";

        String expectedQueryUrl = "http://www.ebi.ac.uk/ols/api/search?q=bob";


        HttpRequest request = olsSearchService.formQueryUrl(query);
        String queryUrl = request.getUrl();

        assertThat(queryUrl, notNullValue());
        assertThat(queryUrl,equalTo(expectedQueryUrl));
    }

    @Test
    public void testEscapedQueryUrl() throws MalformedURLException, UnsupportedEncodingException {
        String query = "bob bob";

        String expectedQueryUrl = "http://www.ebi.ac.uk/ols/api/search?q=bob+bob";

        HttpRequest request = olsSearchService.formQueryUrl(query);
        String queryUrl = request.getUrl();

        assertThat(queryUrl, notNullValue());
        assertThat(queryUrl,equalTo(expectedQueryUrl));
    }

    @Test
    public void testJsonHandling() throws IOException {
        JsonNode jsonNode = loadResourceFileToJsonNode("example_ols_search_output.json");

        String uri = olsSearchService.findTermUri(jsonNode);
        String expectedUri = "http://purl.obolibrary.org/obo/GAZ_00187392";


        assertThat(uri, notNullValue());
        assertThat(uri,equalTo(expectedUri));
    }

    @Test
    public void testJsonHandlingNoResults() throws IOException {
        JsonNode jsonNode = loadResourceFileToJsonNode("example_empty_ols_search_output.json");

        String uri = olsSearchService.findTermUri(jsonNode);

        assertThat(uri, nullValue());
    }

    private JsonNode loadResourceFileToJsonNode(String fileName) throws IOException {
        ClassLoader classLoader = getClass().getClassLoader();
        File file = new File(classLoader.getResource(fileName).getFile());


        List<String> lines = Files.readAllLines(file.toPath());
        StringBuffer jsonBuffer = new StringBuffer();

        lines.forEach(l -> jsonBuffer.append(l));

        JsonNode jsonNode = new JsonNode(jsonBuffer.toString());
        return jsonNode;
    }

}
