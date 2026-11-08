-- Демо-данные статусной машины заказов и счётчика номеров.
-- Таблицы common_* создаёт миграция библиотеки V4 (история common_schema_history),
-- она выполняется раньше миграций приложения.
-- ON CONFLICT DO NOTHING: повторный запуск на базе, где строки уже есть, ничего не ломает.

-- Тип статуса
INSERT INTO common_status_type (id, code, name)
VALUES (gen_random_uuid(), 'ORDER_STATUS', 'Статусы заказов')
ON CONFLICT DO NOTHING;

-- Статусы: NEW (начальный) → REVIEW → DONE (конечный)
INSERT INTO common_status (id, status_type_id, code, name, sort_order, is_initial, is_final)
SELECT gen_random_uuid(), t.id, s.code, s.name, s.sort_order, s.is_initial, s.is_final
FROM common_status_type t
CROSS JOIN (VALUES ('NEW', 'Новый', 1, TRUE, FALSE),
                   ('REVIEW', 'На проверке', 2, FALSE, FALSE),
                   ('DONE', 'Выполнен', 3, FALSE, TRUE))
           AS s (code, name, sort_order, is_initial, is_final)
WHERE t.code = 'ORDER_STATUS'
ON CONFLICT DO NOTHING;

-- Переходы: создание → NEW, NEW → REVIEW, REVIEW → DONE (только с действием orders.approve)
INSERT INTO common_status_transition (id, status_type_id, from_status_id, to_status_id, required_action)
SELECT gen_random_uuid(), t.id, f.id, s.id, tr.required_action
FROM common_status_type t
CROSS JOIN (VALUES (NULL, 'NEW', NULL),
                   ('NEW', 'REVIEW', NULL),
                   ('REVIEW', 'DONE', 'orders.approve'))
           AS tr (from_code, to_code, required_action)
JOIN common_status s ON s.status_type_id = t.id AND s.code = tr.to_code
LEFT JOIN common_status f ON f.status_type_id = t.id AND f.code = tr.from_code
WHERE t.code = 'ORDER_STATUS'
ON CONFLICT DO NOTHING;

-- Счётчик номеров заказов: ORD-2026-11-00001, сброс каждый месяц
INSERT INTO common_sequence (id, code, pattern, reset_period)
VALUES (gen_random_uuid(), 'ORDER', 'ORD-%1$s-%2$05d', 'MONTHLY')
ON CONFLICT DO NOTHING;
