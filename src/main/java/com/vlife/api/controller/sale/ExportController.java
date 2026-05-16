package com.vlife.api.controller.sale;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.vlife.api.builder.sale.ExportBuilder;
import com.vlife.api.controller.base.BaseCrudController;
import com.vlife.api.util.ApiUtil;
import com.vlife.shared.api.dto.ApiResponse;
import com.vlife.shared.jdbc.dao.sale.ExportDao;
import com.vlife.shared.jdbc.dao.sale.ExportItemDao;
import com.vlife.shared.jdbc.entity.sale.Export;
import com.vlife.shared.jdbc.entity.sale.ExportItem;
import com.vlife.shared.service.sale.ExportService;
import io.micronaut.data.model.Page;
import io.micronaut.data.model.Pageable;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.*;
import io.micronaut.serde.annotation.Serdeable;
import io.micronaut.transaction.annotation.Transactional;
import jakarta.inject.Inject;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Controller("/sales/exports")
public class ExportController extends BaseCrudController<Export, Integer, ExportDao> {

    private final ExportItemDao exportItemDao;
    private final ExportService exportService;

    @Inject
    public ExportController(
            ExportDao dao,
            ExportBuilder builder,
            ExportItemDao exportItemDao,
            ExportService exportService
    ) {
        super(dao, builder);
        this.exportItemDao = exportItemDao;
        this.exportService = exportService;
    }

    @Override
    protected Page<Export> doSearch(Map<String, String> filters, Pageable pageable) {
        return dao.search(
                ApiUtil.trim(filters.get("keyword")),
                ApiUtil.parseInteger(filters.get("order_id")),
                ApiUtil.parseInteger(filters.get("delivery_id")),
                ApiUtil.parseInteger(filters.get("warehouse_id")),
                ApiUtil.trim(filters.get("status")),
                ApiUtil.toDate(filters.get("from_date")),
                ApiUtil.toDate(filters.get("to_date")),
                pageable
        );
    }

    // ========================
    // CREATE (manual nếu cần)
    // ========================
    @Post
    @Transactional
    public HttpResponse<?> create(@Body ExportCreateRequest req) {

        if (req.getWarehouseId() == null) {
            return HttpResponse.badRequest(ApiResponse.error(-400, "warehouse_id is required"));
        }

        if (req.getItems() == null || req.getItems().isEmpty()) {
            return HttpResponse.badRequest(ApiResponse.error(-400, "items is required"));
        }

        Export e = new Export();

        e.setOrderId(req.getOrderId());
        e.setDeliveryId(req.getDeliveryId());
        e.setWarehouseId(req.getWarehouseId());
        e.setExportDate(ApiUtil.toDate(req.getExportDate()));
        e.setStatus("NEW");
        e.setCreatedAt(LocalDateTime.now());
        e.setUpdatedAt(LocalDateTime.now());

        e = dao.insert(e);

        List<ExportItem> list = new ArrayList<>();

        for (ItemRequest i : req.getItems()) {

            if (i.getProductId() == null) {
                return HttpResponse.badRequest(ApiResponse.error(-400, "product_id is required"));
            }

            if (i.getQuantity() == null || i.getQuantity().compareTo(BigDecimal.ZERO) <= 0) {
                return HttpResponse.badRequest(ApiResponse.error(-400, "quantity must > 0"));
            }

            ExportItem item = new ExportItem();
            item.setExportId(e.getId());
            item.setProductId(i.getProductId());
            item.setQuantity(i.getQuantity());

            list.add(item);
        }

        exportItemDao.saveAll(list);

        return HttpResponse.ok(ApiResponse.success(e));
    }

    // ========================
    // UPDATE STATUS
    // ========================
    @Put("/{id}/status")
    @Transactional
    public HttpResponse<?> updateStatus(
            @PathVariable Integer id,
            @Body UpdateStatusRequest req
    ) {

        Export export = dao.findById(id)
                .orElseThrow(() -> new RuntimeException("export not found"));

        if (req.getStatus() == null || req.getStatus().isBlank()) {
            return HttpResponse.badRequest(ApiResponse.error(-400, "status is required"));
        }

        if ("DONE".equals(export.getStatus())) {
            return HttpResponse.badRequest(ApiResponse.error(-400, "already DONE"));
        }

        if ("DONE".equals(req.getStatus())) {

            exportService.finishExport(id);

            return HttpResponse.ok(ApiResponse.success(true));
        }

        Export x = new Export();
        x.setStatus(req.getStatus());
        x.setUpdatedAt(LocalDateTime.now());

        dao.updateSelective(id, x);

        return HttpResponse.ok(ApiResponse.success(true));
    }

    // ========================
    // DTO
    // ========================

    @Setter
    @Getter
    @Serdeable
    public static class ExportCreateRequest {

        @JsonProperty("order_id")
        private Integer orderId;

        @JsonProperty("delivery_id")
        private Integer deliveryId;

        @JsonProperty("warehouse_id")
        private Integer warehouseId;

        @JsonProperty("export_date")
        private String exportDate;

        private List<ItemRequest> items;
    }

    @Setter
    @Getter
    @Serdeable
    public static class ItemRequest {

        @JsonProperty("product_id")
        private Integer productId;

        private BigDecimal quantity;
    }

    @Setter
    @Getter
    @Serdeable
    public static class UpdateStatusRequest {
        private String status;
    }
}