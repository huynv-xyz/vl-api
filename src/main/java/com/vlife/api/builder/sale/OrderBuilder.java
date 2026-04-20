package com.vlife.api.builder.sale;

import com.vlife.api.builder.ProductBuilder;
import com.vlife.shared.api.builder.ItemBuilder;
import com.vlife.shared.jdbc.dao.CustomerDao;
import com.vlife.shared.jdbc.dao.EmployeeDao;
import com.vlife.shared.jdbc.dao.ProductDao;
import com.vlife.shared.jdbc.dao.sale.OrderItemDao;
import com.vlife.shared.jdbc.entity.Customer;
import com.vlife.shared.jdbc.entity.Employee;
import com.vlife.shared.jdbc.entity.Product;
import com.vlife.shared.jdbc.entity.sale.Order;
import com.vlife.shared.jdbc.entity.sale.OrderItem;
import com.vlife.shared.util.CommonUtil;
import jakarta.inject.Singleton;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Singleton
public class OrderBuilder extends ItemBuilder<Order> {

    private final CustomerDao customerDao;
    private final EmployeeDao employeeDao;

    private final OrderItemDao orderItemDao;
    private final ProductDao productDao;
    private final ProductBuilder productBuilder;

    public OrderBuilder(
            CustomerDao customerDao,
            EmployeeDao employeeDao,
            OrderItemDao orderItemDao,
            ProductDao productDao,
            ProductBuilder productBuilder
    ) {
        this.customerDao = customerDao;
        this.employeeDao = employeeDao;
        this.orderItemDao = orderItemDao;
        this.productDao = productDao;
        this.productBuilder = productBuilder;
    }

    // ========================
    // BUILD SINGLE (DETAIL)
    // ========================
    @Override
    public Map<String, Object> buildItem(Order item) {
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

        // ===== load items
        List<OrderItem> items = orderItemDao.findByOrderId(item.getId());

        // ===== load product map
        Set<Integer> productIds = items.stream()
                .map(OrderItem::getProductId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<Integer, Product> productMap =
                productIds.isEmpty()
                        ? Collections.emptyMap()
                        : productDao.findByIdsAsMap(productIds);

        List<Map<String, Object>> itemRes = new ArrayList<>();

        BigDecimal total = BigDecimal.ZERO;

        for (OrderItem oi : items) {

            Map<String, Object> m = new LinkedHashMap<>(autoBuildAny(oi));

            Product p = productMap.get(oi.getProductId());
            if (p != null) {
                m.put("product", productBuilder.buildItem(p));
            }

            // ===== calc line total
            BigDecimal lineTotal =
                    oi.getQuantity()
                            .multiply(oi.getUnitPrice())
                            .subtract(Optional.ofNullable(oi.getDiscount()).orElse(BigDecimal.ZERO));

            m.put("line_total", lineTotal);

            total = total.add(lineTotal);

            itemRes.add(m);
        }

        x.put("items", itemRes);
        x.put("total_amount", total);

        return x;
    }

    // ========================
    // BUILD LIST (OPTIMIZE)
    // ========================
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