package com.vlife.api.controller;

import com.vlife.api.builder.ProductBuilder;
import com.vlife.api.controller.base.BaseCrudController;
import com.vlife.shared.api.dto.ApiResponse;
import com.vlife.shared.jdbc.dao.ProductDao;
import com.vlife.shared.jdbc.entity.Product;
import io.micronaut.data.model.Page;
import io.micronaut.data.model.Pageable;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.*;
import io.micronaut.serde.annotation.Serdeable;
import jakarta.inject.Inject;

import java.time.LocalDateTime;
import java.util.Map;

import static com.vlife.shared.util.NumberUtil.parseInt;
import static com.vlife.shared.util.StringUtil.isBlank;
import static com.vlife.shared.util.StringUtil.trim;

@Controller("/products")
public class ProductController extends BaseCrudController<Product, Integer, ProductDao> {

    @Inject
    public ProductController(ProductDao dao, ProductBuilder builder) {
        super(dao, builder);
    }

    @Override
    protected Page<Product> doSearch(Map<String, String> filters, Pageable pageable) {
        return dao.search(
                trim(filters.get("keyword")),
                parseInt(filters.get("status")),
                pageable
        );
    }

    @Post
    public HttpResponse<?> create(@Body ProductCreateRequest req) {
        return handleCreate(req, r -> {
            Product x = new Product();
            x.setCode(trim(r.getCode()));
            x.setName(trim(r.getName()));
            x.setUnit(trim(r.getUnit()));
            x.setStatus(r.getStatus() != null ? r.getStatus() : 1);
            x.setCreatedAt(LocalDateTime.now());
            x.setUpdatedAt(LocalDateTime.now());
            return x;
        });
    }

    @Put("/{id}")
    public HttpResponse<?> update(@PathVariable Integer id, @Body ProductUpdateRequest req) {
        return handleUpdate(id, req, r -> {
            Product x = new Product();
            x.setCode(trim(r.getCode()));
            x.setName(trim(r.getName()));
            x.setUnit(trim(r.getUnit()));
            x.setStatus(r.getStatus());
            x.setUpdatedAt(LocalDateTime.now());
            mergeNullFromDb(id, x, "id", "createdAt");
            return x;
        });
    }

    @Override
    protected <REQ> ApiResponse<?> validateBeforeCreate(Product entity, REQ req) {
        if (isBlank(entity.getCode())) return ApiResponse.error(-400, "code is required");
        if (isBlank(entity.getName())) return ApiResponse.error(-400, "name is required");
        if (dao.findByCode(entity.getCode()).isPresent()) return ApiResponse.error(-400, "code already exists");
        return null;
    }

    @Override
    protected <REQ> ApiResponse<?> validateBeforeUpdate(Integer id, Product entity, REQ req) {
        if (isBlank(entity.getCode())) return ApiResponse.error(-400, "code is required");
        if (isBlank(entity.getName())) return ApiResponse.error(-400, "name is required");

        var oldOpt = dao.findById(id);
        if (oldOpt.isEmpty()) return ApiResponse.error(-404, "not found");

        var old = oldOpt.get();
        if (!entity.getCode().equals(old.getCode()) && dao.findByCode(entity.getCode()).isPresent()) {
            return ApiResponse.error(-400, "code already exists");
        }
        return null;
    }

    @Serdeable
    public static class ProductCreateRequest {
        private String code;
        private String name;
        private String unit;
        private Integer status;

        public String getCode() {
            return code;
        }

        public void setCode(String code) {
            this.code = code;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getUnit() {
            return unit;
        }

        public void setUnit(String unit) {
            this.unit = unit;
        }

        public Integer getStatus() {
            return status;
        }

        public void setStatus(Integer status) {
            this.status = status;
        }
    }

    @Serdeable
    public static class ProductUpdateRequest extends ProductCreateRequest {
    }
}