package com.smartstart.portal.student.mapper;

import com.smartstart.portal.student.dto.StudentDtos.CreateStudentRequest;
import com.smartstart.portal.student.dto.StudentDtos.StudentResponse;
import com.smartstart.portal.student.dto.StudentDtos.UpdateStudentRequest;
import com.smartstart.portal.student.entity.Student;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper
public interface StudentMapper {

    @Mapping(target = "classGroupId", source = "classGroup.id")
    @Mapping(target = "classGroupName", source = "classGroup.name")
    @Mapping(target = "hasHistory", ignore = true)
    StudentResponse toResponse(Student student);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "studentCode", ignore = true)
    @Mapping(target = "classGroup", ignore = true)
    @Mapping(target = "subjects", ignore = true)
    @Mapping(target = "perClassRate", ignore = true)
    @Mapping(target = "status", ignore = true)
    Student toEntity(CreateStudentRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "studentCode", ignore = true)
    @Mapping(target = "classGroup", ignore = true)
    @Mapping(target = "subjects", ignore = true)
    void update(UpdateStudentRequest request, @MappingTarget Student student);
}
