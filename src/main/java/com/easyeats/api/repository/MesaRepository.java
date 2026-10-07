package com.easyeats.api.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.easyeats.api.entity.Mesa;

public interface MesaRepository extends JpaRepository<Mesa, Long> {

    List<Mesa> findByFlativo(String flativo);

    Optional<Mesa> findByNumero(Integer numero);
}
