package com.airecruiter.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Rate limit generico de janela deslizante, em memoria.
 *
 * Os limites vem por parametro em vez de configuracao propria, para o mesmo bean
 * atender casos com politicas diferentes — a recuperacao de senha protege contra
 * enumeracao de contas, a busca de vagas protege a cota mensal da Adzuna.
 *
 * Suficiente enquanto o railway.json fixar numReplicas: 1. Com varias replicas,
 * cada uma teria seu proprio mapa e o limite efetivo viraria N vezes maior —
 * nesse cenario, trocar por Redis.
 */
@Slf4j
@Component
public class SlidingWindowRateLimiter {

    private final Map<String, Deque<Instant>> hits = new ConcurrentHashMap<>();

    /**
     * @param chave        identificador do consumidor (ex.: "vagas:ip:1.2.3.4")
     * @param maxNaJanela  quantas requisicoes sao permitidas na janela
     * @param minutos      tamanho da janela
     * @return true se pode seguir; false se estourou a cota
     */
    public boolean tryAcquire(String chave, int maxNaJanela, int minutos) {
        if (chave == null || chave.isBlank()) return true;

        Instant agora = Instant.now();
        Instant corte = agora.minusSeconds(minutos * 60L);

        Deque<Instant> janela = hits.computeIfAbsent(chave, k -> new ArrayDeque<>());
        synchronized (janela) {
            while (!janela.isEmpty() && janela.peekFirst().isBefore(corte)) {
                janela.pollFirst();
            }
            if (janela.size() >= maxNaJanela) {
                return false;
            }
            janela.addLast(agora);
            return true;
        }
    }

    /** Evita vazamento de memoria: descarta janelas que ficaram vazias. */
    @Scheduled(fixedDelay = 600_000)
    public void evictStale() {
        Instant corte = Instant.now().minusSeconds(3600);
        hits.entrySet().removeIf(e -> {
            Deque<Instant> w = e.getValue();
            synchronized (w) {
                while (!w.isEmpty() && w.peekFirst().isBefore(corte)) {
                    w.pollFirst();
                }
                return w.isEmpty();
            }
        });
    }
}
