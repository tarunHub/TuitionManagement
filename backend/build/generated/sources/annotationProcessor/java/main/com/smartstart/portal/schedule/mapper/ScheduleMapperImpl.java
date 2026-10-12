package com.smartstart.portal.schedule.mapper;

import com.smartstart.portal.schedule.dto.ScheduleDtos;
import com.smartstart.portal.schedule.entity.Term;
import java.time.Instant;
import java.time.LocalDate;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-10-10T22:43:57-0400",
    comments = "version: 1.6.3, compiler: IncrementalProcessingEnvironment from gradle-language-java-9.0.0.jar, environment: Java 25.0.2 (Oracle Corporation)"
)
@Component
public class ScheduleMapperImpl implements ScheduleMapper {

    @Override
    public ScheduleDtos.TermResponse toResponse(Term term) {
        if ( term == null ) {
            return null;
        }

        Long id = null;
        String name = null;
        LocalDate startDate = null;
        LocalDate endDate = null;
        String calendarName = null;
        boolean active = false;
        Instant createdAt = null;
        Instant updatedAt = null;

        id = term.getId();
        name = term.getName();
        startDate = term.getStartDate();
        endDate = term.getEndDate();
        calendarName = term.getCalendarName();
        active = term.isActive();
        createdAt = term.getCreatedAt();
        updatedAt = term.getUpdatedAt();

        ScheduleDtos.TermResponse termResponse = new ScheduleDtos.TermResponse( id, name, startDate, endDate, calendarName, active, createdAt, updatedAt );

        return termResponse;
    }
}
