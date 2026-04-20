package com.vlife.api.controller;

import com.vlife.api.controller.base.BaseCrudController;
import com.vlife.api.util.ApiUtil;
import com.vlife.shared.api.builder.ItemBuilder;
import com.vlife.shared.api.dto.ApiResponse;
import com.vlife.shared.jdbc.dao.WarehouseDao;
import com.vlife.shared.jdbc.entity.Warehouse;
import com.vlife.shared.util.CommonUtil;
import io.micronaut.data.model.Page;
import io.micronaut.data.model.Pageable;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.*;
import io.micronaut.serde.annotation.Serdeable;
import jakarta.inject.Inject;

import java.time.LocalDateTime;
import java.util.Map;

@Controller("/warehouses")
public class WarehouseController extends BaseCrudController<Warehouse, Integer, WarehouseDao> {

    @Inject
    public WarehouseController(WarehouseDao dao, ItemBuilder<Warehouse> builder) {
        super(dao, builder);
    }

    @Override
    protected Page<Warehouse> doSearch(Map<String, String> filters, Pageable pageable) {
        String keyword = ApiUtil.trim(filters.get("keyword"));
        return dao.search(keyword, pageable);
    }

    @Post
    public HttpResponse<?> create(@Body WarehouseRequest req) {
        return handleCreate(req, r -> {
            Warehouse w = new Warehouse();
            w.setName(ApiUtil.trim(r.getName()));
            w.setAddress(ApiUtil.trim(r.getAddress()));

            LocalDateTime now = LocalDateTime.now();
            w.setCreatedAt(now);
            w.setUpdatedAt(now);
            return w;
        });
    }

    @Put("/{id}")
    public HttpResponse<?> update(@PathVariable Integer id, @Body WarehouseRequest req) {
        return handleUpdate(id, req, r -> {
            Warehouse w = new Warehouse();
            w.setName(ApiUtil.trim(r.getName()));
            w.setAddress(ApiUtil.trim(r.getAddress()));
            w.setUpdatedAt(LocalDateTime.now());

            mergeNullFromDb(id, w, "id", "createdAt");
            return w;
        });
    }

    @Override
    protected <REQ> ApiResponse<?> validateBeforeCreate(Warehouse entity, REQ req) {
        if (CommonUtil.isNullOrEmpty(entity.getName())) {
            return ApiResponse.error(-400, "name is required");
        }
        return null;
    }

    @Override
    protected <REQ> ApiResponse<?> validateBeforeUpdate(Integer id, Warehouse entity, REQ req) {
        if (CommonUtil.isNullOrEmpty(entity.getName())) {
            return ApiResponse.error(-400, "name is required");
        }
        return null;
    }

    @Serdeable
    public static class WarehouseRequest {
        private String name;
        private String address;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getAddress() { return address; }
        public void setAddress(String address) { this.address = address; }
    }
}