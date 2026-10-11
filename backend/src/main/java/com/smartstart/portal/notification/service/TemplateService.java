package com.smartstart.portal.notification.service;

import com.smartstart.portal.common.exception.ConflictException;
import com.smartstart.portal.common.exception.NotFoundException;
import com.smartstart.portal.notification.dto.NotificationDtos.CreateTemplateRequest;
import com.smartstart.portal.notification.dto.NotificationDtos.TemplateResponse;
import com.smartstart.portal.notification.dto.NotificationDtos.UpdateTemplateRequest;
import com.smartstart.portal.notification.entity.NotificationTemplate;
import com.smartstart.portal.notification.mapper.NotificationMapper;
import com.smartstart.portal.notification.repository.NotificationTemplateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TemplateService {

    private final NotificationTemplateRepository repository;
    private final NotificationMapper mapper;

    @Transactional(readOnly = true)
    public List<TemplateResponse> list() {
        return repository.findAll(Sort.by("name")).stream().map(mapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public TemplateResponse get(Long id) {
        return mapper.toResponse(require(id));
    }

    @Transactional
    public TemplateResponse create(CreateTemplateRequest request) {
        String name = request.name().trim();
        if (repository.existsByNameIgnoreCase(name)) {
            throw new ConflictException("DUPLICATE_TEMPLATE", "A template named '" + name + "' already exists");
        }
        NotificationTemplate t = mapper.toEntity(request);
        t.setName(name);
        return mapper.toResponse(repository.save(t));
    }

    @Transactional
    public TemplateResponse update(Long id, UpdateTemplateRequest request) {
        NotificationTemplate t = require(id);
        String name = request.name().trim();
        if (!t.getName().equalsIgnoreCase(name) && repository.existsByNameIgnoreCase(name)) {
            throw new ConflictException("DUPLICATE_TEMPLATE", "A template named '" + name + "' already exists");
        }
        mapper.update(request, t);
        t.setName(name);
        return mapper.toResponse(t);
    }

    @Transactional
    public void delete(Long id) {
        repository.delete(require(id));
    }

    private NotificationTemplate require(Long id) {
        return repository.findById(id).orElseThrow(() -> new NotFoundException("Template", id));
    }
}
