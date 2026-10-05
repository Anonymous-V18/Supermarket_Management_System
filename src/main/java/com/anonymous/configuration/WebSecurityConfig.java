package com.anonymous.configuration;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.springframework.security.config.http.SessionCreationPolicy.STATELESS;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@EnableTransactionManagement
@FieldDefaults(level = AccessLevel.PRIVATE)
public class WebSecurityConfig {

    static final String[] PUBLIC_ENDPOINTS = {
            "/auth/**",
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/swagger-ui.html"
    };

    final JwtDecoderCustom jwtDecoderCustom;

    @Value("${com.anonymous.origin-client-1:http://localhost:4200}")
    String origin1;

    @Value("${com.anonymous.origin-client-2:http://localhost:4201}")
    String origin2;

    public WebSecurityConfig(JwtDecoderCustom jwtDecoderCustom) {
        this.jwtDecoderCustom = jwtDecoderCustom;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(PUBLIC_ENDPOINTS).permitAll()
                        .anyRequest().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt
                                .jwtAuthenticationConverter(jwtAuthenticationConverterCustom())
                                .decoder(jwtDecoderCustom)
                        )
                        .authenticationEntryPoint(new JwtAuthenticationEntryPoint())
                )
                .sessionManagement(s -> s.sessionCreationPolicy(STATELESS));

        return http.build();
    }

    @Bean
    public Converter<Jwt, ? extends AbstractAuthenticationToken> jwtAuthenticationConverterCustom() {
        return jwt -> {
            Set<GrantedAuthority> authorities = new HashSet<>();

            // 1. Extract from 'scope' claim (space delimited list of ROLE_... and permissions)
            String scope = jwt.getClaimAsString("scope");
            if (scope != null && !scope.isBlank()) {
                for (String auth : scope.split("\\s+")) {
                    if (!auth.isBlank()) {
                        authorities.add(new SimpleGrantedAuthority(auth));
                    }
                }
            }

            // 2. Extract from 'roles' claim (list of role codes)
            List<String> roles = jwt.getClaimAsStringList("roles");
            if (roles != null) {
                for (String role : roles) {
                    if (role != null && !role.isBlank()) {
                        String roleName = role.startsWith("ROLE_") ? role : "ROLE_" + role;
                        authorities.add(new SimpleGrantedAuthority(roleName));
                    }
                }
            }

            // 3. Extract from 'authorities' claim (list of permission codes)
            List<String> perms = jwt.getClaimAsStringList("authorities");
            if (perms != null) {
                for (String perm : perms) {
                    if (perm != null && !perm.isBlank()) {
                        authorities.add(new SimpleGrantedAuthority(perm));
                    }
                }
            }

            // 4. Role Hierarchy Expansion: SUPER_ADMIN possesses all operational privileges
            boolean isSuperAdmin = authorities.stream().anyMatch(a ->
                    a.getAuthority().equalsIgnoreCase("ROLE_SUPER_ADMIN") ||
                    a.getAuthority().equalsIgnoreCase("SUPER_ADMIN"));

            if (isSuperAdmin) {
                authorities.add(new SimpleGrantedAuthority("ROLE_SUPER_ADMIN"));
                authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
                authorities.add(new SimpleGrantedAuthority("ROLE_STOREKEEPER"));
                authorities.add(new SimpleGrantedAuthority("ROLE_SALESMAN"));
                authorities.add(new SimpleGrantedAuthority("ROLE_EMPLOYEE"));
                authorities.add(new SimpleGrantedAuthority("ROLE_CUSTOMER"));
                authorities.add(new SimpleGrantedAuthority("ROLE_ACCOUNTING_STAFF"));
            }

            // Prioritize 'username' claim for principal name, fallback to subject
            String principalName = jwt.getClaimAsString("username");
            if (principalName == null || principalName.isBlank()) {
                principalName = jwt.getSubject();
            }

            return new JwtAuthenticationToken(jwt, authorities, principalName);
        };
    }

    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration configuration = new CorsConfiguration();

        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedOrigins(List.of(origin1, origin2));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource urlBasedCorsConfigurationSource = new UrlBasedCorsConfigurationSource();
        urlBasedCorsConfigurationSource.registerCorsConfiguration("/**", configuration);

        return new CorsFilter(urlBasedCorsConfigurationSource);
    }

}
