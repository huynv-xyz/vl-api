package com.vlife.api.builder.purchasing;

import com.vlife.api.builder.ProductBuilder;
import com.vlife.api.util.ApiUtil;
import com.vlife.shared.api.builder.ItemBuilder;
import com.vlife.shared.jdbc.dao.ProductDao;
import com.vlife.shared.jdbc.dao.WarehouseDao;
import com.vlife.shared.jdbc.dao.purchasing.ContractDao;
import com.vlife.shared.jdbc.dao.purchasing.ContractItemDao;
import com.vlife.shared.jdbc.dao.purchasing.PortDao;
import com.vlife.shared.jdbc.dao.purchasing.ShipmentItemDao;
import com.vlife.shared.jdbc.entity.Product;
import com.vlife.shared.jdbc.entity.Warehouse;
import com.vlife.shared.jdbc.entity.purchasing.*;
import com.vlife.shared.service.purchasing.ShipmentService;
import com.vlife.shared.util.CommonUtil;
import jakarta.inject.Singleton;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Singleton
public class ShipmentBuilder extends ItemBuilder<Shipment> {

    private final ShipmentItemDao shipmentItemDao;
    private final ProductDao productDao;
    private final ProductBuilder productBuilder;

    private final ContractDao contractDao;
    private final ContractItemDao contractItemDao;

    private final ShipmentService shipmentService;

    private final PortDao portDao;
    private final PortBuilder portBuilder;

    private final WarehouseDao warehouseDao;

    public ShipmentBuilder(
            ShipmentItemDao shipmentItemDao,
            ProductDao productDao,
            ProductBuilder productBuilder,
            ContractDao contractDao,
            ContractItemDao contractItemDao,
            ShipmentService shipmentService,
            PortDao portDao,
            PortBuilder portBuilder,
            WarehouseDao warehouseDao
    ) {
        this.shipmentItemDao = shipmentItemDao;
        this.productDao = productDao;
        this.productBuilder = productBuilder;
        this.contractDao = contractDao;
        this.contractItemDao = contractItemDao;
        this.shipmentService = shipmentService;
        this.portDao = portDao;
        this.portBuilder = portBuilder;
        this.warehouseDao = warehouseDao;
    }

    @Override
    public List<Map<String, Object>> buildList(List<Shipment> shipments) {

        if (CommonUtil.isNullOrEmpty(shipments)) {
            return Collections.emptyList();
        }

        Set<Integer> shipmentIds = shipments.stream()
                .map(Shipment::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<Integer, List<ShipmentItem>> itemMap =
                shipmentIds.isEmpty()
                        ? Collections.emptyMap()
                        : shipmentItemDao.findByShipmentIds(shipmentIds)
                        .stream()
                        .collect(Collectors.groupingBy(ShipmentItem::getShipmentId));

        Set<Integer> productIds = itemMap.values().stream()
                .flatMap(List::stream)
                .map(ShipmentItem::getProductId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<Integer, Product> productMap =
                productIds.isEmpty()
                        ? Collections.emptyMap()
                        : productDao.findByIdsAsMap(productIds);

        Set<Integer> contractIds = shipments.stream()
                .map(Shipment::getContractId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<Integer, Contract> contractMap =
                contractIds.isEmpty()
                        ? Collections.emptyMap()
                        : contractDao.findByIds(contractIds).stream()
                        .collect(Collectors.toMap(Contract::getId, c -> c));

        Map<Integer, Map<Integer, ContractItem>> contractItemMapByContract =
                contractItemDao.findMapByContractIds(contractIds);

        Set<Integer> portIds = shipments.stream()
                .map(Shipment::getDestinationPortId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<Integer, Port> portMap =
                portIds.isEmpty()
                        ? Collections.emptyMap()
                        : portDao.findByIdsAsMap(portIds);

        Set<Integer> warehouseIds = shipments.stream()
                .map(Shipment::getWarehouseId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<Integer, Warehouse> warehouseMap =
                warehouseIds.isEmpty()
                        ? Collections.emptyMap()
                        : warehouseDao.findByIdsAsMap(warehouseIds);

        List<Map<String, Object>> result = new ArrayList<>(shipments.size());

        for (Shipment s : shipments) {

            Map<String, Object> x = new LinkedHashMap<>(autoBuild(s));

            Port port = portMap.get(s.getDestinationPortId());
            x.put("destination_port", port != null ? portBuilder.buildItem(port) : null);

            x.put("warehouse", warehouseMap.get(s.getWarehouseId()));

            List<ShipmentItem> its =
                    itemMap.getOrDefault(s.getId(), Collections.emptyList());

            Contract contract = contractMap.get(s.getContractId());

            Map<Integer, ContractItem> contractItemMap =
                    contractItemMapByContract.getOrDefault(
                            s.getContractId(),
                            Collections.emptyMap()
                    );

            BigDecimal vatRate = contract != null ? ApiUtil.nvl(contract.getVatRate()) : BigDecimal.ZERO;
            BigDecimal importRate = contract != null ? ApiUtil.nvl(contract.getImportTaxRate()) : BigDecimal.ZERO;

            BigDecimal total = BigDecimal.ZERO;
            List<Map<String, Object>> itemRes = new ArrayList<>();

            for (ShipmentItem i : its) {

                Map<String, Object> m = new LinkedHashMap<>(autoBuildAny(i));

                Product p = productMap.get(i.getProductId());
                if (p != null) {
                    m.put("product", productBuilder.buildItem(p));
                }

                ContractItem ci = contractItemMap.get(i.getProductId());

                if (ci != null) {
                    var price = shipmentService.calcPrice(i, ci, vatRate, importRate);

                    m.put("discount_amount", price.discount());
                    m.put("import_tax", price.importTax());
                    m.put("vat_amount", price.vat());
                    m.put("final_price", price.finalPrice());
                    m.put("total_price", price.total());

                    total = total.add(price.total());
                } else {
                    m.put("total_price", BigDecimal.ZERO);
                }

                itemRes.add(m);
            }

            x.put("items", itemRes);
            x.put("total_amount", total);

            result.add(x);
        }

        return result;
    }

    @Override
    public Map<String, Object> buildItemFull(Shipment s) {
        if (s == null) return null;
        return buildList(Collections.singletonList(s)).get(0);
    }
}