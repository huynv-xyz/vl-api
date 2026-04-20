package com.vlife.api.controller.sale;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.vlife.api.builder.sale.DeliveryBuilder;
import com.vlife.api.controller.base.BaseCrudController;
import com.vlife.api.util.ApiUtil;
import com.vlife.shared.api.dto.ApiResponse;
import com.vlife.shared.jdbc.dao.sale.DeliveryDao;
import com.vlife.shared.jdbc.dao.sale.DeliveryItemDao;
import com.vlife.shared.jdbc.entity.sale.Delivery;
import com.vlife.shared.jdbc.entity.sale.DeliveryItem;
import com.vlife.shared.service.sale.DeliveryService;
import com.vlife.shared.util.CommonUtil;
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
import java.util.*;
import java.util.stream.Collectors;

@Controller("/sales/deliveries")
public class DeliveryController extends BaseCrudController<Delivery, Integer, DeliveryDao> {

    private final DeliveryItemDao deliveryItemDao;

    private final DeliveryService deliveryService;

    @Inject
    public DeliveryController(
            DeliveryDao dao,
            DeliveryBuilder builder,
            DeliveryItemDao deliveryItemDao,
            DeliveryService deliveryService   // 👈 thêm
    ) {
        super(dao, builder);
        this.deliveryItemDao = deliveryItemDao;
        this.deliveryService = deliveryService;
    }

    // ========================
    // SEARCH
    // ========================
    @Override
    protected Page<Delivery> doSearch(Map<String, String> filters, Pageable pageable) {
        return dao.search(
                ApiUtil.trim(filters.get("keyword")),
                ApiUtil.parseInteger(filters.get("order_id")),
                ApiUtil.trim(filters.get("status")),
                ApiUtil.toDate(filters.get("from_date")),
                ApiUtil.toDate(filters.get("to_date")),
                pageable
        );
    }

    // ========================
    // CREATE
    // ========================
    @Post
    @Transactional
    public HttpResponse<?> create(@Body DeliveryCreateRequest req) {

        if (req.getOrderId() == null)
            return HttpResponse.badRequest(ApiResponse.error(-400, "order_id is required"));

        if (CommonUtil.isNullOrEmpty(req.getItems()))
            return HttpResponse.badRequest(ApiResponse.error(-400, "items is required"));

        Set<Integer> checkDup = new HashSet<>();

        for (ItemRequest i : req.getItems()) {

            if (i.getProductId() == null)
                return HttpResponse.badRequest(ApiResponse.error(-400, "product_id is required"));

            if (!checkDup.add(i.getProductId()))
                return HttpResponse.badRequest(ApiResponse.error(-400, "duplicate product_id"));

            if (i.getQuantity() == null || i.getQuantity().compareTo(BigDecimal.ZERO) <= 0)
                return HttpResponse.badRequest(ApiResponse.error(-400, "quantity must > 0"));
        }

        Delivery d = new Delivery();

        d.setOrderId(req.getOrderId());
        d.setDeliveryNo(generateDeliveryNo());
        d.setDeliveryDate(ApiUtil.toDate(req.getDeliveryDate()));

        d.setWarehouseId(req.getWarehouseId());
        d.setCompanyId(req.getCompanyId());
        d.setDeliveryAddress(ApiUtil.trim(req.getDeliveryAddress()));

        d.setStatus(req.getStatus() != null ? req.getStatus() : "NEW");
        d.setNote(ApiUtil.trim(req.getNote()));

        d.setCreatedAt(LocalDateTime.now());
        d.setUpdatedAt(LocalDateTime.now());

        d = dao.insert(d);

        List<DeliveryItem> list = new ArrayList<>();

        for (ItemRequest i : req.getItems()) {

            DeliveryItem item = new DeliveryItem();

            item.setDeliveryId(d.getId());
            item.setProductId(i.getProductId());
            item.setQuantity(i.getQuantity());
            item.setNote(i.getNote());

            list.add(item);
        }

        deliveryItemDao.saveAll(list);

        return HttpResponse.ok(ApiResponse.success(d));
    }

    // ========================
    // UPDATE
    // ========================
    @Put("/{id}")
    @Transactional
    public HttpResponse<?> update(@PathVariable Integer id, @Body DeliveryUpdateRequest req) {

        var oldOpt = dao.findById(id);
        if (oldOpt.isEmpty()) {
            return HttpResponse.notFound(ApiResponse.error(-404, "not found"));
        }

        if (CommonUtil.isNullOrEmpty(req.getItems()))
            return HttpResponse.badRequest(ApiResponse.error(-400, "items is required"));

        Set<Integer> checkDup = new HashSet<>();
        for (ItemRequest i : req.getItems()) {

            if (i.getProductId() == null)
                return HttpResponse.badRequest(ApiResponse.error(-400, "product_id is required"));

            if (!checkDup.add(i.getProductId()))
                return HttpResponse.badRequest(ApiResponse.error(-400, "duplicate product_id"));

            if (i.getQuantity() == null || i.getQuantity().compareTo(BigDecimal.ZERO) <= 0)
                return HttpResponse.badRequest(ApiResponse.error(-400, "quantity must > 0"));
        }

        // ===== UPDATE DELIVERY
        Delivery x = new Delivery();

        if (req.getDeliveryDate() != null)
            x.setDeliveryDate(ApiUtil.toDate(req.getDeliveryDate()));

        if (req.getWarehouseId() != null)
            x.setWarehouseId(req.getWarehouseId());

        if (req.getCompanyId() != null)
            x.setCompanyId(req.getCompanyId());

        if (req.getDeliveryAddress() != null)
            x.setDeliveryAddress(ApiUtil.trim(req.getDeliveryAddress()));

        if (req.getStatus() != null)
            x.setStatus(req.getStatus());

        if (req.getNote() != null)
            x.setNote(ApiUtil.trim(req.getNote()));

        x.setUpdatedAt(LocalDateTime.now());

        dao.updateSelective(id, x);

        // ===== ITEMS UPSERT
        List<DeliveryItem> oldItems = deliveryItemDao.findByDeliveryId(id);

        Map<Integer, DeliveryItem> oldMap = oldItems.stream()
                .collect(Collectors.toMap(DeliveryItem::getProductId, i -> i));

        Set<Integer> newProductIds = new HashSet<>();
        List<DeliveryItem> toInsert = new ArrayList<>();

        for (ItemRequest i : req.getItems()) {

            newProductIds.add(i.getProductId());

            DeliveryItem old = oldMap.get(i.getProductId());

            if (old != null) {

                DeliveryItem update = new DeliveryItem();

                update.setProductId(i.getProductId());
                update.setQuantity(i.getQuantity());
                update.setNote(i.getNote());

                deliveryItemDao.updateSelective(old.getId(), update);

            } else {

                DeliveryItem insert = new DeliveryItem();

                insert.setDeliveryId(id);
                insert.setProductId(i.getProductId());
                insert.setQuantity(i.getQuantity());
                insert.setNote(i.getNote());

                toInsert.add(insert);
            }
        }

        for (DeliveryItem old : oldItems) {
            if (!newProductIds.contains(old.getProductId())) {
                deliveryItemDao.deleteById(old.getId());
            }
        }

        if (!toInsert.isEmpty()) {
            deliveryItemDao.saveAll(toInsert);
        }

        return HttpResponse.ok(ApiResponse.success(true));
    }

    @Post("/{id}/confirm")
    @Transactional
    public HttpResponse<?> confirm(@PathVariable Integer id) {

        try {
            deliveryService.confirmDelivery(id);
            return HttpResponse.ok(ApiResponse.success(true));

        } catch (Exception e) {
            return HttpResponse.badRequest(ApiResponse.error(-400, e.getMessage()));
        }
    }

    private String generateDeliveryNo() {

        String prefix = "PX-" + java.time.LocalDate.now()
                .format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"));

        Integer count = dao.countByPrefix(prefix);
        int next = count + 1;

        return prefix + "-" + String.format("%03d", next);
    }

    // ========================
    // DTO
    // ========================
    @Setter
    @Getter
    @Serdeable
    public static class DeliveryCreateRequest {

        @JsonProperty("order_id")
        private Integer orderId;

        @JsonProperty("delivery_date")
        private String deliveryDate;

        @JsonProperty("warehouse_id")
        private Integer warehouseId;

        @JsonProperty("company_id")
        private Integer companyId;

        @JsonProperty("delivery_address")
        private String deliveryAddress;

        private String status;
        private String note;

        private List<ItemRequest> items;
    }

    @Setter
    @Getter
    @Serdeable
    public static class DeliveryUpdateRequest extends DeliveryCreateRequest {}

    @Setter
    @Getter
    @Serdeable
    public static class ItemRequest {

        @JsonProperty("product_id")
        private Integer productId;

        private BigDecimal quantity;

        private String note;
    }
}