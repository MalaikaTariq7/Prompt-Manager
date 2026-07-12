package com.promptmanager.gateway.filter;

import java.time.LocalDateTime;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class LoggingFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {

        System.out.println();
        System.out.println("======================================");
        System.out.println("API Gateway Request");
        System.out.println("Time   : " + LocalDateTime.now());
        System.out.println("Method : " + request.getMethod());
        System.out.println("URI    : " + request.getRequestURI());
        System.out.println("======================================");

        filterChain.doFilter(request, response);

        System.out.println("Response Status : " + response.getStatus());
        System.out.println("======================================");
        System.out.println();
    }
}