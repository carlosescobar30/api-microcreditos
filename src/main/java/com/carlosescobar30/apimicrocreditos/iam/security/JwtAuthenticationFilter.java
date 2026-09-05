package com.carlosescobar30.apimicrocreditos.iam.security;

import com.carlosescobar30.apimicrocreditos.common.exception.ErrorCode;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.SignatureException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static com.carlosescobar30.apimicrocreditos.iam.security.SecurityAttributes.AUTH_ERROR_ATTRIBUTE;


@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;


    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ") ){

            filterChain.doFilter(request, response);
            return;

        }

        try {

            if (SecurityContextHolder.getContext().getAuthentication() == null) {

                String jwt = authHeader.substring(7);
                Claims claims = jwtService.extractAllClaims(jwt);


                List<String> roles = (claims.get(JwtAttributes.NAME_CLAIM_ROLES, List.class) == null) ?
                        new ArrayList<>() : new ArrayList<>(claims.get(JwtAttributes.NAME_CLAIM_ROLES, List.class));

                List<SimpleGrantedAuthority> authorities = roles.stream()
                        .map(SimpleGrantedAuthority::new).toList();


                UserDetails user = new UserDetailsImpl(
                        claims.get(JwtAttributes.NAME_CLAIM_USER_ID, Long.class),
                        claims.getSubject(),
                        null,
                        authorities
                );

                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        user,
                        null,
                        user.getAuthorities()
                );

                SecurityContextHolder.getContext().setAuthentication(authToken);

            }
        }catch (ExpiredJwtException exception){

            log.debug("Expired JWT for on {} {}", request.getMethod(), request.getRequestURI() );
            request.setAttribute(AUTH_ERROR_ATTRIBUTE, ErrorCode.TOKEN_EXPIRED);

        }catch (SignatureException exception){

            log.warn("Illegal JWT on {}: {}", request.getRequestURI(), exception.getClass().getSimpleName());
            request.setAttribute(AUTH_ERROR_ATTRIBUTE, ErrorCode.UNAUTHENTICATED);

        }catch (IllegalArgumentException | JwtException exception){

            log.warn("Invalid JWT on {} {}", request.getMethod(), request.getRequestURI() );
            request.setAttribute(AUTH_ERROR_ATTRIBUTE, ErrorCode.UNAUTHENTICATED);

        }catch (RuntimeException exception){
            log.error("Unexpected Error ", exception);
            request.setAttribute(AUTH_ERROR_ATTRIBUTE, ErrorCode.INTERNAL_ERROR_SERVER);
        }

        filterChain.doFilter(request,response);

    }
}
