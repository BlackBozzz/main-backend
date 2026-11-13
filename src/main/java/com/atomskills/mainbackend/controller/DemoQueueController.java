package com.atomskills.mainbackend.controller;

import com.argent.servicecommon.queue.service.ArgentQueueService;
import com.atomskills.mainbackend.queue.DemoArgentQueueHandler;
import com.argent.servicecommon.docs.openapi.ArgentApiAction;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Создаёт только безопасные фиктивные задачи. Управление настоящими очередями уже есть в Argent Admin. */
@RestController
@RequestMapping("/api/demo/queues")
@Tag(name = "Очереди: демонстрация", description = "Запуск фиктивных задач в механизме ArgentQueue")
public class DemoQueueController {
    private final ArgentQueueService queue;

    public DemoQueueController(ArgentQueueService queue) {
        this.queue = queue;
    }

    @Schema(title = "Запуск фиктивных задач")
    public record EnqueueRequest(@Schema(title = "Количество задач", description = "От 1 до 100", example = "10") @Min(1) @Max(100) int count) { }
    @Schema(title = "Результат запуска очереди")
    public record EnqueueResponse(
            @Schema(title = "Поставлено задач") int queued,
            @Schema(title = "Название очереди") String queueName,
            @Schema(title = "Идентификаторы задач") List<UUID> taskIds) { }

    @PostMapping
    @Operation(summary = "Добавить демонстрационные задачи", description = """
            Создаёт от 1 до 100 безопасных фиктивных задач в ArgentQueue.
            Задачи можно отслеживать в админке очередей; реальные письма и внешние вызовы не выполняются.
            """)
    @ArgentApiAction("admin")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("@argentAccess.action('admin')")
    public EnqueueResponse enqueue(@Valid @RequestBody EnqueueRequest request) {
        List<UUID> ids = new ArrayList<>(request.count());
        // Уникальные идентификаторы демонстрационных заданий; повторный запуск создаёт новую партию.
        for (int index = 0; index < request.count(); index++) {
            ids.add(queue.createQueueTask(DemoArgentQueueHandler.QUEUE,
                    DemoArgentQueueHandler.FAKE_HANDLER,
                    new DemoArgentQueueHandler.FakeTask(UUID.randomUUID().toString(), index + 1)));
        }
        return new EnqueueResponse(ids.size(), DemoArgentQueueHandler.QUEUE, ids);
    }
}
