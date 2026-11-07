package dev.mihab.tenantauth.project;

import java.time.Instant;
import java.util.UUID;

public record Project(UUID id, String tenantId, String name, String createdBy, Instant createdAt) {
}
