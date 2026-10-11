package com.smartstart.portal.schedule.mapper;

import com.smartstart.portal.schedule.dto.ScheduleDtos.TermResponse;
import com.smartstart.portal.schedule.entity.Term;
import org.mapstruct.Mapper;

@Mapper
public interface ScheduleMapper {

    TermResponse toResponse(Term term);
}
