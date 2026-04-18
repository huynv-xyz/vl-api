package com.vlife.api.controller.purchasing;

import com.vlife.api.builder.purchasing.ContractItemBuilder;
import com.vlife.api.controller.base.BaseCrudController;
import com.vlife.api.util.ApiUtil;
import com.vlife.shared.api.dto.ApiResponse;
import com.vlife.shared.jdbc.dao.purchasing.ContractItemDao;
import com.vlife.shared.jdbc.entity.purchasing.ContractItem;
import io.micronaut.data.model.Page;
import io.micronaut.data.model.Pageable;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.*;
import jakarta.inject.Inject;

import java.time.LocalDateTime;
import java.util.Map;

@Controller("/purchasing/contract-items")
public class ContractItemController extends BaseCrudController<ContractItem, Integer, ContractItemDao> {

    @Inject
    public ContractItemController(ContractItemDao dao, ContractItemBuilder builder) {
        super(dao, builder);
    }

    @Override
    protected Page<ContractItem> doSearch(Map<String, String> filters, Pageable pageable) {
        return dao.search(
                ApiUtil.parseInteger(filters.get("contract_id")),
                filters.get("keyword"),
                pageable
        );
    }

    @Post
    public HttpResponse<?> create(@Body ContractItem req) {
        return handleCreate(req, r -> {
            ContractItem x = new ContractItem();

            x.setContractId(r.getContractId());
            x.setProductId(r.getProductId());
            x.setQuantity(r.getQuantity());
            x.setUnitPrice(r.getUnitPrice());
            x.setDiscountAmount(r.getDiscountAmount());

            x.setPackagingPrice(ApiUtil.nvl(r.getPackagingPrice()));
            x.setFreightPrice(ApiUtil.nvl(r.getFreightPrice()));

            x.setCreatedAt(LocalDateTime.now());
            x.setUpdatedAt(LocalDateTime.now());

            return x;
        });
    }

    @Put("/{id}")
    public HttpResponse<?> update(@PathVariable Integer id, @Body ContractItem req) {
        return handleUpdate(id, req, r -> {
            ContractItem x = new ContractItem();

            x.setProductId(r.getProductId());
            x.setQuantity(r.getQuantity());
            x.setUnitPrice(r.getUnitPrice());
            x.setDiscountAmount(r.getDiscountAmount());

            // 🔥 NEW
            x.setPackagingPrice(ApiUtil.nvl(r.getPackagingPrice()));
            x.setFreightPrice(ApiUtil.nvl(r.getFreightPrice()));

            x.setUpdatedAt(LocalDateTime.now());

            mergeNullFromDb(id, x, "id", "contractId", "createdAt", "createdBy");

            return x;
        });
    }

    // ========================
    // VALIDATION (optional nhưng nên có)
    // ========================
    @Override
    protected <REQ> ApiResponse<?> validateBeforeCreate(ContractItem entity, REQ req) {
        if (entity.getContractId() == null) return ApiResponse.error(-400, "contract_id is required");
        if (entity.getProductId() == null) return ApiResponse.error(-400, "product_id is required");
        if (entity.getQuantity() == null) return ApiResponse.error(-400, "quantity is required");
        if (entity.getUnitPrice() == null) return ApiResponse.error(-400, "unit_price is required");
        return null;
    }

    @Override
    protected <REQ> ApiResponse<?> validateBeforeUpdate(Integer id, ContractItem entity, REQ req) {
        var oldOpt = dao.findById(id);
        if (oldOpt.isEmpty()) return ApiResponse.error(-404, "not found");

        if (entity.getProductId() == null) return ApiResponse.error(-400, "product_id is required");
        if (entity.getQuantity() == null) return ApiResponse.error(-400, "quantity is required");
        if (entity.getUnitPrice() == null) return ApiResponse.error(-400, "unit_price is required");

        return null;
    }
}