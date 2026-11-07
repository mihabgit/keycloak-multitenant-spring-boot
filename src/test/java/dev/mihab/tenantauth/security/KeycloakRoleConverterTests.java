package dev.mihab.tenantauth.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class KeycloakRoleConverterTests {

    private final KeycloakRoleConverter converter = new KeycloakRoleConverter();

    @Test
    void mapsRealmRolesToSpringRoles() {
        Jwt jwt = token(Map.of("realm_access", Map.of("roles", List.of("user", "admin"))));

        assertThat(converter.convert(jwt))
                .extracting(GrantedAuthority::getAuthority)
                .containsExactlyInAnyOrder("ROLE_user", "ROLE_admin");
    }

    @Test
    void returnsNoRolesWhenClaimIsMissing() {
        Jwt jwt = token(Map.of("scope", "openid"));

        assertThat(converter.convert(jwt)).isEmpty();
    }

    private static Jwt token(Map<String, Object> claims) {
        Jwt.Builder builder = Jwt.withTokenValue("token").header("alg", "none").subject("someone");
        claims.forEach(builder::claim);
        return builder.build();
    }
}
