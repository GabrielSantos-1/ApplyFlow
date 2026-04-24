# Bloco 1 - Overview

## Objetivo
Entregar fundacao arquitetural, contratual e de seguranca do MVP ApplyFlow/Job Copilot sem acoplamento prematuro.

## Stack oficial
- Backend: Java 21 + Spring Boot 3 + Maven
- Frontend: Next.js App Router + TypeScript
- Banco: PostgreSQL + Flyway
- Arquitetura: Monolito modular com separacao por camadas (domain, pplication, infrastructure, interfaces)

## Modulos backend
- auth
- vacancies
- matching
- resumes
- applications
- shared

## Escopo entregue no Bloco 1
- Estrutura de diretorios e modulos
- Entidades de dominio iniciais com UUID e auditoria
- Contratos REST (requests/responses) com Bean Validation
- Endpoints stubados em /api/v1/*
- Seguranca estrutural (stateless, JWT stub, RBAC inicial, exception handling, correlation id)
- Migration inicial Flyway
- Base frontend com paginas e clientes tipados

## Escopo intencionalmente fora
- Regras de negocio completas
- Persistencia JPA completa e mapeadores
- Rate limiting efetivo (somente desenho/stub)
- ABAC contextual
- Integracoes externas reais e scraping