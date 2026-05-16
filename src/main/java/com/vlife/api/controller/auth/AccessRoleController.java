package com.vlife.api.controller.auth;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.vlife.api.builder.AccessRoleBuilder;
import com.vlife.api.controller.base.BaseCrudController;
import com.vlife.api.util.ApiUtil;
import com.vlife.shared.api.dto.ApiResponse;
import com.vlife.shared.jdbc.dao.auth.PermissionDao;
import com.vlife.shared.jdbc.dao.auth.RoleDao;
import com.vlife.shared.jdbc.dao.auth.RolePermissionDao;
import com.vlife.shared.jdbc.dao.auth.UserRoleDao;
import com.vlife.shared.jdbc.entity.auth.Role;
import com.vlife.shared.util.CommonUtil;
import io.micronaut.data.model.Page;
import io.micronaut.data.model.Pageable;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.*;
import io.micronaut.serde.annotation.Serdeable;
import io.micronaut.serde.config.naming.SnakeCaseStrategy;
import io.micronaut.transaction.annotation.Transactional;
import jakarta.inject.Inject;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

@Controller("/auth/roles")
public class AccessRoleController extends BaseCrudController<Role, Integer, RoleDao> {

    private final RolePermissionDao rolePermissionDao;
    private final PermissionDao permissionDao;
    private final UserRoleDao userRoleDao;

    @Inject
    public AccessRoleController(RoleDao dao,
                                AccessRoleBuilder builder,
                                RolePermissionDao rolePermissionDao,
                                PermissionDao permissionDao,
                                UserRoleDao userRoleDao) {
        super(dao, builder);
        this.rolePermissionDao = rolePermissionDao;
        this.permissionDao = permissionDao;
        this.userRoleDao = userRoleDao;
    }

    @Override
    protected void beforeDelete(Integer id) {
        // Dọn bảng nối trước khi xoá role để tránh dữ liệu mồ côi
        rolePermissionDao.deleteByRoleId(id);
        userRoleDao.deleteByRoleId(id);
    }

    @Override
    protected Page<Role> doSearch(Map<String, String> filters, Pageable pageable) {
        String keyword = ApiUtil.trim(filters.get("keyword"));
        return dao.search(keyword, pageable);
    }

    @Post
    public HttpResponse<?> create(@Body RoleCreateRequest req) {
        return handleCreate(req, r -> {
            Role x = new Role();
            x.setCode(ApiUtil.trim(r.getCode()));
            x.setName(ApiUtil.trim(r.getName()));
            LocalDateTime now = LocalDateTime.now();
            x.setCreatedAt(now);
            x.setUpdatedAt(now);
            return x;
        });
    }

    @Put("/{id}")
    public HttpResponse<?> update(@PathVariable Integer id, @Body RoleUpdateRequest req) {
        return handleUpdate(id, req, r -> {
            Role x = new Role();
            x.setCode(ApiUtil.trim(r.getCode()));
            x.setName(ApiUtil.trim(r.getName()));
            x.setUpdatedAt(LocalDateTime.now());
            mergeNullFromDb(id, x, "id", "createdAt");
            return x;
        });
    }

    @Override
    protected <REQ> ApiResponse<?> validateBeforeCreate(Role entity, REQ req) {
        if (CommonUtil.isNullOrEmpty(entity.getCode())) {
            return ApiResponse.error(-400, "code is required");
        }
        if (CommonUtil.isNullOrEmpty(entity.getName())) {
            return ApiResponse.error(-400, "name is required");
        }
        if (dao.existsByCode(entity.getCode())) {
            return ApiResponse.error(-400, "code already exists");
        }
        return null;
    }

    @Override
    protected <REQ> ApiResponse<?> validateBeforeUpdate(Integer id, Role entity, REQ req) {
        if (CommonUtil.isNullOrEmpty(entity.getCode())) {
            return ApiResponse.error(-400, "code is required");
        }
        if (CommonUtil.isNullOrEmpty(entity.getName())) {
            return ApiResponse.error(-400, "name is required");
        }
        var oldOpt = dao.findById(id);
        if (oldOpt.isEmpty()) {
            return ApiResponse.error(-404, "not found");
        }
        var old = oldOpt.get();
        if (!entity.getCode().equals(old.getCode()) && dao.existsByCode(entity.getCode())) {
            return ApiResponse.error(-400, "code already exists");
        }
        return null;
    }

    // =========================
    // Role <-> Permission
    // =========================

    @Get("/{id}/permissions")
    public HttpResponse<?> getRolePermissions(@PathVariable Integer id) {
        if (!dao.existsById(id)) {
            return HttpResponse.ok(ApiResponse.error(-404, "role not found"));
        }
        List<Integer> permissionIds = rolePermissionDao.findPermissionIdsByRoleId(id);
        return HttpResponse.ok(ApiResponse.success(Map.of(
                "role_id", id,
                "permission_ids", permissionIds
        )));
    }

    @Put("/{id}/permissions")
    @Transactional
    public HttpResponse<?> updateRolePermissions(@PathVariable Integer id,
                                                 @Body RolePermissionRequest req) {
        if (!dao.existsById(id)) {
            return HttpResponse.ok(ApiResponse.error(-404, "role not found"));
        }

        if (req == null || req.getPermissionIds() == null) {
            return HttpResponse.ok(ApiResponse.error(-400, "permission_ids is required"));
        }

        List<Integer> permissionIds = new LinkedHashSet<>(req.getPermissionIds()).stream().toList();

        // validate permission ids tồn tại
        for (Integer pid : permissionIds) {
            if (pid == null || permissionDao.findById(pid).isEmpty()) {
                return HttpResponse.ok(ApiResponse.error(-400, "invalid permission_id: " + pid));
            }
        }

        rolePermissionDao.deleteByRoleId(id);
        for (Integer pid : permissionIds) {
            rolePermissionDao.insert(id, pid);
        }

        return HttpResponse.ok(ApiResponse.success(Map.of(
                "role_id", id,
                "permission_ids", permissionIds
        )));
    }

    // =========================
    // DTO
    // =========================

    @Serdeable
    public static class RoleCreateRequest {
        private String code;
        private String name;

        public String getCode() { return code; }
        public void setCode(String code) { this.code = code; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
    }

    @Serdeable
    public static class RoleUpdateRequest extends RoleCreateRequest {
    }

    @Serdeable(naming = SnakeCaseStrategy.class)
    public static class RolePermissionRequest {
        @JsonProperty("permission_ids")
        private List<Integer> permissionIds;

        public List<Integer> getPermissionIds() { return permissionIds; }
        @JsonProperty("permission_ids")
        public void setPermissionIds(List<Integer> permissionIds) { this.permissionIds = permissionIds; }
    }
}
