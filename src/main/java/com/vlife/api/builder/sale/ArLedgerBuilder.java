package com.vlife.api.builder.sale;

import com.vlife.shared.api.builder.ItemBuilder;
import com.vlife.shared.jdbc.dao.CustomerDao;
import com.vlife.shared.jdbc.dao.sale.ArLedgerDao;
import com.vlife.shared.jdbc.entity.Customer;
import com.vlife.shared.jdbc.entity.sale.ArLedger;
import com.vlife.shared.util.CommonUtil;
import jakarta.inject.Singleton;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Singleton
public class ArLedgerBuilder extends ItemBuilder<ArLedger> {

    private final CustomerDao customerDao;
    private final ArLedgerDao arLedgerDao;

    public ArLedgerBuilder(
            CustomerDao customerDao,
            ArLedgerDao arLedgerDao
    ) {
        this.customerDao = customerDao;
        this.arLedgerDao = arLedgerDao;
    }

    // ========================
    // DETAIL (NO N+1)
    // ========================
    @Override
    public Map<String, Object> buildItemFull(ArLedger item) {

        if (item == null) return Map.of();

        Map<String, Object> x = new LinkedHashMap<>(autoBuild(item));

        // ===== customer
        Customer customer = item.getCustomerId() != null
                ? customerDao.findById(item.getCustomerId()).orElse(null)
                : null;

        x.put("customer", customer);

        // ===== type (DEBIT / CREDIT)
        x.put("type", resolveType(item));

        // ===== balance (optional)
        BigDecimal balance = arLedgerDao.getBalance(item.getCustomerId());
        x.put("balance", balance);

        return x;
    }

    // ========================
    // LIST (BATCH)
    // ========================
    @Override
    public List<Map<String, Object>> buildList(List<ArLedger> items) {

        if (CommonUtil.isNullOrEmpty(items)) {
            return Collections.emptyList();
        }

        // ===== collect ids
        Set<Integer> customerIds = items.stream()
                .map(ArLedger::getCustomerId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        // ===== batch load
        Map<Integer, Customer> customerMap =
                customerDao.findByIdsAsMap(customerIds);

        List<Map<String, Object>> list = new ArrayList<>(items.size());

        for (ArLedger item : items) {

            Map<String, Object> x = new LinkedHashMap<>(autoBuild(item));
            x.put("posting_date", item.getPostingDate());

            x.put("customer", customerMap.get(item.getCustomerId()));

            // ===== type
            x.put("type", resolveType(item));

            // ===== simple label
            x.put("display_type", mapDisplayType(item.getSourceType()));

            list.add(x);
        }

        return list;
    }

    // ========================
    // HELPERS
    // ========================
    private String resolveType(ArLedger x) {
        if (x.getDebitAmount() != null &&
                x.getDebitAmount().compareTo(BigDecimal.ZERO) > 0) {
            return "DEBIT";
        }
        if (x.getCreditAmount() != null &&
                x.getCreditAmount().compareTo(BigDecimal.ZERO) > 0) {
            return "CREDIT";
        }
        return "UNKNOWN";
    }

    private String mapDisplayType(String sourceType) {
        if (sourceType == null) return "-";

        return switch (sourceType) {
            case "EXPORT" -> "Bán hàng";
            case "RECEIPT" -> "Thu tiền";
            case "ADJUST" -> "Điều chỉnh";
            case "IMPORT" -> "Import";
            default -> sourceType;
        };
    }
}