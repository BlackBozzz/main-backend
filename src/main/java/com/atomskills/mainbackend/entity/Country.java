package com.atomskills.mainbackend.entity;

import com.argent.servicecommon.dictionary.annotation.ArgentDictionary;
import com.atomskills.mainbackend.dto.CountryDto;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

/**
 * Справочник стран.
 *
 * Таблица будет создаваться библиотекой service-common.
 */
@Entity
@Table(name = "dict_country")
@ArgentDictionary(
        name = "Страны",
        dtoClass = CountryDto.class
)
@Getter
@Setter
@NoArgsConstructor
public class Country {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Наименование страны.
     */
    @Column(nullable = false, length = 150)
    private String name;

    /**
     * Международный код страны.
     */
    @Column(length = 3)
    private String code;
}