package com.vlife.api.builder.production;

import com.vlife.api.builder.ProductBuilder;
import com.vlife.shared.api.builder.ItemBuilder;
import com.vlife.shared.jdbc.dao.ProductDao;
import com.vlife.shared.jdbc.dao.production.ProductionOrderItemDao;
import com.vlife.shared.jdbc.entity.Product;
import com.vlife.shared.jdbc.entity.production.ProductionExtraItem;
import com.vlife.shared.jdbc.entity.production.ProductionOrderItem;
import com.vlife.shared.util.CommonUtil;
import jakarta.inject.Singleton;

import java.util.*;
import java.util.stream.Collectors;

@Singleton
public class ProductionExtraItemBuilder extends ItemBuilder<ProductionExtraItem> {

    private final ProductDao productDao;
    private final ProductBuilder productBuilder;
    private final ProductionOrderItemDao orderItemDao;

    public ProductionExtraItemBuilder(
            ProductDao productDao,
            ProductBuilder productBuilder,
            ProductionOrderItemDao orderItemDao
    ) {
        this.productDao = productDao;
        this.productBuilder = productBuilder;
        this.orderItemDao = orderItemDao;
    }

    @Override
    public List<Map<String, Object>> buildList(List<ProductionExtraItem> items) {
        if (CommonUtil.isNullOrEmpty(items)) return Collections.emptyList();

        Set<Integer> productIds = items.stream()
                .map(ProductionExtraItem::getProductId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Set<Integer> productionItemIds = items.stream()
                .map(ProductionExtraItem::getProductionItemId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<Integer, Product> productMap =
                productIds.isEmpty() ? Collections.emptyMap() : productDao.findByIdsAsMap(productIds);

        Map<Integer, ProductionOrderItem> itemMap =
                productionItemIds.isEmpty()
                        ? Collections.emptyMap()
                        : orderItemDao.findByIdsAsMap(productionItemIds);

        List<Map<String, Object>> result = new ArrayList<>();

        for (ProductionExtraItem i : items) {
            Map<String, Object> x = new LinkedHashMap<>(autoBuild(i));

            Product product = productMap.get(i.getProductId());

            x.put("product", product != null ? productBuilder.buildItem(product) : null);
            x.put("production_item", itemMap.get(i.getProductionItemId()));

            result.add(x);
        }

        return result;
    }

    @Override
    public Map<String, Object> buildItemFull(ProductionExtraItem item) {
        if (item == null) return Map.of();
        return buildList(Collections.singletonList(item)).get(0);
    }
}