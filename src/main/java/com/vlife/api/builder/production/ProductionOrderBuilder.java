package com.vlife.api.builder.production;

import com.vlife.api.builder.ProductBuilder;
import com.vlife.shared.api.builder.ItemBuilder;
import com.vlife.shared.jdbc.dao.ProductDao;
import com.vlife.shared.jdbc.dao.WarehouseDao;
import com.vlife.shared.jdbc.dao.production.*;
import com.vlife.shared.jdbc.dao.sale.InventoryLotDao;
import com.vlife.shared.jdbc.entity.Product;
import com.vlife.shared.jdbc.entity.Warehouse;
import com.vlife.shared.jdbc.entity.inventory.InventoryLot;
import com.vlife.shared.jdbc.entity.production.*;
import com.vlife.shared.util.CommonUtil;
import jakarta.inject.Singleton;

import java.util.*;
import java.util.stream.Collectors;

@Singleton
public class ProductionOrderBuilder extends ItemBuilder<ProductionOrder> {

    private final ProductionMaterialDao materialDao;
    private final ProductionOutputDao outputDao;
    private final ProductionExtraItemDao extraDao;
    private final ProductionSubstitutionDao substitutionDao;

    private final ProductDao productDao;
    private final ProductBuilder productBuilder;
    private final WarehouseDao warehouseDao;
    private final InventoryLotDao inventoryLotDao;

    public ProductionOrderBuilder(
            ProductionMaterialDao materialDao,
            ProductionOutputDao outputDao,
            ProductionExtraItemDao extraDao,
            ProductionSubstitutionDao substitutionDao,
            ProductDao productDao,
            ProductBuilder productBuilder,
            WarehouseDao warehouseDao,
            InventoryLotDao inventoryLotDao
    ) {
        this.materialDao = materialDao;
        this.outputDao = outputDao;
        this.extraDao = extraDao;
        this.substitutionDao = substitutionDao;
        this.productDao = productDao;
        this.productBuilder = productBuilder;
        this.warehouseDao = warehouseDao;
        this.inventoryLotDao = inventoryLotDao;
    }

    @Override
    public Map<String, Object> buildItemFull(ProductionOrder item) {
        if (item == null) return Map.of();
        return buildList(Collections.singletonList(item)).get(0);
    }

    @Override
    public List<Map<String, Object>> buildList(List<ProductionOrder> items) {
        if (CommonUtil.isNullOrEmpty(items)) {
            return Collections.emptyList();
        }

        Set<Integer> orderIds = items.stream()
                .map(ProductionOrder::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<Integer, List<ProductionMaterial>> materialMap =
                materialDao.findByProductionOrderIds(orderIds)
                        .stream()
                        .collect(Collectors.groupingBy(ProductionMaterial::getProductionId));

        Map<Integer, List<ProductionOutput>> outputMap =
                outputDao.findByProductionOrderIds(orderIds)
                        .stream()
                        .collect(Collectors.groupingBy(ProductionOutput::getProductId));

        Map<Integer, List<ProductionExtraItem>> extraMap =
                extraDao.findByProductionIds(orderIds)
                        .stream()
                        .collect(Collectors.groupingBy(ProductionExtraItem::getProductionId));

        Map<Integer, List<ProductionSubstitution>> substitutionMap =
                substitutionDao.findByProductionIds(orderIds)
                        .stream()
                        .collect(Collectors.groupingBy(ProductionSubstitution::getProductionId));

        Set<Integer> productIds = new HashSet<>();

        items.stream()
                .map(ProductionOrder::getProductId)
                .filter(Objects::nonNull)
                .forEach(productIds::add);

        materialMap.values().forEach(list ->
                list.forEach(i -> {
                    if (i.getProductId() != null) productIds.add(i.getProductId());
                })
        );

        outputMap.values().forEach(list ->
                list.forEach(i -> {
                    if (i.getProductId() != null) productIds.add(i.getProductId());
                })
        );

        extraMap.values().forEach(list ->
                list.forEach(i -> {
                    if (i.getProductId() != null) productIds.add(i.getProductId());
                })
        );

        substitutionMap.values().forEach(list ->
                list.forEach(i -> {
                    if (i.getOriginalProductId() != null) productIds.add(i.getOriginalProductId());
                    if (i.getSubstituteProductId() != null) productIds.add(i.getSubstituteProductId());
                })
        );

        Map<Integer, Product> productMap =
                productIds.isEmpty()
                        ? Collections.emptyMap()
                        : productDao.findByIdsAsMap(productIds);

        Set<Integer> warehouseIds = items.stream()
                .map(ProductionOrder::getWarehouseId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<Integer, Warehouse> warehouseMap =
                warehouseIds.isEmpty()
                        ? Collections.emptyMap()
                        : warehouseDao.findByIdsAsMap(warehouseIds);

        Set<Integer> lotIds = new HashSet<>();

        materialMap.values().forEach(list ->
                list.forEach(i -> {
                    if (i.getLotId() != null) lotIds.add(i.getLotId());
                })
        );

        Map<Integer, InventoryLot> lotMap =
                lotIds.isEmpty()
                        ? Collections.emptyMap()
                        : inventoryLotDao.findByIdsAsMap(lotIds);

        List<Map<String, Object>> result = new ArrayList<>();

        for (ProductionOrder po : items) {
            Map<String, Object> x = new LinkedHashMap<>(autoBuild(po));

            Product finishedProduct = productMap.get(po.getProductId());
            x.put("product", finishedProduct != null ? productBuilder.buildItem(finishedProduct) : null);
            x.put("warehouse", warehouseMap.get(po.getWarehouseId()));

            List<Map<String, Object>> materials = new ArrayList<>();
            for (ProductionMaterial mi : materialMap.getOrDefault(po.getId(), Collections.emptyList())) {
                Map<String, Object> m = new LinkedHashMap<>(autoBuildAny(mi));

                Product p = productMap.get(mi.getProductId());
                m.put("product", p != null ? productBuilder.buildItem(p) : null);
                m.put("lot", mi.getLotId() != null ? lotMap.get(mi.getLotId()) : null);

                materials.add(m);
            }

            List<Map<String, Object>> outputs = new ArrayList<>();
            for (ProductionOutput oi : outputMap.getOrDefault(po.getId(), Collections.emptyList())) {
                Map<String, Object> o = new LinkedHashMap<>(autoBuildAny(oi));

                Product p = productMap.get(oi.getProductId());
                o.put("product", p != null ? productBuilder.buildItem(p) : null);

                outputs.add(o);
            }

            List<Map<String, Object>> extras = new ArrayList<>();
            for (ProductionExtraItem ei : extraMap.getOrDefault(po.getId(), Collections.emptyList())) {
                Map<String, Object> e = new LinkedHashMap<>(autoBuildAny(ei));

                Product p = productMap.get(ei.getProductId());
                e.put("product", p != null ? productBuilder.buildItem(p) : null);

                extras.add(e);
            }

            List<Map<String, Object>> substitutions = new ArrayList<>();
            for (ProductionSubstitution si : substitutionMap.getOrDefault(po.getId(), Collections.emptyList())) {
                Map<String, Object> s = new LinkedHashMap<>(autoBuildAny(si));

                Product original = productMap.get(si.getOriginalProductId());
                Product substitute = productMap.get(si.getSubstituteProductId());

                s.put("original_product", original != null ? productBuilder.buildItem(original) : null);
                s.put("substitute_product", substitute != null ? productBuilder.buildItem(substitute) : null);

                substitutions.add(s);
            }

            x.put("materials", materials);
            x.put("outputs", outputs);
            x.put("extras", extras);
            x.put("substitutions", substitutions);

            result.add(x);
        }

        return result;
    }
}