package com.atomskills.mainbackend.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Демонстрация проверки действия приложения через гранты.
 *
 * Публикация витрины доступна только пользователю с действием {@code showcase.publish}
 * (роль EDITOR) или с системной ролью ADMIN.
 */
@RestController
public class DemoShowcaseController {

    /** Демонстрационная публикация витрины: при наличии права отвечает {@code {"status":"ok"}}. */
    @PreAuthorize("@argentAccess.action('showcase.publish')")
    @PostMapping("/api/demo/showcase/publish")
    public Map<String, String> publish() {
        return Map.of("status", "ok");
    }
}
