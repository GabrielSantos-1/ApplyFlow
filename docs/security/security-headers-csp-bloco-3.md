# Security Headers e CSP - Bloco 3

## Implementacao
Centralizada em `SecurityConfig` e parametrizada por `SecurityProperties`.

## Headers ativos
- `Content-Security-Policy`
- `X-Content-Type-Options: nosniff`
- `Referrer-Policy: strict-origin-when-cross-origin`
- `Permissions-Policy: camera=(), microphone=(), geolocation=(), payment=()`
- `X-Frame-Options: DENY`
- `Strict-Transport-Security` (apenas quando `security.headers.hsts-enabled=true`)

## CSP por ambiente
- `security.headers.mode=dev`: politica mais permissiva para DX local.
- `security.headers.mode=prod`: politica restritiva por default.

Diretivas cobertas:
- `default-src`
- `script-src`
- `style-src`
- `img-src`
- `connect-src`
- `frame-ancestors`

## Trade-offs
- CSP restritiva reduz risco de XSS, mas exige revisao quando frontend incluir novas origens externas.
- HSTS em ambiente sem HTTPS pode causar comportamento indesejado; por isso esta sob flag.
