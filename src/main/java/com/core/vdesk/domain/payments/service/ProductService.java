package com.core.vdesk.domain.payments.service;

import java.util.Arrays;
import java.util.List;

import org.springframework.stereotype.Service;

import com.core.vdesk.domain.payments.dto.ProductListResponseDto;
import com.core.vdesk.domain.payments.dto.PurchaseOptionsResponseDto;
import com.core.vdesk.domain.payments.entity.Product;
import com.core.vdesk.domain.payments.repository.ProductRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProductService {
    private final ProductRepository productRepository;

    public List<ProductListResponseDto> getAllProducts() {
        List<Product> products = productRepository.findAll();
        return products.stream()
                .map(ProductListResponseDto::from)
                .toList();
    }

    public PurchaseOptionsResponseDto getPurchaseOptions() {
        return PurchaseOptionsResponseDto.builder()
                .paymentTypes(List.of(
                        new PurchaseOptionsResponseDto.PaymentTypeDto("GENERAL", "일반결제"),
                        new PurchaseOptionsResponseDto.PaymentTypeDto("BILLING", "정기결제")
                ))
                .availableDurations(Arrays.asList(1, 3, 6, 9, 12))
                .maxQuantity(26)
                .defaultQuantity(1)
                .defaultDurationMonths(1)
                .build();
    }
}
