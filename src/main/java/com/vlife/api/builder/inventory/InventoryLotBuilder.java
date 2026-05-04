package com.vlife.api.builder.inventory;

import com.vlife.api.builder.ProductBuilder;
import com.vlife.shared.api.builder.ItemBuilder;
import com.vlife.shared.jdbc.dao.ProductDao;
import com.vlife.shared.jdbc.dao.WarehouseDao;
import com.vlife.shared.jdbc.entity.Product;
import com.vlife.shared.jdbc.entity.Warehouse;
import com.vlife.shared.jdbc.entity.inventory.InventoryLot;
import com.vlife.shared.util.CommonUtil;
import jakarta.inject.Singleton;

import java.util.*;
import java.util.stream.Collectors;

@Singleton
public class InventoryLotBuilder extends ItemBuilder<InventoryLot> {

    private final ProductDao productDao;
    private final WarehouseDao warehouseDao;
    private final ProductBuilder productBuilder;

    public InventoryLotBuilder(
            ProductDao productDao,
            WarehouseDao warehouseDao,
            ProductBuilder productBuilder
    ) {
        this.productDao = productDao;
        this.warehouseDao = warehouseDao;
        this.productBuilder = productBuilder;
    }

    @Override
    public Map<String, Object> buildItemFull(InventoryLot item) {

        if (item == null) return Map.of();

        Map<String, Object> x = new LinkedHashMap<>(autoBuild(item));

        Product product = productDao.findById(item.getProductId()).orElse(null);
        Warehouse warehouse = warehouseDao.findById(item.getWarehouseId()).orElse(null);

        x.put("product", product != null ? productBuilder.buildItem(product) : null);
        x.put("warehouse", warehouse);

        return x;
    }

    @Override
    public List<Map<String, Object>> buildList(List<InventoryLot> items) {

        if (CommonUtil.isNullOrEmpty(items)) {
            return Collections.emptyList();
        }

        Set<Integer> productIds = items.stream()
                .map(InventoryLot::getProductId)
                .collect(Collectors.toSet());

        Set<Integer> warehouseIds = items.stream()
                .map(InventoryLot::getWarehouseId)
                .collect(Collectors.toSet());

        Map<Integer, Product> productMap = productDao.findByIdsAsMap(productIds);
        Map<Integer, Warehouse> warehouseMap = warehouseDao.findByIdsAsMap(warehouseIds);

        List<Map<String, Object>> list = new ArrayList<>();

        for (InventoryLot item : items) {

            Map<String, Object> x = new LinkedHashMap<>(autoBuild(item));

            Product product = productMap.get(item.getProductId());

            x.put("product", product != null ? productBuilder.buildItem(product) : null);
            x.put("warehouse", warehouseMap.get(item.getWarehouseId()));

            list.add(x);
        }

        return list;
    }
}