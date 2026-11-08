package com.atomskills.mainbackend;

import com.argent.servicecommon.auth.entity.User;
import com.argent.servicecommon.auth.repository.UserRepository;
import com.argent.servicecommon.security.entity.Role;
import com.argent.servicecommon.security.repository.RoleRepository;
import com.argent.servicecommon.security.repository.UserRoleRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Проверяет, что контекст приложения поднимается на PostgreSQL из Testcontainers,
 * стартовые роли загружены из argent-roles.json, а bootstrap-администратор получил ADMIN.
 */
@SpringBootTest
class MainBackendApplicationTests {

    private static final String SCHEMA = "main_backend_it";

    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17-alpine")
            .withDatabaseName("main_backend_tests")
            .withUsername("test")
            .withPassword("test");

    static {
        POSTGRES.start();
    }

    /** Каталог для ключа JWT, чтобы тест не создавал ./config/argent-jwt.pem. */
    @TempDir
    static Path keyDir;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> {
            String jdbcUrl = POSTGRES.getJdbcUrl();
            return jdbcUrl + (jdbcUrl.contains("?") ? "&" : "?") + "currentSchema=" + SCHEMA;
        });
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.flyway.schemas", () -> SCHEMA);
        registry.add("spring.flyway.default-schema", () -> SCHEMA);
        registry.add("spring.jpa.properties.hibernate.default_schema", () -> SCHEMA);
        registry.add("argent.auth.jwt.key-file", () -> keyDir.resolve("argent-jwt.pem").toString());
    }

    @Autowired private RoleRepository roleRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private UserRoleRepository userRoleRepository;

    @Test
    void contextLoadsWithSeededRoles() {
        assertThat(roleRepository.findByCode("ADMIN")).isPresent();
        assertThat(roleRepository.findByCode("READER")).isPresent();
        assertThat(roleRepository.findByCode("EDITOR")).isPresent();
    }

    @Test
    void bootstrapAdminHoldsAdminRole() {
        User admin = userRepository.findByLogin("admin").orElseThrow();
        Role adminRole = roleRepository.findByCode("ADMIN").orElseThrow();
        assertThat(userRoleRepository.findByIdUserId(admin.getId()))
                .anySatisfy(link -> assertThat(link.getId().getRoleId()).isEqualTo(adminRole.getId()));
    }
}
