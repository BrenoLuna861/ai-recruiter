package com.airecruiter.controller;

import com.airecruiter.dto.response.ExternalJobResponse;
import com.airecruiter.service.ExternalJobService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Vagas de fontes externas.
 *
 * O Spring atua como intermediario de proposito: as chaves da Adzuna ficam no
 * servidor. Se o front chamasse a API direto, qualquer pessoa as extrairia do
 * JavaScript e consumiria a cota mensal da conta.
 */
@RestController
@RequestMapping("/api/jobs/external")
@RequiredArgsConstructor
public class ExternalJobController {

    private final ExternalJobService externalJobService;

    @GetMapping
    public ResponseEntity<Map<String, Object>> buscar(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String local,
            @RequestParam(defaultValue = "false") boolean remotas,
            @RequestParam(defaultValue = "1") int pagina) {

        List<ExternalJobResponse> vagas = externalJobService.buscar(q, local, remotas, pagina);

        return ResponseEntity.ok(Map.of(
                "jobs", vagas,
                "total", vagas.size(),
                // A tela usa isto para avisar que esta vendo o Remotive (so remotas,
                // em ingles) em vez da Adzuna, em vez de o usuario estranhar o resultado.
                "source", externalJobService.adzunaConfigurada() ? "Adzuna" : "Remotive"
        ));
    }
}
