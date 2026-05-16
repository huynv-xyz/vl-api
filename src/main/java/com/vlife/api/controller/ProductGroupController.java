package com.vlife.api.controller;

import com.vlife.api.builder.ProductGroupBuilder;
import com.vlife.api.controller.base.BaseCrudController;
import com.vlife.shared.api.dto.ApiResponse;
import com.vlife.shared.jdbc.dao.ProductGroupDao;
import com.vlife.shared.jdbc.entity.ProductGroup;
import io.micronaut.data.model.Page;
import io.micronaut.data.model.Pageable;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

import static com.vlife.shared.util.StringUtil.isBlank;
import static com.vlife.shared.util.StringUtil.trim;

@Controller("/product-groups")
public class ProductGroupController extends BaseCrudController<ProductGroup, Integer, ProductGroupDao> {
    public ProductGroupController(ProductGroupDao dao, ProductGroupBuilder builder) {
        super(dao, builder);
    }

    @Override
    protected Page<ProductGroup> doSearch(Map<String, String> filters, Pageable pageable) {
        return dao.search(trim(filters.get("keyword")), parseBoolean(filters.get("active")), pageable);
    }

    @Post
    public HttpResponse<?> create(@Body ProductGroup req) {
        normalize(req);
        return handleCreate(req, x -> x);
    }

    @Put("/{id}")
    public HttpResponse<?> update(@PathVariable Integer id, @Body ProductGroup req) {
        normalize(req);
        return handleUpdate(id, req, x -> x);
    }

    @Override
    protected <REQ> ApiResponse<?> validateBeforeCreate(ProductGroup entity, REQ req) {
        if (isBlank(entity.getCode())) return ApiResponse.error(-400, "code is required");
        if (isBlank(entity.getName())) return ApiResponse.error(-400, "name is required");
        if (dao.findByCode(entity.getCode()).isPresent()) return ApiResponse.error(-400, "code already exists");
        return null;
    }

    @Override
    protected <REQ> ApiResponse<?> validateBeforeUpdate(Integer id, ProductGroup entity, REQ req) {
        if (isBlank(entity.getCode())) return ApiResponse.error(-400, "code is required");
        if (isBlank(entity.getName())) return ApiResponse.error(-400, "name is required");
        var old = dao.findById(id);
        if (old.isEmpty()) return ApiResponse.error(-404, "not found");
        if (!entity.getCode().equals(old.get().getCode()) && dao.findByCode(entity.getCode()).isPresent()) {
            return ApiResponse.error(-400, "code already exists");
        }
        return null;
    }

    private void normalize(ProductGroup x) {
        LocalDateTime now = LocalDateTime.now();
        x.setCode(normalizeCode(x.getCode()));
        x.setName(trim(x.getName()));
        if (x.getVatRate() == null) x.setVatRate(BigDecimal.valueOf(5));
        if (x.getActive() == null) x.setActive(true);
        if (x.getCreatedAt() == null) x.setCreatedAt(now);
        x.setUpdatedAt(now);
    }

    private String normalizeCode(String value) {
        String x = trim(value);
        return x == null ? null : x.toUpperCase().replace(' ', '_');
    }

    private Boolean parseBoolean(String value) {
        return value == null || value.isBlank() ? null : Boolean.valueOf(value);
    }
}
