package com.atomskills.mainbackend.dto;

import java.math.BigDecimal;
import java.time.Instant;

/** Mutable DTO специально для selectDto: показывает обычные и вложенные алиасы. */
public class DemoOrderQueryRow {
    private Long id;
    private String number;
    private String status;
    private BigDecimal total;
    private Instant createdAt;
    private Details details;

    public DemoOrderQueryRow() { }
    public Long getId() { return id; }
    public String getNumber() { return number; }
    public String getStatus() { return status; }
    public BigDecimal getTotal() { return total; }
    public Instant getCreatedAt() { return createdAt; }
    public Details getDetails() { return details; }

    public static class Details {
        private String customer;
        public Details() { }
        public String getCustomer() { return customer; }
    }
}
