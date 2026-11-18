# ADR 0005 — Configuração de agentes e mapas fora do repositório público

- **Status:** aceito
- **Data:** 2026-11-18
- **Decisor:** Rafael (dono do fork)
- **Supersede:** [0001](0001-versionar-doc-de-agentes.md) e o trecho de [0004](0004-planejamento-fora-do-repo.md) que mantinha esses arquivos aqui

## Contexto

O ADR 0004 tirou o planejamento do repositório público, mas manteve `AGENTS.md`,
`CLAUDE.md`, `.claude/` e `docs/maps/` porque o CI dependia deles. Para um repositório de
portfólio, eles ainda são ruído.

## Decisão

- `AGENTS.md`, `CLAUDE.md` e `.claude/` vão para o repositório privado
  `Rafael-CN/vibe-artemis-notes` e entram no `.gitignore` daqui.
- `docs/maps/` deixa de ser versionado. Quem precisa dos mapas roda
  `node tools/codemap/codemap.mjs` localmente.
- O CI deixa de rodar `codemap --check` e `tools/test-hooks.mjs`. O `tools/validate.mjs`
  pula o estágio `codemap` quando `docs/maps/` não existe.

## Consequências

- Trocar de máquina exige clonar o `vibe-artemis-notes` e copiar os arquivos para a raiz.
- Os mapas podem ficar defasados sem que o CI acuse. O hook local de encerramento continua
  rodando o `validate.mjs` completo, inclusive o `codemap`.
