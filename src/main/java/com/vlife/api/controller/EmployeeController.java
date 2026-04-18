package com.vlife.api.controller;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.vlife.api.builder.EmployeeBuilder;
import com.vlife.api.controller.base.BaseCrudController;
import com.vlife.shared.api.dto.ApiResponse;
import com.vlife.shared.jdbc.dao.EmployeeDao;
import com.vlife.shared.jdbc.entity.Employee;
import io.micronaut.data.model.Page;
import io.micronaut.data.model.Pageable;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.*;
import io.micronaut.serde.annotation.Serdeable;
import jakarta.inject.Inject;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

import static com.vlife.shared.util.NumberUtil.parseInt;
import static com.vlife.shared.util.StringUtil.isBlank;
import static com.vlife.shared.util.StringUtil.trim;

@Controller("/employees")
public class EmployeeController extends BaseCrudController<Employee, Integer, EmployeeDao> {

    @Inject
    public EmployeeController(EmployeeDao dao, EmployeeBuilder builder) {
        super(dao, builder);
    }

    @Override
    protected Page<Employee> doSearch(Map<String, String> filters, Pageable pageable) {
        return dao.search(
                trim(filters.get("keyword")),
                parseInt(filters.get("status")),
                pageable
        );
    }

    @Post
    public HttpResponse<?> create(@Body EmployeeCreateRequest req) {
        return handleCreate(req, r -> {
            Employee x = new Employee();
            x.setCode(trim(r.getCode()));
            x.setName(trim(r.getName()));
            x.setTaxCode(trim(r.getTaxCode()));
            x.setDependentCount(r.getDependentCount() != null ? r.getDependentCount() : 0);
            x.setInsuranceBase(r.getInsuranceBase() != null ? r.getInsuranceBase() : BigDecimal.ZERO);
            x.setBasicSalary(r.getBasicSalary() != null ? r.getBasicSalary() : BigDecimal.ZERO);
            x.setAllowanceSalary(r.getAllowanceSalary() != null ? r.getAllowanceSalary() : BigDecimal.ZERO);
            x.setIsUnionMember(r.getIsUnionMember() != null ? r.getIsUnionMember() : 0);
            x.setStatus(r.getStatus() != null ? r.getStatus() : 1);
            x.setJoinedAt(r.getJoinedAt());
            x.setLeftAt(r.getLeftAt());
            x.setCreatedAt(LocalDateTime.now());
            x.setUpdatedAt(LocalDateTime.now());
            return x;
        });
    }

    @Put("/{id}")
    public HttpResponse<?> update(@PathVariable Integer id, @Body EmployeeUpdateRequest req) {
        return handleUpdate(id, req, r -> {
            Employee x = new Employee();
            x.setCode(trim(r.getCode()));
            x.setName(trim(r.getName()));
            x.setTaxCode(trim(r.getTaxCode()));
            x.setDependentCount(r.getDependentCount());
            x.setInsuranceBase(r.getInsuranceBase());
            x.setBasicSalary(r.getBasicSalary());
            x.setAllowanceSalary(r.getAllowanceSalary());
            x.setIsUnionMember(r.getIsUnionMember());
            x.setStatus(r.getStatus());
            x.setJoinedAt(r.getJoinedAt());
            x.setLeftAt(r.getLeftAt());
            x.setUpdatedAt(LocalDateTime.now());
            mergeNullFromDb(id, x, "id", "createdAt");
            return x;
        });
    }

    @Override
    protected <REQ> ApiResponse<?> validateBeforeCreate(Employee entity, REQ req) {
        if (isBlank(entity.getCode())) return ApiResponse.error(-400, "code is required");
        if (isBlank(entity.getName())) return ApiResponse.error(-400, "name is required");
        if (dao.findByCode(entity.getCode()).isPresent()) return ApiResponse.error(-400, "code already exists");
        return null;
    }

    @Override
    protected <REQ> ApiResponse<?> validateBeforeUpdate(Integer id, Employee entity, REQ req) {
        if (isBlank(entity.getCode())) return ApiResponse.error(-400, "code is required");
        if (isBlank(entity.getName())) return ApiResponse.error(-400, "name is required");

        var oldOpt = dao.findById(id);
        if (oldOpt.isEmpty()) return ApiResponse.error(-404, "not found");

        var old = oldOpt.get();
        if (!entity.getCode().equals(old.getCode()) && dao.findByCode(entity.getCode()).isPresent()) {
            return ApiResponse.error(-400, "code already exists");
        }
        return null;
    }

    @Serdeable
    public static class EmployeeCreateRequest {
        private String code;
        private String name;

        @JsonProperty("tax_code")
        private String taxCode;

        @JsonProperty("dependent_count")
        private Integer dependentCount;

        @JsonProperty("insurance_base")
        private BigDecimal insuranceBase;

        @JsonProperty("basic_salary")
        private BigDecimal basicSalary;

        @JsonProperty("allowance_salary")
        private BigDecimal allowanceSalary;

        @JsonProperty("is_union_member")
        private Integer isUnionMember;

        private Integer status;

        @JsonProperty("joined_at")
        private LocalDate joinedAt;

        @JsonProperty("left_at")
        private LocalDate leftAt;

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

        public String getTaxCode() {
            return taxCode;
        }

        public void setTaxCode(String taxCode) {
            this.taxCode = taxCode;
        }

        public Integer getDependentCount() {
            return dependentCount;
        }

        public void setDependentCount(Integer dependentCount) {
            this.dependentCount = dependentCount;
        }

        public BigDecimal getInsuranceBase() {
            return insuranceBase;
        }

        public void setInsuranceBase(BigDecimal insuranceBase) {
            this.insuranceBase = insuranceBase;
        }

        public BigDecimal getBasicSalary() {
            return basicSalary;
        }

        public void setBasicSalary(BigDecimal basicSalary) {
            this.basicSalary = basicSalary;
        }

        public BigDecimal getAllowanceSalary() {
            return allowanceSalary;
        }

        public void setAllowanceSalary(BigDecimal allowanceSalary) {
            this.allowanceSalary = allowanceSalary;
        }

        public Integer getIsUnionMember() {
            return isUnionMember;
        }

        public void setIsUnionMember(Integer isUnionMember) {
            this.isUnionMember = isUnionMember;
        }

        public Integer getStatus() {
            return status;
        }

        public void setStatus(Integer status) {
            this.status = status;
        }

        public LocalDate getJoinedAt() {
            return joinedAt;
        }

        public void setJoinedAt(LocalDate joinedAt) {
            this.joinedAt = joinedAt;
        }

        public LocalDate getLeftAt() {
            return leftAt;
        }

        public void setLeftAt(LocalDate leftAt) {
            this.leftAt = leftAt;
        }
    }

    @Serdeable
    public static class EmployeeUpdateRequest extends EmployeeCreateRequest {
    }
}