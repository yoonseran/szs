package com.szs.szsapi.repository;

import com.szs.szsapi.dto.IncomeTaxJqueryResponse;
import com.szs.szsapi.entity.IncomeTax;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;


public interface IncomeTaxRepository extends JpaRepository<IncomeTax, Long> {
    @Query(value = "SELECT incomeAmt, taxAmt FROM Cu_IncomeTax WHERE cuIdx = :cuIdx")
    List<IncomeTaxJqueryResponse> findByIdx(@Param(value = "cuIdx") Long cuIdx);
}
