package com.atomskills.mainbackend.service;

import com.argent.servicecommon.attachment.AttachmentService;
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
import java.util.Objects;

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
    /** Таблица заказов: владелец вложений и тип записи в журнале аудита. */
    public static final String ENTITY_TYPE = "dict_demo_order";

    private final DemoOrderRepository orders;
    private final NumberGenerator numberGenerator;
    private final StatusTransitionService statusTransitions;
    private final AttachmentService attachments;

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
     * Переводит заказ в статус target. Если передана expectedVersion, она должна совпасть
     * с текущей версией заказа.
     *
     * @throws EntityNotFoundException заказ, тип или статус не найден (404)
     * @throws IllegalStateException   переход не предусмотрен или заказ изменён другим пользователем (409)
     * @throws com.argent.servicecommon.security.exception.GrantsAccessDeniedException
     *                                 нет действия, которого требует переход (403)
     */
    @Transactional
    public DemoOrderDto changeStatus(Long id, String target, Long expectedVersion) {
        DemoOrder order = find(id);
        // Заказ загружен в этой транзакции, поэтому @Version сам не увидит устаревшие данные клиента
        if (expectedVersion != null && !Objects.equals(expectedVersion, order.getVersion())) {
            throw new IllegalStateException("Заказ изменён другим пользователем, обновите данные");
        }
        statusTransitions.assertAllowed(STATUS_TYPE, order.getStatus(), target);
        order.setStatus(target);
        return toDto(orders.saveAndFlush(order));
    }

    /**
     * Удаляет заказ вместе с его вложениями; файлы вложений удаляются после коммита.
     *
     * @throws EntityNotFoundException заказ не найден (404)
     */
    @Transactional
    public void delete(Long id) {
        orders.delete(find(id));
        attachments.deleteAll(ENTITY_TYPE, id.toString());
    }

    private DemoOrder find(Long id) {
        return orders.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Заказ не найден: " + id));
    }

    private static DemoOrderDto toDto(DemoOrder order) {
        return new DemoOrderDto(order.getId(), order.getNumber(), order.getCustomer(),
                order.getStatus(), order.getTotal(), order.getCreatedAt(), order.getVersion());
    }
}
