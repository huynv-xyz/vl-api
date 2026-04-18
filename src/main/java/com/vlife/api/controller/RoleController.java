package com.vlife.api.controller;
import com.vlife.api.builder.RoleBuilder;
import com.vlife.api.controller.base.BaseCrudController;
import com.vlife.shared.api.dto.ApiResponse;
import com.vlife.shared.jdbc.dao.salary.RoleDao;
import com.vlife.shared.jdbc.entity.salary.Role;
import io.micronaut.data.model.Page;
import io.micronaut.data.model.Pageable;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.*;
import io.micronaut.serde.annotation.Serdeable;
import jakarta.inject.Inject;

import java.time.LocalDateTime;
import java.util.Map;

import static com.vlife.shared.util.NumberUtil.parseInt;
import static com.vlife.shared.util.StringUtil.isBlank;
import static com.vlife.shared.util.StringUtil.trim;

@Controller("/roles")
public class RoleController extends BaseCrudController<Role, Integer, RoleDao> {

    @Inject
    public RoleController(RoleDao dao, RoleBuilder builder) {
        super(dao, builder);
    }

    @Override
    protected Page<Role> doSearch(Map<String, String> filters, Pageable pageable) {
        return dao.search(
                trim(filters.get("keyword")),
                trim(filters.get("type")),
                parseInt(filters.get("status")),
                pageable
        );
    }

    @Post
    public HttpResponse<?> create(@Body RoleCreateRequest req) {
        return handleCreate(req, r -> {
            Role x = new Role();
            x.setCode(trim(r.getCode()));
            x.setName(trim(r.getName()));
            x.setType(trim(r.getType()));
            x.setDescription(trim(r.getDescription()));
            x.setStatus(r.getStatus() != null ? r.getStatus() : 1);
            x.setCreatedAt(LocalDateTime.now());
            x.setUpdatedAt(LocalDateTime.now());
            return x;
        });
    }

    @Put("/{id}")
    public HttpResponse<?> update(@PathVariable Integer id, @Body RoleUpdateRequest req) {
        return handleUpdate(id, req, r -> {
            Role x = new Role();
            x.setCode(trim(r.getCode()));
            x.setName(trim(r.getName()));
            x.setType(trim(r.getType()));
            x.setDescription(trim(r.getDescription()));
            x.setStatus(r.getStatus());
            x.setUpdatedAt(LocalDateTime.now());
            mergeNullFromDb(id, x, "id", "createdAt");
            return x;
        });
    }

    @Override
    protected <REQ> ApiResponse<?> validateBeforeCreate(Role entity, REQ req) {
        if (isBlank(entity.getCode())) return ApiResponse.error(-400, "code is required");
        if (isBlank(entity.getName())) return ApiResponse.error(-400, "name is required");
        if (isBlank(entity.getType())) return ApiResponse.error(-400, "type is required");
        if (dao.findByCode(entity.getCode()).isPresent()) return ApiResponse.error(-400, "code already exists");
        return null;
    }

    @Override
    protected <REQ> ApiResponse<?> validateBeforeUpdate(Integer id, Role entity, REQ req) {
        if (isBlank(entity.getCode())) return ApiResponse.error(-400, "code is required");
        if (isBlank(entity.getName())) return ApiResponse.error(-400, "name is required");
        if (isBlank(entity.getType())) return ApiResponse.error(-400, "type is required");

        var oldOpt = dao.findById(id);
        if (oldOpt.isEmpty()) return ApiResponse.error(-404, "not found");

        var old = oldOpt.get();
        if (!entity.getCode().equals(old.getCode()) && dao.findByCode(entity.getCode()).isPresent()) {
            return ApiResponse.error(-400, "code already exists");
        }
        return null;
    }

    @Serdeable
    public static class RoleCreateRequest {
        private String code;
        private String name;
        private String type;
        private String description;
        private Integer status;
        public String getCode() { return code; }
        public void setCode(String code) { this.code = code; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public Integer getStatus() { return status; }
        public void setStatus(Integer status) { this.status = status; }
    }

    @Serdeable
    public static class RoleUpdateRequest extends RoleCreateRequest {
    }
}