package com.vlife.api.controller.production;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.vlife.api.builder.production.ProductBomBuilder;
import com.vlife.api.controller.base.BaseCrudController;
import com.vlife.api.service.production.VthhBomImportService;
import com.vlife.api.util.ApiUtil;
import com.vlife.shared.api.dto.ApiResponse;
import com.vlife.shared.jdbc.dao.production.ProductBomDao;
import com.vlife.shared.jdbc.dao.production.ProductBomItemDao;
import com.vlife.shared.jdbc.entity.production.ProductBom;
import com.vlife.shared.jdbc.entity.production.ProductBomItem;
import com.vlife.shared.util.CommonUtil;
import io.micronaut.data.model.Page;
import io.micronaut.data.model.Pageable;
import io.micronaut.http.MediaType;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.*;
import io.micronaut.http.multipart.CompletedFileUpload;
import io.micronaut.scheduling.TaskExecutors;
import io.micronaut.scheduling.annotation.ExecuteOn;
import io.micronaut.serde.annotation.Serdeable;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import lombok.Getter;
import lombok.Setter;

import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Controller("/productions/boms")
@ExecuteOn(TaskExecutors.IO)
public class ProductBomController extends BaseCrudController<ProductBom, Integer, ProductBomDao> {

    private final ProductBomItemDao itemDao;
    private final VthhBomImportService vthhBomImportService;

    @Inject
    public ProductBomController(
            ProductBomDao dao,
            ProductBomBuilder builder,
            ProductBomItemDao itemDao,
            VthhBomImportService vthhBomImportService
    ) {
        super(dao, builder);
        this.itemDao = itemDao;
        this.vthhBomImportService = vthhBomImportService;
    }

    @Override
    protected Page<ProductBom> doSearch(Map<String, String> filters, Pageable pageable) {
        return dao.search(
                ApiUtil.parseInteger(filters.get("product_id")),
                ApiUtil.parseBoolean(filters.get("active")),
                pageable
        );
    }

    @Get("/effective")
    public HttpResponse<?> effective(
            @QueryValue("product_id") Integer productId,
            @QueryValue("date") String date
    ) {
        var bom = dao.findEffectiveBom(productId, ApiUtil.toDate(date)).orElse(null);

        if (bom == null) {
            return HttpResponse.notFound(ApiResponse.error(-404, "bom not found"));
        }

        return HttpResponse.ok(ApiResponse.success(builder.buildItemFull(bom)));
    }

    @Post(value = "/import-vthh", consumes = MediaType.MULTIPART_FORM_DATA, produces = MediaType.APPLICATION_JSON)
    @Transactional
    public HttpResponse<?> importVthh(
            @Part("file") CompletedFileUpload file,
            @QueryValue(value = "version", defaultValue = "VTHH-IMPORT-20260515") String version,
            @QueryValue(value = "valid_from", defaultValue = "2026-01-01") String validFrom,
            @QueryValue(value = "replace", defaultValue = "true") Boolean replace
    ) {
        if (file == null || file.getFilename() == null || file.getFilename().isBlank()) {
            return HttpResponse.badRequest(ApiResponse.error(-400, "file is required"));
        }

        String filename = file.getFilename().trim().toLowerCase();
        if (!filename.endsWith(".xlsx")) {
            return HttpResponse.badRequest(ApiResponse.error(-400, "Chỉ hỗ trợ file .xlsx"));
        }

        try (InputStream inputStream = file.getInputStream()) {
            LocalDate from = ApiUtil.toDate(validFrom);
            var result = vthhBomImportService.importExcel(inputStream, version, from, replace);
            return HttpResponse.ok(ApiResponse.success(result));
        } catch (IllegalArgumentException e) {
            return HttpResponse.badRequest(ApiResponse.error(-400, e.getMessage()));
        } catch (Exception e) {
            return HttpResponse.serverError(ApiResponse.error(-500, "Import VTHH thất bại: " + e.getMessage()));
        }
    }

    @Post
    @Transactional
    public HttpResponse<?> create(@Body ProductBomCreateRequest req) {
        return handleCreate(req, r -> {
            ProductBom x = new ProductBom();

            x.setProductId(r.getProductId());
            x.setVersion(!CommonUtil.isNullOrEmpty(r.getVersion()) ? ApiUtil.trim(r.getVersion()) : "V1");
            x.setValidFrom(ApiUtil.toDate(r.getValidFrom()));
            x.setValidTo(!CommonUtil.isNullOrEmpty(r.getValidTo())
                    ? ApiUtil.toDate(r.getValidTo())
                    : java.time.LocalDate.of(2099, 12, 31));

            x.setActive(r.getActive() == null || r.getActive());
            x.setStatus("ACTIVE");
            x.setNote(ApiUtil.trim(r.getNote()));
            x.setCreatedAt(LocalDateTime.now());
            x.setUpdatedAt(LocalDateTime.now());

            return x;
        });
    }

    @Put("/{id}")
    @Transactional
    public HttpResponse<?> update(@PathVariable Integer id, @Body ProductBomUpdateRequest req) {
        return handleUpdate(id, req, r -> {
            ProductBom x = new ProductBom();

            x.setProductId(r.getProductId());
            x.setVersion(ApiUtil.trim(r.getVersion()));
            x.setValidFrom(ApiUtil.toDate(r.getValidFrom()));
            x.setValidTo(ApiUtil.toDate(r.getValidTo()));
            x.setActive(r.getActive());
            x.setNote(ApiUtil.trim(r.getNote()));
            x.setUpdatedAt(LocalDateTime.now());

            mergeNullFromDb(id, x, "id", "createdAt");

            return x;
        });
    }

    @Override
    protected <REQ> void afterCreate(ProductBom entity, REQ req) {
        if (req instanceof ProductBomCreateRequest r) {
            saveItems(entity.getId(), r.getItems());
        }
    }

    @Override
    protected <REQ> void afterUpdate(Integer id, ProductBom entity, REQ req) {
        if (req instanceof ProductBomUpdateRequest r) {
            itemDao.deleteByBomId(id);
            saveItems(id, r.getItems());
        }
    }

    @Delete("/{id}")
    @Transactional
    @Override
    public HttpResponse<?> delete(@PathVariable Integer id) {
        itemDao.deleteByBomId(id);
        return super.delete(id);
    }

    private void saveItems(Integer bomId, List<ProductBomItemRequest> reqs) {
        if (CommonUtil.isNullOrEmpty(reqs)) return;

        LocalDateTime now = LocalDateTime.now();
        List<ProductBomItem> list = new ArrayList<>();

        int line = 1;

        for (ProductBomItemRequest r : reqs) {
            ProductBomItem x = new ProductBomItem();

            x.setBomId(bomId);
            x.setMaterialProductId(r.getMaterialProductId());
            x.setMaterialType(ApiUtil.trim(r.getMaterialType()));
            x.setQuantity(r.getQuantity());
            x.setUnit(ApiUtil.trim(r.getUnit()));
            x.setLineNo(r.getLineNo() != null ? r.getLineNo() : line++);
            x.setNote(ApiUtil.trim(r.getNote()));
            x.setCreatedAt(now);
            x.setUpdatedAt(now);

            list.add(x);
        }

        itemDao.saveAll(list);
    }

    @Override
    protected <REQ> ApiResponse<?> validateBeforeCreate(ProductBom entity, REQ req) {
        return validate(entity, req);
    }

    @Override
    protected <REQ> ApiResponse<?> validateBeforeUpdate(Integer id, ProductBom entity, REQ req) {
        if (dao.findById(id).isEmpty()) {
            return ApiResponse.error(-404, "not found");
        }

        return validate(entity, req);
    }

    private <REQ> ApiResponse<?> validate(ProductBom entity, REQ req) {
        if (entity.getProductId() == null) {
            return ApiResponse.error(-400, "product_id is required");
        }

        if (entity.getValidFrom() == null) {
            return ApiResponse.error(-400, "valid_from is required");
        }

        List<ProductBomItemRequest> items = null;

        if (req instanceof ProductBomCreateRequest r) {
            items = r.getItems();
        }

        if (req instanceof ProductBomUpdateRequest r) {
            items = r.getItems();
        }

        if (CommonUtil.isNullOrEmpty(items)) {
            return ApiResponse.error(-400, "items is required");
        }

        for (ProductBomItemRequest i : items) {
            if (i.getMaterialProductId() == null) {
                return ApiResponse.error(-400, "material_product_id is required");
            }

            if (CommonUtil.isNullOrEmpty(i.getMaterialType())) {
                return ApiResponse.error(-400, "material_type is required");
            }

            if (i.getQuantity() == null || i.getQuantity().compareTo(BigDecimal.ZERO) <= 0) {
                return ApiResponse.error(-400, "quantity must > 0");
            }
        }

        return null;
    }

    @Getter
    @Setter
    @Serdeable
    public static class ProductBomCreateRequest {

        @JsonProperty("product_id")
        private Integer productId;

        private String version;

        @JsonProperty("valid_from")
        private String validFrom;

        @JsonProperty("valid_to")
        private String validTo;

        private Boolean active;

        private String note;

        private List<ProductBomItemRequest> items;
    }

    @Getter
    @Setter
    @Serdeable
    public static class ProductBomUpdateRequest extends ProductBomCreateRequest {
    }

    @Getter
    @Setter
    @Serdeable
    public static class ProductBomItemRequest {

        @JsonProperty("material_product_id")
        private Integer materialProductId;

        @JsonProperty("material_type")
        private String materialType;

        private BigDecimal quantity;

        private String unit;

        @JsonProperty("line_no")
        private Integer lineNo;

        private String note;
    }
}
