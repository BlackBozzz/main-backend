package com.atomskills.mainbackend.entity;

import com.argent.servicecommon.audit.Audited;
import com.argent.servicecommon.dictionary.annotation.ArgentDictionary;
import com.atomskills.mainbackend.dto.DemoTagDto;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Тестовый справочник «Теги демонстрации». */
@Audited
@Entity
@Table(name = "dict_demo_tag")
@ArgentDictionary(name = "Теги демонстрации", dtoClass = DemoTagDto.class)
@Getter
@Setter
@NoArgsConstructor
public class DemoTag {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(length = 20)
    private String color;
}
