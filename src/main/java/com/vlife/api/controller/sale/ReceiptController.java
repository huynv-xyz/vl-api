package com.vlife.api.controller.sale;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.vlife.api.builder.sale.ReceiptBuilder;
import com.vlife.api.controller.base.BaseCrudController;
import com.vlife.api.util.ApiUtil;
import com.vlife.shared.api.dto.ApiResponse;
import com.vlife.shared.jdbc.dao.sale.ReceiptDao;
import com.vlife.shared.jdbc.entity.sale.Receipt;
import com.vlife.shared.service.sale.ReceiptService;
import io.micronaut.data.model.Page;
import io.micronaut.data.model.Pageable;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Post;
import io.micronaut.http.annotation.Put;
import io.micronaut.serde.annotation.Serdeable;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.Map;

@Controller("/sales/receipts")
public class ReceiptController extends BaseCrudController<Receipt, Integer, ReceiptDao> {

    private final ReceiptService receiptService;

    public ReceiptController(
            ReceiptDao dao,
            ReceiptService receiptService,
            ReceiptBuilder builder
    ) {
        super(dao, builder);
        this.receiptService = receiptService;
    }

    @Override
    protected Page<Receipt> doSearch(Map<String, String> filters, Pageable pageable) {
        return dao.search(
                ApiUtil.trim(filters.get("keyword")),
                ApiUtil.parseInteger(filters.get("order_id")),
                ApiUtil.parseInteger(filters.get("customer_id")),
                ApiUtil.trim(filters.get("status")),
                ApiUtil.toDate(filters.get("from_date")),
                ApiUtil.toDate(filters.get("to_date")),
                pageable
        );
    }

    @Post
    public HttpResponse<?> createReceipt(@Body ReceiptRequest req) {

        if (req.getOrderId() == null) {
            return HttpResponse.badRequest(
                    ApiResponse.error(-400, "order_id required")
            );
        }

        if (req.getAmount() == null || req.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            return HttpResponse.badRequest(
                    ApiResponse.error(-400, "amount must > 0")
            );
        }

        Receipt r = receiptService.createReceipt(
                req.getOrderId(),
                req.getAmount(),
                ApiUtil.toDate(req.getReceiptDate()),
                req.getMethod(),
                ApiUtil.trim(req.getNote())
        );

        return HttpResponse.ok(ApiResponse.success(buildItemResponse(r)));
    }

    // ========================
    // UPDATE
    // ========================
    @Put("/{id}")
    public HttpResponse<?> updateReceipt(
            Integer id,
            @Body ReceiptRequest req
    ) {

        if (req.getAmount() == null || req.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            return HttpResponse.badRequest(
                    ApiResponse.error(-400, "amount must > 0")
            );
        }

        try {
            Receipt r = receiptService.updateReceipt(
                    id,
                    req.getAmount(),
                    ApiUtil.toDate(req.getReceiptDate()),
                    req.getMethod(),
                    ApiUtil.trim(req.getNote())
            );

            return HttpResponse.ok(ApiResponse.success(buildItemResponse(r)));

        } catch (Exception e) {
            return HttpResponse.badRequest(
                    ApiResponse.error(-400, e.getMessage())
            );
        }
    }

    // ========================
    // DTO
    // ========================
    @Getter
    @Setter
    @Serdeable
    public static class ReceiptRequest {

        @JsonProperty("order_id")
        private Integer orderId;

        private BigDecimal amount;

        @JsonProperty("receipt_date")
        private String receiptDate;

        private String method;
        private String note;
    }
}