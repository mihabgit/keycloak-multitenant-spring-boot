package dev.mihab.tenantauth.tenant;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.List;

/**
 * Tenants this API trusts. Each tenant is a Keycloak realm.
 *
 * <p>{@code issuer} is the public issuer URL that appears in the token's {@code iss} claim.
 * {@code jwkSetUri} is where the API fetches signing keys. They are separate on purpose:
 * inside Docker the API reaches Keycloak at {@code http://keycloak:8080}, while tokens are
 * issued for {@code http://localhost:8080}.
 */
@Validated
@ConfigurationProperties(prefix = "app")
public record TenantProperties(@NotEmpty List<@Valid Tenant> tenants) {

    public record Tenant(@NotBlank String id,
                         @NotBlank String issuer,
                         @NotBlank String jwkSetUri) {
    }
}
