package com.vlife.api.controller.production;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.vlife.api.builder.production.ProductionOutputBuilder;
import com.vlife.api.controller.base.BaseCrudController;
import com.vlife.api.util.ApiUtil;
import com.vlife.shared.jdbc.dao.production.ProductionOutputDao;
import com.vlife.shared.jdbc.entity.production.ProductionOutput;
import io.micronaut.data.model.Page;
import io.micronaut.data.model.Pageable;
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
import java.util.Map;

@Controller("/productions/outputs")
public class ProductionOutputController
        extends BaseCrudController<ProductionOutput, Integer, ProductionOutputDao> {

    @Inject
    public ProductionOutputController(ProductionOutputDao dao,
                                      ProductionOutputBuilder builder) {
        super(dao, builder);
    }

    @Override
    protected Page<ProductionOutput> doSearch(Map<String, String> filters, Pageable pageable) {
        return dao.search(
                ApiUtil.parseInteger(filters.get("production_id")),
                ApiUtil.parseInteger(filters.get("product_id")),
                pageable
        );
    }

    @Post
    public HttpResponse<?> create(@Body Req req) {
        return handleCreate(req, r -> {
            ProductionOutput x = new ProductionOutput();

            x.setProductionItemId(r.getProductionId());
            x.setProductId(r.getProductId());
            x.setQuantity(r.getQuantity());
            x.setUnitCost(ApiUtil.nvl(r.getUnitCost()));

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

        @JsonProperty("unit_cost")
        private BigDecimal unitCost;
    }
}