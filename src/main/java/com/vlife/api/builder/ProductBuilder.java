package com.vlife.api.builder;

import com.vlife.shared.api.builder.ItemBuilder;
import com.vlife.shared.jdbc.dao.WarehouseDao;
import com.vlife.shared.jdbc.entity.Product;
import com.vlife.shared.jdbc.entity.Warehouse;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Singleton
public class ProductBuilder extends ItemBuilder<Product> {

    private final WarehouseDao warehouseDao;

    @Inject
    public ProductBuilder(WarehouseDao warehouseDao) {
        this.warehouseDao = warehouseDao;
    }

    @Override
    public Map<String, Object> buildItem(Product item) {
        if (item == null) return Map.of();

        Map<String, Object> x = new LinkedHashMap<>(autoBuild(item));
        if (item.getDefaultWarehouseId() != null) {
            x.put("default_warehouse", warehouseDao.findById(item.getDefaultWarehouseId()).orElse(null));
        }
        return x;
    }

    @Override
    public List<Map<String, Object>> buildList(List<Product> items) {
        if (items == null || items.isEmpty()) return Collections.emptyList();

        Set<Integer> warehouseIds = items.stream()
                .map(Product::getDefaultWarehouseId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<Integer, Warehouse> warehouseMap = warehouseIds.isEmpty()
                ? Collections.emptyMap()
                : warehouseDao.findByIdsAsMap(warehouseIds);

        return items.stream().map(item -> {
            Map<String, Object> x = new LinkedHashMap<>(autoBuild(item));
            x.put("default_warehouse", warehouseMap.get(item.getDefaultWarehouseId()));
            return x;
        }).toList();
    }
}
