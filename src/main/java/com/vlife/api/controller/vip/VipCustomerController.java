package com.vlife.api.controller.vip;

import com.vlife.api.builder.vip.VipBuilder;
import com.vlife.api.controller.base.BaseCrudController;
import com.vlife.api.util.ApiUtil;
import com.vlife.shared.jdbc.dao.CustomerVipYearlyResultDao;
import com.vlife.shared.jdbc.entity.CustomerVipYearlyResult;
import io.micronaut.data.model.Page;
import io.micronaut.data.model.Pageable;
import io.micronaut.http.annotation.Controller;

import jakarta.inject.Inject;

import java.util.Map;

@Controller("/vip")
public class VipCustomerController
        extends BaseCrudController<CustomerVipYearlyResult, Integer, CustomerVipYearlyResultDao> {

    @Inject
    public VipCustomerController(CustomerVipYearlyResultDao dao, VipBuilder builder) {
        super(dao, builder);
    }

    // =========================
    // SEARCH
    // =========================
    @Override
    protected Page<CustomerVipYearlyResult> doSearch(Map<String, String> filters, Pageable pageable) {

        Integer calcYear = ApiUtil.parseInteger(filters.get("calc_year"));
        String keyword = ApiUtil.trim(filters.get("keyword"));
        String customerType = ApiUtil.trim(filters.get("customer_type"));
        String region = ApiUtil.trim(filters.get("region"));
        String groupCode = ApiUtil.trim(filters.get("group_code"));
        String tierCode = ApiUtil.trim(filters.get("tier_code"));
        Integer status = ApiUtil.parseInteger(filters.get("status"));

        return dao.search(
                calcYear,
                keyword,
                customerType,
                region,
                groupCode,
                tierCode,
                status,
                pageable
        );
    }
}