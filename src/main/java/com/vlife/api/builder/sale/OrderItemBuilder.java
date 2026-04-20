package com.vlife.api.builder.sale;

import com.vlife.api.builder.ProductBuilder;
import com.vlife.shared.api.builder.ItemBuilder;
import com.vlife.shared.jdbc.dao.ProductDao;
import com.vlife.shared.jdbc.entity.Product;
import com.vlife.shared.jdbc.entity.sale.OrderItem;
import com.vlife.shared.util.CommonUtil;
import jakarta.inject.Singleton;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Singleton
public class OrderItemBuilder extends ItemBuilder<OrderItem> {

    private final ProductDao productDao;
    private final ProductBuilder productBuilder;

    public OrderItemBuilder(
            ProductDao productDao,
            ProductBuilder productBuilder
    ) {
        this.productDao = productDao;
        this.productBuilder = productBuilder;
    }

    public Map<String, Object> buildItemFull(OrderItem item) {
        if (item == null) return Map.of();

        Map<String, Object> x = new LinkedHashMap<>(autoBuild(item));

        Product p = item.getProductId() != null
                ? productDao.findById(item.getProductId()).orElse(null)
                : null;

        if (p != null) {
            x.put("product", productBuilder.buildItem(p));
        }

        // ===== calc line total
        BigDecimal discount = Optional.ofNullable(item.getDiscount()).orElse(BigDecimal.ZERO);

        BigDecimal lineTotal = item.getQuantity()
                .multiply(item.getUnitPrice())
                .subtract(discount);

        x.put("line_total", lineTotal);

        return x;
    }

    @Override
    public List<Map<String, Object>> buildList(List<OrderItem> items) {

        if (CommonUtil.isNullOrEmpty(items)) {
            return Collections.emptyList();
        }

        // ===== collect product ids
        Set<Integer> productIds = items.stream()
                .map(OrderItem::getProductId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<Integer, Product> productMap =
                productIds.isEmpty()
                        ? Collections.emptyMap()
                        : productDao.findByIdsAsMap(productIds);

        List<Map<String, Object>> list = new ArrayList<>(items.size());

        for (OrderItem item : items) {

            Map<String, Object> x = new LinkedHashMap<>(autoBuild(item));

            Product p = productMap.get(item.getProductId());

            if (p != null) {
                x.put("product", productBuilder.buildItem(p));
            }

            BigDecimal discount = Optional.ofNullable(item.getDiscount()).orElse(BigDecimal.ZERO);

            BigDecimal lineTotal = item.getQuantity()
                    .multiply(item.getUnitPrice())
                    .subtract(discount);

            x.put("line_total", lineTotal);

            list.add(x);
        }

        return list;
    }
}