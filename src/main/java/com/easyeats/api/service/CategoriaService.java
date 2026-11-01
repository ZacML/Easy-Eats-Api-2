package com.easyeats.api.service;

import com.easyeats.api.dto.CategoriaDto;
import com.easyeats.api.entity.Categoria;
import com.easyeats.api.exception.RecursoNaoEncontradoException;
import com.easyeats.api.repository.CategoriaRepository;
import org.springframework.stereotype.Service;
import com.easyeats.api.dto.mapper.CategoriaMapper;

import java.util.List;

@Service
public class CategoriaService {

    private final CategoriaRepository categoriaRepository;

    public CategoriaService(CategoriaRepository categoriaRepository) {
        this.categoriaRepository = categoriaRepository;
    }

    public Categoria criar(CategoriaDto dto) {

        if (categoriaRepository.existsByNomeIgnoreCase(dto.nome())) {
            throw new IllegalArgumentException(
                    "Já existe uma categoria com esse nome");
        }

        Categoria categoria = CategoriaMapper.toEntity(dto);

        if (categoria.getFlativo() == null) {
            categoria.setFlativo("S");
        }

        return categoriaRepository.save(categoria);
    }

    // Listar categorias
    public List<Categoria> listar() {
        return categoriaRepository.findAll();
    }

    // Buscar categoria por ID
    public Categoria buscarPorId(Long id) {

        return categoriaRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Categoria", id));   // ALTERADO
    }

    // Editar categoria
    public Categoria editar(Long id, CategoriaDto dto) {

        Categoria categoria = buscarPorId(id);

        if (!categoria.getNome().equalsIgnoreCase(dto.nome())
                && categoriaRepository.existsByNomeIgnoreCase(dto.nome())) {

            throw new IllegalArgumentException(
                    "Já existe uma categoria com esse nome");
        }

        categoria.setNome(dto.nome());

        if (dto.flativo() != null) {
            categoria.setFlativo(dto.flativo());
        }

        return categoriaRepository.save(categoria);
    }

    // Inativar categoria
    public Categoria inativar(Long id) {

        Categoria categoria = buscarPorId(id);

        categoria.setFlativo("N");

        return categoriaRepository.save(categoria);
    }
}