package com.vlife.api.controller.vip;

import com.vlife.api.builder.vip.VipPrivateBonusRuleBuilder;
import com.vlife.api.controller.base.BaseVipRecalcCrudController;
import com.vlife.api.util.ApiUtil;
import com.vlife.shared.api.dto.ApiResponse;
import com.vlife.shared.jdbc.dao.vip.VipPrivateBonusRuleDao;
import com.vlife.shared.jdbc.entity.vip.VipPrivateBonusRule;
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

@Controller("/vip/private-rules")
public class VipPrivateRuleController
        extends BaseVipRecalcCrudController<VipPrivateBonusRule, Integer, VipPrivateBonusRuleDao> {

    @Inject
    public VipPrivateRuleController(
            VipPrivateBonusRuleDao dao,
            VipPrivateBonusRuleBuilder builder,
            VipRecalcJobService vipRecalcJobService
    ) {
        super(dao, builder, vipRecalcJobService);
    }

    @Override
    protected Page<VipPrivateBonusRule> doSearch(Map<String, String> filters, Pageable pageable) {
        String keyword = ApiUtil.trim(filters.get("keyword"));
        Integer status = ApiUtil.parseInteger(filters.get("status"));
        return dao.search(keyword, status, pageable);
    }

    @Post
    public HttpResponse<?> create(@Body VipPrivateBonusRuleCreateRequest req) {
        HttpResponse<?> response = handleCreate(req, r -> {
            VipPrivateBonusRule e = new VipPrivateBonusRule();
            e.setCode(ApiUtil.trim(r.getCode()));
            e.setName(ApiUtil.trim(r.getName()));
            e.setAmount(r.getAmount());
            e.setStatus(r.getStatus() != null ? r.getStatus() : 1);
            e.setNote(ApiUtil.trim(r.getNote()));

            LocalDateTime now = LocalDateTime.now();
            e.setCreatedAt(now);
            e.setUpdatedAt(now);
            return e;
        });

        enqueueRecalcJobIfSuccess(response, "VIP_PRIVATE_RULE_CREATED", null);
        return response;
    }

    @Put("/{id}")
    public HttpResponse<?> update(@PathVariable Integer id, @Body VipPrivateBonusRuleUpdateRequest req) {
        HttpResponse<?> response = handleUpdate(id, req, r -> {
            VipPrivateBonusRule e = new VipPrivateBonusRule();
            e.setCode(ApiUtil.trim(r.getCode()));
            e.setName(ApiUtil.trim(r.getName()));
            e.setAmount(r.getAmount());
            e.setStatus(r.getStatus());
            e.setNote(ApiUtil.trim(r.getNote()));
            e.setUpdatedAt(LocalDateTime.now());

            mergeNullFromDb(id, e, "id", "createdAt");
            return e;
        });

        enqueueRecalcJobIfSuccess(response, "VIP_PRIVATE_RULE_UPDATED", id);
        return response;
    }

    @Delete("/{id}")
    public HttpResponse<?> delete(@PathVariable Integer id) {
        HttpResponse<?> response = super.delete(id);
        enqueueRecalcJobIfSuccess(response, "VIP_PRIVATE_RULE_DELETED", id);
        return response;
    }

    @Override
    protected <REQ> ApiResponse<?> validateBeforeCreate(VipPrivateBonusRule entity, REQ req) {
        if (CommonUtil.isNullOrEmpty(entity.getCode())) {
            return ApiResponse.error(-400, "code is required");
        }
        if (CommonUtil.isNullOrEmpty(entity.getName())) {
            return ApiResponse.error(-400, "name is required");
        }
        if (entity.getAmount() == null) {
            return ApiResponse.error(-400, "amount is required");
        }
        if (dao.existsByCode(entity.getCode())) {
            return ApiResponse.error(-400, "code already exists");
        }
        return null;
    }

    @Override
    protected <REQ> ApiResponse<?> validateBeforeUpdate(Integer id, VipPrivateBonusRule entity, REQ req) {
        if (CommonUtil.isNullOrEmpty(entity.getCode())) {
            return ApiResponse.error(-400, "code is required");
        }
        if (CommonUtil.isNullOrEmpty(entity.getName())) {
            return ApiResponse.error(-400, "name is required");
        }
        if (entity.getAmount() == null) {
            return ApiResponse.error(-400, "amount is required");
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

    @Serdeable
    public static class VipPrivateBonusRuleCreateRequest {
        private String code;
        private String name;
        private Double amount;
        private Integer status;
        private String note;

        public String getCode() { return code; }
        public void setCode(String code) { this.code = code; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public Double getAmount() { return amount; }
        public void setAmount(Double amount) { this.amount = amount; }
        public Integer getStatus() { return status; }
        public void setStatus(Integer status) { this.status = status; }
        public String getNote() { return note; }
        public void setNote(String note) { this.note = note; }
    }

    @Serdeable
    public static class VipPrivateBonusRuleUpdateRequest {
        private String code;
        private String name;
        private Double amount;
        private Integer status;
        private String note;

        public String getCode() { return code; }
        public void setCode(String code) { this.code = code; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public Double getAmount() { return amount; }
        public void setAmount(Double amount) { this.amount = amount; }
        public Integer getStatus() { return status; }
        public void setStatus(Integer status) { this.status = status; }
        public String getNote() { return note; }
        public void setNote(String note) { this.note = note; }
    }
}