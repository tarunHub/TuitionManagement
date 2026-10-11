package com.smartstart.portal.config;

import com.smartstart.portal.common.util.AppTime;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.format.FormatterRegistry;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.time.Clock;
import java.time.YearMonth;

@Configuration
@EnableJpaAuditing
@EnableScheduling
public class CoreConfig implements WebMvcConfigurer {

    @Bean
    public Clock clock() {
        return Clock.system(AppTime.ZONE);
    }

    @Override
    public void addFormatters(FormatterRegistry registry) {
        registry.addConverter(String.class, YearMonth.class, AppTime::parseMonth);
    }
}
