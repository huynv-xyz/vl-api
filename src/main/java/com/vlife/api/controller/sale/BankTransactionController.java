package com.vlife.api.controller.sale;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.vlife.api.controller.base.BaseCrudController;
import com.vlife.api.util.ApiUtil;
import com.vlife.shared.api.dto.ApiResponse;
import com.vlife.shared.jdbc.dao.sale.BankTransactionDao;
import com.vlife.shared.jdbc.entity.sale.BankTransaction;
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
import java.time.LocalDateTime;
import java.util.Map;

@Controller("/sales/bank-transactions")
public class BankTransactionController
        extends BaseCrudController<BankTransaction, Integer, BankTransactionDao> {

    public BankTransactionController(BankTransactionDao dao) {
        super(dao, null);
    }

    @Override
    protected Page<BankTransaction> doSearch(Map<String, String> filters, Pageable pageable) {
        return dao.search(
                ApiUtil.trim(filters.get("keyword")),
                ApiUtil.toDate(filters.get("from_date")),
                ApiUtil.toDate(filters.get("to_date")),
                pageable
        );
    }

    // ========================
    // CREATE
    // ========================
    @Post
    public HttpResponse<?> create(@Body BankTxnRequest req) {

        if (req.getAmount() == null) {
            return HttpResponse.badRequest(ApiResponse.error(-400, "amount required"));
        }

        BankTransaction t = new BankTransaction();

        t.setTxnDate(ApiUtil.toDate(req.getTxnDate()));
        t.setAmount(req.getAmount());
        t.setDescription(ApiUtil.trim(req.getDescription()));
        t.setReferenceNo(ApiUtil.trim(req.getReferenceNo()));
        t.setBankAccount(ApiUtil.trim(req.getBankAccount()));
        t.setCustomerName(ApiUtil.trim(req.getCustomerName()));
        t.setCustomerId(req.getCustomerId());

        t.setCreatedAt(LocalDateTime.now());
        t.setUpdatedAt(LocalDateTime.now());

        t = dao.insert(t);

        return HttpResponse.ok(ApiResponse.success(t));
    }

    // ========================
    // UPDATE
    // ========================
    @Put("/{id}")
    public HttpResponse<?> update(
            Integer id,
            @Body BankTxnRequest req
    ) {

        if (!dao.existsById(id)) {
            return HttpResponse.ok(ApiResponse.error(-404, "not found"));
        }

        BankTransaction t = new BankTransaction();

        t.setTxnDate(ApiUtil.toDate(req.getTxnDate()));
        t.setAmount(req.getAmount());
        t.setDescription(ApiUtil.trim(req.getDescription()));
        t.setReferenceNo(ApiUtil.trim(req.getReferenceNo()));
        t.setBankAccount(ApiUtil.trim(req.getBankAccount()));
        t.setCustomerName(ApiUtil.trim(req.getCustomerName()));
        t.setCustomerId(req.getCustomerId());

        dao.updateSelective(id, t);

        return HttpResponse.ok(ApiResponse.success("updated"));
    }

    // ========================
    // DTO
    // ========================
    @Getter
    @Setter
    @Serdeable
    public static class BankTxnRequest {

        @JsonProperty("txn_date")
        private String txnDate;

        private BigDecimal amount;

        private String description;

        @JsonProperty("reference_no")
        private String referenceNo;

        @JsonProperty("bank_account")
        private String bankAccount;

        @JsonProperty("customer_name")
        private String customerName;

        @JsonProperty("customer_id")
        private Integer customerId;
    }
}