package dev.mihab.tenantauth.api;

import dev.mihab.tenantauth.tenant.TenantResolver;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
public class MeController {

    private final TenantResolver tenantResolver;

    public MeController(TenantResolver tenantResolver) {
        this.tenantResolver = tenantResolver;
    }

    @GetMapping("/api/public/ping")
    public Map<String, String> ping() {
        return Map.of("status", "ok");
    }

    /** Shows who the API thinks you are: handy for checking a token end to end. */
    @GetMapping("/api/me")
    public Map<String, Object> me(Authentication auth) {
        List<String> roles = auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();
        return Map.of(
                "username", auth.getName(),
                "tenant", tenantResolver.tenantOf(auth),
                "roles", roles);
    }
}
