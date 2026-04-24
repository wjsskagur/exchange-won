package com.switchwon.forex.presentation.order;

import com.switchwon.forex.application.order.OrderService;
import com.switchwon.forex.application.order.dto.OrderDto;
import com.switchwon.forex.presentation.common.ApiResponse;
import com.switchwon.forex.presentation.order.dto.OrderRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/order")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    public ResponseEntity<ApiResponse<OrderDto>> placeOrder(
            @RequestBody @Valid OrderRequest request) {
        OrderDto result = orderService.placeOrder(
            request.forexAmount(), request.fromCurrency(), request.toCurrency());
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/list")
    public ResponseEntity<ApiResponse<Map<String, List<OrderDto>>>> getOrderList() {
        List<OrderDto> orders = orderService.getOrderList();
        return ResponseEntity.ok(ApiResponse.success(Map.of("orderList", orders)));
    }
}
