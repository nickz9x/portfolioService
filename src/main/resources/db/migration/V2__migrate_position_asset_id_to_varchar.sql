-- Adota o banco legado (criado pelo ddl-auto, sem historico de migration) na
-- modelagem nova.
--
-- No banco antigo, position.asset_id era bigint com FK para asset(id): a
-- posicao apontava para a linha da tabela asset. Agora asset_id guarda o
-- identificador externo do ativo (ex.: "bitcoin") e a tabela asset nao existe
-- mais. O Hibernate sozinho nao consegue fazer essa conversao: o
-- ALTER COLUMN ... TYPE varchar(255) esbarra na FK
-- (fke8a8t4lrwikmkw4rn3bg6c4yw) e o boot quebra.
--
-- Ordem: solta a FK, traduz o id interno para o asset_id externo pela propria
-- tabela asset, troca a coluna e so entao dropa a tabela.
--
-- O bloco so age quando position.asset_id ainda e bigint. Em banco novo, onde
-- a V1 ja criou a coluna como varchar, a migration inteira e no-op.
DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'position'
          AND column_name = 'asset_id'
          AND data_type = 'bigint'
    ) THEN
        EXECUTE 'ALTER TABLE position DROP CONSTRAINT IF EXISTS fke8a8t4lrwikmkw4rn3bg6c4yw';

        IF to_regclass('public.asset') IS NOT NULL THEN
            EXECUTE 'ALTER TABLE position ADD COLUMN asset_id_external varchar(255)';

            -- 1 -> "bitcoin", 2 -> "solama", 3 -> "solana".
            EXECUTE 'UPDATE position p SET asset_id_external = a.asset_id
                     FROM asset a WHERE p.asset_id = a.id';

            -- Orfaos (asset_id sem linha correspondente em asset) viram o
            -- proprio numero em texto, para nao perder a referencia.
            EXECUTE 'UPDATE position SET asset_id_external = asset_id::text
                     WHERE asset_id_external IS NULL';

            EXECUTE 'ALTER TABLE position DROP COLUMN asset_id';
            EXECUTE 'ALTER TABLE position RENAME COLUMN asset_id_external TO asset_id';
        ELSE
            EXECUTE 'ALTER TABLE position ALTER COLUMN asset_id TYPE varchar(255)
                     USING asset_id::text';
        END IF;
    END IF;
END $$;

DROP TABLE IF EXISTS asset;

-- Duas compras concorrentes do mesmo ativo no mesmo portfolio nao podem mais
-- criar duas linhas: a segunda transacao esbarra aqui.
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'uk_position_portfolio_asset'
    ) THEN
        EXECUTE 'ALTER TABLE position ADD CONSTRAINT uk_position_portfolio_asset
                 UNIQUE (portfolio_id, asset_id)';
    END IF;
END $$;
