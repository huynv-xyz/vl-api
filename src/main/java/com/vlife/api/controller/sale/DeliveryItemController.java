package com.vlife.api.controller.sale;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.vlife.api.controller.base.BaseCrudController;
import com.vlife.shared.api.builder.ItemBuilder;
import com.vlife.shared.api.dto.ApiResponse;
import com.vlife.shared.jdbc.dao.sale.DeliveryItemDao;
import com.vlife.shared.jdbc.entity.sale.DeliveryItem;
import io.micronaut.data.model.Page;
import io.micronaut.data.model.Pageable;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.*;
import io.micronaut.serde.annotation.Serdeable;
import jakarta.inject.Inject;

import java.time.LocalDateTime;
import java.util.Map;

@Controller("/sales/delivery-items")
public class DeliveryItemController extends BaseCrudController<DeliveryItem, Integer, DeliveryItemDao> {

    @Inject
    public DeliveryItemController(DeliveryItemDao dao, ItemBuilder<DeliveryItem> builder) {
        super(dao, builder);
    }

    @Override
    protected Page<DeliveryItem> doSearch(Map<String, String> filters, Pageable pageable) {
        Integer deliveryId = parseInt(filters.get("delivery_id"));
        return dao.search(deliveryId,null, pageable);
    }

    @Post
    public HttpResponse<?> create(@Body DeliveryItemRequest req) {
        return handleCreate(req, r -> {
            DeliveryItem d = new DeliveryItem();

            d.setDeliveryId(r.getDeliveryId());
            d.setProductId(r.getProductId());
            d.setQuantity(r.getQuantity());

            return d;
        });
    }

    @Put("/{id}")
    public HttpResponse<?> update(@PathVariable Integer id, @Body DeliveryItemRequest req) {
        return handleUpdate(id, req, r -> {
            DeliveryItem d = new DeliveryItem();

            d.setQuantity(r.getQuantity());

            mergeNullFromDb(id, d, "id", "deliveryId", "productId", "createdAt");
            return d;
        });
    }

    @Override
    protected <REQ> ApiResponse<?> validateBeforeCreate(DeliveryItem entity, REQ req) {

        if (entity.getDeliveryId() == null)
            return ApiResponse.error(-400, "delivery_id is required");

        if (entity.getProductId() == null)
            return ApiResponse.error(-400, "product_id is required");

        if (entity.getQuantity() == null || entity.getQuantity().doubleValue() <= 0)
            return ApiResponse.error(-400, "quantity must > 0");

        return null;
    }

    @Override
    protected <REQ> ApiResponse<?> validateBeforeUpdate(Integer id, DeliveryItem entity, REQ req) {
        return null;
    }

    private Integer parseInt(String s) {
        try {
            return s == null ? null : Integer.parseInt(s);
        } catch (Exception e) {
            return null;
        }
    }

    // DTO
    @Serdeable
    public static class DeliveryItemRequest {

        @JsonProperty("delivery_id")
        private Integer deliveryId;

        @JsonProperty("product_id")
        private Integer productId;

        private java.math.BigDecimal quantity;

        @JsonProperty("unit_price")
        private java.math.BigDecimal unitPrice;

        public Integer getDeliveryId() { return deliveryId; }
        public void setDeliveryId(Integer deliveryId) { this.deliveryId = deliveryId; }

        public Integer getProductId() { return productId; }
        public void setProductId(Integer productId) { this.productId = productId; }

        public java.math.BigDecimal getQuantity() { return quantity; }
        public void setQuantity(java.math.BigDecimal quantity) { this.quantity = quantity; }

        public java.math.BigDecimal getUnitPrice() { return unitPrice; }
        public void setUnitPrice(java.math.BigDecimal unitPrice) { this.unitPrice = unitPrice; }
    }
}