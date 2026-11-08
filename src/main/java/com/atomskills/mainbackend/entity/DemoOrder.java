package com.atomskills.mainbackend.entity;

import com.argent.servicecommon.audit.Audited;
import com.argent.servicecommon.dictionary.annotation.ArgentDictionary;
import com.atomskills.mainbackend.dto.DemoOrderDto;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Демонстрационный заказ: номер из счётчика ORDER, статус из типа ORDER_STATUS, журнал аудита.
 *
 * Таблицу создаёт регистрация справочников библиотеки. Заказ создаётся и меняет статус
 * через DemoOrderController; общий REST справочника позволяет смотреть и править
 * только незащищённые поля (номер, статус и дата создания в DTO только для чтения).
 */
@Entity
@Audited
@Table(name = "dict_demo_order")
@ArgentDictionary(name = "Демо-заказы", dtoClass = DemoOrderDto.class)
@Getter
@Setter
@NoArgsConstructor
public class DemoOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Номер заказа из NumberGenerator, например ORD-2026-11-00001. */
    @Column(nullable = false, unique = true, length = 50)
    private String number;

    /** Заказчик. */
    @Column(nullable = false, length = 255)
    private String customer;

    /** Код статуса из common_status типа ORDER_STATUS. */
    @Column(nullable = false, length = 100)
    private String status;

    /** Сумма заказа. */
    @Column(precision = 18, scale = 2)
    private BigDecimal total;

    /** Момент создания заказа. */
    @Column(nullable = false)
    private Instant createdAt;

    /** Дата создания проставляется при первой записи, если её не задал код. */
    @PrePersist
    void fillCreatedAt() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
