package com.vlife.api.worker;

import com.vlife.shared.jdbc.dao.CustomerVipYearlyResultDao;
import com.vlife.shared.jdbc.dao.vip.VipRecalcJobDao;
import com.vlife.shared.jdbc.entity.vip.VipRecalcJob;
import com.vlife.shared.service.CustomerVipCalculationService;
import io.micronaut.scheduling.annotation.Scheduled;
import jakarta.inject.Singleton;

@Singleton
public class VipRecalcWorker {

    private final VipRecalcJobDao vipRecalcJobDao;
    private final CustomerVipYearlyResultDao customerVipYearlyResultDao;
    private final CustomerVipCalculationService calculationService;

    public VipRecalcWorker(
            VipRecalcJobDao vipRecalcJobDao,
            CustomerVipYearlyResultDao customerVipYearlyResultDao,
            CustomerVipCalculationService calculationService
    ) {
        this.vipRecalcJobDao = vipRecalcJobDao;
        this.customerVipYearlyResultDao = customerVipYearlyResultDao;
        this.calculationService = calculationService;
    }

    @Scheduled(fixedDelay = "10s")
    public void process() {
        var jobOpt = vipRecalcJobDao.findFirstPending();
        if (jobOpt.isEmpty()) {
            return;
        }

        VipRecalcJob job = jobOpt.get();

        if (job.getCalcYear() == null) {
            vipRecalcJobDao.markFailed(job.getId(), "calcYear is required");
            return;
        }

        int locked = vipRecalcJobDao.markProcessing(job.getId());
        if (locked <= 0) {
            return;
        }

        try {
            int year = job.getCalcYear();
            customerVipYearlyResultDao.deleteByYear(year);
            calculationService.calculateYear(year);
            vipRecalcJobDao.markDone(job.getId());
        } catch (Exception e) {
            vipRecalcJobDao.markFailed(job.getId(), safeErrorMessage(e));
        }
    }

    private String safeErrorMessage(Exception e) {
        if (e == null) {
            return "Unknown error";
        }

        String msg = e.getMessage();
        if (msg == null || msg.isBlank()) {
            return e.getClass().getSimpleName();
        }

        return msg.length() > 1000 ? msg.substring(0, 1000) : msg;
    }
}