package com.transport.transport.config;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration;
import org.springframework.boot.autoconfigure.data.mongo.MongoRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.transport.transport.repository.UtilisateurRepository;
import com.transport.transport.service.PresenceService;

@SpringBootTest(classes = PublicEndpointSecurityTest.TestApplication.class)
@AutoConfigureMockMvc
@TestPropertySource(properties = {
    "app.jwt.secret=01234567890123456789012345678901",
    "app.google.client-id=test-google-client-id"
})
class PublicEndpointSecurityTest {

  @Autowired
  private MockMvc mockMvc;

  @MockBean
  private UtilisateurRepository utilisateurRepository;

  @MockBean
  private PresenceService presenceService;

  @Test
  void publicRegisterEmailEndpointIgnoresInvalidAuthorizationHeader() throws Exception {
    mockMvc.perform(post("/api/utilisateur/register/email")
            .header("Authorization", "Bearer invalid-token")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"email":"test@example.com"}
                """))
        .andExpect(status().isOk())
        .andExpect(content().string("public-ok"));
  }

  @Test
  void privateEndpointStillRequiresAuthentication() throws Exception {
    mockMvc.perform(get("/api/private")
            .header("Authorization", "Bearer invalid-token"))
        .andExpect(status().isUnauthorized());
  }

  @SpringBootConfiguration
  @EnableAutoConfiguration(exclude = {
      MongoAutoConfiguration.class,
      MongoDataAutoConfiguration.class,
      MongoRepositoriesAutoConfiguration.class
  })
  @Import({
      SecurityConfig.class,
      PublicEndpointMatcher.class,
      UpdateLastSeenFilter.class,
      TestController.class
  })
  static class TestApplication {
  }

  @RestController
  static class TestController {

    @PostMapping("/api/utilisateur/register/email")
    String registerEmail(@RequestBody Map<String, String> body) {
      return "public-ok";
    }

    @GetMapping("/api/private")
    String privateEndpoint() {
      return "private-ok";
    }
  }
}
