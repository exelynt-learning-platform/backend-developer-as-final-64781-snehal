package com.assignment.booking.service;

import com.assignment.booking.dto.resource.ResourceRequest;
import com.assignment.booking.dto.resource.ResourceResponse;
import com.assignment.booking.entity.Resource;
import com.assignment.booking.exception.ResourceNotFoundException;
import com.assignment.booking.repository.ResourceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ResourceService {

    private final ResourceRepository resourceRepository;

    @Transactional(readOnly = true)
    public Page<ResourceResponse> findAll(Pageable pageable) {
        return resourceRepository.findAll(pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public ResourceResponse findById(Long id) {
        return toResponse(getEntity(id));
    }

    @Transactional(readOnly = true)
    public Resource getEntity(Long id) {
        return resourceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Resource not found with id: " + id));
    }

    @Transactional
    public ResourceResponse create(ResourceRequest request) {
        Resource resource = Resource.builder()
                .name(request.getName())
                .type(request.getType())
                .description(request.getDescription())
                .available(Boolean.TRUE.equals(request.getAvailable()))
                .build();
        return toResponse(resourceRepository.save(resource));
    }

    @Transactional
    public ResourceResponse update(Long id, ResourceRequest request) {
        Resource resource = getEntity(id);
        resource.setName(request.getName());
        resource.setType(request.getType());
        resource.setDescription(request.getDescription());
        resource.setAvailable(Boolean.TRUE.equals(request.getAvailable()));
        return toResponse(resourceRepository.save(resource));
    }

    @Transactional
    public void delete(Long id) {
        Resource resource = getEntity(id);
        resourceRepository.delete(resource);
    }

    private ResourceResponse toResponse(Resource resource) {
        return ResourceResponse.builder()
                .id(resource.getId())
                .name(resource.getName())
                .type(resource.getType())
                .description(resource.getDescription())
                .available(resource.isAvailable())
                .build();
    }
}
