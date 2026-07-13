---
name: Sudolife
description: Controle de treino direto, intuitivo e sempre em movimento.
colors:
  athlete-green: "#1e7f5c"
  athlete-green-deep: "#1b654c"
  strava-orange: "#fc4c02"
  deep-ink: "#17211b"
  body-ink: "#25322c"
  muted-ink: "#4c5b54"
  label-ink: "#52645c"
  canvas: "#f7f8f3"
  surface: "#ffffff"
  surface-soft: "#f8faf8"
  plan-surface: "#f4f8f4"
  border: "#dbe3df"
  control-border: "#c9d3cd"
  plan-border: "#b7c8bc"
  error: "#a43122"
  disabled: "#a6b7ae"
typography:
  display:
    fontFamily: "Inter, ui-sans-serif, system-ui, -apple-system, BlinkMacSystemFont, Segoe UI, sans-serif"
    fontSize: "2.75rem"
    fontWeight: 700
    lineHeight: 1.05
  headline:
    fontFamily: "Inter, ui-sans-serif, system-ui, sans-serif"
    fontSize: "2rem"
    fontWeight: 700
    lineHeight: 1.12
  title:
    fontFamily: "Inter, ui-sans-serif, system-ui, sans-serif"
    fontSize: "1.1rem"
    fontWeight: 800
    lineHeight: 1.3
  body:
    fontFamily: "Inter, ui-sans-serif, system-ui, sans-serif"
    fontSize: "1rem"
    fontWeight: 400
    lineHeight: 1.55
  label:
    fontFamily: "Inter, ui-sans-serif, system-ui, sans-serif"
    fontSize: "0.75rem"
    fontWeight: 800
    lineHeight: 1.3
    letterSpacing: "0"
rounded:
  control: "8px"
spacing:
  xs: "6px"
  sm: "8px"
  md: "12px"
  lg: "18px"
  xl: "28px"
  shell: "32px"
components:
  button-primary:
    backgroundColor: "{colors.athlete-green}"
    textColor: "{colors.surface}"
    typography: "{typography.label}"
    rounded: "{rounded.control}"
    padding: "0 18px"
    height: "48px"
  button-integration:
    backgroundColor: "{colors.strava-orange}"
    textColor: "{colors.surface}"
    typography: "{typography.label}"
    rounded: "{rounded.control}"
    padding: "0 18px"
    height: "42px"
  button-secondary:
    backgroundColor: "{colors.surface}"
    textColor: "{colors.body-ink}"
    typography: "{typography.label}"
    rounded: "{rounded.control}"
    padding: "0 18px"
    height: "42px"
  input:
    backgroundColor: "{colors.surface}"
    textColor: "{colors.deep-ink}"
    typography: "{typography.body}"
    rounded: "{rounded.control}"
    padding: "0 12px"
    height: "42px"
  panel:
    backgroundColor: "{colors.surface}"
    textColor: "{colors.body-ink}"
    rounded: "{rounded.control}"
    padding: "18px"
---

# Design System: Sudolife

## 1. Overview

**Creative North Star: "Treino em movimento"**

O Sudolife deve parecer uma ferramenta esportiva em uso, não um relatório corporativo. A interface dá ritmo às decisões do atleta por meio de hierarquia curta, estados claros e agrupamentos que transformam dados de treino em próximos passos compreensíveis.

O sistema é firme e intuitivo. A familiaridade dos controles permite agir sem hesitação, enquanto o verde próprio do Sudolife cria personalidade e o laranja identifica exclusivamente a integração com o Strava. Densidade é aceita quando ajuda o atleta, mas nunca sem organização ou prioridade explícita.

**Key Characteristics:**

- Hierarquia direta e orientada à próxima ação.
- Superfícies planas separadas por bordas e tons sutis.
- Componentes compactos, firmes e consistentes.
- Identidade verde própria, com laranja reservado à integração.
- Layout responsivo que reorganiza a estrutura sem reduzir a legibilidade.

## 2. Colors

A paleta combina verdes naturais de desempenho com neutros levemente esverdeados, mantendo o laranja como sinal reconhecível e restrito do Strava.

### Primary

- **Verde Atleta** (`#1e7f5c`): ações primárias do Sudolife, foco e estados que representam progresso sob controle.
- **Verde Atleta Profundo** (`#1b654c`): links e variações de maior contraste da identidade principal.

### Secondary

- **Laranja Strava** (`#fc4c02`): somente ações, estados e vínculos cuja origem seja explicitamente o Strava.

### Neutral

- **Tinta Profunda** (`#17211b`): títulos, valores e informações de maior prioridade.
- **Tinta de Corpo** (`#25322c`): texto de controles e conteúdo funcional.
- **Tinta Atenuada** (`#4c5b54`): explicações e resumos secundários.
- **Tinta de Rótulo** (`#52645c`): metadados e rótulos compactos.
- **Campo de Treino** (`#f7f8f3`): fundo geral da aplicação.
- **Superfície Clara** (`#ffffff`): controles e áreas de conteúdo.
- **Névoa de Treino** (`#f8faf8`): estados vazios e superfícies secundárias.
- **Superfície de Plano** (`#f4f8f4`): orientação de treino e planejamento conservador.
- **Divisor Suave** (`#dbe3df`): contorno de painéis e itens.
- **Contorno de Controle** (`#c9d3cd`): borda de campos e botões neutros.
- **Contorno de Plano** (`#b7c8bc`): separação reforçada de recomendações conservadoras.
- **Erro Terroso** (`#a43122`): falhas e mensagens que exigem correção.
- **Estado Indisponível** (`#a6b7ae`): controles desabilitados.

### Named Rules

**The Integration Color Rule.** O laranja `#fc4c02` pertence ao Strava; ações nativas do Sudolife usam o Verde Atleta `#1e7f5c`.

**The Controlled Energy Rule.** A personalidade vem da hierarquia, dos estados e do verde de identidade, não de saturar todas as superfícies.

## 3. Typography

**Display Font:** Inter com `ui-sans-serif` e fontes de sistema como fallback  
**Body Font:** Inter com `ui-sans-serif` e fontes de sistema como fallback  
**Label Font:** Inter com `ui-sans-serif` e fontes de sistema como fallback

**Character:** Uma única família sans-serif mantém a experiência rápida e familiar. Peso e escala criam direção sem transformar a interface de produto em uma peça publicitária.

### Hierarchy

- **Display** (700, `2.75rem`, `1.05`): título principal do painel.
- **Headline** (700, `2rem`, `1.12`): títulos de fluxos focados e retornos de integração.
- **Title** (800, `1.1rem`, `1.3`): títulos internos e estados importantes.
- **Body** (400, `1rem`, `1.55`): instruções, resumos e conteúdo; limitar prosa contínua a 65–75 caracteres por linha.
- **Label** (800, `0.75rem`, sem espaçamento adicional, caixa alta quando necessário): metadados, filtros e nomes de métricas.

### Named Rules

**The One Voice Rule.** Uma única família tipográfica atende títulos, dados, controles e texto; a hierarquia vem de peso e tamanho, nunca de fontes decorativas.

## 4. Elevation

O sistema não usa sombras. Profundidade é comunicada por camadas tonais, bordas de 1 px e agrupamento espacial. Superfícies permanecem estáveis e planas para que estados, conteúdo e ações tenham prioridade.

### Named Rules

**The Flat-by-Default Rule.** Não adicionar sombras decorativas a painéis ou botões; criar separação com `#ffffff`, `#f8faf8`, `#f4f8f4` e bordas adequadas ao contexto.

## 5. Components

### Buttons

- **Shape:** retangular compacto com cantos de `8px`.
- **Primary:** Verde Atleta `#1e7f5c`, texto branco, altura de `48px` nos formulários de autenticação e peso 800.
- **Hover / Focus:** manter mudança de estado curta, entre 150 e 250 ms, com foco visível equivalente ao anel verde dos campos; não deslocar layout.
- **Secondary:** superfície branca, texto `#25322c`, borda `#c9d3cd`, altura mínima de `42px`.
- **Integration:** Laranja Strava `#fc4c02`, texto branco e uso exclusivo em ações da integração.
- **Disabled / Loading:** reduzir ênfase sem remover legibilidade; bloquear interação e comunicar a ação em andamento pelo rótulo.

### Cards / Containers

- **Corner Style:** raio único de `8px`.
- **Background:** branco para conteúdo, `#f8faf8` para estados vazios e `#f4f8f4` para planos conservadores.
- **Shadow Strategy:** nenhuma sombra; usar borda sólida de 1 px.
- **Border:** `#dbe3df` como padrão e `#b7c8bc` para planos conservadores.
- **Internal Padding:** entre `16px` e `22px`; `18px` é o padrão dos painéis.

### Inputs / Fields

- **Style:** superfície branca, borda `#c9d3cd`, raio de `8px`, altura mínima de `42px` e texto `#17211b`.
- **Focus:** borda Verde Atleta e anel de 3 px com `rgba(30, 127, 92, 0.16)`.
- **Error / Disabled:** Erro Terroso para mensagens; Estado Indisponível e cursor coerente para controles bloqueados.

### Navigation

- **Style:** a navegação atual é orientada por rotas e ações textuais. Links usam Verde Atleta Profundo, peso 800 e foco visível; novos padrões de navegação devem preservar a mesma linguagem de controles.
- **Responsive:** abaixo de `680px`, cabeçalhos, ações, paginação, filtros e itens de atividade passam para uma única coluna.

### Activity List

- Cada atividade é uma linha de conteúdo com dois grupos funcionais em telas largas e uma coluna no mobile.
- Métricas usam rótulos compactos e valores de alto contraste; a lista deve permitir comparação rápida sem virar uma grade de cartões idênticos.

## 6. Do's and Don'ts

### Do:

- **Do** usar Verde Atleta `#1e7f5c` para ações primárias nativas do Sudolife.
- **Do** deixar clara a próxima decisão do atleta antes de apresentar detalhes complementares.
- **Do** organizar informações densas com hierarquia, espaço e rótulos consistentes.
- **Do** usar `8px` como raio padrão e `18px` como preenchimento recorrente de painéis.
- **Do** manter controles familiares, estados completos e reorganização estrutural abaixo de `680px`.

### Don't:

- **Don't** fazer o Sudolife parecer um painel corporativo.
- **Don't** apresentar muitas informações sem organização ou prioridade explícita.
- **Don't** usar um visual infantil, genérico, neutro ou sem personalidade.
- **Don't** aplicar o Laranja Strava `#fc4c02` a ações próprias do Sudolife.
- **Don't** criar grades repetitivas de cartões quando uma lista comparável comunica melhor.
- **Don't** adicionar sombras decorativas, gradientes em texto, glassmorphism ou bordas laterais coloridas.
- **Don't** inventar controles incomuns apenas para criar personalidade.
