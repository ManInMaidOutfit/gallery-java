package com.gallery.gallery.config;

import com.gallery.gallery.service.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtAuthFilter extends OncePerRequestFilter
{
    @Autowired
    private JwtService jwtService;

    @Autowired
    private UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) 
            throws ServletException, IOException {
        
        String path = request.getRequestURI();
        String method = request.getMethod();
        
        // ✅ Пропускаем все GET запросы к /photos (с любыми параметрами)
        if (method.equals("GET") && path.startsWith("/photos")) {
            System.out.println("PUBLIC GET: " + path + " - no auth required");
            chain.doFilter(request, response);
            return;
        }
        
        // ✅ Пропускаем публичные эндпоинты
        if (path.equals("/") || 
            path.equals("/index.html") ||
            path.equals("/style.css") ||
            path.equals("/script.js") ||
            path.startsWith("/auth/login") || 
            path.startsWith("/auth/register") || 
            path.startsWith("/swagger-ui") ||
            path.startsWith("/v3/api-docs") ||
            path.startsWith("/photos/image/")) {
            System.out.println("PUBLIC: " + path + " - no auth required");
            chain.doFilter(request, response);
            return;
        }
        
        // ✅ Для всех остальных запросов проверяем токен
        final String authHeader = request.getHeader("Authorization");
        
        if(authHeader == null || !authHeader.startsWith("Bearer ")) {
            System.out.println("NO TOKEN for: " + path + " - returning 403");
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.getWriter().write("Access denied");
            return;
        }

        try {
            final String jwt = authHeader.substring(7);
            final String username = jwtService.extractUsername(jwt);

            if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails userDetails = this.userDetailsService.loadUserByUsername(username);

                if(jwtService.isTokenValid(jwt, userDetails.getUsername())) {
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null,
                            userDetails.getAuthorities()
                    );
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                    System.out.println("AUTH SUCCESS: " + username + " for " + path);
                }
            }
            chain.doFilter(request, response);
        } catch (Exception e) {
            System.out.println("JWT ERROR: " + e.getMessage() + " for " + path);
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.getWriter().write("Invalid token");
        }
    }
}