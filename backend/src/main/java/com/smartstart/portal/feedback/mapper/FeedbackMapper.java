package com.smartstart.portal.feedback.mapper;

import com.smartstart.portal.feedback.dto.FeedbackDtos.FeedbackResponse;
import com.smartstart.portal.feedback.entity.MonthlyFeedback;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.time.LocalDate;
import java.time.YearMonth;

@Mapper
public interface FeedbackMapper {

    @Mapping(target = "month", source = "feedbackMonth")
    @Mapping(target = "studentId", source = "student.id")
    FeedbackResponse toResponse(MonthlyFeedback entity);

    default String toMonth(LocalDate date) {
        return date == null ? null : YearMonth.from(date).toString();
    }
}
