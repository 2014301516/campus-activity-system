package com.cas.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Spring Security 配置
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Autowired
    private JwtAuthFilter jwtAuthFilter;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf().disable()
            .cors().and()
            .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            .and()
            .authorizeRequests()
            // 公开接口
            .antMatchers("/api/user/register", "/api/user/login", "/api/user/wx-login").permitAll()
            .antMatchers(HttpMethod.GET, "/api/activity/**").permitAll()
            .antMatchers(HttpMethod.GET, "/api/category/**").permitAll()
            .antMatchers(HttpMethod.GET, "/api/review/activity/**").permitAll()
            .antMatchers(HttpMethod.GET, "/api/notice/list").permitAll()
            // 管理员接口
            .antMatchers("/api/admin/**").hasRole("ADMIN")
            // 组织者 + 管理员接口
            .antMatchers(HttpMethod.POST, "/api/activity").hasAnyRole("ORGANIZER", "ADMIN")
            .antMatchers(HttpMethod.PUT, "/api/activity/*").hasAnyRole("ORGANIZER", "ADMIN")
            .antMatchers(HttpMethod.DELETE, "/api/activity/*").hasAnyRole("ORGANIZER", "ADMIN")
            // 其余接口需登录
            .anyRequest().authenticated()
            .and()
            // JWT 过滤器
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
