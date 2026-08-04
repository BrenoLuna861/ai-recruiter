package com.airecruiter.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Rate limit em memoria para /forgot-password, por IP.
 *
 * Janela deslizante simples, sem dependencia nova. Suficiente porque o
 * railway.json fixa numReplicas: 1. Se um dia escalar horizontalmente, cada
 * replica passa a ter seu proprio contador e o limite efetivo vira 5xN —
 * nesse momento troque por Redis / Bucket4j distribuido.
 */
@Slf4j
@Component
public class PasswordResetRateLimiter {

    @Value("${password-reset.rate-limit.max-per-window:5}")
    private int maxPerWindow;

    @Value("${password-reset.rate-limit.window-minutes:15}")
    private int windowMinutes;

    private final Map<String, Deque<Instant>> hits = new ConcurrentHashMap<>();

    /** @return true se o pedido pode seguir; false se estourou a cota. */
    public boolean tryAcquire(String key) {
        if (key == null || key.isBlank()) return true;

        Instant now = Instant.now();
        Instant cutoff = now.minusSeconds(windowMinutes * 60L);

        Deque<Instant> window = hits.computeIfAbsent(key, k -> new ArrayDeque<>());
        synchronized (window) {
            while (!window.isEmpty() && window.peekFirst().isBefore(cutoff)) {
                window.pollFirst();
            }
            if (window.size() >= maxPerWindow) {
                return false;
            }
            window.addLast(now);
            return true;
        }
    }

    /** Evita vazamento de memoria: descarta janelas que ficaram vazias. */
    @Scheduled(fixedDelay = 600_000)
    public void evictStale() {
        Instant cutoff = Instant.now().minusSeconds(windowMinutes * 60L);
        hits.entrySet().removeIf(e -> {
            Deque<Instant> w = e.getValue();
            synchronized (w) {
                while (!w.isEmpty() && w.peekFirst().isBefore(cutoff)) {
                    w.pollFirst();
                }
                return w.isEmpty();
            }
        });
    }
}
