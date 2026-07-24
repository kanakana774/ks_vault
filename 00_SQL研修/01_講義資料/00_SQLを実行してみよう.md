### テーブル作成
```sql
-- 顧客マスタ (customers_mst)
CREATE TABLE customers_mst (
customer_id SERIAL PRIMARY KEY,
customer_name VARCHAR(255) NOT NULL,
email VARCHAR(255) UNIQUE NOT NULL,
created_date DATE NOT NULL,
deleted_at TIMESTAMPTZ -- これは所謂メタカラム（論理削除のフラグ替わりなので正確な時間をTIMESTAMPTZで記録する意図）
);
```

### テストデータ作成
```sql
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
```

### 検索クエリ
```SQL
SELECT *
FROM customers_mst;
```