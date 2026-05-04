package com.vlife.api.controller.inventory;

import com.vlife.api.builder.inventory.InventorySummaryBuilder;
import com.vlife.api.controller.base.BaseController;
import com.vlife.shared.api.dto.ApiResponse;
import com.vlife.shared.jdbc.dao.sale.InventoryLotDao;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.*;
import jakarta.annotation.Nullable;

import java.util.List;
import java.util.Map;

@Controller("/inventory/summary")
public class InventorySummaryController extends BaseController {

    private static final int MAX_LIMIT = 200;
    private static final int MAX_PAGE = 10_000;

    private final InventoryLotDao inventoryLotDao;
    private final InventorySummaryBuilder builder;

    public InventorySummaryController(
            InventoryLotDao inventoryLotDao,
            InventorySummaryBuilder builder
    ) {
        this.inventoryLotDao = inventoryLotDao;
        this.builder = builder;
    }

    @Get
    public HttpResponse<?> list(
            @QueryValue(defaultValue = "1") int page,
            @QueryValue(defaultValue = "20") int size,

            @Nullable @QueryValue("keyword") String keyword,
            @Nullable @QueryValue("product_id") Integer productId,
            @Nullable @QueryValue("warehouse_id") Integer warehouseId
    ) {
        int safePage = Math.min(Math.max(page, 1), MAX_PAGE);
        int safeSize = Math.min(size, MAX_LIMIT);
        int offset = (safePage - 1) * safeSize;

        long total = inventoryLotDao.countInventorySummary(
                keyword,
                productId,
                warehouseId
        );

        List<Map<String, Object>> items = inventoryLotDao.getInventorySummary(
                keyword,
                productId,
                warehouseId,
                safeSize,
                offset
        );

        int totalPage = total == 0
                ? 0
                : (int) Math.ceil((double) total / safeSize);

        Map<String, Object> result = Map.of(
                "page", safePage,
                "size", safeSize,
                "total", total,
                "current_page", safePage,
                "total_page", totalPage,
                "items", builder.buildList(items)
        );

        return HttpResponse.ok(ApiResponse.success(result));
    }
}