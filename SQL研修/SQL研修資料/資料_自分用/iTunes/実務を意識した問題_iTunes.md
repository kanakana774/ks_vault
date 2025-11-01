
### 業務システム開発を想定した実践的SQL演習問題 (基本編・10選)

#### 問題1：【帳票作成】月次請求一覧データの作成

**シナリオ:**
経理システムで、指定した年月（例：2022年5月）に発行された請求書の一覧を帳票として出力する必要があります。顧客名、請求日、請求金額を一覧で取得してください。

**アピールポイント:**
業務システムの基本である「帳票作成」を想定した、最も基本的なデータ抽出スキルです。`JOIN`と`WHERE`句による期間指定は、あらゆる業務で必須となります。

**回答例:**
```sql
SELECT
  c.first_name || ' ' || c.last_name AS customer_name,
  i.invoice_date,
  i.total
FROM invoice AS i
JOIN customer AS c
  ON i.customer_id = c.customer_id
WHERE
  i.invoice_date >= '2022-05-01' AND i.invoice_date < '2022-06-01'
ORDER BY
  i.invoice_date;
```

---

#### 問題2：【データ不整合調査】請求ヘッダと明細の合計金額チェック

**シナリオ:**
システムの不具合により、請求書ヘッダ（`invoice`テーブル）の合計金額(`total`)と、それに紐づく請求明細（`invoice_line`テーブル）の金額（`unit_price * quantity`）の合計が一致しないデータが発生した可能性があります。この不整合がある請求書のIDを特定してください。

**アピールポイント:**
障害調査の基本である「データ整合性チェック」を行うSQLが書けることを示せます。サブクエリ（またはCTE）と`GROUP BY`を使った比較は、データ品質を担保する上で非常に重要なスキルです。

**回答例:**
```sql
SELECT
  i.invoice_id,
  i.total AS header_total,
  SUM(il.unit_price * il.quantity) AS detail_total
FROM invoice AS i
JOIN invoice_line AS il
  ON i.invoice_id = il.invoice_id
GROUP BY
  i.invoice_id,
  i.total
HAVING
  i.total != SUM(il.unit_price * il.quantity)
ORDER BY
  i.invoice_id;
```

---

#### 問題3：【画面表示用データ】顧客詳細画面の担当者情報取得

**シナリオ:**
顧客管理システムの詳細画面で、顧客情報とあわせて、その顧客のサポート担当者名と役職も表示する必要があります。特定の顧客（例：`customer_id` = 15）の氏名、国、および担当従業員の氏名と役職を取得してください。

**アピールポイント:**
アプリケーションの特定画面で必要となる関連情報を、複数のテーブルから適切に`JOIN`して一度に取得する能力をアピールできます。`LEFT JOIN`を使うことで、担当者が未設定の顧客にも対応できることを示唆できます。

**回答例:**
```sql
SELECT
  c.first_name || ' ' || c.last_name AS customer_name,
  c.country,
  e.first_name || ' ' || e.last_name AS support_rep_name,
  e.title AS support_rep_title
FROM customer AS c
LEFT JOIN employee AS e
  ON c.support_rep_id = e.employee_id
WHERE
  c.customer_id = 15;
```

---

#### 問題4：【バッチ処理】更新対象顧客のリストアップ

**シナリオ:**
夜間バッチ処理で、最終購入日から1年以上経過した顧客を「休眠」ステータスに更新する処理があります。その前段として、更新対象となる顧客のIDと氏名、最終購入日をリストアップしてください。

**アピールポイント:**
バッチ処理の基本である「更新対象の抽出」ロジックを組めることを示します。`GROUP BY`と`HAVING`を使い、集計結果を条件にデータを絞り込むスキルは、データメンテナンス業務で頻繁に求められます。

**回答例:**
```sql
SELECT
  c.customer_id,
  c.first_name || ' ' || c.last_name AS customer_name,
  MAX(i.invoice_date) AS last_purchase_date
FROM customer AS c
JOIN invoice AS i
  ON c.customer_id = i.customer_id
GROUP BY
  c.customer_id,
  customer_name
HAVING
  MAX(i.invoice_date) < (CURRENT_DATE - INTERVAL '1 year')
ORDER BY
  last_purchase_date;
```

---

#### 問題5：【データクレンジング】孤立データの検出

**シナリオ:**
データ移行ミスで、アーティスト情報（`artist`テーブル）が存在しないにもかかわらず、アルバム（`album`テーブル）だけが登録されているデータがないか調査します。そのような「孤立した」アルバムのIDとタイトルを検出してください。

**アピールポイント:**
`LEFT JOIN`と`IS NULL`（または`NOT EXISTS`）を使い、マスタデータとの紐付きが切れた「不正データ」を検出するスキルです。これはデータクレンジングや障害調査において非常に重要なテクニックです。

**回答例:**
```sql
SELECT
  al.album_id,
  al.title
FROM album AS al
LEFT JOIN artist AS ar
  ON al.artist_id = ar.artist_id
WHERE
  ar.artist_id IS NULL
ORDER BY
  al.album_id;
```

---

#### 問題6：【人事システム】従業員と上司の一覧作成

**シナリオ:**
人事システムで組織図データを表示するため、全従業員の氏名とその直属の上司の氏名を一覧で出力する必要があります。上司がいない従業員（最上位の役職者）もリストに含めてください。

**アピールポイント:**
自己結合（Self-Join）を使い、同一テーブル内で親子関係（階層構造）を扱うスキルを示せます。人事、商品カテゴリ、勘定科目など、階層構造を持つデータの扱いは業務システム開発の定番です。

**回答例:**
```sql
SELECT
  e1.first_name || ' ' || e1.last_name AS employee_name,
  COALESCE(e2.first_name || ' ' || e2.last_name, 'N/A') AS manager_name
FROM employee AS e1
LEFT JOIN employee AS e2
  ON e1.reports_to = e2.employee_id
ORDER BY
  manager_name,
  employee_name;
```

---

#### 問題7：【販売管理】商品（トラック）別売上集計

**シナリオ:**
販売管理システムで、商品（トラック）ごとの売上実績を確認したいと考えています。各トラックについて、売れた合計数量と売上総額を計算してください。一度も売れたことのないトラックもリストに表示してください。

**アピールポイント:**
商品マスタ（`track`）を基準に、売上実績（`invoice_line`）を集計する、販売管理システムの基本帳票を作成するスキルです。`LEFT JOIN`と集計関数`COALESCE`の組み合わせは実務で頻出します。

**回答例:**
```sql
SELECT
  t.name AS track_name,
  COALESCE(SUM(il.quantity), 0) AS total_quantity_sold,
  COALESCE(SUM(il.quantity * il.unit_price), 0) AS total_sales
FROM track AS t
LEFT JOIN invoice_line AS il
  ON t.track_id = il.track_id
GROUP BY
  t.track_id,
  t.name
ORDER BY
  total_sales DESC;
```

---

#### 問題8：【請求書明細の出力】

**シナリオ:**
帳票システムが、特定の請求書（例：`invoice_id` = 400）の明細を印刷します。その請求書に含まれるすべての商品（トラック）について、トラック名、単価、数量、および小計（単価×数量）をリストで取得してください。

**アピールポイント:**
請求書や納品書のような、ヘッダと明細から成る帳票の「明細部分」を作成するためのSQLです。アプリケーションから渡されたIDを元に、必要な情報を複数テーブルから取得・計算する、非常に実践的なスキルです。

**回答例:**
```sql
SELECT
  t.name AS track_name,
  il.unit_price,
  il.quantity,
  (il.unit_price * il.quantity) AS line_total
FROM invoice_line AS il
JOIN track AS t
  ON il.track_id = t.track_id
WHERE
  il.invoice_id = 400
ORDER BY
  t.name;
```

---

#### 問題9：【ETL処理】複数条件でのデータ集計

**シナリオ:**
データウェアハウスに送るための集計データを作成します（ETL処理の一部）。メディアタイプ（CD, DVDなど）ごと、かつジャンルごとに、トラックの総数と平均収録時間（ミリ秒）を算出してください。

**アピールポイント:**
`GROUP BY`句に複数のカラムを指定し、複数の軸でデータを集計する能力を示します。これは、データ分析基盤へデータを提供するバッチ処理などで日常的に使われる手法です。

**回答例:**
```sql
SELECT
  mt.name AS media_type_name,
  g.name AS genre_name,
  COUNT(t.track_id) AS number_of_tracks,
  AVG(t.milliseconds) AS average_duration_ms
FROM track AS t
JOIN media_type AS mt
  ON t.media_type_id = mt.media_type_id
JOIN genre AS g
  ON t.genre_id = g.genre_id
GROUP BY
  mt.name,
  g.name
ORDER BY
  mt.name,
  g.name;
```

---

#### 問題10：【マスタデータ一覧】プレイリストに含まれるトラック

**シナリオ:**
アプリケーションで、特定のプレイリスト（例：`playlist_id` = 1）にどの曲が含まれているかを表示する必要があります。そのプレイリストに含まれる全トラックの名称をアーティスト名と共に取得してください。

**アピールポイント:**
中間テーブル（`playlist_track`）を介した多対多のリレーションシップを理解し、データを正しく`JOIN`できることを示します。これはマスタ管理機能や、Webサイトの関連情報表示などで頻繁に登場するパターンです。

**回答例:**
```sql
SELECT
  t.name AS track_name,
  ar.name AS artist_name
FROM playlist_track AS pt
JOIN track AS t
  ON pt.track_id = t.track_id
JOIN album AS al
  ON t.album_id = al.album_id
JOIN artist AS ar
  ON al.artist_id = ar.artist_id
WHERE
  pt.playlist_id = 1
ORDER BY
  ar.name,
  t.name;
```


---

### 業務システム開発を想定した実践的SQL演習問題 (応用編・10選)

#### 問題1：【KPI分析】国別トップ顧客とその売上貢献率

**シナリオ:**
各国の営業担当が、自国の最重要顧客を把握するため、国ごとに最も購入金額が高い顧客を特定し、その顧客がその国の総売上に占める割合（貢献率）を算出する必要があります。

**アピールポイント:**
ウィンドウ関数 `RANK()` と `SUM() OVER` を組み合わせ、グループ内でのランキング付けとグループ全体に対する個別の割合計算を一度に行う、高度な分析SQLが書けることを示せます。

**回答例:**
```sql
WITH CustomerCountrySales AS (
  SELECT
    c.country,
    c.first_name || ' ' || c.last_name AS customer_name,
    SUM(i.total) AS total_purchase
  FROM customer AS c
  JOIN invoice AS i
    ON c.customer_id = i.customer_id
  GROUP BY
    c.country,
    c.customer_id,
    customer_name
), CountrySalesRank AS (
  SELECT
    country,
    customer_name,
    total_purchase,
    SUM(total_purchase) OVER (PARTITION BY country) AS total_country_sales,
    RANK() OVER (PARTITION BY country ORDER BY total_purchase DESC) AS rank_in_country
  FROM CustomerCountrySales
)
SELECT
  country,
  customer_name,
  total_purchase,
  total_country_sales,
  (total_purchase / total_country_sales) * 100 AS contribution_percentage
FROM CountrySalesRank
WHERE
  rank_in_country = 1
ORDER BY
  total_country_sales DESC;
```

---

#### 問題2：【時系列分析】月次売上の前月比成長率

**シナリオ:**
経営会議用のレポートとして、会社全体の月次売上と、その前月比成長率（%）を時系列で表示する必要があります。

**アピールポイント:**
CTEで月次売上を集計し、ウィンドウ関数 `LAG()` を使って「1行前のデータ（＝前月の売上）」を取得し、時系列での変化率を計算するスキルをアピールできます。これはKPIモニタリングバッチの典型的なロジックです。

**回答例:**
```sql
WITH MonthlySales AS (
  SELECT
    DATE_TRUNC('month', invoice_date)::date AS sales_month,
    SUM(total) AS monthly_total_sales
  FROM invoice
  GROUP BY
    sales_month
), MonthlySalesGrowth AS (
  SELECT
    sales_month,
    monthly_total_sales,
    LAG(monthly_total_sales, 1, 0) OVER (ORDER BY sales_month) AS previous_month_sales
  FROM MonthlySales
)
SELECT
  sales_month,
  monthly_total_sales,
  previous_month_sales,
  CASE
    WHEN previous_month_sales > 0
    THEN ((monthly_total_sales - previous_month_sales) / previous_month_sales) * 100
    ELSE NULL
  END AS growth_percentage
FROM MonthlySalesGrowth
ORDER BY
  sales_month;```

---

#### 問題3：【データ整合性】マスタに存在するが一度も購入されていない商品

**シナリオ:**
販売管理システムのマスタデータ棚卸しのため、「商品は存在する（`track`テーブルに登録済み）が、一度も購入されたことがない（`invoice_line`に一度も登場しない）」トラックをリストアップする必要があります。

**アピールポイント:**
`LEFT JOIN ... IS NULL` や `NOT EXISTS` 句を使い、「関連テーブルに存在しない」レコードを効率的に見つけ出すスキルを示します。データクレンジングやマスタデータのメンテナンスバッチで必須のテクニックです。

**回答例:**
```sql
SELECT
  t.name AS track_name,
  ar.name AS artist_name
FROM track AS t
LEFT JOIN invoice_line AS il
  ON t.track_id = il.track_id
JOIN album AS al
  ON t.album_id = al.album_id
JOIN artist AS ar
  ON al.artist_id = ar.artist_id
WHERE
  il.invoice_line_id IS NULL
ORDER BY
  ar.name,
  t.name;
```

---

#### 問題4：【顧客分析】顧客ごとの累積購入金額の推移

**シナリオ:**
顧客のLTV（生涯価値）を分析するため、各顧客の購入履歴について、購入時点ごとの「累積購入金額」を算出したい。これにより、どのタイミングで優良顧客化したかを分析できます。

**アピールポイント:**
ウィンドウ関数 `SUM() OVER` の `ORDER BY` 句を使い、時系列に沿った累積合計を算出するスキルをアピールできます。アプリケーションの購入履歴画面や、分析用サマリーテーブルの作成で非常に役立ちます。

**回答例:**
```sql
SELECT
  c.first_name || ' ' || c.last_name AS customer_name,
  i.invoice_date,
  i.total AS purchase_amount,
  SUM(i.total) OVER (PARTITION BY c.customer_id ORDER BY i.invoice_date, i.invoice_id) AS cumulative_total
FROM customer AS c
JOIN invoice AS i
  ON c.customer_id = i.customer_id
ORDER BY
  customer_name,
  i.invoice_date;
```

---

#### 問題5：【人事・営業管理】担当顧客の平均購入額が全社平均を超える営業担当

**シナリオ:**
優秀な営業担当者を表彰するため、「自身の担当顧客の平均購入額」が、「全顧客の平均購入額」を上回っている従業員をリストアップしてください。

**アピールポイント:**
CTEを複数使って、段階的に集計（担当者ごとの平均、全社平均）を行い、それらを比較するロジックを組み立てる能力を示せます。複雑な集計要件を、分解して考える力が試されます。

**回答例:**
```sql
WITH EmployeeCustomerSales AS (
  SELECT
    e.employee_id,
    e.first_name || ' ' || e.last_name AS employee_name,
    AVG(i.total) AS avg_purchase_per_customer
  FROM employee AS e
  JOIN customer AS c
    ON e.employee_id = c.support_rep_id
  JOIN invoice AS i
    ON c.customer_id = i.customer_id
  GROUP BY
    e.employee_id,
    employee_name
), OverallAverageSales AS (
  SELECT
    AVG(total) AS overall_avg_purchase
  FROM invoice
)
SELECT
  ecs.employee_name,
  ecs.avg_purchase_per_customer,
  oas.overall_avg_purchase
FROM EmployeeCustomerSales AS ecs, OverallAverageSales AS oas
WHERE
  ecs.avg_purchase_per_customer > oas.overall_avg_purchase
ORDER BY
  ecs.avg_purchase_per_customer DESC;
```

---

#### 問題6：【障害調査】購入履歴はあるが顧客マスタに存在しない請求

**シナリオ:**
システムの不具合で顧客データが削除されてしまった可能性があります。`invoice`テーブルには存在する`customer_id`が、`customer`マスタテーブルに存在しない「孤立した請求データ」がないか調査してください。

**アピールポイント:**
問題3と考え方は似ていますが、こちらはマスタデータ側が欠損しているケースです。`NOT EXISTS`句を使った相関サブクエリは、このようなデータ不整合調査で非常に強力なツールとなります。

**回答例:**
```sql
SELECT
  i.invoice_id,
  i.customer_id,
  i.invoice_date,
  i.total
FROM invoice AS i
WHERE
  NOT EXISTS (
    SELECT 1
    FROM customer AS c
    WHERE i.customer_id = c.customer_id
  );
```

---

#### 問題7：【バッチ処理】顧客ランク付け

**シナリオ:**
夜間バッチで、全顧客を購入総額に基づいて4つのランク（Quartile 1: 上位25%, Quartile 2: 26-50%, ...）に分類する処理を実装します。各顧客の氏名、購入総額、およびランク番号（1〜4）を取得してください。

**アピールポイント:**
ウィンドウ関数 `NTILE()` を使い、データを自動的にグループ分けするスキルを示せます。これは顧客セグメンテーションや分析用のランク付けバッチで非常に実用的な機能です。

**回答例:**
```sql
WITH CustomerTotalSales AS (
  SELECT
    c.customer_id,
    c.first_name || ' ' || c.last_name AS customer_name,
    SUM(i.total) AS total_purchase
  FROM customer AS c
  LEFT JOIN invoice AS i
    ON c.customer_id = i.customer_id
  GROUP BY
    c.customer_id,
    customer_name
)
SELECT
  customer_name,
  COALESCE(total_purchase, 0) AS total_purchase,
  NTILE(4) OVER (ORDER BY COALESCE(total_purchase, 0) DESC) AS sales_quartile
FROM CustomerTotalSales
ORDER BY
  sales_quartile,
  total_purchase DESC;
```

---

#### 問題8：【階層データ集計】マネージャーとその部下の担当売上合計

**シナリオ:**
人事評価のため、マネージャー職（`employee`テーブルで誰かの`reports_to`になっている従業員）について、その直属の部下たちが担当する顧客の売上合計を算出してください。

**アピールポイント:**
自己結合でマネージャーと部下の関係を定義し、さらに`JOIN`を重ねて売上データを集計する、複雑な階層集計スキルを示せます。CTEを使うことで、この複雑な処理を分かりやすく記述できます。

**回答例:**
```sql
WITH ManagerSubordinates AS (
  SELECT
    mgr.employee_id AS manager_id,
    mgr.first_name || ' ' || mgr.last_name AS manager_name,
    emp.employee_id AS employee_id
  FROM employee AS mgr
  JOIN employee AS emp
    ON mgr.employee_id = emp.reports_to
)
SELECT
  ms.manager_name,
  COALESCE(SUM(i.total), 0) AS subordinates_total_sales
FROM ManagerSubordinates AS ms
LEFT JOIN customer AS c
  ON ms.employee_id = c.support_rep_id
LEFT JOIN invoice AS i
  ON c.customer_id = i.customer_id
GROUP BY
  ms.manager_id,
  ms.manager_name
ORDER BY
  subordinates_total_sales DESC;
```

---

#### 問題9：【クロスセル分析】"Rock"を購入した顧客が他に購入したジャンル

**シナリオ:**
販売戦略として、クロスセル（合わせ買い）を促進したいと考えています。"Rock"ジャンルの曲を購入したことがある顧客は、他にどのジャンルの曲を購入する傾向にあるのか、その組み合わせと購入回数を調べてください。

**アピールポイント:**
`INTERSECT`や`EXISTS`を使い、まず「Rockを購入した顧客」を特定し、その顧客群の全購入履歴を再集計するという、分析的なアプローチをSQLで組み立てる能力を示せます。

**回答例:**
```sql
WITH RockCustomers AS (
  SELECT DISTINCT i.customer_id
  FROM invoice_line il
  JOIN track t ON il.track_id = t.track_id
  JOIN genre g ON t.genre_id = g.genre_id
  JOIN invoice i ON il.invoice_id = i.invoice_id
  WHERE g.name = 'Rock'
)
SELECT
  g.name AS other_genre_purchased,
  COUNT(il.invoice_line_id) AS purchase_count
FROM invoice_line il
JOIN track t ON il.track_id = t.track_id
JOIN genre g ON t.genre_id = g.genre_id
JOIN invoice i ON il.invoice_id = i.invoice_id
WHERE
  i.customer_id IN (SELECT customer_id FROM RockCustomers)
  AND g.name != 'Rock'
GROUP BY
  g.name
ORDER BY
  purchase_count DESC;
```

---

#### 問題10：【View作成】顧客ごとの最新購入情報サマリー

**シナリオ:**
アプリケーションの様々な画面から、顧客ごとの「合計購入額」「合計購入回数」「最終購入日」を頻繁に参照します。パフォーマンス向上のため、これらの情報をまとめたビュー（`V_CustomerSummary`）を作成するSQLを設計してください。

**アピールポイント:**
特定の集計ロジックをビューとして定義し、再利用性を高めるという、データベース設計の基本スキルを示せます。ビューの元となる`SELECT`文は、これまでの集計スキルの集大成となります。

**回答例:**
```sql
CREATE OR REPLACE VIEW V_CustomerSummary AS
SELECT
  c.customer_id,
  c.first_name || ' ' || c.last_name AS customer_name,
  c.country,
  COUNT(i.invoice_id) AS total_invoices,
  COALESCE(SUM(i.total), 0) AS total_sales,
  MAX(i.invoice_date) AS last_purchase_date
FROM customer AS c
LEFT JOIN invoice AS i
  ON c.customer_id = i.customer_id
GROUP BY
  c.customer_id,
  customer_name,
  c.country;

-- 作成したビューの確認クエリ
-- SELECT * FROM V_CustomerSummary ORDER BY total_sales DESC;
```


---

### スキルシート記載例

**件名：SQL（PostgreSQL）を用いた業務システム開発におけるデータ操作・分析スキル**

**■ 保有スキル概要**
1ヶ月間の実践的なSQL研修を通じ、業務システム開発（販売管理、顧客管理等）で必要とされるデータ抽出、集計、加工、分析のスキルを体系的に習得しました。特に、アプリケーションやバッチ処理で利用されるSQLの実装、およびデータ不整合の調査・特定に関する能力を重点的に強化しています。

**■ 詳細スキル**

*   **1. 基本的なデータ操作・抽出スキル**
    *   `JOIN` (INNER, LEFT) を用いた複数テーブルからのデータ取得（顧客情報と担当者、請求書と明細など）。
        *   （基本編: 問題1, 3, 7, 8, 10）
    *   `WHERE`句による柔軟な条件指定（期間指定、ID指定など）と、アプリケーションからのパラメータ利用を想定したSQL設計。
        *   （基本編: 問題1, 3, 8, 10）
    *   `GROUP BY` と集計関数 (`SUM`, `COUNT`, `AVG`, `MAX`) を用いた基本集計（商品別売上、顧客別最終購入日など）。
        *   （基本編: 問題2, 4, 7, 9）
    *   自己結合（Self-Join）による階層構造データ（従業員と上司など）の取り扱い。
        *   （基本編: 問題6）

*   **2. バッチ処理・ETLを想定したSQLスキル**
    *   `HAVING`句を用いた集計結果に基づくレコードの絞り込み（売上不整合データの特定、休眠顧客リストの作成など）。
        *   （基本編: 問題2, 4）
    *   複数カラムをキーとした`GROUP BY`による多角的なデータ集計（メディアタイプ別・ジャンル別集計など）。
        *   （基本編: 問題9）
    *   CTE（共通テーブル式）を利用し、複雑な集計ロジック（担当者別平均と全社平均の比較など）をステップに分割し、可読性・保守性の高いSQLを記述可能。
        *   （応用編: 問題1, 2, 5, 8）
    *   ウィンドウ関数 `NTILE()` を用いたデータセグメンテーション（顧客ランク付けバッチなど）。
        *   （応用編: 問題7）

*   **3. データ品質担保・障害調査に関するスキル**
    *   `LEFT JOIN ... IS NULL` や `NOT EXISTS` 句を用いたデータ不整合・孤立データの検出。
        *   **マスタ起因の不整合調査**（マスタにあるが実績がない、など）
            *   （基本編: 問題5 / 応用編: 問題3）
        *   **トランザクション起因の不整合調査**（実績にあるがマスタがない、など）
            *   （応用編: 問題6）
    *   `HAVING`句を用いた、請求ヘッダと明細の合計金額の不一致など、業務ロジックに基づいたデータ整合性チェック。
        *   （基本編: 問題2）

*   **4. データ分析・KPIモニタリングに関するスキル**
    *   ウィンドウ関数を用いた高度な分析SQLの実装。
        *   **ランキング** (`RANK`, `ROW_NUMBER`): グループ内でのトップN抽出（国別トップ顧客など）。
            *   （応用編: 問題1）
        *   **時系列分析** (`LAG`): 前期比較（月次売上の前月比成長率など）。
            *   （応用編: 問題2）
        *   **累積計算** (`SUM OVER`): 累計値の算出（顧客ごとの累積購入額推移など）。
            *   （応用編: 問題4）

*   **5. データベース設計・再利用性に関するスキル**
    *   頻繁に利用される集計ロジックをビュー（View）として定義し、SQLの再利用性とアプリケーション開発の効率性を向上させる設計思想を理解。
        *   （応用編: 問題10）

---

### このスキルシートのポイントとアピール戦略

*   **派遣先の業務内容に完全準拠:** 「バッチ処理」「障害調査」「帳票作成」「ETL」といった、派遣先で使われるであろうキーワードを意図的に盛り込み、即戦力となりうる人材であることを示唆しています。
*   **「なぜそのスキルが必要か」を明記:** 単に「JOINが使えます」ではなく、「顧客情報と担当者など、複数のテーブルからデータを取得できます」のように、業務上の具体的なシーンを併記することで、実務での活躍イメージを採用担当者に持たせます。
*   **スキルをカテゴリ分け:** 「基本操作」「バッチ処理」「障害調査」のようにスキルを分類することで、研修生が体系的にSQLを学んだこと、そして自分のスキルを客観的に整理できていることをアピールできます。
*   **問題番号との関連付け:**（これは講師・営業用の内部資料として）どのスキルがどの問題で証明できるかを明確にしておくことで、面接で「データ不整合の調査とは、具体的にどのようなことをやりましたか？」と深掘りされた際に、研修生が自信を持って「基本編の問題2で、請求ヘッダと明細の合計金額が一致しないデータを特定する演習を行いました」と具体的に答えられるようになります。

このスキルシートは、単なるスキルの羅列ではなく、「**業務システム開発の現場で起こりうる様々な課題に対して、SQLを使ってどのようにアプローチできるか**」を示した構成になっています。研修生がこれらの演習をしっかりこなせば、このスキルシートの内容を自信を持って語ることができるはずです。