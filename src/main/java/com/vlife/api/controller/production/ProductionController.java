package com.vlife.api.controller.production;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.vlife.api.builder.production.ProductionOrderBuilder;
import com.vlife.api.controller.base.BaseCrudController;
import com.vlife.api.util.ApiUtil;
import com.vlife.shared.api.dto.ApiResponse;
import com.vlife.shared.exception.BusinessException;
import com.vlife.shared.jdbc.dao.production.ProductionOrderDao;
import com.vlife.shared.jdbc.entity.production.ProductionOrder;
import com.vlife.shared.service.production.ProductionService;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.data.model.Page;
import io.micronaut.data.model.Pageable;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.*;
import io.micronaut.scheduling.TaskExecutors;
import io.micronaut.scheduling.annotation.ExecuteOn;
import io.micronaut.serde.annotation.Serdeable;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller("/productions")
@ExecuteOn(TaskExecutors.IO)
public class ProductionController
        extends BaseCrudController<ProductionOrder, Integer, ProductionOrderDao> {

    private final ProductionOrderBuilder productionBuilder;
    private final ProductionService productionService;

    public ProductionController(
            ProductionOrderDao dao,
            ProductionOrderBuilder builder,
            ProductionService productionService
    ) {
        super(dao, builder);
        this.productionBuilder = builder;
        this.productionService = productionService;
    }

    @Override
    protected Page<ProductionOrder> doSearch(
            Map<String, String> filters,
            Pageable pageable
    ) {
        return dao.search(
                ApiUtil.trim(filters.get("keyword")),
                ApiUtil.trim(filters.get("status")),
                ApiUtil.toDate(filters.get("from_date")),
                ApiUtil.toDate(filters.get("to_date")),
                pageable
        );
    }

    @Post
    public HttpResponse<?> create(@Body ProductionCreateRequest req) {
        try {
            ProductionOrder saved =
                    productionService.createProduction(toCreateCommand(req));

            return HttpResponse.ok(ApiResponse.success(orderResult(saved)));
        } catch (BusinessException e) {
            return HttpResponse.badRequest(
                    ApiResponse.error(e.getCode(), e.getMessage())
            );
        }
    }

    @Put("/{id}")
    public HttpResponse<?> update(
            @PathVariable Integer id,
            @Body ProductionUpdateRequest req
    ) {
        try {
            ProductionOrder saved =
                    productionService.updateProduction(id, toUpdateCommand(req));

            return HttpResponse.ok(ApiResponse.success(orderResult(saved)));
        } catch (BusinessException e) {
            return HttpResponse.badRequest(
                    ApiResponse.error(e.getCode(), e.getMessage())
            );
        }
    }

    @Delete("/{id}")
    @Override
    public HttpResponse<?> delete(@PathVariable Integer id) {
        try {
            productionService.deleteProduction(id);
            return HttpResponse.ok(ApiResponse.success(true));
        } catch (BusinessException e) {
            return HttpResponse.badRequest(
                    ApiResponse.error(e.getCode(), e.getMessage())
            );
        }
    }

    @Post("/{id}/generate-materials")
    public HttpResponse<?> generateMaterials(@PathVariable Integer id) {
        try {
            productionService.generateMaterials(id);
            return HttpResponse.ok(ApiResponse.success(actionResult(id)));
        } catch (BusinessException e) {
            return HttpResponse.badRequest(
                    ApiResponse.error(e.getCode(), e.getMessage())
            );
        }
    }

    @Post("/{id}/allocate-fifo")
    public HttpResponse<?> allocateFifo(@PathVariable Integer id) {
        try {
            productionService.allocateFifo(id);
            return HttpResponse.ok(ApiResponse.success(actionResult(id)));
        } catch (BusinessException e) {
            return HttpResponse.badRequest(
                    ApiResponse.error(e.getCode(), e.getMessage())
            );
        }
    }

    @Post("/{id}/extras")
    public HttpResponse<?> addExtraMaterial(
            @PathVariable Integer id,
            @Body ExtraMaterialRequest req
    ) {
        try {
            productionService.addExtraMaterial(id, toExtraCommand(req));
            return HttpResponse.ok(ApiResponse.success(actionResult(id)));
        } catch (BusinessException e) {
            return HttpResponse.badRequest(
                    ApiResponse.error(e.getCode(), e.getMessage())
            );
        }
    }

    @Post("/{id}/substitutions")
    public HttpResponse<?> addSubstitution(
            @PathVariable Integer id,
            @Body SubstitutionRequest req
    ) {
        try {
            productionService.addSubstitution(id, toSubstitutionCommand(req));
            return HttpResponse.ok(ApiResponse.success(actionResult(id)));
        } catch (BusinessException e) {
            return HttpResponse.badRequest(
                    ApiResponse.error(e.getCode(), e.getMessage())
            );
        }
    }

    @Put("/{id}/materials/{materialId}/preferred-lot")
    public HttpResponse<?> setPreferredLot(
            @PathVariable Integer id,
            @PathVariable Integer materialId,
            @Body PreferredLotRequest req
    ) {
        try {
            ProductionService.SetPreferredLotCommand cmd =
                    new ProductionService.SetPreferredLotCommand();

            cmd.setProductionMaterialId(materialId);
            if (req != null) {
                cmd.setLotId(req.getLotId());
                cmd.setLotNo(ApiUtil.trim(req.getLotNo()));
            }

            productionService.setPreferredLot(id, cmd);
            return HttpResponse.ok(ApiResponse.success(actionResult(id)));
        } catch (BusinessException e) {
            return HttpResponse.badRequest(
                    ApiResponse.error(e.getCode(), e.getMessage())
            );
        }
    }

    @Post("/{id}/confirm")
    public HttpResponse<?> confirm(
            @PathVariable Integer id,
            @Nullable @Body ConfirmProductionRequest req
    ) {
        try {
            productionService.confirmProduction(id, toConfirmCommand(req));
            return HttpResponse.ok(ApiResponse.success(actionResult(id)));
        } catch (BusinessException e) {
            return HttpResponse.badRequest(
                    ApiResponse.error(e.getCode(), e.getMessage())
            );
        }
    }

    @Get("/{id}/detail")
    public HttpResponse<?> detail(@PathVariable Integer id) {
        try {
            return HttpResponse.ok(ApiResponse.success(buildFull(id)));
        } catch (BusinessException e) {
            return HttpResponse.badRequest(
                    ApiResponse.error(e.getCode(), e.getMessage())
            );
        }
    }

    private Map<String, Object> buildFull(Integer id) {
        ProductionOrder order = dao.findById(id)
                .orElseThrow(() -> new BusinessException(-404, "production not found"));

        return productionBuilder.buildItemFull(order);
    }

    private Map<String, Object> actionResult(Integer id) {
        return Map.of(
                "success", true,
                "id", id
        );
    }

    private Map<String, Object> orderResult(ProductionOrder order) {
        return Map.of(
                "id", order.getId(),
                "production_no", order.getProductionNo(),
                "status", order.getStatus()
        );
    }

    private ProductionService.CreateProductionCommand toCreateCommand(
            ProductionCreateRequest req
    ) {
        ProductionService.CreateProductionCommand cmd =
                new ProductionService.CreateProductionCommand();

        if (req == null) return cmd;

        cmd.setWarehouseId(req.getWarehouseId());
        cmd.setProductionDate(ApiUtil.toDate(req.getProductionDate()));
        cmd.setPackingCode(ApiUtil.trim(req.getPackingCode()));
        cmd.setNote(ApiUtil.trim(req.getNote()));

        cmd.setItems(
                req.getItems() == null
                        ? List.of()
                        : req.getItems()
                        .stream()
                        .map(this::toItemCommand)
                        .collect(Collectors.toList())
        );

        return cmd;
    }

    private ProductionService.UpdateProductionCommand toUpdateCommand(
            ProductionUpdateRequest req
    ) {
        ProductionService.UpdateProductionCommand cmd =
                new ProductionService.UpdateProductionCommand();

        if (req == null) return cmd;

        cmd.setWarehouseId(req.getWarehouseId());
        cmd.setProductionDate(ApiUtil.toDate(req.getProductionDate()));
        cmd.setStatus(ApiUtil.trim(req.getStatus()));
        cmd.setNote(ApiUtil.trim(req.getNote()));

        if (req.getItems() != null) {
            cmd.setItems(
                    req.getItems()
                            .stream()
                            .map(this::toItemCommand)
                            .collect(Collectors.toList())
            );
        }

        return cmd;
    }

    private ProductionService.ProductionItemCommand toItemCommand(
            ProductionItemRequest r
    ) {
        ProductionService.ProductionItemCommand item =
                new ProductionService.ProductionItemCommand();

        item.setProductId(r.getProductId());
        item.setWarehouseId(r.getWarehouseId());
        item.setQuantityPlan(r.getQuantityPlan());
        item.setQuantityDone(r.getQuantityDone());
        item.setLotNo(ApiUtil.trim(r.getLotNo()));
        item.setExpiryDate(ApiUtil.toDate(r.getExpiryDate()));
        item.setNote(ApiUtil.trim(r.getNote()));

        return item;
    }

    private ProductionService.AddExtraMaterialCommand toExtraCommand(
            ExtraMaterialRequest r
    ) {
        ProductionService.AddExtraMaterialCommand cmd =
                new ProductionService.AddExtraMaterialCommand();

        if (r == null) return cmd;

        cmd.setProductionItemId(r.getProductionItemId());
        cmd.setProductId(r.getProductId());
        cmd.setWarehouseId(r.getWarehouseId());
        cmd.setMaterialType(ApiUtil.trim(r.getMaterialType()));
        cmd.setQuantityPerUnit(r.getQuantityPerUnit());
        cmd.setQuantity(r.getQuantity());
        cmd.setNote(ApiUtil.trim(r.getNote()));

        return cmd;
    }

    private ProductionService.AddSubstitutionCommand toSubstitutionCommand(
            SubstitutionRequest r
    ) {
        ProductionService.AddSubstitutionCommand cmd =
                new ProductionService.AddSubstitutionCommand();

        if (r == null) return cmd;

        cmd.setProductionItemId(r.getProductionItemId());
        cmd.setBomItemId(r.getBomItemId());
        cmd.setOriginalProductId(r.getOriginalProductId());
        cmd.setSubstituteProductId(r.getSubstituteProductId());
        cmd.setQuantityOriginal(r.getQuantityOriginal());
        cmd.setQuantity(r.getQuantity());
        cmd.setReason(ApiUtil.trim(r.getReason()));
        cmd.setNote(ApiUtil.trim(r.getNote()));

        return cmd;
    }

    private ProductionService.ConfirmProductionCommand toConfirmCommand(
            ConfirmProductionRequest req
    ) {
        ProductionService.ConfirmProductionCommand cmd =
                new ProductionService.ConfirmProductionCommand();

        if (req == null || req.getOutputs() == null) {
            cmd.setOutputs(List.of());
            return cmd;
        }

        cmd.setOutputs(
                req.getOutputs()
                        .stream()
                        .map(this::toOutputCommand)
                        .collect(Collectors.toList())
        );

        return cmd;
    }

    private ProductionService.ConfirmOutputCommand toOutputCommand(
            ConfirmOutputRequest r
    ) {
        ProductionService.ConfirmOutputCommand cmd =
                new ProductionService.ConfirmOutputCommand();

        if (r == null) return cmd;

        cmd.setProductionItemId(r.getProductionItemId());
        cmd.setOutputId(r.getOutputId());
        cmd.setLotNo(ApiUtil.trim(r.getLotNo()));
        cmd.setExpiryDate(ApiUtil.toDate(r.getExpiryDate()));
        cmd.setNote(ApiUtil.trim(r.getNote()));

        return cmd;
    }

    @Getter
    @Setter
    @Serdeable
    public static class ProductionCreateRequest {

        @JsonProperty("warehouse_id")
        private Integer warehouseId;

        @JsonProperty("production_date")
        private String productionDate;

        @JsonProperty("packing_code")
        private String packingCode;

        private String note;

        private List<ProductionItemRequest> items;
    }

    @Getter
    @Setter
    @Serdeable
    public static class ProductionUpdateRequest {

        @JsonProperty("warehouse_id")
        private Integer warehouseId;

        @JsonProperty("production_date")
        private String productionDate;

        private String status;

        private String note;

        private List<ProductionItemRequest> items;
    }

    @Getter
    @Setter
    @Serdeable
    public static class ProductionItemRequest {

        @JsonProperty("product_id")
        private Integer productId;

        @JsonProperty("warehouse_id")
        private Integer warehouseId;

        @JsonProperty("quantity_plan")
        private BigDecimal quantityPlan;

        @JsonProperty("quantity_done")
        private BigDecimal quantityDone;

        @JsonProperty("lot_no")
        private String lotNo;

        @JsonProperty("expiry_date")
        private String expiryDate;

        private String note;
    }

    @Getter
    @Setter
    @Serdeable
    public static class ExtraMaterialRequest {

        @JsonProperty("production_item_id")
        private Integer productionItemId;

        @JsonProperty("product_id")
        private Integer productId;

        @JsonProperty("warehouse_id")
        private Integer warehouseId;

        @JsonProperty("material_type")
        private String materialType;

        @JsonProperty("quantity_per_unit")
        private BigDecimal quantityPerUnit;

        private BigDecimal quantity;

        private String note;
    }

    @Getter
    @Setter
    @Serdeable
    public static class SubstitutionRequest {

        @JsonProperty("production_item_id")
        private Integer productionItemId;

        @JsonProperty("bom_item_id")
        private Integer bomItemId;

        @JsonProperty("original_product_id")
        private Integer originalProductId;

        @JsonProperty("substitute_product_id")
        private Integer substituteProductId;

        @JsonProperty("quantity_original")
        private BigDecimal quantityOriginal;

        private BigDecimal quantity;

        private String reason;

        private String note;
    }

    @Getter
    @Setter
    @Serdeable
    public static class PreferredLotRequest {

        @JsonProperty("lot_id")
        private Integer lotId;

        @JsonProperty("lot_no")
        private String lotNo;
    }

    @Getter
    @Setter
    @Serdeable
    public static class ConfirmProductionRequest {

        private List<ConfirmOutputRequest> outputs;
    }

    @Getter
    @Setter
    @Serdeable
    public static class ConfirmOutputRequest {

        @JsonProperty("production_item_id")
        private Integer productionItemId;

        @JsonProperty("output_id")
        private Integer outputId;

        @JsonProperty("lot_no")
        private String lotNo;

        @JsonProperty("expiry_date")
        private String expiryDate;

        private String note;
    }
}
