package com.vlife.api.controller;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.vlife.api.builder.EmployeeScopeBuilder;
import com.vlife.api.controller.base.BaseCrudController;
import com.vlife.shared.api.dto.ApiResponse;
import com.vlife.shared.jdbc.dao.salary.EmployeeScopeDao;
import com.vlife.shared.jdbc.entity.salary.EmployeeScope;
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

@Controller("/employee-scopes")
public class EmployeeScopeController extends BaseCrudController<EmployeeScope, Integer, EmployeeScopeDao> {

    @Inject
    public EmployeeScopeController(EmployeeScopeDao dao, EmployeeScopeBuilder builder) {
        super(dao, builder);
    }

    @Override
    protected Page<EmployeeScope> doSearch(Map<String, String> filters, Pageable pageable) {
        return dao.search(
                parseInt(filters.get("period")),
                parseInt(filters.get("employee_id")),
                parseInt(filters.get("role_id")),
                parseInt(filters.get("region_id")),
                parseInt(filters.get("province_id")),
                parseInt(filters.get("status")),
                pageable
        );
    }

    @Post
    public HttpResponse<?> create(@Body EmployeeScopeRequest req) {
        return handleCreate(req, r -> {
            EmployeeScope x = new EmployeeScope();
            x.setEmployeeId(r.getEmployeeId());
            x.setRoleId(r.getRoleId());
            x.setRegionId(r.getRegionId());
            x.setProvinceId(r.getProvinceId());
            x.setIsPersonalTarget(r.getIsPersonalTarget() != null ? r.getIsPersonalTarget() : 0);
            x.setIsManagerTarget(r.getIsManagerTarget() != null ? r.getIsManagerTarget() : 0);
            x.setEffectiveFrom(r.getEffectiveFrom());
            x.setEffectiveTo(r.getEffectiveTo());
            x.setStatus(r.getStatus() != null ? r.getStatus() : 1);
            x.setCreatedAt(LocalDateTime.now());
            x.setUpdatedAt(LocalDateTime.now());
            return x;
        });
    }

    @Put("/{id}")
    public HttpResponse<?> update(@PathVariable Integer id, @Body EmployeeScopeRequest req) {
        return handleUpdate(id, req, r -> {
            EmployeeScope x = new EmployeeScope();
            x.setEmployeeId(r.getEmployeeId());
            x.setRoleId(r.getRoleId());
            x.setRegionId(r.getRegionId());
            x.setProvinceId(r.getProvinceId());
            x.setIsPersonalTarget(r.getIsPersonalTarget());
            x.setIsManagerTarget(r.getIsManagerTarget());
            x.setEffectiveFrom(r.getEffectiveFrom());
            x.setEffectiveTo(r.getEffectiveTo());
            x.setStatus(r.getStatus());
            x.setUpdatedAt(LocalDateTime.now());
            mergeNullFromDb(id, x, "id", "createdAt");
            return x;
        });
    }

    @Override
    protected <REQ> ApiResponse<?> validateBeforeCreate(EmployeeScope entity, REQ req) {
        if (entity.getEmployeeId() == null) return ApiResponse.error(-400, "employee_id is required");
        if (entity.getRoleId() == null) return ApiResponse.error(-400, "role_id is required");
        if (entity.getEffectiveFrom() == null) return ApiResponse.error(-400, "effective_from is required");
        return null;
    }

    @Override
    protected <REQ> ApiResponse<?> validateBeforeUpdate(Integer id, EmployeeScope entity, REQ req) {
        if (entity.getEmployeeId() == null) return ApiResponse.error(-400, "employee_id is required");
        if (entity.getRoleId() == null) return ApiResponse.error(-400, "role_id is required");
        if (entity.getEffectiveFrom() == null) return ApiResponse.error(-400, "effective_from is required");
        return null;
    }

    @Serdeable
    public static class EmployeeScopeRequest {
        @JsonProperty("employee_id")
        private Integer employeeId;

        @JsonProperty("role_id")
        private Integer roleId;

        @JsonProperty("region_id")
        private Integer regionId;

        @JsonProperty("province_id")
        private Integer provinceId;

        @JsonProperty("is_personal_target")
        private Integer isPersonalTarget;

        @JsonProperty("is_manager_target")
        private Integer isManagerTarget;

        @JsonProperty("effective_from")
        private LocalDate effectiveFrom;

        @JsonProperty("effective_to")
        private LocalDate effectiveTo;

        private Integer status;

        public Integer getEmployeeId() {
            return employeeId;
        }

        public void setEmployeeId(Integer employeeId) {
            this.employeeId = employeeId;
        }

        public Integer getRoleId() {
            return roleId;
        }

        public void setRoleId(Integer roleId) {
            this.roleId = roleId;
        }

        public Integer getRegionId() {
            return regionId;
        }

        public void setRegionId(Integer regionId) {
            this.regionId = regionId;
        }

        public Integer getProvinceId() {
            return provinceId;
        }

        public void setProvinceId(Integer provinceId) {
            this.provinceId = provinceId;
        }

        public Integer getIsPersonalTarget() {
            return isPersonalTarget;
        }

        public void setIsPersonalTarget(Integer isPersonalTarget) {
            this.isPersonalTarget = isPersonalTarget;
        }

        public Integer getIsManagerTarget() {
            return isManagerTarget;
        }

        public void setIsManagerTarget(Integer isManagerTarget) {
            this.isManagerTarget = isManagerTarget;
        }

        public LocalDate getEffectiveFrom() {
            return effectiveFrom;
        }

        public void setEffectiveFrom(LocalDate effectiveFrom) {
            this.effectiveFrom = effectiveFrom;
        }

        public LocalDate getEffectiveTo() {
            return effectiveTo;
        }

        public void setEffectiveTo(LocalDate effectiveTo) {
            this.effectiveTo = effectiveTo;
        }

        public Integer getStatus() {
            return status;
        }

        public void setStatus(Integer status) {
            this.status = status;
        }
    }
}