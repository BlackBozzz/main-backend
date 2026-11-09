package com.atomskills.mainbackend.entity;

import com.argent.servicecommon.audit.Audited;
import com.argent.servicecommon.dictionary.annotation.ArgentDictionary;
import com.atomskills.mainbackend.dto.DemoCategoryDto;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Тестовый справочник «Категории демонстрации». */
@Audited
@Entity
@Table(name = "dict_demo_category")
@ArgentDictionary(name = "Категории демонстрации", dtoClass = DemoCategoryDto.class)
@Getter
@Setter
@NoArgsConstructor
public class DemoCategory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(nullable = false, unique = true, length = 30)
    private String code;
}
