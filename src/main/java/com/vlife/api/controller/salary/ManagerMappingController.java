package com.vlife.api.controller.salary;

import com.vlife.api.builder.ManagerMappingBuilder;
import com.vlife.api.controller.base.BaseCrudController;
import com.vlife.shared.api.dto.ApiResponse;
import com.vlife.shared.jdbc.dao.salary.ManagerMappingDao;
import com.vlife.shared.jdbc.entity.salary.ManagerMapping;
import io.micronaut.data.model.Page;
import io.micronaut.data.model.Pageable;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.*;
import io.micronaut.serde.annotation.Serdeable;
import jakarta.inject.Inject;

import java.time.LocalDateTime;
import java.util.Map;

import static com.vlife.shared.util.NumberUtil.parseInt;

@Controller("/salary-sale/manager-mappings")
public class ManagerMappingController extends BaseCrudController<ManagerMapping, Integer, ManagerMappingDao> {

    @Inject
    public ManagerMappingController(ManagerMappingDao dao, ManagerMappingBuilder builder) {
        super(dao, builder);
    }

    @Override
    protected Page<ManagerMapping> doSearch(Map<String, String> filters, Pageable pageable) {
        return dao.search(
                parseInt(filters.get("period")),
                parseInt(filters.get("sales_employee_id")),
                parseInt(filters.get("asm_employee_id")),
                parseInt(filters.get("rm_employee_id")),
                parseInt(filters.get("region_id")),
                parseInt(filters.get("province_id")),
                pageable
        );
    }

    @Post
    public HttpResponse<?> create(@Body ManagerMappingRequest req) {
        return handleCreate(req, r -> {
            ManagerMapping x = new ManagerMapping();
            x.setPeriod(String.valueOf(r.getPeriod()));
            x.setSalesEmployeeId(r.getSalesEmployeeId());
            x.setAsmEmployeeId(r.getAsmEmployeeId());
            x.setRmEmployeeId(r.getRmEmployeeId());
            x.setRegionId(r.getRegionId());
            x.setProvinceId(r.getProvinceId());
            x.setCreatedAt(LocalDateTime.now());
            x.setUpdatedAt(LocalDateTime.now());
            return x;
        });
    }

    @Put("/{id}")
    public HttpResponse<?> update(@PathVariable Integer id, @Body ManagerMappingRequest req) {
        return handleUpdate(id, req, r -> {
            ManagerMapping x = new ManagerMapping();
            x.setPeriod(String.valueOf(r.getPeriod()));
            x.setSalesEmployeeId(r.getSalesEmployeeId());
            x.setAsmEmployeeId(r.getAsmEmployeeId());
            x.setRmEmployeeId(r.getRmEmployeeId());
            x.setRegionId(r.getRegionId());
            x.setProvinceId(r.getProvinceId());
            x.setUpdatedAt(LocalDateTime.now());
            mergeNullFromDb(id, x, "id", "createdAt");
            return x;
        });
    }

    @Override
    protected <REQ> ApiResponse<?> validateBeforeCreate(ManagerMapping entity, REQ req) {
        if (entity.getPeriod() == null) return ApiResponse.error(-400, "period is required");
        if (entity.getSalesEmployeeId() == null) return ApiResponse.error(-400, "sales_employee_id is required");
        return null;
    }

    @Override
    protected <REQ> ApiResponse<?> validateBeforeUpdate(Integer id, ManagerMapping entity, REQ req) {
        if (entity.getPeriod() == null) return ApiResponse.error(-400, "period is required");
        if (entity.getSalesEmployeeId() == null) return ApiResponse.error(-400, "sales_employee_id is required");
        return null;
    }

    @Serdeable
    public static class ManagerMappingRequest {
        private Integer period;
        private Integer salesEmployeeId;
        private Integer asmEmployeeId;
        private Integer rmEmployeeId;
        private Integer regionId;
        private Integer provinceId;
        public Integer getPeriod() { return period; }
        public void setPeriod(Integer period) { this.period = period; }
        public Integer getSalesEmployeeId() { return salesEmployeeId; }
        public void setSalesEmployeeId(Integer salesEmployeeId) { this.salesEmployeeId = salesEmployeeId; }
        public Integer getAsmEmployeeId() { return asmEmployeeId; }
        public void setAsmEmployeeId(Integer asmEmployeeId) { this.asmEmployeeId = asmEmployeeId; }
        public Integer getRmEmployeeId() { return rmEmployeeId; }
        public void setRmEmployeeId(Integer rmEmployeeId) { this.rmEmployeeId = rmEmployeeId; }
        public Integer getRegionId() { return regionId; }
        public void setRegionId(Integer regionId) { this.regionId = regionId; }
        public Integer getProvinceId() { return provinceId; }
        public void setProvinceId(Integer provinceId) { this.provinceId = provinceId; }
    }
}