package com.vlife.api.controller.production;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.vlife.api.builder.production.ProductionSubstitutionBuilder;
import com.vlife.api.controller.base.BaseCrudController;
import com.vlife.shared.jdbc.dao.production.ProductionSubstitutionDao;
import com.vlife.shared.jdbc.entity.production.ProductionSubstitution;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Post;
import io.micronaut.serde.annotation.Serdeable;
import jakarta.inject.Inject;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Controller("/productions/substitutions")
public class ProductionSubstitutionController
        extends BaseCrudController<ProductionSubstitution, Integer, ProductionSubstitutionDao> {

    @Inject
    public ProductionSubstitutionController(ProductionSubstitutionDao dao,
                                            ProductionSubstitutionBuilder builder) {
        super(dao, builder);
    }

    @Post
    public HttpResponse<?> create(@Body Req req) {
        return handleCreate(req, r -> {
            ProductionSubstitution x = new ProductionSubstitution();

            x.setOriginalProductId(r.getProductionId());
            x.setOriginalProductId(r.getOriginalProductId());
            x.setSubstituteProductId(r.getSubstituteProductId());
            x.setQuantity(r.getQuantity());

            x.setCreatedAt(LocalDateTime.now());
            x.setUpdatedAt(LocalDateTime.now());

            return x;
        });
    }

    @Getter
    @Setter
    @Serdeable
    public static class Req {

        @JsonProperty("production_id")
        private Integer productionId;

        @JsonProperty("original_product_id")
        private Integer originalProductId;

        @JsonProperty("substitute_product_id")
        private Integer substituteProductId;

        private BigDecimal quantity;
    }
}