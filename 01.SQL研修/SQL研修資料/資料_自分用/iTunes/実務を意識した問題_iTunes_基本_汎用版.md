### SQL複合問題 20選

#### 問題1：国別 顧客数レポート
**問題:**
顧客がどの国に分布しているかを把握するため、国別の顧客数を集計し、顧客数が多い順に表示してください。

**想定業務:**
グローバル展開している企業のマーケティング部門が、主要な市場や今後注力すべき国を判断するための基礎データとして利用します。国別の顧客分布を定期的にモニタリングします。

**ヒント:**
`customer`テーブルを国（`country`）でグループ化し、各国の顧客数を`COUNT`で集計します。

**解答例:**
```sql
SELECT
    country,
    COUNT(customer_id) AS customer_count
FROM
    customer
GROUP BY
    country
ORDER BY
    customer_count DESC;
```

---

#### 問題2：営業担当者別 売上実績ランキング
**問題:**
各営業担当者の売上実績を評価するため、担当者ごとの総売上高（担当顧客の請求合計額）を算出し、売上高が高い順にランキング形式で表示してください。

**想定業務:**
営業部門のマネージャーが、四半期ごとのパフォーマンスレビューやインセンティブ計算のために、各担当者の実績を正確に把握する際に使用します。

**ヒント:**
`employee`, `customer`, `invoice`の3つのテーブルを結合します。営業担当者（`employee`）でグループ化し、`invoice`テーブルの`total`を合計（`SUM`）します。

**解答例:**
```sql
SELECT
    e.first_name || ' ' || e.last_name AS employee_name,
    SUM(i.total) AS total_sales
FROM
    employee e
JOIN
    customer c ON e.employee_id = c.support_rep_id
JOIN
    invoice i ON c.customer_id = i.customer_id
GROUP BY
    e.employee_id, employee_name
ORDER BY
    total_sales DESC;
```

---

#### 問題3：上位10社の優良顧客リスト（購入金額順）
**問題:**
全顧客の中から、累計の請求総額が最も高い上位10名を「優良顧客」としてリストアップしてください。顧客の氏名と累計支払額を表示します。

**想定業務:**
CRM（顧客関係管理）の一環として、VIP顧客を特定し、特別なキャンペーンや手厚いサポートを提供するための対象者リストを作成します。

**ヒント:**
`customer`テーブルと`invoice`テーブルを結合し、顧客ごとに請求額を`SUM`で合計します。結果を降順に並べ替え、`LIMIT 10`で上位10件を抽出します。

**解答例:**
```sql
SELECT
    c.first_name || ' ' || c.last_name AS customer_name,
    SUM(i.total) AS lifetime_value
FROM
    customer c
JOIN
    invoice i ON c.customer_id = i.customer_id
GROUP BY
    c.customer_id, customer_name
ORDER BY
    lifetime_value DESC
LIMIT 10;
```

---

#### 問題4：2021年の月別売上推移レポート
**問題:**
2021年の業績を月ごとに把握するため、月別の総売上高を集計してください。結果は月の昇順で表示します。

**想定業務:**
経営会議や事業部会で、売上の季節変動やキャンペーン効果を分析するための月次レポートを作成します。

**ヒント:**
`invoice`テーブルの`invoice_date`から年で絞り込み、月ごとにグループ化します。PostgreSQLの`TO_CHAR`関数や`DATE_TRUNC`関数を使うと月を抽出しやすいです。

**解答例:**
```sql
SELECT
    TO_CHAR(invoice_date, 'YYYY-MM') AS sales_month,
    SUM(total) AS monthly_sales
FROM
    invoice
WHERE
    EXTRACT(YEAR FROM invoice_date) = 2021
GROUP BY
    sales_month
ORDER BY
    sales_month;
```

---

#### 問題5：平均請求額を上回る高額請求書の特定
**問題:**
全請求書の平均請求額を算出し、その平均額よりも金額が高い請求書をすべてリストアップしてください。

**想定業務:**
経理・監査部門が、通常よりも高額な取引を抽出し、内容の確認や不正取引の監視を行うためのデータとして利用します。

**ヒント:**
サブクエリを使用して、先に`invoice`テーブル全体の平均請求額（`AVG(total)`）を計算し、その結果を`WHERE`句の条件に使って比較します。

**解答例:**
```sql
SELECT
    invoice_id,
    customer_id,
    total
FROM
    invoice
WHERE
    total > (SELECT AVG(total) FROM invoice)
ORDER BY
    total DESC;
```

---

#### 問題6：購入履歴のない顧客の抽出
**問題:**
顧客として登録されているものの、まだ一度も購入に至っていない（請求データが存在しない）顧客のリストを作成してください。

**想定業務:**
見込み顧客や休眠顧客に対して、初回購入を促すクーポンやキャンペーン案内を送付するためのアプローチリストを作成します。

**ヒント:**
`customer`テーブルに存在するが`invoice`テーブルには存在しない顧客を探します。`LEFT JOIN`と`WHERE ... IS NULL`を組み合わせるか、`NOT IN`や`NOT EXISTS`句を使用します。

**解答例:**
```sql
SELECT
    c.first_name,
    c.last_name,
    c.email
FROM
    customer c
LEFT JOIN
    invoice i ON c.customer_id = i.customer_id
WHERE
    i.invoice_id IS NULL;
```

---

#### 問題7：従業員とその直属の上司リスト
**問題:**
社内の指揮命令系統を明確にするため、全従業員の氏名とその直属の上司の氏名を一覧で表示してください。最上位の管理者には上司はいないためNULLと表示します。

**想定業務:**
人事部門が組織図を作成したり、社内ポータルにレポートラインを掲載するためのデータを抽出します。

**ヒント:**
`employee`テーブルを自分自身に結合する「自己結合（self-join）」を使用します。従業員用のテーブル（例：e1）と上司用のテーブル（例：e2）に別名を付け、`e1.reports_to = e2.employee_id`で紐付けます。

**解答例:**
```sql
SELECT
    e1.first_name || ' ' || e1.last_name AS employee_name,
    e2.first_name || ' ' || e2.last_name AS manager_name
FROM
    employee e1
LEFT JOIN
    employee e2 ON e1.reports_to = e2.employee_id
ORDER BY
    manager_name, employee_name;
```

---

#### 問題8：各顧客の初回購入日と最終購入日
**問題:**
顧客の利用期間を分析するため、各顧客の氏名、初回購入日、最終購入日を一覧で表示してください。

**想定業務:**
顧客の定着度を測るための分析や、LTV（顧客生涯価値）を計算する際の基礎データとして利用します。長期間利用のない顧客を特定するのにも役立ちます。

**ヒント:**
`customer`テーブルと`invoice`テーブルを結合し、顧客IDでグループ化します。`MIN(invoice_date)`で初回購入日、`MAX(invoice_date)`で最終購入日を取得します。

**解答例:**
```sql
SELECT
    c.first_name || ' ' || c.last_name AS customer_name,
    MIN(i.invoice_date)::date AS first_purchase_date,
    MAX(i.invoice_date)::date AS last_purchase_date
FROM
    customer c
JOIN
    invoice i ON c.customer_id = i.customer_id
GROUP BY
    c.customer_id, customer_name
ORDER BY
    customer_name;
```

---

#### 問題9：アメリカ（USA）国内の州別売上レポート
**問題:**
主要市場であるアメリカ国内の販売状況をより詳細に分析するため、州（`state`）ごとの総売上を集計し、売上が高い州から順に表示してください。

**想定業務:**
カントリーマネージャーが、国内の特定地域（州）の販売戦略を立案・評価するために使用します。州ごとのマーケティング予算配分の参考にもなります。

**ヒント:**
`customer`と`invoice`テーブルを結合し、まず`country`で 'USA' に絞り込みます。その後、`state`でグループ化して売上を合計します。

**解答例:**
```sql
SELECT
    c.state,
    SUM(i.total) AS total_sales
FROM
    customer c
JOIN
    invoice i ON c.customer_id = i.customer_id
WHERE
    c.country = 'USA'
GROUP BY
    c.state
ORDER BY
    total_sales DESC;
```

---

#### 問題10：注文あたりの平均購入商品数
**問題:**
顧客が1回の注文（請求）で平均していくつの商品を購入しているかを算出してください。

**想定業務:**
ECサイトや小売店で、顧客の購買行動を分析する際の指標として利用します。「ついで買い」を促すクロスセル戦略の効果測定などに使われます。

**ヒント:**
`invoice_line`テーブルを`invoice_id`でグループ化し、各請求に含まれる明細の数（`COUNT`）を数えます。その結果をサブクエリ（またはCTE）とし、外側のクエリで平均（`AVG`）を計算します。

**解答例:**
```sql
SELECT
    AVG(item_count) AS avg_items_per_order
FROM (
    SELECT
        invoice_id,
        COUNT(invoice_line_id) AS item_count
    FROM
        invoice_line
    GROUP BY
        invoice_id
) AS order_items;
```

---

#### 問題11：マネージャーごとの部下人数
**問題:**
各マネージャーが何人の部下を管理しているかを把握するため、上司（`reports_to`にIDが指定されている従業員）とその部下の人数を一覧表示してください。

**想定業務:**
人事部門が組織構造を分析し、マネージャーの管理範囲（スパン・オブ・コントロール）が適切かどうかを評価するために使用します。

**ヒント:**
`employee`テーブルを使い、部下が報告する上司のID（`reports_to`）でグループ化します。そのグループ内の従業員数を`COUNT`することで、各上司の部下人数が分かります。上司の名前を表示するために自己結合します。

**解答例:**
```sql
SELECT
    manager.first_name || ' ' || manager.last_name AS manager_name,
    COUNT(subordinate.employee_id) AS number_of_reports
FROM
    employee AS manager
JOIN
    employee AS subordinate ON manager.employee_id = subordinate.reports_to
GROUP BY
    manager.employee_id, manager_name
ORDER BY
    number_of_reports DESC;
```

---

#### 問題12：2回以上購入しているリピート顧客リスト
**問題:**
顧客のロイヤルティを測るため、購入回数が2回以上あるリピート顧客の氏名と購入回数をリストアップしてください。

**想定業務:**
リピート顧客を対象とした特典プログラムや、優良顧客育成のための施策を企画する際のターゲットリストとして活用します。

**ヒント:**
`customer`と`invoice`テーブルを結合し、顧客ごとにグループ化します。`HAVING`句を使い、請求書の数（`COUNT(i.invoice_id)`）が2以上の顧客のみを抽出します。

**解答例:**
```sql
SELECT
    c.first_name || ' ' || c.last_name AS customer_name,
    COUNT(i.invoice_id) AS purchase_count
FROM
    customer c
JOIN
    invoice i ON c.customer_id = i.customer_id
GROUP BY
    c.customer_id, customer_name
HAVING
    COUNT(i.invoice_id) >= 2
ORDER BY
    purchase_count DESC;
```

---

#### 問題13：商品カテゴリ（`genre`）別 売上トップ3
**問題:**
どの商品カテゴリが売上に貢献しているかを把握するため、カテゴリ別の総売上高を算出し、上位3カテゴリを表示してください。

**想定業務:**
商品企画やマーケティング部門が、売れ筋カテゴリを特定し、在庫管理の最適化やプロモーション強化の意思決定を行うために使用します。

**ヒント:**
`genre`, `track`, `invoice_line`の3つのテーブルを結合します。`genre`名でグループ化し、売上（`unit_price * quantity`）を合計します。最後に`LIMIT 3`で上位3件に絞ります。

**解答例:**
```sql
SELECT
    g.name AS genre_name,
    SUM(il.unit_price * il.quantity) AS total_revenue
FROM
    genre g
JOIN
    track t ON g.genre_id = t.genre_id
JOIN
    invoice_line il ON t.track_id = il.track_id
GROUP BY
    g.name
ORDER BY
    total_revenue DESC
LIMIT 3;
```

---

#### 問題14：データ異常検知：商品マスタと請求単価の不一致
**問題:**
請求処理の正確性を確認するため、請求明細（`invoice_line`）に記録された単価と、元の商品マスタ（`track`）に登録されている単価が一致しないレコードをすべて抽出してください。

**想定業務:**
システム障害や不正なデータ操作の調査。セール価格の適用漏れや、マスタ更新前の古い価格で請求されていないかなどを確認するデータ整合性チェック。

**ヒント:**
`invoice_line`テーブルと`track`テーブルを`track_id`で結合し、`WHERE`句で`il.unit_price <> t.unit_price`の条件を指定します。

**解答例:**
```sql
SELECT
    il.invoice_line_id,
    il.invoice_id,
    t.track_id,
    t.name AS track_name,
    t.unit_price AS master_price,
    il.unit_price AS invoice_price
FROM
    invoice_line il
JOIN
    track t ON il.track_id = t.track_id
WHERE
    il.unit_price <> t.unit_price;
```

---

#### 問題15：各顧客のバスケット単価（平均注文額）
**問題:**
顧客一人ひとりの購買傾向を分析するため、各顧客の1回の請求あたりの平均購入額（バスケット単価）を計算してください。

**想定業務:**
顧客セグメンテーションの一環として、「一度にまとめて高額購入する顧客」や「少額を頻繁に購入する顧客」などを分類し、それぞれに合ったアプローチを検討します。

**ヒント:**
`customer`と`invoice`テーブルを結合し、顧客ごとにグループ化します。`AVG(i.total)`を計算することで、その顧客の平均注文額が算出できます。

**解答例:**
```sql
SELECT
    c.first_name || ' ' || c.last_name AS customer_name,
    AVG(i.total) AS average_invoice_total
FROM
    customer c
JOIN
    invoice i ON c.customer_id = i.customer_id
GROUP BY
    c.customer_id, customer_name
ORDER BY
    average_invoice_total DESC;
```

---

#### 問題16：担当顧客を持たない営業サポート担当者
**問題:**
現在、一人も顧客を担当していない営業サポート担当者（`title`が'Sales Support Agent'）がいないか確認してください。

**想定業務:**
営業部門のリソース管理。担当が割り振られていないメンバーを特定し、新規顧客の割り当てやチーム内の業務分担を見直すために使用します。

**ヒント:**
`employee`テーブルと`customer`テーブルを`support_rep_id`で`LEFT JOIN`します。`title`で営業担当者に絞り込んだ後、`customer_id`が`NULL`である従業員を抽出します。

**解答例:**
```sql
SELECT
    e.employee_id,
    e.first_name,
    e.last_name
FROM
    employee e
LEFT JOIN
    customer c ON e.employee_id = c.support_rep_id
WHERE
    e.title = 'Sales Support Agent' AND c.customer_id IS NULL;
```
---

#### 問題17：各国の売上No.1商品カテゴリ
**問題:**
グローバルな販売戦略を立てるため、国ごとに最も売れている商品カテゴリ（`genre`）を特定し、国名、カテゴリ名、その売上額を表示してください。

**想定業務:**
海外事業部が、各国の市場特性や嗜好を理解し、地域に最適化されたマーケティングや商品展開を行うためのデータ分析。

**ヒント:**
共通テーブル式（CTE）とウィンドウ関数`ROW_NUMBER()`を使うと効率的です。まずCTEで国・カテゴリ別の売上を計算し、ウィンドウ関数で国ごとに売上ランキングを付けます。最後にランキング1位のレコードのみを抽出します。

**解答例:**
```sql
WITH CountryGenreSales AS (
    SELECT
        c.country,
        g.name AS genre,
        SUM(il.unit_price * il.quantity) AS sales,
        ROW_NUMBER() OVER(PARTITION BY c.country ORDER BY SUM(il.unit_price * il.quantity) DESC) as rn
    FROM invoice_line il
    JOIN track t ON il.track_id = t.track_id
    JOIN genre g ON t.genre_id = g.genre_id
    JOIN invoice i ON il.invoice_id = i.invoice_id
    JOIN customer c ON i.customer_id = c.customer_id
    GROUP BY c.country, g.name
)
SELECT
    country,
    genre,
    sales
FROM CountryGenreSales
WHERE rn = 1
ORDER BY country;
```

---

#### 問題18：全請求の平均明細数より多くの商品を含む請求書
**問題:**
一度に多くの商品をまとめて購入する傾向のある注文を特定するため、1つの請求書に含まれる商品明細数が、全請求書の平均明細数よりも多い請求書をリストアップしてください。

**想定業務:**
物流部門が、梱包や発送作業が複雑になる大規模な注文を事前に把握したり、マーケティング部門が「まとめ買い」キャンペーンの効果を測定したりするのに利用します。

**ヒント:**
サブクエリを2段階で使います。内側のサブクエリで各請求書の明細数を数え、外側のサブクエリでその平均値を計算します。最後に、その平均値より明細数が多い請求書を`HAVING`句で絞り込みます。

**解答例:**
```sql
SELECT
    invoice_id,
    COUNT(invoice_line_id) AS number_of_items
FROM
    invoice_line
GROUP BY
    invoice_id
HAVING
    COUNT(invoice_line_id) > (
        SELECT AVG(item_count)
        FROM (
            SELECT COUNT(invoice_line_id) AS item_count
            FROM invoice_line
            GROUP BY invoice_id
        ) AS avg_calc
    )
ORDER BY
    number_of_items DESC;
```

---

#### 問題19：営業担当者とその上司の売上実績比較
**問題:**
（クエリの練習として）各営業担当者の総売上と、その担当者の直属の上司の総売上を並べて表示してください。※このDB構造では上司自身は直接売上を計上しないため、上司の売上は0になりますが、構造の理解を深める問題です。

**想定業務:**
より複雑なレポート作成の練習。例えば、マネージャーの実績を「自身の直接売上＋部下の総売上」と定義する場合など、階層的なデータ集計の基礎となります。

**ヒント:**
CTEを複数使うと見通しが良くなります。1つ目のCTEで従業員ごとの売上を計算し、2つ目のCTEで従業員と上司の関連付けを行います。最後にそれらを結合して表示します。

**解答例:**
```sql
WITH EmployeeSales AS (
    SELECT
        c.support_rep_id AS employee_id,
        SUM(i.total) AS total_sales
    FROM
        customer c
    JOIN
        invoice i ON c.customer_id = i.customer_id
    GROUP BY
        c.support_rep_id
),
EmployeeManager AS (
    SELECT
        employee_id,
        first_name,
        last_name,
        reports_to AS manager_id
    FROM
        employee
)
SELECT
    em.first_name || ' ' || em.last_name AS employee_name,
    COALESCE(es.total_sales, 0) AS employee_sales,
    m.first_name || ' ' || m.last_name AS manager_name,
    COALESCE(ms.total_sales, 0) AS manager_sales
FROM
    EmployeeManager em
LEFT JOIN
    EmployeeSales es ON em.employee_id = es.employee_id
LEFT JOIN
    employee m ON em.manager_id = m.employee_id
LEFT JOIN
    EmployeeSales ms ON em.manager_id = ms.employee_id
ORDER BY
    manager_name, employee_name;
```

---

#### 問題20：購入上位顧客が最も購入している商品カテゴリ
**問題:**
累計購入金額トップ5の顧客を特定し、それらの顧客が最も多くのお金を使っている商品カテゴリ（`genre`）を顧客ごとに表示してください。

**想定業務:**
最重要顧客（VIP）の購買嗜好を詳細に分析し、パーソナライズされた商品提案やマーケティング施策に活かすためのデータ抽出。

**ヒント:**
CTEを複数使います。最初のCTEで上位5名の顧客IDを特定します。次のCTEで、それらの顧客がどのカテゴリにいくら使ったかを計算し、ウィンドウ関数でランキング付けします。最後にランキング1位のカテゴリを抽出します。

**解答例:**
```sql
WITH TopCustomers AS (
    SELECT
        customer_id
    FROM
        invoice
    GROUP BY
        customer_id
    ORDER BY
        SUM(total) DESC
    LIMIT 5
),
CustomerGenreSpending AS (
    SELECT
        i.customer_id,
        g.name AS genre_name,
        SUM(il.unit_price * il.quantity) AS spending,
        ROW_NUMBER() OVER(PARTITION BY i.customer_id ORDER BY SUM(il.unit_price * il.quantity) DESC) as rn
    FROM
        invoice i
    JOIN
        invoice_line il ON i.invoice_id = il.invoice_id
    JOIN
        track t ON il.track_id = t.track_id
    JOIN
        genre g ON t.genre_id = g.genre_id
    WHERE
        i.customer_id IN (SELECT customer_id FROM TopCustomers)
    GROUP BY
        i.customer_id, g.name
)
SELECT
    c.first_name || ' ' || c.last_name AS customer_name,
    cgs.genre_name,
    cgs.spending
FROM
    CustomerGenreSpending cgs
JOIN
    customer c ON cgs.customer_id = c.customer_id
WHERE
    cgs.rn = 1
ORDER BY
    cgs.spending DESC;
```

---


### スキルシート記載例（10行）

ビジネス上の多様な要求に応えられるSQLスキルを、網羅的かつ具体的にアピールします。

*   **定型レポート作成:** `GROUP BY`と各種集計関数(`SUM`, `COUNT`, `AVG`, `MIN`, `MAX`)を駆使し、国別顧客数や月次売上レポートなどの定型データ作成が可能。
*   **多角的なデータ分析:** 複数テーブルを`JOIN`し、営業担当者別売上や顧客別平均単価など、パフォーマンス評価や顧客分析に必要なデータを抽出・集計する能力。
*   **ランキング・上位抽出:** `ORDER BY`と`LIMIT`句を組み合わせ、売上上位顧客や人気商品カテゴリのランキングデータを迅速に作成可能。
*   **時系列データ分析:** 日付/時刻関数(`TO_CHAR`, `EXTRACT`等)を活用し、月別売上推移や顧客の初回・最終購入日といった時系列でのデータ分析経験。
*   **サブクエリ（副問い合わせ）:** `WHERE`句や`FROM`句でのサブクエリを用い、平均値との比較や段階的な集計処理を実装できる。
*   **外部結合と自己結合:** `LEFT JOIN`によるデータ有無の判定（例：購入履歴のない顧客）や、自己結合による階層構造データ（例：従業員と上司）の抽出。
*   **集計後データでの条件抽出:** `HAVING`句を利用し、「購入回数2回以上」といった集計結果に基づくセグメント抽出が可能。
*   **高度な分析クエリ:** CTE（共通テーブル式）とウィンドウ関数(`ROW_NUMBER`)を組み合わせ、国別トップカテゴリの特定など、複雑な条件でのランキングやデータ抽出処理の実装経験。
*   **データ品質と整合性担保:** 複数テーブル間のデータを比較し、マスタと実績の価格不一致といったデータ異常を検知するクエリの作成能力。
*   **ビジネス要件のSQLへの翻訳力:** 「リピート顧客を特定したい」「優良顧客の嗜好を知りたい」といったビジネス上の曖昧な要求を、具体的なSQLクエリに落とし込むことができる。

---

### 面接でのアピールポイント

これらの課題から得たスキルを、単なる技術力ではなく「ビジネス課題を解決する能力」としてアピールすることが鍵となります。

#### 1. 「数字の裏側」を読む、分析視点をアピールする

クエリを書けるだけでなく、その結果から何が言えるかを考察する姿勢を見せることが重要です。

*   **アピール例（問題9：アメリカ国内の州別売上レポート を元に）:**
    「こちらの課題では、単純に州別の売上を集計するだけでなく、なぜカリフォルニア州の売上が突出しているのか、逆に売上が低い州はどこで、その背景には何が考えられるか、といったビジネス的な視点を持つことの重要性を学びました。自分が抽出したデータが、次のマーケティング戦略の意思決定にどう繋がるかを意識して業務に取り組みたいと考えています。」

#### 2. データ品質に対する意識の高さを示す

多くのエンジニアがデータ抽出・分析スキルをアピールする中で、「データの正確性」に言及できると、一歩抜きん出た存在として評価されます。

*   **アピール例（問題14：データ異常検知 を元に）:**
    「研修では、売上データを分析するだけでなく、請求明細と商品マスタの価格に不一致がないかを確認する、データ品質チェックのクエリも作成しました。正確なデータ分析は、信頼できるデータの上に成り立つと考えています。システム間のデータ連携ミスや不正操作の可能性を常に念頭に置き、データの整合性を担保する意識を持って業務に貢献したいです。」

#### 3. 複雑な課題を分解して解決する論理的思考力をアピール

CTEやウィンドウ関数を使った問題は、論理的思考力を示す絶好の機会です。

*   **アピール例（問題20：購入上位顧客が最も購入している商品カテゴリ を元に）:**
    「『購入上位顧客が最も購入しているカテゴリ』を特定する課題は、一度のクエリで解くのが困難でした。そこで、まずCTE（共通テーブル式）を使って『上位5名の顧客』という中間的な結果セットを定義し、次にその顧客たちのカテゴリ別購入額を計算してランキング付けする、というように問題を2段階に分解して解決しました。このように、複雑な要求に対しても、処理を分解し、段階的に組み立てていくことで対応できます。」

#### 4. 多様なJOINを使いこなせる柔軟性

`INNER JOIN`だけでなく、`LEFT JOIN`や自己結合をどのような目的で使うかを明確に説明できると、テーブル設計への理解度もアピールできます。

*   **アピール例（問題7：従業員とその直属の上司リスト を元に）:**
    「従業員と上司のリストを作成する課題では、`employee`テーブルを自分自身に結合する『自己結合』というテクニックを使いました。また、上司がいない最上位の管理職も漏れなく表示するために`LEFT JOIN`を選択しました。このように、テーブル間の関連性やデータの特性を理解し、目的に応じて最適なJOINを選択することができます。」

これらの課題をやり遂げた研修生は、SQLの基本が定着していることはもちろん、ビジネスの現場で求められる多様なデータ抽出・分析要求に応える素養が十分にあると言えます。自信を持って面接に臨めるよう、力強く後押ししてあげてください。