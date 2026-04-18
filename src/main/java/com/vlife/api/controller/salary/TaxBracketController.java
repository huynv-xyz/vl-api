package com.vlife.api.controller.salary;
import com.vlife.api.builder.TaxBracketBuilder;
import com.vlife.api.controller.base.BaseCrudController;
import com.vlife.shared.api.dto.ApiResponse;
import com.vlife.shared.jdbc.dao.salary.TaxBracketDao;
import com.vlife.shared.jdbc.entity.salary.TaxBracket;
import io.micronaut.data.model.Page;
import io.micronaut.data.model.Pageable;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.*;
import io.micronaut.serde.annotation.Serdeable;
import jakarta.inject.Inject;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

import static com.vlife.shared.util.NumberUtil.parseInt;

@Controller("/salary-sale/tax-brackets")
public class TaxBracketController extends BaseCrudController<TaxBracket, Integer, TaxBracketDao> {

    @Inject
    public TaxBracketController(TaxBracketDao dao, TaxBracketBuilder builder) {
        super(dao, builder);
    }

    @Override
    protected Page<TaxBracket> doSearch(Map<String, String> filters, Pageable pageable) {
        return dao.search(
                parseInt(filters.get("status")),
                parseInt(filters.get("period")),
                pageable
        );
    }

    @Post
    public HttpResponse<?> create(@Body TaxBracketRequest req) {
        return handleCreate(req, r -> {
            TaxBracket x = new TaxBracket();
            x.setBracketNo(r.getBracketNo());
            x.setIncomeFrom(r.getIncomeFrom());
            x.setIncomeTo(r.getIncomeTo());
            x.setTaxRate(r.getTaxRate());
            x.setQuickDeduction(r.getQuickDeduction() != null ? r.getQuickDeduction() : 0D);
            x.setEffectiveFrom(r.getEffectiveFrom());
            x.setEffectiveTo(r.getEffectiveTo());
            x.setStatus(r.getStatus() != null ? r.getStatus() : 1);
            x.setCreatedAt(LocalDateTime.now());
            x.setUpdatedAt(LocalDateTime.now());
            return x;
        });
    }

    @Put("/{id}")
    public HttpResponse<?> update(@PathVariable Integer id, @Body TaxBracketRequest req) {
        return handleUpdate(id, req, r -> {
            TaxBracket x = new TaxBracket();
            x.setBracketNo(r.getBracketNo());
            x.setIncomeFrom(r.getIncomeFrom());
            x.setIncomeTo(r.getIncomeTo());
            x.setTaxRate(r.getTaxRate());
            x.setQuickDeduction(r.getQuickDeduction());
            x.setEffectiveFrom(r.getEffectiveFrom());
            x.setEffectiveTo(r.getEffectiveTo());
            x.setStatus(r.getStatus());
            x.setUpdatedAt(LocalDateTime.now());
            mergeNullFromDb(id, x, "id", "createdAt");
            return x;
        });
    }

    @Override
    protected <REQ> ApiResponse<?> validateBeforeCreate(TaxBracket entity, REQ req) {
        if (entity.getBracketNo() == null) return ApiResponse.error(-400, "bracket_no is required");
        if (entity.getIncomeFrom() == null) return ApiResponse.error(-400, "income_from is required");
        if (entity.getTaxRate() == null) return ApiResponse.error(-400, "tax_rate is required");
        if (entity.getEffectiveFrom() == null) return ApiResponse.error(-400, "effective_from is required");
        return null;
    }

    @Override
    protected <REQ> ApiResponse<?> validateBeforeUpdate(Integer id, TaxBracket entity, REQ req) {
        if (entity.getBracketNo() == null) return ApiResponse.error(-400, "bracket_no is required");
        if (entity.getIncomeFrom() == null) return ApiResponse.error(-400, "income_from is required");
        if (entity.getTaxRate() == null) return ApiResponse.error(-400, "tax_rate is required");
        if (entity.getEffectiveFrom() == null) return ApiResponse.error(-400, "effective_from is required");
        return null;
    }

    @Serdeable
    public static class TaxBracketRequest {
        private Integer bracketNo;
        private Double incomeFrom;
        private Double incomeTo;
        private Double taxRate;
        private Double quickDeduction;
        private LocalDate effectiveFrom;
        private LocalDate effectiveTo;
        private Integer status;

        public Integer getBracketNo() { return bracketNo; }
        public void setBracketNo(Integer bracketNo) { this.bracketNo = bracketNo; }
        public Double getIncomeFrom() { return incomeFrom; }
        public void setIncomeFrom(Double incomeFrom) { this.incomeFrom = incomeFrom; }
        public Double getIncomeTo() { return incomeTo; }
        public void setIncomeTo(Double incomeTo) { this.incomeTo = incomeTo; }
        public Double getTaxRate() { return taxRate; }
        public void setTaxRate(Double taxRate) { this.taxRate = taxRate; }
        public Double getQuickDeduction() { return quickDeduction; }
        public void setQuickDeduction(Double quickDeduction) { this.quickDeduction = quickDeduction; }
        public LocalDate getEffectiveFrom() { return effectiveFrom; }
        public void setEffectiveFrom(LocalDate effectiveFrom) { this.effectiveFrom = effectiveFrom; }
        public LocalDate getEffectiveTo() { return effectiveTo; }
        public void setEffectiveTo(LocalDate effectiveTo) { this.effectiveTo = effectiveTo; }
        public Integer getStatus() { return status; }
        public void setStatus(Integer status) { this.status = status; }
    }
}