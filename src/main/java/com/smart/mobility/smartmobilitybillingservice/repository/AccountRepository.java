package com.smart.mobility.smartmobilitybillingservice.repository;

import com.smart.mobility.smartmobilitybillingservice.model.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AccountRepository extends JpaRepository<Account, Long> {

    Optional<Account> findByUserId(String userId);

    boolean existsByUserId(String userId);
}
