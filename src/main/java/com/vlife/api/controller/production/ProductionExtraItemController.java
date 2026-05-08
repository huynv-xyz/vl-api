package com.vlife.api.controller.production;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.vlife.api.builder.production.ProductionExtraItemBuilder;
import com.vlife.api.controller.base.BaseCrudController;
import com.vlife.shared.jdbc.dao.production.ProductionExtraItemDao;
import com.vlife.shared.jdbc.entity.production.ProductionExtraItem;
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

@Controller("/productions/extras")
public class ProductionExtraItemController
        extends BaseCrudController<ProductionExtraItem, Integer, ProductionExtraItemDao> {

    @Inject
    public ProductionExtraItemController(ProductionExtraItemDao dao,
                                         ProductionExtraItemBuilder builder) {
        super(dao, builder);
    }

    @Post
    public HttpResponse<?> create(@Body Req req) {
        return handleCreate(req, r -> {
            ProductionExtraItem x = new ProductionExtraItem();

            x.setProductionItemId(r.getProductionId());
            x.setProductId(r.getProductId());
            x.setQuantity(r.getQuantity());
            x.setNote(r.getNote());

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

        @JsonProperty("product_id")
        private Integer productId;

        private BigDecimal quantity;
        private String note;
    }
}