package com.easyeats.api.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "tb_estoques")
@AllArgsConstructor
@NoArgsConstructor
public class Estoque {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Integer qtdAtual;

    @Column(nullable = false)
    private Integer qtdMinima;

    @Column(nullable = false)
    private LocalDateTime dtAtualizacao;

    // dentro da classe Estoque, junto dos outros campos
    @Version
    private Long versao;

    @OneToOne(optional = false)
    @JoinColumn(
            name = "ingrediente_id",
            nullable = false,
            unique = true
    )
    private Ingrediente ingrediente;

    @PrePersist
    public void antesDeCriar() {
        dtAtualizacao = LocalDateTime.now();
    }

    @PreUpdate
    public void antesDeAtualizar() {
        dtAtualizacao = LocalDateTime.now();
    }
}