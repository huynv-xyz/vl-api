package com.vlife.api.controller.inventory;

import com.vlife.api.builder.inventory.InventoryLedgerBuilder;
import com.vlife.api.controller.base.BaseCrudController;
import com.vlife.api.util.ApiUtil;
import com.vlife.shared.api.dto.ApiResponse;
import com.vlife.shared.jdbc.dao.sale.InventoryLedgerDao;
import com.vlife.shared.jdbc.entity.inventory.InventoryLedger;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.data.model.Page;
import io.micronaut.data.model.Pageable;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.QueryValue;

import java.util.Map;

@Controller("/inventory/ledger")
public class InventoryLedgerController extends BaseCrudController<InventoryLedger, Integer, InventoryLedgerDao> {

    public InventoryLedgerController(
            InventoryLedgerDao dao,
            InventoryLedgerBuilder builder
    ) {
        super(dao, builder);
    }

    @Override
    protected Page<InventoryLedger> doSearch(Map<String, String> filters, Pageable pageable) {
        return dao.search(
                ApiUtil.parseInteger(filters.get("product_id")),
                ApiUtil.parseInteger(filters.get("warehouse_id")),
                ApiUtil.trim(filters.get("doc_type")),
                ApiUtil.trim(filters.get("doc_no")),
                ApiUtil.toDate(filters.get("from_date")),
                ApiUtil.toDate(filters.get("to_date")),
                pageable
        );
    }

    @Get("/report")
    public HttpResponse<?> report(
            @QueryValue(defaultValue = "1") int page,
            @QueryValue(defaultValue = "20") int size,
            @Nullable @QueryValue("product_id") Integer productId,
            @Nullable @QueryValue("warehouse_id") Integer warehouseId,
            @Nullable @QueryValue("doc_type") String docType,
            @Nullable @QueryValue("doc_no") String docNo,
            @Nullable @QueryValue("from_date") String fromDate,
            @Nullable @QueryValue("to_date") String toDate
    ) {
        int safePage = Math.max(page, 1);
        int safeSize = Math.min(size, 200);
        int offset = (safePage - 1) * safeSize;

        long total = dao.countLedgerReport(
                productId,
                warehouseId,
                ApiUtil.trim(docType),
                ApiUtil.trim(docNo),
                ApiUtil.toDate(fromDate),
                ApiUtil.toDate(toDate)
        );

        var items = dao.getLedgerReport(
                productId,
                warehouseId,
                ApiUtil.trim(docType),
                ApiUtil.trim(docNo),
                ApiUtil.toDate(fromDate),
                ApiUtil.toDate(toDate),
                safeSize,
                offset
        );

        return HttpResponse.ok(
                ApiResponse.success(
                        buildPagedMapResponse(safePage, safeSize, total, items)
                )
        );
    }

}