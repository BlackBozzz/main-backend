package com.atomskills.mainbackend.dto;

/**
 * Данные справочника стран.
 *
 * В будущем на основе DTO будут формироваться
 * метаданные динамических форм Angular.
 */
public record CountryDto(
        Long id,
        String name,
        String code
) {
}