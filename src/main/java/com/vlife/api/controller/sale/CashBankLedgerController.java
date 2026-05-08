package com.vlife.api.controller.sale;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.vlife.api.builder.sale.CashBankLedgerBuilder;
import com.vlife.api.controller.base.BaseCrudController;
import com.vlife.api.util.ApiUtil;
import com.vlife.shared.api.dto.ApiResponse;
import com.vlife.shared.jdbc.dao.sale.CashBankLedgerDao;
import com.vlife.shared.jdbc.entity.sale.CashBankLedger;
import com.vlife.shared.service.sale.ArLedgerImportService;
import io.micronaut.data.model.Page;
import io.micronaut.data.model.Pageable;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.*;
import io.micronaut.http.multipart.CompletedFileUpload;
import io.micronaut.serde.annotation.Serdeable;
import jakarta.inject.Inject;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

@Controller("/sales/cash-bank-ledger")
public class CashBankLedgerController
        extends BaseCrudController<CashBankLedger, Integer, CashBankLedgerDao> {

    private final ArLedgerImportService importService;

    @Inject
    public CashBankLedgerController(
            CashBankLedgerDao dao,
            CashBankLedgerBuilder builder,
            ArLedgerImportService importService
    ) {
        super(dao, builder);
        this.importService = importService;
    }

    // ========================
    // SEARCH
    // ========================
    @Override
    protected Page<CashBankLedger> doSearch(Map<String, String> filters, Pageable pageable) {
        return dao.search(
                ApiUtil.trim(filters.get("keyword")),
                ApiUtil.parseInteger(filters.get("customer_id")),
                ApiUtil.toDate(filters.get("from_date")),
                ApiUtil.toDate(filters.get("to_date")),
                pageable
        );
    }

    // ========================
    // CREATE
    // ========================
    @Post
    public HttpResponse<?> create(@Body LedgerRequest req) {

        if ((req.getDebitAmount() == null || req.getDebitAmount().compareTo(BigDecimal.ZERO) == 0)
                && (req.getCreditAmount() == null || req.getCreditAmount().compareTo(BigDecimal.ZERO) == 0)) {

            return HttpResponse.badRequest(
                    ApiResponse.error(-400, "Phải có debit hoặc credit")
            );
        }

        CashBankLedger x = new CashBankLedger();

        x.setPostingDate(ApiUtil.toDate(req.getPostingDate()));
        x.setDocDate(ApiUtil.toDate(req.getDocDate()));
        x.setDocNo(req.getDocNo());

        x.setCustomerId(req.getCustomerId());
        x.setCustomerName(req.getCustomerName());

        x.setDescription(req.getDescription());
        x.setAccountCode(req.getAccountCode());

        x.setDebitAmount(req.getDebitAmount() != null ? req.getDebitAmount() : BigDecimal.ZERO);
        x.setCreditAmount(req.getCreditAmount() != null ? req.getCreditAmount() : BigDecimal.ZERO);

        x.setSourceType(req.getSourceType());
        x.setSourceId(req.getSourceId());

        x.setCreatedAt(LocalDateTime.now());
        x.setUpdatedAt(LocalDateTime.now());

        x = dao.insert(x);

        return HttpResponse.ok(
                ApiResponse.success(builder.buildItemFull(x))
        );
    }

    // ========================
    // UPDATE
    // ========================
    @Put("/{id}")
    public HttpResponse<?> update(
            Integer id,
            @Body LedgerRequest req
    ) {

        if (!dao.existsById(id)) {
            return HttpResponse.ok(ApiResponse.error(-404, "not found"));
        }

        CashBankLedger x = new CashBankLedger();

        x.setPostingDate(ApiUtil.toDate(req.getPostingDate()));
        x.setDocDate(ApiUtil.toDate(req.getDocDate()));
        x.setDocNo(req.getDocNo());

        x.setCustomerId(req.getCustomerId());
        x.setCustomerName(req.getCustomerName());

        x.setDescription(req.getDescription());
        x.setAccountCode(req.getAccountCode());

        x.setDebitAmount(req.getDebitAmount());
        x.setCreditAmount(req.getCreditAmount());

        x.setSourceType(req.getSourceType());
        x.setSourceId(req.getSourceId());

        x.setUpdatedAt(LocalDateTime.now());

        dao.updateSelective(id, x);

        CashBankLedger updated = dao.findById(id).orElse(null);

        return HttpResponse.ok(
                ApiResponse.success(builder.buildItemFull(updated))
        );
    }

    @Post(value = "/import", consumes = "multipart/form-data")
    public HttpResponse<?> importCsv(@Part("file") CompletedFileUpload file) {

        if (file == null || file.getFilename() == null) {
            return HttpResponse.badRequest(
                    ApiResponse.error(-400, "file is required")
            );
        }

        try {
            int count = importService.importCsv(file);

            return HttpResponse.ok(
                    ApiResponse.success(count)
            );

        } catch (Exception e) {
            return HttpResponse.serverError(
                    ApiResponse.error(-500, e.getMessage())
            );
        }
    }

    // ========================
    // DTO
    // ========================
    @Getter
    @Setter
    @Serdeable
    public static class LedgerRequest {

        @JsonProperty("posting_date")
        private String postingDate;

        @JsonProperty("doc_date")
        private String docDate;

        @JsonProperty("doc_no")
        private String docNo;

        @JsonProperty("customer_id")
        private Integer customerId;

        @JsonProperty("customer_name")
        private String customerName;

        private String description;

        @JsonProperty("account_code")
        private String accountCode;

        @JsonProperty("debit_amount")
        private BigDecimal debitAmount;

        @JsonProperty("credit_amount")
        private BigDecimal creditAmount;

        @JsonProperty("source_type")
        private String sourceType;

        @JsonProperty("source_id")
        private Integer sourceId;
    }
}