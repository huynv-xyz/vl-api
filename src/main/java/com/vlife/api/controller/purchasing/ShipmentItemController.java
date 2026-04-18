package com.vlife.api.controller.purchasing;

import com.vlife.api.builder.purchasing.ShipmentItemBuilder;
import com.vlife.api.controller.base.BaseCrudController;
import com.vlife.api.util.ApiUtil;
import com.vlife.shared.jdbc.dao.purchasing.ShipmentItemDao;
import com.vlife.shared.jdbc.entity.purchasing.ShipmentItem;
import io.micronaut.data.model.Page;
import io.micronaut.data.model.Pageable;
import io.micronaut.http.annotation.Controller;

import java.util.Map;

@Controller("/purchasing/shipment-items")
public class ShipmentItemController
        extends BaseCrudController<ShipmentItem, Integer, ShipmentItemDao> {

    public ShipmentItemController(
            ShipmentItemDao dao,
            ShipmentItemBuilder builder
    ) {
        super(dao, builder);
    }

    @Override
    protected Page<ShipmentItem> doSearch(Map<String, String> filters, Pageable pageable) {

        return dao.search(
                ApiUtil.parseInteger(filters.get("contract_id")),
                ApiUtil.trim(filters.get("keyword")),
                pageable
        );
    }
}