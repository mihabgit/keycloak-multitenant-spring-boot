package dev.mihab.tenantauth;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Checks authorization and tenant isolation without a running Keycloak:
 * spring-security-test injects ready-made JWTs, so these tests cover everything
 * after signature verification.
 */
@SpringBootTest
@AutoConfigureMockMvc
class TenantIsolationTests {

    private static final String ACME = "http://localhost:8080/realms/acme";
    private static final String GLOBEX = "http://localhost:8080/realms/globex";

    @Autowired
    MockMvc mvc;

    @Test
    void publicEndpointNeedsNoToken() throws Exception {
        mvc.perform(get("/api/public/ping")).andExpect(status().isOk());
    }

    @Test
    void protectedEndpointRejectsMissingToken() throws Exception {
        mvc.perform(get("/api/projects")).andExpect(status().isUnauthorized());
    }

    @Test
    void eachTenantSeesOnlyItsOwnProjects() throws Exception {
        mvc.perform(get("/api/projects").with(user(ACME, "bob", "user")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].tenantId").value(everyItem(is("acme"))))
                .andExpect(jsonPath("$[*].name").value(hasItem("Customer portal")))
                .andExpect(jsonPath("$[*].name").value(not(hasItem("Warehouse tracking"))));

        mvc.perform(get("/api/projects").with(user(GLOBEX, "carol", "user")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].tenantId").value("globex"));
    }

    @Test
    void regularUserCannotCreateProjects() throws Exception {
        mvc.perform(post("/api/projects").with(user(ACME, "bob", "user"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Not allowed\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCreatesProjectInOwnTenantOnly() throws Exception {
        MvcResult created = mvc.perform(post("/api/projects").with(user(ACME, "alice", "user", "admin"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"New dashboard\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tenantId").value("acme"))
                .andExpect(jsonPath("$.createdBy").value("alice"))
                .andReturn();

        String id = com.jayway.jsonpath.JsonPath.read(created.getResponse().getContentAsString(), "$.id");

        // Same id, other tenant: looks like it doesn't exist.
        mvc.perform(get("/api/projects/" + id).with(user(GLOBEX, "carol", "admin")))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/projects/" + id).with(user(ACME, "bob", "user")))
                .andExpect(status().isOk());
    }

    @Test
    void blankProjectNameIsRejected() throws Exception {
        mvc.perform(post("/api/projects").with(user(ACME, "alice", "admin"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void tokenFromUnknownRealmIsRejected() throws Exception {
        mvc.perform(get("/api/projects").with(user("http://evil.example/realms/acme", "mallory", "admin")))
                .andExpect(status().isForbidden());
    }

    @Test
    void meShowsTenantAndRoles() throws Exception {
        mvc.perform(get("/api/me").with(user(GLOBEX, "carol", "admin")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("carol"))
                .andExpect(jsonPath("$.tenant").value("globex"))
                .andExpect(jsonPath("$.roles[0]").value("ROLE_admin"));
    }

    private static JwtRequestPostProcessor user(String issuer, String username, String... roles) {
        SimpleGrantedAuthority[] authorities = java.util.Arrays.stream(roles)
                .map(r -> new SimpleGrantedAuthority("ROLE_" + r))
                .toArray(SimpleGrantedAuthority[]::new);
        return jwt()
                .jwt(j -> j.issuer(issuer).subject(username).claim("preferred_username", username))
                .authorities(authorities);
    }
}
