package dev.mihab.tenantauth.tenant;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.stream.Collectors;

/**
 * Maps an authenticated request to its tenant id, based on the token's issuer.
 * The tenant is never taken from a header or path parameter, so a caller cannot
 * switch tenants by editing the request.
 */
@Component
public class TenantResolver {

    private final Map<String, String> tenantIdByIssuer;

    public TenantResolver(TenantProperties properties) {
        this.tenantIdByIssuer = properties.tenants().stream()
                .collect(Collectors.toUnmodifiableMap(TenantProperties.Tenant::issuer, TenantProperties.Tenant::id));
    }

    public String tenantOf(Authentication authentication) {
        if (!(authentication instanceof JwtAuthenticationToken jwtAuth)) {
            throw new UnknownTenantException("Request is not authenticated with a JWT");
        }
        String issuer = jwtAuth.getToken().getClaimAsString("iss");
        String tenantId = tenantIdByIssuer.get(issuer);
        if (tenantId == null) {
            throw new UnknownTenantException("Untrusted issuer: " + issuer);
        }
        return tenantId;
    }
}
