package com.atomskills.mainbackend.dto;

import com.argent.servicecommon.dictionary.annotation.DictionaryField;
import com.argent.servicecommon.dictionary.dynamic.annotation.DynamicDictionaryDisplay;
import com.argent.servicecommon.dictionary.dynamic.annotation.DynamicFormField;
import com.atomskills.mainbackend.entity.DemoShowcaseStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.*;
import java.util.List;
import java.util.UUID;

/** Полный DTO демонстрации типов и связей справочников. */
public record DemoShowcaseDto(
        @DictionaryField(label = "Идентификатор", order = 0, readOnly = true)
        Long id,

        @DictionaryField(label = "Название", order = 10)
        @DynamicDictionaryDisplay
        @NotBlank
        @Size(max = 150)
        String name,

        @DictionaryField(label = "Описание", order = 20)
        @Size(max = 3000)
        @DynamicFormField(control = DynamicFormField.Control.TEXTAREA)
        String description,

        @DictionaryField(label = "Включено", order = 30)
        @NotNull
        Boolean enabled,

        @DictionaryField(label = "Число Byte", order = 40)
        Byte tinyNumber,

        @DictionaryField(label = "Число Short", order = 50)
        Short smallNumber,

        @DictionaryField(label = "Количество", order = 60)
        @Min(0)
        @Max(1000000)
        Integer quantity,

        @DictionaryField(label = "Число Long", order = 70)
        Long bigNumber,

        @DictionaryField(label = "Число BigInteger", order = 80)
        BigInteger veryBigNumber,

        @DictionaryField(label = "Стоимость", order = 90)
        @DecimalMin("0.0")
        BigDecimal price,

        @DictionaryField(label = "Коэффициент Float", order = 100)
        Float ratio,

        @DictionaryField(label = "Измерение Double", order = 110)
        Double measurement,

        @DictionaryField(label = "Дата", order = 120)
        LocalDate eventDate,

        @DictionaryField(label = "Время", order = 130)
        LocalTime eventTime,

        @DictionaryField(label = "Дата и время", order = 140)
        LocalDateTime localTimestamp,

        @DictionaryField(label = "Момент времени Instant", order = 150)
        Instant instantTimestamp,

        @DictionaryField(label = "Дата с часовым поясом", order = 160)
        OffsetDateTime offsetTimestamp,

        @DictionaryField(label = "Внешний UUID", order = 170)
        UUID externalId,

        @DictionaryField(label = "Статус", order = 180)
        DemoShowcaseStatus status,

        @DictionaryField(label = "Бинарное вложение", order = 190)
        byte[] attachment,

        @DictionaryField(label = "Адрес", order = 200)
        @Valid
        DemoAddressDto address,

        @DictionaryField(label = "Категория (одна)", order = 210)
        DemoCategoryDto category,

        @DictionaryField(label = "Подразделение (одно)", order = 220)
        DemoDepartmentDto department,

        @DictionaryField(label = "Основной контакт (один)", order = 230)
        DemoContactDto primaryContact,

//        @DictionaryField(label = "Теги (множественный выбор)", order = 240)
//        List<DemoTagDto> tags,

        @DictionaryField(label = "Контакты (множественный выбор)", order = 250)
        List<DemoContactDto> contacts,
//
        @DictionaryField(label = "Заметки (список строк)", order = 260)
        List<String> notes
) {
}
