package com.vlife.api.controller.purchasing;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.vlife.api.builder.purchasing.SupplierBuilder;
import com.vlife.api.controller.base.BaseCrudController;
import com.vlife.shared.api.dto.ApiResponse;
import com.vlife.shared.jdbc.dao.purchasing.SupplierDao;
import com.vlife.shared.jdbc.entity.purchasing.Supplier;
import io.micronaut.data.model.Page;
import io.micronaut.data.model.Pageable;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.*;
import io.micronaut.serde.annotation.Serdeable;
import jakarta.inject.Inject;

import java.time.LocalDateTime;
import java.util.Map;

import static com.vlife.shared.util.StringUtil.isBlank;
import static com.vlife.shared.util.StringUtil.trim;

@Controller("/purchasing/suppliers")
public class SupplierController extends BaseCrudController<Supplier, Integer, SupplierDao> {

    @Inject
    public SupplierController(SupplierDao dao, SupplierBuilder builder) {
        super(dao, builder);
    }

    @Override
    protected Page<Supplier> doSearch(Map<String, String> filters, Pageable pageable) {
        return dao.search(
                trim(filters.get("keyword")),
                pageable
        );
    }

    @Post
    public HttpResponse<?> create(@Body SupplierCreateRequest req) {
        return handleCreate(req, r -> {
            Supplier x = new Supplier();
            x.setCode(trim(r.getCode()));
            x.setName(trim(r.getName()));
            x.setNationId(r.getNationId());
            x.setCreatedAt(LocalDateTime.now());
            x.setUpdatedAt(LocalDateTime.now());
            return x;
        });
    }

    @Put("/{id}")
    public HttpResponse<?> update(@PathVariable Integer id, @Body SupplierUpdateRequest req) {
        return handleUpdate(id, req, r -> {
            Supplier x = new Supplier();
            x.setCode(trim(r.getCode()));
            x.setName(trim(r.getName()));
            x.setNationId(r.getNationId());
            x.setUpdatedAt(LocalDateTime.now());
            mergeNullFromDb(id, x, "id", "createdAt", "createdBy");
            return x;
        });
    }

    @Override
    protected <REQ> ApiResponse<?> validateBeforeCreate(Supplier entity, REQ req) {
        if (isBlank(entity.getCode())) return ApiResponse.error(-400, "code is required");
        if (isBlank(entity.getName())) return ApiResponse.error(-400, "name is required");
        if (dao.findByCode(entity.getCode()).isPresent()) return ApiResponse.error(-400, "code already exists");
        return null;
    }

    @Override
    protected <REQ> ApiResponse<?> validateBeforeUpdate(Integer id, Supplier entity, REQ req) {
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
    public static class SupplierCreateRequest {
        private String code;
        private String name;

        @JsonProperty("nation_id")
        private Integer nationId;

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

        public Integer getNationId() {
            return nationId;
        }

        public void setNationId(Integer nationId) {
            this.nationId = nationId;
        }
    }

    @Serdeable
    public static class SupplierUpdateRequest extends SupplierCreateRequest {
    }
}