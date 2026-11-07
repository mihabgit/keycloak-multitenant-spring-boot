package dev.mihab.tenantauth.project;

import dev.mihab.tenantauth.tenant.TenantResolver;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/projects")
public class ProjectController {

    private final ProjectStore store;
    private final TenantResolver tenantResolver;

    public ProjectController(ProjectStore store, TenantResolver tenantResolver) {
        this.store = store;
        this.tenantResolver = tenantResolver;
    }

    /** Any authenticated user sees only their own tenant's projects. */
    @GetMapping
    public List<Project> list(Authentication auth) {
        return store.findAll(tenantResolver.tenantOf(auth));
    }

    /** Looking up another tenant's project id returns 404, not 403, so ids don't leak. */
    @GetMapping("/{id}")
    public ResponseEntity<Project> get(@PathVariable UUID id, Authentication auth) {
        return store.findById(tenantResolver.tenantOf(auth), id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /** Admins only (enforced in SecurityConfig). */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Project create(@Valid @RequestBody CreateProjectRequest request, Authentication auth) {
        return store.create(tenantResolver.tenantOf(auth), request.name(), auth.getName());
    }

    /** Admins only (enforced in SecurityConfig). */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id, Authentication auth) {
        return store.delete(tenantResolver.tenantOf(auth), id)
                ? ResponseEntity.noContent().build()
                : ResponseEntity.notFound().build();
    }

    public record CreateProjectRequest(@NotBlank @Size(max = 100) String name) {
    }
}
