# Modulos e Fronteiras

## Regra de dependencias
interfaces -> application -> domain

infrastructure implementa adaptadores e componentes tecnicos, sem regra critica de negocio.

## Responsabilidades por camada
- domain: entidades e enums de negocio, sem dependencia de framework
- pplication: casos de uso, DTOs de entrada/saida e contratos de servico
- infrastructure: seguranca, filtros, observabilidade, rate limit stub, configuracao
- interfaces: controllers REST e adaptacao HTTP

## Fronteiras por modulo
- uth: autenticacao, refresh, logout, identidade atual
- acancies: busca/listagem/detalhe de vagas
- matching: contrato deterministico de score e analise
- esumes: cadastro de curriculo e variantes
- pplications: rascunho e tracking inicial de candidatura
- shared: contratos compartilhados (paginacao, erro), auditoria e infraestrutura comum

## Decisoes arquiteturais
- RBAC agora (USER, ADMIN), ABAC adiado para Bloco 2
- Contratos definidos antes de regra final
- Ownership por userId previsto nos agregados do candidato
- Pontos de extensao para persistencia e rate limiting mantidos como portas/stubs