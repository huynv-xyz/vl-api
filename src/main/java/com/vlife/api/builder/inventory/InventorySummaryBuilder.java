package com.vlife.api.builder.inventory;

import com.vlife.api.builder.ProductBuilder;
import com.vlife.shared.jdbc.dao.ProductDao;
import com.vlife.shared.jdbc.dao.WarehouseDao;
import com.vlife.shared.jdbc.entity.Product;
import com.vlife.shared.jdbc.entity.Warehouse;
import jakarta.inject.Singleton;

import java.util.*;

@Singleton
public class InventorySummaryBuilder {

    private final ProductDao productDao;
    private final WarehouseDao warehouseDao;
    private final ProductBuilder productBuilder;

    public InventorySummaryBuilder(
            ProductDao productDao,
            WarehouseDao warehouseDao,
            ProductBuilder productBuilder
    ) {
        this.productDao = productDao;
        this.warehouseDao = warehouseDao;
        this.productBuilder = productBuilder;
    }

    public List<Map<String, Object>> buildList(List<Map<String, Object>> rows) {

        if (rows == null || rows.isEmpty()) {
            return Collections.emptyList();
        }

        Set<Integer> productIds = new HashSet<>();
        Set<Integer> warehouseIds = new HashSet<>();

        for (Map<String, Object> r : rows) {
            productIds.add((Integer) r.get("product_id"));
            warehouseIds.add((Integer) r.get("warehouse_id"));
        }

        Map<Integer, Product> productMap = productDao.findByIdsAsMap(productIds);
        Map<Integer, Warehouse> warehouseMap = warehouseDao.findByIdsAsMap(warehouseIds);

        List<Map<String, Object>> result = new ArrayList<>();

        for (Map<String, Object> r : rows) {

            Integer productId = (Integer) r.get("product_id");
            Integer warehouseId = (Integer) r.get("warehouse_id");

            Product p = productMap.get(productId);
            Warehouse w = warehouseMap.get(warehouseId);

            Map<String, Object> x = new LinkedHashMap<>();

            x.put("product_id", productId);
            x.put("warehouse_id", warehouseId);
            x.put("total_quantity", r.get("total_quantity"));
            x.put("total_value", r.get("total_value"));

            x.put("product", p != null ? productBuilder.buildItem(p) : null);
            x.put("warehouse", w);

            result.add(x);
        }

        return result;
    }
}