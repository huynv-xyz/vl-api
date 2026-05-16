package com.vlife.api.service.production;

import com.vlife.shared.jdbc.client.JdbcClient;
import com.vlife.shared.jdbc.dao.ProductDao;
import com.vlife.shared.jdbc.dao.WarehouseDao;
import com.vlife.shared.jdbc.dao.production.ProductBomDao;
import com.vlife.shared.jdbc.dao.production.ProductBomItemDao;
import com.vlife.shared.jdbc.entity.Product;
import com.vlife.shared.jdbc.entity.Warehouse;
import com.vlife.shared.jdbc.entity.production.ProductBom;
import com.vlife.shared.jdbc.entity.production.ProductBomItem;
import io.micronaut.serde.annotation.Serdeable;
import io.micronaut.serde.config.naming.SnakeCaseStrategy;
import jakarta.inject.Singleton;
import jakarta.transaction.Transactional;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

@Singleton
public class VthhBomImportService {

    private static final String DEFAULT_VERSION = "VTHH-IMPORT-20260515";
    private static final LocalDate DEFAULT_VALID_FROM = LocalDate.of(2026, 1, 1);
    private static final LocalDate DEFAULT_VALID_TO = LocalDate.of(2099, 12, 31);

    private final ProductDao productDao;
    private final WarehouseDao warehouseDao;
    private final ProductBomDao productBomDao;
    private final ProductBomItemDao productBomItemDao;
    private final JdbcClient jdbc;

    public VthhBomImportService(
            ProductDao productDao,
            WarehouseDao warehouseDao,
            ProductBomDao productBomDao,
            ProductBomItemDao productBomItemDao,
            JdbcClient jdbc
    ) {
        this.productDao = productDao;
        this.warehouseDao = warehouseDao;
        this.productBomDao = productBomDao;
        this.productBomItemDao = productBomItemDao;
        this.jdbc = jdbc;
    }

    @Transactional
    public ImportResult importExcel(
            InputStream inputStream,
            String version,
            LocalDate validFrom,
            Boolean replace
    ) {
        String bomVersion = normalizeVersion(version);
        LocalDate from = validFrom != null ? validFrom : DEFAULT_VALID_FROM;
        boolean shouldReplace = replace == null || replace;

        ParsedWorkbook parsed = parseWorkbook(inputStream);
        LocalDateTime now = LocalDateTime.now();

        Map<String, Warehouse> warehouseMap = upsertWarehouses(parsed.products().values(), now);
        ProductImportStats productStats = upsertProducts(parsed.products().values(), warehouseMap, now);
        Map<String, Product> productMap = loadProducts(parsed.products().keySet());

        int deletedBoms = 0;
        int deletedBomItems = 0;
        if (shouldReplace) {
            DeleteStats deleteStats = deleteBomVersion(bomVersion);
            deletedBoms = deleteStats.boms();
            deletedBomItems = deleteStats.items();
        }

        Map<String, ProductBom> bomMap = createBoms(parsed.bomItems(), productMap, bomVersion, from, now);
        int insertedBomItems = createBomItems(parsed.bomItems(), bomMap, productMap, warehouseMap, parsed.products(), now);

        return new ImportResult(
                parsed.products().size(),
                productStats.created(),
                productStats.updated(),
                warehouseMap.size(),
                bomMap.size(),
                insertedBomItems,
                deletedBoms,
                deletedBomItems,
                bomVersion,
                from.toString(),
                parsed.skippedRows()
        );
    }

    private ParsedWorkbook parseWorkbook(InputStream inputStream) {
        try (Workbook workbook = WorkbookFactory.create(inputStream)) {
            Sheet sheet = Optional.ofNullable(workbook.getSheet("DS VTHH"))
                    .orElseGet(() -> workbook.getNumberOfSheets() > 0 ? workbook.getSheetAt(0) : null);

            if (sheet == null) {
                throw new IllegalArgumentException("Không tìm thấy sheet dữ liệu");
            }

            DataFormatter formatter = new DataFormatter(Locale.US);
            Map<String, ProductRow> products = new LinkedHashMap<>();
            List<BomLineRow> bomItems = new ArrayList<>();
            List<Integer> skippedRows = new ArrayList<>();

            for (int i = 4; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) {
                    continue;
                }

                String productCode = cell(row, 1, formatter);
                String productName = cell(row, 2, formatter);
                String productUnit = cell(row, 3, formatter);
                String nature = cell(row, 4, formatter);
                String groupCode = cell(row, 5, formatter);
                String groupName = cell(row, 6, formatter);
                String description = cell(row, 7, formatter);
                String warehouseName = cell(row, 8, formatter);
                String accountCode = cell(row, 9, formatter);

                String materialCode = cell(row, 10, formatter);
                String materialName = cell(row, 11, formatter);
                String materialUnit = cell(row, 12, formatter);
                String quantityText = cell(row, 13, formatter);
                String costItem = cell(row, 14, formatter);

                if (productCode != null) {
                    products.put(productCode, new ProductRow(
                            productCode,
                            productName != null ? productName : productCode,
                            productUnit,
                            nature,
                            groupCode,
                            groupName,
                            description,
                            warehouseName,
                            accountCode
                    ));
                }

                if (productCode == null && materialCode == null && quantityText == null) {
                    continue;
                }

                if (materialCode == null) {
                    continue;
                }

                if (productCode == null) {
                    skippedRows.add(i + 1);
                    continue;
                }

                BigDecimal quantity = parseQuantity(quantityText, i + 1);
                bomItems.add(new BomLineRow(
                        i + 1,
                        productCode,
                        materialCode,
                        materialName != null ? materialName : materialCode,
                        materialUnit,
                        quantity,
                        costItem
                ));
            }

            for (BomLineRow line : bomItems) {
                products.putIfAbsent(line.materialCode(), new ProductRow(
                        line.materialCode(),
                        line.materialName(),
                        line.unit(),
                        "Nguyên vật liệu",
                        null,
                        null,
                        null,
                        null,
                        null
                ));
            }

            return new ParsedWorkbook(products, bomItems, skippedRows);
        } catch (Exception e) {
            if (e instanceof IllegalArgumentException illegalArgumentException) {
                throw illegalArgumentException;
            }
            throw new RuntimeException("Không đọc được file Excel VTHH: " + e.getMessage(), e);
        }
    }

    private Map<String, Warehouse> upsertWarehouses(Iterable<ProductRow> products, LocalDateTime now) {
        Map<String, Warehouse> map = new LinkedHashMap<>();

        for (ProductRow product : products) {
            String warehouseName = trim(product.warehouseName());
            if (warehouseName == null || map.containsKey(warehouseName)) {
                continue;
            }

            Warehouse warehouse = warehouseDao.findByName(warehouseName).orElse(null);
            if (warehouse == null) {
                warehouse = new Warehouse();
                warehouse.setName(warehouseName);
                warehouse.setStatus("ACTIVE");
                warehouse.setCreatedAt(now);
                warehouse.setUpdatedAt(now);
                warehouse = warehouseDao.insert(warehouse);
            }

            map.put(warehouseName, warehouse);
        }

        return map;
    }

    private ProductImportStats upsertProducts(
            Iterable<ProductRow> rows,
            Map<String, Warehouse> warehouseMap,
            LocalDateTime now
    ) {
        int created = 0;
        int updated = 0;

        for (ProductRow row : rows) {
            Product old = productDao.findByCode(row.code()).orElse(null);

            if (old == null) {
                Product product = new Product();
                product.setCode(row.code());
                applyProductInfo(product, row, warehouseMap);
                product.setStatus(1);
                product.setCreatedAt(now);
                product.setUpdatedAt(now);

                productDao.insert(product);
                created++;
            } else {
                Product update = new Product();
                applyProductInfo(update, row, warehouseMap);
                update.setStatus(1);
                update.setUpdatedAt(now);

                productDao.updateSelective(old.getId(), update);
                updated++;
            }
        }

        return new ProductImportStats(created, updated);
    }

    private void applyProductInfo(Product product, ProductRow row, Map<String, Warehouse> warehouseMap) {
        product.setName(row.name());
        product.setUnit(row.unit());
        product.setNature(row.nature());
        product.setGroupCode(row.groupCode());
        product.setGroupName(row.groupName());
        product.setDescription(row.description());
        product.setInventoryAccountCode(row.accountCode());
        product.setDefaultWarehouseId(resolveWarehouseId(row, warehouseMap));
    }

    private Map<String, Product> loadProducts(Set<String> codes) {
        Map<String, Product> map = new LinkedHashMap<>();

        for (String code : codes) {
            productDao.findByCode(code).ifPresent(product -> map.put(code, product));
        }

        return map;
    }

    private DeleteStats deleteBomVersion(String version) {
        Integer itemCount = (int) jdbc.queryLong("""
                SELECT COUNT(*)
                FROM product_bom_items pbi
                JOIN product_boms pb ON pb.id = pbi.bom_id
                WHERE pb.version = :version
                """, Map.of("version", version));

        Integer bomCount = (int) jdbc.queryLong("""
                SELECT COUNT(*)
                FROM product_boms
                WHERE version = :version
                """, Map.of("version", version));

        jdbc.update("""
                DELETE pbi
                FROM product_bom_items pbi
                JOIN product_boms pb ON pb.id = pbi.bom_id
                WHERE pb.version = :version
                """, Map.of("version", version));

        jdbc.update("""
                DELETE FROM product_boms
                WHERE version = :version
                """, Map.of("version", version));

        return new DeleteStats(bomCount, itemCount);
    }

    private Map<String, ProductBom> createBoms(
            List<BomLineRow> bomItems,
            Map<String, Product> productMap,
            String version,
            LocalDate validFrom,
            LocalDateTime now
    ) {
        Map<String, ProductBom> map = new LinkedHashMap<>();

        for (String parentCode : bomItems.stream().map(BomLineRow::parentCode).distinct().toList()) {
            Product product = productMap.get(parentCode);
            if (product == null) {
                throw new IllegalArgumentException("Không tìm thấy thành phẩm: " + parentCode);
            }

            ProductBom bom = new ProductBom();
            bom.setProductId(product.getId());
            bom.setVersion(version);
            bom.setValidFrom(validFrom);
            bom.setValidTo(DEFAULT_VALID_TO);
            bom.setActive(true);
            bom.setStatus("ACTIVE");
            bom.setNote("Import từ file Định mức VTHH.xlsx");
            bom.setCreatedAt(now);
            bom.setUpdatedAt(now);

            map.put(parentCode, productBomDao.insert(bom));
        }

        return map;
    }

    private int createBomItems(
            List<BomLineRow> rows,
            Map<String, ProductBom> bomMap,
            Map<String, Product> productMap,
            Map<String, Warehouse> warehouseMap,
            Map<String, ProductRow> productRows,
            LocalDateTime now
    ) {
        Map<String, Integer> lineMap = new LinkedHashMap<>();
        List<ProductBomItem> items = new ArrayList<>();

        for (BomLineRow row : rows) {
            ProductBom bom = bomMap.get(row.parentCode());
            Product material = productMap.get(row.materialCode());

            if (bom == null) {
                throw new IllegalArgumentException("Không tìm thấy BOM của thành phẩm: " + row.parentCode());
            }
            if (material == null) {
                throw new IllegalArgumentException("Không tìm thấy vật tư: " + row.materialCode());
            }

            int lineNo = lineMap.merge(row.parentCode(), 1, Integer::sum);
            ProductRow materialInfo = productRows.get(row.materialCode());

            ProductBomItem item = new ProductBomItem();
            item.setBomId(bom.getId());
            item.setMaterialProductId(material.getId());
            item.setMaterialType(resolveMaterialType(row, materialInfo));
            item.setQuantity(row.quantity());
            item.setUnit(row.unit());
            item.setLineNo(lineNo);
            item.setDefaultWarehouseId(resolveWarehouseId(materialInfo, warehouseMap));
            item.setAccountCode(materialInfo != null ? materialInfo.accountCode() : null);
            item.setNote("Import VTHH dòng Excel " + row.sourceRow());
            item.setCreatedAt(now);
            item.setUpdatedAt(now);

            items.add(item);
        }

        productBomItemDao.saveAll(items);
        return items.size();
    }

    private Integer resolveWarehouseId(ProductRow row, Map<String, Warehouse> warehouseMap) {
        if (row == null || trim(row.warehouseName()) == null) {
            return null;
        }

        Warehouse warehouse = warehouseMap.get(trim(row.warehouseName()));
        return warehouse != null ? warehouse.getId() : null;
    }

    private String resolveMaterialType(BomLineRow row, ProductRow materialInfo) {
        String nature = lower(materialInfo != null ? materialInfo.nature() : null);
        String code = upper(row.materialCode());
        String name = lower(row.materialName());

        if (nature.contains("bao bì")
                || code.startsWith("P.")
                || code.startsWith("3.")
                || name.contains("túi")
                || name.contains("thùng")
                || name.contains("màng")
                || name.contains("nhãn")
                || name.contains("chai")
                || name.contains("can")
                || name.contains("hộp")
                || name.contains("bao bì")
                || name.contains("vách ngăn")
                || name.contains("nắp")
                || name.contains("tem")
                || name.contains("băng keo")) {
            return "BB";
        }

        return "NVL";
    }

    private String normalizeVersion(String version) {
        String x = trim(version);
        return x != null ? x : DEFAULT_VERSION;
    }

    private String cell(Row row, int index, DataFormatter formatter) {
        Cell cell = row.getCell(index);
        if (cell == null) {
            return null;
        }

        return trim(formatter.formatCellValue(cell));
    }

    private BigDecimal parseQuantity(String value, int rowNumber) {
        String x = trim(value);
        if (x == null) {
            throw new IllegalArgumentException("Dòng " + rowNumber + ": thiếu số lượng định mức");
        }

        try {
            return new BigDecimal(x.replace(",", ""));
        } catch (Exception e) {
            throw new IllegalArgumentException("Dòng " + rowNumber + ": số lượng định mức không hợp lệ: " + value);
        }
    }

    private String trim(String value) {
        if (value == null) {
            return null;
        }

        String x = value.trim();
        return x.isEmpty() ? null : x;
    }

    private String lower(String value) {
        return Objects.requireNonNullElse(trim(value), "").toLowerCase(Locale.ROOT);
    }

    private String upper(String value) {
        return Objects.requireNonNullElse(trim(value), "").toUpperCase(Locale.ROOT);
    }

    private record ParsedWorkbook(
            Map<String, ProductRow> products,
            List<BomLineRow> bomItems,
            List<Integer> skippedRows
    ) {
    }

    private record ProductRow(
            String code,
            String name,
            String unit,
            String nature,
            String groupCode,
            String groupName,
            String description,
            String warehouseName,
            String accountCode
    ) {
    }

    private record BomLineRow(
            Integer sourceRow,
            String parentCode,
            String materialCode,
            String materialName,
            String unit,
            BigDecimal quantity,
            String costItem
    ) {
    }

    private record ProductImportStats(Integer created, Integer updated) {
    }

    private record DeleteStats(Integer boms, Integer items) {
    }

    @Serdeable(naming = SnakeCaseStrategy.class)
    public record ImportResult(
            Integer totalProducts,
            Integer createdProducts,
            Integer updatedProducts,
            Integer touchedWarehouses,
            Integer importedBoms,
            Integer importedBomItems,
            Integer deletedOldBoms,
            Integer deletedOldBomItems,
            String version,
            String validFrom,
            List<Integer> skippedRows
    ) {
    }
}
