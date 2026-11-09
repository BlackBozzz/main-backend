package com.atomskills.mainbackend.entity;

import com.argent.servicecommon.audit.Audited;
import com.argent.servicecommon.dictionary.annotation.ArgentDictionary;
import com.atomskills.mainbackend.dto.DemoShowcaseDto;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Демонстрационный справочник с максимальным набором полей.
 * Связанные справочники не каскадируются при сохранении.
 */
@Audited
@Entity
@Table(name = "dict_demo_showcase")
@ArgentDictionary(name = "Демонстрация всех типов полей", dtoClass = DemoShowcaseDto.class)
@Getter
@Setter
@NoArgsConstructor
public class DemoShowcase {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(length = 3000)
    private String description;

    @Column(nullable = false)
    private Boolean enabled = true;

    private Byte tinyNumber;
    private Short smallNumber;
    private Integer quantity;
    private Long bigNumber;

    @Column(precision = 30, scale = 0)
    private BigInteger veryBigNumber;

    @Column(precision = 18, scale = 4)
    private BigDecimal price;

    private Float ratio;
    private Double measurement;

    private LocalDate eventDate;
    private LocalTime eventTime;
    private LocalDateTime localTimestamp;
    private Instant instantTimestamp;
    private OffsetDateTime offsetTimestamp;
    private UUID externalId;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private DemoShowcaseStatus status;

    @Lob
    private byte[] attachment;

    @Embedded
    private DemoAddress address;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private DemoCategory category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id")
    private DemoDepartment department;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "primary_contact_id", unique = true)
    private DemoContact primaryContact;

    @ManyToMany
    @JoinTable(name = "dict_demo_showcase_tag",
            joinColumns = @JoinColumn(name = "showcase_id"),
            inverseJoinColumns = @JoinColumn(name = "tag_id"))
    private List<DemoTag> tags = new ArrayList<>();

    @OneToMany
    @JoinTable(name = "dict_demo_showcase_contact",
            joinColumns = @JoinColumn(name = "showcase_id"),
            inverseJoinColumns = @JoinColumn(name = "contact_id", unique = true))
    private List<DemoContact> contacts = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "dict_demo_showcase_note",
            joinColumns = @JoinColumn(name = "showcase_id"))
    @Column(name = "note", length = 300)
    private List<String> notes = new ArrayList<>();
}
