UPDATE oauth2_registered_client
SET redirect_uris = CASE
    WHEN redirect_uris LIKE '%http://127.0.0.1:8080/bff/login/oauth2/code/hermes-web-client%'
        THEN REPLACE(
            redirect_uris,
            'http://127.0.0.1:8080/bff/login/oauth2/code/hermes-web-client',
            'http://127.0.0.1:5173/bff/login/oauth2/code/hermes-web-client'
        )
    WHEN redirect_uris LIKE '%http://127.0.0.1:8090/login/oauth2/code/hermes-web-client%'
        THEN REPLACE(
            redirect_uris,
            'http://127.0.0.1:8090/login/oauth2/code/hermes-web-client',
            'http://127.0.0.1:5173/bff/login/oauth2/code/hermes-web-client'
        )
    WHEN redirect_uris IS NULL OR TRIM(redirect_uris) = ''
        THEN 'http://127.0.0.1:5173/bff/login/oauth2/code/hermes-web-client'
    WHEN redirect_uris NOT LIKE '%http://127.0.0.1:5173/bff/login/oauth2/code/hermes-web-client%'
        THEN CONCAT(redirect_uris, ',http://127.0.0.1:5173/bff/login/oauth2/code/hermes-web-client')
    ELSE redirect_uris
END
WHERE client_id = 'hermes-web-client';
