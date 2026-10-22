package com.saasvoting.tenant.configuration;

import com.saasvoting.tenant.application.service.CreateTenantService;
import com.saasvoting.tenant.domain.port.in.CreateTenantUseCase;
import com.saasvoting.tenant.domain.port.out.TenantRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class TenantServiceConfiguration {

    @Bean
    Clock systemClock() {
        return Clock.systemUTC();
    }

    @Bean
    CreateTenantUseCase createTenantUseCase(TenantRepository tenantRepository, Clock clock) {
        return new CreateTenantService(tenantRepository, clock);
    }
}
