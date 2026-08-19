# 02章 演習：INSERT文

この章は**データが増える**演習なので、やり直すときは下の「準備」のリセットSQLを使ってください。

---

## 準備

### 使用するテーブル

02章のDDLで作成した `customers_mst` / `products_mst` / `orders_trn` / `order_details_trn` をそのまま使います。新しく作るものはありません。

### リセットSQL

この章は**データが増える**演習です。やり直したいとき・後続の章（07章など）の集計結果が教材と合わなくなったときは、これを実行すれば初期状態に戻ります。

```sql
-- 演習で追加した行を削除する
DELETE FROM order_details_trn WHERE order_id > 18 OR (order_id = 1 AND product_id = 15);
DELETE FROM orders_trn        WHERE order_id    > 18;
DELETE FROM products_mst      WHERE product_id  > 23;
DELETE FROM customers_mst     WHERE customer_id > 9;

-- 自動採番（SERIAL）のカウンタも初期データの最大値へ戻す
SELECT setval('customers_mst_customer_id_seq', 9);
SELECT setval('products_mst_product_id_seq',  23);
SELECT setval('orders_trn_order_id_seq',      18);
```

> 削除は**子テーブルから先**に行います（`order_details_trn` → `orders_trn`）。逆順だと外部キー制約でエラーになります。

---

## 問題 1: 全ての情報を指定して新しい顧客を登録する
- **目的**: `INSERT INTO テーブル名 (列リスト) VALUES (値リスト)` で行を追加でき、`SERIAL` 型の列は指定しなくてよいことを理解する。

### 問題:
`customers_mst` テーブルに、以下の情報を全て指定して新しい顧客を登録してください。

- **顧客名**: 杉山 陽子
- **メール**: sugiyama.yoko@example.com
- **登録日**: 2024-01-10

### 解答:
```sql
ここに解答を記入
```

---

## 問題 2: 必須項目のみ指定して新しい商品を登録する
- **目的**: `NOT NULL` 制約のある列には必ず値を指定し、列リストから省略した NULL 許容列には `NULL` が入ることを理解する。

### 問題:
`products_mst` テーブルに、以下の情報を指定して新しい商品を登録してください。

- **カテゴリ**: Electronics
- **商品名**: ワイヤレス充電器
- **価格**: 3500.00
- **在庫数**: 0

※ `memo`（メモ）は指定しないでください（NULLとして登録すること）。

### 解答:
```sql
ここに解答を記入
```

---

## 問題 3: 特定の注文に新しい注文明細を追加する
- **目的**: 複合主キー（`order_id`, `product_id`）を持つテーブルへ、キーの組み合わせが重複しないように行を追加する。

### 問題:
既存の `orders_trn` の **order_id が 1** の注文に、以下の内容で新しい注文明細を追加してください。

- **商品ID (product_id)**: 15 （国産はちみつ）
- **数量 (quantity)**: 3

### 解答:
```sql
ここに解答を記入
```

---

## 問題 4: 注文と注文明細を同時に登録するシナリオ
- **目的**: 親テーブル（注文）に登録して発番されたIDを、子テーブル（注文明細）の登録に使うという実務的な流れを理解する。

### 問題:
**customer_id が 2** の顧客（鈴木 花子）について、以下の新しい注文を作成してください。

1. **注文日**: 2024-02-01
2. **購入商品**:
   - 「電気ケトル」（product_id=3）を **1個**
   - 「高級チョコレートアソート」（product_id=10）を **2個**

※まず `orders_trn` に注文を登録し、そこで生成された `order_id` を使って `order_details_trn` に登録する手順で記述してください。
※PostgreSQLでは `RETURNING` 句を使うと、発番された `order_id` をその場で確認できます。

### 解答:
```sql
ここに解答を記入
```

