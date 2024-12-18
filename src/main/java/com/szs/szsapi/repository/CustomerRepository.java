package com.szs.szsapi.repository;

import com.szs.szsapi.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CustomerRepository extends JpaRepository<Customer, Long> {

    Optional<Customer> findByUserId(String userId);

    Optional<Customer> findByRegNo(String regNo);

//    @Query("UPDATE SET cc.lastLoginDate = : FROM Cu_Customer cc WHERE cc.cuIdx = :cuIdx")
//    void saveLastLoginDateByCuIdx(Long cuIdx);
}
