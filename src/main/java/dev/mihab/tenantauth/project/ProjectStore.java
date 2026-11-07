package dev.mihab.tenantauth.project;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory storage, partitioned by tenant. Every method takes the tenant id, so
 * one tenant's data can never be read or changed through another tenant's request.
 * Swap this for a JPA repository with a tenant_id column (or schema-per-tenant) in a real app.
 */
@Component
public class ProjectStore {

    private final Map<String, Map<UUID, Project>> projectsByTenant = new ConcurrentHashMap<>();

    public ProjectStore() {
        seed("acme", "Customer portal", "system");
        seed("acme", "Billing migration", "system");
        seed("globex", "Warehouse tracking", "system");
    }

    public List<Project> findAll(String tenantId) {
        return projectsByTenant.getOrDefault(tenantId, Map.of()).values().stream()
                .sorted(Comparator.comparing(Project::createdAt))
                .toList();
    }

    public Optional<Project> findById(String tenantId, UUID id) {
        return Optional.ofNullable(projectsByTenant.getOrDefault(tenantId, Map.of()).get(id));
    }

    public Project create(String tenantId, String name, String createdBy) {
        Project project = new Project(UUID.randomUUID(), tenantId, name, createdBy, Instant.now());
        projectsByTenant.computeIfAbsent(tenantId, t -> new ConcurrentHashMap<>()).put(project.id(), project);
        return project;
    }

    public boolean delete(String tenantId, UUID id) {
        Map<UUID, Project> projects = projectsByTenant.get(tenantId);
        return projects != null && projects.remove(id) != null;
    }

    private void seed(String tenantId, String name, String createdBy) {
        create(tenantId, name, createdBy);
    }
}
