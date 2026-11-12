# main-backend

Демо на [`service-common`](../service-common/README.md): справочники, роли-гранты, заказы со
статусами и номерами, вложения, журнал аудита, импорт/экспорт. UI к нему — `../main-app`.

## Запуск

Нужны Java 21, PostgreSQL и библиотека в `~/.m2`: `cd ../service-common && ./mvnw -q install -DskipTests`.

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=demo   # вход только по логину (небезопасно, только демо)
ARGENT_ADMIN_PASSWORD=<пароль> ./mvnw spring-boot:run     # вход по логину и паролю
```

Без профиля `demo` и без `ARGENT_ADMIN_PASSWORD` первый запуск прерывается: «Задайте argent.auth.bootstrap-admin.password».

**База.** Подключение — в `src/main/resources/application.properties` (`currentSchema=test`,
пароль должен совпадать с вашим PostgreSQL). Схема `test` должна быть чистой: библиотека ведёт
свою историю Flyway и создаёт таблицы `dict_*` только если их нет. На схеме прошлого этапа у
`dict_demo_order` нет колонки `version` — пересоздайте схему или выполните
`ALTER TABLE test.dict_demo_order ADD COLUMN version bigint NOT NULL DEFAULT 0;` (`null` в `@Version` ломает изменения).

Проверка: в логе `Started MainBackendApplication`, `http://localhost:8080/swagger-ui.html` открывается.

## Роли (`ADMIN` создаёт библиотека, остальные — из `argent-roles.json`, если их нет)

| Роль | Таблицы | Действия |
|---|---|---|
| `ADMIN` (системная, пользователь `admin`) | все: read, write | `admin` |
| `READER` | все: read | — |
| `EDITOR` | все: read, write | `showcase.publish`, `import`, `orders.approve` |
| `MANAGER` | все: read; `dict_demo_order`: read, write | `orders.approve` |

## Что потыкать

```bash
B=http://localhost:8080
TOKEN=$(curl -s -X POST $B/api/auth/login -H 'Content-Type: application/json' \
  -d '{"login":"admin"}' | sed -E 's/.*"token":"([^"]+)".*/\1/')   # в парольном режиме добавьте "password"
H="Authorization: Bearer $TOKEN"
curl -X POST $B/api/demo/orders -H "$H" -H 'Content-Type: application/json' -d '{"customer":"Romashka","total":1500}'
# {"id":1,"number":"ORD-2026-11-00001","status":"NEW","version":0,...}
curl -X POST $B/api/demo/orders/1/status -H "$H" -H 'Content-Type: application/json' -d '{"status":"REVIEW","version":0}'
curl -X POST $B/api/demo/orders/1/status -H "$H" -H 'Content-Type: application/json' -d '{"status":"DONE","version":0}'  # 409: устаревшая версия
curl -X POST $B/api/attachments -H "$H" -F entityType=dict_demo_order -F entityId=1 -F file=@README.md
curl "$B/api/attachments?entityType=dict_demo_order&entityId=1" -H "$H"
curl "$B/api/audit?entityType=dict_demo_order&entityId=1" -H "$H"    # создание, статусы, файл
```

- Статусы `NEW → REVIEW → DONE`; `DONE` требует действие `orders.approve` (иначе 403), лишний переход — 409.
- `DELETE /api/demo/orders/{id}` удаляет заказ вместе с вложениями.
- Заказы создаются только через `POST /api/demo/orders` (номер выдаёт счётчик). Форма «Демо-заказы» в админке — для просмотра и правки существующих; создание из неё без номера даёт 409 «Запись нарушает ограничение целостности базы данных.».
- Правки справочников в админке попадают в «Журнал» (все сущности помечены `@Audited`).
- Экспорт: `GET /api/export/dictionaries/dict_country?format=xlsx` (`csv`, `docx`). Импорт:
  `POST /api/import/dictionaries/dict_country` с `-F file=@countries.xlsx -F mode=REPLACE` (действие `import`).
- Кириллицу в теле curl в консоли Windows передавайте файлом в UTF-8 (`--data-binary @order.json`).

## Тесты

`./mvnw test` — Testcontainers (нужен Docker): роли, заказ (номер, переходы, 409, права), вложения, аудит, импорт.

## Новые демонстрационные REST-сценарии

Добавлены `/api/demo/mail`, `/api/demo/queues`, `/api/demo/querydsl/orders`, `/api/demo/reports/*`. QueryDSL APT для `QDemoOrder` настроен в `pom.xml`. Полная инструкция — [`README_SHOWCASE_RU.md`](../README_SHOWCASE_RU.md).
