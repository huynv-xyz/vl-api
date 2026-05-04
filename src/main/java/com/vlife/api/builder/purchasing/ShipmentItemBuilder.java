package com.vlife.api.builder.purchasing;

import com.vlife.api.builder.ProductBuilder;
import com.vlife.api.util.ApiUtil;
import com.vlife.shared.api.builder.ItemBuilder;
import com.vlife.shared.jdbc.dao.ProductDao;
import com.vlife.shared.jdbc.dao.WarehouseDao;
import com.vlife.shared.jdbc.dao.purchasing.ContractDao;
import com.vlife.shared.jdbc.dao.purchasing.PortDao;
import com.vlife.shared.jdbc.dao.purchasing.ShipmentDao;
import com.vlife.shared.jdbc.entity.Product;
import com.vlife.shared.jdbc.entity.Warehouse;
import com.vlife.shared.jdbc.entity.purchasing.Contract;
import com.vlife.shared.jdbc.entity.purchasing.Port;
import com.vlife.shared.jdbc.entity.purchasing.Shipment;
import com.vlife.shared.jdbc.entity.purchasing.ShipmentItem;
import com.vlife.shared.util.CommonUtil;
import jakarta.inject.Singleton;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

@Singleton
public class ShipmentItemBuilder extends ItemBuilder<ShipmentItem> {

    private final ProductDao productDao;
    private final ProductBuilder productBuilder;
    private final ShipmentDao shipmentDao;
    private final ContractDao contractDao;
    private final PortDao portDao;
    private final PortBuilder portBuilder;
    private final ShipmentBuilder shipmentBuilder;
    private final WarehouseDao warehouseDao;

    public ShipmentItemBuilder(
            ProductDao productDao,
            ProductBuilder productBuilder,
            ShipmentDao shipmentDao,
            ContractDao contractDao,
            PortDao portDao,
            PortBuilder portBuilder,
            ShipmentBuilder shipmentBuilder,
            WarehouseDao warehouseDao
    ) {
        this.productDao = productDao;
        this.productBuilder = productBuilder;
        this.shipmentDao = shipmentDao;
        this.contractDao = contractDao;
        this.portDao = portDao;
        this.portBuilder = portBuilder;
        this.shipmentBuilder = shipmentBuilder;
        this.warehouseDao = warehouseDao;
    }

    @Override
    public List<Map<String, Object>> buildList(List<ShipmentItem> items) {

        if (CommonUtil.isNullOrEmpty(items)) {
            return Collections.emptyList();
        }

        Set<Integer> productIds = items.stream()
                .map(ShipmentItem::getProductId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<Integer, Product> productMap =
                productIds.isEmpty()
                        ? Collections.emptyMap()
                        : productDao.findByIdsAsMap(productIds);

        Set<Integer> shipmentIds = items.stream()
                .map(ShipmentItem::getShipmentId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<Integer, Shipment> shipmentMap =
                shipmentIds.isEmpty()
                        ? Collections.emptyMap()
                        : shipmentDao.findByIds(shipmentIds)
                        .stream()
                        .collect(Collectors.toMap(Shipment::getId, s -> s));

        Set<Integer> contractIds = shipmentMap.values().stream()
                .map(Shipment::getContractId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<Integer, Contract> contractMap =
                contractIds.isEmpty()
                        ? Collections.emptyMap()
                        : contractDao.findByIds(contractIds)
                        .stream()
                        .collect(Collectors.toMap(Contract::getId, c -> c));
        Set<Integer> portIds = shipmentMap.values().stream()
                .map(Shipment::getDestinationPortId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<Integer, Port> portMap =
                portIds.isEmpty()
                        ? Collections.emptyMap()
                        : portDao.findByIdsAsMap(portIds);

        Set<Integer> warehouseIds = shipmentMap.values().stream()
                .map(Shipment::getWarehouseId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<Integer, Warehouse> warehouseMap =
                warehouseIds.isEmpty()
                        ? Collections.emptyMap()
                        : warehouseDao.findByIdsAsMap(warehouseIds);

        List<Map<String, Object>> result = new ArrayList<>();

        for (ShipmentItem i : items) {

            Map<String, Object> x = new LinkedHashMap<>(autoBuild(i));

            Product p = productMap.get(i.getProductId());
            if (p != null) {
                x.put("product", productBuilder.buildItem(p));
            }

            Shipment s = shipmentMap.get(i.getShipmentId());
            if (s != null) {
                Map<String, Object> shipmentBuild = shipmentBuilder.buildItem(s);
                Port port = portMap.get(s.getDestinationPortId());

                if(port != null) {
                    shipmentBuild.put("destination_port" , portBuilder.buildItem(port));
                }

                Warehouse warehouse = warehouseMap.get(s.getWarehouseId());

                shipmentBuild.put("warehouse", warehouse);

                x.put("shipment" , shipmentBuild);
            }

            Contract c = s != null ? contractMap.get(s.getContractId()) : null;

            BigDecimal vatRate = c != null ? ApiUtil.nvl(c.getVatRate()) : BigDecimal.ZERO;
            BigDecimal importRate = c != null ? ApiUtil.nvl(c.getImportTaxRate()) : BigDecimal.ZERO;

            enrichCalculated(x, i, vatRate, importRate);

            result.add(x);
        }

        return result;
    }

    @Override
    public Map<String, Object> buildItemFull(ShipmentItem item) {
        if (item == null) return null;
        return buildList(Collections.singletonList(item)).get(0);
    }

    // ========================
    // CALC
    // ========================
    private void enrichCalculated(
            Map<String, Object> x,
            ShipmentItem i,
            BigDecimal vatRate,
            BigDecimal importRate
    ) {

        BigDecimal q = ApiUtil.nvl(i.getQuantity());
        BigDecimal d = ApiUtil.nvl(i.getDefectQuantity());

        BigDecimal realQty = q.subtract(d);
        if (realQty.compareTo(BigDecimal.ZERO) < 0) {
            realQty = BigDecimal.ZERO;
        }

        BigDecimal base = ApiUtil.nvl(i.getUnitPrice());

        BigDecimal priceBeforeTax = base;

        BigDecimal importTax = percentOf(priceBeforeTax, importRate);

        BigDecimal vatBase = priceBeforeTax.add(importTax);
        BigDecimal vat = percentOf(vatBase, vatRate);

        BigDecimal finalPrice = priceBeforeTax
                .add(importTax)
                .add(vat);

        BigDecimal total = realQty.multiply(finalPrice);

        x.put("real_quantity", realQty);
        x.put("import_tax_amount", importTax);
        x.put("vat_amount", vat);
        x.put("final_price", finalPrice);
        x.put("total_price", total);
    }

    private BigDecimal percentOf(BigDecimal amount, BigDecimal rate) {
        if (amount == null || rate == null) return BigDecimal.ZERO;
        if (BigDecimal.ZERO.compareTo(rate) == 0) return BigDecimal.ZERO;

        return amount
                .multiply(rate)
                .divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP);
    }
}