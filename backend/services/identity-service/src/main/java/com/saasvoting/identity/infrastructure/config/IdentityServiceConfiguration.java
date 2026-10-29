package com.saasvoting.identity.infrastructure.config;

import com.saasvoting.identity.application.service.CreateTenantService;
import com.saasvoting.identity.application.port.in.CreateTenantUseCase;
import com.saasvoting.identity.application.port.out.TenantRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class IdentityServiceConfiguration {

    @Bean
    Clock systemClock() {
        return Clock.systemUTC();
    }

    @Bean
    CreateTenantUseCase createTenantUseCase(TenantRepository tenantRepository, Clock clock) {
        return new CreateTenantService(tenantRepository, clock);
    }
}
