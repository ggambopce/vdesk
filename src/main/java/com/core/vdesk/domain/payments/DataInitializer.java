package com.core.vdesk.domain.payments;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.core.vdesk.domain.payments.entity.Product;
import com.core.vdesk.domain.payments.enums.ProductCode;
import com.core.vdesk.domain.payments.repository.ProductRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements ApplicationRunner {

    private final ProductRepository productRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        seedProduct(ProductCode.BASIC_PLAN, "Basic", "일반 사무용", 45000L,
                "Intel Xeon", "3GB DDR4", "256GB", "GPU Share", "Windows 10 Pro", "한국 공인 IP");
        seedProduct(ProductCode.STANDARD_PLAN, "Standard", "중간 사양", 85000L,
                "Intel Core i5-8500", "8GB DDR4", "256GB", "Intel UHD 630", "Windows 10 Pro", "한국 공인 IP");
        seedProduct(ProductCode.BUSINESS_PLAN, "Business", "최고 사양", 140000L,
                "Intel Xeon 12 Core", "64GB DDR4", "256GB", "GeForce 1050Ti", "Windows 10 Pro", "한국 공인 IP");
        log.info("[DataInitializer] 상품 시드 완료");
    }

    private void seedProduct(ProductCode code, String name, String description, Long monthlyAmount,
                             String cpu, String ram, String ssd, String gpu, String os, String publicIp) {
        productRepository.findByProductCode(code).ifPresentOrElse(
                p -> {
                    p.setName(name);
                    p.setDescription(description);
                    p.setMonthlyAmount(monthlyAmount);
                    p.setCpu(cpu);
                    p.setRam(ram);
                    p.setSsd(ssd);
                    p.setGpu(gpu);
                    p.setOs(os);
                    p.setPublicIp(publicIp);
                    productRepository.save(p);
                },
                () -> {
                    Product p = new Product();
                    p.setProductCode(code);
                    p.setName(name);
                    p.setDescription(description);
                    p.setMonthlyAmount(monthlyAmount);
                    p.setCurrency("KRW");
                    p.setCpu(cpu);
                    p.setRam(ram);
                    p.setSsd(ssd);
                    p.setGpu(gpu);
                    p.setOs(os);
                    p.setPublicIp(publicIp);
                    productRepository.save(p);
                }
        );
    }
}
