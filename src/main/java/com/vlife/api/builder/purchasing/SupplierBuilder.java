package com.vlife.api.builder.purchasing;

import com.vlife.shared.api.builder.ItemBuilder;
import com.vlife.shared.jdbc.dao.purchasing.NationDao;
import com.vlife.shared.jdbc.entity.purchasing.Nation;
import com.vlife.shared.jdbc.entity.purchasing.Supplier;
import jakarta.inject.Singleton;

import java.util.*;
import java.util.stream.Collectors;

@Singleton
public class SupplierBuilder extends ItemBuilder<Supplier> {

    private final NationDao nationDao;
    private final NationBuilder nationBuilder;

    public SupplierBuilder(NationDao nationDao, NationBuilder nationBuilder) {
        this.nationDao = nationDao;
        this.nationBuilder = nationBuilder;
    }

    @Override
    public Map<String, Object> buildItem(Supplier item) {
        if (item == null) return Map.of();

        Map<String, Object> x = new LinkedHashMap<>(autoBuild(item));

        return x;
    }

    public Map<String, Object> buildItemFull(Supplier item) {
        Map<String, Object> builder = buildItem(item);
        if (item.getNationId() != null) {
            nationDao.findById(item.getNationId())
                    .ifPresentOrElse(
                            nation -> builder.put("nation", nationBuilder.buildItem(nation)),
                            () -> builder.put("nation", null)
                    );
        } else {
            builder.put("nation", null);
        }
        return builder;
    }

    @Override
    public List<Map<String, Object>> buildList(List<Supplier> items) {
        if (items == null || items.isEmpty()) return Collections.emptyList();

        Set<Integer> nationIds = items.stream()
                .map(Supplier::getNationId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<Integer, Nation> nationMap = nationDao.findByIdsAsMap(nationIds);

        List<Map<String, Object>> list = new ArrayList<>(items.size());

        for (Supplier item : items) {
            Map<String, Object> x = new LinkedHashMap<>(autoBuild(item));

            Nation nation = nationMap.get(item.getNationId());
            x.put("nation", nation != null ? nationBuilder.buildItem(nation) : null);

            list.add(x);
        }

        return list;
    }
}