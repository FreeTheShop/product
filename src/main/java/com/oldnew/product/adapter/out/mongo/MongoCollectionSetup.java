package com.oldnew.product.adapter.out.mongo;

import org.bson.Document;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.CollectionOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.mongodb.core.index.IndexOperations;
import org.springframework.data.mongodb.core.validation.Validator;
import org.springframework.stereotype.Component;

/**
 * Creates the products collection with a $jsonSchema validator (a second safety net behind the domain rules)
 * and every index the queries need. Runs at startup; repeating it is harmless.
 * auto-index-creation is off so index changes are reviewed here, in code.
 */
@Component
class MongoCollectionSetup implements ApplicationRunner {

    static final String SCHEMA = """
            {
              "$jsonSchema": {
                "bsonType": "object",
                "required": ["_id", "sku", "name", "categoryId", "status", "variants", "createdAt", "updatedAt", "createdBy", "version"],
                "properties": {
                  "_id":        { "bsonType": "string", "pattern": "^[0-9a-f]{8}-[0-9a-f]{4}-7[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$" },
                  "sku":        { "bsonType": "string", "pattern": "^[A-Z0-9][A-Z0-9-]{2,39}$" },
                  "name":       { "bsonType": "string", "minLength": 1, "maxLength": 200 },
                  "description":{ "bsonType": "string", "maxLength": 5000 },
                  "categoryId": { "bsonType": "string", "pattern": "^[a-z0-9][a-z0-9-]{0,63}$" },
                  "status":     { "enum": ["DRAFT", "PUBLISHED", "ARCHIVED"] },
                  "variants":   { "bsonType": "array", "maxItems": 100,
                                  "items": { "bsonType": "object", "required": ["sku"],
                                             "properties": { "sku": { "bsonType": "string" } } } },
                  "createdAt":  { "bsonType": "date" },
                  "updatedAt":  { "bsonType": "date" },
                  "createdBy":  { "bsonType": "string" },
                  "version":    { "bsonType": ["int", "long"] }
                }
              }
            }
            """;

    private final MongoTemplate mongo;

    MongoCollectionSetup(MongoTemplate mongo) {
        this.mongo = mongo;
    }

    @Override
    public void run(ApplicationArguments args) {
        Document schema = Document.parse(SCHEMA);
        if (!mongo.collectionExists(ProductDocument.COLLECTION)) {
            mongo.createCollection(ProductDocument.COLLECTION, CollectionOptions.empty()
                    .validator(Validator.document(schema)).strictValidation().failOnValidationError());
        } else {
            mongo.executeCommand(new Document("collMod", ProductDocument.COLLECTION)
                    .append("validator", schema)
                    .append("validationLevel", "strict")
                    .append("validationAction", "error"));
        }

        IndexOperations indexes = mongo.indexOps(ProductDocument.COLLECTION);
        indexes.createIndex(new Index().on("sku", Sort.Direction.ASC).unique().named("ux_products_sku"));
        // Shop catalog, filtered by category, keyset on _id
        indexes.createIndex(new Index().on("status", Sort.Direction.ASC).on("categoryId", Sort.Direction.ASC)
                .on("_id", Sort.Direction.ASC).named("ix_products_status_category_id"));
        // Shop catalog, all categories, keyset on _id
        indexes.createIndex(new Index().on("status", Sort.Direction.ASC).on("_id", Sort.Direction.ASC)
                .named("ix_products_status_id"));
    }
}
