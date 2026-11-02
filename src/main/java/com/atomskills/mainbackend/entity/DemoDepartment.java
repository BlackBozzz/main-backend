package com.atomskills.mainbackend.entity;

import com.argent.servicecommon.dictionary.annotation.ArgentDictionary;
import com.atomskills.mainbackend.dto.DemoDepartmentDto;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Тестовый справочник «Подразделения демонстрации». */
@Entity
@Table(name = "dict_demo_department")
@ArgentDictionary(name = "Подразделения демонстрации", dtoClass = DemoDepartmentDto.class)
@Getter
@Setter
@NoArgsConstructor
public class DemoDepartment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(length = 40)
    private String shortName;
}
