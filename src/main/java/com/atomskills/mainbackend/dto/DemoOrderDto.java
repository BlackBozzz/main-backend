package com.atomskills.mainbackend.dto;

import com.argent.servicecommon.dictionary.annotation.DictionaryField;
import com.argent.servicecommon.dictionary.dynamic.annotation.DynamicDictionaryDisplay;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * DTO демо-заказа. Номер, статус, дата создания и версия только для чтения:
 * их выставляет DemoOrderController (номер — NumberGenerator, статус — через проверку перехода).
 */
public record DemoOrderDto(

        @DictionaryField(label = "Идентификатор", order = 0, readOnly = true)
        Long id,

        @DictionaryField(label = "Номер", order = 10, readOnly = true)
        @DynamicDictionaryDisplay
        String number,

        @DictionaryField(label = "Заказчик", order = 20)
        @NotBlank(message = "Заказчик обязателен.")
        @Size(max = 255, message = "Заказчик не должен превышать 255 символов.")
        String customer,

        @DictionaryField(label = "Статус", order = 30, readOnly = true)
        String status,

        @DictionaryField(label = "Сумма", order = 40)
        @DecimalMin(value = "0.0", message = "Сумма не может быть отрицательной.")
        BigDecimal total,

        @DictionaryField(label = "Создан", order = 50, readOnly = true)
        Instant createdAt,

        @DictionaryField(label = "Версия", order = 60, readOnly = true)
        Long version

) {
}
