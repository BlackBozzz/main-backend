package com.atomskills.mainbackend.controller;

import com.argent.servicecommon.queue.service.ArgentQueueService;
import com.atomskills.mainbackend.queue.DemoArgentQueueHandler;
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
public class DemoQueueController {
    private final ArgentQueueService queue;

    public DemoQueueController(ArgentQueueService queue) {
        this.queue = queue;
    }

    public record EnqueueRequest(@Min(1) @Max(100) int count) { }
    public record EnqueueResponse(int queued, String queueName, List<UUID> taskIds) { }

    @PostMapping
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
