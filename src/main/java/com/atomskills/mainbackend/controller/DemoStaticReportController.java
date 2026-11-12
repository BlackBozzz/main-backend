package com.atomskills.mainbackend.controller;

import com.argent.servicecommon.report.ReportFormat;
import com.argent.servicecommon.report.ReportRequest;
import com.argent.servicecommon.report.ReportService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;

/** Отчёт без БД: зашитые строки для быстрой проверки JasperReports (PDF / XLSX / DOCX). */
@RestController
@RequestMapping("/api/demo/reports")
@PreAuthorize("isAuthenticated()")
public class DemoStaticReportController {
    private final ReportService reports;

    public DemoStaticReportController(ReportService reports) {
        this.reports = reports;
    }

    public record SaleRow(String product, String department, Integer quantity, BigDecimal price) {
        public BigDecimal getAmount() { return price.multiply(BigDecimal.valueOf(quantity)); }
        public String getProduct() { return product; }
        public String getDepartment() { return department; }
        public Integer getQuantity() { return quantity; }
        public BigDecimal getPrice() { return price; }
    }

    /** JSON-таблица на UI показывает ровно те данные, которые попадают в Jasper. */
    @GetMapping("/preview")
    public List<SaleRow> preview() { return rows(); }

    @GetMapping("/static")
    public ResponseEntity<byte[]> staticReport(@RequestParam(defaultValue = "pdf") String format) {
        // Простой detail-бэнд. Данные формируются в памяти, без SQL.
        return reports.render(ReportRequest.template("classpath:reports/demo-sales.jrxml")
                .data(rows())
                .fileName("demo-sales")
                .format(ReportFormat.parse(format))).toResponseEntity();
    }

    @GetMapping("/table")
    public ResponseEntity<byte[]> tableReport(@RequestParam(defaultValue = "pdf") String format) {
        // Второй шаблон рисует настоящую JasperReports Table (subDataset + JRDataSource).
        return reports.render(ReportRequest.template("classpath:reports/demo-sales-table.jrxml")
                .param("rows", rows())
                .fileName("demo-sales-table")
                .format(ReportFormat.parse(format))).toResponseEntity();
    }

    private static List<SaleRow> rows() {
        return List.of(
                new SaleRow("Ноутбук", "Электроника", 2, new BigDecimal("95000.00")),
                new SaleRow("Клавиатура", "Электроника", 4, new BigDecimal("4500.00")),
                new SaleRow("Блокнот", "Канцелярия", 7, new BigDecimal("320.00")),
                new SaleRow("Ручка", "Канцелярия", 12, new BigDecimal("80.00")),
                new SaleRow("Монитор", "Электроника", 3, new BigDecimal("27500.00")));
    }
}
