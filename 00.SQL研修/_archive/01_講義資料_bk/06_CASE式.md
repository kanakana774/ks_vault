## CASE式による条件分岐

### 1. CASE式とは？

CASE式は、SQLクエリ内で条件に応じて処理を分岐させるための構文です。多くのプログラミング言語における `if-then-else` や `switch` 文のように、データの変換、分類、集計、並び替えなど、様々な場面で活用できる強力な機能です。

**主な用途:**
*   **`SELECT`句**: 条件に基づいて値を変換し、新しい列を作成する。
*   **`GROUP BY`と集計関数**: 条件に合うデータだけを集計する（クロス集計）。
*   **`ORDER BY`句**: 独自のルールでデータの並び順を制御する。
*   **`WHERE`句**: 条件によってフィルタリングのロジックを動的に変更する。


### 2. CASE式の基本構文

CASE式には「シンプルCASE式」と「検索CASE式」の2つの書き方があります。

#### シンプルCASE式
特定の**1つの列の値**が何かによって処理を分岐させる場合に使用します。

```sql
CASE 列名
  WHEN 値1 THEN 結果1
  WHEN 値2 THEN 結果2
  ...
  [ELSE デフォルト結果]
END
```

#### 検索CASE式
より複雑な**条件式**（比較演算子や論理演算子など）を用いて分岐させる、より柔軟で一般的に使われる形式です。

```sql
CASE
  WHEN 条件式1 THEN 結果1
  WHEN 条件式2 THEN 結果2
  ...
  [ELSE デフォルト結果]
END
```

**重要なポイント:**
*   `WHEN`句は上から順に評価され、最初に真 (TRUE) となった条件の結果が返されます。**記述する順序が非常に重要です。**
*   `ELSE`句を省略し、どの`WHEN`句にも合致しなかった場合、結果は `NULL` となります。



### 3. 利用シーン別の活用例

#### A. `SELECT`句での利用：データの変換と分類

`SELECT`句でCASE式を使うと、既存のデータから新しい意味を持つ列を動的に作成できます。

**例：社員の役職に応じて「役割」を分類する**
社員テーブルの役職（`position`）に基づいて、新しい「役割（`role`）」列を作成します。

**SQLクエリ（WITH句で事前データを作成）**
```sql
WITH employees AS (
  SELECT '山田 太郎' AS name, '部長' AS position UNION ALL
  SELECT '鈴木 花子', '課長' UNION ALL
  SELECT '佐藤 次郎', 'リーダー' UNION ALL
  SELECT '田中 三郎', '一般' UNION ALL
  SELECT '高橋 四郎', NULL
)
SELECT
  name,
  position,
  CASE position
    WHEN '部長' THEN '経営層'
    WHEN '課長' THEN '管理職'
    WHEN 'リーダー' THEN '管理職'
    ELSE 'メンバー'
  END AS role
FROM
  employees;
```

**実行結果**

| name  | position | role |
| :---- | :------- | :--- |
| 山田 太郎 | 部長       | 経営層  |
| 鈴木 花子 | 課長       | 管理職  |
| 佐藤 次郎 | リーダー     | 管理職  |
| 田中 三郎 | 一般       | メンバー |
| 高橋 四郎 | NULL     | メンバー |
*`position`が`NULL`の高橋さんは、どの`WHEN`句にも合致しないため`ELSE`句の「メンバー」が適用されます。*
**注意**：case式の結果は、同じ型を指定しなければなりません！- **「型の一致」**: THEN や ELSE で返す値のデータ型はすべて一致（あるいは暗黙的に変換可能）していなければならないこと（例えば、あるWHENで数値を返し、別のWHENで文字列を返すことはできない）。

##### 補足： `SELECT`句での利用：単一行関数との組み合わせ

CASE式の中に単一行関数（`LENGTH`, `COALESCE`, `ROUND`, `REPLACE` など）を組み込んだり、逆に関数の引数にCASE式を入れたりすることで、より高度なデータ整形が可能になります。

**例：データクレンジングと表示の最適化**
商品テーブルのデータを使って、以下の処理を一度に行います。
1.  商品名（`name`）が未入力(`NULL`)なら「不明」と表示（`COALESCE`関数）
2.  商品名が長すぎる場合は、10文字で切り取って「...」を付与（`LENGTH`, `LEFT`関数）
3.  価格（`price`）に応じて、切り捨てか四捨五入か計算方法を変える（`FLOOR`, `ROUND`関数）

**SQLクエリ**
```sql
WITH products AS (
  SELECT 'プレミアムノートパソコン 2024モデル' AS name, 1550.75 AS price UNION ALL
  SELECT 'マウス', 25.40 UNION ALL
  SELECT NULL, 10.00 UNION ALL
  SELECT 'USBメモリ', 12.99
)
SELECT
  -- 1&2. 名前がNULLなら代替、長ければカット
  CASE 
    WHEN name IS NULL THEN '（名称未設定）'
    WHEN LENGTH(name) > 10 THEN LEFT(name, 10) || '...'
    ELSE name
  END AS display_name,

  price,

  -- 3. 価格帯によって端数処理を変える
  CASE 
    WHEN price >= 1000 THEN FLOOR(price) -- 高額商品は切り捨て
    ELSE ROUND(price, 1)                -- 安価なものは小数第1位まで
  END AS adjusted_price
FROM
  products;
```

**実行結果**

| display_name | price | adjusted_price |
| :--- | :--- | :--- |
| プレミアムノートパ... | 1550.75 | 1550 |
| マウス | 25.40 | 25.4 |
| （名称未設定） | 10.00 | 10.0 |
| USBメモリ | 12.99 | 13.0 |

**解説：**
*   **`LENGTH(name) > 10`**: 文字列関数で条件を判定しています。
*   **`LEFT(name, 10) || '...'`**: 関数と結合演算子を使って、合致した時の戻り値を加工しています。
*   **`FLOOR` / `ROUND`**: 数値関数を使い分けることで、ビジネスルールに基づいた端数処理を出し分けています。


##### 補足：関数の中でCASE式を使う：関数の引数としての活用

CASE式はそれ自体が「一つの値」として扱われるため、**関数の引数（中身）**に直接記述することができます。これにより、条件によって加工する値そのものを切り替えるような柔軟な処理が可能になります。

**例3：条件に応じた計算結果をまとめて関数で処理する**
「商品カテゴリによって税率を変えて計算し、その結果を最後に四捨五入（`ROUND`関数）する」といったケースです。

**SQLクエリ**
```sql
WITH sales_data AS (
  SELECT 'おにぎり' AS item, '食品' AS category, 120 AS price UNION ALL
  SELECT 'ビール' AS item, '酒類' AS category, 250 AS price UNION ALL
  SELECT '雑誌' AS item, 'その他' AS category, 700 AS price
)
SELECT
  item,
  category,
  price,
  -- ROUND関数の第1引数として、CASE式による計算結果を渡す
  ROUND(
    CASE
      WHEN category = '食品' THEN price * 1.08 -- 軽減税率
      WHEN category = '酒類' THEN price * 1.10 -- 標準税率
      ELSE price * 1.10
    END,
    0
  ) AS tax_included_price
FROM
  sales_data;
```

**例4：条件に応じて表示形式を組み立てる（文字列結合関数との組み合わせ）**
`CONCAT`関数（文字列結合）の中で、会員ランクに応じて名前に付与するラベルを切り替えます。

**SQLクエリ**
```sql
WITH members AS (
  SELECT '田中' AS name, 'gold' AS rank UNION ALL
  SELECT '鈴木' AS name, 'silver' AS rank UNION ALL
  SELECT '佐藤' AS name, 'standard' AS rank
)
SELECT
  name,
  -- CONCAT関数の引数の中でCASE式を使用
  CONCAT(
    name,
    '様',
    CASE 
      WHEN rank = 'gold' THEN '【VIP特典対象】'
      WHEN rank = 'silver' THEN '【優待対象】'
      ELSE '' 
    END
  ) AS display_label
FROM
  members;
```

**実行結果（例4）**

| name | display_label |
| :--- | :--- |
| 田中 | 田中様【VIP特典対象】 |
| 鈴木 | 鈴木様【優待対象】 |
| 佐藤 | 佐藤様 |


#### B. `ORDER BY`句での利用：並び順のカスタマイズ

`ORDER BY`句でCASE式を使うと、デフォルトの昇順・降順では実現できない、ビジネスルールに基づいた複雑なソート順を定義できます。

**例：役職順でソートし、同じ役職内では入社日の古い順に並べる**
「部長 → 課長 → リーダー → 一般」という独自の役職順で並べ替え、役職が`NULL`の社員は最後に表示します。

**SQLクエリ（WITH句で事前データを作成）**
```sql
WITH employees AS (
  SELECT '佐藤 次郎' AS name, 'リーダー' AS position, '2018-04-01'::date AS hire_date UNION ALL
  SELECT '山田 太郎', '部長', '2010-04-01'::date UNION ALL
  SELECT '田中 三郎', '一般', '2022-04-01'::date UNION ALL
  SELECT '伊藤 五郎', '一般', '2020-10-01'::date UNION ALL
  SELECT '鈴木 花子', '課長', '2015-04-01'::date UNION ALL
  SELECT '高橋 四郎', NULL, '2023-04-01'::date
)
SELECT
  name,
  position,
  hire_date
FROM
  employees
ORDER BY
  -- 役職に優先順位を付ける
  CASE position
    WHEN '部長'   THEN 1
    WHEN '課長'   THEN 2
    WHEN 'リーダー' THEN 3
    WHEN '一般'   THEN 4
    ELSE 5 -- NULLやその他の役職は最後
  END,
  -- 同じ役職内では入社日の昇順（古い順）
  hire_date;
```

**実行結果**

| name | position | hire_date |
|:---|:---|:---|
| 山田 太郎 | 部長 | 2010-04-01 |
| 鈴木 花子 | 課長 | 2015-04-01 |
| 佐藤 次郎 | リーダー | 2018-04-01 |
| 伊藤 五郎 | 一般 | 2020-10-01 |
| 田中 三郎 | 一般 | 2022-04-01 |
| 高橋 四郎 | NULL | 2023-04-01 |
*`ORDER BY`句の1番目のCASE式で全体の並び順を制御し、2番目の`hire_date`でグループ内の並び順を制御しています。*

補足：
```SQL
WITH employees AS (
  SELECT '佐藤 次郎' AS name, 'リーダー' AS position, '2018-04-01'::date AS hire_date UNION ALL
  SELECT '山田 太郎', '部長', '2010-04-01'::date UNION ALL
  SELECT '田中 三郎', '一般', '2022-04-01'::date UNION ALL
  SELECT '伊藤 五郎', '一般', '2020-10-01'::date UNION ALL
  SELECT '鈴木 花子', '課長', '2015-04-01'::date UNION ALL
  SELECT '高橋 四郎', NULL, '2023-04-01'::date
)
SELECT
  name,
  position,
  hire_date
FROM
  employees
ORDER BY
  -- 役職に優先順位を付ける
  CASE position
    WHEN '部長'   THEN 'ああああああ'
    WHEN '課長'   THEN 'あい'
    WHEN 'リーダー' THEN 'あうあ'
    WHEN '一般'   THEN 'あえ'
    ELSE 'あおおおおお' -- NULLやその他の役職は最後
  END,
  -- 同じ役職内では入社日の昇順（古い順）
  hire_date;
```

⇒order by は同一の型であればそれを比べてソートできます。文字列を渡せば辞書順に。


#### C. 集計関数との組み合わせ：クロス集計

クロス集計は、**「行」のデータを「列」に変換**して、データを比較しやすくするための強力なテクニックです。例えば、「月ごとのカテゴリ別売上」のようなレポートを作成する際に非常に役立ちます。

##### Step 1: なぜクロス集計が必要か？（動機）

あなたは上司から「**月ごとに、食品、書籍、家電の売上を比較したい**」と依頼されました。
まず、通常の`GROUP BY`でデータを集計してみましょう。

**SQLクエリ (通常のGROUP BY)**
```sql
WITH sales AS (
  SELECT '2023-01-10'::date AS sale_date, '食品' AS category, 1000 AS amount UNION ALL
  SELECT '2023-01-15'::date, '書籍', 2000 UNION ALL
  SELECT '2023-01-20'::date, '食品', 1500 UNION ALL
  SELECT '2023-02-05'::date, '家電', 50000 UNION ALL
  SELECT '2023-02-12'::date, '書籍', 3000 UNION ALL
  SELECT '2023-02-18'::date, '食品', 1200
)
SELECT
  TO_CHAR(sale_date, 'YYYY-MM') AS sale_month,
  category,
  SUM(amount) AS total_amount
FROM
  sales
GROUP BY
  sale_month,
  category
ORDER BY
  sale_month,
  category;
```

**実行結果 (縦長のデータ)**

| sale_month | category | total_amount |
|:---|:---|:---|
| 2023-01 | 書籍 | 2000 |
| 2023-01 | 食品 | 2500 |
| 2023-02 | 家電 | 50000 |
| 2023-02 | 書籍 | 3000 |
| 2023-02 | 食品 | 1200 |

この結果は正しいですが、月ごとのカテゴリ売上を**一目で比較するのが難しい**です。これを解決するのがクロス集計です。

##### Step 2: クロス集計の考え方

クロス集計では、以下の3つの要素を考えます。
1.  **何を行の軸にするか？** → `GROUP BY`句で指定する列（今回は「月」）
2.  **何を新しい列にしたいか？** → `CASE`式で条件分岐させる列（今回は「カテゴリ」）
3.  **各セルで何を計算したいか？** → 集計関数（`SUM`, `COUNT`など）で計算する値（今回は「売上金額」）

##### Step 3: CASE式でクロス集計を実装する

上記の考え方に基づき、SQLを作成します。「カテゴリ」の各値を`CASE`式で新しい列に変換し、`SUM`関数で集計します。

**SQLクエリ (クロス集計)**
```sql
WITH sales AS (
  SELECT '2023-01-10'::date AS sale_date, '食品' AS category, 1000 AS amount UNION ALL
  SELECT '2023-01-15'::date, '書籍', 2000 UNION ALL
  SELECT '2023-01-20'::date, '食品', 1500 UNION ALL
  SELECT '2023-02-05'::date, '家電', 50000 UNION ALL
  SELECT '2023-02-12'::date, '書籍', 3000 UNION ALL
  SELECT '2023-02-18'::date, '食品', 1200
)
SELECT
  TO_CHAR(sale_date, 'YYYY-MM') AS sale_month,
  -- categoryが'食品'の行だけamountを合計し、それ以外は0を合計する
  SUM(CASE WHEN category = '食品' THEN amount ELSE 0 END) AS food_sales,
  -- categoryが'書籍'の行だけamountを合計する
  SUM(CASE WHEN category = '書籍' THEN amount ELSE 0 END) AS book_sales,
  -- categoryが'家電'の行だけamountを合計する
  SUM(CASE WHEN category = '家電' THEN amount ELSE 0 END) AS electronics_sales
FROM
  sales
GROUP BY
  sale_month
ORDER BY
  sale_month;
```

**実行結果 (横長の比較しやすいデータ)**

| sale_month | food_sales | book_sales | electronics_sales |
|:---|:---|:---|:---|
| 2023-01 | 2500 | 2000 | 0 |
| 2023-02 | 1200 | 3000 | 50000 |

このように、カテゴリが列になり、月ごとの売上比較が格段に容易になりました。

**💡 PostgreSQLなら `FILTER` 句がもっと便利！**
PostgreSQLでは、集計関数に直接`FILTER`句を指定することで、より直感的かつ簡潔に同じ処理を記述できます。

```sql
-- (WITH句は上記と同じ)
SELECT
  TO_CHAR(sale_date, 'YYYY-MM') AS sale_month,
  SUM(amount) FILTER (WHERE category = '食品') AS food_sales,
  SUM(amount) FILTER (WHERE category = '書籍') AS book_sales,
  SUM(amount) FILTER (WHERE category = '家電') AS electronics_sales
FROM
  sales
GROUP BY
  sale_month
ORDER BY
  sale_month;
```



#### D. `WHERE`句での利用：動的なフィルタリング (⚠️注意点あり)

`WHERE`句でCASE式を使うと、条件によってフィルタリングのロジックを切り替えられますが、**一般的には推奨されません**。

**例4：会員ランクに応じて表示条件を変更する**
「ゴールド会員」は全商品を表示し、「シルバー会員」は購入金額が3,000円以上の履歴のみ表示する、という動的なフィルタリングを行います。

**SQLクエリ (CASE式 - 非推奨)**
```sql
WITH customer_purchases AS (
  SELECT 'ゴールド会員' AS rank, '商品A' AS item, 5000 AS amount UNION ALL
  SELECT 'ゴールド会員', '商品B', 1000 UNION ALL
  SELECT 'シルバー会員', '商品C', 4000 UNION ALL
  SELECT 'シルバー会員', '商品D', 2000
)
SELECT
  rank, item, amount
FROM
  customer_purchases
WHERE
  CASE
    WHEN rank = 'ゴールド会員' THEN TRUE -- ゴールド会員は常に表示
    WHEN rank = 'シルバー会員' THEN amount >= 3000 -- シルバー会員は金額でフィルタ
    ELSE FALSE
  END;
```

⇒where句は行ごとに評価されるのでしたね？
まずゴールド会員の行は常にTRUEが返るため、表示対象です。
次にシルバー会員の行は金額が3000円以上の場合TRUEが返るため、その場合は表示対象で、その他は表示対象外です。
最後にその他の行は常にFALSEが返るので、表示対象外です。


**実務上のアンチパターンと推奨される書き方**
`WHERE`句でのCASE式の使用は、クエリの**可読性を著しく低下**させ、データベースがインデックスを効率的に利用できず**パフォーマンスが劣化**する原因となるため、通常は避けるべきです。
このロジックは、`AND`と`OR`を使った方がはるかに明確で効率的です。

**SQLクエリ (推奨)**
```sql
-- (WITH句は上記と同じ)
SELECT
  rank, item, amount
FROM
  customer_purchases
WHERE
  (rank = 'ゴールド会員')
  OR
  (rank = 'シルバー会員' AND amount >= 3000);
```

**実行結果 (どちらのクエリも同じ)**

| rank | item | amount |
|:---|:---|:---|
| ゴールド会員 | 商品A | 5000 |
| ゴールド会員 | 商品B | 1000 |
| シルバー会員 | 商品C | 4000 |

**条件式が複雑になった例**
下記どちらも同じ条件ですが、読みにくいですよね。。。

case式を使った書き方：
```SQL
-- どこからどこまでが一つの条件か、非常に分かりにくい
WHERE
  CASE
    WHEN stock_quantity >= 100 THEN ( -- 在庫条件
      CASE
        WHEN price > 500 THEN ( -- 価格条件
          CASE
            WHEN category = '家電' THEN TRUE -- カテゴリ条件
            ELSE FALSE
          END
        )
        ELSE FALSE
      END
    )
    ELSE TRUE -- それ以外はすべて表示
  END;
```
普通の書き方：
```SQL
-- 条件ブロックが明確で、追加や修正も容易
WHERE
  (stock_quantity >= 100 AND price > 500 AND category = '家電')
  OR
  (stock_quantity < 100);
```
