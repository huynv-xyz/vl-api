package com.vlife.api.controller.auth;

import com.vlife.api.builder.PermissionBuilder;
import com.vlife.api.controller.base.BaseCrudController;
import com.vlife.api.util.ApiUtil;
import com.vlife.shared.api.dto.ApiResponse;
import com.vlife.shared.jdbc.dao.auth.PermissionDao;
import com.vlife.shared.jdbc.dao.auth.RolePermissionDao;
import com.vlife.shared.jdbc.entity.auth.Permission;
import com.vlife.shared.util.CommonUtil;
import io.micronaut.data.model.Page;
import io.micronaut.data.model.Pageable;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.*;
import io.micronaut.serde.annotation.Serdeable;
import jakarta.inject.Inject;

import java.util.Map;

@Controller("/auth/permissions")
public class PermissionCrudController extends BaseCrudController<Permission, Integer, PermissionDao> {

    private final RolePermissionDao rolePermissionDao;

    @Inject
    public PermissionCrudController(PermissionDao dao,
                                    PermissionBuilder builder,
                                    RolePermissionDao rolePermissionDao) {
        super(dao, builder);
        this.rolePermissionDao = rolePermissionDao;
    }

    @Override
    protected void beforeDelete(Integer id) {
        // Dọn liên kết trong role_permissions để không bị mồ côi
        rolePermissionDao.deleteByPermissionId(id);
    }

    @Override
    protected Page<Permission> doSearch(Map<String, String> filters, Pageable pageable) {
        String module = ApiUtil.trim(filters.get("module"));
        String action = ApiUtil.trim(filters.get("action"));
        return dao.search(module, action, pageable);
    }

    @Post
    public HttpResponse<?> create(@Body PermissionCreateRequest req) {
        return handleCreate(req, r -> {
            Permission p = new Permission();
            p.setModule(ApiUtil.trim(r.getModule()));
            p.setAction(ApiUtil.trim(r.getAction()));
            p.setName(ApiUtil.trim(r.getName()));
            return p;
        });
    }

    @Put("/{id}")
    public HttpResponse<?> update(@PathVariable Integer id, @Body PermissionUpdateRequest req) {
        return handleUpdate(id, req, r -> {
            Permission p = new Permission();
            p.setModule(ApiUtil.trim(r.getModule()));
            p.setAction(ApiUtil.trim(r.getAction()));
            p.setName(ApiUtil.trim(r.getName()));
            mergeNullFromDb(id, p, "id");
            return p;
        });
    }

    @Override
    protected <REQ> ApiResponse<?> validateBeforeCreate(Permission entity, REQ req) {
        if (CommonUtil.isNullOrEmpty(entity.getModule())) {
            return ApiResponse.error(-400, "module is required");
        }
        if (CommonUtil.isNullOrEmpty(entity.getAction())) {
            return ApiResponse.error(-400, "action is required");
        }
        if (dao.existsByModuleAction(entity.getModule(), entity.getAction(), null)) {
            return ApiResponse.error(-400, "module + action already exists");
        }
        return null;
    }

    @Override
    protected <REQ> ApiResponse<?> validateBeforeUpdate(Integer id, Permission entity, REQ req) {
        if (CommonUtil.isNullOrEmpty(entity.getModule())) {
            return ApiResponse.error(-400, "module is required");
        }
        if (CommonUtil.isNullOrEmpty(entity.getAction())) {
            return ApiResponse.error(-400, "action is required");
        }
        if (!dao.existsById(id)) {
            return ApiResponse.error(-404, "not found");
        }
        if (dao.existsByModuleAction(entity.getModule(), entity.getAction(), id)) {
            return ApiResponse.error(-400, "module + action already exists");
        }
        return null;
    }

    @Serdeable
    public static class PermissionCreateRequest {
        private String module;
        private String action;
        private String name;

        public String getModule() { return module; }
        public void setModule(String module) { this.module = module; }
        public String getAction() { return action; }
        public void setAction(String action) { this.action = action; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
    }

    @Serdeable
    public static class PermissionUpdateRequest extends PermissionCreateRequest {
    }
}
