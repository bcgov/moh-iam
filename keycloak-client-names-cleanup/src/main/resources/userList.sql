select id,realm_id
    from user_entity
where (
        REGEXP_LIKE (last_name, '[<>#(){}]+')
        or
         REGEXP_LIKE (first_name, '[<>#(){}]+'));