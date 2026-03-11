package com.core.vdesk.domain.payments.dto;

import com.core.vdesk.domain.payments.entity.Product;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductListResponseDto {
    private String productCode;
    private String name;
    private String description;
    private Long monthlyAmount;
    private String currency;
    private String cpu;
    private String ram;
    private String ssd;
    private String gpu;
    private String os;
    private String publicIp;

    public static ProductListResponseDto from(Product product) {
        return ProductListResponseDto.builder()
                .productCode(product.getProductCode().name())
                .name(product.getName())
                .description(product.getDescription())
                .monthlyAmount(product.getMonthlyAmount())
                .currency(product.getCurrency())
                .cpu(product.getCpu())
                .ram(product.getRam())
                .ssd(product.getSsd())
                .gpu(product.getGpu())
                .os(product.getOs())
                .publicIp(product.getPublicIp())
                .build();
    }
}
