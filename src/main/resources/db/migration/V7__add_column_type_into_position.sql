-- A V1 ja cria a coluna em banco vazio. O if not exists torna a migration
-- idempotente para o banco de dev, que ganhou a coluna pelo ddl-auto antes
-- de o Flyway entrar no projeto. O varchar(255) casa com a V1 e com a
-- entidade; a versao antiga criava varchar(25). O check da V1 entra na V8,
-- porque aqui ele pode ja existir.
alter table position add column if not exists type varchar(255);
