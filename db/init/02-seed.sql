-- Dados iniciais obrigatórios.
-- POST /usuarios busca cada role por nome (roleRepository.findByNome(...).orElseThrow())
-- e nenhuma parte do código as cria; sem estas linhas, cadastrar usuário falha com HTTP 500.
-- Valores = enum com.easyeats.api.entity.enums.Role.

INSERT INTO role (nome)
SELECT v.nome
FROM (VALUES ('ADMIN'), ('GERENTE'), ('FUNCIONARIO')) AS v (nome)
WHERE NOT EXISTS (SELECT 1 FROM role r WHERE r.nome = v.nome);
