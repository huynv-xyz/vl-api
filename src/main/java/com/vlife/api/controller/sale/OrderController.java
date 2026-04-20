package com.vlife.api.controller.sale;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.vlife.api.builder.sale.OrderBuilder;
import com.vlife.api.controller.base.BaseCrudController;
import com.vlife.api.util.ApiUtil;
import com.vlife.shared.api.dto.ApiResponse;
import com.vlife.shared.jdbc.dao.sale.OrderDao;
import com.vlife.shared.jdbc.dao.sale.OrderItemDao;
import com.vlife.shared.jdbc.entity.sale.Order;
import com.vlife.shared.jdbc.entity.sale.OrderItem;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Controller("/sales/orders")
public class OrderController extends BaseCrudController<Order, Integer, OrderDao> {

    private final OrderItemDao orderItemDao;

    @Inject
    public OrderController(OrderDao dao,
                           OrderBuilder builder,
                           OrderItemDao orderItemDao) {
        super(dao, builder);
        this.orderItemDao = orderItemDao;
    }

    @Override
    protected Page<Order> doSearch(Map<String, String> filters, Pageable pageable) {
        return dao.search(
                ApiUtil.trim(filters.get("keyword")),
                ApiUtil.parseInteger(filters.get("customer_id")),
                ApiUtil.parseInteger(filters.get("employee_id")),
                ApiUtil.trim(filters.get("status")),
                ApiUtil.toDate(filters.get("from_date")),
                ApiUtil.toDate(filters.get("to_date")),
                pageable
        );
    }


    @Post
    @Transactional
    public HttpResponse<?> create(@Body OrderCreateRequest req) {

        if (req.getCustomerId() == null)
            return HttpResponse.badRequest(ApiResponse.error(-400, "customer_id is required"));

        if (CommonUtil.isNullOrEmpty(req.getItems()))
            return HttpResponse.badRequest(ApiResponse.error(-400, "items is required"));

        Set<Integer> checkDup = new HashSet<>();

        for (OrderItemRequest i : req.getItems()) {

            if (i.getProductId() == null)
                return HttpResponse.badRequest(ApiResponse.error(-400, "product_id is required"));

            if (!checkDup.add(i.getProductId()))
                return HttpResponse.badRequest(ApiResponse.error(-400, "duplicate product_id"));

            if (i.getQuantity() == null || i.getQuantity().compareTo(BigDecimal.ZERO) <= 0)
                return HttpResponse.badRequest(ApiResponse.error(-400, "quantity must > 0"));
        }

        Order order = new Order();

        order.setOrderNo(generateOrderNo());
        order.setCustomerId(req.getCustomerId());
        order.setEmployeeId(req.getEmployeeId());
        order.setOrderDate(ApiUtil.toDate(req.getOrderDate()));
        order.setStatus(req.getStatus() != null ? req.getStatus() : "NEW");
        order.setNote(ApiUtil.trim(req.getNote()));
        order.setCreatedAt(LocalDateTime.now());
        order.setUpdatedAt(LocalDateTime.now());

        order = dao.insert(order);

        List<OrderItem> list = new ArrayList<>();

        for (OrderItemRequest i : req.getItems()) {

            OrderItem item = new OrderItem();

            item.setOrderId(order.getId());
            item.setProductId(i.getProductId());
            item.setQuantity(i.getQuantity());
            item.setUnitPrice(i.getUnitPrice());
            item.setDiscount(i.getDiscount() != null ? i.getDiscount() : BigDecimal.ZERO);
            item.setLineType(i.getLineType() != null ? i.getLineType() : "NORMAL");

            list.add(item);
        }

        orderItemDao.saveAll(list);

        return HttpResponse.ok(ApiResponse.success(order));
    }

    @Put("/{id}")
    @Transactional
    public HttpResponse<?> update(@PathVariable Integer id, @Body OrderUpdateRequest req) {

        var oldOpt = dao.findById(id);
        if (oldOpt.isEmpty()) {
            return HttpResponse.notFound(ApiResponse.error(-404, "not found"));
        }

        // ===== VALIDATE
        if (req.getCustomerId() == null)
            return HttpResponse.badRequest(ApiResponse.error(-400, "customer_id is required"));

        if (CommonUtil.isNullOrEmpty(req.getItems()))
            return HttpResponse.badRequest(ApiResponse.error(-400, "items is required"));

        Set<Integer> checkDup = new HashSet<>();
        for (OrderItemRequest i : req.getItems()) {

            if (i.getProductId() == null)
                return HttpResponse.badRequest(ApiResponse.error(-400, "product_id is required"));

            if (!checkDup.add(i.getProductId()))
                return HttpResponse.badRequest(ApiResponse.error(-400, "duplicate product_id"));

            if (i.getQuantity() == null || i.getQuantity().compareTo(BigDecimal.ZERO) <= 0)
                return HttpResponse.badRequest(ApiResponse.error(-400, "quantity must > 0"));
        }

        // ===== UPDATE ORDER (SAFE)
        Order x = new Order();

        if (req.getCustomerId() != null)
            x.setCustomerId(req.getCustomerId());

        if (req.getEmployeeId() != null)
            x.setEmployeeId(req.getEmployeeId());

        if (req.getOrderDate() != null)
            x.setOrderDate(ApiUtil.toDate(req.getOrderDate()));

        if (req.getStatus() != null)
            x.setStatus(req.getStatus());

        if (req.getNote() != null)
            x.setNote(ApiUtil.trim(req.getNote()));

        x.setUpdatedAt(LocalDateTime.now());

        dao.updateSelective(id, x);

        // ===== LOAD OLD ITEMS
        List<OrderItem> oldItems = orderItemDao.findByOrderId(id);

        Map<Integer, OrderItem> oldMap = oldItems.stream()
                .collect(Collectors.toMap(OrderItem::getProductId, i -> i));

        Set<Integer> newProductIds = new HashSet<>();
        List<OrderItem> toInsert = new ArrayList<>();

        // ===== UPSERT
        for (OrderItemRequest i : req.getItems()) {

            newProductIds.add(i.getProductId());

            OrderItem old = oldMap.get(i.getProductId());

            if (old != null) {
                // ===== UPDATE (FIX Ở ĐÂY)
                OrderItem update = new OrderItem();

                update.setProductId(i.getProductId());
                update.setQuantity(i.getQuantity());
                update.setUnitPrice(i.getUnitPrice());
                update.setDiscount(i.getDiscount() != null ? i.getDiscount() : BigDecimal.ZERO);
                update.setLineType(i.getLineType() != null ? i.getLineType() : "NORMAL");

                orderItemDao.updateSelective(old.getId(), update); // 🔥 FIX CHÍNH

            } else {
                // ===== INSERT
                OrderItem insert = new OrderItem();

                insert.setOrderId(id);
                insert.setProductId(i.getProductId());
                insert.setQuantity(i.getQuantity());
                insert.setUnitPrice(i.getUnitPrice());
                insert.setDiscount(i.getDiscount() != null ? i.getDiscount() : BigDecimal.ZERO);
                insert.setLineType(i.getLineType() != null ? i.getLineType() : "NORMAL");

                toInsert.add(insert);
            }
        }

        // ===== DELETE REMOVED
        for (OrderItem old : oldItems) {
            if (!newProductIds.contains(old.getProductId())) {
                orderItemDao.deleteById(old.getId());
            }
        }

        // ===== BATCH INSERT
        if (!toInsert.isEmpty()) {
            orderItemDao.saveAll(toInsert);
        }

        return HttpResponse.ok(ApiResponse.success(true));
    }

    private String generateOrderNo() {

        String prefix = "DH-" + java.time.LocalDate.now()
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
    public static class OrderCreateRequest {

        @JsonProperty("customer_id")
        private Integer customerId;

        @JsonProperty("employee_id")
        private Integer employeeId;

        @JsonProperty("order_date")
        private String orderDate;

        private String status;

        private String note;

        private List<OrderItemRequest> items;
    }

    @Setter
    @Getter
    @Serdeable
    public static class OrderUpdateRequest extends OrderCreateRequest {}

    @Setter
    @Getter
    @Serdeable
    public static class OrderItemRequest {

        @JsonProperty("product_id")
        private Integer productId;

        private BigDecimal quantity;

        @JsonProperty("unit_price")
        private BigDecimal unitPrice;

        private BigDecimal discount;

        @JsonProperty("line_type")
        private String lineType;
    }
}