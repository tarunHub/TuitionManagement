package com.smartstart.portal.classgroup.service;

import com.smartstart.portal.classgroup.dto.ClassGroupDtos.ClassGroupResponse;
import com.smartstart.portal.classgroup.dto.ClassGroupDtos.CreateClassGroupRequest;
import com.smartstart.portal.classgroup.dto.ClassGroupDtos.UpdateClassGroupRequest;
import com.smartstart.portal.classgroup.entity.ClassGroup;
import com.smartstart.portal.classgroup.mapper.ClassGroupMapper;
import com.smartstart.portal.classgroup.repository.ClassGroupRepository;
import com.smartstart.portal.common.exception.BadRequestException;
import com.smartstart.portal.common.exception.ConflictException;
import com.smartstart.portal.common.exception.NotFoundException;
import com.smartstart.portal.schedule.repository.ScheduleSlotRepository;
import com.smartstart.portal.student.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ClassGroupService {

    private final ClassGroupRepository repository;
    private final StudentRepository studentRepository;
    private final ScheduleSlotRepository slotRepository;
    private final ClassGroupMapper mapper;

    @Transactional(readOnly = true)
    public List<ClassGroupResponse> list() {
        Map<Long, Long> counts = studentRepository.countActiveByGroup();
        return repository.findAll(Sort.by("name")).stream()
                .map(g -> mapper.toResponse(g, counts.getOrDefault(g.getId(), 0L)))
                .toList();
    }

    @Transactional
    public ClassGroupResponse create(CreateClassGroupRequest request) {
        String name = request.name().trim();
        if (repository.existsByNameIgnoreCase(name)) {
            throw new ConflictException("DUPLICATE_GROUP", "Class group '" + name + "' already exists");
        }
        ClassGroup entity = mapper.toEntity(request);
        entity.setName(name);
        return mapper.toResponse(repository.save(entity), 0);
    }

    @Transactional
    public ClassGroupResponse update(Long id, UpdateClassGroupRequest request) {
        ClassGroup entity = require(id);
        String name = request.name().trim();
        if (!entity.getName().equalsIgnoreCase(name) && repository.existsByNameIgnoreCase(name)) {
            throw new ConflictException("DUPLICATE_GROUP", "Class group '" + name + "' already exists");
        }
        mapper.update(request, entity);
        entity.setName(name);
        return mapper.toResponse(entity, studentRepository.countActiveByGroup().getOrDefault(id, 0L));
    }

    @Transactional
    public void delete(Long id) {
        ClassGroup entity = require(id);
        long students = studentRepository.countByClassGroupId(id);
        long slots = slotRepository.countByClassGroupId(id);
        if (students > 0 || slots > 0) {
            throw new ConflictException("GROUP_IN_USE", "Class group '" + entity.getName() + "' is used by "
                    + students + " student(s) and " + slots + " schedule slot(s) and cannot be deleted");
        }
        repository.delete(entity);
    }

    @Transactional(readOnly = true)
    public ClassGroup require(Long id) {
        return repository.findById(id).orElseThrow(() -> new NotFoundException("Class group", id));
    }

    /** Resolves an optional group reference from a request body, reporting a field error when it is unknown. */
    @Transactional(readOnly = true)
    public ClassGroup resolveOptional(Long id, String field) {
        if (id == null) {
            return null;
        }
        return repository.findById(id)
                .orElseThrow(() -> new BadRequestException("VALIDATION_FAILED", "Class group " + id + " does not exist", field));
    }
}
