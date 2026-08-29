package com.file.filter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

import java.security.MessageDigest;

import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.file.config.FiloraFSProperties;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class ApiKeyFilter extends OncePerRequestFilter {

	private final FiloraFSProperties properties;

	public ApiKeyFilter(FiloraFSProperties properties) {
		this.properties = properties;
	}

	private static final String HEADER_NAME = "X-API-KEY";

	@Override
	protected boolean shouldNotFilter(HttpServletRequest request) {
		String path = request.getRequestURI();
		// Only apply API key filter to the file API endpoints
		return !path.startsWith("/file");
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {

		if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
			filterChain.doFilter(request, response);
			return;
		}

		String requestKey = request.getHeader(HEADER_NAME);
		if (requestKey == null || !MessageDigest.isEqual(requestKey.getBytes(StandardCharsets.UTF_8),
				properties.apiKey().getBytes(StandardCharsets.UTF_8))) {

			response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
			response.getWriter().write("Unauthorized");
			return;
		}

		filterChain.doFilter(request, response);
	}

}
