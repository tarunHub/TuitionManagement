package com.smartstart.portal.graderate.mapper;

import com.smartstart.portal.graderate.dto.GradeRateDtos;
import com.smartstart.portal.graderate.entity.GradeRate;
import java.math.BigDecimal;
import java.time.Instant;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-10-10T22:43:57-0400",
    comments = "version: 1.6.3, compiler: IncrementalProcessingEnvironment from gradle-language-java-9.0.0.jar, environment: Java 25.0.2 (Oracle Corporation)"
)
@Component
public class GradeRateMapperImpl implements GradeRateMapper {

    @Override
    public GradeRateDtos.GradeRateResponse toResponse(GradeRate entity, long studentCount) {
        if ( entity == null ) {
            return null;
        }

        Long id = null;
        String grade = null;
        BigDecimal perClassRate = null;
        String notes = null;
        Instant createdAt = null;
        Instant updatedAt = null;
        if ( entity != null ) {
            id = entity.getId();
            grade = entity.getGrade();
            perClassRate = entity.getPerClassRate();
            notes = entity.getNotes();
            createdAt = entity.getCreatedAt();
            updatedAt = entity.getUpdatedAt();
        }
        long studentCount1 = 0L;
        studentCount1 = studentCount;

        GradeRateDtos.GradeRateResponse gradeRateResponse = new GradeRateDtos.GradeRateResponse( id, grade, perClassRate, notes, studentCount1, createdAt, updatedAt );

        return gradeRateResponse;
    }

    @Override
    public GradeRate toEntity(GradeRateDtos.CreateGradeRateRequest request) {
        if ( request == null ) {
            return null;
        }

        GradeRate gradeRate = new GradeRate();

        gradeRate.setGrade( request.grade() );
        gradeRate.setPerClassRate( request.perClassRate() );
        gradeRate.setNotes( request.notes() );

        return gradeRate;
    }

    @Override
    public void update(GradeRateDtos.UpdateGradeRateRequest request, GradeRate entity) {
        if ( request == null ) {
            return;
        }

        entity.setGrade( request.grade() );
        entity.setPerClassRate( request.perClassRate() );
        entity.setNotes( request.notes() );
    }
}
