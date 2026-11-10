package com.atomskills.mainbackend.queue;

import com.argent.servicecommon.queue.service.ArgentQueueHandler;
import com.argent.servicecommon.queue.service.ArgentQueueService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.UUID;

/** Пример использования Argent Queue. Не создаёт задания сам при старте приложения. */
@Service
public class DemoArgentQueueHandler {
    private static final Logger log = LoggerFactory.getLogger(DemoArgentQueueHandler.class);
    private final ArgentQueueService argentQueueService;

    public DemoArgentQueueHandler(ArgentQueueService argentQueueService) {
        this.argentQueueService = argentQueueService;
    }

    /** Пример постановки задачи из бизнес-кода. Возвращает UUID для диагностики. */
    public UUID enqueueExample(long orderId) {
        return argentQueueService.createQueueTask(
                "Обработка демо-заказов", "demo.order-check", new CheckOrder(orderId), 5,
                "demo-order-check:" + orderId);
    }

    @ArgentQueueHandler("demo.order-check")
    public void checkOrder(CheckOrder task) {
        log.info("Argent Queue: проверяем демо-заказ {}", task.orderId());
    }

    public record CheckOrder(long orderId) { }
}
