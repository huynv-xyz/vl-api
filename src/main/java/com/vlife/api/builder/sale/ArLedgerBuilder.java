package com.vlife.api.builder.sale;

import com.vlife.shared.api.builder.ItemBuilder;
import com.vlife.shared.jdbc.dao.CustomerDao;
import com.vlife.shared.jdbc.dao.ProductDao;
import com.vlife.shared.jdbc.dao.sale.ExportDao;
import com.vlife.shared.jdbc.dao.sale.OrderDao;
import com.vlife.shared.jdbc.entity.Customer;
import com.vlife.shared.jdbc.entity.Product;
import com.vlife.shared.jdbc.entity.sale.ArLedger;
import com.vlife.shared.jdbc.entity.sale.Export;
import com.vlife.shared.jdbc.entity.sale.Order;
import com.vlife.shared.util.CommonUtil;
import jakarta.inject.Singleton;

import java.util.*;
import java.util.stream.Collectors;

@Singleton
public class ArLedgerBuilder extends ItemBuilder<ArLedger> {

    private final OrderDao orderDao;
    private final ExportDao exportDao;
    private final ProductDao productDao;
    private final CustomerDao customerDao;

    public ArLedgerBuilder(
            OrderDao orderDao,
            ExportDao exportDao,
            ProductDao productDao,
            CustomerDao customerDao
    ) {
        this.orderDao = orderDao;
        this.exportDao = exportDao;
        this.productDao = productDao;
        this.customerDao = customerDao;
    }

    // ========================
    // DETAIL
    // ========================
    @Override
    public Map<String, Object> buildItemFull(ArLedger item) {

        if (item == null) return Map.of();

        Map<String, Object> x = new LinkedHashMap<>(autoBuild(item));

        x.put("order",
                item.getOrderId() != null
                        ? orderDao.findById(item.getOrderId()).orElse(null)
                        : null
        );

        x.put("export",
                item.getExportId() != null
                        ? exportDao.findById(item.getExportId()).orElse(null)
                        : null
        );

        x.put("product",
                item.getProductId() != null
                        ? productDao.findById(item.getProductId()).orElse(null)
                        : null
        );

        x.put("customer",
                item.getCustomerId() != null
                        ? customerDao.findById(item.getCustomerId()).orElse(null)
                        : null
        );

        return x;
    }

    // ========================
    // LIST
    // ========================
    @Override
    public List<Map<String, Object>> buildList(List<ArLedger> items) {

        if (CommonUtil.isNullOrEmpty(items)) {
            return Collections.emptyList();
        }

        Set<Integer> orderIds = items.stream()
                .map(ArLedger::getOrderId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Set<Integer> exportIds = items.stream()
                .map(ArLedger::getExportId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Set<Integer> productIds = items.stream()
                .map(ArLedger::getProductId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Set<Integer> customerIds = items.stream()
                .map(ArLedger::getCustomerId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<Integer, Order> orderMap = orderDao.findByIdsAsMap(orderIds);
        Map<Integer, Export> exportMap = exportDao.findByIdsAsMap(exportIds);
        Map<Integer, Product> productMap = productDao.findByIdsAsMap(productIds);
        Map<Integer, Customer> customerMap = customerDao.findByIdsAsMap(customerIds);

        List<Map<String, Object>> list = new ArrayList<>(items.size());

        for (ArLedger item : items) {

            Map<String, Object> x = new LinkedHashMap<>(autoBuild(item));

            x.put("order", orderMap.get(item.getOrderId()));
            x.put("export", exportMap.get(item.getExportId()));
            x.put("product", productMap.get(item.getProductId()));
            x.put("customer", customerMap.get(item.getCustomerId()));

            list.add(x);
        }

        return list;
    }
}