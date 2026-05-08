package com.vlife.api.controller.purchasing;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.vlife.api.builder.purchasing.ShipmentBuilder;
import com.vlife.api.controller.base.BaseCrudController;
import com.vlife.api.util.ApiUtil;
import com.vlife.shared.api.dto.ApiResponse;
import com.vlife.shared.jdbc.dao.purchasing.ShipmentDao;
import com.vlife.shared.jdbc.entity.purchasing.Shipment;
import com.vlife.shared.jdbc.entity.purchasing.ShipmentItem;
import com.vlife.shared.service.purchasing.ShipmentService;
import io.micronaut.data.model.Page;
import io.micronaut.data.model.Pageable;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.*;
import io.micronaut.serde.annotation.Serdeable;
import jakarta.inject.Inject;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Controller("/purchasing/shipments")
public class ShipmentController extends BaseCrudController<Shipment, Integer, ShipmentDao> {

    @Inject
    ShipmentService shipmentService;

    @Inject
    public ShipmentController(ShipmentDao dao, ShipmentBuilder builder) {
        super(dao, builder);
    }

    @Override
    protected Page<Shipment> doSearch(Map<String, String> filters, Pageable pageable) {
        return dao.search(
                ApiUtil.trim(filters.get("keyword")),
                ApiUtil.trim(filters.get("status")),

                ApiUtil.parseInteger(filters.get("supplier_id")),
                ApiUtil.parseInteger(filters.get("product_id")),          // 🔥 thêm
                ApiUtil.parseInteger(filters.get("port_id")),             // 🔥 thêm

                ApiUtil.toDateTime(filters.get("eta_from")),
                ApiUtil.toDateTime(filters.get("eta_to")),

                pageable
        );
    }

    // ========================
    // CREATE
    // ========================
    @Post
    public HttpResponse<?> create(@Body ShipmentCreateRequest req) {
        validateRequest(req);

        Shipment shipment = mapToEntity(req);
        List<ShipmentItem> items = toItems(req.getItems());

        Shipment saved = shipmentService.create(shipment, items);

        return HttpResponse.ok(ApiResponse.success(buildItemResponse(saved)));
    }

    // ========================
    // UPDATE
    // ========================
    @Put("/{id}")
    public HttpResponse<?> update(@PathVariable Integer id,
                                  @Body ShipmentUpdateRequest req) {

        Shipment shipment = mapToEntity(req);
        List<ShipmentItem> items = toItems(req.getItems());

        Shipment updated = shipmentService.update(id, shipment, items);

        return HttpResponse.ok(ApiResponse.success(buildItemResponse(updated)));
    }

    private void validateRequest(ShipmentCreateRequest req) {

        if (req.getContractId() == null) {
            throw new IllegalArgumentException("contract_id is required");
        }

        if (req.getItems() == null || req.getItems().isEmpty()) {
            throw new IllegalArgumentException("items is required");
        }

        for (ShipmentItemRequest i : req.getItems()) {
            if (i.getProductId() == null) {
                throw new IllegalArgumentException("product_id is required");
            }
        }
    }

    private Shipment mapToEntity(ShipmentCreateRequest req) {
        Shipment shipment = new Shipment();

        shipment.setCode(ApiUtil.trim(req.getCode()));
        shipment.setContractId(req.getContractId());

        shipment.setEtd(parseDate(req.getEtd()));
        shipment.setEta(parseDate(req.getEta()));
        shipment.setAta(parseDate(req.getAta()));
        shipment.setWarehouseAt(parseDate(req.getWarehouseAt()));
        shipment.setWarehouseId(req.getWarehouseId());

        shipment.setContainerNo(ApiUtil.trim(req.getContainerNo()));
        shipment.setDestinationPortId(req.getDestinationPortId());

        shipment.setExchangeRate(req.getExchangeRate());
        shipment.setStatus(ApiUtil.trim(req.getStatus()));
        shipment.setNote(ApiUtil.trim(req.getNote()));

        return shipment;
    }

    // 🔥 FIX: parse safe
    private LocalDate parseDate(String s) {
        if (s == null || s.trim().isEmpty()) return null;
        return LocalDate.parse(s);
    }

    // ========================
    // MAPPING ITEMS
    // ========================
    private List<ShipmentItem> toItems(List<ShipmentItemRequest> reqItems) {
        if (reqItems == null || reqItems.isEmpty()) return Collections.emptyList();

        return reqItems.stream().map(i -> {
            ShipmentItem x = new ShipmentItem();

            x.setProductId(i.getProductId());
            x.setQuantity(i.getQuantity());
            x.setDefectQuantity(
                    i.getDefectQuantity() != null ? i.getDefectQuantity() : BigDecimal.ZERO
            );

            x.setUnitPrice(i.getUnitPrice());

            return x;
        }).toList();
    }

    // ========================
    // DTO
    // ========================
    @Setter
    @Getter
    @Serdeable
    public static class ShipmentCreateRequest {

        private String code;

        @JsonProperty("contract_id")
        private Integer contractId;

        private String etd;
        private String eta;
        private String ata;

        @JsonProperty("warehouse_at")
        private String warehouseAt;

        @JsonProperty("warehouse_id")
        private Integer warehouseId;

        @JsonProperty("container_no")
        private String containerNo;

        @JsonProperty("destination_port_id")
        private Integer destinationPortId;

        @JsonProperty("exchange_rate")
        private BigDecimal exchangeRate;

        private String status;
        private String note;

        private List<ShipmentItemRequest> items;
    }

    @Serdeable
    public static class ShipmentUpdateRequest extends ShipmentCreateRequest {}

    @Setter
    @Getter
    @Serdeable
    public static class ShipmentItemRequest {

        @JsonProperty("product_id")
        private Integer productId;

        private BigDecimal quantity;

        @JsonProperty("defect_quantity")
        private BigDecimal defectQuantity;

        @JsonProperty("unit_price")
        private BigDecimal unitPrice;
    }
}