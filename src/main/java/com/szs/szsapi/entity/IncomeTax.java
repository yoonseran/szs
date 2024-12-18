package com.szs.szsapi.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;

@Schema(description = "회원 소득 정보")
@Entity(name = "Cu_IncomeTax")
@Table
@Getter
@Setter
@NoArgsConstructor
public class IncomeTax {
    @Id
    @Schema(description = "회원 식별자")
    @Column(name="cuIdx")
    private Long cuIdx;
    @ColumnDefault("0")
    @Schema(description = "종합소득금액")
    @Column(name="incomeAmt")
    private Long incomeAmt;
    @ColumnDefault("0")
    @Schema(description = "세액공제")
    @Column(name="taxAmt")
    private Long taxAmt;
}
