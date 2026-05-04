package com.vlife.api.controller.inventory;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.vlife.api.builder.production.ProductionOrderBuilder;
import com.vlife.api.controller.base.BaseCrudController;
import com.vlife.api.util.ApiUtil;
import com.vlife.shared.api.dto.ApiResponse;
import com.vlife.shared.jdbc.dao.production.*;
import com.vlife.shared.jdbc.entity.production.*;
import com.vlife.shared.service.production.ProductionService;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Controller("/inventory/productions")
public class ProductionOrderController extends BaseCrudController<ProductionOrder, Integer, ProductionOrderDao> {

    private final ProductBomDao bomDao;
    private final ProductBomItemDao bomItemDao;
    private final ProductionMaterialDao materialDao;
    private final ProductionOutputDao outputDao;
    private final ProductionExtraItemDao extraDao;
    private final ProductionSubstitutionDao substitutionDao;
    private final ProductionService productionService;

    @Inject
    public ProductionOrderController(
            ProductionOrderDao dao,
            ProductionOrderBuilder builder,
            ProductBomDao bomDao,
            ProductBomItemDao bomItemDao,
            ProductionMaterialDao materialDao,
            ProductionOutputDao outputDao,
            ProductionExtraItemDao extraDao,
            ProductionSubstitutionDao substitutionDao,
            ProductionService productionService
    ) {
        super(dao, builder);
        this.bomDao = bomDao;
        this.bomItemDao = bomItemDao;
        this.materialDao = materialDao;
        this.outputDao = outputDao;
        this.extraDao = extraDao;
        this.substitutionDao = substitutionDao;
        this.productionService = productionService;
    }

    @Override
    protected Page<ProductionOrder> doSearch(Map<String, String> filters, Pageable pageable) {
        return dao.search(
                ApiUtil.trim(filters.get("keyword")),
                ApiUtil.parseInteger(filters.get("product_id")),
                ApiUtil.parseInteger(filters.get("warehouse_id")),
                ApiUtil.trim(filters.get("status")),
                ApiUtil.toDate(filters.get("from_date")),
                ApiUtil.toDate(filters.get("to_date")),
                pageable
        );
    }

    @Post
    @Transactional
    public HttpResponse<?> create(@Body ProductionCreateRequest req) {
        ApiResponse<?> err = validateRequest(req);
        if (err != null) return HttpResponse.badRequest(err);

        LocalDateTime now = LocalDateTime.now();

        ProductionOrder order = new ProductionOrder();
        order.setProductionNo(generateProductionNo());
        order.setProductId(req.getProductId());
        order.setWarehouseId(req.getWarehouseId());
        order.setProductionDate(ApiUtil.toDate(req.getProductionDate()));
        order.setQuantityPlan(req.getQuantityPlan());
        order.setQuantityDone(req.getQuantityDone());
        order.setStatus(req.getStatus() != null ? req.getStatus() : "PLANNED");
        order.setCreatedAt(now);
        order.setUpdatedAt(now);

        order = dao.insert(order);

        saveOutput(order, req, now);
        saveExtras(order.getId(), req.getExtras(), now);
        saveSubstitutions(order.getId(), req.getSubstitutions(), now);
        generateMaterialsFromBom(order, req, now);

        return HttpResponse.ok(ApiResponse.success(buildItemResponse(order)));
    }

    @Put("/{id}")
    @Transactional
    public HttpResponse<?> update(@PathVariable Integer id, @Body ProductionUpdateRequest req) {
        ProductionOrder old = dao.findById(id).orElse(null);
        if (old == null) {
            return HttpResponse.notFound(ApiResponse.error(-404, "not found"));
        }

        if ("DONE".equals(old.getStatus())) {
            return HttpResponse.badRequest(ApiResponse.error(-400, "Lệnh sản xuất đã hoàn tất, không được sửa"));
        }

        ApiResponse<?> err = validateRequest(req);
        if (err != null) return HttpResponse.badRequest(err);

        LocalDateTime now = LocalDateTime.now();

        ProductionOrder update = new ProductionOrder();
        update.setProductId(req.getProductId());
        update.setWarehouseId(req.getWarehouseId());
        update.setProductionDate(ApiUtil.toDate(req.getProductionDate()));
        update.setQuantityPlan(req.getQuantityPlan());
        update.setQuantityDone(req.getQuantityDone());
        update.setStatus(req.getStatus());
        update.setUpdatedAt(now);

        dao.updateSelective(id, update);

        materialDao.deleteByProductionId(id);
        outputDao.deleteByProductionId(id);
        extraDao.deleteByProductionId(id);
        substitutionDao.deleteByProductionId(id);

        ProductionOrder order = dao.findById(id).orElseThrow();

        saveOutput(order, req, now);
        saveExtras(id, req.getExtras(), now);
        saveSubstitutions(id, req.getSubstitutions(), now);
        generateMaterialsFromBom(order, req, now);

        return HttpResponse.ok(ApiResponse.success(true));
    }

    @Post("/{id}/confirm")
    @Transactional
    public HttpResponse<?> confirm(@PathVariable Integer id) {
        productionService.confirmProduction(id);
        return HttpResponse.ok(ApiResponse.success(true));
    }

    private void saveOutput(ProductionOrder order, ProductionCreateRequest req, LocalDateTime now) {
        ProductionOutput output = new ProductionOutput();
        output.setProductionId(order.getId());
        output.setProductId(req.getProductId());
        output.setQuantity(req.getQuantityDone());
        output.setUnitCost(req.getUnitCost() != null ? req.getUnitCost() : BigDecimal.ZERO);
        output.setCreatedAt(now);
        output.setUpdatedAt(now);

        outputDao.insert(output);
    }

    private void generateMaterialsFromBom(
            ProductionOrder order,
            ProductionCreateRequest req,
            LocalDateTime now
    ) {
        ProductBom bom = bomDao.findActiveByProductId(order.getProductId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy định mức cho thành phẩm"));

        List<ProductBomItem> bomItems = bomItemDao.findByBomId(bom.getId());

        Map<Integer, ProductionSubstitutionRequest> substituteMap = new HashMap<>();

        if (req.getSubstitutions() != null) {
            for (ProductionSubstitutionRequest s : req.getSubstitutions()) {
                substituteMap.put(s.getOriginalProductId(), s);
            }
        }

        BigDecimal qty = nvl(order.getQuantityPlan());

        for (ProductBomItem bi : bomItems) {
            Integer materialProductId = bi.getMaterialProductId();
            BigDecimal materialQty = nvl(bi.getQuantity()).multiply(qty);

            ProductionSubstitutionRequest sub = substituteMap.get(materialProductId);

            if (sub != null) {
                materialProductId = sub.getSubstituteProductId();
                materialQty = sub.getQuantity();
            }

            ProductionMaterial m = new ProductionMaterial();
            m.setProductionId(order.getId());
            m.setProductId(materialProductId);
            m.setQuantityRequired(materialQty);
            m.setLotId(null);
            m.setSourceType(sub != null ? "SUBSTITUTE" : "BOM");
            m.setRefId(sub != null ? null : bi.getId());
            m.setCreatedAt(now);
            m.setUpdatedAt(now);

            materialDao.insert(m);
        }

        if (req.getExtras() != null) {
            for (ProductionExtraRequest e : req.getExtras()) {
                ProductionMaterial m = new ProductionMaterial();
                m.setProductionId(order.getId());
                m.setProductId(e.getProductId());
                m.setQuantityRequired(e.getQuantity());
                m.setLotId(e.getLotId());
                m.setSourceType("EXTRA");
                m.setRefId(null);
                m.setCreatedAt(now);
                m.setUpdatedAt(now);

                materialDao.insert(m);
            }
        }
    }

    private void saveExtras(Integer productionId, List<ProductionExtraRequest> extras, LocalDateTime now) {
        if (extras == null) return;

        for (ProductionExtraRequest r : extras) {
            ProductionExtraItem x = new ProductionExtraItem();
            x.setProductionId(productionId);
            x.setProductId(r.getProductId());
            x.setQuantity(r.getQuantity());
            x.setNote(ApiUtil.trim(r.getNote()));
            x.setCreatedAt(now);
            x.setUpdatedAt(now);

            extraDao.insert(x);
        }
    }

    private void saveSubstitutions(
            Integer productionId,
            List<ProductionSubstitutionRequest> substitutions,
            LocalDateTime now
    ) {
        if (substitutions == null) return;

        for (ProductionSubstitutionRequest r : substitutions) {
            ProductionSubstitution x = new ProductionSubstitution();
            x.setProductionId(productionId);
            x.setOriginalProductId(r.getOriginalProductId());
            x.setSubstituteProductId(r.getSubstituteProductId());
            x.setQuantity(r.getQuantity());
            x.setCreatedAt(now);
            x.setUpdatedAt(now);

            substitutionDao.insert(x);
        }
    }

    private ApiResponse<?> validateRequest(ProductionCreateRequest req) {
        if (req.getProductId() == null) {
            return ApiResponse.error(-400, "product_id is required");
        }

        if (req.getWarehouseId() == null) {
            return ApiResponse.error(-400, "warehouse_id is required");
        }

        if (req.getProductionDate() == null || req.getProductionDate().isBlank()) {
            return ApiResponse.error(-400, "production_date is required");
        }

        if (req.getQuantityPlan() == null || req.getQuantityPlan().compareTo(BigDecimal.ZERO) <= 0) {
            return ApiResponse.error(-400, "quantity_plan must > 0");
        }

        if (req.getQuantityDone() == null || req.getQuantityDone().compareTo(BigDecimal.ZERO) <= 0) {
            return ApiResponse.error(-400, "quantity_done must > 0");
        }

        if (req.getExtras() != null) {
            for (ProductionExtraRequest e : req.getExtras()) {
                if (e.getProductId() == null) {
                    return ApiResponse.error(-400, "extra product_id is required");
                }

                if (e.getQuantity() == null || e.getQuantity().compareTo(BigDecimal.ZERO) <= 0) {
                    return ApiResponse.error(-400, "extra quantity must > 0");
                }
            }
        }

        if (req.getSubstitutions() != null) {
            for (ProductionSubstitutionRequest s : req.getSubstitutions()) {
                if (s.getOriginalProductId() == null) {
                    return ApiResponse.error(-400, "original_product_id is required");
                }

                if (s.getSubstituteProductId() == null) {
                    return ApiResponse.error(-400, "substitute_product_id is required");
                }

                if (s.getQuantity() == null || s.getQuantity().compareTo(BigDecimal.ZERO) <= 0) {
                    return ApiResponse.error(-400, "substitute quantity must > 0");
                }
            }
        }

        return null;
    }

    private String generateProductionNo() {
        String prefix = "LSX-" + LocalDate.now()
                .format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"));

        Integer count = dao.countByPrefix(prefix);
        return prefix + "-" + String.format("%03d", count + 1);
    }

    private BigDecimal nvl(BigDecimal x) {
        return x != null ? x : BigDecimal.ZERO;
    }

    @Setter
    @Getter
    @Serdeable
    public static class ProductionCreateRequest {

        @JsonProperty("product_id")
        private Integer productId;

        @JsonProperty("warehouse_id")
        private Integer warehouseId;

        @JsonProperty("production_date")
        private String productionDate;

        @JsonProperty("quantity_plan")
        private BigDecimal quantityPlan;

        @JsonProperty("quantity_done")
        private BigDecimal quantityDone;

        @JsonProperty("unit_cost")
        private BigDecimal unitCost;

        private String status;

        private List<ProductionExtraRequest> extras;

        private List<ProductionSubstitutionRequest> substitutions;
    }

    @Setter
    @Getter
    @Serdeable
    public static class ProductionUpdateRequest extends ProductionCreateRequest {}

    @Setter
    @Getter
    @Serdeable
    public static class ProductionExtraRequest {

        @JsonProperty("product_id")
        private Integer productId;

        private BigDecimal quantity;

        @JsonProperty("lot_id")
        private Integer lotId;

        private String note;
    }

    @Setter
    @Getter
    @Serdeable
    public static class ProductionSubstitutionRequest {

        @JsonProperty("original_product_id")
        private Integer originalProductId;

        @JsonProperty("substitute_product_id")
        private Integer substituteProductId;

        private BigDecimal quantity;
    }
}