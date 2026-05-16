package com.vlife.api.controller.pricing;

import com.vlife.api.builder.pricing.ProductPricingConfigBuilder;
import com.vlife.api.controller.base.BaseCrudController;
import com.vlife.shared.api.dto.ApiResponse;
import com.vlife.shared.jdbc.dao.pricing.ProductPricingConfigDao;
import com.vlife.shared.jdbc.entity.pricing.ProductPricingConfig;
import io.micronaut.data.model.Page;
import io.micronaut.data.model.Pageable;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

import static com.vlife.shared.util.StringUtil.isBlank;

@Controller("/pricing/configs")
public class ProductPricingConfigController extends BaseCrudController<ProductPricingConfig, Integer, ProductPricingConfigDao> {
    public ProductPricingConfigController(ProductPricingConfigDao dao, ProductPricingConfigBuilder builder) {
        super(dao, builder);
    }

    @Override
    protected Page<ProductPricingConfig> doSearch(Map<String, String> filters, Pageable pageable) {
        return dao.search(parseInt(filters.get("pricing_group_id")), parseInt(filters.get("product_id")), parseBoolean(filters.get("active")), pageable);
    }

    @Post
    public HttpResponse<?> create(@Body ProductPricingConfig req) {
        normalize(req);
        return handleCreate(req, x -> x);
    }

    @Put("/{id}")
    public HttpResponse<?> update(@PathVariable Integer id, @Body ProductPricingConfig req) {
        normalize(req);
        return handleUpdate(id, req, x -> x);
    }

    @Override
    protected <REQ> ApiResponse<?> validateBeforeCreate(ProductPricingConfig entity, REQ req) {
        ApiResponse<?> err = validate(entity);
        if (err != null) return err;
        if (dao.findByProductId(entity.getProductId()).isPresent()) return ApiResponse.error(-400, "product_id already configured");
        return null;
    }

    @Override
    protected <REQ> ApiResponse<?> validateBeforeUpdate(Integer id, ProductPricingConfig entity, REQ req) {
        ApiResponse<?> err = validate(entity);
        if (err != null) return err;
        var old = dao.findByProductId(entity.getProductId());
        if (old.isPresent() && !old.get().getId().equals(id)) return ApiResponse.error(-400, "product_id already configured");
        return null;
    }

    private ApiResponse<?> validate(ProductPricingConfig x) {
        if (x.getProductId() == null) return ApiResponse.error(-400, "product_id is required");
        if (x.getPricingGroupId() == null) return ApiResponse.error(-400, "pricing_group_id is required");
        return null;
    }

    private void normalize(ProductPricingConfig x) {
        LocalDateTime now = LocalDateTime.now();
        if (isBlank(x.getRegionCode())) x.setRegionCode("DEFAULT");
        if (isBlank(x.getProfitType())) x.setProfitType("PERCENT");
        if (!"AMOUNT".equalsIgnoreCase(x.getProfitType())) x.setProfitType("PERCENT");
        if (x.getProfitValue() == null) x.setProfitValue(BigDecimal.ZERO);
        if (x.getAdjustmentAmountVnd() == null) x.setAdjustmentAmountVnd(BigDecimal.ZERO);
        if (x.getRoundingUnit() == null) x.setRoundingUnit(BigDecimal.valueOf(1000));
        if (x.getVatRate() == null) x.setVatRate(BigDecimal.valueOf(5));
        if (x.getDisplayOrder() == null) x.setDisplayOrder(0);
        if (x.getActive() == null) x.setActive(true);
        if (x.getCreatedAt() == null) x.setCreatedAt(now);
        x.setUpdatedAt(now);
    }

    private Integer parseInt(String value) {
        return value == null || value.isBlank() ? null : Integer.valueOf(value);
    }

    private Boolean parseBoolean(String value) {
        return value == null || value.isBlank() ? null : Boolean.valueOf(value);
    }
}
