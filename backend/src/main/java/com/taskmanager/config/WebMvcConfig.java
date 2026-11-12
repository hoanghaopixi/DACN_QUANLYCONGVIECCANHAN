package com.taskmanager.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
        String uploadUri = uploadPath.toUri().toString();
        if (!uploadUri.endsWith("/")) {
            uploadUri += "/";
        }

        // Map /uploads/** to the absolute directory on the server file system
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(uploadUri)
                .setCacheControl(org.springframework.http.CacheControl.maxAge(30, java.util.concurrent.TimeUnit.DAYS).cachePublic());
    }

    @org.springframework.context.annotation.Bean
    public org.springframework.boot.web.servlet.FilterRegistrationBean<jakarta.servlet.Filter> inlineMediaFilter() {
        org.springframework.boot.web.servlet.FilterRegistrationBean<jakarta.servlet.Filter> registration = new org.springframework.boot.web.servlet.FilterRegistrationBean<>();
        registration.setFilter(new org.springframework.web.filter.OncePerRequestFilter() {
            @Override
            protected void doFilterInternal(jakarta.servlet.http.HttpServletRequest request,
                                            jakarta.servlet.http.HttpServletResponse response,
                                            jakarta.servlet.FilterChain filterChain)
                    throws jakarta.servlet.ServletException, java.io.IOException {
                response.setHeader("Content-Disposition", "inline");
                response.setHeader("Accept-Ranges", "bytes");
                filterChain.doFilter(request, response);
            }
        });
        registration.addUrlPatterns("/uploads/*");
        registration.setName("inlineMediaFilter");
        registration.setOrder(1);
        return registration;
    }
}
