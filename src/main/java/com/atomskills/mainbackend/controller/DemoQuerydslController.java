package com.atomskills.mainbackend.controller;

import com.argent.servicecommon.querydsl.ArgentQuerydsl;
import com.atomskills.mainbackend.dto.DemoOrderQueryRow;
import com.atomskills.mainbackend.entity.QDemoOrder;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.querydsl.core.types.Expression;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/** Живой пример: QueryDSL + Argent selectDto() + автоматический COUNT + безопасная сортировка. */
@RestController
@RequestMapping("/api/demo/querydsl")
@Tag(name = "QueryDSL: демонстрация", description = "Пример типизированной выборки DTO с автоматической пагинацией")
public class DemoQuerydslController {
    private final JPAQueryFactory factory;

    public DemoQuerydslController(JPAQueryFactory factory) {
        this.factory = factory;
    }

    @Operation(summary = "Заказы с QueryDSL", description = """
            Выборка с selectDto, вложенным полем details.customer и пагинацией с автоматическим COUNT.
            Доступ регулируется грантом на чтение таблицы dict_demo_order (это не action).
            """)
    @GetMapping("/orders")
    @PreAuthorize("@argentAccess.table('dict_demo_order', 'read')")
    public Page<DemoOrderQueryRow> orders(
            @Parameter(description = "Номер страницы, начиная с нуля") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Количество строк на странице (1–100)") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "Имя поля сортировки") @RequestParam(defaultValue = "id") String sort,
            @Parameter(description = "asc или desc") @RequestParam(defaultValue = "desc") String direction,
            @Parameter(description = "Подстрока имени заказчика") @RequestParam(required = false) String customer) {
        if (page < 0 || size < 1 || size > 100) {
            throw new IllegalArgumentException("Страница >= 0, размер 1–100");
        }
        var qOrder = QDemoOrder.demoOrder;
        Map<String, Expression<? extends Comparable<?>>> sortableFields = Map.of(
                "id", qOrder.id,
                "number", qOrder.number,
                "customer", qOrder.customer,
                "total", qOrder.total,
                "createdAt", qOrder.createdAt);
        if (!sortableFields.containsKey(sort)) {
            throw new IllegalArgumentException("Недопустимая сортировка: " + sort);
        }
        Sort.Direction sortDirection = Sort.Direction.fromOptionalString(direction)
                .orElse(Sort.Direction.DESC);
        var pageable = PageRequest.of(page, size, Sort.by(sortDirection, sort));
        var query = factory
                .select(ArgentQuerydsl.selectDto(DemoOrderQueryRow.class,
                        qOrder.id,
                        qOrder.number,
                        qOrder.status,
                        qOrder.total,
                        qOrder.createdAt,
                        qOrder.customer.as("details$customer")))
                .from(qOrder);
        if (customer != null && !customer.isBlank()) {
            query.where(qOrder.customer.containsIgnoreCase(customer.trim()));
        }
        // Тот самый метод без отдельного countQuery.
        return ArgentQuerydsl.paginate(query, pageable, sortableFields);
    }
}
