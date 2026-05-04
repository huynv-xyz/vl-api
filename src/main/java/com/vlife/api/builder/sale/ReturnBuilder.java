package com.vlife.api.builder.sale;

import com.vlife.api.builder.ProductBuilder;
import com.vlife.shared.api.builder.ItemBuilder;
import com.vlife.shared.jdbc.dao.ProductDao;
import com.vlife.shared.jdbc.dao.sale.ExportDao;
import com.vlife.shared.jdbc.dao.sale.OrderDao;
import com.vlife.shared.jdbc.dao.sale.ReturnItemDao;
import com.vlife.shared.jdbc.entity.Product;
import com.vlife.shared.jdbc.entity.sale.Export;
import com.vlife.shared.jdbc.entity.sale.Order;
import com.vlife.shared.jdbc.entity.sale.Return;
import com.vlife.shared.jdbc.entity.sale.ReturnItem;
import com.vlife.shared.util.CommonUtil;
import jakarta.inject.Singleton;

import java.util.*;
import java.util.stream.Collectors;

@Singleton
public class ReturnBuilder extends ItemBuilder<Return> {

    private final OrderDao orderDao;
    private final ExportDao exportDao;

    private final ReturnItemDao returnItemDao;
    private final ProductDao productDao;
    private final ProductBuilder productBuilder;

    public ReturnBuilder(
            OrderDao orderDao,
            ExportDao exportDao,
            ReturnItemDao returnItemDao,
            ProductDao productDao,
            ProductBuilder productBuilder
    ) {
        this.orderDao = orderDao;
        this.exportDao = exportDao;
        this.returnItemDao = returnItemDao;
        this.productDao = productDao;
        this.productBuilder = productBuilder;
    }

    @Override
    public Map<String, Object> buildItemFull(Return item) {

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

        List<ReturnItem> items = returnItemDao.findByReturnId(item.getId());

        Set<Integer> productIds = items.stream()
                .map(ReturnItem::getProductId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<Integer, Product> productMap =
                productIds.isEmpty()
                        ? Collections.emptyMap()
                        : productDao.findByIdsAsMap(productIds);

        List<Map<String, Object>> itemRes = new ArrayList<>();

        for (ReturnItem ri : items) {

            Map<String, Object> m = new LinkedHashMap<>(autoBuildAny(ri));

            Product p = productMap.get(ri.getProductId());
            if (p != null) {
                m.put("product", productBuilder.buildItem(p));
            }

            itemRes.add(m);
        }

        x.put("items", itemRes);

        return x;
    }

    @Override
    public List<Map<String, Object>> buildList(List<Return> items) {

        if (CommonUtil.isNullOrEmpty(items)) {
            return Collections.emptyList();
        }

        Set<Integer> orderIds = items.stream()
                .map(Return::getOrderId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Set<Integer> exportIds = items.stream()
                .map(Return::getExportId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<Integer, Order> orderMap = orderDao.findByIdsAsMap(orderIds);
        Map<Integer, Export> exportMap = exportDao.findByIdsAsMap(exportIds);

        List<Map<String, Object>> list = new ArrayList<>(items.size());

        for (Return item : items) {

            Map<String, Object> x = new LinkedHashMap<>(autoBuild(item));

            x.put("order", orderMap.get(item.getOrderId()));
            x.put("export", exportMap.get(item.getExportId()));

            list.add(x);
        }

        return list;
    }
}