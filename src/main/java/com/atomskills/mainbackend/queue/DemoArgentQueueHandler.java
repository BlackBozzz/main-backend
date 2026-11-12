package com.atomskills.mainbackend.queue;

import com.argent.servicecommon.queue.service.ArgentQueueHandler;
import com.argent.servicecommon.queue.service.ArgentQueueService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.UUID;

/** Реальные обработчики ArgentQueue, демонстрационные задания НЕ изменяют бизнес-данные. */
@Service
public class DemoArgentQueueHandler {
    public static final String QUEUE = "demo-jobs";
    public static final String FAKE_HANDLER = "demo.fake-task";
    private static final Logger log = LoggerFactory.getLogger(DemoArgentQueueHandler.class);
    private final ArgentQueueService argentQueueService;

    public DemoArgentQueueHandler(ArgentQueueService argentQueueService) {
        this.argentQueueService = argentQueueService;
    }

    /** Существующий пример реального обработчика заказа оставлен для обратной совместимости. */
    public UUID enqueueExample(long orderId) {
        return argentQueueService.createQueueTask(
                "Обработка демо-заказов", "demo.order-check", new CheckOrder(orderId), 5,
                "demo-order-check:" + orderId);
    }

    @ArgentQueueHandler("demo.order-check")
    public void checkOrder(CheckOrder task) {
        log.info("Argent Queue: проверяем демо-заказ {}", task.orderId());
    }

    @ArgentQueueHandler(FAKE_HANDLER)
    public void processFakeTask(FakeTask task) {
        // Намеренно без Thread.sleep и без побочных эффектов.
        log.info("Argent Queue: выполнена фиктивная задача №{} ({})", task.number(), task.batchId());
    }

    public record CheckOrder(long orderId) { }
    public record FakeTask(String batchId, int number) { }
}
