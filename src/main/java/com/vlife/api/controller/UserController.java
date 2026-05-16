package com.vlife.api.controller;

import com.vlife.api.builder.UserBuilder;
import com.vlife.api.controller.base.BaseCrudController;
import com.vlife.api.util.ApiUtil;
import com.vlife.shared.api.dto.ApiResponse;
import com.vlife.shared.jdbc.dao.UserDao;
import com.vlife.shared.jdbc.dao.auth.UserRoleDao;
import com.vlife.shared.jdbc.entity.User;
import com.vlife.shared.util.CommonUtil;
import io.micronaut.data.model.Page;
import io.micronaut.data.model.Pageable;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.PathVariable;
import io.micronaut.http.annotation.Post;
import io.micronaut.http.annotation.Put;
import io.micronaut.serde.annotation.Serdeable;
import jakarta.inject.Inject;
import org.mindrot.jbcrypt.BCrypt;

import java.time.LocalDateTime;
import java.util.Map;

@Controller("/users")
public class UserController extends BaseCrudController<User, Long, UserDao> {

    private final UserRoleDao userRoleDao;

    @Inject
    public UserController(UserDao dao, UserBuilder builder, UserRoleDao userRoleDao) {
        super(dao, builder);
        this.userRoleDao = userRoleDao;
    }

    @Override
    protected void beforeDelete(Long id) {
        // Dọn user_roles trước khi xoá user
        userRoleDao.deleteByUserId(id.intValue());
    }

    @Override
    protected Page<User> doSearch(Map<String, String> filters, Pageable pageable) {
        String email = ApiUtil.trim(filters.get("email"));
        String name = ApiUtil.trim(filters.get("name"));
        Integer status = ApiUtil.parseInteger(filters.get("status"));

        return dao.search(email, status,  pageable);
    }

    @Post
    public HttpResponse<?> create(@Body UserCreateRequest req) {
        return handleCreate(req, r -> {
            User u = new User();
            u.setEmail(ApiUtil.trim(r.getEmail()));
            u.setPassword(hashPassword(ApiUtil.trim(r.getPassword())));
            u.setName(ApiUtil.trim(r.getName()));
            u.setStatus(r.getStatus() != null ? r.getStatus() : 1);

            LocalDateTime now = LocalDateTime.now();
            u.setCreatedAt(now);
            u.setUpdatedAt(now);
            return u;
        });
    }

    @Put("/{id}")
    public HttpResponse<?> update(@PathVariable Long id, @Body UserUpdateRequest req) {
        return handleUpdate(id, req, r -> {
            User u = new User();
            u.setEmail(ApiUtil.trim(r.getEmail()));
            u.setName(ApiUtil.trim(r.getName()));
            u.setStatus(r.getStatus());
            u.setUpdatedAt(LocalDateTime.now());

            mergeNullFromDb(id, u, "id", "createdAt", "password");

            String rawPassword = ApiUtil.trim(r.getPassword());
            if (!CommonUtil.isNullOrEmpty(rawPassword)) {
                u.setPassword(hashPassword(rawPassword));
            }

            return u;
        });
    }

    @Override
    protected <REQ> ApiResponse<?> validateBeforeCreate(User entity, REQ req) {
        if (CommonUtil.isNullOrEmpty(entity.getEmail())) {
            return ApiResponse.error(-400, "email is required");
        }
        if (CommonUtil.isNullOrEmpty(entity.getName())) {
            return ApiResponse.error(-400, "name is required");
        }

        String rawPassword = req instanceof UserCreateRequest r ? ApiUtil.trim(r.getPassword()) : null;
        if (CommonUtil.isNullOrEmpty(rawPassword)) {
            return ApiResponse.error(-400, "password is required");
        }

        if (dao.existsByEmail(entity.getEmail())) {
            return ApiResponse.error(-400, "email already exists");
        }

        return null;
    }

    @Override
    protected <REQ> ApiResponse<?> validateBeforeUpdate(Long id, User entity, REQ req) {
        if (CommonUtil.isNullOrEmpty(entity.getEmail())) {
            return ApiResponse.error(-400, "email is required");
        }
        if (CommonUtil.isNullOrEmpty(entity.getName())) {
            return ApiResponse.error(-400, "name is required");
        }

        var oldOpt = dao.findById(id);
        if (oldOpt.isEmpty()) {
            return ApiResponse.error(-404, "not found");
        }

        var old = oldOpt.get();
        if (!entity.getEmail().equals(old.getEmail()) && dao.existsByEmail(entity.getEmail())) {
            return ApiResponse.error(-400, "email already exists");
        }

        return null;
    }

    private String hashPassword(String rawPassword) {
        if (CommonUtil.isNullOrEmpty(rawPassword)) {
            return null;
        }
        return BCrypt.hashpw(rawPassword, BCrypt.gensalt());
    }

    // =========================
    // DTO
    // =========================

    @Serdeable
    public static class UserCreateRequest {
        private String email;
        private String password;
        private String name;
        private Integer status;

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public Integer getStatus() {
            return status;
        }

        public void setStatus(Integer status) {
            this.status = status;
        }
    }

    @Serdeable
    public static class UserUpdateRequest {
        private String email;
        private String password;
        private String name;
        private Integer status;

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public Integer getStatus() {
            return status;
        }

        public void setStatus(Integer status) {
            this.status = status;
        }
    }
}