package com.vlife.api.controller.purchasing;

import com.vlife.api.builder.purchasing.PortBuilder;
import com.vlife.api.controller.base.BaseCrudController;
import com.vlife.shared.api.dto.ApiResponse;
import com.vlife.shared.jdbc.dao.purchasing.PortDao;
import com.vlife.shared.jdbc.entity.purchasing.Port;
import io.micronaut.data.model.Page;
import io.micronaut.data.model.Pageable;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.*;
import io.micronaut.serde.annotation.Serdeable;
import jakarta.inject.Inject;

import java.time.LocalDateTime;
import java.util.Map;

import static com.vlife.shared.util.StringUtil.isBlank;
import static com.vlife.shared.util.StringUtil.trim;

@Controller("/purchasing/ports")
public class PortController extends BaseCrudController<Port, Integer, PortDao> {

    @Inject
    public PortController(PortDao dao, PortBuilder builder) {
        super(dao, builder);
    }

    @Override
    protected Page<Port> doSearch(Map<String, String> filters, Pageable pageable) {
        return dao.search(
                trim(filters.get("keyword")),
                pageable
        );
    }

    @Post
    public HttpResponse<?> create(@Body PortCreateRequest req) {
        return handleCreate(req, r -> {
            Port x = new Port();
            x.setName(trim(r.getName()));
            x.setCreatedAt(LocalDateTime.now());
            x.setUpdatedAt(LocalDateTime.now());
            return x;
        });
    }

    @Put("/{id}")
    public HttpResponse<?> update(@PathVariable Integer id, @Body PortUpdateRequest req) {
        return handleUpdate(id, req, r -> {
            Port x = new Port();
            x.setName(trim(r.getName()));
            x.setUpdatedAt(LocalDateTime.now());
            mergeNullFromDb(id, x, "id", "createdAt");
            return x;
        });
    }

    @Override
    protected <REQ> ApiResponse<?> validateBeforeCreate(Port entity, REQ req) {
        if (isBlank(entity.getName())) return ApiResponse.error(-400, "name is required");

        return null;
    }

    @Override
    protected <REQ> ApiResponse<?> validateBeforeUpdate(Integer id, Port entity, REQ req) {
        if (isBlank(entity.getName())) return ApiResponse.error(-400, "name is required");

        var oldOpt = dao.findById(id);
        if (oldOpt.isEmpty()) return ApiResponse.error(-404, "not found");


        return null;
    }

    @Serdeable
    public static class PortCreateRequest {
        private String name;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }

    @Serdeable
    public static class PortUpdateRequest extends PortCreateRequest {
    }
}