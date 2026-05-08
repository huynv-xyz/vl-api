package com.vlife.api.builder.sale;

import com.vlife.shared.api.builder.ItemBuilder;
import com.vlife.shared.jdbc.dao.CustomerDao;
import com.vlife.shared.jdbc.dao.sale.ReceiptDao;
import com.vlife.shared.jdbc.entity.Customer;
import com.vlife.shared.jdbc.entity.sale.BankTransaction;
import com.vlife.shared.util.CommonUtil;
import jakarta.inject.Singleton;

import java.util.*;
import java.util.stream.Collectors;

@Singleton
public class BankTransactionBuilder extends ItemBuilder<BankTransaction> {

    private final CustomerDao customerDao;
    private final ReceiptDao receiptDao;

    public BankTransactionBuilder(
            CustomerDao customerDao,
            ReceiptDao receiptDao
    ) {
        this.customerDao = customerDao;
        this.receiptDao = receiptDao;
    }

    // ========================
    // BUILD SINGLE
    // ========================
    @Override
    public Map<String, Object> buildItemFull(BankTransaction item) {

        if (item == null) return Map.of();

        Map<String, Object> x = new LinkedHashMap<>(autoBuild(item));

        Customer c = item.getCustomerId() != null
                ? customerDao.findById(item.getCustomerId()).orElse(null)
                : null;

        x.put("customer", c);

        // ===== status (computed)
        boolean used = receiptDao.existsByBankTxnId(item.getId());
        x.put("status", used ? "DONE" : "NEW");

        return x;
    }

    // ========================
    // BUILD LIST (IMPORTANT)
    // ========================
    @Override
    public List<Map<String, Object>> buildList(List<BankTransaction> items) {

        if (CommonUtil.isNullOrEmpty(items)) {
            return Collections.emptyList();
        }

        // ===== collect ids
        Set<Integer> customerIds = items.stream()
                .map(BankTransaction::getCustomerId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Set<Integer> txnIds = items.stream()
                .map(BankTransaction::getId)
                .collect(Collectors.toSet());

        // ===== batch query (NO N+1)
        Map<Integer, Customer> customerMap =
                customerDao.findByIdsAsMap(customerIds);

        Set<Integer> usedTxnIds =
                receiptDao.findUsedBankTxnIds(txnIds);

        // ===== build
        List<Map<String, Object>> list = new ArrayList<>(items.size());

        for (BankTransaction item : items) {

            Map<String, Object> x = new LinkedHashMap<>(autoBuild(item));

            x.put("customer", customerMap.get(item.getCustomerId()));

            x.put("status",
                    usedTxnIds.contains(item.getId()) ? "DONE" : "NEW"
            );

            list.add(x);
        }

        return list;
    }
}