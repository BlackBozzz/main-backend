package com.atomskills.mainbackend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Встраиваемый адрес, не являющийся отдельным справочником. */
@Embeddable
@Getter
@Setter
@NoArgsConstructor
public class DemoAddress {
    @Column(name = "address_city", length = 100)
    private String city;

    @Column(name = "address_street", length = 150)
    private String street;

    @Column(name = "address_zip", length = 12)
    private String postalCode;
}
