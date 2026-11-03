package com.easyeats.api.service;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.client.RestTemplate;

import com.easyeats.api.entity.DispositivoPush;
import com.easyeats.api.repository.DispositivoPushRepository;

@Service
public class PushService {

    private static final Logger log = LoggerFactory.getLogger(PushService.class);
    private static final String EXPO_URL = "https://exp.host/--/api/v2/push/send";

    private final DispositivoPushRepository repository;
    private final RestTemplate http;

    public PushService(DispositivoPushRepository repository) {
        this.repository = repository;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(3000);
        factory.setReadTimeout(5000);
        this.http = new RestTemplate(factory);
    }

    public void notificarPedidoPronto(Long pedidoId, String mesa) {
        List<String> tokens = repository.findAll().stream().map(DispositivoPush::getToken).toList();
        if (tokens.isEmpty()) return;

        Runnable envio = () -> enviar(tokens, pedidoId, mesa);

        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    envio.run();
                }
            });
        } else {
            envio.run();
        }
    }

    @SuppressWarnings("unchecked")
    private void enviar(List<String> tokens, Long pedidoId, String mesa) {
        try {
            String corpo = "🍔 O pedido #" + pedidoId + " da " + mesa
                    + " está pronto! Pode chamar o cliente e entregar essa delícia!";

            List<Map<String, Object>> mensagens = tokens.stream().map(t -> Map.<String, Object>of(
                    "to", t,
                    "title", "🔔 Pedido prontinho!",
                    "body", corpo,
                    "sound", "default",
                    "channelId", "pedidos",
                    "data", Map.of("pedidoId", pedidoId)
            )).toList();

            Map<String, Object> resposta = http.postForObject(EXPO_URL, mensagens, Map.class);
            if (resposta == null || !(resposta.get("data") instanceof List<?> tickets)) return;

            for (int i = 0; i < tickets.size() && i < tokens.size(); i++) {
                Map<String, Object> ticket = (Map<String, Object>) tickets.get(i);
                Map<String, Object> detalhes = (Map<String, Object>) ticket.get("details");
                if ("error".equals(ticket.get("status")) && detalhes != null
                        && "DeviceNotRegistered".equals(detalhes.get("error"))) {
                    repository.removerPorToken(tokens.get(i));
                }
            }
        } catch (Exception e) {
            log.warn("Falha ao enviar push do pedido {}: {}", pedidoId, e.getMessage());
        }
    }
}