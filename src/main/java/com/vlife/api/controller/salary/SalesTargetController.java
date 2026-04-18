package com.vlife.api.controller.salary;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.vlife.api.builder.SalesTargetBuilder;
import com.vlife.api.controller.base.BaseCrudController;
import com.vlife.shared.api.dto.ApiResponse;
import com.vlife.shared.jdbc.dao.salary.SalesTargetDao;
import com.vlife.shared.jdbc.entity.salary.SalesTarget;
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

import java.time.LocalDateTime;
import java.util.Map;

import static com.vlife.shared.util.NumberUtil.parseInt;

@Controller("/salary/sales-targets")
public class SalesTargetController extends BaseCrudController<SalesTarget, Integer, SalesTargetDao> {

    @Inject
    public SalesTargetController(SalesTargetDao dao, SalesTargetBuilder builder) {
        super(dao, builder);
    }

    @Override
    protected Page<SalesTarget> doSearch(Map<String, String> filters, Pageable pageable) {
        return dao.search(
                parseInt(filters.get("period")),
                parseInt(filters.get("employee_id")),
                pageable
        );
    }

    @Post
    public HttpResponse<?> create(@Body SalesTargetCreateRequest req) {
        return handleCreate(req, r -> {
            SalesTarget x = new SalesTarget();
            x.setPeriod(parseInt(r.getPeriod()));
            x.setEmployeeId(r.getEmployeeId());
            x.setBonGoc(r.getBonGoc() != null ? r.getBonGoc() : 0D);
            x.setBonLaBot(r.getBonLaBot() != null ? r.getBonLaBot() : 0D);
            x.setClcn(r.getClcn() != null ? r.getClcn() : 0D);
            x.setBonLaLong(r.getBonLaLong() != null ? r.getBonLaLong() : 0D);
            x.setCreatedAt(LocalDateTime.now());
            x.setUpdatedAt(LocalDateTime.now());
            return x;
        });
    }

    @Put("/{id}")
    public HttpResponse<?> update(@PathVariable Integer id, @Body SalesTargetUpdateRequest req) {
        return handleUpdate(id, req, r -> {
            SalesTarget x = new SalesTarget();
            x.setPeriod(parseInt(r.getPeriod()));
            x.setEmployeeId(r.getEmployeeId());
            x.setBonGoc(r.getBonGoc());
            x.setBonLaBot(r.getBonLaBot());
            x.setClcn(r.getClcn());
            x.setBonLaLong(r.getBonLaLong());
            x.setUpdatedAt(LocalDateTime.now());
            mergeNullFromDb(id, x, "id", "createdAt");
            return x;
        });
    }

    @Override
    protected <REQ> ApiResponse<?> validateBeforeCreate(SalesTarget entity, REQ req) {
        if (entity.getPeriod() == null) {
            return ApiResponse.error(-400, "period is required");
        }
        if (entity.getEmployeeId() == null) {
            return ApiResponse.error(-400, "employee_id is required");
        }
        return null;
    }

    @Override
    protected <REQ> ApiResponse<?> validateBeforeUpdate(Integer id, SalesTarget entity, REQ req) {
        if (entity.getPeriod() == null) {
            return ApiResponse.error(-400, "period is required");
        }
        if (entity.getEmployeeId() == null) {
            return ApiResponse.error(-400, "employee_id is required");
        }

        var oldOpt = dao.findById(id);
        if (oldOpt.isEmpty()) {
            return ApiResponse.error(-404, "not found");
        }
        return null;
    }

    @Serdeable
    public static class SalesTargetCreateRequest {
        private String period;

        @JsonProperty("employee_id")
        private Integer employeeId;

        @JsonProperty("bon_goc")
        private Double bonGoc;

        @JsonProperty("bon_la_bot")
        private Double bonLaBot;

        private Double clcn;

        @JsonProperty("bon_la_long")
        private Double bonLaLong;

        public String getPeriod() {
            return period;
        }

        public void setPeriod(String period) {
            this.period = period;
        }

        public Integer getEmployeeId() {
            return employeeId;
        }

        public void setEmployeeId(Integer employeeId) {
            this.employeeId = employeeId;
        }

        public Double getBonGoc() {
            return bonGoc;
        }

        public void setBonGoc(Double bonGoc) {
            this.bonGoc = bonGoc;
        }

        public Double getBonLaBot() {
            return bonLaBot;
        }

        public void setBonLaBot(Double bonLaBot) {
            this.bonLaBot = bonLaBot;
        }

        public Double getClcn() {
            return clcn;
        }

        public void setClcn(Double clcn) {
            this.clcn = clcn;
        }

        public Double getBonLaLong() {
            return bonLaLong;
        }

        public void setBonLaLong(Double bonLaLong) {
            this.bonLaLong = bonLaLong;
        }
    }

    @Serdeable
    public static class SalesTargetUpdateRequest extends SalesTargetCreateRequest {
    }
}