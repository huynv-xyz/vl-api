package com.vlife.api.controller.auth;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.vlife.api.controller.base.BaseController;
import com.vlife.shared.api.dto.ApiResponse;
import com.vlife.shared.jdbc.dao.UserDao;
import com.vlife.shared.jdbc.dao.auth.RoleDao;
import com.vlife.shared.jdbc.dao.auth.UserRoleDao;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.*;
import io.micronaut.serde.annotation.Serdeable;
import io.micronaut.serde.config.naming.SnakeCaseStrategy;
import io.micronaut.transaction.annotation.Transactional;
import jakarta.inject.Inject;

import java.util.List;
import java.util.Map;

@Controller("/users/{userId}/roles")
public class UserRoleController extends BaseController {

    private final UserDao userDao;
    private final RoleDao roleDao;
    private final UserRoleDao userRoleDao;

    @Inject
    public UserRoleController(UserDao userDao,
                              RoleDao roleDao,
                              UserRoleDao userRoleDao) {
        this.userDao = userDao;
        this.roleDao = roleDao;
        this.userRoleDao = userRoleDao;
    }

    @Get
    public HttpResponse<?> getUserRoles(@PathVariable Long userId) {
        if (userDao.findById(userId).isEmpty()) {
            return HttpResponse.ok(ApiResponse.error(-404, "user not found"));
        }
        List<Integer> roleIds = userRoleDao.findRoleIdsByUserId(userId.intValue());
        return HttpResponse.ok(ApiResponse.success(Map.of(
                "user_id", userId,
                "role_ids", roleIds
        )));
    }

    @Put
    @Transactional
    public HttpResponse<?> updateUserRoles(@PathVariable Long userId,
                                           @Body UserRoleRequest req) {
        if (userDao.findById(userId).isEmpty()) {
            return HttpResponse.ok(ApiResponse.error(-404, "user not found"));
        }

        if (req == null || req.getRoleIds() == null) {
            return HttpResponse.ok(ApiResponse.error(-400, "role_ids is required"));
        }

        List<Integer> roleIds = req.getRoleIds();

        for (Integer rid : roleIds) {
            if (rid == null || roleDao.findById(rid).isEmpty()) {
                return HttpResponse.ok(ApiResponse.error(-400, "invalid role_id: " + rid));
            }
        }

        userRoleDao.deleteByUserId(userId.intValue());
        for (Integer rid : roleIds) {
            userRoleDao.insert(userId.intValue(), rid);
        }

        return HttpResponse.ok(ApiResponse.success(Map.of(
                "user_id", userId,
                "role_ids", roleIds
        )));
    }

    @Serdeable(naming = SnakeCaseStrategy.class)
    public static class UserRoleRequest {
        @JsonProperty("role_ids")
        private List<Integer> roleIds;

        public List<Integer> getRoleIds() { return roleIds; }
        @JsonProperty("role_ids")
        public void setRoleIds(List<Integer> roleIds) { this.roleIds = roleIds; }
    }
}
