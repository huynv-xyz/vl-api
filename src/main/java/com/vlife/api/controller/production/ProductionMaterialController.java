package com.vlife.api.controller.production;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.vlife.api.builder.production.ProductionMaterialBuilder;
import com.vlife.api.controller.base.BaseCrudController;
import com.vlife.api.util.ApiUtil;
import com.vlife.shared.jdbc.dao.production.ProductionMaterialDao;
import com.vlife.shared.jdbc.entity.production.ProductionMaterial;
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

@Controller("/productions/materials")
public class ProductionMaterialController
        extends BaseCrudController<ProductionMaterial, Integer, ProductionMaterialDao> {

    @Inject
    public ProductionMaterialController(ProductionMaterialDao dao,
                                        ProductionMaterialBuilder builder) {
        super(dao, builder);
    }

    @Override
    protected Page<ProductionMaterial> doSearch(Map<String, String> filters, Pageable pageable) {
        return dao.search(
                ApiUtil.parseInteger(filters.get("production_id")),
                ApiUtil.parseInteger(filters.get("product_id")),
                pageable
        );
    }

    @Post
    public HttpResponse<?> create(@Body Req req) {
        return handleCreate(req, r -> {
            ProductionMaterial x = new ProductionMaterial();

            x.setOriginalProductId(r.getProductionId());
            x.setProductId(r.getProductId());
            x.setQuantityRequired(r.getQuantityRequired());
            x.setLotId(r.getLotId());
            x.setSourceType(r.getSourceType());
            x.setSourceRefId(r.getRefId());

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

        @JsonProperty("quantity_required")
        private BigDecimal quantityRequired;

        @JsonProperty("lot_id")
        private Integer lotId;

        @JsonProperty("source_type")
        private String sourceType;

        @JsonProperty("ref_id")
        private Integer refId;
    }
}