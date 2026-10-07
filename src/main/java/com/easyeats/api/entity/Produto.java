package com.easyeats.api.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Entity
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "tb_produto")
public class Produto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nome;

    private String descricao;

    @Column(nullable = false, updatable = false)
    private LocalDateTime dataCriacao;

    private LocalDateTime dataAlteracao;

    @Column(nullable = false)
    private String flativo;

    /*
     * Categoria do produto.
     *
     * Vários produtos podem pertencer
     * à mesma categoria.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "categoria_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Categoria categoria;

    /*
     * Ingredientes utilizados pelo produto.
     */
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @OneToMany(mappedBy = "produto")
    private List<ProdutoIngrediente> ingredientes = new ArrayList<>();

    /*
     * Histórico de preços do produto.
     *
     * Um produto pode possuir vários preços,
     * mas apenas um deve estar ativo.
     */
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @OneToMany(mappedBy = "produto", cascade = CascadeType.ALL)
    private List<Preco> precos = new ArrayList<>();

    @PrePersist
    public void antesDeCriar() {
        dataCriacao = LocalDateTime.now();
        dataAlteracao = LocalDateTime.now();
    }

    @PreUpdate
    public void antesDeAtualizar() {
        dataAlteracao = LocalDateTime.now();
    }
}