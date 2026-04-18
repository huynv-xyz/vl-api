package com.vlife.api.builder.purchasing;

import com.vlife.api.builder.ProductBuilder;
import com.vlife.shared.api.builder.ItemBuilder;
import com.vlife.shared.jdbc.dao.ProductDao;
import com.vlife.shared.jdbc.dao.purchasing.*;
import com.vlife.shared.jdbc.entity.Product;
import com.vlife.shared.jdbc.entity.purchasing.*;
import com.vlife.shared.jdbc.entity.purchasing.Currency;
import com.vlife.shared.service.purchasing.ContractService;
import com.vlife.shared.util.CommonUtil;
import jakarta.inject.Singleton;

import java.util.*;
import java.util.stream.Collectors;

@Singleton
public class ContractBuilder extends ItemBuilder<Contract> {

    private final SupplierDao supplierDao;
    private final SupplierBuilder supplierBuilder;

    private final CurrencyDao currencyDao;
    private final CurrencyBuilder currencyBuilder;

    private final NationDao nationDao;
    private final NationBuilder nationBuilder;

    private final ContractItemDao contractItemDao;
    private final ProductDao productDao;
    private final ProductBuilder productBuilder;

    private final ContractService contractService;

    public ContractBuilder(
            SupplierDao supplierDao,
            SupplierBuilder supplierBuilder,
            CurrencyDao currencyDao,
            CurrencyBuilder currencyBuilder,
            NationDao nationDao,
            NationBuilder nationBuilder,
            ContractItemDao contractItemDao,
            ProductDao productDao,
            ProductBuilder productBuilder,
            ContractService contractService
    ) {
        this.supplierDao = supplierDao;
        this.supplierBuilder = supplierBuilder;
        this.currencyDao = currencyDao;
        this.currencyBuilder = currencyBuilder;
        this.nationDao = nationDao;
        this.nationBuilder = nationBuilder;
        this.contractItemDao = contractItemDao;
        this.productDao = productDao;
        this.productBuilder = productBuilder;
        this.contractService = contractService;
    }

    // ========================
    // BUILD SINGLE (DETAIL)
    // ========================
    @Override
    public Map<String, Object> buildItem(Contract item) {
        if (item == null) return Map.of();

        Map<String, Object> x = new LinkedHashMap<>(autoBuild(item));

        Supplier supplier = item.getSupplierId() != null
                ? supplierDao.findById(item.getSupplierId()).orElse(null)
                : null;

        Currency currency = item.getCurrencyId() != null
                ? currencyDao.findById(item.getCurrencyId()).orElse(null)
                : null;

        Nation nation = (supplier != null && supplier.getNationId() != null)
                ? nationDao.findById(supplier.getNationId()).orElse(null)
                : null;

        x.put("supplier", buildSupplierWithNation(supplier, nation));
        x.put("currency", currency != null ? currencyBuilder.buildItem(currency) : null);

        // ===== summary full (có shipment + payment)
        ContractService.ContractSummary summary =
                contractService.calcSummaryByContractId(item.getId());

        x.put("total_quantity", summary.getTotalQuantity());
        x.put("total_amount", summary.getTotalAmount());
        x.put("total_defect_quantity", summary.getTotalDefectQuantity());
        x.put("total_defect_amount", summary.getTotalDefectAmount());
        x.put("real_quantity", summary.getRealQuantity());
        x.put("real_amount", summary.getRealAmount());
        x.put("total_paid_amount", summary.getTotalPaidAmount());
        x.put("remaining_amount", summary.getRemainingAmount());

        return x;
    }

    @Override
    public List<Map<String, Object>> buildList(List<Contract> items) {

        if (CommonUtil.isNullOrEmpty(items)) {
            return Collections.emptyList();
        }

        // ===== collect ids
        Set<Integer> supplierIds = items.stream()
                .map(Contract::getSupplierId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Set<Integer> currencyIds = items.stream()
                .map(Contract::getCurrencyId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Set<Integer> contractIds = items.stream()
                .map(Contract::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        // ===== load master data
        Map<Integer, Supplier> supplierMap = supplierDao.findByIdsAsMap(supplierIds);
        Map<Integer, Currency> currencyMap = currencyDao.findByIdsAsMap(currencyIds);

        Set<Integer> nationIds = supplierMap.values().stream()
                .map(Supplier::getNationId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<Integer, Nation> nationMap = nationDao.findByIdsAsMap(nationIds);

        // ===== contract_items (1 query)
        Map<Integer, List<ContractItem>> contractItemMap =
                contractItemDao.findByContractIds(contractIds)
                        .stream()
                        .collect(Collectors.groupingBy(ContractItem::getContractId));

        // ===== product (1 query)
        Set<Integer> productIds = contractItemMap.values().stream()
                .flatMap(List::stream)
                .map(ContractItem::getProductId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<Integer, Product> productMap =
                productIds.isEmpty()
                        ? Collections.emptyMap()
                        : productDao.findByIdsAsMap(productIds);

        // ===== build result
        List<Map<String, Object>> list = new ArrayList<>(items.size());

        for (Contract item : items) {

            Map<String, Object> x = new LinkedHashMap<>(autoBuild(item));

            Supplier supplier = supplierMap.get(item.getSupplierId());
            Currency currency = currencyMap.get(item.getCurrencyId());

            Nation nation = supplier != null
                    ? nationMap.get(supplier.getNationId())
                    : null;

            x.put("supplier", buildSupplierWithNation(supplier, nation));
            x.put("currency", currency != null ? currencyBuilder.buildItem(currency) : null);

            List<ContractItem> its =
                    contractItemMap.getOrDefault(item.getId(), List.of());

            List<Map<String, Object>> itemRes = new ArrayList<>();

            for (ContractItem ci : its) {

                Map<String, Object> m = new LinkedHashMap<>(autoBuildAny(ci));

                Product p = productMap.get(ci.getProductId());
                if (p != null) {
                    m.put("product", productBuilder.buildItem(p));
                }

                itemRes.add(m);
            }

            x.put("items", itemRes);

            ContractService.ContractSummary summary =
                    contractService.calcSummary(
                            its,
                            List.of(),
                            List.of(),
                            item.getVatRate(),
                            item.getImportTaxRate()
                    );

            x.put("total_quantity", summary.getTotalQuantity());
            x.put("total_amount", summary.getTotalAmount());

            list.add(x);
        }

        return list;
    }

    private Map<String, Object> buildSupplierWithNation(Supplier supplier, Nation nation) {
        if (supplier == null) return null;

        Map<String, Object> x = new LinkedHashMap<>(supplierBuilder.buildItem(supplier));
        x.put("nation", nation != null ? nationBuilder.buildItem(nation) : null);

        return x;
    }
}