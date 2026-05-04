package com.vlife.api.controller.sale;

import com.vlife.api.builder.sale.ExportBuilder;
import com.vlife.api.controller.base.BaseCrudController;
import com.vlife.api.util.ApiUtil;
import com.vlife.shared.jdbc.dao.sale.ExportDao;
import com.vlife.shared.jdbc.entity.sale.Export;
import io.micronaut.data.model.Page;
import io.micronaut.data.model.Pageable;
import io.micronaut.http.annotation.Controller;

import java.util.Map;

@Controller("/sales/exports")
public class ExportController extends BaseCrudController<Export, Integer, ExportDao> {

    public ExportController(
            ExportDao dao,
            ExportBuilder builder
    ) {
        super(dao, builder);
    }

    // ========================
    // SEARCH
    // ========================
    @Override
    protected Page<Export> doSearch(Map<String, String> filters, Pageable pageable) {
        return dao.search(
                ApiUtil.trim(filters.get("keyword")),
                ApiUtil.parseInteger(filters.get("order_id")),
                ApiUtil.parseInteger(filters.get("delivery_id")),
                ApiUtil.parseInteger(filters.get("warehouse_id")),
                ApiUtil.trim(filters.get("status")),
                ApiUtil.toDate(filters.get("from_date")),
                ApiUtil.toDate(filters.get("to_date")),
                pageable
        );
    }
}