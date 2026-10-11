package com.smartstart.portal;

import com.smartstart.portal.common.util.AppTime;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

import java.util.TimeZone;

@SpringBootApplication
@ConfigurationPropertiesScan
public class PortalApplication {

    public static void main(String[] args) {
        // Business rule 10: all times are America/Toronto.
        TimeZone.setDefault(TimeZone.getTimeZone(AppTime.ZONE));
        SpringApplication.run(PortalApplication.class, args);
    }
}
