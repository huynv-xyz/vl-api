package com.vlife.api.builder.sale;

import com.vlife.shared.api.builder.ItemBuilder;
import com.vlife.shared.jdbc.dao.CustomerDao;
import com.vlife.shared.jdbc.dao.sale.OrderDao;
import com.vlife.shared.jdbc.entity.Customer;
import com.vlife.shared.jdbc.entity.sale.Order;
import com.vlife.shared.jdbc.entity.sale.Receipt;
import com.vlife.shared.util.CommonUtil;
import jakarta.inject.Singleton;

import java.util.*;
import java.util.stream.Collectors;

@Singleton
public class ReceiptBuilder extends ItemBuilder<Receipt> {

    private final OrderDao orderDao;
    private final CustomerDao customerDao;

    public ReceiptBuilder(
            OrderDao orderDao,
            CustomerDao customerDao
    ) {
        this.orderDao = orderDao;
        this.customerDao = customerDao;
    }

    @Override
    public Map<String, Object> buildItemFull(Receipt item) {

        if (item == null) return Map.of();

        Map<String, Object> x = new LinkedHashMap<>(autoBuild(item));

        // ===== order
        x.put("order",
                item.getOrderId() != null
                        ? orderDao.findById(item.getOrderId()).orElse(null)
                        : null
        );

        // ===== customer
        x.put("customer",
                item.getCustomerId() != null
                        ? customerDao.findById(item.getCustomerId()).orElse(null)
                        : null
        );

        return x;
    }

    @Override
    public List<Map<String, Object>> buildList(List<Receipt> items) {

        if (CommonUtil.isNullOrEmpty(items)) {
            return Collections.emptyList();
        }

        // ===== collect ids
        Set<Integer> orderIds = items.stream()
                .map(Receipt::getOrderId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Set<Integer> customerIds = items.stream()
                .map(Receipt::getCustomerId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        // ===== batch load
        Map<Integer, Order> orderMap = orderDao.findByIdsAsMap(orderIds);
        Map<Integer, Customer> customerMap = customerDao.findByIdsAsMap(customerIds);

        List<Map<String, Object>> list = new ArrayList<>(items.size());

        for (Receipt item : items) {

            Map<String, Object> x = new LinkedHashMap<>(autoBuild(item));

            x.put("order", orderMap.get(item.getOrderId()));
            x.put("customer", customerMap.get(item.getCustomerId()));

            list.add(x);
        }

        return list;
    }
}