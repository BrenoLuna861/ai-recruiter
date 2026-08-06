<template>
  <section class="amostra">
    <header class="amostra-head">
      <p class="amostra-eyebrow">Exemplo de resultado</p>
      <h2>Você não recebe só uma nota. Recebe o motivo dela.</h2>
      <p class="amostra-sub">
        Cada dimensão vem com a justificativa que a levou àquela pontuação, citando
        o que está escrito no seu currículo. E a nota final é uma conta que você
        pode conferir.
      </p>
    </header>

    <div class="painel">
      <div class="painel-topo">
        <div>
          <span class="arquivo">curriculo_ana_silva.pdf</span>
          <span class="rotulo-exemplo">dados de exemplo</span>
        </div>
        <span class="nota-geral">{{ notaGeral }}/100</span>
      </div>

      <div class="grade">
        <article v-for="d in dimensoes" :key="d.rotulo" class="dim">
          <div class="dim-topo">
            <span class="dim-nome">{{ d.rotulo }}</span>
            <span class="dim-peso">peso {{ d.peso }}%</span>
          </div>
          <div class="barra"><div class="barra-fill" :style="{ width: d.nota + '%' }"></div></div>
          <div class="dim-nota">{{ d.nota }}</div>
          <p class="dim-just">{{ d.justificativa }}</p>
        </article>
      </div>

      <div class="conta">
        <span class="conta-titulo">Como chegamos em {{ notaGeral }}</span>
        <div class="conta-linhas">
          <span v-for="d in dimensoes" :key="d.rotulo">
            {{ d.rotulo }} {{ d.nota }} × {{ d.peso }}% = {{ (d.nota * d.peso / 100).toFixed(1) }}
          </span>
          <span class="conta-total">Total {{ notaGeral }}</span>
        </div>
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
/*
  Dados de exemplo, nao uma analise real de usuario — dai o selo "dados de
  exemplo" visivel no painel. Inventar um depoimento ou numero de usuarios seria
  desonesto; mostrar o formato do resultado com um caso fabricado e declarado
  como tal e apenas ilustrar o produto.

  Os pesos e a conta sao os mesmos do backend (ResumeService.calcularNotaGeral),
  entao o que aparece aqui e exatamente o que a pessoa recebe.
*/
const dimensoes = [
  {
    rotulo: 'Skills',
    nota: 92,
    peso: 35,
    justificativa: 'Domínio técnico bem evidenciado: React, Node.js e PostgreSQL aparecem aplicados em projetos descritos, não apenas listados.'
  },
  {
    rotulo: 'Experiência',
    nota: 88,
    peso: 30,
    justificativa: 'Progressão clara em cinco anos, mas as entregas raramente vêm com números — falta dimensionar o impacto do trabalho.'
  },
  {
    rotulo: 'ATS',
    nota: 83,
    peso: 20,
    justificativa: 'Estrutura legível por sistemas de triagem, embora faltem termos como "metodologias ágeis" e "CI/CD", comuns nas vagas da área.'
  },
  {
    rotulo: 'Formato',
    nota: 80,
    peso: 15,
    justificativa: 'Duas páginas bem organizadas; o resumo profissional no topo está longo e dilui a mensagem principal.'
  }
]

const notaGeral = Math.round(
  dimensoes.reduce((soma, d) => soma + d.nota * d.peso, 0) / 100
)
</script>

<style scoped>
.amostra { max-width: 1000px; margin: 0 auto; padding: 24px 24px 80px; }

.amostra-head { text-align: center; max-width: 620px; margin: 0 auto 36px; }
.amostra-eyebrow {
  font-size: var(--text-xs); letter-spacing: 0.16em; text-transform: uppercase;
  color: var(--accent); margin-bottom: 12px;
}
.amostra-head h2 { margin-bottom: 14px; }
.amostra-sub { font-size: var(--text-base); line-height: 1.75; color: var(--text-muted); }

.painel {
  background: var(--bg-2);
  border: 1px solid var(--border);
  border-radius: var(--radius-lg);
  overflow: hidden;
}

.painel-topo {
  display: flex; align-items: center; justify-content: space-between;
  gap: 16px; padding: 20px 24px; border-bottom: 1px solid var(--border);
  flex-wrap: wrap;
}
.arquivo { font-family: var(--font-mono); font-size: var(--text-sm); color: var(--text); }
.rotulo-exemplo {
  margin-left: 10px; font-size: 10px; letter-spacing: 0.1em; text-transform: uppercase;
  color: var(--text-faint); border: 1px solid var(--border-2);
  border-radius: 999px; padding: 2px 8px;
}
.nota-geral {
  font-family: var(--font-mono); font-size: var(--text-2xl); font-weight: 600;
  color: var(--accent); background: var(--accent-dim);
  border-radius: var(--radius); padding: 4px 14px;
}

.grade {
  display: grid; grid-template-columns: repeat(auto-fit, minmax(230px, 1fr));
  gap: 1px; background: var(--border);
}
.dim { background: var(--bg-2); padding: 20px 22px; }
.dim-topo { display: flex; align-items: baseline; justify-content: space-between; gap: 8px; margin-bottom: 10px; }
.dim-nome { font-size: var(--text-xs); letter-spacing: 0.1em; text-transform: uppercase; color: var(--text-muted); }
.dim-peso { font-size: 10px; color: var(--text-faint); }
.barra { height: 4px; background: var(--border-2); border-radius: 2px; overflow: hidden; margin-bottom: 8px; }
.barra-fill { height: 100%; background: var(--accent); border-radius: 2px; }
.dim-nota { font-family: var(--font-mono); font-size: var(--text-xl); color: var(--accent); margin-bottom: 10px; }
.dim-just { font-size: var(--text-xs); line-height: 1.65; color: var(--text-muted); margin: 0; }

.conta { padding: 18px 24px; border-top: 1px solid var(--border); background: var(--bg-3); }
.conta-titulo { font-size: var(--text-xs); color: var(--text-muted); display: block; margin-bottom: 10px; }
.conta-linhas {
  display: flex; flex-wrap: wrap; gap: 6px 20px;
  font-family: var(--font-mono); font-size: 12px; color: var(--text-faint);
}
.conta-total { color: var(--accent); }

@media (max-width: 600px) {
  .amostra { padding: 16px 16px 56px; }
  .painel-topo { padding: 16px; }
  .dim { padding: 16px; }
  .conta { padding: 16px; }
}
</style>
