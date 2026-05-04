package com.vlife.api.builder.sale;

import com.vlife.api.builder.ProductBuilder;
import com.vlife.shared.api.builder.ItemBuilder;
import com.vlife.shared.jdbc.dao.ProductDao;
import com.vlife.shared.jdbc.dao.WarehouseDao;
import com.vlife.shared.jdbc.dao.sale.DeliveryDao;
import com.vlife.shared.jdbc.dao.sale.ExportItemDao;
import com.vlife.shared.jdbc.dao.sale.OrderDao;
import com.vlife.shared.jdbc.entity.Product;
import com.vlife.shared.jdbc.entity.Warehouse;
import com.vlife.shared.jdbc.entity.sale.Delivery;
import com.vlife.shared.jdbc.entity.sale.Export;
import com.vlife.shared.jdbc.entity.sale.ExportItem;
import com.vlife.shared.jdbc.entity.sale.Order;
import com.vlife.shared.util.CommonUtil;
import jakarta.inject.Singleton;

import java.util.*;
import java.util.stream.Collectors;

@Singleton
public class ExportBuilder extends ItemBuilder<Export> {

    private final OrderDao orderDao;
    private final DeliveryDao deliveryDao;
    private final WarehouseDao warehouseDao;

    private final ExportItemDao exportItemDao;
    private final ProductDao productDao;
    private final ProductBuilder productBuilder;

    public ExportBuilder(
            OrderDao orderDao,
            DeliveryDao deliveryDao,
            WarehouseDao warehouseDao,
            ExportItemDao exportItemDao,
            ProductDao productDao,
            ProductBuilder productBuilder
    ) {
        this.orderDao = orderDao;
        this.deliveryDao = deliveryDao;
        this.warehouseDao = warehouseDao;
        this.exportItemDao = exportItemDao;
        this.productDao = productDao;
        this.productBuilder = productBuilder;
    }

    @Override
    public Map<String, Object> buildItemFull(Export item) {

        if (item == null) return Map.of();

        Map<String, Object> x = new LinkedHashMap<>(autoBuild(item));

        // ===== relations
        x.put("order",
                item.getOrderId() != null
                        ? orderDao.findById(item.getOrderId()).orElse(null)
                        : null
        );

        x.put("delivery",
                item.getDeliveryId() != null
                        ? deliveryDao.findById(item.getDeliveryId()).orElse(null)
                        : null
        );

        x.put("warehouse",
                item.getWarehouseId() != null
                        ? warehouseDao.findById(item.getWarehouseId()).orElse(null)
                        : null
        );

        // ===== items
        List<ExportItem> items = exportItemDao.findByExportId(item.getId());

        Set<Integer> productIds = items.stream()
                .map(ExportItem::getProductId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<Integer, Product> productMap =
                productIds.isEmpty()
                        ? Collections.emptyMap()
                        : productDao.findByIdsAsMap(productIds);

        List<Map<String, Object>> itemRes = new ArrayList<>();

        for (ExportItem ei : items) {

            Map<String, Object> m = new LinkedHashMap<>(autoBuildAny(ei));

            Product p = productMap.get(ei.getProductId());
            if (p != null) {
                m.put("product", productBuilder.buildItem(p));
            }

            itemRes.add(m);
        }

        x.put("items", itemRes);

        return x;
    }

    @Override
    public List<Map<String, Object>> buildList(List<Export> items) {

        if (CommonUtil.isNullOrEmpty(items)) {
            return Collections.emptyList();
        }

        Set<Integer> orderIds = items.stream()
                .map(Export::getOrderId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Set<Integer> deliveryIds = items.stream()
                .map(Export::getDeliveryId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Set<Integer> warehouseIds = items.stream()
                .map(Export::getWarehouseId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<Integer, Order> orderMap = orderDao.findByIdsAsMap(orderIds);
        Map<Integer, Delivery> deliveryMap = deliveryDao.findByIdsAsMap(deliveryIds);
        Map<Integer, Warehouse> warehouseMap = warehouseDao.findByIdsAsMap(warehouseIds);

        List<Map<String, Object>> list = new ArrayList<>(items.size());

        for (Export item : items) {

            Map<String, Object> x = new LinkedHashMap<>(autoBuild(item));

            x.put("order", orderMap.get(item.getOrderId()));
            x.put("delivery", deliveryMap.get(item.getDeliveryId()));
            x.put("warehouse", warehouseMap.get(item.getWarehouseId()));

            list.add(x);
        }

        return list;
    }
}