-- A V1 cria type como varchar(255), mas o banco de dev herdou varchar(25)
-- da V7 antiga. A V8 corrigiu o tamanho do symbol e criou o check, mas
-- deixou o type em 25. Em banco novo isto e no-op.
alter table position alter column type type varchar(255);
