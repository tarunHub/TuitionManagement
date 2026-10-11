package com.smartstart.portal.classgroup.mapper;

import com.smartstart.portal.classgroup.dto.ClassGroupDtos.ClassGroupResponse;
import com.smartstart.portal.classgroup.dto.ClassGroupDtos.CreateClassGroupRequest;
import com.smartstart.portal.classgroup.dto.ClassGroupDtos.UpdateClassGroupRequest;
import com.smartstart.portal.classgroup.entity.ClassGroup;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper
public interface ClassGroupMapper {

    ClassGroupResponse toResponse(ClassGroup entity, long activeStudentCount);

    ClassGroup toEntity(CreateClassGroupRequest request);

    void update(UpdateClassGroupRequest request, @MappingTarget ClassGroup entity);
}
