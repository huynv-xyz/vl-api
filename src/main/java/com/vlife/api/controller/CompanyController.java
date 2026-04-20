package com.vlife.api.controller;

import com.vlife.api.controller.base.BaseCrudController;
import com.vlife.api.util.ApiUtil;
import com.vlife.shared.api.builder.ItemBuilder;
import com.vlife.shared.api.dto.ApiResponse;
import com.vlife.shared.jdbc.dao.CompanyDao;
import com.vlife.shared.jdbc.entity.Company;
import com.vlife.shared.util.CommonUtil;
import io.micronaut.data.model.Page;
import io.micronaut.data.model.Pageable;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.*;
import io.micronaut.serde.annotation.Serdeable;
import jakarta.inject.Inject;

import java.time.LocalDateTime;
import java.util.Map;

@Controller("/companies")
public class CompanyController extends BaseCrudController<Company, Integer, CompanyDao> {

    @Inject
    public CompanyController(CompanyDao dao, ItemBuilder<Company> builder) {
        super(dao, builder);
    }

    @Override
    protected Page<Company> doSearch(Map<String, String> filters, Pageable pageable) {
        String keyword = ApiUtil.trim(filters.get("keyword"));
        return dao.search(keyword, pageable);
    }

    @Post
    public HttpResponse<?> create(@Body CompanyRequest req) {
        return handleCreate(req, r -> {
            Company c = new Company();
            c.setName(ApiUtil.trim(r.getName()));
            c.setAddress(ApiUtil.trim(r.getAddress()));

            LocalDateTime now = LocalDateTime.now();
            c.setCreatedAt(now);
            c.setUpdatedAt(now);
            return c;
        });
    }

    @Put("/{id}")
    public HttpResponse<?> update(@PathVariable Integer id, @Body CompanyRequest req) {
        return handleUpdate(id, req, r -> {
            Company c = new Company();
            c.setName(ApiUtil.trim(r.getName()));
            c.setAddress(ApiUtil.trim(r.getAddress()));
            c.setUpdatedAt(LocalDateTime.now());

            mergeNullFromDb(id, c, "id", "createdAt");
            return c;
        });
    }

    @Override
    protected <REQ> ApiResponse<?> validateBeforeCreate(Company entity, REQ req) {
        if (CommonUtil.isNullOrEmpty(entity.getName())) {
            return ApiResponse.error(-400, "name is required");
        }
        return null;
    }

    @Override
    protected <REQ> ApiResponse<?> validateBeforeUpdate(Integer id, Company entity, REQ req) {
        if (CommonUtil.isNullOrEmpty(entity.getName())) {
            return ApiResponse.error(-400, "name is required");
        }
        return null;
    }

    // DTO
    @Serdeable
    public static class CompanyRequest {
        private String name;
        private String address;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getAddress() { return address; }
        public void setAddress(String address) { this.address = address; }
    }
}