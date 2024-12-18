package com.szs.szsapi.repository;

import com.szs.szsapi.dto.DeductionJqueryResponse;
import com.szs.szsapi.entity.Deduction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface DeductionRepository extends JpaRepository<Deduction, Long> {
    @Query(value = "SELECT itAmt FROM It_Deduction WHERE cuIdx = :cuIdx")
    List<DeductionJqueryResponse> findByIdx(@Param(value = "cuIdx") Long cuIdx);
}
