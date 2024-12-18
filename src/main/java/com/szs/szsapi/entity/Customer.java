package com.szs.szsapi.entity;

import com.szs.szsapi.dto.CustomerRequestDto;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Schema(description = "고객 정보")
@Entity(name = "Cu_Customer")
@Table
@Getter
@Setter
@NoArgsConstructor
public class Customer extends Timestamped implements UserDetails {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Schema(description = "회원 식별자")
    @Column(name = "cuIdx")
    private Long cuIdx;
    @Column(name="userId", unique = true, nullable = false, length = 50)
    @Schema(description = "아이디")
    private String userId;
    @Column(name="password", nullable = false, length = 255)
    @Schema(description = "비밀번호")
    private String password;
    @Column(name="name", nullable = false, length = 50)
    @Schema(description = "이름")
    private String name;
    @Column(name="regNo", nullable = false, length = 255)
    @Schema(description = "주민등록번호")
    private String regNo;

    @ElementCollection(fetch = FetchType.EAGER)
    private List<String> roles = new ArrayList<>();

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return this.roles.stream()
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());
    }

    @Override
    public String getUsername() {
        return userId;
    }

    @Override
    public String getPassword() {
        return password;
    }

    public void encodePassword(PasswordEncoder passwordEncoder){
        this.password = passwordEncoder.encode(password);
        this.regNo = passwordEncoder.encode(regNo);
    }

    public Customer(CustomerRequestDto requestDto) {
        this.userId = requestDto.getUserId();
        this.password = requestDto.getPassword();
        this.name = requestDto.getName();
        this.regNo = requestDto.getRegNo();
    }
}
