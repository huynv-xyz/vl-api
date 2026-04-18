package com.vlife.api.controller.vip;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.vlife.api.builder.vip.VipTierBuilder;
import com.vlife.api.controller.base.BaseVipRecalcCrudController;
import com.vlife.api.util.ApiUtil;
import com.vlife.shared.api.dto.ApiResponse;
import com.vlife.shared.jdbc.dao.vip.VipTierDao;
import com.vlife.shared.jdbc.entity.vip.VipTier;
import com.vlife.shared.service.VipRecalcJobService;
import com.vlife.shared.util.CommonUtil;
import io.micronaut.data.model.Page;
import io.micronaut.data.model.Pageable;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.*;
import io.micronaut.serde.annotation.Serdeable;
import jakarta.inject.Inject;

import java.time.LocalDateTime;
import java.util.Map;

@Controller("/vip/tiers")
public class VipTierController extends BaseVipRecalcCrudController<VipTier, Integer, VipTierDao> {

    @Inject
    public VipTierController(
            VipTierDao dao,
            VipTierBuilder builder,
            VipRecalcJobService vipRecalcJobService
    ) {
        super(dao, builder, vipRecalcJobService);
    }

    @Override
    protected Page<VipTier> doSearch(Map<String, String> filters, Pageable pageable) {
        String keyword = ApiUtil.trim(filters.get("keyword"));
        Integer status = ApiUtil.parseInteger(filters.get("status"));
        return dao.search(keyword, status, pageable);
    }

    @Post
    public HttpResponse<?> create(@Body VipTierCreateRequest req) {
        HttpResponse<?> response = handleCreate(req, r -> {
            VipTier e = new VipTier();
            e.setName(ApiUtil.trim(r.getName()));
            e.setMbB2bPoint(r.getMbB2bPoint());
            e.setMbB2bReward(r.getMbB2bReward());
            e.setB2cPoint(r.getB2cPoint());
            e.setB2cReward(r.getB2cReward());
            e.setB2bPoint(r.getB2bPoint());
            e.setB2bReward(r.getB2bReward());
            e.setSortOrder(r.getSortOrder());
            e.setStatus(r.getStatus() != null ? r.getStatus() : 1);
            e.setNote(ApiUtil.trim(r.getNote()));

            LocalDateTime now = LocalDateTime.now();
            e.setCreatedAt(now);
            e.setUpdatedAt(now);
            return e;
        });

        enqueueRecalcJobIfSuccess(response, "VIP_TIER_CREATED", null);
        return response;
    }

    @Put("/{id}")
    public HttpResponse<?> update(@PathVariable Integer id, @Body VipTierUpdateRequest req) {
        HttpResponse<?> response = handleUpdate(id, req, r -> {
            VipTier e = new VipTier();
            e.setName(ApiUtil.trim(r.getName()));
            e.setMbB2bPoint(r.getMbB2bPoint());
            e.setMbB2bReward(r.getMbB2bReward());
            e.setB2cPoint(r.getB2cPoint());
            e.setB2cReward(r.getB2cReward());
            e.setB2bPoint(r.getB2bPoint());
            e.setB2bReward(r.getB2bReward());
            e.setSortOrder(r.getSortOrder());
            e.setStatus(r.getStatus());
            e.setNote(ApiUtil.trim(r.getNote()));
            e.setUpdatedAt(LocalDateTime.now());

            mergeNullFromDb(id, e, "id", "createdAt");
            return e;
        });

        enqueueRecalcJobIfSuccess(response, "VIP_TIER_UPDATED", id);
        return response;
    }

    @Delete("/{id}")
    public HttpResponse<?> delete(@PathVariable Integer id) {
        HttpResponse<?> response = super.delete(id);
        enqueueRecalcJobIfSuccess(response, "VIP_TIER_DELETED", id);
        return response;
    }

    @Override
    protected <REQ> ApiResponse<?> validateBeforeCreate(VipTier entity, REQ req) {
        if (CommonUtil.isNullOrEmpty(entity.getName())) {
            return ApiResponse.error(-400, "name is required");
        }
        if (dao.existsByName(entity.getName())) {
            return ApiResponse.error(-400, "name already exists");
        }
        return null;
    }

    @Override
    protected <REQ> ApiResponse<?> validateBeforeUpdate(Integer id, VipTier entity, REQ req) {
        if (CommonUtil.isNullOrEmpty(entity.getName())) {
            return ApiResponse.error(-400, "name is required");
        }

        var oldOpt = dao.findById(id);
        if (oldOpt.isEmpty()) {
            return ApiResponse.error(-404, "not found");
        }

        var old = oldOpt.get();
        if (!entity.getName().equals(old.getName()) && dao.existsByName(entity.getName())) {
            return ApiResponse.error(-400, "name already exists");
        }

        return null;
    }

    @Serdeable
    public static class VipTierCreateRequest {
        private String name;

        @JsonProperty("mb_b2b_point")
        private Double mbB2bPoint;

        @JsonProperty("mb_b2b_reward")
        private Double mbB2bReward;

        @JsonProperty("b2c_point")
        private Double b2cPoint;

        @JsonProperty("b2c_reward")
        private Double b2cReward;

        @JsonProperty("b2b_point")
        private Double b2bPoint;

        @JsonProperty("b2b_reward")
        private Double b2bReward;

        @JsonProperty("sort_order")
        private Integer sortOrder;

        private Integer status;
        private String note;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public Double getMbB2bPoint() { return mbB2bPoint; }
        public void setMbB2bPoint(Double mbB2bPoint) { this.mbB2bPoint = mbB2bPoint; }

        public Double getMbB2bReward() { return mbB2bReward; }
        public void setMbB2bReward(Double mbB2bReward) { this.mbB2bReward = mbB2bReward; }

        public Double getB2cPoint() { return b2cPoint; }
        public void setB2cPoint(Double b2cPoint) { this.b2cPoint = b2cPoint; }

        public Double getB2cReward() { return b2cReward; }
        public void setB2cReward(Double b2cReward) { this.b2cReward = b2cReward; }

        public Double getB2bPoint() { return b2bPoint; }
        public void setB2bPoint(Double b2bPoint) { this.b2bPoint = b2bPoint; }

        public Double getB2bReward() { return b2bReward; }
        public void setB2bReward(Double b2bReward) { this.b2bReward = b2bReward; }

        public Integer getSortOrder() { return sortOrder; }
        public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }

        public Integer getStatus() { return status; }
        public void setStatus(Integer status) { this.status = status; }

        public String getNote() { return note; }
        public void setNote(String note) { this.note = note; }
    }

    @Serdeable
    public static class VipTierUpdateRequest {
        private String name;

        @JsonProperty("mb_b2b_point")
        private Double mbB2bPoint;

        @JsonProperty("mb_b2b_reward")
        private Double mbB2bReward;

        @JsonProperty("b2c_point")
        private Double b2cPoint;

        @JsonProperty("b2c_reward")
        private Double b2cReward;

        @JsonProperty("b2b_point")
        private Double b2bPoint;

        @JsonProperty("b2b_reward")
        private Double b2bReward;

        @JsonProperty("sort_order")
        private Integer sortOrder;

        private Integer status;
        private String note;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public Double getMbB2bPoint() { return mbB2bPoint; }
        public void setMbB2bPoint(Double mbB2bPoint) { this.mbB2bPoint = mbB2bPoint; }

        public Double getMbB2bReward() { return mbB2bReward; }
        public void setMbB2bReward(Double mbB2bReward) { this.mbB2bReward = mbB2bReward; }

        public Double getB2cPoint() { return b2cPoint; }
        public void setB2cPoint(Double b2cPoint) { this.b2cPoint = b2cPoint; }

        public Double getB2cReward() { return b2cReward; }
        public void setB2cReward(Double b2cReward) { this.b2cReward = b2cReward; }

        public Double getB2bPoint() { return b2bPoint; }
        public void setB2bPoint(Double b2bPoint) { this.b2bPoint = b2bPoint; }

        public Double getB2bReward() { return b2bReward; }
        public void setB2bReward(Double b2bReward) { this.b2bReward = mbB2bReward; }

        public Integer getSortOrder() { return sortOrder; }
        public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }

        public Integer getStatus() { return status; }
        public void setStatus(Integer status) { this.status = status; }

        public String getNote() { return note; }
        public void setNote(String note) { this.note = note; }
    }
}