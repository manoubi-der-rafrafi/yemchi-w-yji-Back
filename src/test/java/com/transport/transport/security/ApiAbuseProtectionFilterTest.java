package com.transport.transport.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import com.transport.transport.service.RequestRateLimiter;

class ApiAbuseProtectionFilterTest {
  @Test
  void forgotPasswordIsLimitedToFiveRequestsPerIpAndHour() throws Exception {
    ApiAbuseProtectionFilter filter = new ApiAbuseProtectionFilter(
        new RequestRateLimiter(), false);

    for (int requestNumber = 1; requestNumber <= 6; requestNumber++) {
      MockHttpServletRequest request = new MockHttpServletRequest(
          "POST", "/api/utilisateur/forgot-password");
      request.setRemoteAddr("203.0.113.10");
      request.setContentType("application/json");
      MockHttpServletResponse response = new MockHttpServletResponse();
      MockFilterChain chain = new MockFilterChain();

      filter.doFilter(request, response, chain);

      if (requestNumber <= 5) {
        assertEquals(200, response.getStatus());
        assertNotNull(chain.getRequest());
      } else {
        assertEquals(429, response.getStatus());
        assertNotNull(response.getHeader("Retry-After"));
      }
    }
  }
}
