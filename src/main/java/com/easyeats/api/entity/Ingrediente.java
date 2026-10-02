package com.easyeats.api.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "tb_ingrediente")
@AllArgsConstructor
@NoArgsConstructor
public class Ingrediente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nome;

    @Column(nullable = false)
    private String unidadeMedida;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal custo;

    @Column(nullable = false, length = 1)
    private String flativo;

    @Column(nullable = false, updatable = false)
    private LocalDateTime dataCriacao;

    private LocalDateTime dataAlteracao;

    // Evita que o estoque seja incluído no toString() e nos métodos equals/hashCode,
    // prevenindo problemas de recursão infinita e comparações desnecessárias entre as entidades.
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @OneToOne(mappedBy = "ingrediente")
    private Estoque estoque;

    @PrePersist
    public void antesDeCriar() {
        dataCriacao = LocalDateTime.now();
        dataAlteracao = LocalDateTime.now();

        if (flativo == null) {
            flativo = "S";
        }
    }

    @PreUpdate
    public void antesDeAtualizar() {
        dataAlteracao = LocalDateTime.now();
    }
}