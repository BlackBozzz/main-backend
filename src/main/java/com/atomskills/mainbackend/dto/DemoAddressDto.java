package com.atomskills.mainbackend.dto;

import com.argent.servicecommon.dictionary.annotation.DictionaryField;

/** Поля встроенного адреса. */
public record DemoAddressDto(
        @DictionaryField(label = "Город", order = 10) String city,
        @DictionaryField(label = "Улица", order = 20) String street,
        @DictionaryField(label = "Почтовый индекс", order = 30) String postalCode
) {
}
