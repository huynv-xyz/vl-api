package com.vlife.api.controller.salary;
import com.vlife.api.builder.ProgressiveBonusTierBuilder;
import com.vlife.api.controller.base.BaseCrudController;
import com.vlife.shared.api.dto.ApiResponse;
import com.vlife.shared.jdbc.dao.salary.ProgressiveBonusTierDao;
import com.vlife.shared.jdbc.entity.salary.ProgressiveBonusTier;
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

@Controller("/salary-sale/progressive-bonus-tiers")
public class ProgressiveBonusTierController extends BaseCrudController<ProgressiveBonusTier, Integer, ProgressiveBonusTierDao> {

    @Inject
    public ProgressiveBonusTierController(ProgressiveBonusTierDao dao, ProgressiveBonusTierBuilder builder) {
        super(dao, builder);
    }

    @Override
    protected Page<ProgressiveBonusTier> doSearch(Map<String, String> filters, Pageable pageable) {
        return dao.search(
                parseInt(filters.get("status")),
                parseInt(filters.get("period")),
                pageable
        );
    }

    @Post
    public HttpResponse<?> create(@Body ProgressiveBonusTierRequest req) {
        return handleCreate(req, r -> {
            ProgressiveBonusTier x = new ProgressiveBonusTier();
            x.setCode(trim(r.getCode()));
            x.setFromRate(r.getFromRate());
            x.setToRate(r.getToRate());
            x.setBonusRate(r.getBonusRate());
            x.setEffectiveFrom(r.getEffectiveFrom());
            x.setEffectiveTo(r.getEffectiveTo());
            x.setStatus(r.getStatus() != null ? r.getStatus() : 1);
            x.setSortOrder(r.getSortOrder() != null ? r.getSortOrder() : 0);
            x.setDescription(trim(r.getDescription()));
            x.setCreatedAt(LocalDateTime.now());
            x.setUpdatedAt(LocalDateTime.now());
            return x;
        });
    }

    @Put("/{id}")
    public HttpResponse<?> update(@PathVariable Integer id, @Body ProgressiveBonusTierRequest req) {
        return handleUpdate(id, req, r -> {
            ProgressiveBonusTier x = new ProgressiveBonusTier();
            x.setCode(trim(r.getCode()));
            x.setFromRate(r.getFromRate());
            x.setToRate(r.getToRate());
            x.setBonusRate(r.getBonusRate());
            x.setEffectiveFrom(r.getEffectiveFrom());
            x.setEffectiveTo(r.getEffectiveTo());
            x.setStatus(r.getStatus());
            x.setSortOrder(r.getSortOrder());
            x.setDescription(trim(r.getDescription()));
            x.setUpdatedAt(LocalDateTime.now());
            mergeNullFromDb(id, x, "id", "createdAt");
            return x;
        });
    }

    @Override
    protected <REQ> ApiResponse<?> validateBeforeCreate(ProgressiveBonusTier entity, REQ req) {
        if (isBlank(entity.getCode())) return ApiResponse.error(-400, "code is required");
        if (entity.getFromRate() == null) return ApiResponse.error(-400, "from_rate is required");
        if (entity.getBonusRate() == null) return ApiResponse.error(-400, "bonus_rate is required");
        if (entity.getEffectiveFrom() == null) return ApiResponse.error(-400, "effective_from is required");
        return null;
    }

    @Override
    protected <REQ> ApiResponse<?> validateBeforeUpdate(Integer id, ProgressiveBonusTier entity, REQ req) {
        if (isBlank(entity.getCode())) return ApiResponse.error(-400, "code is required");
        if (entity.getFromRate() == null) return ApiResponse.error(-400, "from_rate is required");
        if (entity.getBonusRate() == null) return ApiResponse.error(-400, "bonus_rate is required");
        if (entity.getEffectiveFrom() == null) return ApiResponse.error(-400, "effective_from is required");
        return null;
    }

    @Serdeable
    public static class ProgressiveBonusTierRequest {
        private String code;
        private Double fromRate;
        private Double toRate;
        private Double bonusRate;
        private LocalDate effectiveFrom;
        private LocalDate effectiveTo;
        private Integer status;
        private Integer sortOrder;
        private String description;

        public String getCode() { return code; }
        public void setCode(String code) { this.code = code; }
        public Double getFromRate() { return fromRate; }
        public void setFromRate(Double fromRate) { this.fromRate = fromRate; }
        public Double getToRate() { return toRate; }
        public void setToRate(Double toRate) { this.toRate = toRate; }
        public Double getBonusRate() { return bonusRate; }
        public void setBonusRate(Double bonusRate) { this.bonusRate = bonusRate; }
        public LocalDate getEffectiveFrom() { return effectiveFrom; }
        public void setEffectiveFrom(LocalDate effectiveFrom) { this.effectiveFrom = effectiveFrom; }
        public LocalDate getEffectiveTo() { return effectiveTo; }
        public void setEffectiveTo(LocalDate effectiveTo) { this.effectiveTo = effectiveTo; }
        public Integer getStatus() { return status; }
        public void setStatus(Integer status) { this.status = status; }
        public Integer getSortOrder() { return sortOrder; }
        public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
    }
}