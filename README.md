# main-backend

Демо-приложение на библиотеке `service-common` (этап 2): справочники, роли-гранты, импорт и
экспорт, статусы, нумерация документов, журнал аудита. Документация библиотеки —
`service-common/README.md`.

## Запуск

Нужны Java 21, PostgreSQL и установленная в `~/.m2` библиотека:

```bash
cd ../service-common && ./mvnw install -DskipTests
```

### База данных

Подключение задано в `src/main/resources/application.properties`:
`jdbc:postgresql://localhost:5432/main_backend?currentSchema=test`, пользователь `postgres`.

- **Пароль datasource** (`spring.datasource.password`) должен совпадать с паролем вашего
  PostgreSQL — проверьте его до запуска или передайте свой:
  `--spring.datasource.password=...`.
- **Схема `test` должна быть чистой** (пересоздайте её или укажите другую в `currentSchema`,
  `spring.flyway.*` и `hibernate.default_schema`): библиотека ведёт собственную историю Flyway
  `common_schema_history`, и схема, мигрированная старой версией, не запустится.
- Порядок при запуске: миграции библиотеки (таблицы `common_*`) → миграции приложения
  `db/migration/app` (`V1__demo_status_seed.sql` — статусы и счётчик заказов) → создание таблиц
  справочников `dict_*` по сущностям `@ArgentDictionary`.

### Профили

| Запуск | Вход |
|---|---|
| `./mvnw spring-boot:run -Dspring-boot.run.profiles=demo` | **демо**: только по логину, без пароля (`application-demo.properties`) |
| `ARGENT_ADMIN_PASSWORD=<пароль> ./mvnw spring-boot:run` | по логину и паролю |

В парольном режиме (по умолчанию) пароль администратора `admin` берётся из переменной окружения
`ARGENT_ADMIN_PASSWORD`. Если её нет, а администратора в базе ещё нет, запуск прерывается с
ошибкой `Задайте argent.auth.bootstrap-admin.password`.

> **Профиль demo небезопасен**: любой, кто достучится до бэкенда, войдёт под любым
> пользователем, включая `admin`. Только для демонстрации.

Ключ подписи JWT создаётся в `./config/argent-jwt.pem` (в `.gitignore`). Swagger UI —
`http://localhost:8080/swagger-ui.html`.

## Роли

Стартовые роли — `src/main/resources/argent-roles.json` (создаются, только если их ещё нет в БД;
изменения файла на существующие роли не действуют — правьте их через `/api/roles` или пересоздайте
схему).

| Роль | Таблицы | Действия |
|---|---|---|
| `ADMIN` (системная) | все: чтение и запись | `admin` — проходит любую проверку действия |
| `READER` | все: чтение | — |
| `EDITOR` | все: чтение и запись | `showcase.publish`, `import`, `orders.approve` |
| `MANAGER` | все: чтение; `dict_demo_order`: чтение и запись | `orders.approve` |

Пользователи создаются через `POST /api/users` и `PUT /api/users/{id}/roles` (под `admin`).

## Демо-сценарий заказа

Заказ — справочник `dict_demo_order` (`DemoOrder`, `@Audited`). Номер выдаёт счётчик `ORDER`
(шаблон `ORD-%1$s-%2$05d`, сброс каждый месяц: `ORD-2026-11-00001`), статус — машина
`ORDER_STATUS`:

```
(создание) → NEW → REVIEW → DONE
                            └ требуется действие orders.approve
```

| Запрос | Доступ | Что делает |
|---|---|---|
| `POST /api/demo/orders` `{customer, total}` | `write` на `dict_demo_order` | 201, заказ с новым номером и статусом `NEW` |
| `POST /api/demo/orders/{id}/status` `{status}` | `write` на `dict_demo_order` (+ `orders.approve` для `DONE`) | 200; непредусмотренный переход — 409, нет действия — 403 «Для перехода требуется действие orders.approve» |

```bash
B=http://localhost:8080
TOKEN=$(curl -s -X POST $B/api/auth/login -H 'Content-Type: application/json' \
  -d '{"login":"admin"}' | sed -E 's/.*"token":"([^"]+)".*/\1/')

# Тело с кириллицей передавайте файлом в UTF-8 (в консоли Windows аргумент curl уходит не в UTF-8)
printf '%s' '{"customer":"ООО Ромашка","total":1500.50}' > order.json
curl -X POST $B/api/demo/orders -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' --data-binary @order.json
# {"id":1,"number":"ORD-2026-11-00001","customer":"ООО Ромашка","status":"NEW",...}

curl -X POST $B/api/demo/orders/1/status -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' -d '{"status":"REVIEW"}'
```

## Импорт

Нужны действие `import` и `write` на таблицу справочника (роль `EDITOR` или `ADMIN`). Колонки
сопоставляются по подписям полей DTO (`Название страны`, `Код страны`) или по именам полей.

```bash
curl -X POST $B/api/import/dictionaries/dict_country -H "Authorization: Bearer $TOKEN" \
  -F file=@countries.xlsx -F mode=REPLACE
# {"total":2,"created":2,"updated":0,"failed":0,"errors":[]}
```

`REPLACE` заменяет все записи справочника, `UPSERT` требует ключевых полей (`-F keyFields=code`).
Файл до 20 МБ (лимиты multipart подняты в `application.properties`).

## Где смотреть

| Что | Запрос |
|---|---|
| Журнал изменений заказа | `GET /api/audit?entityType=dict_demo_order&entityId=<id>` |
| Экспорт справочника | `GET /api/export/dictionaries/dict_country?format=xlsx` (также `csv`, `docx`; заказы — `dict_demo_order`) |
| Доступные переходы статуса | `GET /api/status/ORDER_STATUS/transitions` (при создании), `?from=NEW` |
| Статусы, переходы, счётчики | справочники `common_status_type`, `common_status`, `common_status_transition`, `common_sequence` |
| Свои права | `GET /api/auth/me` |

## Тесты

```bash
./mvnw test    # Testcontainers, нужен запущенный Docker
```

`MainBackendApplicationTests` поднимает контекст на `postgres:17-alpine` в режиме без пароля и
проверяет роли, демо-данные статусов, сценарий заказа (номер, переходы, 403 без
`orders.approve`, аудит) и импорт XLSX стран.
