package com.smartstart.portal.graderate.mapper;

import com.smartstart.portal.graderate.dto.GradeRateDtos.CreateGradeRateRequest;
import com.smartstart.portal.graderate.dto.GradeRateDtos.GradeRateResponse;
import com.smartstart.portal.graderate.dto.GradeRateDtos.UpdateGradeRateRequest;
import com.smartstart.portal.graderate.entity.GradeRate;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper
public interface GradeRateMapper {

    GradeRateResponse toResponse(GradeRate entity, long studentCount);

    GradeRate toEntity(CreateGradeRateRequest request);

    void update(UpdateGradeRateRequest request, @MappingTarget GradeRate entity);
}
