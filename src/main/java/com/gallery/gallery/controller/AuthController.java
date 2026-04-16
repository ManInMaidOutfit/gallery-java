package com.gallery.gallery.controller;

import com.gallery.gallery.entity.User;
import com.gallery.gallery.repository.UserRepository;
import com.gallery.gallery.service.JwtService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController
{
    @Autowired
    private AuthenticationManager autenticationManager;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @PostMapping("/login")
    public Map<String, String> login(@RequestParam String username, @RequestParam String password)
    {
        Authentication authentication = autenticationManager.authenticate(new UsernamePasswordAuthenticationToken(username, password));

        String role = authentication.getAuthorities().iterator().next().getAuthority().replace("ROLE_", "");
        String token = jwtService.generateToken(username, role);

        Map<String, String> response = new HashMap<>();
        response.put("access_token", token);
        response.put("token_type", "bearer");
        response.put("role", role);

        return response;
    }

    @PostMapping("/register")
    public String register(@RequestParam String username, @RequestParam String password, @RequestParam(defaultValue = "user") String role)
    {
        if(userRepository.findByUsername(username).isPresent())
            return "username already exists";

        User user = new User();
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setRole(role);
        userRepository.save(user);

        return "User registered successfully";
    }
}
