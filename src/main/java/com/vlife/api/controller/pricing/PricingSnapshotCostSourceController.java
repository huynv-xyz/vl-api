package com.vlife.api.controller.pricing;

import com.vlife.api.builder.pricing.PricingSnapshotCostSourceBuilder;
import com.vlife.api.controller.base.BaseCrudController;
import com.vlife.shared.jdbc.dao.pricing.PricingSnapshotCostSourceDao;
import com.vlife.shared.jdbc.entity.pricing.PricingSnapshotCostSource;
import com.vlife.shared.service.pricing.PricingService;
import io.micronaut.data.model.Page;
import io.micronaut.data.model.Pageable;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.PathVariable;
import com.vlife.shared.api.dto.ApiResponse;

import java.util.Map;

@Controller("/pricing/cost-sources")
public class PricingSnapshotCostSourceController extends BaseCrudController<PricingSnapshotCostSource, Integer, PricingSnapshotCostSourceDao> {
    private final PricingService pricingService;

    public PricingSnapshotCostSourceController(PricingSnapshotCostSourceDao dao,
                                               PricingSnapshotCostSourceBuilder builder,
                                               PricingService pricingService) {
        super(dao, builder);
        this.pricingService = pricingService;
    }

    @Override
    protected Page<PricingSnapshotCostSource> doSearch(Map<String, String> filters, Pageable pageable) {
        return dao.findAll(pageable);
    }

    @Get("/snapshot-item/{snapshotItemId}")
    public HttpResponse<?> bySnapshotItem(@PathVariable Integer snapshotItemId) {
        return HttpResponse.ok(ApiResponse.success(pricingService.findCostSources(snapshotItemId)));
    }
}
