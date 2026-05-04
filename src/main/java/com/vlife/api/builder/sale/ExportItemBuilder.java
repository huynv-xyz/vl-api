package com.vlife.api.builder.sale;

import com.vlife.api.builder.ProductBuilder;
import com.vlife.shared.api.builder.ItemBuilder;
import com.vlife.shared.jdbc.dao.ProductDao;
import com.vlife.shared.jdbc.entity.Product;
import com.vlife.shared.jdbc.entity.sale.ExportItem;
import com.vlife.shared.util.CommonUtil;
import jakarta.inject.Singleton;

import java.util.*;
import java.util.stream.Collectors;

@Singleton
public class ExportItemBuilder extends ItemBuilder<ExportItem> {

    private final ProductDao productDao;
    private final ProductBuilder productBuilder;

    public ExportItemBuilder(
            ProductDao productDao,
            ProductBuilder productBuilder
    ) {
        this.productDao = productDao;
        this.productBuilder = productBuilder;
    }

    public Map<String, Object> buildItemFull(ExportItem item) {

        if (item == null) return Map.of();

        Map<String, Object> x = new LinkedHashMap<>(autoBuild(item));

        if (item.getProductId() != null) {
            Product p = productDao.findById(item.getProductId()).orElse(null);
            if (p != null) {
                x.put("product", productBuilder.buildItem(p));
            }
        }

        return x;
    }

    @Override
    public List<Map<String, Object>> buildList(List<ExportItem> items) {

        if (CommonUtil.isNullOrEmpty(items)) {
            return Collections.emptyList();
        }

        Set<Integer> productIds = items.stream()
                .map(ExportItem::getProductId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<Integer, Product> productMap =
                productIds.isEmpty()
                        ? Collections.emptyMap()
                        : productDao.findByIdsAsMap(productIds);

        List<Map<String, Object>> list = new ArrayList<>(items.size());

        for (ExportItem item : items) {

            Map<String, Object> x = new LinkedHashMap<>(autoBuild(item));

            Product p = productMap.get(item.getProductId());

            if (p != null) {
                x.put("product", productBuilder.buildItem(p));
            }

            list.add(x);
        }

        return list;
    }
}