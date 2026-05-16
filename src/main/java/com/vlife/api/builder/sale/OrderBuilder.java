package com.vlife.api.builder.sale;

import com.vlife.api.builder.ProductBuilder;
import com.vlife.shared.api.builder.ItemBuilder;
import com.vlife.shared.jdbc.dao.CustomerDao;
import com.vlife.shared.jdbc.dao.EmployeeDao;
import com.vlife.shared.jdbc.dao.ProductDao;
import com.vlife.shared.jdbc.dao.WarehouseDao;
import com.vlife.shared.jdbc.dao.sale.*;
import com.vlife.shared.jdbc.entity.Customer;
import com.vlife.shared.jdbc.entity.Employee;
import com.vlife.shared.jdbc.entity.Product;
import com.vlife.shared.jdbc.entity.Warehouse;
import com.vlife.shared.jdbc.entity.sale.*;
import com.vlife.shared.util.CommonUtil;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Singleton
public class OrderBuilder extends ItemBuilder<Order> {

    private final CustomerDao customerDao;
    private final EmployeeDao employeeDao;
    private final DeliveryDao deliveryDao;
    private final DeliveryItemDao deliveryItemDao;

    private final ExportDao exportDao;
    private final ExportItemDao exportItemDao;

    private final ArLedgerDao arLedgerDao;
    private final OrderItemDao orderItemDao;
    private final ReceiptDao receiptDao;

    private final ProductDao productDao;
    private final ProductBuilder productBuilder;

    private final ReturnDao returnDao;
    private final ReturnItemDao returnItemDao;


    @Inject
    DeliveryItemBuilder deliveryItemBuilder;

    @Inject
    WarehouseDao warehouseDao;

    public OrderBuilder(
            CustomerDao customerDao,
            EmployeeDao employeeDao,
            OrderItemDao orderItemDao,
            ProductDao productDao,
            ProductBuilder productBuilder,
            DeliveryDao deliveryDao,
            DeliveryItemDao deliveryItemDao,
            ExportDao exportDao,
            ExportItemDao exportItemDao,
            ArLedgerDao arLedgerDao,
            ReceiptDao receiptDao,
            ReturnDao returnDao,
            ReturnItemDao returnItemDao
    ) {
        this.customerDao = customerDao;
        this.employeeDao = employeeDao;
        this.orderItemDao = orderItemDao;
        this.productDao = productDao;
        this.productBuilder = productBuilder;

        this.deliveryDao = deliveryDao;
        this.deliveryItemDao = deliveryItemDao;
        this.exportDao = exportDao;
        this.exportItemDao = exportItemDao;
        this.arLedgerDao = arLedgerDao;
        this.receiptDao = receiptDao;
        this.returnDao = returnDao;
        this.returnItemDao = returnItemDao;
    }

    @Override
    public Map<String, Object> buildItemFull(Order item) {

        if (item == null) return Map.of();

        Map<String, Object> x = new LinkedHashMap<>(autoBuild(item));

        // ===== BASIC
        x.put("customer",
                Optional.ofNullable(item.getCustomerId())
                        .flatMap(customerDao::findById)
                        .orElse(null)
        );

        x.put("employee",
                Optional.ofNullable(item.getEmployeeId())
                        .flatMap(employeeDao::findById)
                        .orElse(null)
        );

        // ===== LOAD
        List<OrderItem> items = orderItemDao.findByOrderId(item.getId());
        List<Delivery> deliveries = deliveryDao.findByOrderId(item.getId());
        List<Export> exports = exportDao.findByOrderId(item.getId());

        Set<Integer> productIds = items.stream()
                .map(OrderItem::getProductId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<Integer, Product> productMap =
                productIds.isEmpty()
                        ? Collections.emptyMap()
                        : productDao.findByIdsAsMap(productIds);

        Map<Integer, BigDecimal> exportedMap =
                exportItemDao.sumExportedByOrderId(item.getId());

        Map<Integer, BigDecimal> returnedMap =
                returnItemDao.sumReturnedByOrderId(item.getId());

        // ===== ITEMS
        List<Map<String, Object>> itemRes = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;

        for (OrderItem oi : items) {

            Integer productId = oi.getProductId();

            Map<String, Object> m = new LinkedHashMap<>(autoBuildAny(oi));

            Product p = productMap.get(productId);
            if (p != null) {
                m.put("product", productBuilder.buildItem(p));
            }

            BigDecimal exportedQty =
                    exportedMap.getOrDefault(productId, BigDecimal.ZERO);

            BigDecimal returnedQty =
                    returnedMap.getOrDefault(productId, BigDecimal.ZERO);

            // ✅ remain chuẩn
            BigDecimal remain =
                    oi.getQuantity().subtract(exportedQty);

            if (remain.compareTo(BigDecimal.ZERO) < 0) {
                remain = BigDecimal.ZERO;
            }

            // optional
            BigDecimal realExported =
                    exportedQty.subtract(returnedQty);

            if (realExported.compareTo(BigDecimal.ZERO) < 0) {
                realExported = BigDecimal.ZERO;
            }

            BigDecimal discount =
                    Optional.ofNullable(oi.getDiscount()).orElse(BigDecimal.ZERO);

            BigDecimal lineTotal =
                    oi.getQuantity()
                            .multiply(oi.getUnitPrice())
                            .subtract(discount);

            m.put("exported_quantity", exportedQty);
            m.put("returned_quantity", returnedQty);
            m.put("real_exported_quantity", realExported);
            m.put("remain_quantity", remain);
            m.put("line_total", lineTotal);

            total = total.add(lineTotal);

            itemRes.add(m);
        }

        x.put("items", itemRes);
        x.put("total_amount", total);

        // ===== WAREHOUSE MAP
        Set<Integer> warehouseIds = new HashSet<>();

        deliveries.forEach(d -> {
            if (d.getWarehouseId() != null) warehouseIds.add(d.getWarehouseId());
        });

        exports.forEach(e -> {
            if (e.getWarehouseId() != null) warehouseIds.add(e.getWarehouseId());
        });

        Map<Integer, Warehouse> warehouseMap =
                warehouseIds.isEmpty()
                        ? Collections.emptyMap()
                        : warehouseDao.findByIdsAsMap(warehouseIds);

        // ===== DELIVERY
        List<Map<String, Object>> deliveryRes = new ArrayList<>();

        for (Delivery d : deliveries) {

            Map<String, Object> dm = new LinkedHashMap<>(autoBuildAny(d));
            dm.put("warehouse", warehouseMap.get(d.getWarehouseId()));

            List<DeliveryItem> dis = deliveryItemDao.findByDeliveryId(d.getId());
            dm.put("items", deliveryItemBuilder.buildList(dis));

            deliveryRes.add(dm);
        }

        x.put("deliveries", deliveryRes);

        // ===== EXPORT
        Set<Integer> exportIds = exports.stream()
                .map(Export::getId)
                .collect(Collectors.toSet());

        Map<Integer, List<ExportItem>> exportItemMap =
                exportIds.isEmpty()
                        ? Collections.emptyMap()
                        : exportItemDao.findByExportIds(exportIds)
                        .stream()
                        .collect(Collectors.groupingBy(ExportItem::getExportId));

        List<Map<String, Object>> exportRes = new ArrayList<>();

        for (Export e : exports) {

            Map<String, Object> em = new LinkedHashMap<>(autoBuildAny(e));
            em.put("warehouse", warehouseMap.get(e.getWarehouseId()));

            List<ExportItem> eis =
                    exportItemMap.getOrDefault(e.getId(), List.of());

            List<Map<String, Object>> eisRes = new ArrayList<>();

            for (ExportItem ei : eis) {

                Map<String, Object> im =
                        new LinkedHashMap<>(autoBuildAny(ei));

                Product p = productMap.get(ei.getProductId());
                if (p != null) {
                    im.put("product", productBuilder.buildItem(p));
                }

                eisRes.add(im);
            }

            em.put("items", eisRes);
            exportRes.add(em);
        }

        x.put("exports", exportRes);

        // ===== RECEIPT
        x.put("receipts",
                receiptDao.findByOrderId(item.getId())
                        .stream()
                        .map(r -> new LinkedHashMap<>(autoBuildAny(r)))
                        .toList()
        );

        // ===== RETURN
        List<Return> returns = returnDao.findByOrderId(item.getId());

        Set<Integer> returnIds = returns.stream()
                .map(Return::getId)
                .collect(Collectors.toSet());

        Map<Integer, List<ReturnItem>> returnItemMap =
                returnIds.isEmpty()
                        ? Collections.emptyMap()
                        : returnItemDao.findByReturnIds(returnIds)
                        .stream()
                        .collect(Collectors.groupingBy(ReturnItem::getReturnId));

        List<Map<String, Object>> returnRes = new ArrayList<>();

        for (Return r : returns) {

            Map<String, Object> rm = new LinkedHashMap<>(autoBuildAny(r));

            List<ReturnItem> ris =
                    returnItemMap.getOrDefault(r.getId(), List.of());

            List<Map<String, Object>> risRes = new ArrayList<>();

            for (ReturnItem ri : ris) {

                Map<String, Object> im =
                        new LinkedHashMap<>(autoBuildAny(ri));

                Product p = productMap.get(ri.getProductId());
                if (p != null) {
                    im.put("product", productBuilder.buildItem(p));
                }

                risRes.add(im);
            }

            rm.put("items", risRes);
            returnRes.add(rm);
        }

        x.put("returns", returnRes);

        return x;
    }

    @Override
    public List<Map<String, Object>> buildList(List<Order> items) {

        if (CommonUtil.isNullOrEmpty(items)) {
            return Collections.emptyList();
        }

        // ===== collect ids
        Set<Integer> customerIds = items.stream()
                .map(Order::getCustomerId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Set<Integer> employeeIds = items.stream()
                .map(Order::getEmployeeId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Set<Integer> orderIds = items.stream()
                .map(Order::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<Integer, Customer> customerMap = customerDao.findByIdsAsMap(customerIds);
        Map<Integer, Employee> employeeMap = employeeDao.findByIdsAsMap(employeeIds);

        Map<Integer, List<OrderItem>> itemMap =
                orderItemDao.findByOrderIds(orderIds)
                        .stream()
                        .collect(Collectors.groupingBy(OrderItem::getOrderId));

        Set<Integer> productIds = itemMap.values().stream()
                .flatMap(List::stream)
                .map(OrderItem::getProductId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<Integer, Product> productMap =
                productIds.isEmpty()
                        ? Collections.emptyMap()
                        : productDao.findByIdsAsMap(productIds);

        // ===== build result
        List<Map<String, Object>> list = new ArrayList<>(items.size());

        for (Order item : items) {

            Map<String, Object> x = new LinkedHashMap<>(autoBuild(item));

            x.put("customer", customerMap.get(item.getCustomerId()));
            x.put("employee", employeeMap.get(item.getEmployeeId()));

            List<OrderItem> its = itemMap.getOrDefault(item.getId(), List.of());

            BigDecimal total = BigDecimal.ZERO;

            for (OrderItem oi : its) {
                BigDecimal lineTotal =
                        oi.getQuantity()
                                .multiply(oi.getUnitPrice())
                                .subtract(Optional.ofNullable(oi.getDiscount()).orElse(BigDecimal.ZERO));

                total = total.add(lineTotal);
            }

            x.put("total_amount", total);

            list.add(x);
        }

        return list;
    }
}