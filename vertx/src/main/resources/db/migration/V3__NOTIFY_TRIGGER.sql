CREATE OR REPLACE FUNCTION notifyPessoaCreated()
    RETURNS TRIGGER AS $$
DECLARE newPessoa jsonb;
BEGIN
    newPessoa := row_to_json(NEW);
    PERFORM pg_notify('cacheChannel', newPessoa::text);
    RETURN NEW;
END
$$ LANGUAGE plpgsql;

CREATE TRIGGER notifyCreated
    AFTER INSERT ON pessoa
    FOR EACH ROW EXECUTE PROCEDURE notifyPessoaCreated();