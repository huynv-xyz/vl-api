package com.vlife.api.controller;

import com.vlife.api.builder.ProductBuilder;
import com.vlife.api.controller.base.BaseCrudController;
import com.vlife.shared.api.dto.ApiResponse;
import com.vlife.shared.jdbc.dao.ProductDao;
import com.vlife.shared.jdbc.entity.Product;
import com.vlife.shared.service.ProductImportService;
import io.micronaut.data.model.Page;
import io.micronaut.data.model.Pageable;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.*;
import io.micronaut.http.multipart.CompletedFileUpload;
import io.micronaut.serde.annotation.Serdeable;
import io.micronaut.serde.config.naming.SnakeCaseStrategy;
import jakarta.inject.Inject;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.Map;

import static com.vlife.shared.util.NumberUtil.parseInt;
import static com.vlife.shared.util.StringUtil.isBlank;
import static com.vlife.shared.util.StringUtil.trim;

@Controller("/products")
public class ProductController extends BaseCrudController<Product, Integer, ProductDao> {

    private final ProductImportService productImportService;

    @Inject
    public ProductController(ProductDao dao,
                             ProductBuilder builder,
                             ProductImportService productImportService) {
        super(dao, builder);
        this.productImportService = productImportService;
    }

    @Override
    protected Page<Product> doSearch(Map<String, String> filters, Pageable pageable) {
        return dao.search(
                trim(filters.get("keyword")),
                parseInt(filters.get("status")),
                trim(filters.get("nature")),
                trim(filters.get("group_code")),
                parseInt(filters.get("default_warehouse_id")),
                trim(filters.get("inventory_account_code")),
                pageable
        );
    }

    @Post
    public HttpResponse<?> create(@Body ProductCreateRequest req) {
        return handleCreate(req, r -> {
            Product x = new Product();
            x.setCode(trim(r.getCode()));
            x.setName(trim(r.getName()));
            x.setUnit(trim(r.getUnit()));
            x.setNature(trim(r.getNature()));
            x.setGroupCode(trim(r.getGroupCode()));
            x.setGroupName(trim(r.getGroupName()));
            x.setDescription(trim(r.getDescription()));
            x.setDefaultWarehouseId(r.getDefaultWarehouseId());
            x.setInventoryAccountCode(trim(r.getInventoryAccountCode()));
            x.setStatus(r.getStatus() != null ? r.getStatus() : 1);
            x.setCreatedAt(LocalDateTime.now());
            x.setUpdatedAt(LocalDateTime.now());
            return x;
        });
    }

    @Put("/{id}")
    public HttpResponse<?> update(@PathVariable Integer id, @Body ProductUpdateRequest req) {
        return handleUpdate(id, req, r -> {
            Product x = new Product();
            x.setCode(trim(r.getCode()));
            x.setName(trim(r.getName()));
            x.setUnit(trim(r.getUnit()));
            x.setNature(trim(r.getNature()));
            x.setGroupCode(trim(r.getGroupCode()));
            x.setGroupName(trim(r.getGroupName()));
            x.setDescription(trim(r.getDescription()));
            x.setDefaultWarehouseId(r.getDefaultWarehouseId());
            x.setInventoryAccountCode(trim(r.getInventoryAccountCode()));
            x.setStatus(r.getStatus());
            x.setUpdatedAt(LocalDateTime.now());
            mergeNullFromDb(id, x, "id", "createdAt");
            return x;
        });
    }

    @Post(value = "/import-csv", consumes = MediaType.MULTIPART_FORM_DATA, produces = MediaType.APPLICATION_JSON)
    public HttpResponse<?> importCsv(@Part("file") CompletedFileUpload file) {
        if (file == null || file.getFilename() == null || file.getFilename().isBlank()) {
            return HttpResponse.ok(ApiResponse.error(-400, "File upload không hợp lệ"));
        }

        String originalFilename = file.getFilename().trim();
        String safeFilename = Paths.get(originalFilename).getFileName().toString();

        if (!safeFilename.toLowerCase().endsWith(".csv")) {
            return HttpResponse.ok(ApiResponse.error(-400, "Chỉ hỗ trợ file CSV"));
        }

        try {
            Path dir = Paths.get("files");
            Files.createDirectories(dir);

            Path target = dir.resolve(safeFilename);

            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, target, StandardCopyOption.REPLACE_EXISTING);
            }

            int affected = productImportService.importCsv(target);

            return HttpResponse.ok(ApiResponse.success(Map.of(
                    "message", "Import CSV thành công",
                    "file_name", safeFilename,
                    "file_path", target.toString(),
                    "affected", affected
            )));
        } catch (IOException e) {
            return HttpResponse.ok(ApiResponse.error(-500, "Không thể lưu file CSV: " + e.getMessage()));
        } catch (Exception e) {
            return HttpResponse.ok(ApiResponse.error(-500, "Import CSV thất bại: " + e.getMessage()));
        }
    }

    @Override
    protected <REQ> ApiResponse<?> validateBeforeCreate(Product entity, REQ req) {
        if (isBlank(entity.getCode())) return ApiResponse.error(-400, "code is required");
        if (isBlank(entity.getName())) return ApiResponse.error(-400, "name is required");
        if (dao.findByCode(entity.getCode()).isPresent()) return ApiResponse.error(-400, "code already exists");
        return null;
    }

    @Override
    protected <REQ> ApiResponse<?> validateBeforeUpdate(Integer id, Product entity, REQ req) {
        if (isBlank(entity.getCode())) return ApiResponse.error(-400, "code is required");
        if (isBlank(entity.getName())) return ApiResponse.error(-400, "name is required");

        var oldOpt = dao.findById(id);
        if (oldOpt.isEmpty()) return ApiResponse.error(-404, "not found");

        var old = oldOpt.get();
        if (!entity.getCode().equals(old.getCode()) && dao.findByCode(entity.getCode()).isPresent()) {
            return ApiResponse.error(-400, "code already exists");
        }
        return null;
    }

    @Serdeable(naming = SnakeCaseStrategy.class)
    public static class ProductCreateRequest {
        private String code;
        private String name;
        private String unit;
        private String nature;
        private String groupCode;
        private String groupName;
        private String description;
        private Integer defaultWarehouseId;
        private String inventoryAccountCode;
        private Integer status;

        public String getCode() {
            return code;
        }

        public void setCode(String code) {
            this.code = code;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getUnit() {
            return unit;
        }

        public void setUnit(String unit) {
            this.unit = unit;
        }

        public String getNature() {
            return nature;
        }

        public void setNature(String nature) {
            this.nature = nature;
        }

        public String getGroupCode() {
            return groupCode;
        }

        public void setGroupCode(String groupCode) {
            this.groupCode = groupCode;
        }

        public String getGroupName() {
            return groupName;
        }

        public void setGroupName(String groupName) {
            this.groupName = groupName;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public Integer getDefaultWarehouseId() {
            return defaultWarehouseId;
        }

        public void setDefaultWarehouseId(Integer defaultWarehouseId) {
            this.defaultWarehouseId = defaultWarehouseId;
        }

        public String getInventoryAccountCode() {
            return inventoryAccountCode;
        }

        public void setInventoryAccountCode(String inventoryAccountCode) {
            this.inventoryAccountCode = inventoryAccountCode;
        }

        public Integer getStatus() {
            return status;
        }

        public void setStatus(Integer status) {
            this.status = status;
        }
    }

    @Serdeable
    public static class ProductUpdateRequest extends ProductCreateRequest {
    }
}
