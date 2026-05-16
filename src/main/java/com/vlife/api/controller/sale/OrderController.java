package com.vlife.api.controller.sale;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.vlife.api.builder.sale.OrderBuilder;
import com.vlife.api.controller.base.BaseCrudController;
import com.vlife.api.util.ApiUtil;
import com.vlife.shared.api.dto.ApiResponse;
import com.vlife.shared.jdbc.dao.sale.InventoryLotDao;
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
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Controller("/sales/orders")
public class OrderController extends BaseCrudController<Order, Integer, OrderDao> {

    private final OrderItemDao orderItemDao;
    private final InventoryLotDao inventoryLotDao;

    @Inject
    public OrderController(OrderDao dao,
                           OrderBuilder builder,
                           OrderItemDao orderItemDao,
                           InventoryLotDao inventoryLotDao) {
        super(dao, builder);
        this.orderItemDao = orderItemDao;
        this.inventoryLotDao = inventoryLotDao;
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

    @Post("/{id}/items")
    @Transactional
    public HttpResponse<?> addItem(
            @PathVariable Integer id,
            @Body OrderItemRequest req
    ) {

        var orderOpt = dao.findById(id);
        if (orderOpt.isEmpty()) {
            return HttpResponse.notFound(ApiResponse.error(-404, "order not found"));
        }

        Order order = orderOpt.get();

        if ("DONE".equals(order.getStatus())) {
            return HttpResponse.badRequest(ApiResponse.error(-400, "Order already DONE"));
        }

        if (req.getProductId() == null) {
            return HttpResponse.badRequest(ApiResponse.error(-400, "product_id is required"));
        }

        if (req.getQuantity() == null || req.getQuantity().compareTo(BigDecimal.ZERO) <= 0) {
            return HttpResponse.badRequest(ApiResponse.error(-400, "quantity must > 0"));
        }

        // check duplicate
        boolean exists = orderItemDao.existsByOrderIdAndProductId(id, req.getProductId());
        if (exists) {
            return HttpResponse.badRequest(ApiResponse.error(-400, "product already exists"));
        }

        /*BigDecimal available =
                inventoryLotDao.getAvailableQuantity(req.getProductId(), req.getWarehouseId());

        if (available.compareTo(req.getQuantity()) < 0) {
            return HttpResponse.badRequest(
                    ApiResponse.error(-400,
                            "Không đủ tồn kho. Tồn: " + available +
                                    ", yêu cầu: " + req.getQuantity())
            );
        }*/

        OrderItem item = new OrderItem();

        item.setOrderId(id);
        item.setProductId(req.getProductId());
        item.setQuantity(req.getQuantity());
        item.setUnitPrice(req.getUnitPrice());
        item.setDiscount(req.getDiscount() != null ? req.getDiscount() : BigDecimal.ZERO);
        item.setLineType(req.getLineType() != null ? req.getLineType() : "NORMAL");

        orderItemDao.insert(item);

        return HttpResponse.ok(ApiResponse.success(true));
    }

    @Put("/items/{id}")
    @Transactional
    public HttpResponse<?> updateItem(
            @PathVariable Integer id,
            @Body OrderItemRequest req
    ) {

        OrderItem item = orderItemDao.findById(id)
                .orElseThrow(() -> new RuntimeException("item not found"));

        Order order = dao.findById(item.getOrderId())
                .orElseThrow(() -> new RuntimeException("order not found"));

        if ("DONE".equals(order.getStatus())) {
            return HttpResponse.badRequest(ApiResponse.error(-400, "Order already DONE"));
        }

        if (req.getQuantity() == null || req.getQuantity().compareTo(BigDecimal.ZERO) <= 0) {
            return HttpResponse.badRequest(ApiResponse.error(-400, "quantity must > 0"));
        }

        OrderItem update = new OrderItem();
        update.setQuantity(req.getQuantity());
        update.setUnitPrice(req.getUnitPrice());
        update.setDiscount(req.getDiscount() != null ? req.getDiscount() : BigDecimal.ZERO);
        update.setLineType(req.getLineType() != null ? req.getLineType() : "NORMAL");

        orderItemDao.updateSelective(id, update);

        return HttpResponse.ok(ApiResponse.success(true));
    }

    @Delete("/items/{id}")
    @Transactional
    public HttpResponse<?> deleteItem(@PathVariable Integer id) {

        OrderItem item = orderItemDao.findById(id)
                .orElseThrow(() -> new RuntimeException("item not found"));

        Order order = dao.findById(item.getOrderId())
                .orElseThrow(() -> new RuntimeException("order not found"));

        if ("DONE".equals(order.getStatus())) {
            return HttpResponse.badRequest(ApiResponse.error(-400, "Order already DONE"));
        }

        orderItemDao.deleteById(id);

        return HttpResponse.ok(ApiResponse.success(true));
    }

    @Put("/{id}/status")
    @Transactional
    public HttpResponse<?> updateStatus(
            @PathVariable Integer id,
            @Body UpdateStatusRequest req
    ) {

        var opt = dao.findById(id);
        if (opt.isEmpty()) {
            return HttpResponse.notFound(ApiResponse.error(-404, "order not found"));
        }

        Order order = opt.get();

        if (req.getStatus() == null || req.getStatus().isBlank()) {
            return HttpResponse.badRequest(ApiResponse.error(-400, "status is required"));
        }

        // 🔥 RULE: DONE thì không cho đổi nữa
        if ("DONE".equals(order.getStatus())) {
            return HttpResponse.badRequest(ApiResponse.error(-400, "Order already DONE"));
        }

        if (!isValidTransition(order.getStatus(), req.getStatus())) {
            return HttpResponse.badRequest(ApiResponse.error(-400, "Invalid status transition"));
        }

        Order update = new Order();
        update.setStatus(req.getStatus());
        update.setUpdatedAt(LocalDateTime.now());

        dao.updateSelective(id, update);

        return HttpResponse.ok(ApiResponse.success(true));
    }

    private boolean isValidTransition(String from, String to) {

        Map<String, List<String>> flow = Map.of(
                "NEW", List.of("CONFIRMED", "CANCELLED"),
                "CONFIRMED", List.of("DONE", "CANCELLED"),
                "DONE", List.of(),
                "CANCELLED", List.of()
        );

        return flow.getOrDefault(from, List.of()).contains(to);
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

    @Setter
    @Getter
    @Serdeable
    public static class UpdateStatusRequest {

        private String status;
    }
}