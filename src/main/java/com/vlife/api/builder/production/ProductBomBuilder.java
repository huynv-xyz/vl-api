package com.vlife.api.builder.production;

import com.vlife.api.builder.ProductBuilder;
import com.vlife.shared.api.builder.ItemBuilder;
import com.vlife.shared.jdbc.dao.ProductDao;
import com.vlife.shared.jdbc.dao.production.ProductBomItemDao;
import com.vlife.shared.jdbc.entity.Product;
import com.vlife.shared.jdbc.entity.production.ProductBom;
import com.vlife.shared.jdbc.entity.production.ProductBomItem;
import com.vlife.shared.util.CommonUtil;
import jakarta.inject.Singleton;

import java.util.*;
import java.util.stream.Collectors;

@Singleton
public class ProductBomBuilder extends ItemBuilder<ProductBom> {

    private final ProductBomItemDao itemDao;
    private final ProductDao productDao;
    private final ProductBuilder productBuilder;

    public ProductBomBuilder(
            ProductBomItemDao itemDao,
            ProductDao productDao,
            ProductBuilder productBuilder
    ) {
        this.itemDao = itemDao;
        this.productDao = productDao;
        this.productBuilder = productBuilder;
    }

    @Override
    public Map<String, Object> buildItemFull(ProductBom item) {
        if (item == null) return Map.of();
        return buildList(Collections.singletonList(item)).get(0);
    }

    @Override
    public List<Map<String, Object>> buildList(List<ProductBom> items) {
        if (CommonUtil.isNullOrEmpty(items)) {
            return Collections.emptyList();
        }

        Set<Integer> bomIds = items.stream()
                .map(ProductBom::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Set<Integer> productIds = items.stream()
                .map(ProductBom::getProductId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<Integer, List<ProductBomItem>> bomItemMap =
                bomIds.isEmpty()
                        ? Collections.emptyMap()
                        : itemDao.findByBomIds(bomIds)
                        .stream()
                        .collect(Collectors.groupingBy(ProductBomItem::getBomId));

        bomItemMap.values().forEach(list ->
                list.forEach(i -> {
                    if (i.getMaterialProductId() != null) {
                        productIds.add(i.getMaterialProductId());
                    }
                })
        );

        Map<Integer, Product> productMap =
                productIds.isEmpty()
                        ? Collections.emptyMap()
                        : productDao.findByIdsAsMap(productIds);

        List<Map<String, Object>> result = new ArrayList<>();

        for (ProductBom bom : items) {
            Map<String, Object> x = new LinkedHashMap<>(autoBuild(bom));

            Product product = productMap.get(bom.getProductId());
            x.put("product", product != null ? productBuilder.buildItem(product) : null);

            List<Map<String, Object>> itemRes = new ArrayList<>();

            for (ProductBomItem bi : bomItemMap.getOrDefault(bom.getId(), List.of())) {
                Map<String, Object> m = new LinkedHashMap<>(autoBuildAny(bi));

                Product material = productMap.get(bi.getMaterialProductId());
                m.put("material_product", material != null ? productBuilder.buildItem(material) : null);

                itemRes.add(m);
            }

            x.put("items", itemRes);

            result.add(x);
        }

        return result;
    }
}