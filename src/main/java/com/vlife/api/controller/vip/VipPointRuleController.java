package com.vlife.api.controller.vip;

import com.vlife.api.builder.vip.VipPointRuleBuilder;
import com.vlife.api.controller.base.BaseVipRecalcCrudController;
import com.vlife.api.util.ApiUtil;
import com.vlife.shared.api.dto.ApiResponse;
import com.vlife.shared.jdbc.dao.vip.VipPointRuleDao;
import com.vlife.shared.jdbc.entity.vip.VipPointRule;
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

@Controller("/vip/point-rules")
public class VipPointRuleController extends BaseVipRecalcCrudController<VipPointRule, Integer, VipPointRuleDao> {

    @Inject
    public VipPointRuleController(
            VipPointRuleDao dao,
            VipPointRuleBuilder builder,
            VipRecalcJobService vipRecalcJobService
    ) {
        super(dao, builder, vipRecalcJobService);
    }

    @Override
    protected Page<VipPointRule> doSearch(Map<String, String> filters, Pageable pageable) {
        String keyword = ApiUtil.trim(filters.get("keyword"));
        Integer status = ApiUtil.parseInteger(filters.get("status"));
        return dao.search(keyword, null, status, null, null, pageable);
    }

    @Post
    public HttpResponse<?> create(@Body VipPointRuleCreateRequest req) {
        HttpResponse<?> response = handleCreate(req, r -> {
            VipPointRule e = new VipPointRule();
            e.setVthhCon(ApiUtil.trim(r.getVthh_con()));
            e.setFromValue(r.getFrom_value());
            e.setToValue(r.getTo_value());
            e.setHeSoMb(r.getHe_so_mb());
            e.setHeSoMn(r.getHe_so_mn());
            e.setStatus(r.getStatus() != null ? r.getStatus() : 1);
            e.setNote(ApiUtil.trim(r.getNote()));

            LocalDateTime now = LocalDateTime.now();
            e.setCreatedAt(now);
            e.setUpdatedAt(now);
            return e;
        });

        enqueueRecalcJobIfSuccess(response, "VIP_POINT_RULE_CREATED", null);
        return response;
    }

    @Put("/{id}")
    public HttpResponse<?> update(@PathVariable Integer id, @Body VipPointRuleUpdateRequest req) {
        HttpResponse<?> response = handleUpdate(id, req, r -> {
            VipPointRule e = new VipPointRule();
            e.setVthhCon(ApiUtil.trim(r.getVthh_con()));
            e.setFromValue(r.getFrom_value());
            e.setToValue(r.getTo_value());
            e.setHeSoMb(r.getHe_so_mb());
            e.setHeSoMn(r.getHe_so_mn());
            e.setStatus(r.getStatus());
            e.setNote(ApiUtil.trim(r.getNote()));
            e.setUpdatedAt(LocalDateTime.now());

            mergeNullFromDb(id, e, "id", "createdAt");
            return e;
        });

        enqueueRecalcJobIfSuccess(response, "VIP_POINT_RULE_UPDATED", id);
        return response;
    }

    @Delete("/{id}")
    public HttpResponse<?> delete(@PathVariable Integer id) {
        HttpResponse<?> response = super.delete(id);
        enqueueRecalcJobIfSuccess(response, "VIP_POINT_RULE_DELETED", id);
        return response;
    }

    @Override
    protected <REQ> ApiResponse<?> validateBeforeCreate(VipPointRule entity, REQ req) {
        if (CommonUtil.isNullOrEmpty(entity.getVthhCon())) {
            return ApiResponse.error(-400, "vthh_con is required");
        }
        if (entity.getFromValue() == null) {
            return ApiResponse.error(-400, "from_value is required");
        }
        if (entity.getToValue() == null) {
            return ApiResponse.error(-400, "to_value is required");
        }
        if (entity.getHeSoMb() == null) {
            return ApiResponse.error(-400, "he_so_mb is required");
        }
        if (entity.getHeSoMn() == null) {
            return ApiResponse.error(-400, "he_so_mn is required");
        }
        if (entity.getFromValue() > entity.getToValue()) {
            return ApiResponse.error(-400, "from_value must be <= to_value");
        }
        return null;
    }

    @Override
    protected <REQ> ApiResponse<?> validateBeforeUpdate(Integer id, VipPointRule entity, REQ req) {
        var oldOpt = dao.findById(id);
        if (oldOpt.isEmpty()) {
            return ApiResponse.error(-404, "not found");
        }

        if (CommonUtil.isNullOrEmpty(entity.getVthhCon())) {
            return ApiResponse.error(-400, "vthh_con is required");
        }
        if (entity.getFromValue() == null) {
            return ApiResponse.error(-400, "from_value is required");
        }
        if (entity.getToValue() == null) {
            return ApiResponse.error(-400, "to_value is required");
        }
        if (entity.getHeSoMb() == null) {
            return ApiResponse.error(-400, "he_so_mb is required");
        }
        if (entity.getHeSoMn() == null) {
            return ApiResponse.error(-400, "he_so_mn is required");
        }
        if (entity.getFromValue() > entity.getToValue()) {
            return ApiResponse.error(-400, "from_value must be <= to_value");
        }

        return null;
    }

    @Serdeable
    public static class VipPointRuleCreateRequest {
        private String vthh_con;
        private Double from_value;
        private Double to_value;
        private Double he_so_mb;
        private Double he_so_mn;
        private Integer status;
        private String note;

        public String getVthh_con() { return vthh_con; }
        public void setVthh_con(String vthh_con) { this.vthh_con = vthh_con; }
        public Double getFrom_value() { return from_value; }
        public void setFrom_value(Double from_value) { this.from_value = from_value; }
        public Double getTo_value() { return to_value; }
        public void setTo_value(Double to_value) { this.to_value = to_value; }
        public Double getHe_so_mb() { return he_so_mb; }
        public void setHe_so_mb(Double he_so_mb) { this.he_so_mb = he_so_mb; }
        public Double getHe_so_mn() { return he_so_mn; }
        public void setHe_so_mn(Double he_so_mn) { this.he_so_mn = he_so_mn; }
        public Integer getStatus() { return status; }
        public void setStatus(Integer status) { this.status = status; }
        public String getNote() { return note; }
        public void setNote(String note) { this.note = note; }
    }

    @Serdeable
    public static class VipPointRuleUpdateRequest {
        private String vthh_con;
        private Double from_value;
        private Double to_value;
        private Double he_so_mb;
        private Double he_so_mn;
        private Integer status;
        private String note;

        public String getVthh_con() { return vthh_con; }
        public void setVthh_con(String vthh_con) { this.vthh_con = vthh_con; }
        public Double getFrom_value() { return from_value; }
        public void setFrom_value(Double from_value) { this.from_value = from_value; }
        public Double getTo_value() { return to_value; }
        public void setTo_value(Double to_value) { this.to_value = to_value; }
        public Double getHe_so_mb() { return he_so_mb; }
        public void setHe_so_mb(Double he_so_mb) { this.he_so_mb = he_so_mb; }
        public Double getHe_so_mn() { return he_so_mn; }
        public void setHe_so_mn(Double he_so_mn) { this.he_so_mn = he_so_mn; }
        public Integer getStatus() { return status; }
        public void setStatus(Integer status) { this.status = status; }
        public String getNote() { return note; }
        public void setNote(String note) { this.note = note; }
    }
}