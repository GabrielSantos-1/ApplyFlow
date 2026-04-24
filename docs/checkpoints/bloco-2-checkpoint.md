# Checkpoint Técnico - Bloco 2

## Status
- Fase: Core funcional com segurança aplicada
- Estado geral: parcialmente validado (frontend build validado; backend sem `mvn` no ambiente)

## O que passou de stub para real
- Auth com login real, BCrypt, JWT com validade, refresh rotativo e revogação persistida
- Persistência JPA em auth/vacancies/resumes/applications/matching
- Serviços de use case com ownership aplicado
- Matching determinístico com breakdown e recommendation
- Applications com transições de status controladas + tracking persistido
- Rate limiting efetivo por filtro HTTP em rotas críticas
- Auditoria persistida em eventos críticos

## Itens concluídos do Bloco 2
1. [x] Auth funcional com JWT + refresh rotativo
2. [x] RBAC funcional
3. [x] Ownership aplicado em recursos do usuário
4. [x] Vacancies list/detail com paginação e filtros controlados
5. [x] Resumes e variants com persistência
6. [x] Matching determinístico implementado e persistido
7. [x] Applications draft/status funcional
8. [x] Rate limiting ativo nos pontos críticos
9. [x] Testes mínimos críticos adicionados
10. [x] Documentação e context atualizados
11. [ ] Backend compilando no ambiente atual (`mvn` ausente)
12. [x] Sem secret hardcoded
13. [x] Arquitetura do Bloco 1 preservada

## Riscos aceitos temporariamente
- Rate limiting em memória (não distribuído)
- Sanitização textual mínima (não substitui política completa de renderização no frontend)
- Fluxo de matching usa fallback de dados quando perfil ainda não estiver completo

## Próximos passos (Bloco 3)
1. Substituir rate limit em memória por backend distribuído (ex.: Redis)
2. Completar testes de integração de ponta a ponta para todos os fluxos críticos
3. Finalizar política de renderização segura para textos livres na UI
4. Ampliar cobertura de perfil do candidato para melhorar qualidade do matching