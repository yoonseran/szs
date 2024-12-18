package com.szs.szsapi.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CustomerRequestDto {
    private String userId;
    private String password;
    private String name;
    private String regNo;
}
