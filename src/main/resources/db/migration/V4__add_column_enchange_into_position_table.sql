-- A V1 ja cria a coluna em banco vazio. O if not exists torna a migration
-- idempotente para o banco de dev, que ganhou a coluna pelo ddl-auto antes
-- de o Flyway entrar no projeto.
alter table position add column if not exists exchange varchar(255);
