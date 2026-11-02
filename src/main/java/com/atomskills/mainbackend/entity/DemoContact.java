package com.atomskills.mainbackend.entity;

import com.argent.servicecommon.dictionary.annotation.ArgentDictionary;
import com.atomskills.mainbackend.dto.DemoContactDto;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Тестовый справочник «Контакты демонстрации». */
@Entity
@Table(name = "dict_demo_contact")
@ArgentDictionary(name = "Контакты демонстрации", dtoClass = DemoContactDto.class)
@Getter
@Setter
@NoArgsConstructor
public class DemoContact {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(length = 180)
    private String email;

    @Column(length = 30)
    private String phone;
}
