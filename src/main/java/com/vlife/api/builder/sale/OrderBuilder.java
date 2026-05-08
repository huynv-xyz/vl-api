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
            ReceiptDao receiptDao
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
    }

    @Override
    public Map<String, Object> buildItemFull(Order item) {
        if (item == null) return Map.of();

        Map<String, Object> x = new LinkedHashMap<>(autoBuild(item));

        Customer customer = item.getCustomerId() != null
                ? customerDao.findById(item.getCustomerId()).orElse(null)
                : null;

        Employee employee = item.getEmployeeId() != null
                ? employeeDao.findById(item.getEmployeeId()).orElse(null)
                : null;

        x.put("customer", customer);
        x.put("employee", employee);

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

        Set<Integer> warehouseIds = new HashSet<>();

        for (Delivery d : deliveries) {
            if (d.getWarehouseId() != null) {
                warehouseIds.add(d.getWarehouseId());
            }
        }

        for (Export e : exports) {
            if (e.getWarehouseId() != null) {
                warehouseIds.add(e.getWarehouseId());
            }
        }

        Map<Integer, BigDecimal> exportedMap =
                exportItemDao.sumExportedByOrderId(item.getId());

        Map<Integer, Warehouse> warehouseMap =
                warehouseIds.isEmpty()
                        ? Collections.emptyMap()
                        : warehouseDao.findByIdsAsMap(warehouseIds);

        List<Map<String, Object>> itemRes = new ArrayList<>();

        BigDecimal total = BigDecimal.ZERO;

        for (OrderItem oi : items) {

            Map<String, Object> m = new LinkedHashMap<>(autoBuildAny(oi));

            Product p = productMap.get(oi.getProductId());
            if (p != null) {
                m.put("product", productBuilder.buildItem(p));
            }

            BigDecimal exportedQty =
                    exportedMap.getOrDefault(oi.getProductId(), BigDecimal.ZERO);

            BigDecimal remain =
                    oi.getQuantity().subtract(exportedQty);

            BigDecimal discount =
                    Optional.ofNullable(oi.getDiscount()).orElse(BigDecimal.ZERO);

            BigDecimal lineTotal =
                    oi.getQuantity()
                            .multiply(oi.getUnitPrice())
                            .subtract(discount);

            m.put("exported_quantity", exportedQty);
            m.put("remain_quantity", remain);
            m.put("line_total", lineTotal);

            total = total.add(lineTotal);

            itemRes.add(m);
        }

        x.put("items", itemRes);
        x.put("total_amount", total);

        // ========================
        // DELIVERY
        // ========================

        List<Map<String, Object>> deliveryRes = new ArrayList<>();

        for (Delivery d : deliveries) {

            Map<String, Object> dm = new LinkedHashMap<>(autoBuildAny(d));
            dm.put("warehouse", warehouseMap.get(d.getWarehouseId()));

            List<DeliveryItem> dis = deliveryItemDao.findByDeliveryId(d.getId());

            dm.put("items", deliveryItemBuilder.buildList(dis));

            deliveryRes.add(dm);
        }

        x.put("deliveries", deliveryRes);

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

            List<ExportItem> eis = exportItemMap.getOrDefault(e.getId(), List.of());

            List<Map<String, Object>> eisRes = new ArrayList<>();

            for (ExportItem ei : eis) {

                Map<String, Object> im = new LinkedHashMap<>(autoBuildAny(ei));

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

        List<Receipt> receipts = receiptDao.findByOrderId(item.getId());

        List<Map<String, Object>> receiptRes = new ArrayList<>();

        for (Receipt r : receipts) {
            Map<String, Object> rm = new LinkedHashMap<>(autoBuildAny(r));
            receiptRes.add(rm);
        }

        x.put("receipts", receiptRes);

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