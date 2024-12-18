package com.szs.szsapi.controller;

import com.szs.szsapi.dto.*;
import com.szs.szsapi.service.ApiService;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/szs")
public class ApiController {

    private final ApiService apiService;

    public ApiController(ApiService apiService) {
        this.apiService = apiService;
    }

    @Operation(summary="회원가입")
    @PostMapping("/signup")
    public ResponseEntity<String> signup(@RequestBody CustomerRequestDto requestDto) throws Exception {
        return apiService.signup(requestDto);
    }

    @Operation(summary="로그인")
    @PostMapping("/login")
    public JwtTokenDto login(@RequestBody LoginRequestDto loginDto){
        return apiService.login(loginDto);
    }

    @Operation(summary="회원별 소득공제 정보 스크래핑")
    @PostMapping("/scrap")
    public ResponseEntity<String> scrap(@RequestBody ScrapRequestDto ScraprequestDto) throws Exception {
        return apiService.scrap(ScraprequestDto);
    }

    @Operation(summary="회원별 결정세액 조회")
    @GetMapping("/refund")
    public IncomeTaxResponseDto refund(){
        return apiService.refund();
    }

}
