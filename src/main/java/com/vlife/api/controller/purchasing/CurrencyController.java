package com.vlife.api.controller.purchasing;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.vlife.api.builder.purchasing.CurrencyBuilder;
import com.vlife.api.controller.base.BaseCrudController;
import com.vlife.shared.api.dto.ApiResponse;
import com.vlife.shared.jdbc.dao.purchasing.CurrencyDao;
import com.vlife.shared.jdbc.entity.purchasing.Currency;
import io.micronaut.data.model.Page;
import io.micronaut.data.model.Pageable;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.*;
import io.micronaut.serde.annotation.Serdeable;
import jakarta.inject.Inject;

import java.time.LocalDateTime;
import java.util.Map;

import static com.vlife.shared.util.StringUtil.isBlank;
import static com.vlife.shared.util.StringUtil.trim;

@Controller("/purchasing/currencies")
public class CurrencyController extends BaseCrudController<Currency, Integer, CurrencyDao> {

    @Inject
    public CurrencyController(CurrencyDao dao, CurrencyBuilder builder) {
        super(dao, builder);
    }

    @Override
    protected Page<Currency> doSearch(Map<String, String> filters, Pageable pageable) {
        return dao.search(
                trim(filters.get("keyword")),
                pageable
        );
    }

    @Post
    public HttpResponse<?> create(@Body CurrencyCreateRequest req) {
        return handleCreate(req, r -> {
            Currency x = new Currency();
            x.setCode(trim(r.getCode()));
            x.setName(trim(r.getName()));
            x.setSymbol(trim(r.getSymbol()));
            x.setExchangeRate(r.getExchangeRate());
            x.setCreatedAt(LocalDateTime.now());
            x.setUpdatedAt(LocalDateTime.now());
            return x;
        });
    }

    @Put("/{id}")
    public HttpResponse<?> update(@PathVariable Integer id, @Body CurrencyUpdateRequest req) {
        return handleUpdate(id, req, r -> {
            Currency x = new Currency();
            x.setCode(trim(r.getCode()));
            x.setName(trim(r.getName()));
            x.setSymbol(trim(r.getSymbol()));
            x.setExchangeRate(r.getExchangeRate());
            x.setUpdatedAt(LocalDateTime.now());

            mergeNullFromDb(id, x, "id", "createdAt", "createdBy");
            return x;
        });
    }

    @Override
    protected <REQ> ApiResponse<?> validateBeforeCreate(Currency entity, REQ req) {
        if (isBlank(entity.getCode())) return ApiResponse.error(-400, "code is required");
        if (isBlank(entity.getName())) return ApiResponse.error(-400, "name is required");
        if (dao.findByCode(entity.getCode()).isPresent()) return ApiResponse.error(-400, "code already exists");
        return null;
    }

    @Override
    protected <REQ> ApiResponse<?> validateBeforeUpdate(Integer id, Currency entity, REQ req) {
        if (isBlank(entity.getCode())) return ApiResponse.error(-400, "code is required");
        if (isBlank(entity.getName())) return ApiResponse.error(-400, "name is required");

        var oldOpt = dao.findById(id);
        if (oldOpt.isEmpty()) return ApiResponse.error(-404, "not found");

        var old = oldOpt.get();
        if (!entity.getCode().equals(old.getCode()) && dao.findByCode(entity.getCode()).isPresent()) {
            return ApiResponse.error(-400, "code already exists");
        }

        return null;
    }

    @Serdeable
    public static class CurrencyCreateRequest {
        private String code;
        private String name;
        private String symbol;

        @JsonProperty("exchange_rate")
        private Double exchangeRate;

        public String getCode() { return code; }
        public void setCode(String code) { this.code = code; }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getSymbol() { return symbol; }
        public void setSymbol(String symbol) { this.symbol = symbol; }

        public Double getExchangeRate() { return exchangeRate; }
        public void setExchangeRate(Double exchangeRate) { this.exchangeRate = exchangeRate; }
    }

    @Serdeable
    public static class CurrencyUpdateRequest extends CurrencyCreateRequest {
    }
}