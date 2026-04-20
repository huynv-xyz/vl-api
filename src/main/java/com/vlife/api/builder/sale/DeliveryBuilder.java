package com.vlife.api.builder.sale;

import com.vlife.api.builder.ProductBuilder;
import com.vlife.shared.api.builder.ItemBuilder;
import com.vlife.shared.jdbc.dao.CompanyDao;
import com.vlife.shared.jdbc.dao.WarehouseDao;
import com.vlife.shared.jdbc.dao.ProductDao;
import com.vlife.shared.jdbc.dao.sale.DeliveryItemDao;
import com.vlife.shared.jdbc.dao.sale.OrderDao;
import com.vlife.shared.jdbc.entity.Company;
import com.vlife.shared.jdbc.entity.Warehouse;
import com.vlife.shared.jdbc.entity.Product;
import com.vlife.shared.jdbc.entity.sale.Delivery;
import com.vlife.shared.jdbc.entity.sale.DeliveryItem;
import com.vlife.shared.jdbc.entity.sale.Order;
import com.vlife.shared.util.CommonUtil;
import jakarta.inject.Singleton;

import java.util.*;
import java.util.stream.Collectors;

@Singleton
public class DeliveryBuilder extends ItemBuilder<Delivery> {

    private final OrderDao orderDao;
    private final WarehouseDao warehouseDao;
    private final CompanyDao companyDao;

    private final DeliveryItemDao deliveryItemDao;
    private final ProductDao productDao;
    private final ProductBuilder productBuilder;

    public DeliveryBuilder(
            OrderDao orderDao,
            WarehouseDao warehouseDao,
            CompanyDao companyDao,
            DeliveryItemDao deliveryItemDao,
            ProductDao productDao,
            ProductBuilder productBuilder
    ) {
        this.orderDao = orderDao;
        this.warehouseDao = warehouseDao;
        this.companyDao = companyDao;
        this.deliveryItemDao = deliveryItemDao;
        this.productDao = productDao;
        this.productBuilder = productBuilder;
    }

    @Override
    public Map<String, Object> buildItemFull(Delivery item) {

        if (item == null) return Map.of();

        Map<String, Object> x = new LinkedHashMap<>(autoBuild(item));

        x.put("order",
                item.getOrderId() != null
                        ? orderDao.findById(item.getOrderId()).orElse(null)
                        : null
        );

        x.put("warehouse",
                item.getWarehouseId() != null
                        ? warehouseDao.findById(item.getWarehouseId()).orElse(null)
                        : null
        );

        x.put("company",
                item.getCompanyId() != null
                        ? companyDao.findById(item.getCompanyId()).orElse(null)
                        : null
        );

        // ===== items
        List<DeliveryItem> items = deliveryItemDao.findByDeliveryId(item.getId());

        Set<Integer> productIds = items.stream()
                .map(DeliveryItem::getProductId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<Integer, Product> productMap =
                productIds.isEmpty()
                        ? Collections.emptyMap()
                        : productDao.findByIdsAsMap(productIds);

        List<Map<String, Object>> itemRes = new ArrayList<>();

        for (DeliveryItem di : items) {

            Map<String, Object> m = new LinkedHashMap<>(autoBuildAny(di));

            Product p = productMap.get(di.getProductId());
            if (p != null) {
                m.put("product", productBuilder.buildItem(p));
            }

            itemRes.add(m);
        }

        x.put("items", itemRes);

        return x;
    }

    // ========================
    // BUILD LIST
    // ========================
    @Override
    public List<Map<String, Object>> buildList(List<Delivery> items) {

        if (CommonUtil.isNullOrEmpty(items)) {
            return Collections.emptyList();
        }

        Set<Integer> orderIds = items.stream()
                .map(Delivery::getOrderId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Set<Integer> warehouseIds = items.stream()
                .map(Delivery::getWarehouseId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Set<Integer> companyIds = items.stream()
                .map(Delivery::getCompanyId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<Integer, Order> orderMap = orderDao.findByIdsAsMap(orderIds);
        Map<Integer, Warehouse> warehouseMap = warehouseDao.findByIdsAsMap(warehouseIds);
        Map<Integer, Company> companyMap = companyDao.findByIdsAsMap(companyIds);

        List<Map<String, Object>> list = new ArrayList<>(items.size());

        for (Delivery item : items) {

            Map<String, Object> x = new LinkedHashMap<>(autoBuild(item));

            x.put("order", orderMap.get(item.getOrderId()));
            x.put("warehouse", warehouseMap.get(item.getWarehouseId()));
            x.put("company", companyMap.get(item.getCompanyId()));

            list.add(x);
        }

        return list;
    }
}