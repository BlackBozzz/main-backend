package com.atomskills.mainbackend.controller;

import com.argent.servicecommon.audit.dto.AuditEntryDto;
import com.argent.servicecommon.report.ReportFormat;
import com.argent.servicecommon.report.ReportRequest;
import com.argent.servicecommon.report.ReportService;
import com.argent.servicecommon.security.CurrentUser;
import com.atomskills.mainbackend.dto.DemoOrderDto;
import com.atomskills.mainbackend.service.DemoOrderService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Демо-заказы: номер из счётчика, статусная машина и аудит.
 *
 * Изменяющие методы требуют {@code write} на таблицу {@code dict_demo_order}, печать отчётов
 * (JasperReports, шаблоны в {@code resources/reports}) — {@code read}. Переход
 * REVIEW → DONE дополнительно требует действие {@code orders.approve} (проверяет
 * StatusTransitionService, отказ — 403). Журнал изменений заказа отдаёт библиотека:
 * {@code GET /api/audit?entityType=dict_demo_order&entityId=<id>}. Удаление заказа
 * удаляет и его вложения ({@code /api/attachments}).
 */
@RestController
@RequestMapping("/api/demo/orders")
@RequiredArgsConstructor
public class DemoOrderController {

    private final DemoOrderService orderService;
    private final ReportService reports;

    /** Тело создания заказа. */
    public record CreateOrderRequest(
            @NotBlank(message = "Заказчик обязателен.")
            @Size(max = 255, message = "Заказчик не должен превышать 255 символов.")
            String customer,
            @DecimalMin(value = "0.0", message = "Сумма не может быть отрицательной.")
            BigDecimal total) {
    }

    /** Тело смены статуса; version — версия заказа, которую видел клиент (необязательна). */
    public record ChangeStatusRequest(
            @NotBlank(message = "Укажите статус.")
            String status,
            Long version) {
    }

    /** Создаёт заказ: номер из счётчика ORDER, статус NEW. */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("@argentAccess.table('dict_demo_order', 'write')")
    public DemoOrderDto create(@Valid @RequestBody CreateOrderRequest request) {
        return orderService.create(request.customer(), request.total());
    }

    /** Переводит заказ в новый статус, если переход разрешён текущему пользователю. */
    @PostMapping("/{id}/status")
    @PreAuthorize("@argentAccess.table('dict_demo_order', 'write')")
    public DemoOrderDto changeStatus(@PathVariable Long id, @Valid @RequestBody ChangeStatusRequest request) {
        return orderService.changeStatus(id, request.status(), request.version());
    }

    /** Отчёт по заказам, созданным с from по to включительно (SQL в шаблоне), в формате pdf, xlsx или docx. */
    @GetMapping("/report")
    @PreAuthorize("@argentAccess.table('dict_demo_order', 'read')")
    public ResponseEntity<byte[]> report(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                         @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                                         @RequestParam(defaultValue = "pdf") String format) {
        // Шаблон сравнивает created_at < to, поэтому верхняя граница — начало следующего дня
        return reports.render(ReportRequest.template("classpath:reports/orders-period.jrxml")
                        .param("from", from)
                        .param("to", to.plusDays(1))
                        .fileName("Заказы за период")
                        .format(ReportFormat.parse(format)))
                .toResponseEntity();
    }

    /** Карточка заказа с историей изменений из аудита в формате pdf, xlsx или docx. */
    @GetMapping("/{id}/card")
    @PreAuthorize("@argentAccess.table('dict_demo_order', 'read')")
    public ResponseEntity<byte[]> card(@PathVariable Long id, @RequestParam(defaultValue = "pdf") String format) {
        DemoOrderDto order = orderService.get(id);
        List<AuditEntryDto> history = orderService.history(id);
        return reports.render(ReportRequest.template("classpath:reports/order-card.jrxml")
                        .data(List.of(order))
                        .param("printedBy", CurrentUser.login().orElse("—"))
                        .param("history", history)
                        .fileName("Заказ " + order.number())
                        .format(ReportFormat.parse(format)))
                .toResponseEntity();
    }

    /** Удаляет заказ и его вложения. */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("@argentAccess.table('dict_demo_order', 'write')")
    public void delete(@PathVariable Long id) {
        orderService.delete(id);
    }
}
