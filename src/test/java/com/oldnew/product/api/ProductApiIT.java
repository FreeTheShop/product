package com.oldnew.product.api;

import com.oldnew.product.support.IntegrationTest;
import com.oldnew.product.support.MockMvcFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.context.WebApplicationContext;

import static com.oldnew.product.support.Tokens.catalogManager;
import static com.oldnew.product.support.Tokens.customerWithWriteScope;
import static com.oldnew.product.support.Tokens.priceService;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@IntegrationTest
class ProductApiIT {

    private static final String NEW_PRODUCT = """
            {"sku":"RUN-2026","name":"Trail Runner","description":"Light","categoryId":"running-shoes",
             "variants":[{"sku":"RUN-2026-42","attributes":{"size":"42"}}]}""";

    @Autowired
    WebApplicationContext context;
    @Autowired
    MongoTemplate mongo;

    MockMvc mvc;

    @BeforeEach
    void setUp() {
        mongo.remove(new Query(), "products");
        mvc = MockMvcFactory.withSecurity(context);
    }

    private MvcResult create(String body) throws Exception {
        return mvc.perform(post("/api/v1/admin/products").with(catalogManager())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(header().string(HttpHeaders.ETAG, "\"0\""))
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andReturn();
    }

    private static String id(MvcResult result) throws Exception {
        String location = result.getResponse().getHeader(HttpHeaders.LOCATION);
        return location.substring(location.lastIndexOf('/') + 1);
    }

    @Test
    void catalogManagerCreatesPublishesAndShoppersSeeIt() throws Exception {
        String id = id(create(NEW_PRODUCT));

        mvc.perform(get("/api/v1/products/{id}", id)).andExpect(status().isNotFound());

        mvc.perform(post("/api/v1/admin/products/{id}/publish", id).with(catalogManager()).header(HttpHeaders.IF_MATCH, "\"0\""))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ETAG, "\"1\""))
                .andExpect(jsonPath("$.status").value("PUBLISHED"));

        mvc.perform(get("/api/v1/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].id").value(id))
                .andExpect(jsonPath("$.items[0].status").doesNotExist())
                .andExpect(jsonPath("$.nextCursor").doesNotExist());
    }

    @Test
    void writeScopeAloneIsNotEnoughWithoutTheRole() throws Exception {
        mvc.perform(post("/api/v1/admin/products").with(customerWithWriteScope())
                        .contentType(MediaType.APPLICATION_JSON).content(NEW_PRODUCT))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
    }

    @Test
    void noTokenMeansUnauthorizedProblem() throws Exception {
        mvc.perform(post("/api/v1/admin/products").contentType(MediaType.APPLICATION_JSON).content(NEW_PRODUCT))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }

    @Test
    void duplicateSkuIsAConflict() throws Exception {
        create(NEW_PRODUCT);
        mvc.perform(post("/api/v1/admin/products").with(catalogManager())
                        .contentType(MediaType.APPLICATION_JSON).content(NEW_PRODUCT))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("SKU_ALREADY_EXISTS"));
    }

    @Test
    void publishWithoutVariantsBreaksARule() throws Exception {
        String id = id(create("""
                {"sku":"BARE-1","name":"Bare","categoryId":"misc"}"""));

        mvc.perform(post("/api/v1/admin/products/{id}/publish", id).with(catalogManager()).header(HttpHeaders.IF_MATCH, "\"0\""))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("PRODUCT_HAS_NO_VARIANTS"));
    }

    @Test
    void staleOrMissingIfMatchIsRefused() throws Exception {
        String id = id(create(NEW_PRODUCT));

        mvc.perform(post("/api/v1/admin/products/{id}/publish", id).with(catalogManager()))
                .andExpect(status().isPreconditionRequired())
                .andExpect(jsonPath("$.code").value("VERSION_REQUIRED"));
        mvc.perform(post("/api/v1/admin/products/{id}/publish", id).with(catalogManager()).header(HttpHeaders.IF_MATCH, "\"7\""))
                .andExpect(status().isPreconditionFailed())
                .andExpect(jsonPath("$.code").value("VERSION_MISMATCH"));
    }

    @Test
    void unknownFieldsCannotSetStatus() throws Exception {
        MockHttpServletResponse response = mvc.perform(post("/api/v1/admin/products").with(catalogManager())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sku\":\"SNEAKY-1\",\"name\":\"Sneaky\",\"categoryId\":\"misc\",\"status\":\"PUBLISHED\",\"version\":99}"))
                .andReturn().getResponse();

        // Either the unknown fields are ignored (product stays DRAFT) or rejected outright; never applied.
        if (response.getStatus() == 201) {
            assertThat(response.getContentAsString()).contains("\"status\":\"DRAFT\"").contains("\"version\":0");
        } else {
            assertThat(response.getStatus()).isEqualTo(400);
        }
    }

    @Test
    void pageSizeAboveOneHundredIsRejected() throws Exception {
        mvc.perform(get("/api/v1/products").param("size", "101"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    void internalLookupNeedsTheInternalScope() throws Exception {
        String id = id(create(NEW_PRODUCT));

        mvc.perform(get("/api/v1/internal/products/{id}", id).with(catalogManager()))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/internal/products/{id}", id).with(priceService()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sellable").value(false))
                .andExpect(jsonPath("$.variantSkus[0]").value("RUN-2026-42"));
    }

    @Test
    void malformedIdIsABadRequestNotAServerError() throws Exception {
        mvc.perform(get("/api/v1/products/{id}", "not-an-id"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_PRODUCT_ID"));
    }

    @Test
    void unknownPathsAreDenied() throws Exception {
        mvc.perform(post("/api/v1/products").with(catalogManager()))
                .andExpect(status().isForbidden());
    }
}
