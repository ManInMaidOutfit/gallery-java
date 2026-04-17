package com.gallery.gallery.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import java.util.List;

@Configuration
public class SecurityConfig 
{
    @Autowired
    private JwtAuthFilter jwtAuthFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .authorizeHttpRequests(auth -> auth
                    // Публичные эндпоинты
                    .requestMatchers("/", "/index.html", "/style.css", "/script.js").permitAll()
                    .requestMatchers("/auth/login", "/auth/register").permitAll()
                    .requestMatchers("/photos/image/**").permitAll()
                    .requestMatchers("/photos/{id}/view").permitAll()
                    .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/swagger-ui.html").permitAll()
                    
                    // GET запросы к категориям и фото - всем можно
                    .requestMatchers(HttpMethod.GET, "/**").permitAll()
                    .requestMatchers(HttpMethod.GET, "/categories").permitAll()
                    .requestMatchers(HttpMethod.GET, "/categories/**").permitAll()
                    .requestMatchers(HttpMethod.GET, "/photos").permitAll()
                    .requestMatchers(HttpMethod.GET, "/photos/**").permitAll()
                    
                    // POST, PUT, DELETE, PATCH к категориям - только для ADMIN
                    .requestMatchers(HttpMethod.POST, "/categories/**").hasRole("admin")
                    .requestMatchers(HttpMethod.PUT, "/categories/**").hasRole("admin")
                    .requestMatchers(HttpMethod.PATCH, "/categories/**").hasRole("admin")
                    .requestMatchers(HttpMethod.DELETE, "/categories/**").hasRole("admin")
                    
                    // Загрузка фото - только для ADMIN
                    .requestMatchers("/photos/upload").hasRole("admin")
                    
                    // Удаление фото - только для ADMIN
                    .requestMatchers(HttpMethod.DELETE, "/photos/**").hasRole("admin")
                    
                    // ОБНОВЛЕНИЕ ФОТО (PATCH) - только для ADMIN
                    .requestMatchers(HttpMethod.PATCH, "/photos/**").hasRole("admin")
                    
                    // PUT для фото - только для ADMIN (на всякий случай)
                    .requestMatchers(HttpMethod.PUT, "/photos/**").hasRole("admin")
                    
                    // Все остальные запросы требуют аутентификации
                    .anyRequest().authenticated()
                )
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(List.of("*"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}