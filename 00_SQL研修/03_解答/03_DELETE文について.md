### ⚠️ 重要：学習上の注意
本問題集では、学習目的で `DELETE` 文による**物理削除**（レコードを完全に消す操作）を行います。
しかし、実際のテーブルには**外部キー制約**が設定されているため、親テーブル（顧客や商品）をいきなり削除しようとしてもエラーになります。

**「子テーブル（参照している側）から先に削除する」**というデータベースの整合性を保つためのルールを意識して解答してください。

---

## 問題 1: 特定の商品を削除する
- **目的**: 外部キー制約がある場合の基本的な削除手順（子 → 親）を理解する。

### 問題:
`products_mst` テーブルから、商品名が **「セラミックフライパン」** の情報を削除してください。

### 解答:
```sql
-- まず、この商品が含まれている注文明細を削除し、その後に商品を削除する

-- 1. 注文明細(子)を削除（サブクエリを使用する例）
DELETE FROM order_details_trn
WHERE product_id = (SELECT product_id FROM products_mst WHERE product_name = 'セラミックフライパン');

-- 2. 商品(親)を削除
DELETE FROM products_mst
WHERE product_name = 'セラミックフライパン';
```

**❌ よくある間違い**
```sql
DELETE FROM products_mst WHERE product_name = 'セラミックフライパン';
-- エラー: 注文明細テーブル(order_details_trn)で参照されているため削除できません
```
> **注意**: 子テーブルから参照されたままの親行は外部キー制約に阻まれて削除できないため、先に注文明細を消す必要があります。

> **参考**: 実務では物理削除を行わず、`deleted_at` に日時を入れる「論理削除」を行うのが一般的です。

---

## 問題 2: 在庫がゼロの商品を全て削除する
- **目的**: 条件に合致する複数の行を、整合性を保ちながら一括削除する方法を理解する。

### 問題:
`products_mst` テーブルから、在庫数(`stock_quantity`)が **0 個** の商品を全て削除してください。

### 解答:
```sql
-- 在庫0の商品IDを特定し、明細 → 商品の順で削除する

-- 1. 在庫0の商品が含まれる注文明細を削除
DELETE FROM order_details_trn
WHERE product_id IN (SELECT product_id FROM products_mst WHERE stock_quantity = 0);

-- 2. 在庫0の商品自体を削除
DELETE FROM products_mst
WHERE stock_quantity = 0;
```

---

## 問題 3: 特定のカテゴリに属する商品をまとめて削除する
- **目的**: `WHERE` 句でカテゴリを指定し、関連する複数の子レコードと親レコードを処理する。

### 問題:
`products_mst` テーブルから、カテゴリ(`category`)が **'Food'** の全ての商品を削除してください。

### 解答:
```sql
-- 1. 'Food'カテゴリ商品の注文明細を削除
DELETE FROM order_details_trn
WHERE product_id IN (SELECT product_id FROM products_mst WHERE category = 'Food');

-- 2. 'Food'カテゴリの商品を削除
DELETE FROM products_mst
WHERE category = 'Food';
```

---

## 問題 4: 長期間利用がない顧客を削除する
- **目的**: 3階層のテーブル（明細 → 注文 → 顧客）の削除順序と、日付比較を理解する。

### 問題:
`customers_mst` テーブルから、**2023年3月1日より前** に登録された顧客の情報を削除してください。

### 解答:
```sql
-- 顧客(customers_mst)を消すには、その顧客の注文(orders_trn)を消す必要があり、
-- 注文を消すには注文明細(order_details_trn)を消す必要がある。
-- したがって最も深い階層（孫テーブル）から順に削除する。
-- 対象: 2023-03-01 より前の顧客 (customer_id: 1, 2 が該当)

-- 1. [孫] 対象顧客の注文に紐づく「注文明細」を削除
DELETE FROM order_details_trn
WHERE order_id IN (
    SELECT order_id 
    FROM orders_trn 
    WHERE customer_id IN (SELECT customer_id FROM customers_mst WHERE created_date < '2023-03-01')
);

-- 2. [子] 対象顧客の「注文」を削除
DELETE FROM orders_trn
WHERE customer_id IN (SELECT customer_id FROM customers_mst WHERE created_date < '2023-03-01');

-- 3. [親] 「顧客」自体を削除
DELETE FROM customers_mst
WHERE created_date < '2023-03-01';
```

---

## 問題 5: メモが設定されていない商品を削除する
- **目的**: `IS NULL` を用いた削除条件と、リレーションの解消。

### 問題:
`products_mst` テーブルから、メモ(`memo`)が **NULL** である商品を全て削除してください。

### 解答:
```sql
-- 1. メモがNULLの商品の注文明細を削除
DELETE FROM order_details_trn
WHERE product_id IN (SELECT product_id FROM products_mst WHERE memo IS NULL);

-- 2. メモがNULLの商品を削除
DELETE FROM products_mst
WHERE memo IS NULL;
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
-- IDを指定して、上の階層から確実に消していく手順

-- 1. まず、削除対象の顧客に関連する注文の order_id を確認（実務的な手順）
-- SELECT order_id FROM orders_trn WHERE customer_id = 4;
-- 結果: 5 が該当すると確認できたと仮定

-- 2. 注文明細(order_details_trn)を削除
DELETE FROM order_details_trn
WHERE order_id IN (SELECT order_id FROM orders_trn WHERE customer_id = 4);
-- または確認したIDを使って: DELETE FROM order_details_trn WHERE order_id = 5;

-- 3. 注文(orders_trn)を削除
DELETE FROM orders_trn
WHERE customer_id = 4;

-- 4. 顧客(customers_mst)を削除
DELETE FROM customers_mst
WHERE customer_id = 4;
```

> **参考**: `ON DELETE CASCADE` オプションが設定されているテーブルであれば、親を消すだけで子も自動で消えますが、危険な操作になりうるため、意図して手動削除を行うケースも多々あります。

---

## 問題 7: 【極めて危険！】 全ての注文データを削除する
- **目的**: `WHERE` 句なしの `DELETE` の危険性と、全件削除時における外部キー制約の影響を理解する。

### 問題:
`orders_trn` テーブルの **全ての注文情報** を削除してください。
※実務環境では絶対に行わないでください。

### 解答:
```sql
-- 全件削除であっても、参照されている子テーブルから消す必要がある

-- 1. 注文明細を全て削除
DELETE FROM order_details_trn;

-- 2. 注文を全て削除
DELETE FROM orders_trn;
```

**❌ 実行できない例**
```sql
DELETE FROM orders_trn;
-- エラー: 注文明細(order_details_trn)に残っているデータがあるため削除できません。
```
> **注意**: `WHERE` 句の有無にかかわらず外部キー制約は効くため、子テーブルに1行でも残っていれば親の全件削除は失敗します。

> **⚠️ 講師向けの注意**: `DELETE` は物理的にデータを消してしまうため、復元が困難です。実務では `deleted_at` カラムに日付を入れる `UPDATE` 文（論理削除）を使うことがほとんどであることを併せて指導すること！

---

# 追加課題 解答
---

> **注意**: この章の追加問題は物理削除を行います。各問の末尾にある復旧SQLを必ず実行してください（04章・06章・07章の期待結果はこのデータが揃っている前提です）。

## 追加問題 1: 消す前に「参照されているか」を確かめる
- **目的**: `DELETE ... RETURNING` で削除した行の内容を証跡として取得しつつ、「参照されていない行は消せる／参照されている行は消せない」の違いから、なぜ実務が論理削除を選ぶのかを説明できるようにする。

### 問題:
商品マスタの棚卸しで、重複登録が2件見つかりました。

- `product_id = 20`: 商品名が `' 国産はちみつ '` と前後に空白付きで登録されており、`product_id = 15` と実質同じ商品
- `product_id = 19`: 商品名が `'SQL 入門'` で、`product_id = 2` と同名の二重登録

**(1)** `product_id = 20` を物理削除してください。その際、**削除した行の内容を `RETURNING` で出力** してください。

**(2)** 同じやり方で `product_id = 19` を削除してください。何が起きますか。エラーメッセージを読み、原因を説明してください。

**(3)** (2) を成功させる手順を書いてください。ただし、**その手順を実行すると業務上どんな情報が失われるか** も答えてください。

**(4)** この2件は、それぞれ物理削除・論理削除のどちらで処理すべきでしょうか。理由とともに述べてください。

### 解答:
```sql
-- (1) 削除した行の内容を RETURNING で証跡として受け取る
DELETE FROM products_mst
WHERE product_id = 20
RETURNING *;

-- (2) 同じやり方で product_id = 19 を削除しようとすると、次のエラーになる
DELETE FROM products_mst
WHERE product_id = 19
RETURNING *;
-- ERROR:  update or delete on table "products_mst" violates foreign key constraint
--         "order_details_trn_product_id_fkey" on table "order_details_trn"
-- DETAIL:  Key (product_id)=(19) is still referenced from table "order_details_trn".
--
-- product_id=19 は order_details_trn から1行参照されている（order_id=16 / 鈴木 花子 / 2024-08-07）。
-- 外部キー制約が「まだ使われているデータを消させない」ため、親の削除がブロックされている。
-- 一方 product_id=20 はどこからも参照されていないので、そのまま消せた。

-- (3) 成功させるには、参照している子（注文明細）を先に消してから親を消す
DELETE FROM order_details_trn
WHERE product_id = 19
RETURNING *;

DELETE FROM products_mst
WHERE product_id = 19
RETURNING *;
--
-- 失われる情報: 「鈴木 花子が 2024-08-07 に SQL 入門を1個購入した」という売上明細そのもの。
-- マスタの重複を掃除するために、無関係な売上実績を1件消してしまっている。

-- (4)
-- product_id=20（' 国産はちみつ '）: 一度も注文されていない純粋な誤登録 → 物理削除でよい。
-- product_id=19（'SQL 入門'）: 売上実績がある → 物理削除すると売上履歴が壊れるので、
--   次のように論理削除して商品一覧から隠すのが正解。
--   UPDATE products_mst SET deleted_at = NOW() WHERE product_id = 19;
```

### 期待結果:

(1) `RETURNING *` の出力 ―― 削除された1行がそのまま返る

| product_id | category | product_name | price | stock_quantity | memo | deleted_at |
| ---: | :--- | :--- | ---: | ---: | :--- | :--- |
| 20 | Food | ` 国産はちみつ ` | 1200.00 | 30 | (NULL) | (NULL) |

(2) の実行結果 ―― 1行も削除されずエラーで停止します。`DETAIL:` 行に `Key (product_id)=(19) is still referenced from table "order_details_trn".` と、参照元のテーブルとキーが明示されます。

> **注意**: (2) はエラーになるため、トランザクション内で試している場合はいったん `ROLLBACK;` してから (1) をやり直し、(3) に進んでください。

**復旧SQL（解き終えたら必ず実行）**
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

> **解説**: 同じ「重複データの掃除」でも、参照されているかどうかで手順も是非も変わり、外部キー制約はまだ使われているデータを消させない安全装置として働きます（どのテーブルのどのキーが参照しているかはエラーの `DETAIL:` 行に必ず書かれている）。`RETURNING` は削除した行をその場で返すので、元に戻せない `DELETE` にとって唯一の証跡となり、実務では実行ログとして必ず残します。

---

## 追加問題 2: 論理削除済みデータの一掃（パージ）
- **目的**: `deleted_at IS NOT NULL` を削除条件にして「論理削除済みの行を物理的に消す定期バッチ」を、親子関係の順序を守って書けるようにする。

### 問題:
「キャンセル済みの注文を、DBから完全に削除してほしい」という依頼を受けました。

**(1)** まず削除対象を確認します。`orders_trn` と `order_details_trn` に、論理削除済み（`deleted_at` が入っている）の行はそれぞれ何件ありますか。

**(2)** いきなり `DELETE FROM orders_trn WHERE deleted_at IS NOT NULL;` を実行するとどうなりますか。

**(3)** 正しい順序でパージ（物理削除）を実行してください。

**(4)** パージ後、`customer_id = 6`（高橋 明）の注文は何件になりますか。またこの顧客は、注文履歴のうえで `customer_id = 9`（伊藤 さやか）とどう区別がつかなくなりますか。それはなぜ問題なのでしょうか。

### 解答:
```sql
-- (1) 削除対象の件数を先に確認する（消す前の確認は必須）
SELECT COUNT(*) AS cancelled_orders
FROM orders_trn
WHERE deleted_at IS NOT NULL;

SELECT COUNT(*) AS cancelled_details
FROM order_details_trn
WHERE deleted_at IS NOT NULL;

-- (2) 親（注文）から消そうとすると外部キー制約違反でエラーになる
DELETE FROM orders_trn
WHERE deleted_at IS NOT NULL;
-- ERROR:  update or delete on table "orders_trn" violates foreign key constraint
--         "order_details_trn_order_id_fkey" on table "order_details_trn"
-- DETAIL:  Key (order_id)=(9) is still referenced from table "order_details_trn".

-- (3) 子（注文明細）→ 親（注文）の順にパージする
DELETE FROM order_details_trn
WHERE deleted_at IS NOT NULL;

DELETE FROM orders_trn
WHERE deleted_at IS NOT NULL;

-- (4) パージ後の高橋 明（customer_id = 6）の注文件数
SELECT COUNT(*) AS akira_orders
FROM orders_trn
WHERE customer_id = 6;
--
-- 0件になる。高橋 明の注文は order_id=9 の1件だけで、それがキャンセル済みだったため消えた。
-- 一度も注文したことがない伊藤 さやか（customer_id=9）も0件なので、注文履歴のうえでは
-- 両者がまったく同じ「注文0件の顧客」に見えてしまう。
-- 「注文したがキャンセルした顧客」と「そもそも注文したことがない顧客」は、
-- 販促やサポートの打ち手がまるで違うのに、その区別が永久に失われる。
```

### 期待結果:

(1) 削除対象の件数

| クエリ | 結果 |
| :--- | ---: |
| cancelled_orders | 2 |
| cancelled_details | 2 |

該当行は `orders_trn` が `order_id` = 9, 18、`order_details_trn` が `(9, 13)` と `(18, 17)` です。

(3) パージの実行結果 ―― `DELETE 2` が2回。`orders_trn` は 18 → 16 件、`order_details_trn` は 28 → 26 件になります。

(4) `akira_orders` は **0**。伊藤 さやか（`customer_id = 9`）も 0 件のため、両者が区別できなくなります。

> **注意**: (2) はエラーになるため、トランザクション内で試している場合はいったん `ROLLBACK;` してから (3) に進んでください。

**復旧SQL（解き終えたら必ず実行）**
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

> **解説**: 論理削除済みの行を後から物理削除する処理はパージと呼ばれ、実運用でも定期的に行います（削除条件が `WHERE deleted_at IS NOT NULL` という論理削除フラグそのものになる点だけが特徴で、子 → 親という順序は通常の削除と同じ）。ただし (4) が示すとおり、パージには「キャンセルという事実そのもの」を失う不可逆な副作用があります。パージ要件が来たら、本当に消してよいか・退避先はあるかを必ず確認してください。

> **⚠️ 講師向けの注意**: 復旧SQLを実行し忘れると 04章・06章・07章の期待結果が合わなくなるため、演習の終わりに `orders_trn` 18件 / `order_details_trn` 28件 / `products_mst` 23件 を全員に確認させること。
