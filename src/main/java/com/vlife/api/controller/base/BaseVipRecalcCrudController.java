package com.vlife.api.controller.base;

import com.vlife.shared.api.builder.ItemBuilder;
import com.vlife.shared.jdbc.dao.base.BaseDao;
import com.vlife.shared.jdbc.entity.base.Identifiable;
import com.vlife.shared.service.VipRecalcJobService;
import io.micronaut.http.HttpResponse;

import java.io.Serializable;

public abstract class BaseVipRecalcCrudController<
        E extends Identifiable<ID>,
        ID extends Serializable,
        D extends BaseDao<E, ID>
        > extends BaseCrudController<E, ID, D> {

    protected final VipRecalcJobService vipRecalcJobService;

    protected BaseVipRecalcCrudController(
            D dao,
            ItemBuilder<E> builder,
            VipRecalcJobService vipRecalcJobService
    ) {
        super(dao, builder);
        this.vipRecalcJobService = vipRecalcJobService;
    }

    protected void enqueueRecalcJobIfSuccess(HttpResponse<?> response, String triggerSource, Integer triggerRefId) {
        if (response == null) {
            return;
        }

        int code = response.code();
        if (code < 200 || code >= 300) {
            return;
        }

        vipRecalcJobService.enqueueCurrentYearIfNeeded(triggerSource, triggerRefId);
    }
}