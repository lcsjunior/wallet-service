insert into public.wallet
    (balance, created_at, updated_at, version, id)
values (0, current_timestamp, current_timestamp, 1, '00000000-0000-0000-0000-000000000001'),
       (0, current_timestamp, current_timestamp, 1, '00000000-0000-0000-0000-000000000002'),
       (0, current_timestamp, current_timestamp, 1, '00000000-0000-0000-0000-000000000003');

insert into public.transaction
    (transaction_type, amount, created_at, related_transaction_id, version, wallet_id, id)
values ('CREDIT', 0.03, current_timestamp, null, 1, '00000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-100000000000'),
       ('DEBIT', 0.03, current_timestamp, null, 1, '00000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-200000000000'),
       ('CREDIT', 0.01, current_timestamp, null, 1, '00000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-300000000000');
update public.wallet set balance = 0.01 where id = '00000000-0000-0000-0000-000000000001';

insert into public.transaction
(transaction_type, amount, created_at, related_transaction_id, version, wallet_id, id)
values ('CREDIT', 0.02, current_timestamp, null, 1, '00000000-0000-0000-0000-000000000002', '00000000-0000-0000-0000-400000000000'),
       ('DEBIT', 0.01, current_timestamp, null, 1, '00000000-0000-0000-0000-000000000002', '00000000-0000-0000-0000-500000000000');
update public.wallet set balance = 0.01 where id = '00000000-0000-0000-0000-000000000002';