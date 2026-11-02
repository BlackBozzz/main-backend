package com.atomskills.mainbackend.dto;

import com.argent.servicecommon.dictionary.annotation.DictionaryField;
import com.argent.servicecommon.dictionary.dynamic.annotation.DynamicDictionaryDisplay;
import jakarta.validation.constraints.NotBlank;

/** DTO справочника «Подразделения демонстрации». */
public record DemoDepartmentDto(
        @DictionaryField(label = "Идентификатор", order = 0, readOnly = true)
        Long id,

        @DictionaryField(label = "Название", order = 10)
        @NotBlank
        @DynamicDictionaryDisplay
        String name,

        @DictionaryField(label = "Короткое название", order = 20)
        String shortName
) {
}
