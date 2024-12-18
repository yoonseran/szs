package com.szs.szsapi.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;

@Schema(description = "소득공제 정보")
@Entity(name = "It_Deduction")
@Table
@Getter
@Setter
@NoArgsConstructor
public class Deduction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Schema(description = "시퀀스")
    @Column(name="itSeq")
    private Long itSeq;
    @Schema(description = "회원 식별자")
    @Column(name="cuIdx")
    private Long cuIdx; //회원 식별자
    @Column(name="type", nullable = false, length = 1)
    @Schema(description = "소득공제구분 (1:국민연금, 2:신용카드)")
    private String type;
    @Column(name="itYear", nullable = false, length = 4)
    @Schema(description = "소득공제 연도")
    private String itYear;
    @Column(name="itMonth", nullable = false, length = 2)
    @Schema(description = "소득공제 월")
    private String itMonth;
    @Column(name="itAmt", scale=2)
    @ColumnDefault("0.00")
    @Schema(description = "소득공제 금액")
    private double itAmt;
}
