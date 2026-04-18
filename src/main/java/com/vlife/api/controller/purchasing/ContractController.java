package com.vlife.api.controller.purchasing;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.vlife.api.builder.purchasing.ContractBuilder;
import com.vlife.api.controller.base.BaseCrudController;
import com.vlife.api.util.ApiUtil;
import com.vlife.shared.api.dto.ApiResponse;
import com.vlife.shared.jdbc.dao.purchasing.ContractDao;
import com.vlife.shared.jdbc.entity.purchasing.Contract;
import com.vlife.shared.util.CommonUtil;
import io.micronaut.data.model.Page;
import io.micronaut.data.model.Pageable;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.*;
import io.micronaut.serde.annotation.Serdeable;
import jakarta.inject.Inject;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

@Controller("/purchasing/contracts")
public class ContractController extends BaseCrudController<Contract, Integer, ContractDao> {

    @Inject
    public ContractController(ContractDao dao, ContractBuilder builder) {
        super(dao, builder);
    }

    @Override
    protected Page<Contract> doSearch(Map<String, String> filters, Pageable pageable) {

        return dao.search(
                ApiUtil.trim(filters.get("keyword")),
                ApiUtil.parseInteger(filters.get("product_id")),
                ApiUtil.trim(filters.get("product_keyword")),

                ApiUtil.parseInteger(filters.get("supplier_id")),
                ApiUtil.toDateTime(filters.get("signed_date_from")),
                ApiUtil.toDateTime(filters.get("signed_date_to")),

                pageable
        );
    }

    // ========================
    // CREATE
    // ========================
    @Post
    public HttpResponse<?> create(@Body ContractCreateRequest req) {
        return handleCreate(req, r -> {

            Contract x = new Contract();

            x.setCode(ApiUtil.trim(r.getCode()));
            x.setSupplierId(r.getSupplierId());
            x.setCurrencyId(r.getCurrencyId());
            x.setSignedDate(ApiUtil.toDate(r.getSignedDate()));

            x.setStatus(r.getStatus() != null ? r.getStatus() : "DRAFT");

            x.setDepositRate(ApiUtil.nvl(r.getDepositRate()));
            x.setDepositDate(ApiUtil.toDate(r.getDepositDate()));

            x.setVatRate(ApiUtil.nvl(r.getVatRate()));
            x.setImportTaxRate(ApiUtil.nvl(r.getImportTaxRate()));

            x.setCreatedAt(LocalDateTime.now());
            x.setUpdatedAt(LocalDateTime.now());

            return x;
        });
    }

    // ========================
    // UPDATE
    // ========================
    @Put("/{id}")
    public HttpResponse<?> update(@PathVariable Integer id, @Body ContractUpdateRequest req) {
        return handleUpdate(id, req, r -> {

            Contract x = new Contract();

            x.setCode(ApiUtil.trim(r.getCode()));
            x.setSupplierId(r.getSupplierId());
            x.setCurrencyId(r.getCurrencyId());
            x.setSignedDate(ApiUtil.toDate(r.getSignedDate()));

            x.setStatus(r.getStatus());

            x.setDepositRate(ApiUtil.nvl(r.getDepositRate()));
            x.setDepositDate(ApiUtil.toDate(r.getDepositDate()));

            x.setVatRate(ApiUtil.nvl(r.getVatRate()));
            x.setImportTaxRate(ApiUtil.nvl(r.getImportTaxRate()));

            x.setUpdatedAt(LocalDateTime.now());

            mergeNullFromDb(id, x, "id", "createdAt");

            return x;
        });
    }

    // ========================
    // VALIDATION
    // ========================
    @Override
    protected <REQ> ApiResponse<?> validateBeforeCreate(Contract entity, REQ req) {

        if (CommonUtil.isNullOrEmpty(entity.getCode()))
            return ApiResponse.error(-400, "code is required");

        if (entity.getSupplierId() == null)
            return ApiResponse.error(-400, "supplier_id is required");

        if (entity.getCurrencyId() == null)
            return ApiResponse.error(-400, "currency_id is required");

        if (dao.findByCode(entity.getCode()).isPresent())
            return ApiResponse.error(-400, "code already exists");

        return validateFinance(entity);
    }

    @Override
    protected <REQ> ApiResponse<?> validateBeforeUpdate(Integer id, Contract entity, REQ req) {

        if (CommonUtil.isNullOrEmpty(entity.getCode()))
            return ApiResponse.error(-400, "code is required");

        var oldOpt = dao.findById(id);
        if (oldOpt.isEmpty())
            return ApiResponse.error(-404, "not found");

        var old = oldOpt.get();

        if (!entity.getCode().equals(old.getCode())
                && dao.findByCode(entity.getCode()).isPresent()) {
            return ApiResponse.error(-400, "code already exists");
        }

        return validateFinance(entity);
    }

    private ApiResponse<?> validateFinance(Contract entity) {

        BigDecimal depositRate = ApiUtil.nvl(entity.getDepositRate());
        BigDecimal vatRate = ApiUtil.nvl(entity.getVatRate());
        BigDecimal importTaxRate = ApiUtil.nvl(entity.getImportTaxRate());

        if (depositRate.compareTo(BigDecimal.ZERO) < 0 || depositRate.compareTo(new BigDecimal("100")) > 0)
            return ApiResponse.error(-400, "deposit_rate must be between 0-100");

        if (vatRate.compareTo(BigDecimal.ZERO) < 0 || vatRate.compareTo(new BigDecimal("100")) > 0)
            return ApiResponse.error(-400, "vat_rate must be between 0-100");

        if (importTaxRate.compareTo(BigDecimal.ZERO) < 0 || importTaxRate.compareTo(new BigDecimal("100")) > 0)
            return ApiResponse.error(-400, "import_tax_rate must be between 0-100");

        return null;
    }

    // ========================
    // DTO
    // ========================
    @Setter
    @Getter
    @Serdeable
    public static class ContractCreateRequest {

        private String code;

        @JsonProperty("supplier_id")
        private Integer supplierId;

        @JsonProperty("currency_id")
        private Integer currencyId;

        @JsonProperty("signed_date")
        private String signedDate;

        private String status;

        @JsonProperty("deposit_rate")
        private BigDecimal depositRate;

        @JsonProperty("deposit_date")
        private String depositDate;

        @JsonProperty("vat_rate")
        private BigDecimal vatRate;

        @JsonProperty("import_tax_rate")
        private BigDecimal importTaxRate;
    }

    @Serdeable
    public static class ContractUpdateRequest extends ContractCreateRequest {}
}