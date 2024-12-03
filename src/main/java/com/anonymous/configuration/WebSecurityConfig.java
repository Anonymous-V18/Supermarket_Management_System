package com.anonymous.configuration;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.List;

import static org.springframework.security.config.http.SessionCreationPolicy.STATELESS;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@EnableTransactionManagement
@FieldDefaults(level = AccessLevel.PRIVATE)
public class WebSecurityConfig {

    static final String[] PUBLIC_ENDPOINTS = {
            "/api/v1/**", "/auth/**", "/users/**", "/login", "/logout", "/register", "/introspect-token", "/refresh-token"
            , "/roles/**", "/customers/**", "/employees/**", "/warehouses/**", "/suppliers/**"
            , "/product-categories/**", "/units/**", "/stock-ins/**", "/stock-outs/**"
            , "/brands/**", "/positions/**", "/wards/**", "/districts/**", "/cities/**"
            , "/products/**", "/invoices/**", "/promotions/**", "/status-invoices/**", "/status-products/**"
    };

    final JwtDecoderCustom jwtDecoderCustom;

    @Value("${com.anonymous.jwt.secret-key}")
    String SECRET_KEY;

    @Value("${com.anonymous.origin-client-1}")
    String origin1;

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
                                .jwtAuthenticationConverter(this.jwtAuthenticationConverterCustom())
                                .decoder(jwtDecoderCustom)
                        )
                        .authenticationEntryPoint(new JwtAuthenticationEntryPoint())
                )
                .sessionManagement(s -> s.sessionCreationPolicy(STATELESS));

        return http.build();
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverterCustom() {
        JwtGrantedAuthoritiesConverter jwtGrantedAuthoritiesConverter = new JwtGrantedAuthoritiesConverter();
        jwtGrantedAuthoritiesConverter.setAuthorityPrefix("");

        JwtAuthenticationConverter jwtAuthenticationConverter = new JwtAuthenticationConverter();
        jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(jwtGrantedAuthoritiesConverter);
        return jwtAuthenticationConverter;
    }

    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration configuration = new CorsConfiguration();

        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowedMethods(List.of("*"));
        configuration.setAllowedOrigins(List.of(origin1));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource urlBasedCorsConfigurationSource = new UrlBasedCorsConfigurationSource();
        urlBasedCorsConfigurationSource.registerCorsConfiguration("/**", configuration);

        return new CorsFilter(urlBasedCorsConfigurationSource);
    }

}
