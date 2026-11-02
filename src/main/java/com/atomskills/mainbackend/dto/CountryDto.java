
package com.atomskills.mainbackend.dto;

import com.argent.servicecommon.dictionary.annotation.DictionaryField;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTO справочника стран.
 *
 * <p>Аннотации DictionaryField используются для построения
 * динамических форм, а Jakarta Validation — для проверки
 * данных на сервере.</p>
 */
public record CountryDto(

        @DictionaryField(
                label = "Идентификатор",
                order = 0,
                readOnly = true
        )
        Long id,

        @DictionaryField(
                label = "Название страны",
                order = 10
        )
        @NotBlank(message = "Название страны обязательно.")
        @Size(max = 150, message = "Название не должно превышать 150 символов.")
        String name,

        @DictionaryField(
                label = "Код страны",
                order = 20
        )
        @Size(max = 3, message = "Код страны не должен превышать 3 символа.")
        String code

) {
}
