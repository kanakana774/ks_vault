```SQL
-- 顧客マスタ (customers_mst)
CREATE TABLE customers_mst (
customer_id SERIAL PRIMARY KEY,
customer_name VARCHAR(255) NOT NULL,
email VARCHAR(255) UNIQUE NOT NULL,
created_date DATE NOT NULL,
deleted_at TIMESTAMPTZ -- これは所謂メタカラム（論理削除のフラグ替わりなので正確な時間をTIMESTAMPTZで記録する意図）
);

-- 商品マスタ (products_mst)
CREATE TABLE products_mst (
product_id SERIAL PRIMARY KEY,
category VARCHAR(100) NOT NULL,
product_name VARCHAR(255) NOT NULL,
price NUMERIC(10, 2) NOT NULL,
stock_quantity INTEGER NOT NULL,
memo TEXT,
deleted_at TIMESTAMPTZ
);

-- 注文トランザクション (orders_trn)
CREATE TABLE orders_trn (
order_id SERIAL PRIMARY KEY,
customer_id INTEGER NOT NULL,
order_date DATE NOT NULL,
deleted_at TIMESTAMPTZ,
FOREIGN KEY (customer_id) REFERENCES customers_mst(customer_id)
);

-- 注文明細トランザクション (order_details_trn)
CREATE TABLE order_details_trn (
order_id INTEGER NOT NULL,
product_id INTEGER NOT NULL,
quantity INTEGER NOT NULL,
deleted_at TIMESTAMPTZ,
PRIMARY KEY (order_id, product_id),
FOREIGN KEY (order_id) REFERENCES orders_trn(order_id),
FOREIGN KEY (product_id) REFERENCES products_mst(product_id)
);

```


```SQL
-- サンプルデータの挿入
-- customers_mst にデータ挿入
INSERT INTO customers_mst (customer_id, customer_name, email, created_date, deleted_at) VALUES
(1, '佐藤 太郎', 'sato.taro@example.com', '2023-01-15', NULL),
(2, '鈴木 花子', 'suzuki.hanako@example.com', '2023-02-20', NULL),
(3, '田中 健太', 'tanaka.kenta@example.com', '2023-03-01', NULL),
(4, '山田 恵美', 'yamada.emi@example.com', '2023-04-10', '2023-09-15 10:00:00+0900'), -- 【狙い①】論理削除された顧客
(5, '渡辺 剛', 'watanabe.takeshi@example.com', '2023-05-05', NULL),
(6, '高橋 明', 'takahashi.akira@example.com', '2023-06-12', NULL),
(7, '中村 友子', 'nakamura.tomoko@example.com', '2023-07-25', NULL),
(8, '小林 大輔', 'kobayashi.daisuke@example.com', '2023-08-01', NULL),
(9, '伊藤 さやか', 'ito.sayaka@example.com', '2023-09-18', NULL); -- 【狙い②】未購入の顧客

-- products_mst にデータ挿入
INSERT INTO products_mst (product_id, category, product_name, price, stock_quantity, memo, deleted_at) VALUES
(1, 'Electronics', 'ワイヤレスイヤホン', 12800.00, 150, '高音質でノイズキャンセリング機能付き', NULL),
(2, 'Books', 'SQL 入門', 2500.00, 200, NULL, NULL),
(3, 'Home & Kitchen', '電気ケトル', 4500.00, 80, '1L 容量、自動電源オフ機能', NULL),
(4, 'Electronics', 'スマートウォッチ', 29800.00, 100, '心拍数モニタリング、GPS 搭載', NULL),
(5, 'Books', 'Python プログラミング', 3200.00, 120, '初心者向けの解説書', NULL),
(6, 'Food', 'オーガニックコーヒー豆', 1800.00, 300, NULL, NULL),
(7, 'Home & Kitchen', '高性能ブレンダー', 9800.00, 0, 'スムージー作りに最適', NULL), -- 【狙い④】在庫が0の商品
(8, 'Electronics', 'USB 充電器', 1500.00, 500, 'PD 対応、急速充電可能', NULL),
(9, 'Books', 'データ分析の基礎', 3800.00, 90, '統計学の基本から学習', NULL),
(10, 'Food', '高級チョコレートアソート', 2200.00, 180, 'ギフトにも最適', NULL),
(11, 'Electronics', 'ゲーミングマウス', 7800.00, 70, NULL, '2023-09-20 18:00:00+0900'), -- 【狙い①】論理削除された商品
(12, 'Home & Kitchen', 'セラミックフライパン', 5500.00, 110, '焦げ付きにくい加工', NULL),
(13, 'Books', '自己啓発の法則', 1800.00, 250, '成功へのヒント', NULL),
(14, 'Electronics', 'ポータブルバッテリー', 3980.00, 220, '大容量、軽量設計', NULL),
(15, 'Food', '国産はちみつ', 1200.00, 400, '100%純粋なはちみつ', NULL),
(16, 'Stationery', '多機能ボールペン', 2800.00, 300, '5色ボールペン+シャープペンシル', NULL); -- 【狙い③】未販売の商品

-- orders_trn にデータ挿入
INSERT INTO orders_trn (order_id, customer_id, order_date, deleted_at) VALUES
(1, 1, '2023-08-01', NULL), -- 佐藤太郎
(2, 2, '2023-08-05', NULL), -- 鈴木花子
(3, 1, '2023-08-10', NULL), -- 佐藤太郎 (2回目)
(4, 3, '2023-08-12', NULL), -- 田中健太
(5, 4, '2023-08-15', NULL), -- 山田恵美 (退会前の注文)
(6, 2, '2023-08-20', NULL), -- 鈴木花子 (2回目)
(7, 5, '2023-08-22', NULL), -- 渡辺剛
(8, 1, '2023-08-25', NULL), -- 佐藤太郎 (3回目)
(9, 6, '2023-09-01', '2023-09-02 11:30:00+0900'), -- 【狙い①】キャンセルされた注文
(10, 7, '2023-09-05', NULL), -- 中村友子
(11, 8, '2023-09-10', NULL); -- 小林大輔

-- order_details_trn にデータ挿入
INSERT INTO order_details_trn (order_id, product_id, quantity, deleted_at) VALUES
(1, 1, 1, NULL),  -- 佐藤: ワイヤレスイヤホン
(1, 6, 2, NULL),  -- 佐藤: オーガニックコーヒー豆
(2, 2, 1, NULL),  -- 鈴木: SQL 入門
(2, 3, 1, NULL),  -- 鈴木: 電気ケトル
(3, 4, 1, NULL),  -- 佐藤: スマートウォッチ
(3, 8, 3, NULL),  -- 佐藤: USB 充電器
(4, 5, 1, NULL),  -- 田中: Python プログラミング
(4, 7, 1, NULL),  -- 田中: 高性能ブレンダー
(5, 1, 1, NULL),  -- 山田: ワイヤレスイヤホン
(5, 9, 1, NULL),  -- 山田: データ分析の基礎
(6, 10, 2, NULL), -- 鈴木: 高級チョコレートアソート
(7, 11, 1, NULL), -- 渡辺: ゲーミングマウス (販売終了前の注文)
(7, 14, 1, NULL), -- 渡辺: ポータブルバッテリー
(8, 2, 1, NULL),  -- 佐藤: SQL 入門
(8, 12, 1, NULL), -- 佐藤: セラミックフライパン
(9, 13, 1, '2023-09-02 11:30:00+0900'), -- 【狙い①】キャンセルされた注文の明細
(10, 15, 500, NULL), -- 【狙い④】在庫数を超える大量注文
(11, 1, 1, NULL);  -- 小林: ワイヤレスイヤホン

-- SERIALのシーケンスを手動でリセット（データ投入後にIDの続きから始まるようにするため） 
SELECT setval('customers_mst_customer_id_seq', (SELECT MAX(customer_id) FROM customers_mst)); 
SELECT setval('products_mst_product_id_seq', (SELECT MAX(product_id) FROM products_mst)); 
SELECT setval('orders_trn_order_id_seq', (SELECT MAX(order_id) FROM orders_trn));
```