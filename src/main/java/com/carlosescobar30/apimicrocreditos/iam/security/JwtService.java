package com.carlosescobar30.apimicrocreditos.iam.security;

import com.carlosescobar30.apimicrocreditos.iam.configuration.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Clock;
import java.time.Instant;
import java.util.Date;

@Service
public class JwtService {

    private final JwtProperties jwtProperties;
    private Clock clock;
    private io.jsonwebtoken.Clock jwtClock;

    public JwtService (Clock clock, JwtProperties jwtProperties){

        this.jwtProperties = jwtProperties;
        this.clock = clock;
        this.jwtClock = () -> Date.from(clock.instant());

    }



    public String generateJwt (UserDetailsImpl userDetails){

        Instant now = clock.instant();

        return Jwts.builder()
                .subject(userDetails.getUsername())
                .claim("uId", userDetails.getId())
                .claim("roles",userDetails.getAuthorities().stream()
                        .map(GrantedAuthority::getAuthority).toList())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusMillis(jwtProperties.expiration().toMillis())))
                .signWith(getSignInKey())
                .compact();

    }


    private SecretKey getSignInKey(){
        byte[] secretKeyInBytes = Decoders.BASE64.decode(jwtProperties.secretKey());
        return Keys.hmacShaKeyFor(secretKeyInBytes);
    }

    public Claims extractAllClaims (String token){

        return Jwts.parser()
                .verifyWith(getSignInKey())
                .clock(jwtClock)
                .build()
                .parseSignedClaims(token)
                .getPayload();

    }


}
