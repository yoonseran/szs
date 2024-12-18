package com.szs.szsapi.dto;

import com.szs.szsapi.entity.Customer;
import lombok.Getter;
import lombok.Setter;
import org.springframework.http.ResponseEntity;

@Getter
@Setter
public class CustomerResponseDto {
    private Long cuIdx;
    private String userId;
    private String name;

    public CustomerResponseDto(Customer customer) {
        this.cuIdx = customer.getCuIdx();
        this.userId = customer.getUserId();
        this.name = customer.getName();
    }
}
