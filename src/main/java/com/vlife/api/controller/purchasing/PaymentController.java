package com.vlife.api.controller.purchasing;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.vlife.api.builder.purchasing.PaymentBuilder;
import com.vlife.api.controller.base.BaseCrudController;
import com.vlife.api.util.ApiUtil;
import com.vlife.shared.api.dto.ApiResponse;
import com.vlife.shared.jdbc.dao.purchasing.PaymentDao;
import com.vlife.shared.jdbc.entity.purchasing.Payment;
import com.vlife.shared.service.purchasing.PaymentService;
import io.micronaut.data.model.Page;
import io.micronaut.data.model.Pageable;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.*;
import io.micronaut.serde.annotation.Serdeable;
import jakarta.inject.Inject;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

import static com.vlife.shared.util.StringUtil.isBlank;
import static com.vlife.shared.util.StringUtil.trim;

@Controller("/purchasing/payments")
public class PaymentController extends BaseCrudController<Payment, Integer, PaymentDao> {
    @Inject
    PaymentService paymentService;

    @Inject
    public PaymentController(PaymentDao dao, PaymentBuilder builder) {
        super(dao, builder);
    }

    @Override
    protected Page<Payment> doSearch(Map<String, String> filters, Pageable pageable) {
        return dao.search(
                ApiUtil.parseInteger(filters.get("contract_id")),
                trim(filters.get("keyword")),
                pageable
        );
    }

    @Post
    public HttpResponse<?> create(@Body PaymentCreateRequest req) {

        Payment x = new Payment();
        x.setContractId(req.getContractId());
        x.setShipmentId(req.getShipmentId());
        x.setType(trim(req.getType()));
        x.setAmount(req.getAmount() != null ? req.getAmount() : BigDecimal.ZERO);
        x.setExchangeRate(
                req.getExchangeRate() != null ? req.getExchangeRate() : BigDecimal.ONE
        );
        x.setPaidAt(req.getPaidAt());
        x.setNote(trim(req.getNote()));
        x.setCreatedAt(LocalDateTime.now());

        Payment saved = paymentService.create(x);

        return HttpResponse.ok(ApiResponse.success(buildItemResponse(saved)));
    }

    @Put("/{id}")
    public HttpResponse<?> update(@PathVariable Integer id, @Body PaymentUpdateRequest req) {

        Payment x = new Payment();
        x.setContractId(req.getContractId());
        x.setShipmentId(req.getShipmentId());
        x.setType(trim(req.getType()));
        x.setAmount(req.getAmount());
        x.setExchangeRate(req.getExchangeRate());
        x.setPaidAt(req.getPaidAt());
        x.setNote(trim(req.getNote()));

        Payment updated = paymentService.update(id, x);

        return HttpResponse.ok(ApiResponse.success(buildItemResponse(updated)));
    }

    // ========================
    // VALIDATION
    // ========================
    @Override
    protected <REQ> ApiResponse<?> validateBeforeCreate(Payment entity, REQ req) {
        if (entity.getContractId() == null) return ApiResponse.error(-400, "contract_id is required");
        if (isBlank(entity.getType())) return ApiResponse.error(-400, "type is required");
        if (entity.getAmount() == null) return ApiResponse.error(-400, "amount is required");
        if (entity.getPaidAt() == null) return ApiResponse.error(-400, "paid_at is required");

        if (entity.getAmount().compareTo(BigDecimal.ZERO) < 0) {
            return ApiResponse.error(-400, "amount must be greater than or equal to 0");
        }

        return null;
    }

    @Override
    protected <REQ> ApiResponse<?> validateBeforeUpdate(Integer id, Payment entity, REQ req) {
        var oldOpt = dao.findById(id);
        if (oldOpt.isEmpty()) return ApiResponse.error(-404, "not found");

        if (entity.getContractId() == null) return ApiResponse.error(-400, "contract_id is required");
        if (isBlank(entity.getType())) return ApiResponse.error(-400, "type is required");
        if (entity.getAmount() == null) return ApiResponse.error(-400, "amount is required");
        if (entity.getPaidAt() == null) return ApiResponse.error(-400, "paid_at is required");

        if (entity.getAmount().compareTo(BigDecimal.ZERO) < 0) {
            return ApiResponse.error(-400, "amount must be greater than or equal to 0");
        }

        return null;
    }

    // ========================
    // REQUEST DTO
    // ========================
    @Setter
    @Getter
    @Serdeable
    public static class PaymentCreateRequest {

        @JsonProperty("contract_id")
        private Integer contractId;

        @JsonProperty("shipment_id")
        private Integer shipmentId;

        private String type;

        private BigDecimal amount;

        @JsonProperty("paid_at")
        private LocalDate paidAt;

        @JsonProperty("exchange_rate")
        private BigDecimal exchangeRate;

        private String note;

    }

    @Serdeable
    public static class PaymentUpdateRequest extends PaymentCreateRequest {
    }
}