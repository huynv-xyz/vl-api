package com.vlife.api.controller.sale;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.vlife.api.builder.sale.ReturnBuilder;
import com.vlife.api.controller.base.BaseCrudController;
import com.vlife.api.util.ApiUtil;
import com.vlife.shared.api.dto.ApiResponse;
import com.vlife.shared.jdbc.dao.sale.ReturnDao;
import com.vlife.shared.jdbc.dao.sale.ReturnItemDao;
import com.vlife.shared.jdbc.entity.sale.Return;
import com.vlife.shared.jdbc.entity.sale.ReturnItem;
import com.vlife.shared.service.sale.ReturnService;
import com.vlife.shared.util.CommonUtil;
import io.micronaut.data.model.Page;
import io.micronaut.data.model.Pageable;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.*;
import io.micronaut.serde.annotation.Serdeable;
import io.micronaut.transaction.annotation.Transactional;
import jakarta.inject.Inject;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Controller("/sales/returns")
public class ReturnController extends BaseCrudController<Return, Integer, ReturnDao> {

    private final ReturnItemDao returnItemDao;
    private final ReturnService returnService;

    @Inject
    public ReturnController(
            ReturnDao dao,
            ReturnBuilder builder,
            ReturnItemDao returnItemDao,
            ReturnService returnService
    ) {
        super(dao, builder);
        this.returnItemDao = returnItemDao;
        this.returnService = returnService;
    }

    // ========================
    // SEARCH
    // ========================
    @Override
    protected Page<Return> doSearch(Map<String, String> filters, Pageable pageable) {
        return dao.search(
                ApiUtil.parseInteger(filters.get("order_id")),
                ApiUtil.parseInteger(filters.get("export_id")),
                ApiUtil.trim(filters.get("status")),
                pageable
        );
    }

    // ========================
    // CREATE
    // ========================
    @Post
    @Transactional
    public HttpResponse<?> create(@Body ReturnCreateRequest req) {

        if (req.getOrderId() == null)
            return HttpResponse.badRequest(ApiResponse.error(-400, "order_id is required"));

        if (CommonUtil.isNullOrEmpty(req.getItems()))
            return HttpResponse.badRequest(ApiResponse.error(-400, "items is required"));

        Set<Integer> checkDup = new HashSet<>();

        for (ItemRequest i : req.getItems()) {

            if (i.getProductId() == null)
                return HttpResponse.badRequest(ApiResponse.error(-400, "product_id is required"));

            if (!checkDup.add(i.getProductId()))
                return HttpResponse.badRequest(ApiResponse.error(-400, "duplicate product_id"));

            if (i.getQuantity() == null || i.getQuantity().compareTo(BigDecimal.ZERO) <= 0)
                return HttpResponse.badRequest(ApiResponse.error(-400, "quantity must > 0"));
        }

        Return r = new Return();

        r.setOrderId(req.getOrderId());
        r.setExportId(req.getExportId());
        r.setReturnNo(generateReturnNo());

        r.setStatus("NEW");
        r.setReason(ApiUtil.trim(req.getReason()));

        r.setCreatedAt(LocalDateTime.now());
        r.setUpdatedAt(LocalDateTime.now());

        r = dao.insert(r);

        List<ReturnItem> list = new ArrayList<>();

        for (ItemRequest i : req.getItems()) {

            ReturnItem item = new ReturnItem();

            item.setReturnId(r.getId());
            item.setProductId(i.getProductId());
            item.setQuantity(i.getQuantity());

            list.add(item);
        }

        returnItemDao.saveAll(list);

        return HttpResponse.ok(ApiResponse.success(r));
    }

    // ========================
    // UPDATE
    // ========================
    @Put("/{id}")
    @Transactional
    public HttpResponse<?> update(@PathVariable Integer id, @Body ReturnUpdateRequest req) {

        var oldOpt = dao.findById(id);
        if (oldOpt.isEmpty()) {
            return HttpResponse.notFound(ApiResponse.error(-404, "not found"));
        }

        Return old = oldOpt.get();

        if ("DONE".equals(old.getStatus())) {
            return HttpResponse.badRequest(ApiResponse.error(-400, "Return already DONE"));
        }

        if (CommonUtil.isNullOrEmpty(req.getItems()))
            return HttpResponse.badRequest(ApiResponse.error(-400, "items is required"));

        // ===== UPDATE RETURN
        Return x = new Return();

        if (req.getReason() != null)
            x.setReason(ApiUtil.trim(req.getReason()));

        x.setUpdatedAt(LocalDateTime.now());

        dao.updateSelective(id, x);

        // ===== UPSERT ITEMS
        List<ReturnItem> oldItems = returnItemDao.findByReturnId(id);

        Map<Integer, ReturnItem> oldMap = oldItems.stream()
                .collect(Collectors.toMap(ReturnItem::getProductId, i -> i));

        Set<Integer> newProductIds = new HashSet<>();
        List<ReturnItem> toInsert = new ArrayList<>();

        for (ItemRequest i : req.getItems()) {

            newProductIds.add(i.getProductId());

            ReturnItem oldItem = oldMap.get(i.getProductId());

            if (oldItem != null) {

                ReturnItem update = new ReturnItem();
                update.setQuantity(i.getQuantity());

                returnItemDao.updateSelective(oldItem.getId(), update);

            } else {

                ReturnItem insert = new ReturnItem();
                insert.setReturnId(id);
                insert.setProductId(i.getProductId());
                insert.setQuantity(i.getQuantity());

                toInsert.add(insert);
            }
        }

        for (ReturnItem oi : oldItems) {
            if (!newProductIds.contains(oi.getProductId())) {
                returnItemDao.deleteById(oi.getId());
            }
        }

        if (!toInsert.isEmpty()) {
            returnItemDao.saveAll(toInsert);
        }

        return HttpResponse.ok(ApiResponse.success(true));
    }

    @Put("/{id}/status")
    @Transactional
    public HttpResponse<?> updateStatus(
            @PathVariable Integer id,
            @Body UpdateStatusRequest req
    ) {

        var opt = dao.findById(id);
        if (opt.isEmpty()) {
            return HttpResponse.notFound(ApiResponse.error(-404, "not found"));
        }

        String newStatus = req.getStatus();

        if ("DONE".equals(newStatus)) {
            returnService.finishReturn(id);
            return HttpResponse.ok(ApiResponse.success(true));
        }

        Return x = new Return();
        x.setStatus(newStatus);
        x.setUpdatedAt(LocalDateTime.now());

        dao.updateSelective(id, x);

        return HttpResponse.ok(ApiResponse.success(true));
    }

    private String generateReturnNo() {

        String prefix = "RT-" + java.time.LocalDate.now()
                .format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"));

        Integer count = dao.countByPrefix(prefix);
        int next = count + 1;

        return prefix + "-" + String.format("%03d", next);
    }

    // ========================
    // DTO
    // ========================
    @Setter
    @Getter
    @Serdeable
    public static class ReturnCreateRequest {

        @JsonProperty("order_id")
        private Integer orderId;

        @JsonProperty("export_id")
        private Integer exportId;

        private String reason;

        private List<ItemRequest> items;
    }

    @Setter
    @Getter
    @Serdeable
    public static class ReturnUpdateRequest extends ReturnCreateRequest {}

    @Setter
    @Getter
    @Serdeable
    public static class ItemRequest {

        @JsonProperty("product_id")
        private Integer productId;

        private BigDecimal quantity;
    }

    @Setter
    @Getter
    @Serdeable
    public static class UpdateStatusRequest {
        private String status;
    }
}