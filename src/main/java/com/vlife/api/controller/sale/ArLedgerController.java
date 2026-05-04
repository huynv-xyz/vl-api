package com.vlife.api.controller.sale;

import com.vlife.api.builder.sale.ArLedgerBuilder;
import com.vlife.api.controller.base.BaseCrudController;
import com.vlife.api.util.ApiUtil;
import com.vlife.shared.jdbc.dao.sale.ArLedgerDao;
import com.vlife.shared.jdbc.entity.sale.ArLedger;
import io.micronaut.data.model.Page;
import io.micronaut.data.model.Pageable;
import io.micronaut.http.annotation.Controller;

import java.util.Map;

@Controller("/sales/ar-ledgers")
public class ArLedgerController extends BaseCrudController<ArLedger, Integer, ArLedgerDao> {

    public ArLedgerController(
            ArLedgerDao dao,
            ArLedgerBuilder builder
    ) {
        super(dao, builder);
    }

    @Override
    protected Page<ArLedger> doSearch(Map<String, String> filters, Pageable pageable) {
        return dao.search(
                ApiUtil.parseInteger(filters.get("customer_id")),
                ApiUtil.parseInteger(filters.get("order_id")),
                ApiUtil.parseInteger(filters.get("export_id")),
                ApiUtil.trim(filters.get("doc_type")),
                ApiUtil.toDate(filters.get("from_date")),
                ApiUtil.toDate(filters.get("to_date")),
                pageable
        );
    }
}