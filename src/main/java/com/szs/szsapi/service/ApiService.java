package com.szs.szsapi.service;

import com.szs.szsapi.dto.*;
import com.szs.szsapi.entity.Customer;
import com.szs.szsapi.entity.Deduction;
import com.szs.szsapi.entity.IncomeTax;
import com.szs.szsapi.jwt.JwtTokenProvider;
import com.szs.szsapi.repository.CustomerRepository;
import com.szs.szsapi.repository.DeductionRepository;
import com.szs.szsapi.repository.IncomeTaxRepository;
import org.apache.commons.lang3.StringUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;


@Service
public class ApiService {

    @Value("${scrap.url}")
    private String scrapUrl;
    @Value("${scrap.api.key}")
    private String apiKey;

    private final CustomerRepository customerRepository;
    private final IncomeTaxRepository incomeTaxRepository;
    private final DeductionRepository deductionRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuthenticationManagerBuilder authenticationManagerBuilder;

    public ApiService(CustomerRepository customerRepository, IncomeTaxRepository incomeTaxRepository, DeductionRepository deductionRepository, PasswordEncoder passwordEncoder, JwtTokenProvider jwtTokenProvider, AuthenticationManagerBuilder authenticationManagerBuilder) {
        this.customerRepository = customerRepository;
        this.incomeTaxRepository = incomeTaxRepository;
        this.deductionRepository = deductionRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
        this.authenticationManagerBuilder = authenticationManagerBuilder;
    }

    @Transactional
    public ResponseEntity<String> signup(CustomerRequestDto requestDto) throws Exception {
        Customer customer = new Customer(requestDto);
        //암호화
        customer.setPassword(passwordEncoder.encode(customer.getPassword()));
        customer.setRegNo(passwordEncoder.encode(requestDto.getRegNo()));

        //동일한 ID가 존재하는지 여부 확인
        if(customerRepository.findByUserId(customer.getUserId()).isPresent()) {
            throw new Exception("이미 존재하는 아이디입니다.");
        }

        //주민등록번호로 이미 가입 된 회원 여부 확인
        if(customerRepository.findByRegNo(customer.getRegNo()).isPresent()){
            throw new Exception("이미 가입 된 회원입니다. 로그인을 해주세요.");
        }

        Customer joinCustomer = customerRepository.save(customer);

        CustomerResponseDto responseDto = new CustomerResponseDto(joinCustomer);

        return ResponseEntity.ok(responseDto.getName() + "님 회원가입이 완료되었습니다.");
    }

    public JwtTokenDto login(LoginRequestDto loginDto) {
        Customer customer = customerRepository.findByUserId(loginDto.getUserId()).orElseThrow(() -> new IllegalArgumentException("가입되지 않은 아이디입니다."));

        if (!passwordEncoder.matches(loginDto.getPassword(), customer.getPassword())) {
            throw new IllegalArgumentException("비밀번호가 일치하지 않습니다.");
        }

//        if(jwtToken != null) {
//            //로그인 날짜 시간 DB에 저장
//            customerRepository.saveLastLoginDateByCuIdx(customer.getCuIdx());
//        }

        // 1. Login ID/PW 를 기반으로 Authentication 객체 생성
        // 이때 authentication 는 인증 여부를 확인하는 authenticated 값이 false
        UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(customer.getUserId(), customer.getPassword());

        // 2. 실제 검증 (사용자 비밀번호 체크)이 이루어지는 부분
        // authenticate 매서드가 실행될 때 CustomUserDetailsService 에서 만든 loadUserByUsername 메서드가 실행
        Authentication authentication = authenticationManagerBuilder.getObject().authenticate(authenticationToken);

        // 3. 인증 정보를 기반으로 JWT 토큰 생성
        JwtTokenDto jwtToken = jwtTokenProvider.generateToken(authentication);


        return ResponseEntity.ok(jwtToken).getBody();
    }

    @Transactional
    public ResponseEntity<String> scrap(ScrapRequestDto scraprequestDto) throws Exception {
        // SecurityContext 조회
        SecurityContext securityContext = SecurityContextHolder.getContext();
        // 인증된 정보 조회
        Authentication authentication = securityContext.getAuthentication();
        Object principal = authentication.getPrincipal();
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();

        Optional<Customer> customer = customerRepository.findByUserId(userId);

        //스크래핑 api 조회
        RestTemplate restTemplate = new RestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", "application/json");
        headers.set("X-API-KEY", apiKey);

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("name", scraprequestDto.getName());
        jsonObject.put("regNo", scraprequestDto.getRegNo());

        HttpEntity<String> entity = new HttpEntity<>(jsonObject.toJSONString(), headers);
        ResponseEntity<String> response = restTemplate.postForEntity(scrapUrl, entity, String.class);

        //String으로 되어져있는 바디부분을 다시 JSON형태로 파싱
        JSONParser parser = new JSONParser();
        JSONObject body = (JSONObject) parser.parse(response.getBody());

        if(body.get("status").equals("success")) {
            JSONObject data = (JSONObject) parser.parse(body.get("data").toString());
            JSONObject deductionInfo = (JSONObject) parser.parse(data.get("소득공제").toString());

            //기존 회원 소득정보 삭제
            incomeTaxRepository.deleteById(customer.get().getCuIdx());
            deductionRepository.deleteById(customer.get().getCuIdx());

            //회원별 소득정보 저장
            IncomeTax incomeTax = new IncomeTax();
            incomeTax.setCuIdx(customer.get().getCuIdx());
            incomeTax.setIncomeAmt((Long) data.get("종합소득금액"));
            Long taxAmt = Long.valueOf(deductionInfo.get("세액공제").toString().replaceAll(",", ""));
            incomeTax.setTaxAmt(taxAmt);
            incomeTaxRepository.save(incomeTax);

            //국민연금
            JSONArray pensionArr = (JSONArray) deductionInfo.get("국민연금");
            for(int i=0; i<pensionArr.size(); i++) {
                JSONObject arr1 = (JSONObject) pensionArr.get(i);
                Double p_amt = Double.valueOf(arr1.get("공제액").toString().replaceAll(",", ""));

                String[] split = arr1.get("월").toString().split("-");
                String p_year = split[0];
                String p_month = StringUtils.stripStart(split[1], "0"); //앞자리 0제거

                //소득공제정보 저장 - 국민연금
                Deduction deduction = new Deduction();
                deduction.setCuIdx(customer.get().getCuIdx());
                deduction.setType("1"); //국민연금
                deduction.setItYear(p_year);
                deduction.setItMonth(p_month);
                deduction.setItAmt(p_amt);
                deductionRepository.save(deduction);
            }

            //신용카드소득공제
            JSONObject creditCard = (JSONObject) parser.parse(deductionInfo.get("신용카드소득공제").toString());
            String c_year = String.valueOf(creditCard.get("year"));
            JSONArray monthArr = (JSONArray) creditCard.get("month");

            Double[] tmpList = new Double[12];
            for(int i=0; i<12; i++){
                int c_month = i+1;
                String key = String.valueOf(c_month);
                if(c_month < 10) key = "0" + key;

                tmpList[i] = 0.00;

                for (int j=0; j<monthArr.size(); j++){
                    JSONObject info = (JSONObject) monthArr.get(j);

                    if(info.containsKey(key)){
                        tmpList[i] = Double.valueOf(info.get(key).toString().replaceAll(",", ""));
                    }
                }
            }

            for(int i=0; i<tmpList.length; i++) {
                //소득공제정보 저장 - 신용카드 소득공제
                Deduction deduction = new Deduction();
                deduction.setCuIdx(customer.get().getCuIdx());
                deduction.setType("2"); //신용카드소득공제
                deduction.setItYear(c_year);
                deduction.setItMonth(String.valueOf((i+1)));
                deduction.setItAmt(tmpList[i]);
                deductionRepository.save(deduction);
            }
        }

        return ResponseEntity.ok("소득공제 스크래핑 정보가 DB에 정상적으로 저장되었습니다.");
    }

    public IncomeTaxResponseDto refund() {
        BigDecimal totalAmt = BigDecimal.ZERO;

        // SecurityContext 조회
        SecurityContext securityContext = SecurityContextHolder.getContext();
        // 인증된 정보 조회
        Authentication authentication = securityContext.getAuthentication();
        Object principal = authentication.getPrincipal();
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();

        Optional<Customer> customer = customerRepository.findByUserId(userId);

        List<IncomeTaxJqueryResponse> incomeTax = incomeTaxRepository.findByIdx(customer.get().getCuIdx());
        List<DeductionJqueryResponse> deduction = deductionRepository.findByIdx(customer.get().getCuIdx());

        BigDecimal incomeAmt = BigDecimal.ZERO;
        BigDecimal taxtAmt = BigDecimal.ZERO;
        for(int i=0; i<incomeTax.size(); i++) {
            incomeAmt = BigDecimal.valueOf(incomeTax.get(i).getIncomeAmt());
            taxtAmt = BigDecimal.valueOf(incomeTax.get(i).getTaxAmt());
        }

        BigDecimal totalDeductionAmt = BigDecimal.ZERO; //소득공제 합계
        for(int i=0; i<deduction.size(); i++){
            totalDeductionAmt.add(BigDecimal.valueOf(deduction.get(i).getItAmt()));
        }

        BigDecimal taxBase = incomeAmt.subtract(totalDeductionAmt); //과세표준
        BigDecimal generalRate = taxBase.multiply(BigDecimal.valueOf(6)).divide(BigDecimal.valueOf(100), RoundingMode.HALF_EVEN) ; //기본세율 (1400만원 이하인 경우 기본값)

        if(taxBase.compareTo(BigDecimal.valueOf(14000000)) > 0 && (taxBase.compareTo(BigDecimal.valueOf(50000000)) < 0 || taxBase.compareTo(BigDecimal.valueOf(50000000)) == 0)){
            generalRate = taxBase.subtract(BigDecimal.valueOf(14000000)).multiply(BigDecimal.valueOf(15)).divide(BigDecimal.valueOf(100), RoundingMode.HALF_EVEN).add(BigDecimal.valueOf(840000));
        }else if(taxBase.compareTo(BigDecimal.valueOf(50000000)) > 0 && (taxBase.compareTo(BigDecimal.valueOf(88000000)) < 0 || taxBase.compareTo(BigDecimal.valueOf(88000000)) == 0)){
            generalRate = taxBase.subtract(BigDecimal.valueOf(50000000)).multiply(BigDecimal.valueOf(24)).divide(BigDecimal.valueOf(100), RoundingMode.HALF_EVEN).add(BigDecimal.valueOf(6240000));
        }else if(taxBase.compareTo(BigDecimal.valueOf(88000000)) > 0 && (taxBase.compareTo(BigDecimal.valueOf(150000000)) < 0 || taxBase.compareTo(BigDecimal.valueOf(150000000)) == 0)){
            generalRate = taxBase.subtract(BigDecimal.valueOf(88000000)).multiply(BigDecimal.valueOf(35)).divide(BigDecimal.valueOf(100), RoundingMode.HALF_EVEN).add(BigDecimal.valueOf(15360000));
        }else if(taxBase.compareTo(BigDecimal.valueOf(150000000)) > 0 && (taxBase.compareTo(BigDecimal.valueOf(300000000)) < 0 || taxBase.compareTo(BigDecimal.valueOf(300000000)) == 0)){
            generalRate = taxBase.subtract(BigDecimal.valueOf(150000000)).multiply(BigDecimal.valueOf(38)).divide(BigDecimal.valueOf(100), RoundingMode.HALF_EVEN).add(BigDecimal.valueOf(37060000));
        }else if(taxBase.compareTo(BigDecimal.valueOf(300000000)) > 0 && (taxBase.compareTo(BigDecimal.valueOf(500000000)) < 0 || taxBase.compareTo(BigDecimal.valueOf(500000000)) == 0)){
            generalRate = taxBase.subtract(BigDecimal.valueOf(300000000)).multiply(BigDecimal.valueOf(40)).divide(BigDecimal.valueOf(100), RoundingMode.HALF_EVEN).add(BigDecimal.valueOf(94060000));
        }else if(taxBase.compareTo(BigDecimal.valueOf(500000000)) > 0 && (taxBase.compareTo(BigDecimal.valueOf(1000000000)) < 0 || taxBase.compareTo(BigDecimal.valueOf(1000000000)) == 0)){
            generalRate = taxBase.subtract(BigDecimal.valueOf(500000000)).multiply(BigDecimal.valueOf(42)).divide(BigDecimal.valueOf(100), RoundingMode.HALF_EVEN).add(BigDecimal.valueOf(174060000));
        }else if(taxBase.compareTo(BigDecimal.valueOf(1000000000)) > 0){
            generalRate = taxBase.subtract(BigDecimal.valueOf(1000000000)).multiply(BigDecimal.valueOf(45)).divide(BigDecimal.valueOf(100), RoundingMode.HALF_EVEN).add(BigDecimal.valueOf(384060000));
        }

        BigDecimal calculatedAmt = taxBase.multiply(generalRate); //산출세액

        totalAmt = calculatedAmt.subtract(taxtAmt); //결정세액

        IncomeTaxResponseDto responseDto = new IncomeTaxResponseDto();
        responseDto.setAmt(totalAmt.doubleValue());

        return ResponseEntity.ok(responseDto).getBody();
    }
}
