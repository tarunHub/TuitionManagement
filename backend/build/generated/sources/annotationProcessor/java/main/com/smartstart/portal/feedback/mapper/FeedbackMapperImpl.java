package com.smartstart.portal.feedback.mapper;

import com.smartstart.portal.feedback.dto.FeedbackDtos;
import com.smartstart.portal.feedback.entity.MonthlyFeedback;
import com.smartstart.portal.student.entity.Student;
import java.time.Instant;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-10-10T22:43:57-0400",
    comments = "version: 1.6.3, compiler: IncrementalProcessingEnvironment from gradle-language-java-9.0.0.jar, environment: Java 25.0.2 (Oracle Corporation)"
)
@Component
public class FeedbackMapperImpl implements FeedbackMapper {

    @Override
    public FeedbackDtos.FeedbackResponse toResponse(MonthlyFeedback entity) {
        if ( entity == null ) {
            return null;
        }

        String month = null;
        Long studentId = null;
        Long id = null;
        String topicsTaught = null;
        String improvementAreas = null;
        String achievements = null;
        Instant createdAt = null;
        Instant updatedAt = null;

        month = toMonth( entity.getFeedbackMonth() );
        studentId = entityStudentId( entity );
        id = entity.getId();
        topicsTaught = entity.getTopicsTaught();
        improvementAreas = entity.getImprovementAreas();
        achievements = entity.getAchievements();
        createdAt = entity.getCreatedAt();
        updatedAt = entity.getUpdatedAt();

        FeedbackDtos.FeedbackResponse feedbackResponse = new FeedbackDtos.FeedbackResponse( id, month, studentId, topicsTaught, improvementAreas, achievements, createdAt, updatedAt );

        return feedbackResponse;
    }

    private Long entityStudentId(MonthlyFeedback monthlyFeedback) {
        Student student = monthlyFeedback.getStudent();
        if ( student == null ) {
            return null;
        }
        return student.getId();
    }
}
