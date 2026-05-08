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
import io.micronaut.data.model.Page;
import io.micronaut.data.model.Pageable;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.*;
import io.micronaut.serde.annotation.Serdeable;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller("/productions")
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

            return HttpResponse.ok(
                    ApiResponse.success(productionBuilder.buildItemFull(saved))
            );
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

            return HttpResponse.ok(
                    ApiResponse.success(productionBuilder.buildItemFull(saved))
            );
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
            return HttpResponse.ok(ApiResponse.success(true));
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
            return HttpResponse.ok(ApiResponse.success(true));
        } catch (BusinessException e) {
            return HttpResponse.badRequest(
                    ApiResponse.error(e.getCode(), e.getMessage())
            );
        }
    }

    @Post("/{id}/confirm")
    public HttpResponse<?> confirm(@PathVariable Integer id) {
        try {
            productionService.confirmProduction(id);
            return HttpResponse.ok(ApiResponse.success(true));
        } catch (BusinessException e) {
            return HttpResponse.badRequest(
                    ApiResponse.error(e.getCode(), e.getMessage())
            );
        }
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
}