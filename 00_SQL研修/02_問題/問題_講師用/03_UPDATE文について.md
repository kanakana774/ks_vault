## 問題 1: 特定商品の価格を変更する
- **目的**: `UPDATE` 文と `WHERE` 句を用いて、特定の 1 つの行の特定の列の値を更新する基本を理解する。

### 問題:
`products_mst` テーブルの **「ワイヤレスイヤホン」** の価格を、現在の 12,800.00 円から **11,980.00 円** に更新してください。

### 解答:
```sql
UPDATE products_mst
SET price = 11980.00
WHERE product_name = 'ワイヤレスイヤホン';
```

---

## 問題 2: 複数の情報を同時に更新する
- **目的**: 1 つの `UPDATE` 文で複数の列(`column1 = val1, column2 = val2`)を同時に更新する方法を理解する。

### 問題:
`products_mst` テーブルの **「SQL 入門」** について、以下の 2 つの情報を同時に更新してください。

1. 価格を **2,200.00 円** に変更する。
2. 在庫数を **180 個** に変更する。

### 解答:
```sql
UPDATE products_mst
SET price = 2200.00,
    stock_quantity = 180
WHERE product_name = 'SQL 入門';
```

---

## 問題 3: 在庫数に基づいて商品の価格を割引する
- **目的**: 既存の列の値を参照して計算し、その結果で自分自身の列を更新する（相対的な更新）方法を理解する。

### 問題:
`products_mst` テーブルで、在庫数(`stock_quantity`)が **200 個以上** ある商品の価格を、現在の価格から **10% 割引** （0.9倍）してください。

### 解答:
```sql
UPDATE products_mst
SET price = price * 0.9
WHERE stock_quantity >= 200;
```
> **補足**: `price = price * 0.90` や `price = price - (price * 0.10)` など、計算式の書き方は複数ありますが、結果は同じです。

---

## 問題 4: 特定カテゴリの商品のメモを更新する
- **目的**: `WHERE` 句でカテゴリを指定し、複数の行を一括で更新する方法、および `TEXT` 型の更新を理解する。

### 問題:
`products_mst` テーブルで、カテゴリ(`category`)が **'Books'** の全ての商品について、メモ欄(`memo`)を **'人気書籍'** に書き換えてください。

### 解答:
```sql
UPDATE products_mst
SET memo = '人気書籍'
WHERE category = 'Books';
```

---

## 問題 5: 登録日が古い顧客のメールアドレスを更新する（文字列置換）
- **目的**: `WHERE` 句での日付比較と、関数（`REPLACE`）を用いた更新を理解する。

### 問題:
`customers_mst` テーブルで、**2023年4月1日より前** に登録された顧客のメールアドレスについて、ドメイン部分を `@example.com` から **`@newcompany.com`** に変更してください。
（例: `sato.taro@example.com` → `sato.taro@newcompany.com`）

### 解答:
```sql
UPDATE customers_mst
SET email = REPLACE(email, '@example.com', '@newcompany.com')
WHERE created_date < '2023-04-01';
```
> **解説**: `REPLACE(対象列, '探す文字', '置換する文字')` は、PostgreSQL等のDBで使える文字列操作関数です。

---

## 問題 6: 特定の顧客の最近の注文日を更新する
- **目的**: 実務でよくある「特定条件のレコードを目視確認してからID指定で更新する」という手順を学ぶ（まだサブクエリを使わない方法）。

### 問題:
`orders_trn` テーブルで、customer_id が 1 の顧客（佐藤 太郎）の **最も新しい注文日** を **2023-08-30** に修正してください。
※いきなりUPDATEせず、まずは対象の注文IDを特定してから更新を行ってください。

### 解答:
```sql
-- 手順1. customer_id=1 の注文を日付の新しい順に表示し、一番上の order_id を確認する
SELECT order_id, order_date
FROM orders_trn
WHERE customer_id = 1
ORDER BY order_date DESC;

-- 結果確認: 一番上が order_id = 8 だったと仮定

-- 手順2. 特定した order_id を使って更新する
UPDATE orders_trn
SET order_date = '2023-08-30'
WHERE order_id = 8;
```
> **解説**: 条件が複雑な場合や「最新の1件だけ」といった更新を行いたい場合、初心者のうちは無理に1つのSQLにまとめず、このように「SELECTでID特定」→「ID指定でUPDATE」とするのが確実で安全です。

---

## 問題 7: メモが NULL の商品にデフォルト値を設定する
- **目的**: `IS NULL` 演算子を使って、NULL 値を持つ行のみを対象に更新を行う。

### 問題:
`products_mst` テーブルで、メモ(`memo`)がまだ登録されていない（NULL である）商品のメモ欄を、**'詳細未設定'** という文字列に更新してください。

### 解答:
```sql
UPDATE products_mst
SET memo = '詳細未設定'
WHERE memo IS NULL;
```

---

## 問題 8: 売れ残りの可能性のある商品の在庫をゼロにする
- **目的**: 複数の `WHERE` 条件を `AND` で組み合わせ、特定のビジネスロジックに基づいた一括更新を行う。

### 問題:
`products_mst` テーブルで、以下の条件を両方満たす商品の在庫数(`stock_quantity`)を **0** に変更してください。

1. 価格が **2,000 円未満**
2. 在庫数が **0 より大きい**（まだ在庫がある）

### 解答:
```sql
UPDATE products_mst
SET stock_quantity = 0
WHERE price < 2000 
  AND stock_quantity > 0;
```

---

## 問題 9: 論理削除を行う
- **目的**: 物理削除（DELETE）ではなく、フラグや日時項目を更新することで削除扱いにする「論理削除」の実装方法を学ぶ。

### 問題:
`products_mst` テーブルで、**product_id が 1** の商品を論理削除してください。
※このテーブルでは `deleted_at` カラムに日時が入っているデータを「削除済み」とみなします。現在の日時(`NOW()`)をセットしてください。

### 解答:
```sql
UPDATE products_mst
SET deleted_at = NOW()
WHERE product_id = 1;
```
> **解説**: `NOW()` は現在の日時を取得するPostgreSQLの関数です。実務ではこのように `UPDATE` 文を使って削除日時を記録し、データそのものは消さない運用が一般的です。

---

## 問題 10: 【危険！】 全ての商品価格をゼロにする
- **目的**: `WHERE` 句を付けずに `UPDATE` 文を実行した場合の危険性（全件更新）を認識する。

### 問題:
`products_mst` テーブルの **全ての商品の価格** を **0.00 円** に更新してください。

**⚠️ 警告**: この操作はテーブルの全てのデータに影響を与えます。実務環境では絶対に行わないでください。

### 解答:
```sql
UPDATE products_mst
SET price = 0.00;
-- WHERE 句がないため、全ての行の price が 0 になります
```

> **指導ポイント**: `UPDATE` や `DELETE` を実行する際は、必ず `WHERE` 句で対象が絞り込まれているか確認する癖をつけましょう。実務では、まず `SELECT` 文で `WHERE` 条件をテストしてから、その条件を `UPDATE` 文にコピー＆ペーストすると安全です。