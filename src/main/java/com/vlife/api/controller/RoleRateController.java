package com.vlife.api.controller;
import com.vlife.api.builder.RoleRateBuilder;
import com.vlife.api.controller.base.BaseCrudController;
import com.vlife.shared.api.dto.ApiResponse;
import com.vlife.shared.jdbc.dao.salary.RoleRateDao;
import com.vlife.shared.jdbc.entity.salary.RoleRate;
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

@Controller("/salary-sale/role-rates")
public class RoleRateController extends BaseCrudController<RoleRate, Integer, RoleRateDao> {

    @Inject
    public RoleRateController(RoleRateDao dao, RoleRateBuilder builder) {
        super(dao, builder);
    }

    @Override
    protected Page<RoleRate> doSearch(Map<String, String> filters, Pageable pageable) {
        return dao.search(
                parseInt(filters.get("role_id")),
                parseInt(filters.get("status")),
                parseInt(filters.get("period")),
                pageable
        );
    }

    @Post
    public HttpResponse<?> create(@Body RoleRateRequest req) {
        return handleCreate(req, r -> {
            RoleRate x = new RoleRate();
            x.setRoleId(r.getRoleId());
            x.setSalaryRate(r.getSalaryRate());
            x.setBonusRate(r.getBonusRate());
            x.setBasicSalaryRate(r.getBasicSalaryRate());
            x.setAllowanceRate(r.getAllowanceRate());
            x.setEffectiveFrom(r.getEffectiveFrom());
            x.setEffectiveTo(r.getEffectiveTo());
            x.setStatus(r.getStatus() != null ? r.getStatus() : 1);
            x.setCreatedAt(LocalDateTime.now());
            x.setUpdatedAt(LocalDateTime.now());
            return x;
        });
    }

    @Put("/{id}")
    public HttpResponse<?> update(@PathVariable Integer id, @Body RoleRateRequest req) {
        return handleUpdate(id, req, r -> {
            RoleRate x = new RoleRate();
            x.setRoleId(r.getRoleId());
            x.setSalaryRate(r.getSalaryRate());
            x.setBonusRate(r.getBonusRate());
            x.setBasicSalaryRate(r.getBasicSalaryRate());
            x.setAllowanceRate(r.getAllowanceRate());
            x.setEffectiveFrom(r.getEffectiveFrom());
            x.setEffectiveTo(r.getEffectiveTo());
            x.setStatus(r.getStatus());
            x.setUpdatedAt(LocalDateTime.now());
            mergeNullFromDb(id, x, "id", "createdAt");
            return x;
        });
    }

    @Override
    protected <REQ> ApiResponse<?> validateBeforeCreate(RoleRate entity, REQ req) {
        if (entity.getRoleId() == null) return ApiResponse.error(-400, "role_id is required");
        if (entity.getSalaryRate() == null) return ApiResponse.error(-400, "salary_rate is required");
        if (entity.getBonusRate() == null) return ApiResponse.error(-400, "bonus_rate is required");
        if (entity.getBasicSalaryRate() == null) return ApiResponse.error(-400, "basic_salary_rate is required");
        if (entity.getAllowanceRate() == null) return ApiResponse.error(-400, "allowance_rate is required");
        if (entity.getEffectiveFrom() == null) return ApiResponse.error(-400, "effective_from is required");
        return null;
    }

    @Override
    protected <REQ> ApiResponse<?> validateBeforeUpdate(Integer id, RoleRate entity, REQ req) {
        if (entity.getRoleId() == null) return ApiResponse.error(-400, "role_id is required");
        if (entity.getSalaryRate() == null) return ApiResponse.error(-400, "salary_rate is required");
        if (entity.getBonusRate() == null) return ApiResponse.error(-400, "bonus_rate is required");
        if (entity.getBasicSalaryRate() == null) return ApiResponse.error(-400, "basic_salary_rate is required");
        if (entity.getAllowanceRate() == null) return ApiResponse.error(-400, "allowance_rate is required");
        if (entity.getEffectiveFrom() == null) return ApiResponse.error(-400, "effective_from is required");
        return null;
    }

    @Serdeable
    public static class RoleRateRequest {
        private Integer roleId;
        private Double salaryRate;
        private Double bonusRate;
        private Double basicSalaryRate;
        private Double allowanceRate;
        private LocalDate effectiveFrom;
        private LocalDate effectiveTo;
        private Integer status;

        public Integer getRoleId() { return roleId; }
        public void setRoleId(Integer roleId) { this.roleId = roleId; }
        public Double getSalaryRate() { return salaryRate; }
        public void setSalaryRate(Double salaryRate) { this.salaryRate = salaryRate; }
        public Double getBonusRate() { return bonusRate; }
        public void setBonusRate(Double bonusRate) { this.bonusRate = bonusRate; }
        public Double getBasicSalaryRate() { return basicSalaryRate; }
        public void setBasicSalaryRate(Double basicSalaryRate) { this.basicSalaryRate = basicSalaryRate; }
        public Double getAllowanceRate() { return allowanceRate; }
        public void setAllowanceRate(Double allowanceRate) { this.allowanceRate = allowanceRate; }
        public LocalDate getEffectiveFrom() { return effectiveFrom; }
        public void setEffectiveFrom(LocalDate effectiveFrom) { this.effectiveFrom = effectiveFrom; }
        public LocalDate getEffectiveTo() { return effectiveTo; }
        public void setEffectiveTo(LocalDate effectiveTo) { this.effectiveTo = effectiveTo; }
        public Integer getStatus() { return status; }
        public void setStatus(Integer status) { this.status = status; }
    }
}