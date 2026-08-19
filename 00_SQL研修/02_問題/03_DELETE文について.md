# 03章 演習：DELETE文

この章では学習目的で `DELETE` 文による**物理削除**（レコードを完全に消す操作）を行います。テーブルには**外部キー制約**が設定されているため、親テーブル（顧客や商品）をいきなり削除しようとしてもエラーになります。**「子テーブル（参照している側）から先に削除する」**というデータベースの整合性を保つためのルールを意識して解答してください。

**1問解くごとにデータが消えます。** 次の問題に進む前に、下の「準備」にある**リセットSQL**で初期状態に戻してください。

---

## 準備

### 使用するテーブル

02章で作成した `customers_mst` / `products_mst` / `orders_trn` / `order_details_trn` をそのまま使います。まだ作っていない場合は [[00_SQL研修/03_解答/02_DDL（前半用）|02_DDL（前半用）]] で作成してください。

初期状態の件数は **顧客 9 件 / 商品 23 件 / 注文 18 件 / 注文明細 28 件** です。

### テーブルの親子関係

外部キーは次の3本です。削除は矢印の**逆向き**（参照している側から）に進めます。

- `orders_trn.customer_id` → `customers_mst.customer_id`
- `order_details_trn.order_id` → `orders_trn.order_id`
- `order_details_trn.product_id` → `products_mst.product_id`

### リセットSQL

データが分からなくなったら、これを実行すれば初期状態に戻ります。**1問解くごとに実行してください。**

```sql
-- 1. 子 → 親 の順に全件削除する
DELETE FROM order_details_trn;
DELETE FROM orders_trn;
DELETE FROM products_mst;
DELETE FROM customers_mst;

-- 2. 初期データを入れ直す
INSERT INTO customers_mst (customer_id, customer_name, email, created_date, deleted_at) VALUES
(1, '佐藤 太郎',   'sato.taro@example.com',        '2023-01-15', NULL),
(2, '鈴木 花子',   'suzuki.hanako@example.com',    '2023-02-20', NULL),
(3, '田中 健太',   'tanaka.kenta@example.com',     '2023-03-01', NULL),
(4, '山田 恵美',   'yamada.emi@example.com',       '2023-04-10', '2023-09-15 10:00:00+0900'),
(5, '渡辺 剛',     'watanabe.takeshi@example.com', '2023-05-05', NULL),
(6, '高橋 明',     'takahashi.akira@example.com',  '2023-06-12', NULL),
(7, '中村 友子',   'nakamura.tomoko@example.com',  '2023-07-25', NULL),
(8, '小林 大輔',   'kobayashi.daisuke@example.com','2023-08-01', NULL),
(9, '伊藤 さやか', 'ito.sayaka@example.com',       '2023-09-18', NULL);

INSERT INTO products_mst (product_id, category, product_name, price, stock_quantity, memo, deleted_at) VALUES
(1,  'Electronics',    'ワイヤレスイヤホン',       12800.00, 150, '高音質でノイズキャンセリング機能付き', NULL),
(2,  'Books',          'SQL 入門',                  2500.00, 200, NULL, NULL),
(3,  'Home & Kitchen', '電気ケトル',                4500.00,  80, '1L 容量、自動電源オフ機能', NULL),
(4,  'Electronics',    'スマートウォッチ',         29800.00, 100, '心拍数モニタリング、GPS 搭載', NULL),
(5,  'Books',          'Python プログラミング',     3200.00, 120, '初心者向けの解説書', NULL),
(6,  'Food',           'オーガニックコーヒー豆',    1800.00, 300, NULL, NULL),
(7,  'Home & Kitchen', '高性能ブレンダー',          9800.00,   0, 'スムージー作りに最適', NULL),
(8,  'Electronics',    'USB 充電器',                1500.00, 500, 'PD 対応、急速充電可能', NULL),
(9,  'Books',          'データ分析の基礎',          3800.00,  90, '統計学の基本から学習', NULL),
(10, 'Food',           '高級チョコレートアソート',  2200.00, 180, 'ギフトにも最適', NULL),
(11, 'Electronics',    'ゲーミングマウス',          7800.00,  70, NULL, '2023-09-20 18:00:00+0900'),
(12, 'Home & Kitchen', 'セラミックフライパン',      5500.00, 110, '焦げ付きにくい加工', NULL),
(13, 'Books',          '自己啓発の法則',            1800.00, 250, '成功へのヒント', NULL),
(14, 'Electronics',    'ポータブルバッテリー',      3980.00, 220, '大容量、軽量設計', NULL),
(15, 'Food',           '国産はちみつ',              1200.00, 400, '100%純粋なはちみつ', NULL),
(16, 'Stationery',     '多機能ボールペン',          2800.00, 300, '5色ボールペン+シャープペンシル', NULL),
(17, 'Toys',           '木製パズル',                3300.00,   0, '知育玩具・対象年齢3歳から', NULL),
(18, 'Toys',           'ラジコンカー',              8800.00,   0, NULL, NULL),
(19, 'Books',          'SQL 入門',                  2500.00,  60, '改訂版として誤って二重登録', NULL),
(20, 'Food',           ' 国産はちみつ ',            1200.00,  30, NULL, NULL),
(21, 'Stationery',     'A4 ノート 5冊セット',        750.00, 500, NULL, NULL),
(22, 'Stationery',     '蛍光マーカー 6色',           980.00, 420, NULL, NULL),
(23, 'Toys',           'ぬいぐるみ（うさぎ）',      2400.00,   0, 'ギフト包装対応', NULL);

INSERT INTO orders_trn (order_id, customer_id, order_date, deleted_at) VALUES
(1,  1, '2023-08-01', NULL), (2,  2, '2023-08-05', NULL), (3,  1, '2023-08-10', NULL),
(4,  3, '2023-08-12', NULL), (5,  4, '2023-08-15', NULL), (6,  2, '2023-08-20', NULL),
(7,  5, '2023-08-22', NULL), (8,  1, '2023-08-25', NULL),
(9,  6, '2023-09-01', '2023-09-02 11:30:00+0900'),
(10, 7, '2023-09-05', NULL), (11, 8, '2023-09-10', NULL), (12, 3, '2023-09-15', NULL),
(13, 1, '2024-02-14', NULL), (14, 2, '2024-02-20', NULL), (15, 1, '2024-03-05', NULL),
(16, 2, '2024-08-07', NULL), (17, 1, '2024-08-21', NULL),
(18, 7, '2024-09-03', '2024-09-04 09:15:00+0900');

INSERT INTO order_details_trn (order_id, product_id, quantity, deleted_at) VALUES
(1,  1,  1, NULL), (1,  6,  2, NULL), (2,  2,  1, NULL), (2,  3,  1, NULL),
(3,  4,  1, NULL), (3,  8,  3, NULL), (4,  5,  1, NULL), (4,  7,  1, NULL),
(5,  1,  1, NULL), (5,  9,  1, NULL), (6, 10,  2, NULL), (7, 11,  1, NULL),
(7, 14,  1, NULL), (8,  2,  1, NULL), (8, 12,  1, NULL),
(9, 13,  1, '2023-09-02 11:30:00+0900'),
(10, 15, 500, NULL), (11, 1,  1, NULL), (12, 13, 1, NULL), (13, 2,  1, NULL),
(13, 16, 3, NULL), (14, 21, 2, NULL), (15, 6,  4, NULL), (15, 15, 2, NULL),
(16, 19, 1, NULL), (17, 1,  1, NULL), (17, 8,  2, NULL),
(18, 17, 1, '2024-09-04 09:15:00+0900');

-- 3. SERIAL のシーケンスを最大IDに合わせ直す
SELECT setval('customers_mst_customer_id_seq', (SELECT MAX(customer_id) FROM customers_mst));
SELECT setval('products_mst_product_id_seq',   (SELECT MAX(product_id)  FROM products_mst));
SELECT setval('orders_trn_order_id_seq',       (SELECT MAX(order_id)    FROM orders_trn));

-- 4. 検証: 9 / 23 / 18 / 28 になっていること
SELECT (SELECT COUNT(*) FROM customers_mst)     AS customers,
       (SELECT COUNT(*) FROM products_mst)      AS products,
       (SELECT COUNT(*) FROM orders_trn)        AS orders,
       (SELECT COUNT(*) FROM order_details_trn) AS details;
```

---

## 問題 1: 特定の商品を削除する
- **目的**: 外部キー制約がある場合の基本的な削除手順（子 → 親）を理解する。

### 問題:
`products_mst` テーブルから、商品名が **「セラミックフライパン」** の情報を削除してください。

### 解答:
```sql
ここに解答を記入
```

---

## 問題 2: 在庫がゼロの商品を全て削除する
- **目的**: 条件に合致する複数の行を、整合性を保ちながら一括削除する方法を理解する。

### 問題:
`products_mst` テーブルから、在庫数(`stock_quantity`)が **0 個** の商品を全て削除してください。

### 解答:
```sql
ここに解答を記入
```

---

## 問題 3: 特定のカテゴリに属する商品をまとめて削除する
- **目的**: `WHERE` 句でカテゴリを指定し、関連する複数の子レコードと親レコードを処理する。

### 問題:
`products_mst` テーブルから、カテゴリ(`category`)が **'Food'** の全ての商品を削除してください。

### 解答:
```sql
ここに解答を記入
```

---

## 問題 4: 長期間利用がない顧客を削除する
- **目的**: 3階層のテーブル（明細 → 注文 → 顧客）の削除順序と、日付比較を理解する。

### 問題:
`customers_mst` テーブルから、**2023年3月1日より前** に登録された顧客の情報を削除してください。

> **ヒント**: 顧客(`customers_mst`)を消すには、その顧客の注文(`orders_trn`)を消す必要があり、注文を消すには注文明細(`order_details_trn`)を消す必要があります。

### 解答:
```sql
ここに解答を記入
```

---

## 問題 5: メモが設定されていない商品を削除する
- **目的**: `IS NULL` を用いた削除条件と、リレーションの解消。

### 問題:
`products_mst` テーブルから、メモ(`memo`)が **NULL** である商品を全て削除してください。

### 解答:
```sql
ここに解答を記入
```

---

## 問題 6: 特定の顧客IDに関連するデータを削除する
- **目的**: 特定のIDを指定して、関連データを手動でクリーンアップする手順を確実に遂行する能力を養う。

### 問題:
**customer_id が 4** の顧客（山田 恵美）について、以下の手順でデータを削除してください。
1. この顧客が購入した全ての「注文明細」
2. この顧客の「注文履歴」
3. この顧客の「顧客情報」

### 解答:
```sql
ここに解答を記入
```

---

## 問題 7: 【極めて危険！】 全ての注文データを削除する
- **目的**: `WHERE` 句なしの `DELETE` の危険性と、全件削除時における外部キー制約の影響を理解する。

### 問題:
`orders_trn` テーブルの **全ての注文情報** を削除してください。
※実務環境では絶対に行わないでください。

### 解答:
```sql
ここに解答を記入
```

---

## 追加課題（ここから先は任意）

**問題 7 まで**が必須です。ここから先は、早く終わった人・もっと解きたい人向けです。

> **注意**: ここから先の問題も物理削除を行います。各問の `### 問題:` の末尾にある復旧SQLを必ず実行してください（04章・06章・07章の期待結果はこのデータが揃っている前提です）。

---

## 問題 8: 消す前に「参照されているか」を確かめる
- **目的**: `DELETE ... RETURNING` で削除した行の内容を証跡として取得しつつ、「参照されていない行は消せる／参照されている行は消せない」の違いから、なぜ実務が論理削除を選ぶのかを説明できるようにする。

### 問題:
商品マスタの棚卸しで、重複登録が2件見つかりました。

- `product_id = 20`: 商品名が `' 国産はちみつ '` と前後に空白付きで登録されており、`product_id = 15` と実質同じ商品
- `product_id = 19`: 商品名が `'SQL 入門'` で、`product_id = 2` と同名の二重登録

**(1)** `product_id = 20` を物理削除してください。その際、**削除した行の内容を `RETURNING` で出力** してください。

**(2)** 同じやり方で `product_id = 19` を削除してください。何が起きますか。エラーメッセージを読み、原因を説明してください。

**(3)** (2) を成功させる手順を書いてください。ただし、**その手順を実行すると業務上どんな情報が失われるか** も答えてください。

**(4)** この2件は、それぞれ物理削除・論理削除のどちらで処理すべきでしょうか。理由とともに述べてください。

> **ヒント**: エラーメッセージの `DETAIL:` 行には、どのテーブルのどのキーが参照しているかが必ず書かれています。

**復旧SQL（この問題を解き終えたら必ず実行）**
```sql
INSERT INTO products_mst (product_id, category, product_name, price, stock_quantity, memo, deleted_at)
VALUES (19, 'Books', 'SQL 入門', 2500.00, 60, '改訂版として誤って二重登録', NULL),
       (20, 'Food', ' 国産はちみつ ', 1200.00, 30, NULL, NULL);

INSERT INTO order_details_trn (order_id, product_id, quantity, deleted_at)
VALUES (16, 19, 1, NULL);

SELECT setval('products_mst_product_id_seq', (SELECT MAX(product_id) FROM products_mst));

-- 検証: products_mst が 23 件、order_details_trn が 28 件に戻っていること
SELECT COUNT(*) AS products FROM products_mst;
SELECT COUNT(*) AS details  FROM order_details_trn;
```

### 解答:
```sql
ここに解答を記入
```

---

## 問題 9: 論理削除済みデータの一掃（パージ）
- **目的**: `deleted_at IS NOT NULL` を削除条件にして「論理削除済みの行を物理的に消す定期バッチ」を、親子関係の順序を守って書けるようにする。

### 問題:
「キャンセル済みの注文を、DBから完全に削除してほしい」という依頼を受けました。

**(1)** まず削除対象を確認します。`orders_trn` と `order_details_trn` に、論理削除済み（`deleted_at` が入っている）の行はそれぞれ何件ありますか。

**(2)** いきなり `DELETE FROM orders_trn WHERE deleted_at IS NOT NULL;` を実行するとどうなりますか。

**(3)** 正しい順序でパージ（物理削除）を実行してください。

**(4)** パージ後、`customer_id = 6`（高橋 明）の注文は何件になりますか。またこの顧客は、注文履歴のうえで `customer_id = 9`（伊藤 さやか）とどう区別がつかなくなりますか。それはなぜ問題なのでしょうか。

**復旧SQL（この問題を解き終えたら必ず実行）**
```sql
INSERT INTO orders_trn (order_id, customer_id, order_date, deleted_at)
VALUES (9,  6, '2023-09-01', '2023-09-02 11:30:00+0900'),
       (18, 7, '2024-09-03', '2024-09-04 09:15:00+0900');

INSERT INTO order_details_trn (order_id, product_id, quantity, deleted_at)
VALUES (9,  13, 1, '2023-09-02 11:30:00+0900'),
       (18, 17, 1, '2024-09-04 09:15:00+0900');

SELECT setval('orders_trn_order_id_seq', (SELECT MAX(order_id) FROM orders_trn));

-- 検証: orders_trn が 18 件、order_details_trn が 28 件に戻っていること
SELECT COUNT(*) AS orders  FROM orders_trn;
SELECT COUNT(*) AS details FROM order_details_trn;
```

### 解答:
```sql
ここに解答を記入
```

