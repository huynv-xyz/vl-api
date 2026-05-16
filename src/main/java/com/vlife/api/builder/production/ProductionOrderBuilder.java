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

    private final ProductionOrderItemDao itemDao;

    private final ProductionMaterialDao materialDao;
    private final ProductionExtraItemDao extraDao;
    private final ProductionSubstitutionDao substitutionDao;
    private final ProductionOutputDao outputDao;
    private final ProductionFifoAllocationRunDao fifoRunDao;
    private final ProductionFifoAllocationDao fifoAllocationDao;
    private final ProductionWarningDao warningDao;
    private final ProductionActionLogDao actionLogDao;

    private final ProductDao productDao;
    private final ProductBuilder productBuilder;

    private final WarehouseDao warehouseDao;
    private final InventoryLotDao inventoryLotDao;

    public ProductionOrderBuilder(
            ProductionOrderItemDao itemDao,
            ProductionMaterialDao materialDao,
            ProductionExtraItemDao extraDao,
            ProductionSubstitutionDao substitutionDao,
            ProductionOutputDao outputDao,
            ProductionFifoAllocationRunDao fifoRunDao,
            ProductionFifoAllocationDao fifoAllocationDao,
            ProductionWarningDao warningDao,
            ProductionActionLogDao actionLogDao,
            ProductDao productDao,
            ProductBuilder productBuilder,
            WarehouseDao warehouseDao,
            InventoryLotDao inventoryLotDao
    ) {
        this.itemDao = itemDao;
        this.materialDao = materialDao;
        this.extraDao = extraDao;
        this.substitutionDao = substitutionDao;
        this.outputDao = outputDao;
        this.fifoRunDao = fifoRunDao;
        this.fifoAllocationDao = fifoAllocationDao;
        this.warningDao = warningDao;
        this.actionLogDao = actionLogDao;
        this.productDao = productDao;
        this.productBuilder = productBuilder;
        this.warehouseDao = warehouseDao;
        this.inventoryLotDao = inventoryLotDao;
    }

    @Override
    public Map<String, Object> buildItemFull(ProductionOrder item) {
        if (item == null) return Map.of();

        List<Map<String, Object>> rs = buildList(List.of(item));
        return rs.isEmpty() ? Map.of() : rs.get(0);
    }

    @Override
    public List<Map<String, Object>> buildList(List<ProductionOrder> orders) {

        if (CommonUtil.isNullOrEmpty(orders)) {
            return List.of();
        }

        // =====================================================
        // ORDER IDS
        // =====================================================

        Set<Integer> orderIds = orders.stream()
                .map(ProductionOrder::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        // =====================================================
        // ORDER ITEMS
        // =====================================================

        List<ProductionOrderItem> orderItems =
                itemDao.findByProductionIds(orderIds);

        Map<Integer, List<ProductionOrderItem>> itemMap =
                orderItems.stream()
                        .collect(Collectors.groupingBy(
                                ProductionOrderItem::getProductionId
                        ));

        Set<Integer> productionItemIds = orderItems.stream()
                .map(ProductionOrderItem::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        // =====================================================
        // CHILD TABLES
        // =====================================================

        Map<Integer, List<ProductionMaterial>> materialMap =
                materialDao.findByProductionItemIds(productionItemIds)
                        .stream()
                        .collect(Collectors.groupingBy(
                                ProductionMaterial::getProductionItemId
                        ));

        Map<Integer, List<ProductionExtraItem>> extraMap =
                extraDao.findByProductionItemIds(productionItemIds)
                        .stream()
                        .collect(Collectors.groupingBy(
                                ProductionExtraItem::getProductionItemId
                        ));

        Map<Integer, List<ProductionSubstitution>> substitutionMap =
                substitutionDao.findByProductionItemIds(productionItemIds)
                        .stream()
                        .collect(Collectors.groupingBy(
                                ProductionSubstitution::getProductionItemId
                        ));

        Map<Integer, List<ProductionOutput>> outputMap =
                outputDao.findByProductionItemIds(productionItemIds)
                        .stream()
                        .collect(Collectors.groupingBy(
                                ProductionOutput::getProductionItemId
                        ));

        Map<Integer, List<ProductionFifoAllocation>> fifoAllocationMap =
                fifoAllocationDao.findByProductionMaterialIds(
                                materialMap.values().stream()
                                        .flatMap(List::stream)
                                        .map(ProductionMaterial::getId)
                                        .filter(Objects::nonNull)
                                        .collect(Collectors.toSet())
                        )
                        .stream()
                        .collect(Collectors.groupingBy(
                                ProductionFifoAllocation::getProductionMaterialId
                        ));

        Set<Integer> inventoryLotIds = fifoAllocationMap.values().stream()
                .flatMap(List::stream)
                .map(ProductionFifoAllocation::getInventoryLotId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<Integer, List<ProductionWarning>> warningMap =
                orderIds.stream()
                        .flatMap(id -> warningDao.findByProductionId(id).stream())
                        .collect(Collectors.groupingBy(ProductionWarning::getProductionId));

        Map<Integer, List<ProductionActionLog>> actionLogMap =
                orderIds.stream()
                        .flatMap(id -> actionLogDao.findByProductionId(id).stream())
                        .collect(Collectors.groupingBy(ProductionActionLog::getProductionId));

        Map<Integer, List<ProductionFifoAllocationRun>> fifoRunMap =
                orderIds.stream()
                        .flatMap(id -> fifoRunDao.findByProductionId(id).stream())
                        .collect(Collectors.groupingBy(ProductionFifoAllocationRun::getProductionId));

        // =====================================================
        // LOAD PRODUCT IDS
        // =====================================================

        Set<Integer> productIds = new HashSet<>();
        Set<Integer> warehouseIds = new HashSet<>();

        for (ProductionOrderItem i : orderItems) {
            if (i.getProductId() != null) {
                productIds.add(i.getProductId());
            }

            if (i.getWarehouseId() != null) {
                warehouseIds.add(i.getWarehouseId());
            }
        }

        materialMap.values().forEach(list -> {
            for (ProductionMaterial i : list) {

                if (i.getProductId() != null) {
                    productIds.add(i.getProductId());
                }

                if (i.getOriginalProductId() != null) {
                    productIds.add(i.getOriginalProductId());
                }

                if (i.getWarehouseId() != null) {
                    warehouseIds.add(i.getWarehouseId());
                }
            }
        });

        extraMap.values().forEach(list -> {
            for (ProductionExtraItem i : list) {
                if (i.getProductId() != null) {
                    productIds.add(i.getProductId());
                }
            }
        });

        substitutionMap.values().forEach(list -> {
            for (ProductionSubstitution i : list) {

                if (i.getOriginalProductId() != null) {
                    productIds.add(i.getOriginalProductId());
                }

                if (i.getSubstituteProductId() != null) {
                    productIds.add(i.getSubstituteProductId());
                }
            }
        });

        outputMap.values().forEach(list -> {
            for (ProductionOutput i : list) {

                if (i.getProductId() != null) {
                    productIds.add(i.getProductId());
                }

                if (i.getWarehouseId() != null) {
                    warehouseIds.add(i.getWarehouseId());
                }
            }
        });

        fifoAllocationMap.values().forEach(list -> {
            for (ProductionFifoAllocation i : list) {
                if (i.getMaterialProductId() != null) {
                    productIds.add(i.getMaterialProductId());
                }

                if (i.getMaterialWarehouseId() != null) {
                    warehouseIds.add(i.getMaterialWarehouseId());
                }
            }
        });

        // =====================================================
        // LOAD MASTER DATA
        // =====================================================

        Map<Integer, Product> productMap =
                productIds.isEmpty()
                        ? Map.of()
                        : productDao.findByIdsAsMap(productIds);

        Map<Integer, Warehouse> warehouseMap =
                warehouseIds.isEmpty()
                        ? Map.of()
                        : warehouseDao.findByIdsAsMap(warehouseIds);

        Map<Integer, InventoryLot> inventoryLotMap =
                inventoryLotIds.isEmpty()
                        ? Map.of()
                        : inventoryLotDao.findByIdsAsMap(inventoryLotIds);

        // =====================================================
        // BUILD RESPONSE
        // =====================================================

        List<Map<String, Object>> result = new ArrayList<>();

        for (ProductionOrder order : orders) {

            Map<String, Object> x =
                    new LinkedHashMap<>(autoBuild(order));

            List<Map<String, Object>> itemResults = new ArrayList<>();

            List<ProductionOrderItem> items =
                    itemMap.getOrDefault(order.getId(), List.of());

            for (ProductionOrderItem item : items) {

                Map<String, Object> itemJson =
                        new LinkedHashMap<>(autoBuildAny(item));

                Product product =
                        productMap.get(item.getProductId());

                Warehouse warehouse =
                        warehouseMap.get(item.getWarehouseId());

                itemJson.put(
                        "product",
                        product != null
                                ? productBuilder.buildItem(product)
                                : null
                );

                itemJson.put("warehouse", warehouse);

                itemJson.put(
                        "materials",
                        buildMaterials(
                                materialMap.getOrDefault(item.getId(), List.of()),
                                fifoAllocationMap,
                                productMap,
                                warehouseMap,
                                inventoryLotMap
                        )
                );

                itemJson.put(
                        "extras",
                        buildExtras(
                                extraMap.getOrDefault(item.getId(), List.of()),
                                productMap
                        )
                );

                itemJson.put(
                        "substitutions",
                        buildSubstitutions(
                                substitutionMap.getOrDefault(item.getId(), List.of()),
                                productMap
                        )
                );

                itemJson.put(
                        "outputs",
                        buildOutputs(
                                outputMap.getOrDefault(item.getId(), List.of()),
                                productMap,
                                warehouseMap
                        )
                );

                itemResults.add(itemJson);
            }

            x.put("items", itemResults);
            x.put("fifo_runs", buildSimpleList(fifoRunMap.getOrDefault(order.getId(), List.of())));
            x.put("warnings", buildSimpleList(warningMap.getOrDefault(order.getId(), List.of())));
            x.put("action_logs", buildSimpleList(actionLogMap.getOrDefault(order.getId(), List.of())));

            result.add(x);
        }

        return result;
    }

    // =====================================================
    // MATERIALS
    // =====================================================

    private List<Map<String, Object>> buildMaterials(
            List<ProductionMaterial> items,
            Map<Integer, List<ProductionFifoAllocation>> fifoAllocationMap,
            Map<Integer, Product> productMap,
            Map<Integer, Warehouse> warehouseMap,
            Map<Integer, InventoryLot> inventoryLotMap
    ) {

        List<Map<String, Object>> result = new ArrayList<>();

        for (ProductionMaterial i : items) {

            Map<String, Object> x =
                    new LinkedHashMap<>(autoBuildAny(i));

            Product product =
                    productMap.get(i.getProductId());

            Product original =
                    productMap.get(i.getOriginalProductId());

            x.put(
                    "product",
                    product != null
                            ? productBuilder.buildItem(product)
                            : null
            );

            x.put(
                    "original_product",
                    original != null
                            ? productBuilder.buildItem(original)
                            : null
            );

            x.put("warehouse", warehouseMap.get(i.getWarehouseId()));
            x.put(
                    "fifo_allocations",
                    buildFifoAllocations(
                            fifoAllocationMap.getOrDefault(i.getId(), List.of()),
                            productMap,
                            warehouseMap,
                            inventoryLotMap
                    )
            );

            result.add(x);
        }

        return result;
    }

    // =====================================================
    // EXTRA
    // =====================================================

    private List<Map<String, Object>> buildExtras(
            List<ProductionExtraItem> items,
            Map<Integer, Product> productMap
    ) {

        List<Map<String, Object>> result = new ArrayList<>();

        for (ProductionExtraItem i : items) {

            Map<String, Object> x =
                    new LinkedHashMap<>(autoBuildAny(i));

            Product product =
                    productMap.get(i.getProductId());

            x.put(
                    "product",
                    product != null
                            ? productBuilder.buildItem(product)
                            : null
            );

            result.add(x);
        }

        return result;
    }

    // =====================================================
    // SUBSTITUTIONS
    // =====================================================

    private List<Map<String, Object>> buildSubstitutions(
            List<ProductionSubstitution> items,
            Map<Integer, Product> productMap
    ) {

        List<Map<String, Object>> result = new ArrayList<>();

        for (ProductionSubstitution i : items) {

            Map<String, Object> x =
                    new LinkedHashMap<>(autoBuildAny(i));

            Product original =
                    productMap.get(i.getOriginalProductId());

            Product substitute =
                    productMap.get(i.getSubstituteProductId());

            x.put(
                    "original_product",
                    original != null
                            ? productBuilder.buildItem(original)
                            : null
            );

            x.put(
                    "substitute_product",
                    substitute != null
                            ? productBuilder.buildItem(substitute)
                            : null
            );

            result.add(x);
        }

        return result;
    }

    // =====================================================
    // OUTPUTS
    // =====================================================

    private List<Map<String, Object>> buildOutputs(
            List<ProductionOutput> items,
            Map<Integer, Product> productMap,
            Map<Integer, Warehouse> warehouseMap
    ) {

        List<Map<String, Object>> result = new ArrayList<>();

        for (ProductionOutput i : items) {

            Map<String, Object> x =
                    new LinkedHashMap<>(autoBuildAny(i));

            Product product =
                    productMap.get(i.getProductId());

            x.put(
                    "product",
                    product != null
                            ? productBuilder.buildItem(product)
                            : null
            );

            x.put(
                    "warehouse",
                    warehouseMap.get(i.getWarehouseId())
            );

            result.add(x);
        }

        return result;
    }

    private List<Map<String, Object>> buildFifoAllocations(
            List<ProductionFifoAllocation> items,
            Map<Integer, Product> productMap,
            Map<Integer, Warehouse> warehouseMap,
            Map<Integer, InventoryLot> inventoryLotMap
    ) {

        List<Map<String, Object>> result = new ArrayList<>();

        for (ProductionFifoAllocation i : items) {
            Map<String, Object> x =
                    new LinkedHashMap<>(autoBuildAny(i));

            Product material =
                    productMap.get(i.getMaterialProductId());

            x.put(
                    "material_product",
                    material != null
                            ? productBuilder.buildItem(material)
                            : null
            );

            x.put("warehouse", warehouseMap.get(i.getMaterialWarehouseId()));

            InventoryLot lot = inventoryLotMap.get(i.getInventoryLotId());
            if (lot != null) {
                x.put("expiry_date", lot.getExpiryDate());
                x.put("quantity_remaining", lot.getQuantityRemaining());
            }

            result.add(x);
        }

        return result;
    }

    private List<Map<String, Object>> buildSimpleList(List<?> items) {
        if (CommonUtil.isNullOrEmpty(items)) {
            return List.of();
        }

        List<Map<String, Object>> result = new ArrayList<>();

        for (Object item : items) {
            result.add(new LinkedHashMap<>(autoBuildAny(item)));
        }

        return result;
    }
}
