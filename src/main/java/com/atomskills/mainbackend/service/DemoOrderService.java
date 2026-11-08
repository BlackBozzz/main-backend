package com.atomskills.mainbackend.service;

import com.argent.servicecommon.sequence.NumberGenerator;
import com.argent.servicecommon.status.StatusTransitionService;
import com.atomskills.mainbackend.dto.DemoOrderDto;
import com.atomskills.mainbackend.entity.DemoOrder;
import com.atomskills.mainbackend.repository.DemoOrderRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * Демо-сценарий заказа: номер из счётчика ORDER, статус по переходам типа ORDER_STATUS.
 *
 * Библиотека только проверяет переход; поле статуса хранит и меняет приложение.
 */
@Service
@RequiredArgsConstructor
public class DemoOrderService {

    /** Тип статуса заказов (миграция V1__demo_status_seed.sql). */
    public static final String STATUS_TYPE = "ORDER_STATUS";
    /** Код счётчика номеров заказов. */
    public static final String SEQUENCE = "ORDER";
    /** Статус нового заказа. */
    public static final String INITIAL_STATUS = "NEW";

    private final DemoOrderRepository orders;
    private final NumberGenerator numberGenerator;
    private final StatusTransitionService statusTransitions;

    /** Создаёт заказ со следующим номером и статусом NEW. */
    @Transactional
    public DemoOrderDto create(String customer, BigDecimal total) {
        statusTransitions.assertAllowed(STATUS_TYPE, null, INITIAL_STATUS);
        DemoOrder order = new DemoOrder();
        // Номер выдаётся в этой же транзакции: откат создания откатывает и счётчик
        order.setNumber(numberGenerator.next(SEQUENCE));
        order.setCustomer(customer);
        order.setTotal(total);
        order.setStatus(INITIAL_STATUS);
        return toDto(orders.save(order));
    }

    /**
     * Переводит заказ в статус target.
     *
     * @throws EntityNotFoundException заказ, тип или статус не найден (404)
     * @throws IllegalStateException   переход не предусмотрен (409)
     * @throws com.argent.servicecommon.security.exception.GrantsAccessDeniedException
     *                                 нет действия, которого требует переход (403)
     */
    @Transactional
    public DemoOrderDto changeStatus(Long id, String target) {
        DemoOrder order = orders.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Заказ не найден: " + id));
        statusTransitions.assertAllowed(STATUS_TYPE, order.getStatus(), target);
        order.setStatus(target);
        return toDto(orders.saveAndFlush(order));
    }

    private static DemoOrderDto toDto(DemoOrder order) {
        return new DemoOrderDto(order.getId(), order.getNumber(), order.getCustomer(),
                order.getStatus(), order.getTotal(), order.getCreatedAt());
    }
}
