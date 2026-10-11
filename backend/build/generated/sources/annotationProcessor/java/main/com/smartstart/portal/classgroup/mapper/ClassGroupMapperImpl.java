package com.smartstart.portal.classgroup.mapper;

import com.smartstart.portal.classgroup.dto.ClassGroupDtos;
import com.smartstart.portal.classgroup.entity.ClassGroup;
import java.time.Instant;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-10-05T23:36:52-0400",
    comments = "version: 1.6.3, compiler: IncrementalProcessingEnvironment from gradle-language-java-9.0.0.jar, environment: Java 25.0.2 (Oracle Corporation)"
)
@Component
public class ClassGroupMapperImpl implements ClassGroupMapper {

    @Override
    public ClassGroupDtos.ClassGroupResponse toResponse(ClassGroup entity, long activeStudentCount) {
        if ( entity == null ) {
            return null;
        }

        Long id = null;
        String name = null;
        String description = null;
        Instant createdAt = null;
        Instant updatedAt = null;
        if ( entity != null ) {
            id = entity.getId();
            name = entity.getName();
            description = entity.getDescription();
            createdAt = entity.getCreatedAt();
            updatedAt = entity.getUpdatedAt();
        }
        long activeStudentCount1 = 0L;
        activeStudentCount1 = activeStudentCount;

        ClassGroupDtos.ClassGroupResponse classGroupResponse = new ClassGroupDtos.ClassGroupResponse( id, name, description, activeStudentCount1, createdAt, updatedAt );

        return classGroupResponse;
    }

    @Override
    public ClassGroup toEntity(ClassGroupDtos.CreateClassGroupRequest request) {
        if ( request == null ) {
            return null;
        }

        ClassGroup classGroup = new ClassGroup();

        classGroup.setName( request.name() );
        classGroup.setDescription( request.description() );

        return classGroup;
    }

    @Override
    public void update(ClassGroupDtos.UpdateClassGroupRequest request, ClassGroup entity) {
        if ( request == null ) {
            return;
        }

        entity.setName( request.name() );
        entity.setDescription( request.description() );
    }
}
