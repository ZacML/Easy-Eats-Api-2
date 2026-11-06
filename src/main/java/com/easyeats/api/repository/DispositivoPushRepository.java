package com.easyeats.api.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.easyeats.api.entity.DispositivoPush;

public interface DispositivoPushRepository extends JpaRepository<DispositivoPush, Long> {

    boolean existsByToken(String token);

    @Modifying
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Query("delete from DispositivoPush d where d.token = :token")
    void removerPorToken(@Param("token") String token);
}