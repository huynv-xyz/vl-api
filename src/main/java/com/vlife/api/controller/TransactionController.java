package com.vlife.api.controller;

import com.vlife.api.builder.TransactionBuilder;
import com.vlife.api.controller.base.BaseCrudController;
import com.vlife.shared.api.dto.ApiResponse;
import com.vlife.shared.jdbc.dao.SalesTransactionDao;
import com.vlife.shared.jdbc.entity.SalesTransaction;
import com.vlife.shared.service.SalesTransactionImportService;
import com.vlife.shared.service.VipRecalcJobService;
import io.micronaut.data.model.Page;
import io.micronaut.data.model.Pageable;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Part;
import io.micronaut.http.annotation.Post;
import io.micronaut.http.multipart.CompletedFileUpload;
import jakarta.inject.Inject;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Controller("/transactions")
public class TransactionController extends BaseCrudController<SalesTransaction, Integer, SalesTransactionDao> {

    private final SalesTransactionImportService importService;
    private final VipRecalcJobService vipRecalcJobService;

    @Inject
    public TransactionController(
            SalesTransactionDao dao,
            TransactionBuilder builder,
            SalesTransactionImportService importService,
            VipRecalcJobService vipRecalcJobService
    ) {
        super(dao, builder);
        this.importService = importService;
        this.vipRecalcJobService = vipRecalcJobService;
    }

    @Override
    protected Page<SalesTransaction> doSearch(Map<String, String> filters, Pageable pageable) {
        String keyword = trim(filters.get("keyword"));
        String customerCode = trim(filters.get("customer_code"));
        String productCode = trim(filters.get("product_code"));
        String privateCode = trim(filters.get("private_code"));
        String customerType = trim(filters.get("customer_type"));
        String region = trim(filters.get("region"));
        String hdnStatus = trim(filters.get("hdn_status"));
        Integer processMonth = parseInteger(filters.get("process_month"));
        Integer status = parseInteger(filters.get("status"));

        LocalDateTime documentDateFrom = parseDateStart(filters.get("document_date_from"));
        LocalDateTime documentDateTo = parseDateEndExclusive(filters.get("document_date_to"));

        return dao.search(
                keyword,
                customerCode,
                productCode,
                privateCode,
                customerType,
                region,
                hdnStatus,
                processMonth,
                status,
                documentDateFrom,
                documentDateTo,
                pageable
        );
    }

    @Post(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA, produces = MediaType.APPLICATION_JSON)
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

            int inserted = importService.replaceAllFromCsv(target.toString());
            vipRecalcJobService.enqueueCurrentYearIfNeeded("SALES_TRANSACTION_IMPORTED", null);

            Map<String, Object> result = new HashMap<>();
            result.put("message", "Import CSV thành công");
            result.put("file_name", safeFilename);
            result.put("file_path", target.toString());
            result.put("inserted", inserted);

            return HttpResponse.ok(ApiResponse.success(result));
        } catch (IOException e) {
            return HttpResponse.ok(ApiResponse.error(-500, "Không thể lưu file CSV: " + e.getMessage()));
        } catch (Exception e) {
            return HttpResponse.ok(ApiResponse.error(-500, "Import CSV thất bại: " + e.getMessage()));
        }
    }

    private String trim(String s) {
        return s == null ? null : s.trim();
    }

    private Integer parseInteger(String value) {
        try {
            return value == null || value.isBlank() ? null : Integer.parseInt(value);
        } catch (Exception e) {
            return null;
        }
    }

    private LocalDateTime parseDateStart(String value) {
        try {
            return value == null || value.isBlank()
                    ? null
                    : LocalDate.parse(value.trim()).atStartOfDay();
        } catch (Exception e) {
            return null;
        }
    }

    private LocalDateTime parseDateEndExclusive(String value) {
        try {
            return value == null || value.isBlank()
                    ? null
                    : LocalDate.parse(value.trim()).plusDays(1).atStartOfDay();
        } catch (Exception e) {
            return null;
        }
    }
}