-- Indice para PortfolioRepository.findByOwnerId (listar os portfolios de um
-- usuario). Sem ele a consulta vira full scan na tabela portfolio.
--
-- Nao e UNIQUE de proposito: o codigo de hoje permite varios portfolios por
-- usuario — createPortfolio nao checa duplicata e findByOwnerId devolve List.
-- Se a regra um dia virar "1 portfolio por usuario", isso e uma migration nova
-- com UNIQUE (owner_id), e este indice fica redundante.
CREATE INDEX IF NOT EXISTS idx_portfolio_owner_id ON portfolio (owner_id);
