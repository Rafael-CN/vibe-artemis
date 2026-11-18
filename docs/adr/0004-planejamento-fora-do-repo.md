# ADR 0004 — Planejamento e backlog fora do repositório público

- **Status:** aceito
- **Data:** 2026-11-18
- **Decisor:** Rafael (dono do fork)
- **Supersede em parte:** [0001](0001-versionar-doc-de-agentes.md)

## Contexto

O repositório é público e faz parte do portfólio. Os documentos de planejamento (catálogo
de propostas, backlog, planos por fatia, épicos, fotos de verificação e brainstorms de
design abandonados) somavam mais de 100 arquivos e vários MB. Eles são úteis para trabalhar,
mas são ruído para quem visita o projeto.

## Decisão

Esses documentos saem do repositório público e passam a viver no repositório privado
`Rafael-CN/vibe-artemis-notes`. Continuam versionados aqui: `docs/reference/`,
`docs/guides/`, `docs/adr/`, `docs/maps/`, `docs/glossary.md`, a base de design
(`docs/design/DESIGN-SYSTEM.md`, `design-system.html`, `console/`) e a configuração de agentes
(`AGENTS.md`, `CLAUDE.md`, `.claude/`), que o CI e o gate de validação usam.

## Consequências

- O que o ADR 0001 dizia sobre `docs/` vale agora só para as pastas listadas acima.
- Referências antigas a `docs/planos/`, `docs/REPLANEJAMENTO.md` e similares, como as do
  ADR 0003, apontam para o repositório privado.
