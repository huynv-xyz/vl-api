package com.vlife.api.controller.base;

import com.vlife.shared.api.builder.ItemBuilder;
import com.vlife.shared.api.dto.ApiResponse;
import com.vlife.shared.jdbc.dao.base.BaseDao;
import com.vlife.shared.jdbc.entity.base.Identifiable;
import io.micronaut.core.beans.BeanWrapper;
import io.micronaut.data.model.Page;
import io.micronaut.data.model.Pageable;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.*;
import io.micronaut.http.context.ServerRequestContext;
import io.micronaut.scheduling.TaskExecutors;
import io.micronaut.scheduling.annotation.ExecuteOn;
import io.micronaut.security.annotation.Secured;
import io.micronaut.security.rules.SecurityRule;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

public abstract class BaseCrudController<
        E extends Identifiable<ID>,
        ID extends Serializable,
        D extends BaseDao<E, ID>
        > extends BaseController {

    protected final D dao;
    protected final ItemBuilder<E> builder;

    private static final int DEFAULT_LIMIT = 20;
    private static final int MAX_LIMIT = 200;
    private static final int MAX_PAGE = 10_000;

    protected BaseCrudController(D dao, ItemBuilder<E> builder) {
        this.dao = dao;
        this.builder = builder;
    }

    protected String getUser() {
        return ServerRequestContext.currentRequest()
                .flatMap(req -> req.getAttribute("currentUser", String.class))
                .orElse("");
    }

    protected Object buildItemResponse(E entity) {
        return builder != null ? builder.buildItemFull(entity) : entity;
    }

    protected Map<String, Object> buildPagedResponse(Page<E> page) {
        int currentPage = page.getPageNumber() + 1;

        return Map.of(
                "page", (page.getPageNumber() + 1),
                "size", page.getSize(),
                "total", page.getTotalSize(),
                "current_page", currentPage,
                "total_page", page.getTotalPages(),
                "items", builder != null ? builder.buildList(page.getContent()) : page.getContent()
        );
    }

    // ======================
    // READ
    // ======================

    @Get("/{id}")
    public HttpResponse<?> get(@PathVariable ID id) {
        Optional<E> opt = dao.findById(id);
        if (opt.isEmpty()) return HttpResponse.ok(ApiResponse.error(-404, "not found"));
        return HttpResponse.ok(ApiResponse.success(buildItemResponse(opt.get())));
    }

    @Get
    public HttpResponse<?> list(
            HttpRequest<?> request,
            @QueryValue(defaultValue = "1") int page,
            @QueryValue(defaultValue = "20") int limit
    ) {
        int safePage = Math.min(Math.max(page, 1), MAX_PAGE);
        int safeLimit = Math.min(limit, MAX_LIMIT);

        Pageable pageable = Pageable.from(safePage - 1, safeLimit);

        Map<String, String> filters = request.getParameters().asMap()
                .entrySet()
                .stream()
                .filter(e -> !e.getKey().equals("page") && !e.getKey().equals("limit"))
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> {
                            List<String> values = e.getValue();
                            return (values != null && !values.isEmpty()) ? values.get(0) : null;
                        }
                ));

        Page<E> result = doSearch(filters, pageable);
        return HttpResponse.ok(ApiResponse.success(buildPagedResponse(result)));
    }

    protected Page<E> doSearch(Map<String, String> filters, Pageable pageable) {
        return dao.findAll(pageable);
    }

    // ======================
    // WRITE (DTO-agnostic helpers)
    // Subclass tự declare @Post/@Put với DTO bất kỳ rồi gọi các hàm này
    // ======================

    protected <REQ> HttpResponse<?> handleCreate(REQ req, Function<REQ, E> toEntity) {
        E entity = toEntity.apply(req);

        beforeCreate(entity, req);
        ApiResponse<?> err = validateBeforeCreate(entity, req);
        if (err != null) return HttpResponse.ok(err);

        E saved = dao.insert(entity);
        afterCreate(saved, req);

        return HttpResponse.ok(ApiResponse.success(buildItemResponse(saved)));
    }

    protected <REQ> HttpResponse<?> handleUpdate(ID id, REQ req, Function<REQ, E> toEntity) {
        if (!dao.existsById(id)) return HttpResponse.ok(ApiResponse.error(-404, "not found"));

        E entity = toEntity.apply(req);
        entity.setId(id);

        beforeUpdate(id, entity, req);

        ApiResponse<?> err = validateBeforeUpdate(id, entity, req);
        if (err != null) return HttpResponse.ok(err);

        dao.updateSelective(id, entity);
        afterUpdate(id, entity, req);

        return HttpResponse.ok(ApiResponse.success(buildItemResponse(entity)));
    }

    // ======================
    // DELETE
    // ======================

    @Delete("/{id}")
    public HttpResponse<?> delete(@PathVariable ID id) {
        beforeDelete(id);

        if (!dao.existsById(id)) return HttpResponse.ok(ApiResponse.error(-404, "not found"));

        dao.deleteById(id);

        afterDelete(id);
        return HttpResponse.ok(ApiResponse.success("deleted"));
    }

    // ======================
    // Hooks (entity + req bất kỳ)
    // ======================

    protected <REQ> void beforeCreate(E entity, REQ req) {}
    protected <REQ> void afterCreate(E saved, REQ req) {}
    protected <REQ> ApiResponse<?> validateBeforeCreate(E entity, REQ req) { return null; }

    protected <REQ> void beforeUpdate(ID id, E entity, REQ req) {}
    protected <REQ> void afterUpdate(ID id, E entity, REQ req) {}
    protected <REQ> ApiResponse<?> validateBeforeUpdate(ID id, E entity, REQ req) { return null; }

    protected void beforeDelete(ID id) {}
    protected void afterDelete(ID id) {}

    // ======================
    // Generic merge helpers
    // ======================

    protected Optional<E> mergeNullFromDb(ID id, E target, String... ignoreProps) {
        Optional<E> optOld = dao.findById(id);
        optOld.ifPresent(old -> mergeNullProperties(old, target, ignoreProps));
        return optOld;
    }

    private void mergeNullProperties(E source, E target, String... ignoreProps) {
        BeanWrapper<E> src = BeanWrapper.getWrapper(source);
        BeanWrapper<E> dst = BeanWrapper.getWrapper(target);

        Set<String> ignore = Set.of(ignoreProps);

        for (String prop : dst.getPropertyNames()) {
            if (ignore.contains(prop)) continue;

            Object newVal = dst.getProperty(prop, Object.class).orElse(null);
            if (newVal == null) {
                Object oldVal = src.getProperty(prop, Object.class).orElse(null);
                if (oldVal != null) dst.setProperty(prop, oldVal);
            }
        }
    }
}