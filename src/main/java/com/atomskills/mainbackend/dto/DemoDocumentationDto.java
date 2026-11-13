package com.atomskills.mainbackend.dto;

import com.argent.servicecommon.dictionary.annotation.DictionaryField;
import com.argent.servicecommon.dictionary.dynamic.annotation.DynamicDictionaryDisplay;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** DTO, которое админка редактирует через универсальную форму. */
@Schema(title = "Документ", description = "Запись демонстрационного реестра документов")
public record DemoDocumentationDto(
        @Schema(title = "Идентификатор", description = "Автоматический номер записи")
        @DictionaryField(label = "ID", order = 0, readOnly = true)
        Long id,
        @Schema(title = "Название документа", example = "Регламент работы")
        @DictionaryField(label = "Название документа", order = 10)
        @DynamicDictionaryDisplay
        @NotBlank @Size(max = 160)
        String title,
        @Schema(title = "Примечание", description = "Необязательные сведения о документе")
        @DictionaryField(label = "Примечание", order = 20)
        @Size(max = 600)
        String note
) { }
