package com.vlife.api.dto;

import com.vlife.shared.jdbc.entity.Employee;
import com.vlife.shared.jdbc.entity.salary.SalesActual;
import com.vlife.shared.jdbc.entity.salary.SalesTarget;
import io.micronaut.serde.annotation.Serdeable;

@Serdeable
public class SalesActualItem {

    private SalesActual actual;
    private SalesTarget target;
    private Employee employee;

    public SalesActualItem() {
    }

    public SalesActualItem(SalesActual actual, SalesTarget target, Employee employee) {
        this.actual = actual;
        this.target = target;
        this.employee = employee;
    }

    public SalesActual getActual() {
        return actual;
    }

    public void setActual(SalesActual actual) {
        this.actual = actual;
    }

    public SalesTarget getTarget() {
        return target;
    }

    public void setTarget(SalesTarget target) {
        this.target = target;
    }

    public Employee getEmployee() {
        return employee;
    }

    public void setEmployee(Employee employee) {
        this.employee = employee;
    }
}