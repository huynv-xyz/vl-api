package com.vlife.api.controller.salary;
import com.vlife.api.builder.ConfigParameterBuilder;
import com.vlife.api.controller.base.BaseCrudController;
import com.vlife.shared.api.dto.ApiResponse;
import com.vlife.shared.jdbc.dao.salary.ConfigParameterDao;
import com.vlife.shared.jdbc.entity.salary.ConfigParameter;
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

@Controller("/salary-sale/config-parameters")
public class ConfigParameterController extends BaseCrudController<ConfigParameter, Integer, ConfigParameterDao> {

    @Inject
    public ConfigParameterController(ConfigParameterDao dao, ConfigParameterBuilder builder) {
        super(dao, builder);
    }

    @Override
    protected Page<ConfigParameter> doSearch(Map<String, String> filters, Pageable pageable) {
        return dao.search(
                trim(filters.get("group_code")),
                trim(filters.get("code")),
                parseInt(filters.get("status")),
                pageable
        );
    }

    @Post
    public HttpResponse<?> create(@Body ConfigParameterRequest req) {
        return handleCreate(req, r -> {
            ConfigParameter x = new ConfigParameter();
            x.setGroupCode(trim(r.getGroupCode()));
            x.setCode(trim(r.getCode()));
            x.setValue(trim(r.getValue()));
            x.setValueType(trim(r.getValueType()));
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
    public HttpResponse<?> update(@PathVariable Integer id, @Body ConfigParameterRequest req) {
        return handleUpdate(id, req, r -> {
            ConfigParameter x = new ConfigParameter();
            x.setGroupCode(trim(r.getGroupCode()));
            x.setCode(trim(r.getCode()));
            x.setValue(trim(r.getValue()));
            x.setValueType(trim(r.getValueType()));
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
    protected <REQ> ApiResponse<?> validateBeforeCreate(ConfigParameter entity, REQ req) {
        if (isBlank(entity.getGroupCode())) return ApiResponse.error(-400, "group_code is required");
        if (isBlank(entity.getCode())) return ApiResponse.error(-400, "code is required");
        if (isBlank(entity.getValue())) return ApiResponse.error(-400, "value is required");
        if (isBlank(entity.getValueType())) return ApiResponse.error(-400, "value_type is required");
        if (entity.getEffectiveFrom() == null) return ApiResponse.error(-400, "effective_from is required");
        return null;
    }

    @Override
    protected <REQ> ApiResponse<?> validateBeforeUpdate(Integer id, ConfigParameter entity, REQ req) {
        if (isBlank(entity.getGroupCode())) return ApiResponse.error(-400, "group_code is required");
        if (isBlank(entity.getCode())) return ApiResponse.error(-400, "code is required");
        if (isBlank(entity.getValue())) return ApiResponse.error(-400, "value is required");
        if (isBlank(entity.getValueType())) return ApiResponse.error(-400, "value_type is required");
        if (entity.getEffectiveFrom() == null) return ApiResponse.error(-400, "effective_from is required");
        return null;
    }

    @Serdeable
    public static class ConfigParameterRequest {
        private String groupCode;
        private String code;
        private String value;
        private String valueType;
        private LocalDate effectiveFrom;
        private LocalDate effectiveTo;
        private Integer status;
        private String description;

        public String getGroupCode() { return groupCode; }
        public void setGroupCode(String groupCode) { this.groupCode = groupCode; }
        public String getCode() { return code; }
        public void setCode(String code) { this.code = code; }
        public String getValue() { return value; }
        public void setValue(String value) { this.value = value; }
        public String getValueType() { return valueType; }
        public void setValueType(String valueType) { this.valueType = valueType; }
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