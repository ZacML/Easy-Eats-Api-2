package com.easyeats.api.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Entity
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "tb_pedido")
public class Pedido {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, updatable = false)
    private LocalDateTime dataCriacao;

    private LocalDateTime dataAlteracao;

    private String cliente;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "mesa_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Mesa mesa;

    @Enumerated(EnumType.STRING)
    @Column
    private StatusPedidoEnum status;

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @OneToMany(mappedBy = "pedido", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ItemPedido> itens = new ArrayList<>();

    public void adicionarItem(ItemPedido item) {
        item.setPedido(this);
        itens.add(item);
    }

    @PrePersist
    public void antesDeCriar() {
        LocalDateTime agora = LocalDateTime.now();
        dataCriacao = agora;
        dataAlteracao = agora;

        if (status == null) {
            status = StatusPedidoEnum.ABERTO;
        }
    }

    @PreUpdate
    public void antesDeAtualizar() {
        dataAlteracao = LocalDateTime.now();

        if (status == null) {
            status = StatusPedidoEnum.ABERTO;
        }
    }
}
