package com.atomskills.mainbackend.repository;

import com.atomskills.mainbackend.entity.DemoOrder;
import org.springframework.data.jpa.repository.JpaRepository;

/** Репозиторий демо-заказов. */
public interface DemoOrderRepository extends JpaRepository<DemoOrder, Long> {
}
