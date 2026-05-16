package com.vlife.api.controller.pricing;

import com.vlife.api.builder.pricing.PricingSnapshotItemBuilder;
import com.vlife.api.controller.base.BaseCrudController;
import com.vlife.shared.jdbc.dao.pricing.PricingSnapshotItemDao;
import com.vlife.shared.jdbc.entity.pricing.PricingSnapshotItem;
import io.micronaut.data.model.Page;
import io.micronaut.data.model.Pageable;
import io.micronaut.http.annotation.Controller;

import java.util.Map;

@Controller("/pricing/snapshot-items")
public class PricingSnapshotItemController extends BaseCrudController<PricingSnapshotItem, Integer, PricingSnapshotItemDao> {
    public PricingSnapshotItemController(PricingSnapshotItemDao dao, PricingSnapshotItemBuilder builder) {
        super(dao, builder);
    }

    @Override
    protected Page<PricingSnapshotItem> doSearch(Map<String, String> filters, Pageable pageable) {
        return dao.search(parseInt(filters.get("snapshot_id")), parseInt(filters.get("pricing_group_id")), parseInt(filters.get("product_id")), pageable);
    }

    private Integer parseInt(String value) {
        return value == null || value.isBlank() ? null : Integer.valueOf(value);
    }
}
