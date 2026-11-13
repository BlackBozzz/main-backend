package com.atomskills.mainbackend.entity;

import com.argent.servicecommon.dictionary.annotation.ArgentDataTable;
import com.atomskills.mainbackend.dto.DemoDocumentationDto;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Пример таблицы приложения, созданной ТОЛЬКО миграцией V2. */
@Entity
@Table(name = "app_demo_documentation")
@ArgentDataTable(name = "Документы", group = "Управление документацией",
        dtoClass = DemoDocumentationDto.class)
@Getter @Setter @NoArgsConstructor
public class DemoDocumentation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 160)
    private String title;

    @Column(length = 600)
    private String note;
}
