package com.atomskills.mainbackend.controller;

import com.argent.servicecommon.querydsl.ArgentQuerydsl;
import com.atomskills.mainbackend.dto.DemoOrderQueryRow;
import com.atomskills.mainbackend.entity.QDemoOrder;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.querydsl.core.types.Expression;
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
public class DemoQuerydslController {
    private final JPAQueryFactory factory;

    public DemoQuerydslController(JPAQueryFactory factory) {
        this.factory = factory;
    }

    @GetMapping("/orders")
    @PreAuthorize("@argentAccess.table('dict_demo_order', 'read')")
    public Page<DemoOrderQueryRow> orders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sort,
            @RequestParam(defaultValue = "desc") String direction,
            @RequestParam(required = false) String customer) {
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
