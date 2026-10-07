package com.easyeats.api.service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.easyeats.api.dto.MesaDto;
import com.easyeats.api.dto.mapper.MesaMapper;
import com.easyeats.api.entity.Mesa;
import com.easyeats.api.repository.MesaRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MesaService {

    private final MesaRepository repository;
    private final MesaMapper mapper;

    @Transactional
    public MesaDto salvar(MesaDto dto) {
        repository.findByNumero(dto.numero()).ifPresent(mesa -> {
            throw new IllegalArgumentException(
                    "Já existe uma mesa cadastrada com o número " + dto.numero()
            );
        });

        Mesa mesa = mapper.toEntity(dto);
        mesa.setFlativo("S");

        return mapper.toDto(repository.save(mesa));
    }

    @Transactional
    public MesaDto alterar(Long id, MesaDto dto) {
        Optional<Mesa> existente = repository.findById(id);

        if (existente.isEmpty()) {
            return null;
        }

        repository.findByNumero(dto.numero()).ifPresent(mesa -> {
            if (!mesa.getId().equals(id)) {
                throw new IllegalArgumentException(
                        "Já existe uma mesa cadastrada com o número " + dto.numero()
                );
            }
        });

        Mesa mesa = existente.get();
        mesa.setNumero(dto.numero());

        return mapper.toDto(repository.save(mesa));
    }

    @Transactional(readOnly = true)
    public List<MesaDto> listarAtivas() {
        return repository.findByFlativo("S")
                .stream()
                .map(mapper::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public Optional<MesaDto> buscarPorId(Long id) {
        return repository.findById(id)
                .filter(mesa -> "S".equals(mesa.getFlativo()))
                .map(mapper::toDto);
    }

    @Transactional
    public void excluir(Long id) {
        repository.findById(id).ifPresent(mesa -> {
            mesa.setFlativo("N");
            repository.save(mesa);
        });
    }
}
