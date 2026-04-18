package com.vlife.api.builder.purchasing;

import com.vlife.api.builder.ProductBuilder;
import com.vlife.api.util.ApiUtil;
import com.vlife.shared.api.builder.ItemBuilder;
import com.vlife.shared.jdbc.dao.ProductDao;
import com.vlife.shared.jdbc.dao.purchasing.ContractDao;
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

    public ContractItemBuilder(ProductDao productDao,
                               ProductBuilder productBuilder,
                               ShipmentItemDao shipmentItemDao,
                               ContractDao contractDao) {
        this.productDao = productDao;
        this.productBuilder = productBuilder;
        this.shipmentItemDao = shipmentItemDao;
        this.contractDao = contractDao;
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

        Map<Integer, BigDecimal> shippedMap = item.getContractId() != null
                ? shipmentItemDao.sumQuantityByContractId(item.getContractId())
                : Collections.emptyMap();

        enrichCalculatedFields(x, item, vatRate, importTaxRate);

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

        Map<Integer, BigDecimal> shippedMap = contractId != null
                ? shipmentItemDao.sumQuantityByContractId(contractId)
                : Collections.emptyMap();

        List<Map<String, Object>> list = new ArrayList<>(items.size());

        for (ContractItem item : items) {
            list.add(buildItemWithContext(item, productMap, vatRate, importTaxRate, shippedMap));
        }

        return list;
    }

    public Map<String, Object> buildItemWithContext(
            ContractItem item,
            Map<Integer, Product> productMap,
            BigDecimal vatRate,
            BigDecimal importTaxRate,
            Map<Integer, BigDecimal> shippedMap
    ) {
        if (item == null) return Map.of();

        Map<String, Object> x = new LinkedHashMap<>(autoBuild(item));

        Product p = productMap.get(item.getProductId());
        x.put("product", p != null ? productBuilder.buildItem(p) : null);

        enrichCalculatedFields(x, item, vatRate, importTaxRate);

        return x;
    }

    private void enrichCalculatedFields(
            Map<String, Object> x,
            ContractItem item,
            BigDecimal vatRate,
            BigDecimal importTaxRate
    ) {
        BigDecimal qty = ApiUtil.nvl(item.getQuantity());

        BigDecimal unitPrice = ApiUtil.nvl(item.getUnitPrice());
        BigDecimal discountAmount = ApiUtil.nvl(item.getDiscountAmount());

        BigDecimal packaging = ApiUtil.nvl(item.getPackagingPrice());
        BigDecimal freight = ApiUtil.nvl(item.getFreightPrice());

        BigDecimal basePrice = unitPrice.subtract(discountAmount);

        BigDecimal priceBeforeTax = basePrice
                .add(packaging)
                .add(freight);

        BigDecimal importTax = percentOf(priceBeforeTax, importTaxRate);

        BigDecimal vatBase = priceBeforeTax.add(importTax);
        BigDecimal vat = percentOf(vatBase, vatRate);

        BigDecimal finalPrice = priceBeforeTax
                .add(importTax)
                .add(vat);

        BigDecimal totalAmount = qty.multiply(finalPrice);

        x.put("base_price", basePrice);
        x.put("packaging_price", packaging);
        x.put("freight_price", freight);

        x.put("price_before_tax", priceBeforeTax);

        x.put("import_tax_rate", importTaxRate);
        x.put("vat_rate", vatRate);

        x.put("import_tax_amount", importTax);
        x.put("vat_amount", vat);

        x.put("final_price", finalPrice);
        x.put("total_amount", totalAmount);
    }

    private BigDecimal percentOf(BigDecimal amount, BigDecimal rate) {
        if (amount == null || rate == null) return BigDecimal.ZERO;
        if (BigDecimal.ZERO.compareTo(rate) == 0) return BigDecimal.ZERO;

        return amount
                .multiply(rate)
                .divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP);
    }
}