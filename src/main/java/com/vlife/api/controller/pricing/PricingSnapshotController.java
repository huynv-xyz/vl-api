package com.vlife.api.controller.pricing;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.vlife.api.builder.pricing.PricingSnapshotBuilder;
import com.vlife.api.controller.base.BaseCrudController;
import com.vlife.shared.api.dto.ApiResponse;
import com.vlife.shared.jdbc.dao.pricing.PricingSnapshotDao;
import com.vlife.shared.jdbc.entity.pricing.PricingSnapshot;
import com.vlife.shared.service.pricing.PricingService;
import io.micronaut.data.model.Page;
import io.micronaut.data.model.Pageable;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.*;
import io.micronaut.serde.annotation.Serdeable;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

import static com.vlife.shared.util.StringUtil.isBlank;
import static com.vlife.shared.util.StringUtil.trim;

@Controller("/pricing/snapshots")
public class PricingSnapshotController extends BaseCrudController<PricingSnapshot, Integer, PricingSnapshotDao> {
    private final PricingService pricingService;

    public PricingSnapshotController(PricingSnapshotDao dao,
                                     PricingSnapshotBuilder builder,
                                     PricingService pricingService) {
        super(dao, builder);
        this.pricingService = pricingService;
    }

    @Override
    protected Page<PricingSnapshot> doSearch(Map<String, String> filters, Pageable pageable) {
        return dao.search(
                trim(filters.get("keyword")),
                trim(filters.get("pricing_month")),
                trim(filters.get("region_code")),
                trim(filters.get("status")),
                pageable
        );
    }

    @Post
    public HttpResponse<?> create(@Body PricingSnapshot req) {
        normalize(req);
        return handleCreate(req, x -> x);
    }

    @Put("/{id}")
    public HttpResponse<?> update(@PathVariable Integer id, @Body PricingSnapshot req) {
        normalize(req);
        return handleUpdate(id, req, x -> x);
    }

    @Post("/calculate")
    public HttpResponse<?> calculate(@Body CalculatePricingRequest req) {
        if (req == null) return HttpResponse.ok(ApiResponse.error(-400, "request is required"));
        if (!isBlank(req.code()) && dao.findByCode(req.code()).isPresent()) {
            return HttpResponse.ok(ApiResponse.error(-400, "code already exists"));
        }

        var snapshot = pricingService.calculate(new PricingService.CalculatePricingRequest(
                trim(req.code()),
                req.pricingDate(),
                trim(req.pricingMonth()),
                trim(req.regionCode()),
                req.pricingGroupId(),
                trim(req.priceMethod()),
                trim(req.note())
        ));
        return HttpResponse.ok(ApiResponse.success(buildItemResponse(snapshot)));
    }

    @Get("/{id}/items")
    public HttpResponse<?> items(@PathVariable Integer id) {
        if (!dao.existsById(id)) return HttpResponse.ok(ApiResponse.error(-404, "not found"));
        return HttpResponse.ok(ApiResponse.success(pricingService.findItems(id)));
    }

    @Override
    protected void beforeDelete(Integer id) {
        pricingService.deleteSnapshot(id);
    }

    @Override
    @Delete("/{id}")
    public HttpResponse<?> delete(@PathVariable Integer id) {
        if (!dao.existsById(id)) return HttpResponse.ok(ApiResponse.error(-404, "not found"));
        pricingService.deleteSnapshot(id);
        return HttpResponse.ok(ApiResponse.success("deleted"));
    }

    @Override
    protected <REQ> ApiResponse<?> validateBeforeCreate(PricingSnapshot entity, REQ req) {
        if (isBlank(entity.getCode())) return ApiResponse.error(-400, "code is required");
        if (entity.getPricingDate() == null) return ApiResponse.error(-400, "pricing_date is required");
        if (dao.findByCode(entity.getCode()).isPresent()) return ApiResponse.error(-400, "code already exists");
        return null;
    }

    @Override
    protected <REQ> ApiResponse<?> validateBeforeUpdate(Integer id, PricingSnapshot entity, REQ req) {
        if (isBlank(entity.getCode())) return ApiResponse.error(-400, "code is required");
        if (entity.getPricingDate() == null) return ApiResponse.error(-400, "pricing_date is required");
        var old = dao.findById(id);
        if (old.isEmpty()) return ApiResponse.error(-404, "not found");
        if (!entity.getCode().equals(old.get().getCode()) && dao.findByCode(entity.getCode()).isPresent()) {
            return ApiResponse.error(-400, "code already exists");
        }
        return null;
    }

    private void normalize(PricingSnapshot x) {
        LocalDateTime now = LocalDateTime.now();
        if (x.getPricingDate() == null) x.setPricingDate(LocalDate.now());
        if (isBlank(x.getPricingMonth())) x.setPricingMonth(x.getPricingDate().toString().substring(0, 7));
        if (isBlank(x.getRegionCode())) x.setRegionCode("DEFAULT");
        if (isBlank(x.getPriceMethod())) x.setPriceMethod(PricingService.METHOD_LATEST);
        if (isBlank(x.getStatus())) x.setStatus("DRAFT");
        if (x.getCreatedAt() == null) x.setCreatedAt(now);
        x.setUpdatedAt(now);
    }

    @Serdeable
    public record CalculatePricingRequest(
            String code,
            @JsonProperty("pricing_date")
            @JsonFormat(pattern = "yyyy-MM-dd")
            LocalDate pricingDate,
            @JsonProperty("pricing_month")
            String pricingMonth,
            @JsonProperty("region_code")
            String regionCode,
            @JsonProperty("pricing_group_id")
            Integer pricingGroupId,
            @JsonProperty("price_method")
            String priceMethod,
            String note
    ) {}
}
