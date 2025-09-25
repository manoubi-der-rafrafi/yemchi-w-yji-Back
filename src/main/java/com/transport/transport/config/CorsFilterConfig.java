// com.transport.transport.config.CorsFilterConfig
package com.transport.transport.config;

import java.io.IOException;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE) // s'assurer qu'il passe avant les filtres de sécurité
public class CorsFilterConfig implements Filter {

  @Override
  public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
      throws IOException, ServletException {

    HttpServletRequest request = (HttpServletRequest) req;
    HttpServletResponse response = (HttpServletResponse) res;

    String origin = request.getHeader("Origin");

    // Autorise explicitement tes origines
    if (origin != null && (
        origin.equals("http://localhost:4200") ||
        origin.equals("https://yemchi-w-yji-front.vercel.app")
    )) {
      response.setHeader("Access-Control-Allow-Origin", origin);
      response.setHeader("Vary", "Origin"); // utile pour le cache
    }

    response.setHeader("Access-Control-Allow-Credentials", "true");
    response.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
    // ✅ ajoute X-User-Id et X-Requested-With
    response.setHeader("Access-Control-Allow-Headers",
        "Origin, Content-Type, Accept, Authorization, X-Requested-With, X-User-Id");
    response.setHeader("Access-Control-Max-Age", "3600");
    // (optionnel) si le front doit lire ces en-têtes dans la réponse
    response.setHeader("Access-Control-Expose-Headers", "Authorization, Location");

    // Préflight : on répond sans passer plus loin
    if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
      response.setStatus(HttpServletResponse.SC_NO_CONTENT); // 204
      return;
    }

    chain.doFilter(req, res);
  }

  @Override public void init(FilterConfig filterConfig) {}
  @Override public void destroy() {}
}
