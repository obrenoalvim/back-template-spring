package com.example.backtemplate.config;

import com.example.backtemplate.auth.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

  private final JwtService jwtService;

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    String header = request.getHeader("Authorization");
    if (header != null && header.startsWith("Bearer ")) {
      try {
        var claims = jwtService.parse(header.substring(7)).getPayload();
        if ("access".equals(claims.get("type", String.class))) {
          var auth = new UsernamePasswordAuthenticationToken(claims.getSubject(), null, List.of());
          SecurityContextHolder.getContext().setAuthentication(auth);
        }
      } catch (io.jsonwebtoken.JwtException ignored) {
        // leave SecurityContext empty -- request falls through to unauthenticated handling
      }
    }
    chain.doFilter(request, response);
  }
}
