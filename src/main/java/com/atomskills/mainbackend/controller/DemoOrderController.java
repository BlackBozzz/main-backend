package com.atomskills.mainbackend.controller;

import com.atomskills.mainbackend.dto.DemoOrderDto;
import com.atomskills.mainbackend.service.DemoOrderService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

/**
 * Демо-заказы: номер из счётчика, статусная машина и аудит.
 *
 * Все методы требуют {@code write} на таблицу {@code dict_demo_order}. Переход
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

    /** Удаляет заказ и его вложения. */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("@argentAccess.table('dict_demo_order', 'write')")
    public void delete(@PathVariable Long id) {
        orderService.delete(id);
    }
}
