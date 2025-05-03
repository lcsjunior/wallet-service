insert into public.wallet
    (balance, created_at, updated_at, version, user_id, id)
values (0, current_timestamp, current_timestamp, 1, '00000000-0000-0000-0000-000000000000', '00000000-0000-0000-0000-000000000001'),
       (0, current_timestamp, current_timestamp, 1, '00000000-0000-0000-0000-000000000000', '00000000-0000-0000-0000-000000000002'),
       (0, current_timestamp, current_timestamp, 1, '00000000-0000-0000-0000-000000000000', '00000000-0000-0000-0000-000000000003');

insert into public.transaction
    (operation_type, amount, transaction_type, created_at, related_transaction_id, version, wallet_id, id)
values ('CREDIT', 0.03, 'DEPOSIT', '1900-01-01T00:00:00', null, 1, '00000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-100000000000'),
       ('DEBIT', 0.03, 'WITHDRAW', '1900-01-01T00:00:01', null, 1, '00000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-200000000000'),
       ('CREDIT', 0.01, 'DEPOSIT', '1900-01-01T00:00:02', null, 1, '00000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-300000000000'),
       ('CREDIT', 0.02, 'DEPOSIT', current_timestamp, null, 1, '00000000-0000-0000-0000-000000000002', '00000000-0000-0000-0000-400000000000'),
       ('DEBIT', 0.01, 'WITHDRAW', current_timestamp, null, 1, '00000000-0000-0000-0000-000000000002', '00000000-0000-0000-0000-500000000000');

update public.wallet set balance = 0.01 where id = '00000000-0000-0000-0000-000000000001';
update public.wallet set balance = 0.01 where id = '00000000-0000-0000-0000-000000000002';
