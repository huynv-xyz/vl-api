package com.vlife.api.builder;

import com.vlife.api.dto.SalesActualItem;
import com.vlife.shared.api.builder.ItemBuilder;
import jakarta.inject.Singleton;

import java.util.LinkedHashMap;
import java.util.Map;

@Singleton
public class SalesActualBuilder extends ItemBuilder<SalesActualItem> {

    @Override
    public Map<String, Object> buildItem(SalesActualItem item) {
        if (item == null) {
            return Map.of();
        }

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("actual", autoBuildAny(item.getActual()));
        out.put("target", autoBuildAny(item.getTarget()));
        out.put("employee", autoBuildAny(item.getEmployee()));
        return out;
    }
}