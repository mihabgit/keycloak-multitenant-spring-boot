package dev.mihab.tenantauth.security;

import dev.mihab.tenantauth.tenant.TenantProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationProvider;
import org.springframework.security.oauth2.server.resource.authentication.JwtIssuerAuthenticationManagerResolver;
import org.springframework.security.web.SecurityFilterChain;

import java.util.Map;
import java.util.stream.Collectors;

/**
 * One API, many Keycloak realms.
 *
 * <p>For every incoming token, Spring reads the (unverified) {@code iss} claim, looks up the
 * matching tenant, and verifies the signature with that tenant's keys only. Tokens from
 * realms that are not configured are rejected before any signature check.
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http,
                                            JwtIssuerAuthenticationManagerResolver tenantAuthResolver) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/public/**", "/actuator/health", "/error").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/projects/**").hasRole("admin")
                        .requestMatchers(HttpMethod.DELETE, "/api/projects/**").hasRole("admin")
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2.authenticationManagerResolver(tenantAuthResolver));
        return http.build();
    }

    @Bean
    JwtIssuerAuthenticationManagerResolver tenantAuthResolver(TenantProperties properties) {
        Map<String, AuthenticationManager> managers = properties.tenants().stream()
                .collect(Collectors.toUnmodifiableMap(TenantProperties.Tenant::issuer, this::authenticationManagerFor));
        return new JwtIssuerAuthenticationManagerResolver(managers::get);
    }

    private AuthenticationManager authenticationManagerFor(TenantProperties.Tenant tenant) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(tenant.jwkSetUri()).build();
        decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(tenant.issuer()));

        JwtAuthenticationProvider provider = new JwtAuthenticationProvider(decoder);
        provider.setJwtAuthenticationConverter(jwtAuthenticationConverter());
        return provider::authenticate;
    }

    @Bean
    JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(new KeycloakRoleConverter());
        converter.setPrincipalClaimName("preferred_username");
        return converter;
    }
}
