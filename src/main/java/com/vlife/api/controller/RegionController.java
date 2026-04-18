package com.vlife.api.controller;

import com.vlife.api.builder.RegionBuilder;
import com.vlife.api.controller.base.BaseCrudController;
import com.vlife.shared.api.builder.ItemBuilder;
import com.vlife.shared.api.dto.ApiResponse;
import com.vlife.shared.jdbc.dao.RegionDao;
import com.vlife.shared.jdbc.entity.Region;
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

@Controller("/regions")
public class RegionController extends BaseCrudController<Region, Integer, RegionDao> {

    @Inject
    public RegionController(RegionDao dao, RegionBuilder builder) {
        super(dao, builder);
    }

    @Override
    protected Page<Region> doSearch(Map<String, String> filters, Pageable pageable) {
        String keyword = trim(filters.get("keyword"));
        Integer status = parseInt(filters.get("status"));
        return dao.search(keyword, status, pageable);
    }

    @Post
    public HttpResponse<?> create(@Body RegionCreateRequest req) {
        return handleCreate(req, r -> {
            Region x = new Region();
            x.setCode(trim(r.getCode()));
            x.setName(trim(r.getName()));
            x.setStatus(r.getStatus() != null ? r.getStatus() : 1);

            LocalDateTime now = LocalDateTime.now();
            x.setCreatedAt(now);
            x.setUpdatedAt(now);
            return x;
        });
    }

    @Put("/{id}")
    public HttpResponse<?> update(@PathVariable Integer id, @Body RegionUpdateRequest req) {
        return handleUpdate(id, req, r -> {
            Region x = new Region();
            x.setCode(trim(r.getCode()));
            x.setName(trim(r.getName()));
            x.setStatus(r.getStatus());
            x.setUpdatedAt(LocalDateTime.now());

            mergeNullFromDb(id, x, "id", "createdAt");
            return x;
        });
    }

    @Override
    protected <REQ> ApiResponse<?> validateBeforeCreate(Region entity, REQ req) {
        if (isBlank(entity.getCode())) {
            return ApiResponse.error(-400, "code is required");
        }
        if (isBlank(entity.getName())) {
            return ApiResponse.error(-400, "name is required");
        }
        if (dao.findByCode(entity.getCode()).isPresent()) {
            return ApiResponse.error(-400, "code already exists");
        }
        return null;
    }

    @Override
    protected <REQ> ApiResponse<?> validateBeforeUpdate(Integer id, Region entity, REQ req) {
        if (isBlank(entity.getCode())) {
            return ApiResponse.error(-400, "code is required");
        }
        if (isBlank(entity.getName())) {
            return ApiResponse.error(-400, "name is required");
        }

        var oldOpt = dao.findById(id);
        if (oldOpt.isEmpty()) {
            return ApiResponse.error(-404, "not found");
        }

        var old = oldOpt.get();
        if (!entity.getCode().equals(old.getCode()) && dao.findByCode(entity.getCode()).isPresent()) {
            return ApiResponse.error(-400, "code already exists");
        }

        return null;
    }

    @Serdeable
    public static class RegionCreateRequest {
        private String code;
        private String name;
        private Integer status;

        public String getCode() { return code; }
        public void setCode(String code) { this.code = code; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public Integer getStatus() { return status; }
        public void setStatus(Integer status) { this.status = status; }
    }

    @Serdeable
    public static class RegionUpdateRequest {
        private String code;
        private String name;
        private Integer status;

        public String getCode() { return code; }
        public void setCode(String code) { this.code = code; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public Integer getStatus() { return status; }
        public void setStatus(Integer status) { this.status = status; }
    }
}