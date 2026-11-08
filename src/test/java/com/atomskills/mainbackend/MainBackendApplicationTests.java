package com.atomskills.mainbackend;

import com.argent.servicecommon.auth.entity.User;
import com.argent.servicecommon.auth.repository.UserRepository;
import com.argent.servicecommon.security.entity.Role;
import com.argent.servicecommon.security.repository.RoleRepository;
import com.argent.servicecommon.security.repository.UserRoleRepository;
import com.jayway.jsonpath.JsonPath;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Проверяет, что контекст приложения поднимается на PostgreSQL из Testcontainers,
 * стартовые роли и демо-данные статусов загружены, и проходит демо-сценарий:
 * заказ с номером и статусами, аудит, импорт XLSX, права на заказы, версия заказа и вложения.
 *
 * Тесты работают в режиме входа без пароля (как профиль demo).
 */
@SpringBootTest
@AutoConfigureMockMvc
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

    /** Корень вложений, чтобы тест не писал в ./data/attachments. */
    @TempDir
    static Path attachmentsDir;

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
        registry.add("argent.attachments.root", () -> attachmentsDir.toString());
        // Вход без пароля, как в профиле demo: пользователи теста входят по логину
        registry.add("argent.auth.password-required", () -> "false");
    }

    @Autowired private RoleRepository roleRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private UserRoleRepository userRoleRepository;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private MockMvc mvc;

    @Test
    void contextLoadsWithSeededRoles() {
        assertThat(roleRepository.findByCode("ADMIN")).isPresent();
        assertThat(roleRepository.findByCode("READER")).isPresent();
        assertThat(roleRepository.findByCode("EDITOR")).isPresent();
        assertThat(roleRepository.findByCode("MANAGER")).isPresent();
    }

    @Test
    void bootstrapAdminHoldsAdminRole() {
        User admin = userRepository.findByLogin("admin").orElseThrow();
        Role adminRole = roleRepository.findByCode("ADMIN").orElseThrow();
        assertThat(userRoleRepository.findByIdUserId(admin.getId()))
                .anySatisfy(link -> assertThat(link.getId().getRoleId()).isEqualTo(adminRole.getId()));
    }

    @Test
    void statusTypeAndSequenceAreSeeded() {
        Integer statuses = jdbc.queryForObject("""
                SELECT count(*) FROM common_status s
                JOIN common_status_type t ON t.id = s.status_type_id
                WHERE t.code = 'ORDER_STATUS'""", Integer.class);
        assertThat(statuses).isEqualTo(3);
        Integer transitions = jdbc.queryForObject("""
                SELECT count(*) FROM common_status_transition tr
                JOIN common_status_type t ON t.id = tr.status_type_id
                WHERE t.code = 'ORDER_STATUS'""", Integer.class);
        assertThat(transitions).isEqualTo(3);
        assertThat(jdbc.queryForObject(
                "SELECT pattern FROM common_sequence WHERE code = 'ORDER'", String.class))
                .isEqualTo("ORD-%1$s-%2$05d");
    }

    @Test
    void orderFlowWithStatusesAndAudit() throws Exception {
        String admin = login("admin");
        // Пользователь только с ролью MANAGER: write на заказы и действие orders.approve
        String manager = userWithRoles("it-manager", "MANAGER");
        // Роль без orders.approve: write на заказы есть, согласовать нельзя
        mvc.perform(put("/api/roles/ORDER_WRITER").header(HttpHeaders.AUTHORIZATION, admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Оператор заказов",
                                 "grants": {"tables": {"*": ["read"], "dict_demo_order": ["read", "write"]}}}"""))
                .andExpect(status().isOk());
        String writer = userWithRoles("it-writer", "READER", "ORDER_WRITER");

        String created = mvc.perform(post("/api/demo/orders").header(HttpHeaders.AUTHORIZATION, admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"customer\": \"ООО Ромашка\", \"total\": 1500.50}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.number").value(matchesPattern("ORD-\\d{4}-\\d{2}-\\d{5}")))
                .andExpect(jsonPath("$.status").value("NEW"))
                .andReturn().getResponse().getContentAsString();
        Number id = JsonPath.read(created, "$.id");

        mvc.perform(post("/api/demo/orders/{id}/status", id).header(HttpHeaders.AUTHORIZATION, admin)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\": \"REVIEW\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REVIEW"));

        // Переход REVIEW → DONE требует orders.approve
        mvc.perform(post("/api/demo/orders/{id}/status", id).header(HttpHeaders.AUTHORIZATION, writer)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\": \"DONE\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail").value("Для перехода требуется действие orders.approve"));

        mvc.perform(post("/api/demo/orders/{id}/status", id).header(HttpHeaders.AUTHORIZATION, manager)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\": \"DONE\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DONE"));

        // Непредусмотренный переход — 409
        mvc.perform(post("/api/demo/orders/{id}/status", id).header(HttpHeaders.AUTHORIZATION, admin)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\": \"NEW\"}"))
                .andExpect(status().isConflict());

        mvc.perform(get("/api/audit").header(HttpHeaders.AUTHORIZATION, admin)
                        .param("entityType", "dict_demo_order").param("entityId", id.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[*].action").value(hasItems("INSERT", "UPDATE")))
                .andExpect(jsonPath("$.items[?(@.action == 'UPDATE')].field").value(hasItems("status")))
                .andExpect(jsonPath("$.items[?(@.action == 'UPDATE')].newValue").value(hasItems("REVIEW", "DONE")));
    }

    @Test
    void editorImportsCountriesFromXlsx() throws Exception {
        String editor = userWithRoles("it-editor", "EDITOR");
        MockMultipartFile file = new MockMultipartFile("file", "countries.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                xlsx(List.of(List.of("Название страны", "Код страны"),
                        List.of("Россия", "RUS"),
                        List.of("Казахстан", "KAZ"))));

        mvc.perform(multipart("/api/import/dictionaries/{code}", "dict_country").file(file)
                        .param("mode", "REPLACE").header(HttpHeaders.AUTHORIZATION, editor))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.created").value(2))
                .andExpect(jsonPath("$.failed").value(0));

        assertThat(jdbc.queryForList("SELECT code FROM dict_country ORDER BY code", String.class))
                .containsExactly("KAZ", "RUS");
    }

    @Test
    void readerCannotCreateOrder() throws Exception {
        String reader = userWithRoles("it-reader", "READER");

        mvc.perform(post("/api/demo/orders").header(HttpHeaders.AUTHORIZATION, reader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"customer\": \"ООО Читатель\", \"total\": 10}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void managerReadsOrderAudit() throws Exception {
        String manager = userWithRoles("it-audit-manager", "MANAGER");

        mvc.perform(get("/api/audit").header(HttpHeaders.AUTHORIZATION, manager)
                        .param("entityType", "dict_demo_order"))
                .andExpect(status().isOk());
    }

    @Test
    void staleVersionGets409() throws Exception {
        String admin = login("admin");
        String created = mvc.perform(post("/api/demo/orders").header(HttpHeaders.AUTHORIZATION, admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"customer\": \"ООО Версия\", \"total\": 100}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.version").value(0))
                .andReturn().getResponse().getContentAsString();
        Number id = JsonPath.read(created, "$.id");

        mvc.perform(post("/api/demo/orders/{id}/status", id).header(HttpHeaders.AUTHORIZATION, admin)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\": \"REVIEW\", \"version\": 0}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.version").value(1));

        // Клиент прислал версию, которую уже обогнало чужое изменение
        mvc.perform(post("/api/demo/orders/{id}/status", id).header(HttpHeaders.AUTHORIZATION, admin)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\": \"DONE\", \"version\": 0}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Заказ изменён другим пользователем, обновите данные"));

        assertThat(jdbc.queryForObject("SELECT status FROM dict_demo_order WHERE id = ?", String.class,
                id.longValue())).isEqualTo("REVIEW");
    }

    @Test
    void deleteOrderRemovesAttachments() throws Exception {
        String editor = userWithRoles("it-attach-editor", "EDITOR");
        String created = mvc.perform(post("/api/demo/orders").header(HttpHeaders.AUTHORIZATION, editor)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"customer\": \"ООО Вложение\", \"total\": 5}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String id = JsonPath.read(created, "$.id").toString();
        MockMultipartFile file = new MockMultipartFile("file", "note.txt", "text/plain",
                "Счёт к заказу".getBytes(StandardCharsets.UTF_8));

        mvc.perform(multipart("/api/attachments").file(file)
                        .param("entityType", "dict_demo_order").param("entityId", id)
                        .header(HttpHeaders.AUTHORIZATION, editor))
                .andExpect(status().isCreated());
        assertThat(storedFiles()).hasSize(1);

        mvc.perform(delete("/api/demo/orders/{id}", id).header(HttpHeaders.AUTHORIZATION, editor))
                .andExpect(status().isNoContent());

        mvc.perform(get("/api/attachments").header(HttpHeaders.AUTHORIZATION, editor)
                        .param("entityType", "dict_demo_order").param("entityId", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
        assertThat(storedFiles()).isEmpty();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM dict_demo_order WHERE id = ?", Integer.class,
                Long.parseLong(id))).isZero();
    }

    /** Файлы в хранилище вложений теста. */
    private static List<Path> storedFiles() throws IOException {
        if (!Files.exists(attachmentsDir)) {
            return List.of();
        }
        try (Stream<Path> files = Files.walk(attachmentsDir)) {
            return files.filter(Files::isRegularFile).toList();
        }
    }

    /** Создаёт пользователя с ролями через REST администрирования и возвращает заголовок Bearer. */
    private String userWithRoles(String login, String... roles) throws Exception {
        String admin = login("admin");
        String user = mvc.perform(post("/api/users").header(HttpHeaders.AUTHORIZATION, admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"login\": \"" + login + "\", \"displayName\": \"" + login + "\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String userId = JsonPath.read(user, "$.id");
        String codes = String.join("\", \"", roles);
        mvc.perform(put("/api/users/{id}/roles", userId).header(HttpHeaders.AUTHORIZATION, admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"roleCodes\": [\"" + codes + "\"]}"))
                .andExpect(status().isOk());
        return login(login);
    }

    /** Вход без пароля, возвращает значение заголовка Authorization. */
    private String login(String login) throws Exception {
        String body = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"login\": \"" + login + "\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return "Bearer " + JsonPath.read(body, "$.token");
    }

    /** Собирает книгу XLSX с одним листом из строк значений. */
    private static byte[] xlsx(List<List<String>> rows) throws IOException {
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Страны");
            for (int i = 0; i < rows.size(); i++) {
                Row row = sheet.createRow(i);
                for (int j = 0; j < rows.get(i).size(); j++) {
                    row.createCell(j).setCellValue(rows.get(i).get(j));
                }
            }
            workbook.write(out);
            return out.toByteArray();
        }
    }
}
