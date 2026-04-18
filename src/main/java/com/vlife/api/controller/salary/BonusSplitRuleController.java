package com.vlife.api.controller.salary;
import com.vlife.api.builder.BonusSplitRuleBuilder;
import com.vlife.api.controller.base.BaseCrudController;
import com.vlife.shared.api.dto.ApiResponse;
import com.vlife.shared.jdbc.dao.salary.BonusSplitRuleDao;
import com.vlife.shared.jdbc.entity.salary.BonusSplitRule;
import io.micronaut.data.model.Page;
import io.micronaut.data.model.Pageable;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.*;
import io.micronaut.serde.annotation.Serdeable;
import jakarta.inject.Inject;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

import static com.vlife.shared.util.NumberUtil.parseInt;
import static com.vlife.shared.util.StringUtil.isBlank;
import static com.vlife.shared.util.StringUtil.trim;

@Controller("/salary-sale/bonus-split-rules")
public class BonusSplitRuleController extends BaseCrudController<BonusSplitRule, Integer, BonusSplitRuleDao> {

    @Inject
    public BonusSplitRuleController(BonusSplitRuleDao dao, BonusSplitRuleBuilder builder) {
        super(dao, builder);
    }

    @Override
    protected Page<BonusSplitRule> doSearch(Map<String, String> filters, Pageable pageable) {
        return dao.search(
                parseInt(filters.get("has_asm")),
                parseInt(filters.get("status")),
                parseInt(filters.get("period")),
                pageable
        );
    }

    @Post
    public HttpResponse<?> create(@Body BonusSplitRuleRequest req) {
        return handleCreate(req, r -> {
            BonusSplitRule x = new BonusSplitRule();
            x.setCode(trim(r.getCode()));
            x.setHasAsm(r.getHasAsm());
            x.setSalesRate(r.getSalesRate());
            x.setAsmRate(r.getAsmRate());
            x.setRmRate(r.getRmRate());
            x.setEffectiveFrom(r.getEffectiveFrom());
            x.setEffectiveTo(r.getEffectiveTo());
            x.setStatus(r.getStatus() != null ? r.getStatus() : 1);
            x.setDescription(trim(r.getDescription()));
            x.setCreatedAt(LocalDateTime.now());
            x.setUpdatedAt(LocalDateTime.now());
            return x;
        });
    }

    @Put("/{id}")
    public HttpResponse<?> update(@PathVariable Integer id, @Body BonusSplitRuleRequest req) {
        return handleUpdate(id, req, r -> {
            BonusSplitRule x = new BonusSplitRule();
            x.setCode(trim(r.getCode()));
            x.setHasAsm(r.getHasAsm());
            x.setSalesRate(r.getSalesRate());
            x.setAsmRate(r.getAsmRate());
            x.setRmRate(r.getRmRate());
            x.setEffectiveFrom(r.getEffectiveFrom());
            x.setEffectiveTo(r.getEffectiveTo());
            x.setStatus(r.getStatus());
            x.setDescription(trim(r.getDescription()));
            x.setUpdatedAt(LocalDateTime.now());
            mergeNullFromDb(id, x, "id", "createdAt");
            return x;
        });
    }

    @Override
    protected <REQ> ApiResponse<?> validateBeforeCreate(BonusSplitRule entity, REQ req) {
        if (isBlank(entity.getCode())) return ApiResponse.error(-400, "code is required");
        if (entity.getHasAsm() == null) return ApiResponse.error(-400, "has_asm is required");
        if (entity.getSalesRate() == null) return ApiResponse.error(-400, "sales_rate is required");
        if (entity.getAsmRate() == null) return ApiResponse.error(-400, "asm_rate is required");
        if (entity.getRmRate() == null) return ApiResponse.error(-400, "rm_rate is required");
        if (entity.getEffectiveFrom() == null) return ApiResponse.error(-400, "effective_from is required");
        return null;
    }

    @Override
    protected <REQ> ApiResponse<?> validateBeforeUpdate(Integer id, BonusSplitRule entity, REQ req) {
        if (isBlank(entity.getCode())) return ApiResponse.error(-400, "code is required");
        if (entity.getHasAsm() == null) return ApiResponse.error(-400, "has_asm is required");
        if (entity.getSalesRate() == null) return ApiResponse.error(-400, "sales_rate is required");
        if (entity.getAsmRate() == null) return ApiResponse.error(-400, "asm_rate is required");
        if (entity.getRmRate() == null) return ApiResponse.error(-400, "rm_rate is required");
        if (entity.getEffectiveFrom() == null) return ApiResponse.error(-400, "effective_from is required");
        return null;
    }

    @Serdeable
    public static class BonusSplitRuleRequest {
        private String code;
        private Integer hasAsm;
        private Double salesRate;
        private Double asmRate;
        private Double rmRate;
        private LocalDate effectiveFrom;
        private LocalDate effectiveTo;
        private Integer status;
        private String description;

        public String getCode() { return code; }
        public void setCode(String code) { this.code = code; }
        public Integer getHasAsm() { return hasAsm; }
        public void setHasAsm(Integer hasAsm) { this.hasAsm = hasAsm; }
        public Double getSalesRate() { return salesRate; }
        public void setSalesRate(Double salesRate) { this.salesRate = salesRate; }
        public Double getAsmRate() { return asmRate; }
        public void setAsmRate(Double asmRate) { this.asmRate = asmRate; }
        public Double getRmRate() { return rmRate; }
        public void setRmRate(Double rmRate) { this.rmRate = rmRate; }
        public LocalDate getEffectiveFrom() { return effectiveFrom; }
        public void setEffectiveFrom(LocalDate effectiveFrom) { this.effectiveFrom = effectiveFrom; }
        public LocalDate getEffectiveTo() { return effectiveTo; }
        public void setEffectiveTo(LocalDate effectiveTo) { this.effectiveTo = effectiveTo; }
        public Integer getStatus() { return status; }
        public void setStatus(Integer status) { this.status = status; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
    }
}