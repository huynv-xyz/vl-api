package com.vlife.api.controller.inventory;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.vlife.api.builder.inventory.InventoryLotBuilder;
import com.vlife.api.controller.base.BaseCrudController;
import com.vlife.api.util.ApiUtil;
import com.vlife.shared.api.dto.ApiResponse;
import com.vlife.shared.jdbc.dao.sale.InventoryLedgerDao;
import com.vlife.shared.jdbc.dao.sale.InventoryLotDao;
import com.vlife.shared.jdbc.dao.sale.ProductStockDao;
import com.vlife.shared.jdbc.entity.inventory.InventoryLedger;
import com.vlife.shared.jdbc.entity.inventory.InventoryLot;
import com.vlife.shared.service.inventory.OpeningStockImportService;
import com.vlife.shared.service.inventory.PurchaseStockImportService;
import io.micronaut.data.model.Page;
import io.micronaut.data.model.Pageable;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.*;
import io.micronaut.http.multipart.CompletedFileUpload;
import io.micronaut.serde.annotation.Serdeable;
import io.micronaut.transaction.annotation.Transactional;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Controller("/inventory/lots")
public class InventoryLotController extends BaseCrudController<InventoryLot, Integer, InventoryLotDao> {

    private final ProductStockDao productStockDao;
    private final InventoryLedgerDao inventoryLedgerDao;
    private final OpeningStockImportService openingStockImportService;
    private final PurchaseStockImportService purchaseStockImportService;

    public InventoryLotController(
            InventoryLotDao dao,
            InventoryLotBuilder builder,
            ProductStockDao productStockDao,
            InventoryLedgerDao inventoryLedgerDao,
            OpeningStockImportService openingStockImportService,
            PurchaseStockImportService purchaseStockImportService
    ) {
        super(dao, builder);
        this.productStockDao = productStockDao;
        this.inventoryLedgerDao = inventoryLedgerDao;
        this.openingStockImportService = openingStockImportService;
        this.purchaseStockImportService = purchaseStockImportService;
    }

    @Override
    protected Page<InventoryLot> doSearch(Map<String, String> filters, Pageable pageable) {
        return dao.search(
                ApiUtil.parseInteger(filters.get("product_id")),
                ApiUtil.parseInteger(filters.get("warehouse_id")),
                ApiUtil.trim(filters.get("lot_no")),
                ApiUtil.trim(filters.get("source_type")),
                ApiUtil.toDate(filters.get("from_date")),
                ApiUtil.toDate(filters.get("to_date")),
                Boolean.parseBoolean(filters.getOrDefault("only_remaining", "false")),
                ApiUtil.trim(filters.get("expiry_status")),
                pageable
        );
    }

    @Get("/stock")
    public HttpResponse<?> getStock(
            @QueryValue("warehouse_id") Integer warehouseId,
            @QueryValue("product_ids") List<Integer> productIds
    ) {

        if (warehouseId == null || productIds == null || productIds.isEmpty()) {
            return HttpResponse.badRequest(ApiResponse.error(-400, "invalid params"));
        }

        var data = dao.sumRemainingByProducts(warehouseId, productIds);

        return HttpResponse.ok(ApiResponse.success(data));
    }

    @Post
    @Transactional
    public HttpResponse<?> create(@Body InventoryLotCreateRequest req) {

        ApiResponse<?> err = validateRequest(req);
        if (err != null) {
            return HttpResponse.badRequest(err);
        }

        LocalDateTime now = LocalDateTime.now();

        InventoryLot lot = new InventoryLot();
        lot.setProductId(req.getProductId());
        lot.setWarehouseId(req.getWarehouseId());
        lot.setLotNo(ApiUtil.trim(req.getLotNo()));
        lot.setInboundDate(ApiUtil.toDate(req.getInboundDate()));
        lot.setSourceType(req.getSourceType());
        lot.setSourceId(req.getSourceId());
        lot.setSourceNo(ApiUtil.trim(req.getSourceNo()));
        lot.setQuantityIn(req.getQuantityIn());
        lot.setQuantityRemaining(req.getQuantityIn());
        lot.setUnitCost(req.getUnitCost() != null ? req.getUnitCost() : BigDecimal.ZERO);
        lot.setExpiryDate(ApiUtil.toDate(req.getExpiryDate()));
        lot.setLocked(false);
        lot.setCreatedAt(now);
        lot.setUpdatedAt(now);

        lot = dao.insert(lot);

        insertOpeningLedger(lot, now);

        productStockDao.increase(
                lot.getProductId(),
                lot.getWarehouseId(),
                lot.getQuantityIn()
        );

        return HttpResponse.ok(ApiResponse.success(buildItemResponse(lot)));
    }

    @Put("/{id}")
    @Transactional
    public HttpResponse<?> update(@PathVariable Integer id, @Body InventoryLotUpdateRequest req) {

        InventoryLot old = dao.findById(id).orElse(null);
        if (old == null) {
            return HttpResponse.notFound(ApiResponse.error(-404, "not found"));
        }

        /*
         * Nếu lô đã bị xuất một phần thì không cho sửa số lượng nhập.
         * Vì sửa quantity_in sau khi đã FIFO sẽ làm sai allocation/cost.
         */
        boolean lotAlreadyUsed =
                old.getQuantityRemaining() != null
                        && old.getQuantityIn() != null
                        && old.getQuantityRemaining().compareTo(old.getQuantityIn()) < 0;

        if (lotAlreadyUsed && req.getQuantityIn() != null
                && req.getQuantityIn().compareTo(old.getQuantityIn()) != 0) {
            return HttpResponse.badRequest(
                    ApiResponse.error(-400, "Không được sửa số lượng vì lô đã phát sinh xuất kho")
            );
        }

        if (req.getQuantityIn() != null && req.getQuantityIn().compareTo(BigDecimal.ZERO) <= 0) {
            return HttpResponse.badRequest(ApiResponse.error(-400, "quantity_in must > 0"));
        }

        if (req.getUnitCost() != null && req.getUnitCost().compareTo(BigDecimal.ZERO) < 0) {
            return HttpResponse.badRequest(ApiResponse.error(-400, "unit_cost must >= 0"));
        }

        InventoryLot update = new InventoryLot();

        if (req.getProductId() != null) update.setProductId(req.getProductId());
        if (req.getWarehouseId() != null) update.setWarehouseId(req.getWarehouseId());
        if (req.getLotNo() != null) update.setLotNo(ApiUtil.trim(req.getLotNo()));
        if (req.getInboundDate() != null) update.setInboundDate(ApiUtil.toDate(req.getInboundDate()));
        if (req.getSourceType() != null) update.setSourceType(req.getSourceType());
        if (req.getSourceId() != null) update.setSourceId(req.getSourceId());
        if (req.getSourceNo() != null) update.setSourceNo(ApiUtil.trim(req.getSourceNo()));
        if (req.getUnitCost() != null) update.setUnitCost(req.getUnitCost());
        if (req.getExpiryDate() != null) update.setExpiryDate(ApiUtil.toDate(req.getExpiryDate()));

        /*
         * Chỉ cho sửa quantity_remaining theo quantity_in nếu lô chưa bị xuất.
         */
        if (!lotAlreadyUsed && req.getQuantityIn() != null) {
            BigDecimal diff = req.getQuantityIn().subtract(old.getQuantityIn());

            update.setQuantityIn(req.getQuantityIn());
            update.setQuantityRemaining(req.getQuantityIn());

            if (diff.compareTo(BigDecimal.ZERO) != 0) {
                productStockDao.increase(
                        old.getProductId(),
                        old.getWarehouseId(),
                        diff
                );
            }
        }

        update.setUpdatedAt(LocalDateTime.now());

        dao.updateSelective(id, update);

        return HttpResponse.ok(ApiResponse.success(true));
    }

    @Post(value = "/opening/import-csv", consumes = MediaType.MULTIPART_FORM_DATA, produces = MediaType.APPLICATION_JSON)
    public HttpResponse<?> importOpeningCsv(@Part("file") CompletedFileUpload file) {
        try {
            var result = openingStockImportService.importCsv(file);
            return HttpResponse.ok(ApiResponse.success(result));
        } catch (Exception e) {
            return HttpResponse.badRequest(ApiResponse.error(-400, e.getMessage()));
        }
    }

    @Post(value = "/purchase/import-csv", consumes = MediaType.MULTIPART_FORM_DATA, produces = MediaType.APPLICATION_JSON)
    public HttpResponse<?> importPurchaseCsv(@Part("file") CompletedFileUpload file) {
        try {
            var result = purchaseStockImportService.importCsv(file);
            return HttpResponse.ok(ApiResponse.success(result));
        } catch (Exception e) {
            return HttpResponse.badRequest(ApiResponse.error(-400, e.getMessage()));
        }
    }

    private ApiResponse<?> validateRequest(InventoryLotCreateRequest req) {
        if (req.getProductId() == null)
            return ApiResponse.error(-400, "product_id is required");

        if (req.getWarehouseId() == null)
            return ApiResponse.error(-400, "warehouse_id is required");

        if (req.getInboundDate() == null)
            return ApiResponse.error(-400, "inbound_date is required");

        if (req.getSourceType() == null || req.getSourceType().isBlank())
            return ApiResponse.error(-400, "source_type is required");

        if (req.getQuantityIn() == null || req.getQuantityIn().compareTo(BigDecimal.ZERO) <= 0)
            return ApiResponse.error(-400, "quantity_in must > 0");

        if (req.getUnitCost() != null && req.getUnitCost().compareTo(BigDecimal.ZERO) < 0)
            return ApiResponse.error(-400, "unit_cost must >= 0");

        return null;
    }

    private void insertOpeningLedger(InventoryLot lot, LocalDateTime now) {
        InventoryLedger ledger = new InventoryLedger();
        ledger.setPostingDate(lot.getInboundDate());
        ledger.setProductId(lot.getProductId());
        ledger.setWarehouseId(lot.getWarehouseId());
        ledger.setQuantity(lot.getQuantityIn());
        ledger.setDocType(lot.getSourceType());
        ledger.setDocNo(
                lot.getSourceNo() != null && !lot.getSourceNo().isBlank()
                        ? lot.getSourceNo()
                        : "LOT-" + lot.getId()
        );
        ledger.setRefId(lot.getId());
        ledger.setCreatedAt(now);
        ledger.setUpdatedAt(now);

        inventoryLedgerDao.insert(ledger);
    }

    @Setter
    @Getter
    @Serdeable
    public static class InventoryLotCreateRequest {

        @JsonProperty("product_id")
        private Integer productId;

        @JsonProperty("warehouse_id")
        private Integer warehouseId;

        @JsonProperty("lot_no")
        private String lotNo;

        @JsonProperty("inbound_date")
        private String inboundDate;

        @JsonProperty("source_type")
        private String sourceType;

        @JsonProperty("source_id")
        private Integer sourceId;

        @JsonProperty("source_no")
        private String sourceNo;

        @JsonProperty("quantity_in")
        private BigDecimal quantityIn;

        @JsonProperty("unit_cost")
        private BigDecimal unitCost;

        @JsonProperty("expiry_date")
        private String expiryDate;
    }

    @Setter
    @Getter
    @Serdeable
    public static class InventoryLotUpdateRequest extends InventoryLotCreateRequest {}
}
