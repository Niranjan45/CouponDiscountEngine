-- Sample coupons for local/dev testing (H2). Remove or gate behind a
-- profile check before deploying against a real database.

INSERT INTO coupons (code, type, discount_value, max_discount_amount, min_order_value, applicable_category, expiry_date, usage_limit, usage_count, single_use_per_user, stackable, active, version)
VALUES ('WELCOME100', 'FLAT', 100.00, NULL, NULL, NULL, DATEADD('YEAR', 1, CURRENT_TIMESTAMP), 1000, 0, TRUE, FALSE, TRUE, 0);

INSERT INTO coupons (code, type, discount_value, max_discount_amount, min_order_value, applicable_category, expiry_date, usage_limit, usage_count, single_use_per_user, stackable, active, version)
VALUES ('SAVE10PCT', 'PERCENTAGE', 10.00, 500.00, NULL, NULL, DATEADD('YEAR', 1, CURRENT_TIMESTAMP), NULL, 0, TRUE, TRUE, TRUE, 0);

INSERT INTO coupons (code, type, discount_value, max_discount_amount, min_order_value, applicable_category, expiry_date, usage_limit, usage_count, single_use_per_user, stackable, active, version)
VALUES ('ELEC15', 'CONDITIONAL', 15.00, 1000.00, 2000.00, 'ELECTRONICS', DATEADD('YEAR', 1, CURRENT_TIMESTAMP), 500, 0, TRUE, TRUE, TRUE, 0);

INSERT INTO coupons (code, type, discount_value, max_discount_amount, min_order_value, applicable_category, expiry_date, usage_limit, usage_count, single_use_per_user, stackable, active, version)
VALUES ('EXPIRED5', 'FLAT', 5.00, NULL, NULL, NULL, DATEADD('DAY', -1, CURRENT_TIMESTAMP), 100, 0, TRUE, FALSE, TRUE, 0);
