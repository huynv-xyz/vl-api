package com.vlife.api.builder.purchasing;

import com.vlife.api.builder.ProductBuilder;
import com.vlife.api.util.ApiUtil;
import com.vlife.shared.api.builder.ItemBuilder;
import com.vlife.shared.jdbc.dao.ProductDao;
import com.vlife.shared.jdbc.dao.purchasing.ContractDao;
import com.vlife.shared.jdbc.dao.purchasing.CurrencyDao;
import com.vlife.shared.jdbc.dao.purchasing.ShipmentItemDao;
import com.vlife.shared.jdbc.entity.Product;
import com.vlife.shared.jdbc.entity.purchasing.Contract;
import com.vlife.shared.jdbc.entity.purchasing.ContractItem;
import com.vlife.shared.util.CommonUtil;
import jakarta.inject.Singleton;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

@Singleton
public class ContractItemBuilder extends ItemBuilder<ContractItem> {

    private final ProductDao productDao;
    private final ProductBuilder productBuilder;
    private final ShipmentItemDao shipmentItemDao;
    private final ContractDao contractDao;
    private final CurrencyDao currencyDao;

    public ContractItemBuilder(ProductDao productDao,
                               ProductBuilder productBuilder,
                               ShipmentItemDao shipmentItemDao,
                               ContractDao contractDao,
                               CurrencyDao currencyDao) {
        this.productDao = productDao;
        this.productBuilder = productBuilder;
        this.shipmentItemDao = shipmentItemDao;
        this.contractDao = contractDao;
        this.currencyDao = currencyDao;
    }

    public Map<String, Object> buildItemFull(ContractItem item) {
        if (item == null) return Map.of();

        Map<String, Object> x = new LinkedHashMap<>(autoBuild(item));

        Product product = null;
        if (item.getProductId() != null) {
            product = productDao.findById(item.getProductId()).orElse(null);
        }
        x.put("product", product != null ? productBuilder.buildItem(product) : null);

        Contract contract = null;
        if (item.getContractId() != null) {
            contract = contractDao.findById(item.getContractId()).orElse(null);
        }

        BigDecimal vatRate = contract != null ? ApiUtil.nvl(contract.getVatRate()) : BigDecimal.ZERO;
        BigDecimal importTaxRate = contract != null ? ApiUtil.nvl(contract.getImportTaxRate()) : BigDecimal.ZERO;
        BigDecimal exchangeRate = resolveExchangeRate(contract);
        BigDecimal handlingFee = contract != null ? ApiUtil.nvl(contract.getHandlingFee()) : BigDecimal.ZERO;

        Map<Integer, BigDecimal> shippedMap = item.getContractId() != null
                ? shipmentItemDao.sumQuantityByContractId(item.getContractId())
                : Collections.emptyMap();

        enrichCalculatedFields(x, item, vatRate, importTaxRate, exchangeRate, handlingFee);

        return x;
    }

    @Override
    public List<Map<String, Object>> buildList(List<ContractItem> items) {

        if (CommonUtil.isNullOrEmpty(items)) return Collections.emptyList();

        Set<Integer> productIds = items.stream()
                .map(ContractItem::getProductId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<Integer, Product> productMap = productIds.isEmpty()
                ? Collections.emptyMap()
                : productDao.findByIdsAsMap(productIds);

        Integer contractId = items.get(0).getContractId();
        Contract contract = contractId != null
                ? contractDao.findById(contractId).orElse(null)
                : null;

        BigDecimal vatRate = contract != null ? ApiUtil.nvl(contract.getVatRate()) : BigDecimal.ZERO;
        BigDecimal importTaxRate = contract != null ? ApiUtil.nvl(contract.getImportTaxRate()) : BigDecimal.ZERO;
        BigDecimal exchangeRate = resolveExchangeRate(contract);
        BigDecimal handlingFee = contract != null ? ApiUtil.nvl(contract.getHandlingFee()) : BigDecimal.ZERO;

        Map<Integer, BigDecimal> shippedMap = contractId != null
                ? shipmentItemDao.sumQuantityByContractId(contractId)
                : Collections.emptyMap();

        List<Map<String, Object>> list = new ArrayList<>(items.size());

        for (ContractItem item : items) {
            list.add(buildItemWithContext(item, productMap, vatRate, importTaxRate, exchangeRate, handlingFee, shippedMap));
        }

        return list;
    }

    public Map<String, Object> buildItemWithContext(
            ContractItem item,
            Map<Integer, Product> productMap,
            BigDecimal vatRate,
            BigDecimal importTaxRate,
            BigDecimal exchangeRate,
            BigDecimal handlingFee,
            Map<Integer, BigDecimal> shippedMap
    ) {
        if (item == null) return Map.of();

        Map<String, Object> x = new LinkedHashMap<>(autoBuild(item));

        Product p = productMap.get(item.getProductId());
        x.put("product", p != null ? productBuilder.buildItem(p) : null);

        enrichCalculatedFields(x, item, vatRate, importTaxRate, exchangeRate, handlingFee);

        return x;
    }

    private void enrichCalculatedFields(
            Map<String, Object> x,
            ContractItem item,
            BigDecimal vatRate,
            BigDecimal importTaxRate,
            BigDecimal exchangeRate,
            BigDecimal handlingFee
    ) {
        BigDecimal qty = ApiUtil.nvl(item.getQuantity());

        BigDecimal unitPrice = ApiUtil.nvl(item.getUnitPrice());
        BigDecimal discountAmount = ApiUtil.nvl(item.getDiscountAmount());

        BigDecimal packaging = ApiUtil.nvl(item.getPackagingPrice());
        BigDecimal freight = ApiUtil.nvl(item.getFreightPrice());

        BigDecimal basePrice = unitPrice.subtract(discountAmount);

        BigDecimal foreignPrice = basePrice
                .add(packaging)
                .add(freight);

        BigDecimal priceBeforeTax = foreignPrice;

        BigDecimal priceBeforeTaxVnd = foreignPrice
                .multiply(resolvePositive(exchangeRate))
                .add(ApiUtil.nvl(handlingFee));

        BigDecimal importTax = percentOf(priceBeforeTax, importTaxRate);
        BigDecimal importTaxVnd = percentOf(priceBeforeTaxVnd, importTaxRate);

        BigDecimal vatBase = priceBeforeTax.add(importTax);
        BigDecimal vat = percentOf(vatBase, vatRate);
        BigDecimal vatBaseVnd = priceBeforeTaxVnd.add(importTaxVnd);
        BigDecimal vatVnd = percentOf(vatBaseVnd, vatRate);

        BigDecimal finalPrice = priceBeforeTax
                .add(importTax)
                .add(vat);
        BigDecimal finalPriceVnd = priceBeforeTaxVnd
                .add(importTaxVnd)
                .add(vatVnd);

        BigDecimal totalAmount = qty.multiply(priceBeforeTax);
        BigDecimal totalAmountVnd = qty.multiply(priceBeforeTaxVnd);

        x.put("base_price", basePrice);
        x.put("packaging_price", packaging);
        x.put("freight_price", freight);

        x.put("foreign_price", foreignPrice);
        x.put("exchange_rate", resolvePositive(exchangeRate));
        x.put("handling_fee", ApiUtil.nvl(handlingFee));
        x.put("input_price", priceBeforeTax);
        x.put("price_before_tax", priceBeforeTax);
        x.put("input_price_vnd", priceBeforeTaxVnd);
        x.put("price_before_tax_vnd", priceBeforeTaxVnd);

        x.put("import_tax_rate", importTaxRate);
        x.put("vat_rate", vatRate);

        x.put("import_tax_amount", importTax);
        x.put("vat_amount", vat);
        x.put("import_tax_amount_vnd", importTaxVnd);
        x.put("vat_amount_vnd", vatVnd);

        x.put("final_price", finalPrice);
        x.put("final_price_vnd", finalPriceVnd);
        x.put("total_amount", totalAmount);
        x.put("total_amount_vnd", totalAmountVnd);
    }

    private BigDecimal percentOf(BigDecimal amount, BigDecimal rate) {
        if (amount == null || rate == null) return BigDecimal.ZERO;
        if (BigDecimal.ZERO.compareTo(rate) == 0) return BigDecimal.ZERO;

        return amount
                .multiply(rate)
                .divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP);
    }

    private BigDecimal resolveExchangeRate(Contract contract) {
        if (contract == null) {
            return BigDecimal.ONE;
        }

        if (contract.getExchangeRate() != null && contract.getExchangeRate().compareTo(BigDecimal.ZERO) > 0) {
            return contract.getExchangeRate();
        }

        if (contract.getCurrencyId() == null) {
            return BigDecimal.ONE;
        }

        return currencyDao.findById(contract.getCurrencyId())
                .map(currency -> currency.getExchangeRate() != null
                        ? BigDecimal.valueOf(currency.getExchangeRate())
                        : BigDecimal.ONE)
                .map(this::resolvePositive)
                .orElse(BigDecimal.ONE);
    }

    private BigDecimal resolvePositive(BigDecimal value) {
        if (value == null || value.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ONE;
        }
        return value;
    }
}
