package com.transport.transport.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {
  private final BackendErrorInterceptor backendErrorInterceptor;

  public WebMvcConfig(BackendErrorInterceptor backendErrorInterceptor) {
    this.backendErrorInterceptor = backendErrorInterceptor;
  }

  @Override
  public void addInterceptors(InterceptorRegistry registry) {
    registry.addInterceptor(backendErrorInterceptor);
  }
}
