package com.vlife.api.controller;

import com.vlife.api.controller.base.BaseCrudController;
import com.vlife.shared.api.builder.ItemBuilder;
import com.vlife.shared.api.dto.ApiResponse;
import com.vlife.shared.jdbc.dao.CustomerDao;
import com.vlife.shared.jdbc.entity.Customer;
import io.micronaut.data.model.Page;
import io.micronaut.data.model.Pageable;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.*;

import io.micronaut.serde.annotation.Serdeable;
import jakarta.inject.Inject;

import java.time.LocalDateTime;
import java.util.Map;

@Controller("/customers")
public class CustomerController extends BaseCrudController<Customer, Integer, CustomerDao> {

    @Inject
    public CustomerController(CustomerDao dao, ItemBuilder<Customer> builder) {
        super(dao, builder);
    }

    @Override
    protected Page<Customer> doSearch(Map<String, String> filters, Pageable pageable) {
        String keyword = trim(filters.get("keyword"));
        String type = trim(filters.get("type"));
        String region = trim(filters.get("region"));
        Integer status = parseInteger(filters.get("status"));

        return dao.search(keyword, type, region, status, pageable);
    }

    @Post
    public HttpResponse<?> create(@Body CustomerCreateRequest req) {
        return handleCreate(req, r -> {
            Customer c = new Customer();
            c.setCode(trim(r.getCode()));
            c.setName(trim(r.getName()));
            c.setType(trim(r.getType()));
            c.setRegion(trim(r.getRegion()));
            c.setStatus(r.getStatus() != null ? r.getStatus() : 1);
            c.setNote(trim(r.getNote()));

            LocalDateTime now = LocalDateTime.now();
            c.setCreatedAt(now);
            c.setUpdatedAt(now);
            return c;
        });
    }

    @Put("/{id}")
    public HttpResponse<?> update(@PathVariable Integer id, @Body CustomerUpdateRequest req) {
        return handleUpdate(id, req, r -> {
            Customer c = new Customer();
            c.setCode(trim(r.getCode()));
            c.setName(trim(r.getName()));
            c.setType(trim(r.getType()));
            c.setRegion(trim(r.getRegion()));
            c.setStatus(r.getStatus());
            c.setNote(trim(r.getNote()));
            c.setUpdatedAt(LocalDateTime.now());

            mergeNullFromDb(id, c, "id", "createdAt");
            return c;
        });
    }

    @Override
    protected <REQ> ApiResponse<?> validateBeforeCreate(Customer entity, REQ req) {
        if (isBlank(entity.getCode())) {
            return ApiResponse.error(-400, "code is required");
        }
        if (isBlank(entity.getName())) {
            return ApiResponse.error(-400, "name is required");
        }
        if (dao.existsByCode(entity.getCode())) {
            return ApiResponse.error(-400, "code already exists");
        }
        return null;
    }

    @Override
    protected <REQ> ApiResponse<?> validateBeforeUpdate(Integer id, Customer entity, REQ req) {
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
        if (!entity.getCode().equals(old.getCode()) && dao.existsByCode(entity.getCode())) {
            return ApiResponse.error(-400, "code already exists");
        }

        return null;
    }

    private String trim(String s) {
        return s == null ? null : s.trim();
    }

    private boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    private Integer parseInteger(String value) {
        try {
            return value == null || value.isBlank() ? null : Integer.parseInt(value);
        } catch (Exception e) {
            return null;
        }
    }

    // =========================
    // DTO
    // =========================

    @Serdeable
    public static class CustomerCreateRequest {
        private String code;
        private String name;
        private String type;
        private String region;
        private Integer status;
        private String note;

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

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getRegion() {
            return region;
        }

        public void setRegion(String region) {
            this.region = region;
        }

        public Integer getStatus() {
            return status;
        }

        public void setStatus(Integer status) {
            this.status = status;
        }

        public String getNote() {
            return note;
        }

        public void setNote(String note) {
            this.note = note;
        }
    }

    @Serdeable
    public static class CustomerUpdateRequest {
        private String code;
        private String name;
        private String type;
        private String region;
        private Integer status;
        private String note;

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

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getRegion() {
            return region;
        }

        public void setRegion(String region) {
            this.region = region;
        }

        public Integer getStatus() {
            return status;
        }

        public void setStatus(Integer status) {
            this.status = status;
        }

        public String getNote() {
            return note;
        }

        public void setNote(String note) {
            this.note = note;
        }
    }
}