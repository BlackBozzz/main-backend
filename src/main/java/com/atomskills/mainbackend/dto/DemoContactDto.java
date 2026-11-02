package com.atomskills.mainbackend.dto;

import com.argent.servicecommon.dictionary.annotation.DictionaryField;
import com.argent.servicecommon.dictionary.dynamic.annotation.DynamicDictionaryDisplay;
import jakarta.validation.constraints.NotBlank;

/** DTO справочника «Контакты демонстрации». */
public record DemoContactDto(
        @DictionaryField(label = "Идентификатор", order = 0, readOnly = true)
        Long id,

        @DictionaryField(label = "Название", order = 10)
        @NotBlank
        @DynamicDictionaryDisplay
        String name,

        @DictionaryField(label = "Электронная почта", order = 20)
        String email,

        @DictionaryField(label = "Телефон", order = 30)
        String phone
) {
}
