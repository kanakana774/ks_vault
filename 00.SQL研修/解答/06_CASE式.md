## 問題 1: 商品の価格帯を分類する（検索CASE式）
- **目的**: 検索CASE式（CASE WHEN ...）を使用して、数値データ（価格）を基に新しいカテゴリ文字列を作成して表示する方法を理解する。

### 問題:
`products_mst` テーブルから、各商品の **価格帯** を以下の基準で分類し、「商品名」「価格」「分類された価格帯（price_category）」を表示してください。

- **高価格帯**: 20,000 円以上
- **中価格帯**: 5,000 円以上 20,000 円未満
- **低価格帯**: 5,000 円未満

### 解答:
```sql
SELECT
    product_name,
    price,
    CASE
        WHEN price >= 20000 THEN '高価格帯'
        WHEN price >= 5000  THEN '中価格帯'
        ELSE '低価格帯'
    END AS price_category
FROM
    products_mst
ORDER BY
    price DESC;
```
> **解説**: CASE式は上から順に評価され、最初に真(True)になった条件が適用されます。

---

## 問題 2: 顧客の登録時期をセグメント化する（日付範囲）
- **目的**: CASE式と日付比較を組み合わせて、日付データを基に顧客を分類する方法を理解する。

### 問題:
`customers_mst` テーブルから、各顧客の **登録時期** を以下の基準で分類し、「顧客名」「登録日」「分類された登録時期（registration_segment）」を表示してください。

- **初期登録顧客**: 2023年1月1日 ～ 2023年3月31日
- **中期登録顧客**: 2023年4月1日 ～ 2023年6月30日
- **最近登録顧客**: 2023年7月1日以降
- **その他**: 上記以外

### 解答:
```sql
SELECT
    customer_name,
    created_date,
    CASE
        WHEN created_date BETWEEN '2023-01-01' AND '2023-03-31' THEN '初期登録顧客'
        WHEN created_date BETWEEN '2023-04-01' AND '2023-06-30' THEN '中期登録顧客'
        WHEN created_date >= '2023-07-01' THEN '最近登録顧客'
        ELSE 'その他'
    END AS registration_segment
FROM
    customers_mst
ORDER BY
    created_date ASC;
```

---

## 問題 3: 商品カテゴリごとの在庫状況を評価する（クロス集計の基礎）
- **目的**: `COUNT(CASE ... END)` というテクニックを使い、条件に応じた件数を数える方法（いわゆるピボット/クロス集計）を学ぶ。
  ※少し応用的な内容ですが、レポート作成で非常によく使います。

### 問題:
`products_mst` テーブルから、 **カテゴリごと** に以下の在庫状況別の商品数を集計して表示してください。

- **stock_abundant**: 在庫数が 100 個以上
- **stock_normal**: 在庫数が 10 個以上 100 個未満
- **stock_low**: 在庫数が 10 個未満

また、そのカテゴリの **合計商品数 (total_products)** も併せて表示してください。

### 解答:
```sql
SELECT
    category,
    COUNT(CASE WHEN stock_quantity >= 100 THEN 1 END) AS stock_abundant,
    COUNT(CASE WHEN stock_quantity >= 10 AND stock_quantity < 100 THEN 1 END) AS stock_normal,
    COUNT(CASE WHEN stock_quantity < 10 THEN 1 END) AS stock_low,
    COUNT(*) AS total_products
FROM
    products_mst
GROUP BY
    category
ORDER BY
    category;
```
> **解説**: `COUNT` 関数は `NULL` を無視します。CASE式で `ELSE` を省略すると、条件に合わない場合は `NULL` が返るため、条件に合った行だけが「1」となり、結果としてその件数がカウントされます。

---

## 問題 4: 特定カテゴリの商品を優先して並び替える（ORDER BYでのCASE式）
- **目的**: `ORDER BY` 句内で CASE 式を使用し、特定の条件を満たす行を強制的に先頭や末尾に持ってくる「カスタムソート」の手法を理解する。

### 問題:
`products_mst` テーブルから、商品を以下の優先順位で並び替えて表示してください。

1. **'Electronics'** カテゴリの商品を最優先
2. 次に **'Books'** カテゴリの商品
3. それ以外のカテゴリの商品

※各グループ内（同じ優先度内）では、**価格が高い順** に並び替えてください。

### 解答:
```sql
SELECT
    product_name,
    category,
    price
FROM
    products_mst
ORDER BY
    CASE category
        WHEN 'Electronics' THEN 1
        WHEN 'Books' THEN 2
        ELSE 3
    END ASC,
    price DESC;
```
> **解説**: `ORDER BY` 句でCASE式を使うと、カテゴリ名そのものではなく、「1, 2, 3」という変換後の数値に基づいて並び替えが行われます。

---

## 問題 5: 特定の顧客のみ注文日を調整して表示する（WHERE句でのCASE式）
- **目的**: `WHERE` 句で条件分岐を行いたい場合の記述方法と、実務的なベストプラクティス（論理演算子 `OR` の活用）を対比して学ぶ。

### 問題:
`orders_trn` テーブルから、以下の条件でデータを抽出してください。

- **customer_id が 1 （佐藤 太郎）の場合**: 2023年8月10日 **以降** の注文のみ表示
- **それ以外の顧客の場合**: 日付に関わらず、全ての注文を表示

### 解答:

**推奨される方法（論理演算子 OR を使用）**
実務ではこちらの書き方が一般的で、可読性もパフォーマンスも良いケースが多いです。
```sql
SELECT
    order_id,
    customer_id,
    order_date
FROM
    orders_trn
WHERE
    (customer_id = 1 AND order_date >= '2023-08-10')
    OR
    (customer_id <> 1)
ORDER BY
    customer_id, order_date;
```

**参考：CASE式を使った方法**
「もし○○なら条件A、そうでなければ条件B」というロジックを無理やりCASE式で書くと以下のようになります。
```sql
SELECT
    order_id,
    customer_id,
    order_date
FROM
    orders_trn
WHERE
    CASE 
        WHEN customer_id = 1 THEN order_date >= '2023-08-10'
        ELSE TRUE 
    END
ORDER BY
    customer_id, order_date;
```
> **補足**: SQLの `WHERE` 句は「行ごとにTrueかFalseかを判定する場所」なので、CASE式の結果として比較式（`order_date >= ...`）を返すことは通常できません（一部のDBを除く）。PostgreSQLでは上記のような `CASE WHEN ... THEN 比較式 ELSE TRUE END` がブール値を返す式として機能する場合がありますが、可読性が低いため通常は推奨されません。

---

## 問題 6: 商品メモの有無と在庫状況に応じた評価（NULL と複数条件）
- **目的**: CASE式内で `IS NULL` と複数の条件(`AND`)を組み合わせ、複雑なビジネスロジックを表現する。

### 問題:
`products_mst` テーブルから、各商品の **詳細状況** を以下の基準で評価し表示してください。

- **詳細充実・在庫あり**: メモが入力済(`IS NOT NULL`) かつ 在庫数が 50 個以上
- **詳細充実・在庫少**: メモが入力済(`IS NOT NULL`) かつ 在庫数が 50 個未満
- **詳細未入力・販売中**: メモが未入力(`IS NULL`) かつ 在庫数が 0 個より大きい
- **その他**: 上記以外（例: メモなしで在庫0など）

### 解答:
```sql
SELECT
    product_name,
    stock_quantity,
    memo,
    CASE
        WHEN memo IS NOT NULL AND stock_quantity >= 50 THEN '詳細充実・在庫あり'
        WHEN memo IS NOT NULL AND stock_quantity < 50  THEN '詳細充実・在庫少'
        WHEN memo IS NULL     AND stock_quantity > 0   THEN '詳細未入力・販売中'
        ELSE 'その他'
    END AS detail_and_stock_status
FROM
    products_mst
ORDER BY
    detail_and_stock_status, product_name;
```