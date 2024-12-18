package com.szs.szsapi.jwt;

import com.szs.szsapi.dto.JwtTokenDto;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Component
public class JwtTokenProvider {
    private final Key key;

    // 생성자를 통한 JWT 서명용 Key 초기화
    // application.property에서 secret 값 가져와서 key에 저장
    public JwtTokenProvider(@Value("${jwt.secret}") String secretKey) {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey); // Base64로 인코딩된 Secret Key 디코딩
        this.key = Keys.hmacShaKeyFor(keyBytes); // Secret Key를 이용하여 Key 객체 생성
    }

    // 유저 정보를 이용하여 AccessToken과 RefreshToken을 생성하는 메서드
    public JwtTokenDto generateToken(Authentication authentication) {
        // 권한 가져오기
        String authorities = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.joining(","));

        long now = (new Date()).getTime(); // 현재 시각 가져오기
        Date issuedAt = new Date(); // 토큰 발급 시각

        //Header 부분 설정
        Map<String, Object> headers = new HashMap<>();
        headers.put("alg", "HS256");
        headers.put("typ", "JWT");

        // Access Token 생성
        String accessToken = Jwts.builder()
                .setHeader(createHeaders()) // Header 부분 설정
                .setSubject("accessToken") // 토큰 주제 설정
                .claim("aud", authentication.getName()) // 토큰 대상자 설정
                .claim("auth", authorities) // 사용자 권한 설정
                .setExpiration(new Date(now + 1800000)) // 토큰 만료 시간 설정 (30분)
                .setIssuedAt(issuedAt) // 토큰 발급 시각 설정
                .signWith(key, SignatureAlgorithm.HS256) // 서명 알고리즘 설정
                .compact(); // 토큰 생성

        // Refresh Token 생성
        String refreshToken = Jwts.builder()
                .setHeader(createHeaders()) // Header 부분 설정
                .setSubject("refreshToken") // 토큰 주제 설정
                .claim("aud", authentication.getName()) // 토큰 대상자 설정
                .claim("auth", authorities) // 사용자 권한 설정
                .setExpiration(new Date(now + 86400000)) // 토큰 만료 시간 설정 (1일)
                .setIssuedAt(issuedAt) // 토큰 발급 시각 설정
                .signWith(key, SignatureAlgorithm.HS256) // 서명 알고리즘 설정
                .compact(); // 토큰 생성

        // TokenInfo 객체 생성 및 반환
        return JwtTokenDto.builder()
                .grantType("Bearer") // 토큰 타입 설정
                .accessToken(accessToken) // Access Token 설정
                .refreshToken(refreshToken) // Refresh Token 설정
                .build(); // TokenInfo 객체 생성
    }

    // JWT 토큰을 복호화하여 토큰에 들어있는 정보를 꺼내 Authentication 객체를 생성하는 메서드
    public Authentication getAuthentication(String token) {
        // Jwt 토큰 복호화
        Claims claims = parseClaims(token);

        if (claims.get("auth") == null) {
            throw new RuntimeException("권한 정보가 없는 토큰입니다.");
        }

        // 클레임에서 권한 가져오기
        Collection<? extends GrantedAuthority> authorities = Arrays.stream(claims.get("auth").toString().split(","))
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());

        // UserDetails 객체를 만들어서 Authentication return
        // UserDetails: interface, User: UserDetails를 구현한 class
        UserDetails principal = new User((String) claims.get("aud"), "", authorities);

        // UsernamePasswordAuthenticationToken 객체 반환
        return new UsernamePasswordAuthenticationToken(principal, token, authorities);
    }

    // JWT 토큰의 유효성을 검증하는 메서드
    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token).getBody().getSubject(); // 토큰 파싱하여 유효성 검증
            return true; // 유효한 토큰일 경우 true 반환
        } catch (SecurityException | MalformedJwtException e) {
            log.info("Invalid JWT Token", e);
        } catch (ExpiredJwtException e) {
            log.info("Expired JWT Token", e);
        } catch (UnsupportedJwtException e) {
            log.info("Unsupported JWT Token", e);
        } catch (IllegalArgumentException e) {
            log.info("JWT claims string is empty.", e);
        }

        return false;
    }

    // JWT 토큰을 파싱하여 클레임 정보를 반환하는 메서드
    private Claims parseClaims(String accessToken) {
        try {
            return Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(accessToken).getBody(); // 토큰 파싱하여 클레임 정보 반환
        } catch (ExpiredJwtException e) {
            return e.getClaims(); // 만료된 토큰의 경우 클레임 정보 반환
        }
    }

    // JWT 토큰의 Header 정보를 생성하는 메서드
    private static Map<String, Object> createHeaders() {
        Map<String, Object> headers = new HashMap<>();
        headers.put("alg", "HS256"); // 알고리즘 정보 설정
        headers.put("typ", "JWT"); // 토큰 타입 정보 설정
        return headers; // 생성된 Header 정보 반환
    }
}
