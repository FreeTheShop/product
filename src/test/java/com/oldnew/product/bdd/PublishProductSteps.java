package com.oldnew.product.bdd;

import com.nimbusds.jose.util.JSONObjectUtils;
import com.oldnew.product.support.MockMvcFactory;
import io.cucumber.java.Before;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.WebApplicationContext;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.oldnew.product.support.Tokens.catalogManager;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/** Drives the real HTTP API, as a catalog manager and as an anonymous shopper. */
public class PublishProductSteps {

    @Autowired
    WebApplicationContext context;
    @Autowired
    MongoTemplate mongo;

    private MockMvc mvc;
    private final Map<String, String> idBySku = new HashMap<>();
    private final Map<String, String> etagBySku = new HashMap<>();
    private MockHttpServletResponse lastResponse;

    @Before
    public void cleanDatabase() {
        mongo.remove(new Query(), "products");
        mvc = MockMvcFactory.withSecurity(context);
    }

    @Given("a catalog manager has created the draft {string} named {string} with variant {string}")
    public void createdWithVariant(String sku, String name, String variantSku) throws Exception {
        create(sku, "{\"sku\":\"" + sku + "\",\"name\":\"" + name + "\",\"categoryId\":\"bdd\","
                + "\"variants\":[{\"sku\":\"" + variantSku + "\"}]}");
    }

    @Given("a catalog manager has created the draft {string} named {string} with no variants")
    public void createdWithoutVariants(String sku, String name) throws Exception {
        create(sku, "{\"sku\":\"" + sku + "\",\"name\":\"" + name + "\",\"categoryId\":\"bdd\"}");
    }

    @When("the catalog manager publishes {string}")
    public void publishes(String sku) throws Exception {
        change(sku, "publish");
    }

    @When("the catalog manager archives {string}")
    public void archives(String sku) throws Exception {
        change(sku, "archive");
    }

    @Then("publishing is refused with {string}")
    public void refusedWith(String code) throws Exception {
        assertThat(lastResponse.getStatus()).isEqualTo(422);
        assertThat(JSONObjectUtils.parse(lastResponse.getContentAsString()).get("code")).isEqualTo(code);
    }

    @Then("shoppers cannot see {string}")
    public void shoppersCannotSee(String sku) throws Exception {
        assertThat(mvc.perform(get("/api/v1/products/{id}", idBySku.get(sku))).andReturn().getResponse().getStatus())
                .isEqualTo(404);
        assertThat(shopSkus()).doesNotContain(sku);
    }

    @Then("shoppers can see {string} with variant {string}")
    public void shoppersCanSee(String sku, String variantSku) throws Exception {
        MockHttpServletResponse response = mvc.perform(get("/api/v1/products/{id}", idBySku.get(sku))).andReturn().getResponse();
        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getContentAsString()).contains(variantSku);
        assertThat(shopSkus()).contains(sku);
    }

    private void create(String sku, String json) throws Exception {
        MockHttpServletResponse response = mvc.perform(post("/api/v1/admin/products").with(catalogManager())
                .contentType(MediaType.APPLICATION_JSON).content(json)).andReturn().getResponse();
        assertThat(response.getStatus()).isEqualTo(201);
        String location = response.getHeader(HttpHeaders.LOCATION);
        idBySku.put(sku, location.substring(location.lastIndexOf('/') + 1));
        etagBySku.put(sku, response.getHeader(HttpHeaders.ETAG));
    }

    private void change(String sku, String action) throws Exception {
        lastResponse = mvc.perform(post("/api/v1/admin/products/{id}/" + action, idBySku.get(sku))
                .with(catalogManager()).header(HttpHeaders.IF_MATCH, etagBySku.get(sku))).andReturn().getResponse();
        if (lastResponse.getStatus() == 200) {
            etagBySku.put(sku, lastResponse.getHeader(HttpHeaders.ETAG));
        }
    }

    @SuppressWarnings("unchecked")
    private List<String> shopSkus() throws Exception {
        String body = mvc.perform(get("/api/v1/products").param("size", "100")).andReturn().getResponse().getContentAsString();
        List<Object> items = (List<Object>) JSONObjectUtils.parse(body).get("items");
        return items.stream().map(i -> (String) ((Map<String, Object>) i).get("sku")).toList();
    }
}
