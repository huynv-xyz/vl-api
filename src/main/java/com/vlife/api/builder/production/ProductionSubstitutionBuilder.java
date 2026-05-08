package com.vlife.api.builder.production;

import com.vlife.api.builder.ProductBuilder;
import com.vlife.shared.api.builder.ItemBuilder;
import com.vlife.shared.jdbc.dao.ProductDao;
import com.vlife.shared.jdbc.dao.production.ProductBomItemDao;
import com.vlife.shared.jdbc.dao.production.ProductionOrderItemDao;
import com.vlife.shared.jdbc.entity.Product;
import com.vlife.shared.jdbc.entity.production.ProductBomItem;
import com.vlife.shared.jdbc.entity.production.ProductionOrderItem;
import com.vlife.shared.jdbc.entity.production.ProductionSubstitution;
import com.vlife.shared.util.CommonUtil;
import jakarta.inject.Singleton;

import java.util.*;
import java.util.stream.Collectors;

@Singleton
public class ProductionSubstitutionBuilder extends ItemBuilder<ProductionSubstitution> {

    private final ProductDao productDao;
    private final ProductBuilder productBuilder;
    private final ProductionOrderItemDao orderItemDao;
    private final ProductBomItemDao bomItemDao;

    public ProductionSubstitutionBuilder(
            ProductDao productDao,
            ProductBuilder productBuilder,
            ProductionOrderItemDao orderItemDao,
            ProductBomItemDao bomItemDao
    ) {
        this.productDao = productDao;
        this.productBuilder = productBuilder;
        this.orderItemDao = orderItemDao;
        this.bomItemDao = bomItemDao;
    }

    @Override
    public List<Map<String, Object>> buildList(List<ProductionSubstitution> items) {
        if (CommonUtil.isNullOrEmpty(items)) return Collections.emptyList();

        Set<Integer> productIds = new HashSet<>();
        Set<Integer> productionItemIds = new HashSet<>();
        Set<Integer> bomItemIds = new HashSet<>();

        for (ProductionSubstitution i : items) {
            if (i.getOriginalProductId() != null) productIds.add(i.getOriginalProductId());
            if (i.getSubstituteProductId() != null) productIds.add(i.getSubstituteProductId());
            if (i.getProductionItemId() != null) productionItemIds.add(i.getProductionItemId());
            if (i.getBomItemId() != null) bomItemIds.add(i.getBomItemId());
        }

        Map<Integer, Product> productMap =
                productIds.isEmpty() ? Collections.emptyMap() : productDao.findByIdsAsMap(productIds);

        Map<Integer, ProductionOrderItem> itemMap =
                productionItemIds.isEmpty()
                        ? Collections.emptyMap()
                        : orderItemDao.findByIdsAsMap(productionItemIds);

        Map<Integer, ProductBomItem> bomItemMap =
                bomItemIds.isEmpty()
                        ? Collections.emptyMap()
                        : bomItemDao.findByIdsAsMap(bomItemIds);

        List<Map<String, Object>> result = new ArrayList<>();

        for (ProductionSubstitution i : items) {
            Map<String, Object> x = new LinkedHashMap<>(autoBuild(i));

            Product original = productMap.get(i.getOriginalProductId());
            Product substitute = productMap.get(i.getSubstituteProductId());

            x.put("original_product", original != null ? productBuilder.buildItem(original) : null);
            x.put("substitute_product", substitute != null ? productBuilder.buildItem(substitute) : null);
            x.put("production_item", itemMap.get(i.getProductionItemId()));
            x.put("bom_item", bomItemMap.get(i.getBomItemId()));

            result.add(x);
        }

        return result;
    }

    @Override
    public Map<String, Object> buildItemFull(ProductionSubstitution item) {
        if (item == null) return Map.of();
        return buildList(Collections.singletonList(item)).get(0);
    }
}