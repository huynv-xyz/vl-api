package com.vlife.api.controller.salary;

import com.vlife.api.builder.SalesActualBuilder;
import com.vlife.api.controller.base.BaseController;
import com.vlife.api.dto.SalesActualItem;
import com.vlife.shared.api.dto.ApiResponse;
import com.vlife.shared.jdbc.dao.EmployeeDao;
import com.vlife.shared.jdbc.dao.salary.SalesActualDao;
import com.vlife.shared.jdbc.dao.salary.SalesTargetDao;
import com.vlife.shared.jdbc.entity.Employee;
import com.vlife.shared.jdbc.entity.salary.SalesActual;
import com.vlife.shared.jdbc.entity.salary.SalesTarget;
import io.micronaut.data.model.Page;
import io.micronaut.data.model.Pageable;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.QueryValue;
import jakarta.inject.Inject;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.vlife.shared.util.NumberUtil.parseInt;
import static com.vlife.shared.util.StringUtil.trim;

@Controller("/salary/sales-actuals")
public class SalesActualController extends BaseController {

    private final SalesActualDao salesActualDao;
    private final SalesTargetDao salesTargetDao;
    private final EmployeeDao employeeDao;
    private final SalesActualBuilder builder;

    @Inject
    public SalesActualController(
            SalesActualDao salesActualDao,
            SalesTargetDao salesTargetDao,
            EmployeeDao employeeDao,
            SalesActualBuilder builder
    ) {
        this.salesActualDao = salesActualDao;
        this.salesTargetDao = salesTargetDao;
        this.employeeDao = employeeDao;
        this.builder = builder;
    }

    @Get
    public HttpResponse<?> list(
            HttpRequest<?> request,
            @QueryValue(defaultValue = "1") int page,
            @QueryValue(defaultValue = "20") int limit
    ) {
        Pageable pageable = Pageable.from(page - 1, limit);

        Map<String, String> filters = request.getParameters().asMap()
                .entrySet()
                .stream()
                .filter(e -> !e.getKey().equals("page") && !e.getKey().equals("limit"))
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> {
                            List<String> values = e.getValue();
                            return (values != null && !values.isEmpty()) ? values.get(0) : null;
                        }
                ));

        Integer period = parseInt(filters.get("period"));
        Integer employeeId = parseInt(filters.get("employeeId"));
        String keyword = trim(filters.get("keyword"));

        Page<SalesActual> pageRs = salesActualDao.search(period, employeeId, pageable);

        List<SalesActualItem> items = pageRs.getContent()
                .stream()
                .map(actual -> {
                    Employee employee = employeeDao.findById(actual.getEmployeeId()).orElse(null);

                    if (keyword != null && !keyword.isBlank()) {
                        boolean matched = employee != null && (
                                containsIgnoreCase(employee.getCode(), keyword)
                                        || containsIgnoreCase(employee.getName(), keyword)
                                        || containsIgnoreCase(employee.getTaxCode(), keyword)
                        );

                        if (!matched) {
                            return null;
                        }
                    }

                    SalesTarget target = salesTargetDao
                            .findByEmployeeAndPeriod(actual.getEmployeeId(), actual.getPeriod())
                            .orElse(null);

                    return new SalesActualItem(actual, target, employee);
                })
                .filter(item -> item != null)
                .toList();

        List<Map<String, Object>> responseItems = builder.buildList(items);
        int currentPage = pageRs.getPageNumber() + 1;

        return HttpResponse.ok(ApiResponse.success(
                Map.of(
                        "items", responseItems,
                        "page", (pageRs.getPageNumber() + 1),
                        "size", pageRs.getSize(),
                        "total", pageRs.getTotalSize(),
                        "current_page", currentPage,
                        "total_page", pageRs.getTotalPages()
                )
        ));
    }

    private boolean containsIgnoreCase(String source, String keyword) {
        return source != null && source.toLowerCase().contains(keyword.toLowerCase());
    }
}