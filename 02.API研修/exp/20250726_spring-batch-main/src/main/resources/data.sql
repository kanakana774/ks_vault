-- 顧客ポイントテーブルのデータをクリア
DELETE FROM user_monthly_points;

-- 購買履歴テーブルのデータをクリアして再挿入
DELETE FROM purchase_history;
INSERT INTO purchase_history (customer_id, purchase_date, item_name, amount, created_at) VALUES
('C001', CURRENT_DATE - INTERVAL '1 month', 'A', 1000, CURRENT_DATE),
('C001', CURRENT_DATE - INTERVAL '2 month', 'B', 500, CURRENT_DATE),
('C002', CURRENT_DATE - INTERVAL '1 month', 'C', 2000, CURRENT_DATE),
('C001', CURRENT_DATE - INTERVAL '2 month', 'D', 800, CURRENT_DATE),
('C001', CURRENT_DATE - INTERVAL '2 month', 'D', 800, CURRENT_DATE),
('C001', CURRENT_DATE - INTERVAL '2 month', 'D', 800, CURRENT_DATE),
('C001', CURRENT_DATE - INTERVAL '2 month', 'D', 800, CURRENT_DATE),
('C001', CURRENT_DATE - INTERVAL '2 month', 'D', 800, CURRENT_DATE),
('C003', CURRENT_DATE - INTERVAL '1 month', 'E', 1500, CURRENT_DATE),
('C004', CURRENT_DATE - INTERVAL '1 month', 'F', 3000, CURRENT_DATE);