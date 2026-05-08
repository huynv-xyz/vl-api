package com.vlife.api.builder.production;

import com.vlife.api.builder.ProductBuilder;
import com.vlife.shared.api.builder.ItemBuilder;
import com.vlife.shared.jdbc.dao.ProductDao;
import com.vlife.shared.jdbc.dao.WarehouseDao;
import com.vlife.shared.jdbc.dao.production.ProductionOrderItemDao;
import com.vlife.shared.jdbc.entity.Product;
import com.vlife.shared.jdbc.entity.Warehouse;
import com.vlife.shared.jdbc.entity.production.ProductionOrderItem;
import com.vlife.shared.jdbc.entity.production.ProductionOutput;
import com.vlife.shared.util.CommonUtil;
import jakarta.inject.Singleton;

import java.util.*;
import java.util.stream.Collectors;

@Singleton
public class ProductionOutputBuilder extends ItemBuilder<ProductionOutput> {

    private final ProductDao productDao;
    private final ProductBuilder productBuilder;
    private final WarehouseDao warehouseDao;
    private final ProductionOrderItemDao orderItemDao;

    public ProductionOutputBuilder(
            ProductDao productDao,
            ProductBuilder productBuilder,
            WarehouseDao warehouseDao,
            ProductionOrderItemDao orderItemDao
    ) {
        this.productDao = productDao;
        this.productBuilder = productBuilder;
        this.warehouseDao = warehouseDao;
        this.orderItemDao = orderItemDao;
    }

    @Override
    public List<Map<String, Object>> buildList(List<ProductionOutput> items) {
        if (CommonUtil.isNullOrEmpty(items)) return Collections.emptyList();

        Set<Integer> productIds = items.stream()
                .map(ProductionOutput::getProductId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Set<Integer> warehouseIds = items.stream()
                .map(ProductionOutput::getWarehouseId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Set<Integer> productionItemIds = items.stream()
                .map(ProductionOutput::getProductionItemId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<Integer, Product> productMap =
                productIds.isEmpty() ? Collections.emptyMap() : productDao.findByIdsAsMap(productIds);

        Map<Integer, Warehouse> warehouseMap =
                warehouseIds.isEmpty() ? Collections.emptyMap() : warehouseDao.findByIdsAsMap(warehouseIds);

        Map<Integer, ProductionOrderItem> itemMap =
                productionItemIds.isEmpty()
                        ? Collections.emptyMap()
                        : orderItemDao.findByIdsAsMap(productionItemIds);

        List<Map<String, Object>> result = new ArrayList<>();

        for (ProductionOutput i : items) {
            Map<String, Object> x = new LinkedHashMap<>(autoBuild(i));

            Product product = productMap.get(i.getProductId());

            x.put("product", product != null ? productBuilder.buildItem(product) : null);
            x.put("warehouse", warehouseMap.get(i.getWarehouseId()));
            x.put("production_item", itemMap.get(i.getProductionItemId()));

            result.add(x);
        }

        return result;
    }

    @Override
    public Map<String, Object> buildItemFull(ProductionOutput item) {
        if (item == null) return Map.of();
        return buildList(Collections.singletonList(item)).get(0);
    }
}