package com.vlife.api.controller.auth;

import com.vlife.api.controller.base.BaseController;
import com.vlife.api.util.ApiUtil;
import com.vlife.shared.api.dto.ApiResponse;
import com.vlife.shared.jdbc.dao.auth.PermissionDao;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.*;
import io.micronaut.security.authentication.Authentication;
import jakarta.inject.Inject;


@Controller("/auth")
public class PermissionController extends BaseController {

    private final PermissionDao permissionDao;

    @Inject
    public PermissionController(PermissionDao permissionDao) {
        this.permissionDao = permissionDao;
    }

    @Get("/me/permissions")
    public HttpResponse<?> getMyPermissions(Authentication authentication) {

        Long userId = ApiUtil.parseLong(authentication.getName());

        if (userId == null) {
            return HttpResponse.badRequest(ApiResponse.error(-400, "invalid auth"));
        }

        var data = permissionDao.findByUserId(userId.intValue());

        return HttpResponse.ok(ApiResponse.success(data));
    }
}