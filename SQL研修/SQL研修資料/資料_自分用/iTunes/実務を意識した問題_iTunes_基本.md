
### 問題一覧

#### 問題1：アーティスト別アルバム数集計
**問題:**
各アーティストがそれぞれ何枚のアルバムをリリースしているかを集計し、アルバム数が多い順にアーティスト名とアルバム数を表示してください。

**想定業務:**
コンテンツ管理システムにおける、アーティストごとの作品数実績を把握するためのデータ抽出。定期的なレポート作成や、アーティストとの契約更新時の参考資料として利用します。

**ヒント:**
`artist`テーブルと`album`テーブルを`artist_id`で結合します。アーティスト名でグループ化し、アルバム数をカウント（`COUNT`）したのち、結果を降順（`DESC`）で並べ替えます。

**解答例:**
```sql
SELECT
    ar.name,
    COUNT(al.album_id) AS album_count
FROM
    artist ar
JOIN
    album al ON ar.artist_id = al.artist_id
GROUP BY
    ar.name
ORDER BY
    album_count DESC;
```

---

#### 問題2：営業担当者別 顧客担当数ランキング
**問題:**
営業担当者（`employee`テーブルで`title`が'Sales Support Agent'の従業員）ごとに、担当している顧客（`customer`）の数を集計し、担当顧客数が多い順に表示してください。

**想定業務:**
営業部門のマネージャーが、各担当者の業務負荷を把握・調整するために使用します。担当顧客数の平準化や、パフォーマンス評価の一環として利用されるデータです。

**ヒント:**
`employee`テーブルと`customer`テーブルを`support_rep_id`で結合します。従業員の役職（`title`）で絞り込み、従業員ごとにグループ化して顧客数をカウントします。

**解答例:**
```sql
SELECT
    e.first_name || ' ' || e.last_name AS employee_name,
    COUNT(c.customer_id) AS customer_count
FROM
    employee e
JOIN
    customer c ON e.employee_id = c.support_rep_id
WHERE
    e.title = 'Sales Support Agent'
GROUP BY
    e.employee_id, employee_name
ORDER BY
    customer_count DESC;
```

---

#### 問題3：優良顧客の特定
**問題:**
請求合計額（`invoice`テーブルの`total`）が$40以上の顧客を「優良顧客」とし、その顧客の氏名と合計支払額をリストアップしてください。

**想定業務:**
マーケティング部門が、優良顧客（VIP顧客）を対象としたキャンペーンや特典案内のためにリストを抽出する際に使用します。

**ヒント:**
`customer`テーブルと`invoice`テーブルを結合し、顧客ごとに請求合計額を`SUM`関数で集計します。集計結果に対して`HAVING`句を使い、合計額が$40以上の顧客のみを抽出します。

**解答例:**
```sql
SELECT
    c.first_name || ' ' || c.last_name AS customer_name,
    SUM(i.total) AS total_spent
FROM
    customer c
JOIN
    invoice i ON c.customer_id = i.customer_id
GROUP BY
    c.customer_id, customer_name
HAVING
    SUM(i.total) >= 40
ORDER BY
    total_spent DESC;
```
---

#### 問題4：国別売上レポート
**問題:**
国（`customer`テーブルの`country`）別の総売上高を集計し、売上が高い国から順に表示してください。

**想定業務:**
経営企画や海外事業部が、グローバルな販売戦略を立てるために、国別の市場規模や売上貢献度を把握するレポートを作成します。

**ヒント:**
`customer`テーブルと`invoice`テーブルを結合し、国（`country`）でグループ化して、各国の請求合計額を`SUM`で計算します。

**解答例:**
```sql
SELECT
    c.country,
    SUM(i.total) AS total_sales
FROM
    customer c
JOIN
    invoice i ON c.customer_id = i.customer_id
GROUP BY
    c.country
ORDER BY
    total_sales DESC;
```

---

#### 問題5：「Rock」ジャンルを購入した顧客リスト
**問題:**
ジャンルが「Rock」のトラックを一度でも購入したことがある顧客の氏名をリストアップしてください。重複は表示しないようにしてください。

**想定業務:**
特定の音楽ジャンルに関心のある顧客層をターゲットとした、新譜の案内や関連アーティストのコンサート情報をメールで配信するためのリスト作成。

**ヒント:**
サブクエリを使用します。「Rock」ジャンルの`track_id`をサブクエリで取得し、その`track_id`が`invoice_line`テーブルに含まれる`invoice_id`を特定。最後に、その`invoice_id`を持つ顧客を`customer`テーブルから`DISTINCT`で抽出します。

**解答例:**
```sql
SELECT DISTINCT
    c.first_name,
    c.last_name
FROM
    customer c
JOIN
    invoice i ON c.customer_id = i.customer_id
JOIN
    invoice_line il ON i.invoice_id = il.invoice_id
WHERE
    il.track_id IN (
        SELECT t.track_id
        FROM track t
        JOIN genre g ON t.genre_id = g.genre_id
        WHERE g.name = 'Rock'
    );
```
---

#### 問題6：平均請求額を超える請求書の抽出
**問題:**
すべての請求書（`invoice`）の平均請求額を算出し、その平均額よりも高い請求額の請求書IDと金額を表示してください。

**想定業務:**
経理部門が高額取引の傾向を分析したり、不正な取引がないかを確認するためのデータ抽出。

**ヒント:**
サブクエリを使って`invoice`テーブル全体の平均`total`を計算し、その結果を`WHERE`句の条件として使用します。

**解答例:**
```sql
SELECT
    invoice_id,
    total
FROM
    invoice
WHERE
    total > (SELECT AVG(total) FROM invoice)
ORDER BY
    total DESC;
```

---

#### 問題7：各ジャンルの総販売数量
**問題:**
ジャンルごとに、販売されたトラックの総数量（`quantity`）を集計し、販売数量が多い順にジャンル名と総数量を表示してください。

**想定業務:**
商品企画部門が、どの音楽ジャンルが人気があるかを販売数量ベースで分析し、今後の仕入れやプロモーションの参考にします。

**ヒント:**
`genre`, `track`, `invoice_line`の3つのテーブルを結合します。ジャンル名でグループ化し、`invoice_line`の`quantity`を合計します。

**解答例:**
```sql
SELECT
    g.name AS genre_name,
    SUM(il.quantity) AS total_quantity_sold
FROM
    genre g
JOIN
    track t ON g.genre_id = t.genre_id
JOIN
    invoice_line il ON t.track_id = il.track_id
GROUP BY
    g.name
ORDER BY
    total_quantity_sold DESC;
```

---

#### 問題8：購入履歴のない顧客
**問題:**
一度も商品を購入したことがない（`invoice`テーブルにレコードが存在しない）顧客のリストを表示してください。

**想定業務:**
休眠顧客や未購入顧客を対象とした、初回購入キャンペーンや利用促進のためのアプローチリストを作成します。

**ヒント:**
`customer`テーブルを主軸に`invoice`テーブルを`LEFT JOIN`し、`invoice_id`が`NULL`である顧客を抽出します。または、`NOT IN`や`NOT EXISTS`でサブクエリを使用する方法もあります。

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

#### 問題9：従業員とその上司の表示
**問題:**
すべての従業員について、その従業員の氏名と、直属の上司の氏名を表示してください。最上位の管理者には上司がいないため、上司名はNULLで表示します。

**想定業務:**
人事システムや社内ポータルで、組織図やレポートライン（指揮命令系統）を確認するためのデータを抽出します。

**ヒント:**
`employee`テーブルを自己結合（self-join）します。従業員テーブル（`e1`）と上司テーブル（`e2`）として別名を付け、`e1.reports_to`と`e2.employee_id`をキーにして`LEFT JOIN`します。

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
    manager_name;
```

---

#### 問題10：月別売上推移
**問題:**
2021年の月別総売上高を計算し、月順に表示してください。

**想定業務:**
月次の経営会議で報告するための、売上推移データを作成します。季節性やキャンペーン効果の分析にも利用されます。

**ヒント:**
`invoice`テーブルの`invoice_date`から年と月を抽出し、それでグループ化します。PostgreSQLでは`DATE_TRUNC`関数や`TO_CHAR`関数が便利です。

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

#### 問題11：最も多くのトラックが含まれるアルバム
**問題:**
最も多くのトラック（曲）が含まれているアルバムのタイトルと、そのトラック数を表示してください。

**想定業務:**
コンテンツ分析の一環として、ボリュームの大きいアルバム（例えば、ベスト盤やライブ盤など）を特定し、特集企画などに利用します。

**ヒント:**
`album`テーブルと`track`テーブルを結合し、アルバムごとにトラック数をカウントします。その結果をトラック数の降順に並べ替え、`LIMIT 1`で先頭の1件を取得します。

**解答例:**
```sql
SELECT
    a.title AS album_title,
    COUNT(t.track_id) AS number_of_tracks
FROM
    album a
JOIN
    track t ON a.album_id = t.album_id
GROUP BY
    a.album_id, a.title
ORDER BY
    number_of_tracks DESC
LIMIT 1;
```

---

#### 問題12：複数ジャンルにまたがるアルバム
**問題:**
1つのアルバムに2つ以上の異なるジャンルのトラックが含まれているアルバムのタイトルをリストアップしてください。

**想定業務:**
データ品質管理の一環として、アルバムとトラック、ジャンルの関連データに不整合がないかを確認する。または、多様なジャンルを含むコンピレーションアルバムを特定する。

**ヒント:**
`album`, `track`, `genre`の3つのテーブルを結合します。アルバムタイトルでグループ化し、`HAVING`句でアルバム内のユニークなジャンル数（`COUNT(DISTINCT g.genre_id)`）が1より大きいものを抽出します。

**解答例:**
```sql
SELECT
    a.title
FROM
    album a
JOIN
    track t ON a.album_id = t.album_id
JOIN
    genre g ON t.genre_id = g.genre_id
GROUP BY
    a.title
HAVING
    COUNT(DISTINCT g.genre_id) > 1;
```

---

#### 問題13：各顧客の最終購入日
**問題:**
各顧客について、顧客の氏名と、その顧客の最終購入日（`invoice_date`）を表示してください。

**想定業務:**
RFM分析（Recency）の元データとして利用。最終購入日からの経過期間を計算し、長期間購入のない休眠顧客を特定するのに役立ちます。

**ヒント:**
`customer`テーブルと`invoice`テーブルを結合し、顧客ごとにグループ化して`MAX(invoice_date)`で最新の日付を取得します。

**解答例:**
```sql
SELECT
    c.first_name || ' ' || c.last_name AS customer_name,
    MAX(i.invoice_date) AS last_purchase_date
FROM
    customer c
JOIN
    invoice i ON c.customer_id = i.customer_id
GROUP BY
    c.customer_id, customer_name
ORDER BY
    last_purchase_date DESC;
```
---

#### 問題14：売上トップ5のトラック
**問題:**
総売上額（`unit_price` * `quantity`）が最も高いトラックのトップ5を、トラック名と総売上額と共に表示してください。

**想定業務:**
最も収益性の高い人気商品を特定し、広告やプロモーションで重点的に取り上げるためのデータ分析。

**ヒント:**
`track`テーブルと`invoice_line`テーブルを結合し、トラックごとに売上を`SUM(il.unit_price * il.quantity)`で計算します。結果を売上額の降順に並べ替え、`LIMIT 5`で上位5件を抽出します。

**解答例:**
```sql
SELECT
    t.name AS track_name,
    SUM(il.unit_price * il.quantity) AS total_revenue
FROM
    track t
JOIN
    invoice_line il ON t.track_id = il.track_id
GROUP BY
    t.track_id, t.name
ORDER BY
    total_revenue DESC
LIMIT 5;
```
---

#### 問題15：ブラジル人顧客の担当営業
**問題:**
ブラジル（Brazil）在住の顧客を担当している営業担当者の氏名を、重複なくリストアップしてください。

**想定業務:**
特定の国（この場合はブラジル）の市場を担当している営業チームのメンバーを特定し、地域ミーティングの招集やレポート提出依頼などに利用します。

**ヒント:**
`customer`と`employee`テーブルを結合し、`customer`の`country`が'Brazil'であるレコードに絞り込みます。`DISTINCT`を使って営業担当者の氏名が一意になるようにします。

**解答例:**
```sql
SELECT DISTINCT
    e.first_name,
    e.last_name
FROM
    employee e
JOIN
    customer c ON e.employee_id = c.support_rep_id
WHERE
    c.country = 'Brazil';
```
---

#### 問題16：メディアタイプごとのトラック数と平均再生時間
**問題:**
メディアタイプ（`media_type`）ごとに、登録されているトラック数と、そのトラックの平均再生時間（ミリ秒）を計算してください。

**想定業務:**
保有しているデジタルコンテンツの形式ごとの内訳や特性を分析します。例えば、動画ファイルと音声ファイルの数や平均的な長さを把握するのに使います。

**ヒント:**
`track`テーブルと`media_type`テーブルを結合し、メディアタイプ名でグループ化します。`COUNT`でトラック数を、`AVG`で平均再生時間を計算します。

**解答例:**
```sql
SELECT
    mt.name AS media_type_name,
    COUNT(t.track_id) AS number_of_tracks,
    AVG(t.milliseconds) AS avg_duration_ms
FROM
    media_type mt
JOIN
    track t ON mt.media_type_id = t.media_type_id
GROUP BY
    mt.name
ORDER BY
    number_of_tracks DESC;
```
---

#### 問題17：全アーティストのアルバムを購入している顧客
**問題:**
（このデータセットでは該当者がいない可能性が高いですが、クエリの練習として）すべてのアーティストのアルバムを最低1枚は購入している顧客がいるか調べ、もし存在すればその顧客の氏名を表示してください。

**想定業務:**
非常に熱心なファンやコレクターを特定するための高度なデータ抽出。ロイヤリティプログラムの最上位顧客の候補を探すといった特殊なケースで利用します。

**ヒント:**
これは少し複雑です。顧客ごとに購入したユニークなアーティストの数を数え、それが`artist`テーブルの全アーティスト数と一致するかを`HAVING`句でチェックします。

**解答例:**
```sql
SELECT
    c.first_name,
    c.last_name
FROM
    customer c
JOIN invoice i ON c.customer_id = i.customer_id
JOIN invoice_line il ON i.invoice_id = il.invoice_id
JOIN track t ON il.track_id = t.track_id
JOIN album al ON t.album_id = al.album_id
GROUP BY
    c.customer_id
HAVING
    COUNT(DISTINCT al.artist_id) = (SELECT COUNT(*) FROM artist);
```
---

#### 問題18：AC/DCの曲が含まれるプレイリスト
**問題:**
アーティスト「AC/DC」のトラックが1曲以上含まれているプレイリストの名前をすべて表示してください。

**想定業務:**
コンテンツの利用状況を分析する一環として、特定の人気アーティストの楽曲が、ユーザー作成のプレイリストにどの程度含まれているかを調査します。

**ヒント:**
`playlist`, `playlist_track`, `track`, `album`, `artist`の5つのテーブルを結合する必要があります。`artist`名で絞り込み、`DISTINCT`でプレイリスト名が重複しないようにします。

**解答例:**
```sql
SELECT DISTINCT
    p.name AS playlist_name
FROM
    playlist p
JOIN
    playlist_track pt ON p.playlist_id = pt.playlist_id
JOIN
    track t ON pt.track_id = t.track_id
JOIN
    album al ON t.album_id = al.album_id
JOIN
    artist ar ON al.artist_id = ar.artist_id
WHERE
    ar.name = 'AC/DC';
```
---

#### 問題19：各国のトップセールスジャンル
**問題:**
国ごとに、最も売上高が高かったジャンルを特定し、国名、ジャンル名、その売上額を表示してください。

**想定業務:**
国別の音楽市場の嗜好を分析し、地域ごとのマーケティング戦略を最適化するためのレポート作成。

**ヒント:**
共通テーブル式（CTE）やサブクエリを使います。まず国別・ジャンル別の売上を計算し、その結果からウィンドウ関数（`ROW_NUMBER()`や`RANK()`）を使って国ごとに売上1位のジャンルを抽出します。

**解答例:**
```sql
WITH CountryGenreSales AS (
    SELECT
        cu.country,
        g.name AS genre_name,
        SUM(il.unit_price * il.quantity) AS total_sales,
        ROW_NUMBER() OVER(PARTITION BY cu.country ORDER BY SUM(il.unit_price * il.quantity) DESC) as rn
    FROM
        customer cu
    JOIN invoice i ON cu.customer_id = i.customer_id
    JOIN invoice_line il ON i.invoice_id = il.invoice_id
    JOIN track t ON il.track_id = t.track_id
    JOIN genre g ON t.genre_id = g.genre_id
    GROUP BY
        cu.country, g.name
)
SELECT
    country,
    genre_name,
    total_sales
FROM
    CountryGenreSales
WHERE
    rn = 1
ORDER BY
    country;
```

---

#### 問題20：営業担当者とその担当顧客の総売上
**問題:**
各営業担当者について、その担当者がサポートする全顧客からの総売上高を計算し、担当者名と総売上高を表示してください。

**想定業務:**
営業担当者ごとの売上実績を評価するためのパフォーマンスレポート。インセンティブ計算の基礎データとしても利用されます。

**ヒント:**
`employee`, `customer`, `invoice`の3つのテーブルを結合します。営業担当者でグループ化し、その担当顧客の請求合計額を`SUM`で集計します。

**解答例:**
```sql
SELECT
    e.first_name || ' ' || e.last_name AS sales_rep_name,
    SUM(i.total) AS total_sales_generated
FROM
    employee e
JOIN
    customer c ON e.employee_id = c.support_rep_id
JOIN
    invoice i ON c.customer_id = i.customer_id
GROUP BY
    e.employee_id, sales_rep_name
ORDER BY
    total_sales_generated DESC;
```


使用テーブル

```SQL
/*******************************************************************************
   Create Tables
********************************************************************************/
CREATE TABLE album
(
    album_id SERIAL NOT NULL,
    title VARCHAR(160) NOT NULL,
    artist_id INT NOT NULL,
    CONSTRAINT album_pkey PRIMARY KEY  (album_id)
);

CREATE TABLE artist
(
    artist_id SERIAL NOT NULL,
    name VARCHAR(120),
    CONSTRAINT artist_pkey PRIMARY KEY  (artist_id)
);

CREATE TABLE customer
(
    customer_id SERIAL NOT NULL,
    first_name VARCHAR(40) NOT NULL,
    last_name VARCHAR(20) NOT NULL,
    company VARCHAR(80),
    address VARCHAR(70),
    city VARCHAR(40),
    state VARCHAR(40),
    country VARCHAR(40),
    postal_code VARCHAR(10),
    phone VARCHAR(24),
    fax VARCHAR(24),
    email VARCHAR(60) NOT NULL,
    support_rep_id INT,
    CONSTRAINT customer_pkey PRIMARY KEY  (customer_id)
);

CREATE TABLE employee
(
    employee_id SERIAL NOT NULL,
    last_name VARCHAR(20) NOT NULL,
    first_name VARCHAR(20) NOT NULL,
    title VARCHAR(30),
    reports_to INT,
    birth_date TIMESTAMP,
    hire_date TIMESTAMP,
    address VARCHAR(70),
    city VARCHAR(40),
    state VARCHAR(40),
    country VARCHAR(40),
    postal_code VARCHAR(10),
    phone VARCHAR(24),
    fax VARCHAR(24),
    email VARCHAR(60),
    CONSTRAINT employee_pkey PRIMARY KEY  (employee_id)
);

CREATE TABLE genre
(
    genre_id SERIAL NOT NULL,
    name VARCHAR(120),
    CONSTRAINT genre_pkey PRIMARY KEY  (genre_id)
);

CREATE TABLE invoice
(
    invoice_id SERIAL NOT NULL,
    customer_id INT NOT NULL,
    invoice_date TIMESTAMP NOT NULL,
    billing_address VARCHAR(70),
    billing_city VARCHAR(40),
    billing_state VARCHAR(40),
    billing_country VARCHAR(40),
    billing_postal_code VARCHAR(10),
    total NUMERIC(10,2) NOT NULL,
    CONSTRAINT invoice_pkey PRIMARY KEY  (invoice_id)
);

CREATE TABLE invoice_line
(
    invoice_line_id SERIAL NOT NULL,
    invoice_id INT NOT NULL,
    track_id INT NOT NULL,
    unit_price NUMERIC(10,2) NOT NULL,
    quantity INT NOT NULL,
    CONSTRAINT invoice_line_pkey PRIMARY KEY  (invoice_line_id)
);

CREATE TABLE media_type
(
    media_type_id SERIAL NOT NULL,
    name VARCHAR(120),
    CONSTRAINT media_type_pkey PRIMARY KEY  (media_type_id)
);

CREATE TABLE playlist
(
    playlist_id SERIAL NOT NULL,
    name VARCHAR(120),
    CONSTRAINT playlist_pkey PRIMARY KEY  (playlist_id)
);

CREATE TABLE playlist_track
(
    playlist_id INT NOT NULL,
    track_id INT NOT NULL,
    CONSTRAINT playlist_track_pkey PRIMARY KEY  (playlist_id, track_id)
);

CREATE TABLE track
(
    track_id SERIAL NOT NULL,
    name VARCHAR(200) NOT NULL,
    album_id INT,
    media_type_id INT NOT NULL,
    genre_id INT,
    composer VARCHAR(220),
    milliseconds INT NOT NULL,
    bytes INT,
    unit_price NUMERIC(10,2) NOT NULL,
    CONSTRAINT track_pkey PRIMARY KEY  (track_id)
);



/*******************************************************************************
   Create Primary Key Unique Indexes
********************************************************************************/

/*******************************************************************************
   Create Foreign Keys
********************************************************************************/
ALTER TABLE album ADD CONSTRAINT album_artist_id_fkey
    FOREIGN KEY (artist_id) REFERENCES artist (artist_id) ON DELETE NO ACTION ON UPDATE NO ACTION;

CREATE INDEX album_artist_id_idx ON album (artist_id);

ALTER TABLE customer ADD CONSTRAINT customer_support_rep_id_fkey
    FOREIGN KEY (support_rep_id) REFERENCES employee (employee_id) ON DELETE NO ACTION ON UPDATE NO ACTION;

CREATE INDEX customer_support_rep_id_idx ON customer (support_rep_id);

ALTER TABLE employee ADD CONSTRAINT employee_reports_to_fkey
    FOREIGN KEY (reports_to) REFERENCES employee (employee_id) ON DELETE NO ACTION ON UPDATE NO ACTION;

CREATE INDEX employee_reports_to_idx ON employee (reports_to);

ALTER TABLE invoice ADD CONSTRAINT invoice_customer_id_fkey
    FOREIGN KEY (customer_id) REFERENCES customer (customer_id) ON DELETE NO ACTION ON UPDATE NO ACTION;

CREATE INDEX invoice_customer_id_idx ON invoice (customer_id);

ALTER TABLE invoice_line ADD CONSTRAINT invoice_line_invoice_id_fkey
    FOREIGN KEY (invoice_id) REFERENCES invoice (invoice_id) ON DELETE NO ACTION ON UPDATE NO ACTION;

CREATE INDEX invoice_line_invoice_id_idx ON invoice_line (invoice_id);

ALTER TABLE invoice_line ADD CONSTRAINT invoice_line_track_id_fkey
    FOREIGN KEY (track_id) REFERENCES track (track_id) ON DELETE NO ACTION ON UPDATE NO ACTION;

CREATE INDEX invoice_line_track_id_idx ON invoice_line (track_id);

ALTER TABLE playlist_track ADD CONSTRAINT playlist_track_playlist_id_fkey
    FOREIGN KEY (playlist_id) REFERENCES playlist (playlist_id) ON DELETE NO ACTION ON UPDATE NO ACTION;

CREATE INDEX playlist_track_playlist_id_idx ON playlist_track (playlist_id);

ALTER TABLE playlist_track ADD CONSTRAINT playlist_track_track_id_fkey
    FOREIGN KEY (track_id) REFERENCES track (track_id) ON DELETE NO ACTION ON UPDATE NO ACTION;

CREATE INDEX playlist_track_track_id_idx ON playlist_track (track_id);

ALTER TABLE track ADD CONSTRAINT track_album_id_fkey
    FOREIGN KEY (album_id) REFERENCES album (album_id) ON DELETE NO ACTION ON UPDATE NO ACTION;

CREATE INDEX track_album_id_idx ON track (album_id);

ALTER TABLE track ADD CONSTRAINT track_genre_id_fkey
    FOREIGN KEY (genre_id) REFERENCES genre (genre_id) ON DELETE NO ACTION ON UPDATE NO ACTION;

CREATE INDEX track_genre_id_idx ON track (genre_id);

ALTER TABLE track ADD CONSTRAINT track_media_type_id_fkey
    FOREIGN KEY (media_type_id) REFERENCES media_type (media_type_id) ON DELETE NO ACTION ON UPDATE NO ACTION;

CREATE INDEX track_media_type_id_idx ON track (media_type_id);
```

---

## 研修生向け：SQLスキルシートと面接アピールポイント

### スキルシート記載例（10行）

SQLのスキルを具体的かつ簡潔に示すことで、技術的な能力を効果的にアピールできます。

*   **基本的なデータ抽出:** `SELECT`、`WHERE`、`ORDER BY` を用いた基本的なデータ抽出、および`DISTINCT`による重複排除が可能。
*   **データ集計・分析:** `GROUP BY`句と集計関数(`COUNT`, `SUM`, `AVG`, `MAX`)を組み合わせた、レポート作成やデータ分析経験。
*   **複数テーブルの結合:** `INNER JOIN`、`LEFT JOIN`を用いた複数テーブルからのデータ抽出、および自己結合（Self Join）による階層構造データの取り扱い。
*   **絞り込みと条件分岐:** `HAVING`句による集計結果の絞り込みや、`WHERE`句での複雑な条件設定。
*   **サブクエリ:** `WHERE`句や`SELECT`句でのサブクエリ（副問い合わせ）を利用した、段階的なデータ抽出と分析。
*   **応用的なSQL関数:** 日付関数(`TO_CHAR`, `EXTRACT`)や文字列結合を用いたデータ加工、および`LIMIT`による結果件数の制御。
*   **データモデリングの理解:** テーブル間のリレーションシップ（主キー、外部キー）を理解し、クエリを作成する能力。
*   **複雑なデータ抽出:** CTE（共通テーブル式）およびウィンドウ関数(`ROW_NUMBER`)を用いた、ランキングやグループ内での順位付け処理の実装。
*   **データ整合性の確認:** `LEFT JOIN`と`IS NULL`の組み合わせによる、関連データが存在しないレコードの特定。
*   **ビジネス要件の理解:** 営業実績、顧客分析、売上レポートなど、ビジネス上の要求に基づいたSQLクエリの作成能力。

---

### 面接でのアピールポイント

スキルシートの内容を裏付けるため、面接では「なぜそのクエリが必要だったのか」「どのように考えたのか」を具体的に話すことが重要です。

#### 1. 「なぜ」を語り、ビジネスへの貢献意欲を示す

単に「SQLが書けます」と答えるのではなく、**「どのようなビジネス課題を解決するために、そのクエリを作成したか」** を具体的に説明することが効果的です。

*   **アピール例（問題3：優良顧客の特定 を元に）:**
    「研修課題では、売上データから優良顧客を特定するSQLを作成しました。これは、マーケティング部門がVIP顧客向けのキャンペーンを企画する、といった実務を想定したものです。単にテーブルを結合して合計額を出すだけでなく、`HAVING`句を用いて『合計請求額が$40以上』という条件で絞り込むことで、ビジネスの目的に沿ったデータ抽出ができることを学びました。」

#### 2. 複数のアプローチを理解していることをアピールする

一つの課題に対して、複数の解決策を知っていることは、知識の深さを示す良いアピールになります。

*   **アピール例（問題8：購入履歴のない顧客 を元に）:**
    「購入履歴のない顧客をリストアップする課題では、`LEFT JOIN`と`IS NULL`を組み合わせて解決しました。この方法の他に、`NOT IN`や`NOT EXISTS`といったサブクエリを使うアプローチもあると理解しており、データの量やシステムの特性によって最適なクエリを選択する必要があることを学びました。」

#### 3. 応用的なスキルを具体例で説明する

特にCTEやウィンドウ関数を使った課題は、他の候補者との差別化を図る絶好の機会です。

*   **アピール例（問題19：各国のトップセールスジャンル を元に）:**
    「国ごとに最も売れている音楽ジャンルを特定するという、少し複雑な課題にも取り組みました。ここではまず、共通テーブル式（CTE）を使って国別・ジャンル別の売上を算出し、その結果に対して`ROW_NUMBER()`というウィンドウ関数を適用しました。これにより、国ごとに売上ランキングを付け、1位のデータだけを効率的に抽出することができました。こうした複雑な集計もSQLで対応可能です。」

#### 4. 自走力と学習意欲をアピール

研修で得た知識を土台に、さらに自ら学んでいく姿勢を示すことも大切です。

*   **アピール例:**
    「今回の研修課題を通して、基本的なデータ抽出から複雑な分析まで、幅広いSQLの記述方法を体系的に習得できました。今後は、クエリのパフォーマンスチューニング（インデックスの利用や実行計画の読解）についても学習を深め、より実務で貢献できるエンジニアを目指したいと考えています。」

これらのポイントを参考に、研修生が自信を持って自身のスキルをアピールできるよう、サポートしてあげてください。