package com.atomskills.mainbackend;

import com.argent.servicecommon.dictionary.generator.DictionarySchemaGenerator;
import com.argent.servicecommon.dictionary.service.DictionaryScannerService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

/**
 * Автоконфигурация общей библиотеки.
 *
 * Регистрирует пакет служебных JPA-сущностей
 * для автоматического обнаружения Spring Boot.
 */
@SpringBootApplication
public class MainBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(MainBackendApplication.class, args);
    }
}
