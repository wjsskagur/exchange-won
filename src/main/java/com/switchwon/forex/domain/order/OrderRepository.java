package com.switchwon.forex.domain.order;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Arrays;
import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {

    /**
     * 전체 주문 내역을 최신순으로 조회
     * 주문 목록 API는 페이징 없이 전체 반환 (과제 스펙 기준)
     */

    List<Order> findAllByOrderByCreatedAt();
}
