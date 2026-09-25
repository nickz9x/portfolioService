-- Garante que um mesmo asset_id nao seja cadastrado duas vezes.
--
-- Sem este indice, o guard do AssetService.findOrCreate (existsByassetId +
-- save) e apenas consultivo: sob READ_COMMITTED, duas transacoes concorrentes
-- leem "nao existe" antes de qualquer uma commitar e ambas inserem. Duas linhas
-- com o mesmo asset_id quebram toda leitura seguinte, porque findByAssetId
-- devolve Optional<Asset> e o Spring Data nao consegue montar um Optional a
-- partir de mais de uma linha (IncorrectResultSizeDataAccessException).
--
-- Escrito de forma idempotente porque o @Column(unique = true) da entidade nao
-- cria a constraint em tabela ja existente (o ddl-auto: update ignora), entao
-- esta migration pode rodar sobre um banco onde ela ja foi aplicada na mao.
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'uk_asset_asset_id') THEN
        ALTER TABLE asset ADD CONSTRAINT uk_asset_asset_id UNIQUE (asset_id);
    END IF;
END $$;
