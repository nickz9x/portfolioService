-- A V1 ja cria a coluna em banco vazio. O if not exists torna a migration
-- idempotente para o banco de dev, que ganhou a coluna pelo ddl-auto antes
-- de o Flyway entrar no projeto. O varchar(255) casa com a V1 e com a
-- entidade; a versao antiga criava varchar(10) e estourava com symbol maior.
alter table position add column if not exists symbol varchar(255);
