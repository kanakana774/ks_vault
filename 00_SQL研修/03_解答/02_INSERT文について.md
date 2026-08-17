## 問題 1: 全ての情報を指定して新しい顧客を登録する
- **目的**: `INSERT INTO テーブル名 (列リスト) VALUES (値リスト)` を使用して、新しい行を追加する基本を理解する。また、`SERIAL` 型（`customer_id`）は自動採番されるため、INSERT 文で指定する必要がないことを確認する。

### 問題:
`customers_mst` テーブルに、以下の情報を全て指定して新しい顧客を登録してください。

- **顧客名**: 杉山 陽子
- **メール**: sugiyama.yoko@example.com
- **登録日**: 2024-01-10

### 解答:
```sql
INSERT INTO customers_mst (customer_name, email, created_date)
VALUES ('杉山 陽子', 'sugiyama.yoko@example.com', '2024-01-10');
```

> **解説**: `customer_id` は `SERIAL` 型（自動採番）として定義されているため、INSERT文で指定する必要はありません。データベースが自動的に連番を割り当てます。

---

## 問題 2: 必須項目のみ指定して新しい商品を登録する
- **目的**: `NOT NULL` 制約がある列には必ず値を指定する必要があること、および列の指定を省略した場合に `NULL` が許可されている列（`memo`）には `NULL` が入る挙動を理解する。

### 問題:
`products_mst` テーブルに、以下の情報を指定して新しい商品を登録してください。

- **カテゴリ**: Electronics
- **商品名**: ワイヤレス充電器
- **価格**: 3500.00
- **在庫数**: 0

※ `memo`（メモ）は指定しないでください（NULLとして登録すること）。

### 解答:
```sql
INSERT INTO products_mst (category, product_name, price, stock_quantity)
VALUES ('Electronics', 'ワイヤレス充電器', 3500.00, 0);
```

> **解説**:
> - `stock_quantity` は `NOT NULL` 制約があり、かつデフォルト値が設定されていないため、必ず値を指定する必要があります。
> - `memo` は指定を省略したため、自動的に `NULL` が設定されます。

---

## 問題 3: 特定の注文に新しい注文明細を追加する
- **目的**: 複合主キー（`order_id`, `product_id`）を持つテーブルへの INSERT 方法を理解し、既存のキーの組み合わせが重複しないようにデータを追加する。

### 問題:
既存の `orders_trn` の **order_id が 1** の注文に、以下の内容で新しい注文明細を追加してください。

- **商品ID (product_id)**: 15 （国産はちみつ）
- **数量 (quantity)**: 3

### 解答:
```sql
INSERT INTO order_details_trn (order_id, product_id, quantity)
VALUES (1, 15, 3);
```

> **解説**: これにより、既存の注文（order_id=1）に新しい商品行が追加されます。もし `order_id=1` かつ `product_id=15` のデータが既に存在していた場合は、主キー制約違反（重複エラー）となります。

---

## 問題 4: 注文と注文明細を同時に登録するシナリオ
- **目的**: 親テーブル（注文）にデータを登録し、その発番されたIDを使って子テーブル（注文明細）にデータを登録するという、実務的なデータ作成の流れ（リレーションの構築）を理解する。

### 問題:
**customer_id が 2** の顧客（鈴木 花子）について、以下の新しい注文を作成してください。

1. **注文日**: 2024-02-01
2. **購入商品**:
   - 「電気ケトル」（product_id=3）を **1個**
   - 「高級チョコレートアソート」（product_id=10）を **2個**

※まず `orders_trn` に注文を登録し、そこで生成された `order_id` を使って `order_details_trn` に登録する手順で記述してください。

### 解答:
```sql
-- 手順1: まず orders_trn に新しい注文を登録し、生成された order_id を確認する
INSERT INTO orders_trn (customer_id, order_date)
VALUES (2, '2024-02-01')
RETURNING order_id;
-- 実行結果例: order_id = 19 が返される（初期データの最大が 18 のため）

-- 手順2: 取得した order_id (例: 19) を使用して order_details_trn に明細を登録
-- ※実際には直前の実行結果のIDを使用してください
INSERT INTO order_details_trn (order_id, product_id, quantity)
VALUES (19, 3, 1),   -- 電気ケトルを 1 個
       (19, 10, 2);  -- 高級チョコレートアソートを 2 個
```

> **解説**: 実務のアプリケーション開発では、手順1の結果（ID）をプログラムが受け取り、それを手順2の SQL に埋め込んで実行します。`RETURNING` 句は INSERT した直後にその行の特定の列（ここでは自動採番されたID）を返す PostgreSQL の機能です。複数行をまとめて INSERT するときは `VALUES (…), (…);` と括弧を並べます（2つ目に `VALUES` を書くと構文エラーになります）。
