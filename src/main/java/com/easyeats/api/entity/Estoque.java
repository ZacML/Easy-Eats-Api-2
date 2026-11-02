package com.easyeats.api.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
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

    // Na mesma unidade do ingrediente (kg, g, ml, un...), com até 3 casas decimais
    @Column(nullable = false, precision = 12, scale = 3)
    private BigDecimal qtdAtual;

    @Column(nullable = false, precision = 12, scale = 3)
    private BigDecimal qtdMinima;

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