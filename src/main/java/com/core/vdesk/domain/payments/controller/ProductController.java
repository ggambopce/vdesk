package com.core.vdesk.domain.payments.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.core.vdesk.domain.payments.dto.ProductListResponseDto;
import com.core.vdesk.domain.payments.dto.PurchaseOptionsResponseDto;
import com.core.vdesk.domain.payments.service.ProductService;
import com.core.vdesk.global.response.ApiResponse;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @GetMapping("/list")
    public ResponseEntity<ApiResponse<List<ProductListResponseDto>>> productsAll() {
        List<ProductListResponseDto> result = productService.getAllProducts();
        return ResponseEntity.ok(ApiResponse.ok("요금제 목록 조회 성공", result));
    }

    @GetMapping("/purchase-options")
    public ResponseEntity<ApiResponse<PurchaseOptionsResponseDto>> purchaseOptions() {
        PurchaseOptionsResponseDto result = productService.getPurchaseOptions();
        return ResponseEntity.ok(ApiResponse.ok("구매 옵션 조회 성공", result));
    }
}
