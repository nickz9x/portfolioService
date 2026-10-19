-- Fecha a divergencia que as V4-V7 antigas criaram no banco de dev:
-- symbol ficou varchar(10) e type ficou varchar(25) sem o check da V1.
-- Em banco novo, a V1 ja criou tudo certo e as duas operacoes sao no-op.
alter table position alter column symbol type varchar(255);

do $$
begin
    if not exists (
        select 1
        from pg_constraint
        where conname = 'position_type_check'
          and conrelid = 'position'::regclass
    ) then
        alter table position
            add constraint position_type_check check (type in ('CRIPTO', 'STOCK'));
    end if;
end
$$;
