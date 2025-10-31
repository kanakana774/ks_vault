## PostgreSQL spotify

### 問題 1: 各アーティストのアルバム数と総曲数を取得してください。

**回答:**

```sql
SELECT
  a.name AS artist_name,
  COUNT(DISTINCT al.album_id) AS album_count,
  COUNT(t.track_id) AS total_tracks
FROM artist AS a
LEFT JOIN album AS al
  ON a.artist_id = al.artist_id
LEFT JOIN track AS t
  ON al.album_id = t.album_id
GROUP BY
  a.name
ORDER BY
  artist_name;
```

---

### 問題 2: 2021 年以降に最も多くの顧客が購入したジャンルを上位 5 つ取得してください。

**ヒント:** `EXTRACT(YEAR FROM date_column)` を使用して年を抽出できます。

**回答:**

```sql
SELECT
  g.name AS genre_name,
  COUNT(DISTINCT i.customer_id) AS unique_customer_count
FROM genre AS g
JOIN track AS t
  ON g.genre_id = t.genre_id
JOIN invoice_line AS il
  ON t.track_id = il.track_id
JOIN invoice AS i
  ON il.invoice_id = i.invoice_id
WHERE
  EXTRACT(YEAR FROM i.invoice_date) >= 2021
GROUP BY
  g.name
ORDER BY
  unique_customer_count DESC
LIMIT 5;
```

---

### 問題 3: 各従業員の顧客サポート売上合計と、全従業員の平均売上に対する比率を計算してください。

**ヒント:** ウィンドウ関数 `SUM(...) OVER ()` で全体の合計を求められます。

**回答:**

```sql
SELECT
  e.first_name || ' ' || e.last_name AS employee_name,
  SUM(i.total) AS total_sales,
  (SUM(i.total) / SUM(SUM(i.total)) OVER ()) * 100 AS sales_percentage_of_total
FROM employee AS e
JOIN customer AS c
  ON e.employee_id = c.support_rep_id
JOIN invoice AS i
  ON c.customer_id = i.customer_id
GROUP BY
  e.employee_id,
  employee_name
ORDER BY
  total_sales DESC;
```

---

### 問題 4: 各顧客の最初の購入日と最後の購入日、およびその間の購入回数を取得してください。

**回答:**

```sql
SELECT
  c.first_name || ' ' || c.last_name AS customer_name,
  MIN(i.invoice_date) AS first_purchase_date,
  MAX(i.invoice_date) AS last_purchase_date,
  COUNT(i.invoice_id) AS total_purchases
FROM customer AS c
JOIN invoice AS i
  ON c.customer_id = i.customer_id
GROUP BY
  c.customer_id,
  customer_name
ORDER BY
  customer_name;
```

---

### 問題 5: アルバムに複数のジャンルが含まれているアルバムのタイトルと、含まれるジャンル数を取得してください。

**回答:**

```sql
SELECT
  a.title AS album_title,
  COUNT(DISTINCT g.genre_id) AS genre_count
FROM album AS a
JOIN track AS t
  ON a.album_id = t.album_id
JOIN genre AS g
  ON t.genre_id = g.genre_id
GROUP BY
  a.title
HAVING
  COUNT(DISTINCT g.genre_id) > 1
ORDER BY
  genre_count DESC,
  album_title;
```

---

### 問題 6: 各国で最も売上が高かったジャンルと、その国の総売上におけるそのジャンルの売上比率を取得してください。

**ヒント:** サブクエリとウィンドウ関数を組み合わせて、国ごとのジャンル売上と国ごとの総売上を計算し、比率を求めます。`RANK()` または `ROW_NUMBER()` を使用して、各国のトップジャンルを特定できます。

**回答:**

```sql
WITH CountryGenreSales AS (
  SELECT
    i.billing_country AS country,
    g.name AS genre_name,
    SUM(il.quantity * il.unit_price) AS genre_sales
  FROM invoice AS i
  JOIN invoice_line AS il
    ON i.invoice_id = il.invoice_id
  JOIN track AS t
    ON il.track_id = t.track_id
  JOIN genre AS g
    ON t.genre_id = g.genre_id
  GROUP BY
    i.billing_country,
    g.name
), CountryTotalSales AS (
  SELECT
    i.billing_country AS country,
    SUM(i.total) AS total_country_sales
  FROM invoice AS i
  GROUP BY
    i.billing_country
)
SELECT
  cgs.country,
  cgs.genre_name,
  cgs.genre_sales,
  (cgs.genre_sales / cts.total_country_sales) * 100 AS sales_percentage
FROM CountryGenreSales AS cgs
JOIN CountryTotalSales AS cts
  ON cgs.country = cts.country
WHERE
  (cgs.country, cgs.genre_sales) IN (
    SELECT
      country,
      MAX(genre_sales)
    FROM CountryGenreSales
    GROUP BY
      country
  )
ORDER BY
  cgs.country,
  sales_percentage DESC;

-- 別パターン (ROW_NUMBERを使用)
WITH CountryGenreSalesRanked AS (
  SELECT
    i.billing_country AS country,
    g.name AS genre_name,
    SUM(il.quantity * il.unit_price) AS genre_sales,
    SUM(SUM(il.quantity * il.unit_price)) OVER (PARTITION BY i.billing_country) AS total_country_sales,
    ROW_NUMBER() OVER (PARTITION BY i.billing_country ORDER BY SUM(il.quantity * il.unit_price) DESC) AS rn
  FROM invoice AS i
  JOIN invoice_line AS il
    ON i.invoice_id = il.invoice_id
  JOIN track AS t
    ON il.track_id = t.track_id
  JOIN genre AS g
    ON t.genre_id = g.genre_id
  GROUP BY
    i.billing_country,
    g.name
)
SELECT
  country,
  genre_name,
  genre_sales,
  (genre_sales / total_country_sales) * 100 AS sales_percentage
FROM CountryGenreSalesRanked
WHERE
  rn = 1
ORDER BY
  country,
  sales_percentage DESC;
```

---

### 問題 7: 過去 12 ヶ月間での各顧客の月ごとの購入金額と、その顧客の月平均購入金額からの乖離率を計算してください。

**ヒント:** `DATE_TRUNC('month', date_column)` で日付を月初に丸められます。ウィンドウ関数 `AVG(...) OVER (PARTITION BY customer_id)` で顧客ごとの月平均を計算し、`LAG()` や `LEAD()` を使わずに直接計算できます。

**回答:**

```sql
WITH MonthlyCustomerSales AS (
  SELECT
    c.customer_id,
    c.first_name || ' ' || c.last_name AS customer_name,
    DATE_TRUNC('month', i.invoice_date) AS sales_month,
    SUM(i.total) AS monthly_total_sales
  FROM customer AS c
  JOIN invoice AS i
    ON c.customer_id = i.customer_id
  WHERE
    i.invoice_date >= DATE_TRUNC('month', CURRENT_DATE) - INTERVAL '12 months'
    AND i.invoice_date < DATE_TRUNC('month', CURRENT_DATE)
  GROUP BY
    c.customer_id,
    customer_name,
    sales_month
), CustomerMonthlyAvg AS (
  SELECT
    customer_id,
    customer_name,
    AVG(monthly_total_sales) AS avg_monthly_sales
  FROM MonthlyCustomerSales
  GROUP BY
    customer_id,
    customer_name
)
SELECT
  mcs.customer_name,
  mcs.sales_month,
  mcs.monthly_total_sales,
  cma.avg_monthly_sales,
  (mcs.monthly_total_sales - cma.avg_monthly_sales) / cma.avg_monthly_sales * 100 AS deviation_percentage
FROM MonthlyCustomerSales AS mcs
JOIN CustomerMonthlyAvg AS cma
  ON mcs.customer_id = cma.customer_id
ORDER BY
  mcs.customer_name,
  mcs.sales_month;
```

---

### 問題 8: 各国の顧客のうち、最も購入金額が高かった顧客の氏名と購入金額、およびその国の全顧客の平均購入金額を取得してください。

**回答:**

```sql
WITH CustomerTotalSales AS (
  SELECT
    c.customer_id,
    c.first_name,
    c.last_name,
    c.country,
    SUM(i.total) AS total_purchase
  FROM customer AS c
  JOIN invoice AS i
    ON c.customer_id = i.customer_id
  GROUP BY
    c.customer_id,
    c.first_name,
    c.last_name,
    c.country
), CountryAvgSales AS (
  SELECT
    country,
    AVG(total_purchase) AS avg_country_purchase
  FROM CustomerTotalSales
  GROUP BY
    country
)
SELECT
  cts.country,
  (
    SELECT
      first_name || ' ' || last_name
    FROM CustomerTotalSales
    WHERE
      country = cts.country
    ORDER BY
      total_purchase DESC
    LIMIT 1
  ) AS top_customer_name,
  (
    SELECT
      total_purchase
    FROM CustomerTotalSales
    WHERE
      country = cts.country
    ORDER BY
      total_purchase DESC
    LIMIT 1
  ) AS top_customer_sales,
  cas.avg_country_purchase
FROM CustomerTotalSales AS cts
JOIN CountryAvgSales AS cas
  ON cts.country = cas.country
GROUP BY
  cts.country,
  cas.avg_country_purchase
ORDER BY
  cts.country;

-- 別パターン (ROW_NUMBERを使用)
WITH CustomerTotalSalesRanked AS (
  SELECT
    c.customer_id,
    c.first_name,
    c.last_name,
    c.country,
    SUM(i.total) AS total_purchase,
    AVG(SUM(i.total)) OVER (PARTITION BY c.country) AS avg_country_purchase,
    ROW_NUMBER() OVER (PARTITION BY c.country ORDER BY SUM(i.total) DESC) AS rn
  FROM customer AS c
  JOIN invoice AS i
    ON c.customer_id = i.customer_id
  GROUP BY
    c.customer_id,
    c.first_name,
    c.last_name,
    c.country
)
SELECT
  country,
  first_name || ' ' || last_name AS top_customer_name,
  total_purchase AS top_customer_sales,
  avg_country_purchase
FROM CustomerTotalSalesRanked
WHERE
  rn = 1
ORDER BY
  country;
```

---

### 問題 9: 全てのプレイリストに含まれる曲の数と、そのうち複数のプレイリストに含まれる曲の数を取得してください。

**ヒント:** `UNION ALL` と `GROUP BY` を組み合わせて、全曲数と重複曲数を計算できます。

**回答:**

```sql
SELECT
  'Total Tracks in Playlists' AS category,
  COUNT(DISTINCT track_id) AS track_count
FROM playlist_track
UNION ALL
SELECT
  'Tracks in Multiple Playlists' AS category,
  COUNT(track_id) AS track_count
FROM (
  SELECT
    track_id
  FROM playlist_track
  GROUP BY
    track_id
  HAVING
    COUNT(playlist_id) > 1
) AS multiple_playlists;
```

---

### 問題 10: 各アルバムの最長トラック名、最短トラック名、平均トラック長（ミリ秒）を取得してください。

**回答:**

```sql
SELECT
  a.title AS album_title,
  (
    SELECT
      t2.name
    FROM track AS t2
    WHERE
      t2.album_id = a.album_id
    ORDER BY
      t2.milliseconds DESC
    LIMIT 1
  ) AS longest_track_name,
  (
    SELECT
      t3.name
    FROM track AS t3
    WHERE
      t3.album_id = a.album_id
    ORDER BY
      t3.milliseconds ASC
    LIMIT 1
  ) AS shortest_track_name,
  AVG(t.milliseconds) AS average_track_length_ms
FROM album AS a
JOIN track AS t
  ON a.album_id = t.album_id
GROUP BY
  a.album_id,
  a.title
ORDER BY
  a.title;

-- 別パターン (ウィンドウ関数と結合を使用)
WITH RankedTracks AS (
  SELECT
    album_id,
    name,
    milliseconds,
    ROW_NUMBER() OVER (PARTITION BY album_id ORDER BY milliseconds DESC) AS rn_longest,
    ROW_NUMBER() OVER (PARTITION BY album_id ORDER BY milliseconds ASC) AS rn_shortest
  FROM track
)
SELECT
  a.title AS album_title,
  MAX(CASE WHEN rt_longest.rn_longest = 1 THEN rt_longest.name ELSE NULL END) AS longest_track_name,
  MAX(CASE WHEN rt_shortest.rn_shortest = 1 THEN rt_shortest.name ELSE NULL END) AS shortest_track_name,
  AVG(t.milliseconds) AS average_track_length_ms
FROM album AS a
LEFT JOIN track AS t
  ON a.album_id = t.album_id
LEFT JOIN RankedTracks AS rt_longest
  ON a.album_id = rt_longest.album_id
LEFT JOIN RankedTracks AS rt_shortest
  ON a.album_id = rt_shortest.album_id
GROUP BY
  a.album_id,
  a.title
ORDER BY
  a.title;
```

---

### 問題 11: 各従業員の直接の上司の名前と、その上司が管理している従業員の数を取得してください。

**回答:**

```sql
SELECT
  e1.first_name || ' ' || e1.last_name AS employee_name,
  e2.first_name || ' ' || e2.last_name AS manager_name,
  COUNT(e3.employee_id) AS subordinates_count
FROM employee AS e1
LEFT JOIN employee AS e2
  ON e1.reports_to = e2.employee_id
LEFT JOIN employee AS e3
  ON e2.employee_id = e3.reports_to
GROUP BY
  e1.employee_id,
  employee_name,
  manager_name
ORDER BY
  employee_name;
```

---

### 問題 12: 顧客ごとの購入履歴で、前回購入時からの経過日数と累積購入金額を取得してください。

**ヒント:** ウィンドウ関数 `LAG()` と `SUM(...) OVER (ORDER BY ...)` を使用します。経過日数は `EXTRACT(DAY FROM (current_date - previous_date))` や `(current_date::date - previous_date::date)` で計算できます。

**回答:**

```sql
SELECT
  c.first_name || ' ' || c.last_name AS customer_name,
  i.invoice_date,
  i.total AS purchase_amount,
  (i.invoice_date - LAG(i.invoice_date) OVER (PARTITION BY c.customer_id ORDER BY i.invoice_date)) AS days_since_last_purchase,
  SUM(i.total) OVER (PARTITION BY c.customer_id ORDER BY i.invoice_date) AS cumulative_purchase_amount
FROM customer AS c
JOIN invoice AS i
  ON c.customer_id = i.customer_id
ORDER BY
  customer_name,
  i.invoice_date;
```

---

### 問題 13: 各メディアタイプで最も売上が高かったトラックの名称と、その売上金額を取得してください。

**回答:**

```sql
WITH TrackSales AS (
  SELECT
    t.track_id,
    t.name AS track_name,
    mt.name AS media_type_name,
    SUM(il.quantity * il.unit_price) AS total_sales,
    ROW_NUMBER() OVER (PARTITION BY mt.media_type_id ORDER BY SUM(il.quantity * il.unit_price) DESC) AS rn
  FROM track AS t
  JOIN media_type AS mt
    ON t.media_type_id = mt.media_type_id
  JOIN invoice_line AS il
    ON t.track_id = il.track_id
  GROUP BY
    t.track_id,
    t.name,
    mt.name
)
SELECT
  media_type_name,
  track_name,
  total_sales
FROM TrackSales
WHERE
  rn = 1
ORDER BY
  media_type_name;
```

---

### 問題 14: 2020 年以降に全く購入がない顧客の氏名とメールアドレスを取得してください。

**回答:**

```sql
SELECT
  c.first_name,
  c.last_name,
  c.email
FROM customer AS c
WHERE
  NOT EXISTS (
    SELECT
      1
    FROM invoice AS i
    WHERE
      i.customer_id = c.customer_id
      AND i.invoice_date >= '2020-01-01'
  )
ORDER BY
  c.last_name,
  c.first_name;

-- 別パターン (LEFT JOIN と IS NULL)
SELECT
  c.first_name,
  c.last_name,
  c.email
FROM customer AS c
LEFT JOIN invoice AS i
  ON c.customer_id = i.customer_id
  AND i.invoice_date >= '2020-01-01'
GROUP BY
  c.customer_id,
  c.first_name,
  c.last_name,
  c.email
HAVING
  COUNT(i.invoice_id) = 0
ORDER BY
  c.last_name,
  c.first_name;
```

---

### 問題 15: 各ジャンルで最も平均トラック長が短いアルバムのタイトルと、その平均トラック長を取得してください。

**回答:**

```sql
WITH AlbumGenreAvgTrackLength AS (
  SELECT
    a.album_id,
    a.title AS album_title,
    g.name AS genre_name,
    AVG(t.milliseconds) AS avg_track_length_ms
  FROM album AS a
  JOIN track AS t
    ON a.album_id = t.album_id
  JOIN genre AS g
    ON t.genre_id = g.genre_id
  GROUP BY
    a.album_id,
    a.title,
    g.name
)
SELECT
  genre_name,
  album_title,
  avg_track_length_ms
FROM AlbumGenreAvgTrackLength
WHERE
  (genre_name, avg_track_length_ms) IN (
    SELECT
      genre_name,
      MIN(avg_track_length_ms)
    FROM AlbumGenreAvgTrackLength
    GROUP BY
      genre_name
  )
ORDER BY
  genre_name,
  avg_track_length_ms;

-- 別パターン (ROW_NUMBERを使用)
WITH AlbumGenreAvgTrackLengthRanked AS (
  SELECT
    a.album_id,
    a.title AS album_title,
    g.name AS genre_name,
    AVG(t.milliseconds) AS avg_track_length_ms,
    ROW_NUMBER() OVER (PARTITION BY g.genre_id ORDER BY AVG(t.milliseconds) ASC) AS rn
  FROM album AS a
  JOIN track AS t
    ON a.album_id = t.album_id
  JOIN genre AS g
    ON t.genre_id = g.genre_id
  GROUP BY
    a.album_id,
    a.title,
    g.name
)
SELECT
  genre_name,
  album_title,
  avg_track_length_ms
FROM AlbumGenreAvgTrackLengthRanked
WHERE
  rn = 1
ORDER BY
  genre_name;
```

---

### 問題 16: 各アーティストのアルバムごとの売上合計と、そのアーティストの総売上に対する比率を計算してください。

**回答:**

```sql
WITH ArtistAlbumSales AS (
  SELECT
    ar.name AS artist_name,
    al.title AS album_title,
    SUM(il.quantity * il.unit_price) AS album_sales,
    SUM(SUM(il.quantity * il.unit_price)) OVER (PARTITION BY ar.artist_id) AS artist_total_sales
  FROM artist AS ar
  JOIN album AS al
    ON ar.artist_id = al.artist_id
  JOIN track AS t
    ON al.album_id = t.album_id
  JOIN invoice_line AS il
    ON t.track_id = il.track_id
  GROUP BY
    ar.artist_id,
    ar.name,
    al.album_id,
    al.title
)
SELECT
  artist_name,
  album_title,
  album_sales,
  (album_sales / artist_total_sales) * 100 AS sales_percentage_of_artist_total
FROM ArtistAlbumSales
ORDER BY
  artist_name,
  album_sales DESC;
```

---

### 問題 17: 2022 年に購入があった顧客と、2023 年に購入があった顧客の両方に共通する顧客の氏名とメールアドレスを取得してください。

**回答:**

```sql
SELECT
  c.first_name,
  c.last_name,
  c.email
FROM customer AS c
JOIN invoice AS i
  ON c.customer_id = i.customer_id
WHERE
  EXTRACT(YEAR FROM i.invoice_date) = 2022
INTERSECT
SELECT
  c.first_name,
  c.last_name,
  c.email
FROM customer AS c
JOIN invoice AS i
  ON c.customer_id = i.customer_id
WHERE
  EXTRACT(YEAR FROM i.invoice_date) = 2023
ORDER BY
  last_name,
  first_name;
```

---

### 問題 18: 各顧客が購入した最も高価なトラックと最も安価なトラックの名前、およびそれらの単価を取得してください。

**回答:**

```sql
WITH CustomerTrackPrices AS (
  SELECT
    c.customer_id,
    c.first_name || ' ' || c.last_name AS customer_name,
    t.name AS track_name,
    il.unit_price,
    ROW_NUMBER() OVER (PARTITION BY c.customer_id ORDER BY il.unit_price DESC) AS rn_highest,
    ROW_NUMBER() OVER (PARTITION BY c.customer_id ORDER BY il.unit_price ASC) AS rn_lowest
  FROM customer AS c
  JOIN invoice AS i
    ON c.customer_id = i.customer_id
  JOIN invoice_line AS il
    ON i.invoice_id = il.invoice_id
  JOIN track AS t
    ON il.track_id = t.track_id
)
SELECT
  ctp_high.customer_name,
  ctp_high.track_name AS highest_price_track,
  ctp_high.unit_price AS highest_price,
  ctp_low.track_name AS lowest_price_track,
  ctp_low.unit_price AS lowest_price
FROM CustomerTrackPrices AS ctp_high
JOIN CustomerTrackPrices AS ctp_low
  ON ctp_high.customer_id = ctp_low.customer_id
WHERE
  ctp_high.rn_highest = 1 AND ctp_low.rn_lowest = 1
ORDER BY
  ctp_high.customer_name;
```

---

### 問題 19: 従業員の直属の上司の名前、さらにその上司の名前（存在する場合）を最大 3 階層まで取得してください。

**ヒント:** 複数回の自己結合（Self-Join）を使用します。

**回答:**

```sql
SELECT
  e1.first_name || ' ' || e1.last_name AS employee_name,
  e2.first_name || ' ' || e2.last_name AS manager_1_name,
  e3.first_name || ' ' || e3.last_name AS manager_2_name,
  e4.first_name || ' ' || e4.last_name AS manager_3_name
FROM employee AS e1
LEFT JOIN employee AS e2
  ON e1.reports_to = e2.employee_id
LEFT JOIN employee AS e3
  ON e2.reports_to = e3.employee_id
LEFT JOIN employee AS e4
  ON e3.reports_to = e4.employee_id
ORDER BY
  employee_name;
```

---

### 問題 20: 各ジャンルで最も人気のある（最も売上合計が高い）トラックのタイトルと、その売上合計を取得してください。

**回答:**

```sql
WITH GenreTrackSales AS (
  SELECT
    g.name AS genre_name,
    t.name AS track_name,
    SUM(il.quantity * il.unit_price) AS total_track_sales,
    ROW_NUMBER() OVER (PARTITION BY g.genre_id ORDER BY SUM(il.quantity * il.unit_price) DESC) AS rn
  FROM genre AS g
  JOIN track AS t
    ON g.genre_id = t.genre_id
  JOIN invoice_line AS il
    ON t.track_id = il.track_id
  GROUP BY
    g.genre_id,
    g.name,
    t.name
)
SELECT
  genre_name,
  track_name,
  total_track_sales
FROM GenreTrackSales
WHERE
  rn = 1
ORDER BY
  genre_name;
```

---

### 問題 21: 各顧客の購入総額が、その顧客の居住国における平均購入総額よりも高い顧客の氏名、国、購入総額、および国の平均購入総額を取得してください。

**回答:**

```sql
WITH CustomerTotalSales AS (
  SELECT
    c.customer_id,
    c.first_name,
    c.last_name,
    c.country,
    SUM(i.total) AS total_purchase_amount
  FROM customer AS c
  JOIN invoice AS i
    ON c.customer_id = i.customer_id
  GROUP BY
    c.customer_id,
    c.first_name,
    c.last_name,
    c.country
), CountryAvgSales AS (
  SELECT
    country,
    AVG(total_purchase_amount) AS avg_country_purchase_amount
  FROM CustomerTotalSales
  GROUP BY
    country
)
SELECT
  cts.first_name || ' ' || cts.last_name AS customer_name,
  cts.country,
  cts.total_purchase_amount,
  cas.avg_country_purchase_amount
FROM CustomerTotalSales AS cts
JOIN CountryAvgSales AS cas
  ON cts.country = cas.country
WHERE
  cts.total_purchase_amount > cas.avg_country_purchase_amount
ORDER BY
  cts.country,
  cts.total_purchase_amount DESC;
```

---

### 問題 22: 過去 3 ヶ月以内に購入があった顧客と、一度も購入がない顧客の情報をそれぞれ取得し、結合して表示してください。

**ヒント:** `UNION` を使用します。

**回答:**

```sql
-- 過去3ヶ月以内に購入があった顧客
SELECT
  c.first_name,
  c.last_name,
  c.email,
  'Recent Buyer' AS customer_status
FROM customer AS c
WHERE
  EXISTS (
    SELECT
      1
    FROM invoice AS i
    WHERE
      i.customer_id = c.customer_id
      AND i.invoice_date >= CURRENT_DATE - INTERVAL '3 months'
  )
UNION
-- 一度も購入がない顧客
SELECT
  c.first_name,
  c.last_name,
  c.email,
  'Non-Buyer' AS customer_status
FROM customer AS c
WHERE
  NOT EXISTS (
    SELECT
      1
    FROM invoice AS i
    WHERE
      i.customer_id = c.customer_id
  )
ORDER BY
  customer_status,
  last_name,
  first_name;
```

---

### 問題 23: 各顧客が購入した総トラック数、総アルバム数、総アーティスト数を取得してください。

**回答:**

```sql
SELECT
  c.first_name || ' ' || c.last_name AS customer_name,
  COUNT(DISTINCT il.track_id) AS total_tracks_purchased,
  COUNT(DISTINCT t.album_id) AS total_albums_purchased,
  COUNT(DISTINCT al.artist_id) AS total_artists_purchased
FROM customer AS c
LEFT JOIN invoice AS i
  ON c.customer_id = i.customer_id
LEFT JOIN invoice_line AS il
  ON i.invoice_id = il.invoice_id
LEFT JOIN track AS t
  ON il.track_id = t.track_id
LEFT JOIN album AS al
  ON t.album_id = al.album_id
GROUP BY
  c.customer_id,
  customer_name
ORDER BY
  customer_name;
```

---

### 問題 24: 各従業員の入社年ごとの売上合計と、全従業員の入社年ごとの売上合計に対する比率を計算してください。

**回答:**

```sql
WITH EmployeeHireYearSales AS (
  SELECT
    EXTRACT(YEAR FROM e.hire_date) AS hire_year,
    e.first_name || ' ' || e.last_name AS employee_name,
    SUM(i.total) AS employee_sales_in_year
  FROM employee AS e
  JOIN customer AS c
    ON e.employee_id = c.support_rep_id
  JOIN invoice AS i
    ON c.customer_id = i.customer_id
  GROUP BY
    EXTRACT(YEAR FROM e.hire_date),
    e.employee_id,
    employee_name
), TotalHireYearSales AS (
  SELECT
    hire_year,
    SUM(employee_sales_in_year) AS total_sales_for_hire_year
  FROM EmployeeHireYearSales
  GROUP BY
    hire_year
)
SELECT
  ehys.hire_year,
  ehys.employee_name,
  ehys.employee_sales_in_year,
  (ehys.employee_sales_in_year / thys.total_sales_for_hire_year) * 100 AS percentage_of_year_sales
FROM EmployeeHireYearSales AS ehys
JOIN TotalHireYearSales AS thys
  ON ehys.hire_year = thys.hire_year
ORDER BY
  ehys.hire_year,
  employee_sales_in_year DESC;
```

---

### 問題 25: 各トラックの購入頻度（購入された回数）と、そのトラックがプレイリストに追加された回数を取得してください。

**回答:**

```sql
SELECT
  t.name AS track_name,
  COUNT(DISTINCT il.invoice_line_id) AS purchase_count,
  COUNT(DISTINCT pt.playlist_id) AS playlist_add_count
FROM track AS t
LEFT JOIN invoice_line AS il
  ON t.track_id = il.track_id
LEFT JOIN playlist_track AS pt
  ON t.track_id = pt.track_id
GROUP BY
  t.track_id,
  t.name
ORDER BY
  purchase_count DESC,
  playlist_add_count DESC,
  track_name;
```

---

### 問題 26: 各国で、最も多くの顧客が居住している都市と、その都市の顧客数を取得してください。

**回答:**

```sql
WITH CityCustomerCount AS (
  SELECT
    country,
    city,
    COUNT(customer_id) AS customer_count,
    ROW_NUMBER() OVER (PARTITION BY country ORDER BY COUNT(customer_id) DESC) AS rn
  FROM customer
  GROUP BY
    country,
    city
)
SELECT
  country,
  city AS most_customers_city,
  customer_count
FROM CityCustomerCount
WHERE
  rn = 1
ORDER BY
  country;
```

---

### 問題 27: 各顧客の購入履歴において、連続して同じジャンルの曲を購入した回数と、そのジャンル名を取得してください。

**ヒント:** ウィンドウ関数 `LAG()` と `CASE` 文を組み合わせて連続購入を検出します。

**回答:**

```sql
WITH CustomerPurchaseHistory AS (
  SELECT
    c.customer_id,
    c.first_name || ' ' || c.last_name AS customer_name,
    i.invoice_date,
    g.name AS genre_name,
    ROW_NUMBER() OVER (PARTITION BY c.customer_id ORDER BY i.invoice_date, il.invoice_line_id) AS purchase_order
  FROM customer AS c
  JOIN invoice AS i
    ON c.customer_id = i.customer_id
  JOIN invoice_line AS il
    ON i.invoice_id = il.invoice_id
  JOIN track AS t
    ON il.track_id = t.track_id
  JOIN genre AS g
    ON t.genre_id = g.genre_id
), ConsecutiveGenrePurchases AS (
  SELECT
    customer_id,
    customer_name,
    invoice_date,
    genre_name,
    purchase_order,
    LAG(genre_name, 1, '') OVER (PARTITION BY customer_id ORDER BY purchase_order) AS prev_genre_name,
    CASE
      WHEN genre_name = LAG(genre_name, 1, '') OVER (PARTITION BY customer_id ORDER BY purchase_order) THEN 0
      ELSE 1
    END AS is_new_group
  FROM CustomerPurchaseHistory
), GroupedConsecutivePurchases AS (
  SELECT
    customer_id,
    customer_name,
    genre_name,
    SUM(is_new_group) OVER (PARTITION BY customer_id ORDER BY purchase_order) AS genre_group
  FROM ConsecutiveGenrePurchases
)
SELECT
  customer_name,
  genre_name,
  COUNT(*) AS consecutive_purchase_count
FROM GroupedConsecutivePurchases
GROUP BY
  customer_id,
  customer_name,
  genre_name,
  genre_group
HAVING
  COUNT(*) > 1
ORDER BY
  customer_name,
  consecutive_purchase_count DESC,
  genre_name;
```

---

### 問題 28: 各顧客が購入した最も多いジャンルと、そのジャンルにおける購入金額合計を取得してください。

**回答:**

```sql
WITH CustomerGenreSales AS (
  SELECT
    c.customer_id,
    c.first_name || ' ' || c.last_name AS customer_name,
    g.name AS genre_name,
    SUM(il.quantity * il.unit_price) AS genre_purchase_amount,
    ROW_NUMBER() OVER (PARTITION BY c.customer_id ORDER BY SUM(il.quantity * il.unit_price) DESC) AS rn
  FROM customer AS c
  JOIN invoice AS i
    ON c.customer_id = i.customer_id
  JOIN invoice_line AS il
    ON i.invoice_id = il.invoice_id
  JOIN track AS t
    ON il.track_id = t.track_id
  JOIN genre AS g
    ON t.genre_id = g.genre_id
  GROUP BY
    c.customer_id,
    customer_name,
    g.name
)
SELECT
  customer_name,
  genre_name AS most_purchased_genre,
  genre_purchase_amount
FROM CustomerGenreSales
WHERE
  rn = 1
ORDER BY
  customer_name;
```

---

### 問題 29: 全従業員の平均給与（架空で、ここでは売上合計とする）よりも多くの売上を上げた従業員の氏名と売上合計、および平均売上を取得してください。

**回答:**

```sql
WITH EmployeeSales AS (
  SELECT
    e.employee_id,
    e.first_name || ' ' || e.last_name AS employee_name,
    SUM(i.total) AS total_sales
  FROM employee AS e
  JOIN customer AS c
    ON e.employee_id = c.support_rep_id
  JOIN invoice AS i
    ON c.customer_id = i.customer_id
  GROUP BY
    e.employee_id,
    employee_name
), AvgTotalSales AS (
  SELECT
    AVG(total_sales) AS overall_avg_sales
  FROM EmployeeSales
)
SELECT
  es.employee_name,
  es.total_sales,
  ats.overall_avg_sales
FROM EmployeeSales AS es, AvgTotalSales AS ats
WHERE
  es.total_sales > ats.overall_avg_sales
ORDER BY
  es.total_sales DESC;

-- 別パターン (ウィンドウ関数を使用)
SELECT
  e.first_name || ' ' || e.last_name AS employee_name,
  SUM(i.total) AS total_sales,
  AVG(SUM(i.total)) OVER () AS overall_avg_sales
FROM employee AS e
JOIN customer AS c
  ON e.employee_id = c.support_rep_id
JOIN invoice AS i
  ON c.customer_id = i.customer_id
GROUP BY
  e.employee_id,
  employee_name
HAVING
  SUM(i.total) > AVG(SUM(i.total)) OVER ()
ORDER BY
  total_sales DESC;
```

---

### 問題 30: 各アルバムに属するトラックのうち、最も高価なトラックと最も安価なトラックの名前と価格、そしてアルバムの総価格（全トラックの単価合計）を取得してください。

**回答:**

```sql
WITH AlbumTrackPriceRank AS (
  SELECT
    al.album_id,
    al.title AS album_title,
    t.name AS track_name,
    t.unit_price,
    ROW_NUMBER() OVER (PARTITION BY al.album_id ORDER BY t.unit_price DESC) AS rn_highest,
    ROW_NUMBER() OVER (PARTITION BY al.album_id ORDER BY t.unit_price ASC) AS rn_lowest
  FROM album AS al
  JOIN track AS t
    ON al.album_id = t.album_id
)
SELECT
  atpr_high.album_title,
  atpr_high.track_name AS highest_price_track,
  atpr_high.unit_price AS highest_price,
  atpr_low.track_name AS lowest_price_track,
  atpr_low.unit_price AS lowest_price,
  SUM(t_total.unit_price) AS total_album_price
FROM AlbumTrackPriceRank AS atpr_high
JOIN AlbumTrackPriceRank AS atpr_low
  ON atpr_high.album_id = atpr_low.album_id
JOIN track AS t_total
  ON atpr_high.album_id = t_total.album_id
WHERE
  atpr_high.rn_highest = 1 AND atpr_low.rn_lowest = 1
GROUP BY
  atpr_high.album_id,
  atpr_high.album_title,
  highest_price_track,
  highest_price,
  lowest_price_track,
  lowest_price
ORDER BY
  atpr_high.album_title;
```

---

### 問題 31: 各顧客の購入総額が、全体の上位 20%に入る顧客の氏名と購入総額を取得してください。

**ヒント:** `NTILE()` ウィンドウ関数を使用してデータを分位に分割します。

**回答:**

```sql
WITH CustomerTotalSales AS (
  SELECT
    c.customer_id,
    c.first_name || ' ' || c.last_name AS customer_name,
    SUM(i.total) AS total_purchase_amount
  FROM customer AS c
  JOIN invoice AS i
    ON c.customer_id = i.customer_id
  GROUP BY
    c.customer_id,
    customer_name
), RankedCustomerSales AS (
  SELECT
    customer_name,
    total_purchase_amount,
    NTILE(5) OVER (ORDER BY total_purchase_amount DESC) AS sales_quartile -- 5分位に分割し、1が上位20%
  FROM CustomerTotalSales
)
SELECT
  customer_name,
  total_purchase_amount
FROM RankedCustomerSales
WHERE
  sales_quartile = 1
ORDER BY
  total_purchase_amount DESC;
```

---

### 問題 32: 各国の顧客ごとの購入金額について、その国の購入金額の合計に対する割合を計算し、さらにその顧客がその国で何番目に購入金額が高いかを取得してください。

**回答:**

```sql
WITH CustomerCountrySales AS (
  SELECT
    c.customer_id,
    c.first_name || ' ' || c.last_name AS customer_name,
    c.country,
    SUM(i.total) AS total_purchase_amount
  FROM customer AS c
  JOIN invoice AS i
    ON c.customer_id = i.customer_id
  GROUP BY
    c.customer_id,
    customer_name,
    c.country
)
SELECT
  ccs.country,
  ccs.customer_name,
  ccs.total_purchase_amount,
  (ccs.total_purchase_amount / SUM(ccs.total_purchase_amount) OVER (PARTITION BY ccs.country)) * 100 AS percentage_of_country_sales,
  RANK() OVER (PARTITION BY ccs.country ORDER BY ccs.total_purchase_amount DESC) AS rank_in_country
FROM CustomerCountrySales AS ccs
ORDER BY
  ccs.country,
  rank_in_country;
```

---

### 問題 33: プレイリストに含まれていないトラックと、購入されたことがないトラックの両方を結合して取得してください。

**回答:**

```sql
-- プレイリストに含まれていないトラック
SELECT
  t.track_id,
  t.name AS track_name,
  'Not in Playlist' AS status
FROM track AS t
LEFT JOIN playlist_track AS pt
  ON t.track_id = pt.track_id
WHERE
  pt.track_id IS NULL
UNION
-- 購入されたことがないトラック
SELECT
  t.track_id,
  t.name AS track_name,
  'Never Purchased' AS status
FROM track AS t
LEFT JOIN invoice_line AS il
  ON t.track_id = il.track_id
WHERE
  il.track_id IS NULL
ORDER BY
  track_name,
  status;
```

---

### 問題 34: 各顧客の最初の購入から 30 日間の購入総額と、その期間中に購入したユニークなトラック数を取得してください。

**ヒント:** `MIN(invoice_date) OVER (PARTITION BY customer_id)` で最初の購入日を特定し、日付計算を行います。

**回答:**

```sql
WITH CustomerFirstPurchase AS (
  SELECT
    customer_id,
    MIN(invoice_date) AS first_purchase_date
  FROM invoice
  GROUP BY
    customer_id
)
SELECT
  c.first_name || ' ' || c.last_name AS customer_name,
  cfp.first_purchase_date,
  SUM(i.total) AS total_sales_first_30_days,
  COUNT(DISTINCT il.track_id) AS unique_tracks_first_30_days
FROM customer AS c
JOIN CustomerFirstPurchase AS cfp
  ON c.customer_id = cfp.customer_id
JOIN invoice AS i
  ON c.customer_id = i.customer_id
WHERE
  i.invoice_date BETWEEN cfp.first_purchase_date AND (cfp.first_purchase_date + INTERVAL '29 days')
GROUP BY
  c.customer_id,
  customer_name,
  cfp.first_purchase_date
ORDER BY
  customer_name;
```

---

### 問題 35: 各アーティストのアルバムの中で、平均トラック長が最も長いアルバムのタイトルと、その平均トラック長を取得してください。

**回答:**

```sql
WITH ArtistAlbumAvgLength AS (
  SELECT
    ar.artist_id,
    ar.name AS artist_name,
    al.title AS album_title,
    AVG(t.milliseconds) AS avg_track_length_ms,
    ROW_NUMBER() OVER (PARTITION BY ar.artist_id ORDER BY AVG(t.milliseconds) DESC) AS rn
  FROM artist AS ar
  JOIN album AS al
    ON ar.artist_id = al.artist_id
  JOIN track AS t
    ON al.album_id = t.album_id
  GROUP BY
    ar.artist_id,
    ar.name,
    al.title
)
SELECT
  artist_name,
  album_title AS longest_avg_track_album,
  avg_track_length_ms
FROM ArtistAlbumAvgLength
WHERE
  rn = 1
ORDER BY
  artist_name;
```

---

### 問題 36: 各ジャンルのトラックのうち、単価がそのジャンルの平均単価よりも高いトラックのタイトル、ジャンル名、単価、およびジャンルの平均単価を取得してください。

**回答:**

```sql
WITH GenreAvgUnitPrice AS (
  SELECT
    genre_id,
    AVG(unit_price) AS avg_genre_unit_price
  FROM track
  GROUP BY
    genre_id
)
SELECT
  t.name AS track_name,
  g.name AS genre_name,
  t.unit_price,
  gaup.avg_genre_unit_price
FROM track AS t
JOIN genre AS g
  ON t.genre_id = g.genre_id
JOIN GenreAvgUnitPrice AS gaup
  ON t.genre_id = gaup.genre_id
WHERE
  t.unit_price > gaup.avg_genre_unit_price
ORDER BY
  genre_name,
  t.unit_price DESC;

-- 別パターン (ウィンドウ関数を使用)
SELECT
  t.name AS track_name,
  g.name AS genre_name,
  t.unit_price,
  AVG(t.unit_price) OVER (PARTITION BY g.genre_id) AS avg_genre_unit_price
FROM track AS t
JOIN genre AS g
  ON t.genre_id = g.genre_id
WHERE
  t.unit_price > AVG(t.unit_price) OVER (PARTITION BY g.genre_id)
ORDER BY
  genre_name,
  t.unit_price DESC;
```

---

### 問題 37: 各年の四半期ごとの総売上と、前年同四半期からの成長率を計算してください。

**ヒント:** `EXTRACT(YEAR FROM date_column)` と `EXTRACT(QUARTER FROM date_column)` で年と四半期を抽出します。`LAG()` ウィンドウ関数で前年同四半期の売上を取得します。

**回答:**

```sql
WITH QuarterlySales AS (
  SELECT
    EXTRACT(YEAR FROM invoice_date) AS sales_year,
    EXTRACT(QUARTER FROM invoice_date) AS sales_quarter,
    SUM(total) AS quarterly_total_sales
  FROM invoice
  GROUP BY
    sales_year,
    sales_quarter
)
SELECT
  qs.sales_year,
  qs.sales_quarter,
  qs.quarterly_total_sales,
  LAG(qs.quarterly_total_sales, 1) OVER (PARTITION BY qs.sales_quarter ORDER BY qs.sales_year) AS previous_year_quarter_sales,
  (
    (qs.quarterly_total_sales - LAG(qs.quarterly_total_sales, 1) OVER (PARTITION BY qs.sales_quarter ORDER BY qs.sales_year)) /
    LAG(qs.quarterly_total_sales, 1) OVER (PARTITION BY qs.sales_quarter ORDER BY qs.sales_year)
  ) * 100 AS year_over_year_growth_percentage
FROM QuarterlySales AS qs
ORDER BY
  qs.sales_year,
  qs.sales_quarter;
```

---

### 問題 38: 2020 年以前に作成されたプレイリストにのみ含まれるトラックと、2021 年以降に作成されたプレイリストにのみ含まれるトラックをそれぞれ取得してください。

**ヒント:** `EXCEPT` 集合演算子とサブクエリを使用します。プレイリストの作成日情報がないため、ここではトラックがプレイリストに追加された日付（invoice_date）の年で代替するか、より単純に「プレイリスト A には含まれるがプレイリスト B には含まれない」といった論理で解釈します。ここでは、トラックがプレイリストに追加された日を考慮しない、純粋なプレイリスト包含の有無で考えます。

**回答:**

```sql
-- TrackがPlayListに含まれているかどうかはPlayList_Trackテーブルで判断
-- しかし、PlayListテーブルには作成日がないため、
-- 「特定のプレイリストには含まれるが、他の特定のプレイリストには含まれないトラック」
-- という解釈で問題を再構成します。
-- 例: playlist_id = 1 には含まれるが playlist_id = 2 には含まれないトラック
-- 例: Rockプレイリストには含まれるが、Classicalプレイリストには含まれないトラック

-- ここでは、問題を「あるプレイリストには含まれるが、別の特定のプレイリストには含まれないトラック」と解釈します。
-- 具体的なプレイリストIDや名前がないため、仮に「Playlist 1 と Playlist 2」という設定で記述します。
-- (実際のデータで `playlist.name` を使ってフィルタリングすることも可能です。)

-- 例：Playlist IDが1のプレイリストには含まれるが、Playlist IDが2のプレイリストには含まれないトラック
SELECT
  t.track_id,
  t.name AS track_name
FROM track AS t
JOIN playlist_track AS pt1
  ON t.track_id = pt1.track_id
WHERE
  pt1.playlist_id = 1
EXCEPT
SELECT
  t.track_id,
  t.name AS track_name
FROM track AS t
JOIN playlist_track AS pt2
  ON t.track_id = pt2.track_id
WHERE
  pt2.playlist_id = 2;
```

**補足:** `playlist` テーブルに作成日 (`created_date` など) がないため、この問題は「2020 年以前に作成されたプレイリスト」という条件を満たすことができません。上記の回答は、「特定の条件を持つプレイリストにのみ含まれるトラック」という解釈で記述しています。もし`playlist`テーブルに日付情報があれば、その情報を使って絞り込みが可能です。

---

### 問題 39: 各顧客の購入品目において、最も購入回数が多いトラックを特定し、そのトラックの名称と購入回数を取得してください。

**回答:**

```sql
WITH CustomerTrackPurchaseCount AS (
  SELECT
    c.customer_id,
    c.first_name || ' ' || c.last_name AS customer_name,
    t.name AS track_name,
    COUNT(il.track_id) AS purchase_count,
    ROW_NUMBER() OVER (PARTITION BY c.customer_id ORDER BY COUNT(il.track_id) DESC) AS rn
  FROM customer AS c
  JOIN invoice AS i
    ON c.customer_id = i.customer_id
  JOIN invoice_line AS il
    ON i.invoice_id = il.invoice_id
  JOIN track AS t
    ON il.track_id = t.track_id
  GROUP BY
    c.customer_id,
    customer_name,
    t.name
)
SELECT
  customer_name,
  track_name AS most_purchased_track,
  purchase_count
FROM CustomerTrackPurchaseCount
WHERE
  rn = 1
ORDER BY
  customer_name;
```

---

### 問題 40: 各顧客が購入した総金額が、その顧客の担当サポート担当者の管理する全顧客の平均購入金額よりも高い顧客の氏名、総金額、およびサポート担当者の管理する顧客の平均購入金額を取得してください。

**回答:**

```sql
WITH CustomerTotalSales AS (
  SELECT
    c.customer_id,
    c.first_name || ' ' || c.last_name AS customer_name,
    c.support_rep_id,
    SUM(i.total) AS total_customer_sales
  FROM customer AS c
  JOIN invoice AS i
    ON c.customer_id = i.customer_id
  GROUP BY
    c.customer_id,
    customer_name,
    c.support_rep_id
), SupportRepAvgSales AS (
  SELECT
    support_rep_id,
    AVG(total_customer_sales) AS avg_rep_customer_sales
  FROM CustomerTotalSales
  GROUP BY
    support_rep_id
)
SELECT
  cts.customer_name,
  cts.total_customer_sales,
  sras.avg_rep_customer_sales,
  e.first_name || ' ' || e.last_name AS support_rep_name
FROM CustomerTotalSales AS cts
JOIN SupportRepAvgSales AS sras
  ON cts.support_rep_id = sras.support_rep_id
JOIN employee AS e
  ON cts.support_rep_id = e.employee_id
WHERE
  cts.total_customer_sales > sras.avg_rep_customer_sales
ORDER BY
  cts.total_customer_sales DESC;
```

---

### 問題 41: 各顧客の購入履歴で、購入金額がその顧客の過去の購入の最大金額を上回った回数を取得してください。

**ヒント:** ウィンドウ関数 `MAX(...) OVER (ORDER BY ... ROWS BETWEEN UNBOUNDED PRECEDING AND 1 PRECEDING)` を使用して、現在の行より前の最大値を取得します。

**回答:**

```sql
WITH CustomerPurchaseHistoryLagged AS (
  SELECT
    c.customer_id,
    c.first_name || ' ' || c.last_name AS customer_name,
    i.invoice_id,
    i.invoice_date,
    i.total,
    MAX(i.total) OVER (PARTITION BY c.customer_id ORDER BY i.invoice_date, i.invoice_id ROWS BETWEEN UNBOUNDED PRECEDING AND 1 PRECEDING) AS previous_max_total
  FROM customer AS c
  JOIN invoice AS i
    ON c.customer_id = i.customer_id
)
SELECT
  customer_name,
  COUNT(invoice_id) AS purchases_exceeding_previous_max
FROM CustomerPurchaseHistoryLagged
WHERE
  total > previous_max_total
GROUP BY
  customer_id,
  customer_name
ORDER BY
  purchases_exceeding_previous_max DESC;
```

---

### 問題 42: 各ジャンルに属するアルバムの総数と、そのジャンルに属するトラックの平均ミリ秒を取得してください。

**回答:**

```sql
SELECT
  g.name AS genre_name,
  COUNT(DISTINCT a.album_id) AS total_albums_in_genre,
  AVG(t.milliseconds) AS average_track_length_ms
FROM genre AS g
LEFT JOIN track AS t
  ON g.genre_id = t.genre_id
LEFT JOIN album AS a
  ON t.album_id = a.album_id
GROUP BY
  g.genre_id,
  g.name
ORDER BY
  genre_name;
```

---

### 問題 43: 2020 年、2021 年、2022 年の各年に最も売上が高かった国と、その国の売上合計をそれぞれ取得してください。

**回答:**

```sql
WITH YearlyCountrySales AS (
  SELECT
    EXTRACT(YEAR FROM invoice_date) AS sales_year,
    billing_country AS country,
    SUM(total) AS total_country_sales,
    ROW_NUMBER() OVER (PARTITION BY EXTRACT(YEAR FROM invoice_date) ORDER BY SUM(total) DESC) AS rn
  FROM invoice
  WHERE
    EXTRACT(YEAR FROM invoice_date) IN (2020, 2021, 2022)
  GROUP BY
    sales_year,
    billing_country
)
SELECT
  sales_year,
  country AS top_sales_country,
  total_country_sales
FROM YearlyCountrySales
WHERE
  rn = 1
ORDER BY
  sales_year;
```

---

### 問題 44: 全ての顧客が少なくとも一度は購入したことがあるトラックの名前を取得してください。

**ヒント:** `INTERSECT` を使用し、各顧客が購入したトラックの集合を結合します。または、`GROUP BY` と `HAVING COUNT(DISTINCT customer_id)` を使用して、全顧客数を満たすトラックを特定します。

**回答:**

```sql
-- 各顧客が購入したトラックの集合を準備
WITH CustomerTracks AS (
  SELECT DISTINCT
    c.customer_id,
    t.track_id
  FROM customer AS c
  JOIN invoice AS i
    ON c.customer_id = i.customer_id
  JOIN invoice_line AS il
    ON i.invoice_id = il.invoice_id
  JOIN track AS t
    ON il.track_id = t.track_id
)
-- 全顧客数を取得
, TotalCustomers AS (
  SELECT
    COUNT(DISTINCT customer_id) AS total_customers_count
  FROM customer
)
-- 全ての顧客に購入されたトラックを特定
SELECT
  t.name AS track_name
FROM track AS t
JOIN invoice_line AS il
  ON t.track_id = il.track_id
JOIN invoice AS i
  ON il.invoice_id = i.invoice_id
GROUP BY
  t.track_id,
  t.name
HAVING
  COUNT(DISTINCT i.customer_id) = (SELECT total_customers_count FROM TotalCustomers)
ORDER BY
  track_name;
```

---

### 問題 45: 各メディアタイプにおいて、最も多くのトラックが属するジャンルと、そのジャンルに属するトラック数を取得してください。

**回答:**

```sql
WITH MediaTypeGenreTrackCount AS (
  SELECT
    mt.name AS media_type_name,
    g.name AS genre_name,
    COUNT(t.track_id) AS track_count,
    ROW_NUMBER() OVER (PARTITION BY mt.media_type_id ORDER BY COUNT(t.track_id) DESC) AS rn
  FROM media_type AS mt
  JOIN track AS t
    ON mt.media_type_id = t.media_type_id
  JOIN genre AS g
    ON t.genre_id = g.genre_id
  GROUP BY
    mt.media_type_id,
    mt.name,
    g.name
)
SELECT
  media_type_name,
  genre_name AS most_common_genre,
  track_count
FROM MediaTypeGenreTrackCount
WHERE
  rn = 1
ORDER BY
  media_type_name;
```

---

### 問題 46: 各顧客の購入総額が、全体の上位 10%または下位 10%に入る顧客の氏名と購入総額を取得してください。

**ヒント:** `NTILE()` ウィンドウ関数を使用します。

**回答:**

```sql
WITH CustomerTotalSales AS (
  SELECT
    c.customer_id,
    c.first_name || ' ' || c.last_name AS customer_name,
    SUM(i.total) AS total_purchase_amount
  FROM customer AS c
  JOIN invoice AS i
    ON c.customer_id = i.customer_id
  GROUP BY
    c.customer_id,
    customer_name
), RankedCustomerSales AS (
  SELECT
    customer_name,
    total_purchase_amount,
    NTILE(10) OVER (ORDER BY total_purchase_amount DESC) AS sales_decile_desc, -- 降順で1が上位10%
    NTILE(10) OVER (ORDER BY total_purchase_amount ASC) AS sales_decile_asc -- 昇順で1が下位10%
  FROM CustomerTotalSales
)
SELECT
  customer_name,
  total_purchase_amount,
  CASE
    WHEN sales_decile_desc = 1 THEN 'Top 10%'
    WHEN sales_decile_asc = 1 THEN 'Bottom 10%'
    ELSE 'Middle'
  END AS sales_category
FROM RankedCustomerSales
WHERE
  sales_decile_desc = 1 OR sales_decile_asc = 1
ORDER BY
  total_purchase_amount DESC;
```

---

### 問題 47: 各従業員について、彼らがサポートする顧客の中で最も購入回数が多い顧客と、その購入回数を取得してください。

**回答:**

```sql
WITH EmployeeCustomerPurchaseCount AS (
  SELECT
    e.employee_id,
    e.first_name || ' ' || e.last_name AS support_rep_name,
    c.customer_id,
    c.first_name || ' ' || c.last_name AS customer_name,
    COUNT(i.invoice_id) AS purchase_count,
    ROW_NUMBER() OVER (PARTITION BY e.employee_id ORDER BY COUNT(i.invoice_id) DESC) AS rn
  FROM employee AS e
  JOIN customer AS c
    ON e.employee_id = c.support_rep_id
  JOIN invoice AS i
    ON c.customer_id = i.customer_id
  GROUP BY
    e.employee_id,
    support_rep_name,
    c.customer_id,
    customer_name
)
SELECT
  support_rep_name,
  customer_name AS most_purchasing_customer,
  purchase_count
FROM EmployeeCustomerPurchaseCount
WHERE
  rn = 1
ORDER BY
  support_rep_name;
```

---

### 問題 48: 顧客が過去の購入で最も多く使用した請求国（billing_country）と、その国の購入回数を取得してください。

**回答:**

```sql
WITH CustomerBillingCountryCount AS (
  SELECT
    c.customer_id,
    c.first_name || ' ' || c.last_name AS customer_name,
    i.billing_country,
    COUNT(i.invoice_id) AS country_invoice_count,
    ROW_NUMBER() OVER (PARTITION BY c.customer_id ORDER BY COUNT(i.invoice_id) DESC) AS rn
  FROM customer AS c
  JOIN invoice AS i
    ON c.customer_id = i.customer_id
  GROUP BY
    c.customer_id,
    customer_name,
    i.billing_country
)
SELECT
  customer_name,
  billing_country AS most_frequent_billing_country,
  country_invoice_count
FROM CustomerBillingCountryCount
WHERE
  rn = 1
ORDER BY
  customer_name;
```

---

### 問題 49: 各アルバムのリリース年（架空で、最初のトラックの購入日を代用）と、そのアルバムの平均単価（全トラックの単価の平均）を取得してください。

**ヒント:** `MIN(invoice_date)` を使用して「リリース年」を近似します。

**回答:**

```sql
SELECT
  a.title AS album_title,
  MIN(EXTRACT(YEAR FROM i.invoice_date)) AS approximated_release_year,
  AVG(t.unit_price) AS average_track_unit_price
FROM album AS a
JOIN track AS t
  ON a.album_id = t.album_id
LEFT JOIN invoice_line AS il
  ON t.track_id = il.track_id
LEFT JOIN invoice AS i
  ON il.invoice_id = i.invoice_id
GROUP BY
  a.album_id,
  a.title
ORDER BY
  album_title;
```

**補足:** `album` テーブルにリリース年情報がないため、ここではそのアルバムに含まれる**最も古いトラックの購入日**を「近似リリース年」としています。購入がないトラックやアルバムは年が表示されません。

---

承知いたしました。問題 50 を再度出力し、さらに 10 問（問題 51〜60）を追加で作成します。

---

### 問題 50: 顧客と従業員の両方で、同じ「city」に住んでいる人々の氏名と居住都市を結合して取得してください。

**ヒント:** `INTERSECT` を使用して共通の都市に住む人々を特定します。

**回答:**

```sql
-- 顧客で特定の都市に住む人
SELECT
  first_name || ' ' || last_name AS person_name,
  city AS resident_city,
  'Customer' AS person_type
FROM customer
WHERE
  city IS NOT NULL
INTERSECT
-- 従業員で特定の都市に住む人
SELECT
  first_name || ' ' || last_name AS person_name,
  city AS resident_city,
  'Employee' AS person_type
FROM employee
WHERE
  city IS NOT NULL
ORDER BY
  resident_city,
  person_name;
```

---

### 問題 51: 各アーティストが作曲したトラックの総数と、それらのトラックが属するアルバムの平均トラック数を取得してください。

**回答:**

```sql
SELECT
  ar.name AS artist_name,
  COUNT(t.track_id) AS total_tracks_by_artist,
  AVG(album_track_counts.track_count_per_album) AS avg_tracks_per_album
FROM artist AS ar
LEFT JOIN album AS al
  ON ar.artist_id = al.artist_id
LEFT JOIN track AS t
  ON al.album_id = t.album_id
LEFT JOIN (
  SELECT
    album_id,
    COUNT(track_id) AS track_count_per_album
  FROM track
  GROUP BY
    album_id
) AS album_track_counts
  ON al.album_id = album_track_counts.album_id
GROUP BY
  ar.artist_id,
  ar.name
ORDER BY
  artist_name;
```

---

### 問題 52: 各顧客の最初の購入から 2 回目の購入までの期間（日数）を取得してください。2 回以上購入している顧客のみを対象とします。

**ヒント:** ウィンドウ関数 `LEAD()` または `LAG()` を使用して前後の購入日を比較します。

**回答:**

```sql
WITH CustomerPurchaseDates AS (
  SELECT
    customer_id,
    invoice_date,
    ROW_NUMBER() OVER (PARTITION BY customer_id ORDER BY invoice_date) AS rn
  FROM invoice
)
SELECT
  c.first_name || ' ' || c.last_name AS customer_name,
  cpd1.invoice_date AS first_purchase_date,
  cpd2.invoice_date AS second_purchase_date,
  (cpd2.invoice_date - cpd1.invoice_date) AS days_between_first_and_second_purchase
FROM customer AS c
JOIN CustomerPurchaseDates AS cpd1
  ON c.customer_id = cpd1.customer_id
  AND cpd1.rn = 1
JOIN CustomerPurchaseDates AS cpd2
  ON c.customer_id = cpd2.customer_id
  AND cpd2.rn = 2
ORDER BY
  customer_name;
```

---

### 問題 53: 各顧客の総購入金額が、その顧客の居住国における購入金額の合計の上位 25%に入る顧客の氏名、国、購入総額を取得してください。

**ヒント:** `NTILE()` ウィンドウ関数とサブクエリを組み合わせます。

**回答:**

```sql
WITH CustomerTotalSalesByCountry AS (
  SELECT
    c.customer_id,
    c.first_name || ' ' || c.last_name AS customer_name,
    c.country,
    SUM(i.total) AS total_purchase_amount
  FROM customer AS c
  JOIN invoice AS i
    ON c.customer_id = i.customer_id
  GROUP BY
    c.customer_id,
    customer_name,
    c.country
), RankedCustomerSalesByCountry AS (
  SELECT
    customer_name,
    country,
    total_purchase_amount,
    NTILE(4) OVER (PARTITION BY country ORDER BY total_purchase_amount DESC) AS sales_quartile_in_country
  FROM CustomerTotalSalesByCountry
)
SELECT
  customer_name,
  country,
  total_purchase_amount
FROM RankedCustomerSalesByCountry
WHERE
  sales_quartile_in_country = 1
ORDER BY
  country,
  total_purchase_amount DESC;
```

---

### 問題 54: 全てのトラックを対象に、そのトラックの単価がそのジャンルの平均単価と比較して、どれだけ高いか（または低いか）をパーセンテージで示してください。

**回答:**

```sql
SELECT
  t.name AS track_name,
  g.name AS genre_name,
  t.unit_price,
  AVG(t.unit_price) OVER (PARTITION BY g.genre_id) AS avg_genre_unit_price,
  ((t.unit_price - AVG(t.unit_price) OVER (PARTITION BY g.genre_id)) / AVG(t.unit_price) OVER (PARTITION BY g.genre_id)) * 100 AS percentage_difference_from_genre_avg
FROM track AS t
JOIN genre AS g
  ON t.genre_id = g.genre_id
ORDER BY
  g.name,
  percentage_difference_from_genre_avg DESC;
```

---

### 問題 55: 各プレイリストに含まれるトラックの中で、最も長いトラックの名称と、最も短いトラックの名称を取得してください。

**回答:**

```sql
WITH PlaylistTrackLengths AS (
  SELECT
    p.name AS playlist_name,
    t.name AS track_name,
    t.milliseconds,
    ROW_NUMBER() OVER (PARTITION BY p.playlist_id ORDER BY t.milliseconds DESC) AS rn_longest,
    ROW_NUMBER() OVER (PARTITION BY p.playlist_id ORDER BY t.milliseconds ASC) AS rn_shortest
  FROM playlist AS p
  JOIN playlist_track AS pt
    ON p.playlist_id = pt.playlist_id
  JOIN track AS t
    ON pt.track_id = t.track_id
)
SELECT
  pltl_longest.playlist_name,
  pltl_longest.track_name AS longest_track_name,
  pltl_longest.milliseconds AS longest_track_length_ms,
  pltl_shortest.track_name AS shortest_track_name,
  pltl_shortest.milliseconds AS shortest_track_length_ms
FROM PlaylistTrackLengths AS pltl_longest
JOIN PlaylistTrackLengths AS pltl_shortest
  ON pltl_longest.playlist_name = pltl_shortest.playlist_name
WHERE
  pltl_longest.rn_longest = 1 AND pltl_shortest.rn_shortest = 1
ORDER BY
  pltl_longest.playlist_name;
```

---

### 問題 56: 各国の顧客の売上合計が、全世界の平均顧客売上合計よりも高い国とその売上合計、および全世界の平均顧客売上合計を取得してください。

**回答:**

```sql
WITH CountryCustomerSales AS (
  SELECT
    c.country,
    SUM(i.total) AS total_country_sales
  FROM customer AS c
  JOIN invoice AS i
    ON c.customer_id = i.customer_id
  GROUP BY
    c.country
), GlobalAvgCustomerSales AS (
  SELECT
    AVG(total_customer_sales) AS overall_avg_customer_sales
  FROM (
    SELECT
      customer_id,
      SUM(total) AS total_customer_sales
    FROM invoice
    GROUP BY
      customer_id
  ) AS individual_customer_sales
)
SELECT
  ccs.country,
  ccs.total_country_sales,
  gacs.overall_avg_customer_sales
FROM CountryCustomerSales AS ccs, GlobalAvgCustomerSales AS gacs
WHERE
  ccs.total_country_sales > gacs.overall_avg_customer_sales
ORDER BY
  ccs.total_country_sales DESC;

-- 別パターン (ウィンドウ関数を使用)
SELECT
  country,
  SUM(total_purchase_amount) AS total_country_sales,
  AVG(SUM(total_purchase_amount)) OVER () AS overall_avg_customer_sales
FROM (
  SELECT
    c.country,
    c.customer_id,
    SUM(i.total) AS total_purchase_amount
  FROM customer AS c
  JOIN invoice AS i
    ON c.customer_id = i.customer_id
  GROUP BY
    c.country,
    c.customer_id
) AS customer_country_sales_detail
GROUP BY
  country
HAVING
  SUM(total_purchase_amount) > AVG(SUM(total_purchase_amount)) OVER ()
ORDER BY
  total_country_sales DESC;
```

---

### 問題 57: 各従業員がサポートする顧客の中で、最も年齢が若い顧客と最も年齢が高い顧客の氏名と生年月日を取得してください。

**ヒント:** 顧客テーブルには生年月日がないため、ここでは「Customer ID」が最も小さい/大きい顧客を「若い/年上」と仮定します。または、より現実的なデータとして、購買日を基準に「最新の購買顧客」と「最も古い購買顧客」を「若い/年上」と見なすこともできます。ここでは後者のアプローチで「最新の購入顧客」と「最も古い購入顧客」と解釈します。

**回答:**

```sql
WITH EmployeeCustomerFirstLastPurchase AS (
  SELECT
    e.employee_id,
    e.first_name || ' ' || e.last_name AS support_rep_name,
    c.customer_id,
    c.first_name || ' ' || c.last_name AS customer_name,
    MIN(i.invoice_date) AS first_purchase_date,
    MAX(i.invoice_date) AS last_purchase_date,
    ROW_NUMBER() OVER (PARTITION BY e.employee_id ORDER BY MIN(i.invoice_date) ASC) AS rn_oldest_buyer,
    ROW_NUMBER() OVER (PARTITION BY e.employee_id ORDER BY MAX(i.invoice_date) DESC) AS rn_newest_buyer
  FROM employee AS e
  JOIN customer AS c
    ON e.employee_id = c.support_rep_id
  JOIN invoice AS i
    ON c.customer_id = i.customer_id
  GROUP BY
    e.employee_id,
    support_rep_name,
    c.customer_id,
    customer_name
)
SELECT
  eclp_oldest.support_rep_name,
  eclp_oldest.customer_name AS oldest_customer_name,
  eclp_oldest.first_purchase_date AS oldest_customer_first_purchase,
  eclp_newest.customer_name AS newest_customer_name,
  eclp_newest.last_purchase_date AS newest_customer_last_purchase
FROM EmployeeCustomerFirstLastPurchase AS eclp_oldest
JOIN EmployeeCustomerFirstLastPurchase AS eclp_newest
  ON eclp_oldest.employee_id = eclp_newest.employee_id
WHERE
  eclp_oldest.rn_oldest_buyer = 1 AND eclp_newest.rn_newest_buyer = 1
ORDER BY
  eclp_oldest.support_rep_name;
```

---

### 問題 58: 各トラックについて、それが最も多く購入された国と、その国での購入回数を取得してください。

**回答:**

```sql
WITH TrackCountryPurchaseCount AS (
  SELECT
    t.track_id,
    t.name AS track_name,
    i.billing_country AS country,
    COUNT(il.invoice_line_id) AS purchase_count,
    ROW_NUMBER() OVER (PARTITION BY t.track_id ORDER BY COUNT(il.invoice_line_id) DESC) AS rn
  FROM track AS t
  JOIN invoice_line AS il
    ON t.track_id = il.track_id
  JOIN invoice AS i
    ON il.invoice_id = i.invoice_id
  GROUP BY
    t.track_id,
    t.name,
    i.billing_country
)
SELECT
  track_name,
  country AS most_purchased_country,
  purchase_count
FROM TrackCountryPurchaseCount
WHERE
  rn = 1
ORDER BY
  track_name;
```

---

### 問題 59: 各顧客の購入総額が、その顧客の担当サポート担当者の**直属の上司**が管理する全顧客の平均購入金額よりも高い顧客の氏名、総金額、担当サポート担当者名、および上司が管理する顧客の平均購入金額を取得してください。

**回答:**

```sql
WITH CustomerTotalSales AS (
  SELECT
    c.customer_id,
    c.first_name || ' ' || c.last_name AS customer_name,
    c.support_rep_id,
    SUM(i.total) AS total_customer_sales
  FROM customer AS c
  JOIN invoice AS i
    ON c.customer_id = i.customer_id
  GROUP BY
    c.customer_id,
    customer_name,
    c.support_rep_id
), ManagerManagedCustomers AS (
  SELECT
    e_manager.employee_id AS manager_id,
    e_manager.first_name || ' ' || e_manager.last_name AS manager_name,
    e_rep.employee_id AS rep_id,
    c.customer_id
  FROM employee AS e_manager
  JOIN employee AS e_rep
    ON e_manager.employee_id = e_rep.reports_to
  JOIN customer AS c
    ON e_rep.employee_id = c.support_rep_id
), ManagerAvgSales AS (
  SELECT
    mmc.manager_id,
    mmc.manager_name,
    AVG(cts.total_customer_sales) AS avg_manager_managed_customer_sales
  FROM ManagerManagedCustomers AS mmc
  JOIN CustomerTotalSales AS cts
    ON mmc.customer_id = cts.customer_id
  GROUP BY
    mmc.manager_id,
    mmc.manager_name
)
SELECT
  cts.customer_name,
  cts.total_customer_sales,
  e_rep.first_name || ' ' || e_rep.last_name AS support_rep_name,
  mas.manager_name,
  mas.avg_manager_managed_customer_sales
FROM CustomerTotalSales AS cts
JOIN employee AS e_rep
  ON cts.support_rep_id = e_rep.employee_id
JOIN employee AS e_manager
  ON e_rep.reports_to = e_manager.employee_id
JOIN ManagerAvgSales AS mas
  ON e_manager.employee_id = mas.manager_id
WHERE
  cts.total_customer_sales > mas.avg_manager_managed_customer_sales
ORDER BY
  cts.total_customer_sales DESC;
```

---

### 問題 60: 各年ごとの総売上が最も高い月と、その月の売上金額、そしてその年の総売上に対する比率を取得してください。

**ヒント:** `EXTRACT(YEAR FROM date)` と `EXTRACT(MONTH FROM date)` を使用し、ウィンドウ関数で月ごとの売上と年ごとの総売上を計算します。

**回答:**

```sql
WITH MonthlySales AS (
  SELECT
    EXTRACT(YEAR FROM invoice_date) AS sales_year,
    EXTRACT(MONTH FROM invoice_date) AS sales_month,
    SUM(total) AS monthly_total_sales
  FROM invoice
  GROUP BY
    sales_year,
    sales_month
), YearlySales AS (
  SELECT
    sales_year,
    SUM(monthly_total_sales) AS annual_total_sales
  FROM MonthlySales
  GROUP BY
    sales_year
), RankedMonthlySales AS (
  SELECT
    ms.sales_year,
    ms.sales_month,
    ms.monthly_total_sales,
    ys.annual_total_sales,
    ROW_NUMBER() OVER (PARTITION BY ms.sales_year ORDER BY ms.monthly_total_sales DESC) AS rn
  FROM MonthlySales AS ms
  JOIN YearlySales AS ys
    ON ms.sales_year = ys.sales_year
)
SELECT
  sales_year,
  sales_month AS top_sales_month,
  monthly_total_sales AS top_month_sales_amount,
  (monthly_total_sales / annual_total_sales) * 100 AS percentage_of_annual_sales
FROM RankedMonthlySales
WHERE
  rn = 1
ORDER BY
  sales_year;
```
