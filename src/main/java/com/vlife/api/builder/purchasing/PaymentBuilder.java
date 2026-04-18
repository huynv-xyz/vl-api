package com.vlife.api.builder.purchasing;

import com.vlife.shared.api.builder.ItemBuilder;
import com.vlife.shared.jdbc.dao.purchasing.ContractDao;
import com.vlife.shared.jdbc.dao.purchasing.ShipmentDao;
import com.vlife.shared.jdbc.entity.purchasing.Contract;
import com.vlife.shared.jdbc.entity.purchasing.Payment;
import com.vlife.shared.jdbc.entity.purchasing.Shipment;
import com.vlife.shared.util.CommonUtil;
import jakarta.inject.Singleton;

import java.util.*;
import java.util.stream.Collectors;

@Singleton
public class PaymentBuilder extends ItemBuilder<Payment> {

    private final ContractDao contractDao;
    private final ShipmentDao shipmentDao;
    private final ContractBuilder contractBuilder;
    private final ShipmentBuilder shipmentBuilder;

    public PaymentBuilder(ContractDao contractDao,
                          ShipmentDao shipmentDao,
                          ContractBuilder contractBuilder,
                          ShipmentBuilder shipmentBuilder) {
        this.contractDao = contractDao;
        this.shipmentDao = shipmentDao;
        this.contractBuilder = contractBuilder;
        this.shipmentBuilder = shipmentBuilder;
    }

    @Override
    public Map<String, Object> buildItemFull(Payment entity) {
        if (entity == null) return null;

        Map<String, Object> x = new LinkedHashMap<>(autoBuild(entity));

        Contract contract = null;
        Shipment shipment = null;

        if (entity.getContractId() != null) {
            contract = contractDao.findById(entity.getContractId()).orElse(null);
        }

        if (entity.getShipmentId() != null) {
            shipment = shipmentDao.findById(entity.getShipmentId()).orElse(null);
        }

        x.put("contract", contractBuilder.buildItem(contract));
        x.put("shipment", shipmentBuilder.buildItem(shipment));

        return x;
    }

    @Override
    public List<Map<String, Object>> buildList(List<Payment> items) {
        if (CommonUtil.isNullOrEmpty(items)) return Collections.emptyList();

        Set<Integer> contractIds = items.stream()
                .map(Payment::getContractId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Set<Integer> shipmentIds = items.stream()
                .map(Payment::getShipmentId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<Integer, Contract> contractMap = new HashMap<>();
        Map<Integer, Shipment> shipmentMap = new HashMap<>();

        for (Integer id : contractIds) {
            contractDao.findById(id).ifPresent(x -> contractMap.put(id, x));
        }

        for (Integer id : shipmentIds) {
            shipmentDao.findById(id).ifPresent(x -> shipmentMap.put(id, x));
        }

        List<Map<String, Object>> list = new ArrayList<>(items.size());

        for (Payment p : items) {
            Map<String, Object> x = new LinkedHashMap<>(autoBuild(p));

            Contract contract = contractMap.get(p.getContractId());
            Shipment shipment = shipmentMap.get(p.getShipmentId());

            x.put("contract", contractBuilder.buildItem(contract));
            x.put("shipment", shipmentBuilder.buildItem(shipment));

            list.add(x);
        }

        return list;
    }
}