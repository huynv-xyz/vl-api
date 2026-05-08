package com.vlife.api.builder.sale;

import com.vlife.shared.api.builder.ItemBuilder;
import com.vlife.shared.jdbc.dao.CustomerDao;
import com.vlife.shared.jdbc.entity.Customer;
import com.vlife.shared.jdbc.entity.sale.CashBankLedger;
import com.vlife.shared.util.CommonUtil;
import jakarta.inject.Singleton;

import java.util.*;
import java.util.stream.Collectors;

@Singleton
public class CashBankLedgerBuilder extends ItemBuilder<CashBankLedger> {

    private final CustomerDao customerDao;

    public CashBankLedgerBuilder(CustomerDao customerDao) {
        this.customerDao = customerDao;
    }

    // ========================
    // BUILD SINGLE
    // ========================
    @Override
    public Map<String, Object> buildItemFull(CashBankLedger item) {

        if (item == null) return Map.of();

        Map<String, Object> x = new LinkedHashMap<>(autoBuild(item));

        // ===== customer
        if (item.getCustomerId() != null) {
            customerDao.findById(item.getCustomerId())
                    .ifPresent(c -> x.put("customer", c));
        }

        // ===== direction (computed)
        String direction =
                (item.getDebitAmount() != null && item.getDebitAmount().signum() > 0)
                        ? "IN"
                        : "OUT";

        x.put("direction", direction);

        return x;
    }

    // ========================
    // BUILD LIST (NO N+1)
    // ========================
    @Override
    public List<Map<String, Object>> buildList(List<CashBankLedger> items) {

        if (CommonUtil.isNullOrEmpty(items)) {
            return Collections.emptyList();
        }

        // ===== collect customer ids
        Set<Integer> customerIds = items.stream()
                .map(CashBankLedger::getCustomerId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        // ===== batch load
        Map<Integer, Customer> customerMap =
                customerDao.findByIdsAsMap(customerIds);

        // ===== build
        List<Map<String, Object>> list = new ArrayList<>(items.size());

        for (CashBankLedger item : items) {

            Map<String, Object> x = new LinkedHashMap<>(autoBuild(item));

            // customer
            if (item.getCustomerId() != null) {
                x.put("customer", customerMap.get(item.getCustomerId()));
            }

            String direction =
                    (item.getDebitAmount() != null && item.getDebitAmount().signum() > 0)
                            ? "IN"
                            : "OUT";

            x.put("direction", direction);

            list.add(x);
        }

        return list;
    }
}