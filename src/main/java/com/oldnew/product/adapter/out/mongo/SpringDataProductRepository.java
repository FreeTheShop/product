package com.oldnew.product.adapter.out.mongo;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface SpringDataProductRepository extends MongoRepository<ProductDocument, String> {

    boolean existsBySku(String sku);
}
