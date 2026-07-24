# iTunes(Chinook) 発展 50 問 — 問題と解答

> **対象**：講義資料 `00`〜`10`（サブクエリ）まで学習した人向け。基本問題を早く解き終えた人が、**複数の技法を組み合わせて**解く練習をするための発展問題集です。
>
> **使う技法（講義 00〜10 の範囲）**
> - 結合（内部結合・外部結合・自己結合）… `08`
> - 集約関数・`GROUP BY`・`HAVING`… `07`
> - `CASE` 式 … `06`
> - 関数（文字列・数値・日付）… `04`
> - 集合演算子（`UNION` / `INTERSECT` / `EXCEPT`）… `09`
> - サブクエリ（スカラ・`IN`・派生表・相関サブクエリ・`EXISTS`）… `10`
>
> **あえて使っていない技法**：ウィンドウ関数（`OVER()` / `ROW_NUMBER()` / `LAG()` など）と CTE（`WITH`）は範囲外なので、解答では**一切使っていません**。同じ「グループ内トップ」も相関サブクエリや派生表で書くのが本問題集の狙いです。
>
> **データについて知っておくこと（重要）**
> - `track.unit_price`（曲の単価）は **音声 = 0.99 ドル / 動画 = 1.99 ドルの 2 種類だけ**です。「高い曲・安い曲」を問う問題は基本的にこの 2 値の話になります。
> - 請求データ（`invoice`）は **2021 年〜2025 年**の 5 年分です。「最近」を表すときは `CURRENT_DATE` ではなく年をリテラルで指定します。
> - 主な国は USA・Canada・France・Brazil・Germany など。従業員（`employee`）には `reports_to` による上司—部下の階層があります。顧客の担当者は `title = 'Sales Support Agent'` の従業員（`customer.support_rep_id`）です。
>
> 各問には **狙い（どの技法を使うか）** と **期待される結果のイメージ** を添えています。解答は一例で、他の書き方も正解になり得ます。

---

## Part 1. 結合の総合練習（問題 1〜8）

### 問題 1: ジャンルが Jazz の曲について、曲名・アルバム名・アーティスト名・ジャンル名・メディアタイプ名を 1 行にまとめて表示してください。曲名の昇順で。

**狙い**：track を中心に album → artist、genre、media_type を結ぶ 5 テーブル結合。

**解答:**

```sql
SELECT
  t.name          AS track_name,
  al.title        AS album_title,
  ar.name         AS artist_name,
  g.name          AS genre_name,
  mt.name         AS media_type_name
FROM track AS t
JOIN album AS al       ON t.album_id = al.album_id
JOIN artist AS ar      ON al.artist_id = ar.artist_id
JOIN genre AS g        ON t.genre_id = g.genre_id
JOIN media_type AS mt  ON t.media_type_id = mt.media_type_id
WHERE g.name = 'Jazz'
ORDER BY t.name;
```

**期待される結果**：Jazz の曲が、どのアルバム・誰の演奏・どの形式かまで 1 行で見渡せます。「1 曲を説明するのに何個のテーブルを繋ぐ必要があるか」を体感するのが目的。

---

### 問題 2: アルバムを 1 枚も登録していないアーティストの名前を全て取得してください。

**狙い**：外部結合 + `IS NULL` で「関連が無い側」を炙り出す。

**解答:**

```sql
SELECT ar.name AS artist_name
FROM artist AS ar
LEFT JOIN album AS al ON ar.artist_id = al.artist_id
WHERE al.album_id IS NULL
ORDER BY ar.name;
```

**期待される結果**：アーティスト登録はあるがアルバムが未登録の人だけが並びます。`LEFT JOIN` して結合相手が `NULL` の行＝「相手がいない行」という定番パターン。

---

### 問題 3: 一度も購入されたことがないトラックの曲数と、そのうちの何曲かの曲名（曲名昇順で先頭 20 件）を確認してください。

**狙い**：`invoice_line` に存在しないトラック＝外部結合の `IS NULL`。

**解答:**

```sql
-- まず件数を確認
SELECT COUNT(*) AS never_sold_count
FROM track AS t
LEFT JOIN invoice_line AS il ON t.track_id = il.track_id
WHERE il.track_id IS NULL;

-- 具体的な曲名（先頭20件）
SELECT t.name AS track_name
FROM track AS t
LEFT JOIN invoice_line AS il ON t.track_id = il.track_id
WHERE il.track_id IS NULL
ORDER BY t.name
LIMIT 20;
```

**期待される結果**：カタログには載っているが売れていない「死蔵在庫」の曲が見えます。全 3,500 曲のうちかなりの数が未購入で、「登録＝売れている、ではない」ことが分かる気持ちのよい結果。

---

### 問題 4: 全従業員について、氏名とその直属の上司の氏名を表示してください。上司がいない人（社長）は上司欄を空欄扱いにします。

**狙い**：同一テーブルの自己結合。上司なしも残すため外部結合。

**解答:**

```sql
SELECT
  e.first_name || ' ' || e.last_name AS employee_name,
  e.title,
  boss.first_name || ' ' || boss.last_name AS manager_name
FROM employee AS e
LEFT JOIN employee AS boss ON e.reports_to = boss.employee_id
ORDER BY e.employee_id;
```

**期待される結果**：Andrew Adams（General Manager）の上司が空欄、他の全員に上司名が付きます。`employee` を 2 回名前を変えて登場させる自己結合の基本。

---

### 問題 5: 顧客担当者（`title = 'Sales Support Agent'` の従業員）ごとに、担当している顧客数を多い順に表示してください。

**狙い**：従業員↔顧客の結合と `GROUP BY`。対象を担当者だけに絞る。

**解答:**

```sql
SELECT
  e.first_name || ' ' || e.last_name AS support_rep,
  COUNT(c.customer_id) AS customer_count
FROM employee AS e
JOIN customer AS c ON c.support_rep_id = e.employee_id
WHERE e.title = 'Sales Support Agent'
GROUP BY e.employee_id, support_rep
ORDER BY customer_count DESC;
```

**期待される結果**：Jane・Margaret・Steve の 3 名に顧客がほぼ均等に割り振られている様子が見えます。

---

### 問題 6: 各顧客の氏名・国と、その顧客の担当者名を一覧にしてください。担当者未設定の顧客も表示します。国→顧客名の順で並べてください。

**狙い**：外部結合で「担当者がいない顧客も落とさない」。

**解答:**

```sql
SELECT
  c.first_name || ' ' || c.last_name AS customer_name,
  c.country,
  e.first_name || ' ' || e.last_name AS support_rep
FROM customer AS c
LEFT JOIN employee AS e ON c.support_rep_id = e.employee_id
ORDER BY c.country, customer_name;
```

**期待される結果**：国ごとに顧客と担当者の対応が並びます。`JOIN` にすると担当者未設定の顧客が消えてしまう点を `LEFT JOIN` で回避するのが狙い。

---

### 問題 7: 各プレイリストについて、収録されている曲数を多い順に表示してください。1 曲も入っていないプレイリストは 0 と表示します。

**狙い**：多対多の中間表 `playlist_track` を外部結合し `COUNT`。

**解答:**

```sql
SELECT
  p.name AS playlist_name,
  COUNT(pt.track_id) AS track_count
FROM playlist AS p
LEFT JOIN playlist_track AS pt ON p.playlist_id = pt.playlist_id
GROUP BY p.playlist_id, p.name
ORDER BY track_count DESC, playlist_name;
```

**期待される結果**：`Music` が突出して多く、`Movies` や `Audiobooks` は少ない、空のプレイリストは 0 と出ます。`COUNT(*)` ではなく `COUNT(pt.track_id)` にすることで「空でも 0 行にせず 0 件と数える」のがポイント。

---

### 問題 8: アーティスト "Iron Maiden" の全アルバムについて、アルバム名と収録曲数を、曲数の多い順に表示してください。

**狙い**：特定アーティストに絞った 3 テーブル結合と集計。

**解答:**

```sql
SELECT
  al.title AS album_title,
  COUNT(t.track_id) AS track_count
FROM artist AS ar
JOIN album AS al ON ar.artist_id = al.artist_id
LEFT JOIN track AS t ON al.album_id = t.album_id
WHERE ar.name = 'Iron Maiden'
GROUP BY al.album_id, al.title
ORDER BY track_count DESC, album_title;
```

**期待される結果**：Iron Maiden は Chinook で最もアルバム数が多いアーティストの一人なので、多数のアルバムがずらりと並ぶ気持ちのよい結果になります。

---

## Part 2. 集約・GROUP BY・HAVING の応用（問題 9〜16）

### 問題 9: ジャンルごとに、曲数・平均再生時間（分, 小数第 1 位）・最長曲の再生時間（分）を求め、曲数の多い順に表示してください。

**狙い**：`COUNT` / `AVG` / `MAX` の同時集計と単位変換（ミリ秒→分）。

**解答:**

```sql
SELECT
  g.name AS genre_name,
  COUNT(t.track_id) AS track_count,
  ROUND(AVG(t.milliseconds) / 60000.0, 1) AS avg_minutes,
  ROUND(MAX(t.milliseconds) / 60000.0, 1) AS max_minutes
FROM genre AS g
JOIN track AS t ON g.genre_id = t.genre_id
GROUP BY g.genre_id, g.name
ORDER BY track_count DESC;
```

**期待される結果**：Rock が圧倒的に曲数が多く、`TV Shows` などの映像ジャンルは平均・最長ともに桁違いに長い（数十分）ことが一目で分かります。ミリ秒を 60000 で割って分に直すのがコツ。

---

### 問題 10: 国ごとに、顧客数・請求件数・総売上（小数第 2 位）を求め、総売上の多い順に表示してください。

**狙い**：3 テーブル結合 + 複数集計。`COUNT(DISTINCT ...)` の使い分け。

**解答:**

```sql
SELECT
  c.country,
  COUNT(DISTINCT c.customer_id) AS customer_count,
  COUNT(i.invoice_id) AS invoice_count,
  ROUND(SUM(i.total), 2) AS total_sales
FROM customer AS c
JOIN invoice AS i ON c.customer_id = i.customer_id
GROUP BY c.country
ORDER BY total_sales DESC;
```

**期待される結果**：USA が売上・顧客数ともにトップ、Canada・France が続きます。顧客数は `DISTINCT` を付けないと請求件数の分だけ重複してしまう点が学びどころ。

---

### 問題 11: 年（2021〜2025）ごとに、総売上・請求件数・請求 1 件あたりの平均額を求めてください。年の昇順で。

**狙い**：`EXTRACT(YEAR FROM ...)` で日付から年を取り出して集計。

**解答:**

```sql
SELECT
  EXTRACT(YEAR FROM i.invoice_date) AS sales_year,
  ROUND(SUM(i.total), 2) AS total_sales,
  COUNT(*) AS invoice_count,
  ROUND(AVG(i.total), 2) AS avg_invoice
FROM invoice AS i
GROUP BY sales_year
ORDER BY sales_year;
```

**期待される結果**：5 年分の売上推移がきれいに並びます。`CURRENT_DATE` を使わず年をそのまま集計軸にするのがこのデータでの正解。

---

### 問題 12: 収録時間の合計が長いアルバム上位 10 枚を、アルバム名・曲数・合計再生時間（分）で表示してください。

**狙い**：`SUM` と `LIMIT` によるランキング。

**解答:**

```sql
SELECT
  al.title AS album_title,
  COUNT(t.track_id) AS track_count,
  ROUND(SUM(t.milliseconds) / 60000.0, 1) AS total_minutes
FROM album AS al
JOIN track AS t ON al.album_id = t.album_id
GROUP BY al.album_id, al.title
ORDER BY total_minutes DESC
LIMIT 10;
```

**期待される結果**：TV ドラマや長編もののアルバムが上位に来て、合計数百分という結果になります。

---

### 問題 13: 2 曲以上を手がけている作曲家（`composer`）を、担当曲数の多い順に上位 15 件表示してください。作曲者未設定（NULL）は除外します。

**狙い**：`GROUP BY` + `HAVING` で「一定数以上」を絞る。`NULL` の除外。

**解答:**

```sql
SELECT
  t.composer,
  COUNT(*) AS track_count
FROM track AS t
WHERE t.composer IS NOT NULL
GROUP BY t.composer
HAVING COUNT(*) >= 2
ORDER BY track_count DESC
LIMIT 15;
```

**期待される結果**：Steve Harris（Iron Maiden）など多作な作曲家が上位に。`WHERE`（集計前の絞り込み）と `HAVING`（集計後の絞り込み）の役割の違いを確認できます。

---

### 問題 14: 10 曲以上を収録しているアルバムだけを、曲数の多い順に表示してください。

**狙い**：`HAVING COUNT(...) >= 10`。

**解答:**

```sql
SELECT
  al.title AS album_title,
  COUNT(t.track_id) AS track_count
FROM album AS al
JOIN track AS t ON al.album_id = t.album_id
GROUP BY al.album_id, al.title
HAVING COUNT(t.track_id) >= 10
ORDER BY track_count DESC, album_title;
```

**期待される結果**：ベスト盤や 2 枚組など、大ボリュームのアルバムだけが残ります。

---

### 問題 15: メディアタイプごとに、曲数・単価の種類（最小と最大）・平均単価を表示してください。

**狙い**：単価が 0.99 / 1.99 の 2 種類しかないという事実を集計で確認する。

**解答:**

```sql
SELECT
  mt.name AS media_type,
  COUNT(t.track_id) AS track_count,
  MIN(t.unit_price) AS min_price,
  MAX(t.unit_price) AS max_price,
  ROUND(AVG(t.unit_price), 2) AS avg_price
FROM media_type AS mt
JOIN track AS t ON mt.media_type_id = t.media_type_id
GROUP BY mt.media_type_id, mt.name
ORDER BY track_count DESC;
```

**期待される結果**：音声系メディアは `min = max = 0.99`、映像系（`Protected MPEG-4 video file`）は `1.99` に揃っている、と一目で分かります。「価格は 2 種類だけ」を自分の目で確かめる問題。

---

### 問題 16: 総購入額が多い顧客の上位 10 名を、氏名・国・購入回数・総購入額・請求 1 件あたり平均額で表示してください。

**狙い**：顧客単位の複数集計とランキング。

**解答:**

```sql
SELECT
  c.first_name || ' ' || c.last_name AS customer_name,
  c.country,
  COUNT(i.invoice_id) AS invoice_count,
  ROUND(SUM(i.total), 2) AS total_spent,
  ROUND(AVG(i.total), 2) AS avg_invoice
FROM customer AS c
JOIN invoice AS i ON c.customer_id = i.customer_id
GROUP BY c.customer_id, customer_name, c.country
ORDER BY total_spent DESC
LIMIT 10;
```

**期待される結果**：優良顧客トップ 10 がずらりと並びます。「誰を大事にすべきか」が見える実務的な結果。

---

## Part 3. CASE 式の応用（問題 17〜22）

### 問題 17: 全トラックを再生時間で「短尺（3 分未満）」「標準（3〜5 分）」「長尺（5 分超）」の 3 区分に分け、それぞれの曲数を表示してください。

**狙い**：`CASE` でカテゴリを作り、そのカテゴリで `GROUP BY`。

**解答:**

```sql
SELECT
  CASE
    WHEN t.milliseconds < 180000 THEN '1_短尺(3分未満)'
    WHEN t.milliseconds <= 300000 THEN '2_標準(3〜5分)'
    ELSE '3_長尺(5分超)'
  END AS length_category,
  COUNT(*) AS track_count
FROM track AS t
GROUP BY length_category
ORDER BY length_category;
```

**期待される結果**：大多数が「標準」に入り、映像系が「長尺」に固まります。並び順を安定させるためラベル先頭に番号を付けています。

---

### 問題 18: 曲を単価で「音声(0.99)」「動画(1.99)」に分類し、区分ごとの曲数・売上（`invoice_line` 実績）を表示してください。

**狙い**：`CASE` による価格区分ラベル付けと、購入実績の集計を結合。

**解答:**

```sql
SELECT
  CASE t.unit_price
    WHEN 0.99 THEN '音声 (0.99)'
    WHEN 1.99 THEN '動画 (1.99)'
    ELSE 'その他'
  END AS price_type,
  COUNT(DISTINCT t.track_id) AS catalog_tracks,
  COUNT(il.invoice_line_id) AS sold_lines,
  ROUND(SUM(il.unit_price * il.quantity), 2) AS sales
FROM track AS t
LEFT JOIN invoice_line AS il ON t.track_id = il.track_id
GROUP BY price_type
ORDER BY sales DESC NULLS LAST;
```

**期待される結果**：カタログは音声が圧倒的多数、売上も音声が中心という健全な結果。単価が 2 値であることが売上構成にどう効くかが見えます。

---

### 問題 19: 顧客の国を「北米」「南米」「ヨーロッパ」「その他」に大分類し、地域ごとの顧客数と総売上を表示してください。

**狙い**：`CASE` + `IN` で地域マッピングし集計。

**解答:**

```sql
SELECT
  CASE
    WHEN c.country IN ('USA', 'Canada') THEN '北米'
    WHEN c.country IN ('Brazil', 'Argentina', 'Chile') THEN '南米'
    WHEN c.country IN ('France', 'Germany', 'United Kingdom', 'Portugal',
                       'Spain', 'Italy', 'Netherlands', 'Belgium', 'Norway',
                       'Sweden', 'Denmark', 'Finland', 'Austria', 'Poland',
                       'Hungary', 'Ireland', 'Czech Republic') THEN 'ヨーロッパ'
    ELSE 'その他'
  END AS region,
  COUNT(DISTINCT c.customer_id) AS customer_count,
  ROUND(SUM(i.total), 2) AS total_sales
FROM customer AS c
JOIN invoice AS i ON c.customer_id = i.customer_id
GROUP BY region
ORDER BY total_sales DESC;
```

**期待される結果**：北米とヨーロッパが二大市場、南米・その他が続く、という地域構成が分かります。

---

### 問題 20: 月（1〜12 月）ごとに、5 年分を合算した総売上を表示してください。どの月がよく売れるか見たいので、売上の多い順に。

**狙い**：`EXTRACT(MONTH FROM ...)` で月だけを取り出し、年をまたいで集計。

**解答:**

```sql
SELECT
  EXTRACT(MONTH FROM i.invoice_date) AS sales_month,
  ROUND(SUM(i.total), 2) AS total_sales,
  COUNT(*) AS invoice_count
FROM invoice AS i
GROUP BY sales_month
ORDER BY total_sales DESC;
```

**期待される結果**：季節性（売れる月・売れない月）が見えます。年をまたいで「月」だけで束ねるのがポイント。

---

### 問題 21: ジャンルごとに、「音声曲数」「動画曲数」を横並び（1 行 1 ジャンル）で表示してください。

**狙い**：`CASE` を `SUM` の中に入れる「条件付き集計（擬似ピボット）」。

**解答:**

```sql
SELECT
  g.name AS genre_name,
  SUM(CASE WHEN t.unit_price = 0.99 THEN 1 ELSE 0 END) AS audio_tracks,
  SUM(CASE WHEN t.unit_price = 1.99 THEN 1 ELSE 0 END) AS video_tracks,
  COUNT(*) AS total_tracks
FROM genre AS g
JOIN track AS t ON g.genre_id = t.genre_id
GROUP BY g.genre_id, g.name
ORDER BY total_tracks DESC;
```

**期待される結果**：音楽系ジャンルは音声のみ、`TV Shows` / `Sci Fi & Fantasy` などは動画のみ、とジャンルの性質がくっきり分かれます。`SUM(CASE ...)` で列方向に集計する定番テクニック。

---

### 問題 22: 顧客を総購入額で「VIP（40 ドル以上）」「優良（20 ドル以上）」「一般（20 ドル未満）」にランク分けし、各ランクの人数と平均購入額を表示してください。

**狙い**：派生表で顧客ごとの合計を出し、その結果を `CASE` で区分して再集計（2 段階集計）。

**解答:**

```sql
SELECT
  CASE
    WHEN s.total_spent >= 40 THEN '1_VIP (40ドル以上)'
    WHEN s.total_spent >= 20 THEN '2_優良 (20ドル以上)'
    ELSE '3_一般 (20ドル未満)'
  END AS customer_rank,
  COUNT(*) AS customer_count,
  ROUND(AVG(s.total_spent), 2) AS avg_spent
FROM (
  SELECT c.customer_id, SUM(i.total) AS total_spent
  FROM customer AS c
  JOIN invoice AS i ON c.customer_id = i.customer_id
  GROUP BY c.customer_id
) AS s
GROUP BY customer_rank
ORDER BY customer_rank;
```

**期待される結果**：顧客層のピラミッドが見えます。「集計した結果をさらに集計する」には派生表（`FROM` 句のサブクエリ）が必要、という重要パターン。

---

## Part 4. 集合演算子（問題 23〜27）

### 問題 23: 2021 年にも 2025 年にも購入している「継続顧客」の氏名とメールアドレスを取得してください。

**狙い**：2 つの結果集合の共通部分を `INTERSECT` で取る。

**解答:**

```sql
SELECT c.first_name || ' ' || c.last_name AS customer_name, c.email
FROM customer AS c
JOIN invoice AS i ON c.customer_id = i.customer_id
WHERE EXTRACT(YEAR FROM i.invoice_date) = 2021

INTERSECT

SELECT c.first_name || ' ' || c.last_name AS customer_name, c.email
FROM customer AS c
JOIN invoice AS i ON c.customer_id = i.customer_id
WHERE EXTRACT(YEAR FROM i.invoice_date) = 2025

ORDER BY customer_name;
```

**期待される結果**：5 年間離れずに買い続けているロイヤル顧客が抽出できます。「両方に含まれる」＝`INTERSECT` の代表例。

---

### 問題 24: 「2021 年に購入があった顧客」から「2022 年に購入があった顧客」を差し引いて、2021 年に買ったが 2022 年には買わなかった顧客（離反の兆し）を取得してください。

**狙い**：差集合 `EXCEPT`。

**解答:**

```sql
SELECT c.customer_id, c.first_name || ' ' || c.last_name AS customer_name
FROM customer AS c
JOIN invoice AS i ON c.customer_id = i.customer_id
WHERE EXTRACT(YEAR FROM i.invoice_date) = 2021

EXCEPT

SELECT c.customer_id, c.first_name || ' ' || c.last_name AS customer_name
FROM customer AS c
JOIN invoice AS i ON c.customer_id = i.customer_id
WHERE EXTRACT(YEAR FROM i.invoice_date) = 2022

ORDER BY customer_id;
```

**期待される結果**：「去年は買ったが今年は音沙汰なし」の顧客リスト。`EXCEPT` は「A にあって B にない」を一発で表現できます。

---

### 問題 25: Jazz を購入したことがある顧客と、Blues を購入したことがある顧客の**両方**に該当する顧客の氏名を取得してください。

**狙い**：ジャンル別購入者集合の `INTERSECT`。

**解答:**

```sql
SELECT c.first_name || ' ' || c.last_name AS customer_name
FROM customer AS c
JOIN invoice AS i      ON c.customer_id = i.customer_id
JOIN invoice_line AS il ON i.invoice_id = il.invoice_id
JOIN track AS t         ON il.track_id = t.track_id
JOIN genre AS g         ON t.genre_id = g.genre_id
WHERE g.name = 'Jazz'

INTERSECT

SELECT c.first_name || ' ' || c.last_name AS customer_name
FROM customer AS c
JOIN invoice AS i      ON c.customer_id = i.customer_id
JOIN invoice_line AS il ON i.invoice_id = il.invoice_id
JOIN track AS t         ON il.track_id = t.track_id
JOIN genre AS g         ON t.genre_id = g.genre_id
WHERE g.name = 'Blues'

ORDER BY customer_name;
```

**期待される結果**：Jazz も Blues も好む「渋い音楽ファン」が見つかります。

---

### 問題 26: プレイリストに 1 度でも入っている曲の集合と、1 度でも購入された曲の集合を合わせて、「プレイリスト入り or 購入実績あり」＝“何らかの形で使われた曲”の総数を求めてください。

**狙い**：`UNION`（重複排除）で 2 集合を合算し件数を数える。

**解答:**

```sql
SELECT COUNT(*) AS used_track_count
FROM (
  SELECT track_id FROM playlist_track
  UNION
  SELECT track_id FROM invoice_line
) AS used_tracks;
```

**期待される結果**：「プレイリストにも入らず売れてもいない、完全に眠っている曲」との対比が作れます（全曲数 − この件数が完全死蔵曲）。`UNION`（重複を除く）と `UNION ALL`（除かない）の違いを確認するのに最適。

---

### 問題 27: プレイリスト "Music" に入っている曲から、プレイリスト "90's Music" に入っている曲を除いた曲の `track_id` を取得してください（件数の確認でも可）。

**狙い**：`EXCEPT` を中間表 `playlist_track` に対して使う。

**解答:**

```sql
SELECT pt.track_id
FROM playlist_track AS pt
JOIN playlist AS p ON pt.playlist_id = p.playlist_id
WHERE p.name = 'Music'

EXCEPT

SELECT pt.track_id
FROM playlist_track AS pt
JOIN playlist AS p ON pt.playlist_id = p.playlist_id
WHERE p.name = '90’s Music'

ORDER BY track_id;
```

> 補足：`90's Music` のアポストロフィはデータ上「右シングルクォート（’）」です。マッチしない場合は `p.name LIKE '90%Music'` で確認してください。

**期待される結果**：総合プレイリストにはあるが 90 年代版には入っていない曲が抽出されます。

---

## Part 5. サブクエリ（非相関）（問題 28〜36）

### 問題 28: 全トラックの平均再生時間より長い曲の、曲名と再生時間（分）を、長い順に上位 20 件表示してください。

**狙い**：`WHERE` 句のスカラサブクエリ（全体平均を 1 つの値として使う）。

**解答:**

```sql
SELECT
  t.name AS track_name,
  ROUND(t.milliseconds / 60000.0, 1) AS minutes
FROM track AS t
WHERE t.milliseconds > (SELECT AVG(milliseconds) FROM track)
ORDER BY t.milliseconds DESC
LIMIT 20;
```

**期待される結果**：長尺の映像作品が上位を独占します。「平均」という 1 つの値を条件に使うスカラサブクエリの基本形。

---

### 問題 29: 全トラックの中で最も長い曲を含むアルバムのタイトルと、その曲名・再生時間（分）を表示してください。

**狙い**：`MAX` を返すスカラサブクエリを `WHERE` の等値条件に使う。

**解答:**

```sql
SELECT
  al.title AS album_title,
  t.name   AS track_name,
  ROUND(t.milliseconds / 60000.0, 1) AS minutes
FROM track AS t
JOIN album AS al ON t.album_id = al.album_id
WHERE t.milliseconds = (SELECT MAX(milliseconds) FROM track);
```

**期待される結果**：最長曲（映像作品）と、それが収録されたアルバムがピンポイントで 1 件表示されます。

---

### 問題 30: 総購入額が「全顧客の平均総購入額」を上回る顧客の氏名と総購入額を、多い順に表示してください。

**狙い**：派生表で顧客別合計を作り、その平均をスカラサブクエリで比較。

**解答:**

```sql
SELECT
  c.first_name || ' ' || c.last_name AS customer_name,
  ROUND(SUM(i.total), 2) AS total_spent
FROM customer AS c
JOIN invoice AS i ON c.customer_id = i.customer_id
GROUP BY c.customer_id, customer_name
HAVING SUM(i.total) > (
  SELECT AVG(s.total_spent)
  FROM (
    SELECT SUM(i2.total) AS total_spent
    FROM invoice AS i2
    GROUP BY i2.customer_id
  ) AS s
)
ORDER BY total_spent DESC;
```

**期待される結果**：平均より上の「上位半分」の顧客が並びます。`HAVING` の中でサブクエリを使う応用形。

---

### 問題 31: Rock ジャンルの曲数より多くの曲を持つジャンルを、曲数の多い順に表示してください。

**狙い**：`GROUP BY` + `HAVING` の閾値にスカラサブクエリ（Rock の曲数）を使う。

**解答:**

```sql
SELECT
  g.name AS genre_name,
  COUNT(t.track_id) AS track_count
FROM genre AS g
JOIN track AS t ON g.genre_id = t.genre_id
GROUP BY g.genre_id, g.name
HAVING COUNT(t.track_id) > (
  SELECT COUNT(*)
  FROM track AS t2
  JOIN genre AS g2 ON t2.genre_id = g2.genre_id
  WHERE g2.name = 'Rock'
)
ORDER BY track_count DESC;
```

**期待される結果**：Rock は Chinook 最大ジャンルなので、これを超えるジャンルは基本的に無く、**結果 0 件**になります。「0 件であることに意味がある（Rock が最大だと証明できる）」タイプの問題。閾値を `'Jazz'` に変えると多数ヒットします。

---

### 問題 32: 最も総売上が高い国に住んでいる顧客の氏名・都市・総購入額を、購入額の多い順に表示してください。

**狙い**：派生表で「国別売上トップ 1 の国名」を求め、それを `WHERE` に使う。

**解答:**

```sql
SELECT
  c.first_name || ' ' || c.last_name AS customer_name,
  c.city,
  ROUND(SUM(i.total), 2) AS total_spent
FROM customer AS c
JOIN invoice AS i ON c.customer_id = i.customer_id
WHERE c.country = (
  SELECT country
  FROM invoice AS i2
  JOIN customer AS c2 ON i2.customer_id = c2.customer_id
  GROUP BY c2.country
  ORDER BY SUM(i2.total) DESC
  LIMIT 1
)
GROUP BY c.customer_id, customer_name, c.city
ORDER BY total_spent DESC;
```

**期待される結果**：最大市場（USA）の顧客一覧が売上順で並びます。`ORDER BY ... LIMIT 1` を返すスカラサブクエリで「トップ 1 の値」を取り出すテクニック。

---

### 問題 33: 収録曲数が最も多いアルバムのタイトルと曲数を表示してください（同数の場合は複数表示）。

**狙い**：「曲数の最大値」を派生表 + `MAX` で求め、それに一致するアルバムを抽出。

**解答:**

```sql
SELECT
  al.title AS album_title,
  COUNT(t.track_id) AS track_count
FROM album AS al
JOIN track AS t ON al.album_id = t.album_id
GROUP BY al.album_id, al.title
HAVING COUNT(t.track_id) = (
  SELECT MAX(cnt)
  FROM (
    SELECT COUNT(*) AS cnt
    FROM track
    WHERE album_id IS NOT NULL
    GROUP BY album_id
  ) AS album_counts
);
```

**期待される結果**：Chinook では "Greatest Hits" 系の大型アルバムが最多（50 曲超）で 1〜数件出ます。「最大値と一致する行」を出す＝集約結果を条件に使う王道。

---

### 問題 34: 一度も購入したことがない顧客の氏名とメールアドレスを取得してください。

**狙い**：`NOT IN` サブクエリ（購入者 ID の集合に含まれない顧客）。

**解答:**

```sql
SELECT
  c.first_name || ' ' || c.last_name AS customer_name,
  c.email
FROM customer AS c
WHERE c.customer_id NOT IN (
  SELECT customer_id FROM invoice
)
ORDER BY customer_name;
```

> 注意：`NOT IN` はサブクエリ側に `NULL` が混ざると意図しない結果になります。`invoice.customer_id` は `NOT NULL` なのでここでは安全ですが、一般には `NOT EXISTS`（問題 42 参照）が安全です。

**期待される結果**：Chinook では全顧客が最低 1 回は購入しているため **0 件**になる想定。「0 件＝全員が購入済み」を確認できる健全な結果。試しに `invoice` の年を絞ると（例：2025 年に限定）非購入者が現れます。

---

### 問題 35: 担当顧客からの売上合計が、全担当者の平均を上回る担当者の氏名と売上合計を表示してください。

**狙い**：派生表で担当者別売上を作り、その平均と比較。

**解答:**

```sql
SELECT
  e.first_name || ' ' || e.last_name AS support_rep,
  ROUND(SUM(i.total), 2) AS total_sales
FROM employee AS e
JOIN customer AS c ON c.support_rep_id = e.employee_id
JOIN invoice AS i  ON c.customer_id = i.customer_id
GROUP BY e.employee_id, support_rep
HAVING SUM(i.total) > (
  SELECT AVG(rep_sales.s)
  FROM (
    SELECT SUM(i2.total) AS s
    FROM customer AS c2
    JOIN invoice AS i2 ON c2.customer_id = i2.customer_id
    WHERE c2.support_rep_id IS NOT NULL
    GROUP BY c2.support_rep_id
  ) AS rep_sales
)
ORDER BY total_sales DESC;
```

**期待される結果**：担当者は 3 名なので、平均超えの「稼ぎ頭」1〜2 名が出ます。

---

### 問題 36: 各トラックの単価が「そのトラックが属するジャンルの平均単価」より高い曲を取得してください（曲名・ジャンル名・単価・ジャンル平均単価）。

**狙い**：ジャンル別平均単価を派生表で作り、元の track と結合して比較。

**解答:**

```sql
SELECT
  t.name AS track_name,
  g.name AS genre_name,
  t.unit_price,
  ga.avg_price
FROM track AS t
JOIN genre AS g ON t.genre_id = g.genre_id
JOIN (
  SELECT genre_id, AVG(unit_price) AS avg_price
  FROM track
  GROUP BY genre_id
) AS ga ON t.genre_id = ga.genre_id
WHERE t.unit_price > ga.avg_price
ORDER BY g.name, track_name;
```

**期待される結果**：ほとんどのジャンルは単価が 1 種類なので該当なし。**音声と動画が混在するジャンルだけ**、動画曲（1.99）が「ジャンル平均超え」として現れます。単価が 2 値であることの帰結が体感できる良問。

---

## Part 6. 相関サブクエリ・EXISTS（問題 37〜44）

### 問題 37: 各アルバムで最も長い曲（アルバム内で `milliseconds` が最大の曲）の、アルバム名・曲名・再生時間（分）を表示してください。

**狙い**：相関サブクエリで「同じアルバムの最大値」と一致する行だけ残す（グループ内トップをウィンドウ関数なしで）。

**解答:**

```sql
SELECT
  al.title AS album_title,
  t.name   AS track_name,
  ROUND(t.milliseconds / 60000.0, 1) AS minutes
FROM track AS t
JOIN album AS al ON t.album_id = al.album_id
WHERE t.milliseconds = (
  SELECT MAX(t2.milliseconds)
  FROM track AS t2
  WHERE t2.album_id = t.album_id
)
ORDER BY al.title;
```

**期待される結果**：アルバムごとに“看板曲（最長曲）”が 1 曲ずつ並びます。外側の `t` を内側が参照する相関サブクエリの典型。

---

### 問題 38: 各ジャンルで最も購入回数が多い曲を、ジャンル名・曲名・購入回数で表示してください（同数の場合は複数表示）。

**狙い**：派生表（曲別購入回数）+ 相関サブクエリ（同ジャンル内の最大回数）。

**解答:**

```sql
SELECT
  g.name AS genre_name,
  t.name AS track_name,
  ts.buy_count
FROM (
  SELECT il.track_id, COUNT(*) AS buy_count
  FROM invoice_line AS il
  GROUP BY il.track_id
) AS ts
JOIN track AS t ON ts.track_id = t.track_id
JOIN genre AS g ON t.genre_id = g.genre_id
WHERE ts.buy_count = (
  SELECT MAX(ts2.buy_count)
  FROM (
    SELECT il2.track_id, COUNT(*) AS buy_count
    FROM invoice_line AS il2
    GROUP BY il2.track_id
  ) AS ts2
  JOIN track AS t2 ON ts2.track_id = t2.track_id
  WHERE t2.genre_id = t.genre_id
)
ORDER BY g.name, track_name;
```

**期待される結果**：ジャンルごとの“ヒット曲”が並びます。購入回数が同数の曲が多いため各ジャンル複数出ることもあります（それも正しい結果）。派生表と相関サブクエリの合わせ技。

---

### 問題 39: 一度でも購入したことがある顧客の氏名を取得してください。`EXISTS` を使って書いてください。

**狙い**：`EXISTS` による存在チェック（相関サブクエリ）。

**解答:**

```sql
SELECT c.first_name || ' ' || c.last_name AS customer_name
FROM customer AS c
WHERE EXISTS (
  SELECT 1
  FROM invoice AS i
  WHERE i.customer_id = c.customer_id
)
ORDER BY customer_name;
```

**期待される結果**：購入実績のある顧客が並びます（Chinook では全員）。`EXISTS` は「1 件でもあれば真」なので `SELECT 1` で十分、という書き方を覚える問題。

---

### 問題 40: 自分の総購入額が「同じ国の顧客の平均総購入額」を上回る顧客の、氏名・国・総購入額を国名→購入額降順で表示してください。

**狙い**：相関サブクエリで「同じ国だけの平均」を都度計算して比較。

**解答:**

```sql
SELECT
  c.first_name || ' ' || c.last_name AS customer_name,
  c.country,
  (SELECT ROUND(SUM(i.total), 2)
   FROM invoice AS i
   WHERE i.customer_id = c.customer_id) AS total_spent
FROM customer AS c
WHERE (SELECT SUM(i.total)
       FROM invoice AS i
       WHERE i.customer_id = c.customer_id)
      >
      (SELECT AVG(ct.s)
       FROM (
         SELECT c2.customer_id, c2.country, SUM(i2.total) AS s
         FROM customer AS c2
         JOIN invoice AS i2 ON c2.customer_id = i2.customer_id
         GROUP BY c2.customer_id, c2.country
       ) AS ct
       WHERE ct.country = c.country)
ORDER BY c.country, total_spent DESC;
```

**期待される結果**：国ごとの「平均より上のお得意様」が抽出されます。`SELECT` 句と `WHERE` 句の両方でサブクエリを使う濃い問題。

---

### 問題 41: 各担当者について、その担当者が受け持つ顧客の中で最も総購入額が多い顧客の氏名と購入額を表示してください。

**狙い**：派生表（顧客別合計 + 担当者）と相関サブクエリ（担当者内の最大額）。

**解答:**

```sql
SELECT
  e.first_name || ' ' || e.last_name AS support_rep,
  cs.customer_name,
  ROUND(cs.total_spent, 2) AS total_spent
FROM (
  SELECT
    c.customer_id,
    c.support_rep_id,
    c.first_name || ' ' || c.last_name AS customer_name,
    SUM(i.total) AS total_spent
  FROM customer AS c
  JOIN invoice AS i ON c.customer_id = i.customer_id
  GROUP BY c.customer_id, c.support_rep_id, customer_name
) AS cs
JOIN employee AS e ON cs.support_rep_id = e.employee_id
WHERE cs.total_spent = (
  SELECT MAX(cs2.total_spent)
  FROM (
    SELECT c2.support_rep_id, SUM(i2.total) AS total_spent
    FROM customer AS c2
    JOIN invoice AS i2 ON c2.customer_id = i2.customer_id
    GROUP BY c2.customer_id, c2.support_rep_id
  ) AS cs2
  WHERE cs2.support_rep_id = cs.support_rep_id
)
ORDER BY total_spent DESC;
```

**期待される結果**：担当者ごとの“最重要顧客”が 1 人ずつ判明します。

---

### 問題 42: どのプレイリストにも入っていないトラックの曲名を、`NOT EXISTS` を使って取得してください（件数確認でも可）。

**狙い**：`NOT EXISTS` による不在チェック（`NOT IN` より安全）。

**解答:**

```sql
-- 件数
SELECT COUNT(*) AS not_in_any_playlist
FROM track AS t
WHERE NOT EXISTS (
  SELECT 1
  FROM playlist_track AS pt
  WHERE pt.track_id = t.track_id
);

-- 曲名一覧（先頭20件）
SELECT t.name AS track_name
FROM track AS t
WHERE NOT EXISTS (
  SELECT 1
  FROM playlist_track AS pt
  WHERE pt.track_id = t.track_id
)
ORDER BY t.name
LIMIT 20;
```

**期待される結果**：プレイリスト未収録の曲が抽出できます。`NOT EXISTS` は `NULL` の影響を受けず「存在しないもの」を安全に絞れる、という `NOT IN` との違いがポイント。

---

### 問題 43: 各アーティストで最も曲数が多いアルバムを、アーティスト名・アルバム名・曲数で表示してください（同数は複数表示）。

**狙い**：派生表（アルバム別曲数 + アーティスト）と相関サブクエリ（アーティスト内の最大曲数）。

**解答:**

```sql
SELECT
  ar.name AS artist_name,
  ac.title AS album_title,
  ac.track_count
FROM (
  SELECT al.album_id, al.artist_id, al.title, COUNT(t.track_id) AS track_count
  FROM album AS al
  JOIN track AS t ON al.album_id = t.album_id
  GROUP BY al.album_id, al.artist_id, al.title
) AS ac
JOIN artist AS ar ON ac.artist_id = ar.artist_id
WHERE ac.track_count = (
  SELECT MAX(ac2.track_count)
  FROM (
    SELECT al2.artist_id, COUNT(t2.track_id) AS track_count
    FROM album AS al2
    JOIN track AS t2 ON al2.album_id = t2.album_id
    GROUP BY al2.album_id, al2.artist_id
  ) AS ac2
  WHERE ac2.artist_id = ac.artist_id
)
ORDER BY ac.track_count DESC, artist_name;
```

**期待される結果**：アーティストごとの“代表作（最大収録アルバム）”が並びます。

---

### 問題 44: 各顧客の「最後の購入（最新の請求日）」の請求 ID・日付・金額を表示してください。

**狙い**：相関サブクエリで「同じ顧客の最大 `invoice_date`」に一致する請求を抽出。

**解答:**

```sql
SELECT
  c.first_name || ' ' || c.last_name AS customer_name,
  i.invoice_id,
  i.invoice_date,
  i.total
FROM customer AS c
JOIN invoice AS i ON c.customer_id = i.customer_id
WHERE i.invoice_date = (
  SELECT MAX(i2.invoice_date)
  FROM invoice AS i2
  WHERE i2.customer_id = c.customer_id
)
ORDER BY i.invoice_date DESC;
```

**期待される結果**：顧客ごとの直近購入が 1 件ずつ。誰が最近アクティブかが分かります。

---

## Part 7. 総合問題（問題 45〜50）

### 問題 45: 国ごとに「最も売れているジャンル」を、国名・ジャンル名・そのジャンルの売上で表示してください。

**狙い**：派生表（国×ジャンルの売上）+ 相関サブクエリ（同じ国での最大売上ジャンル）。国別トップ 1 をウィンドウ関数なしで。

**解答:**

```sql
SELECT
  cg.country,
  cg.genre_name,
  ROUND(cg.sales, 2) AS sales
FROM (
  SELECT
    i.billing_country AS country,
    g.name AS genre_name,
    SUM(il.unit_price * il.quantity) AS sales
  FROM invoice AS i
  JOIN invoice_line AS il ON i.invoice_id = il.invoice_id
  JOIN track AS t         ON il.track_id = t.track_id
  JOIN genre AS g         ON t.genre_id = g.genre_id
  GROUP BY i.billing_country, g.name
) AS cg
WHERE cg.sales = (
  SELECT MAX(cg2.sales)
  FROM (
    SELECT i2.billing_country AS country, SUM(il2.unit_price * il2.quantity) AS sales
    FROM invoice AS i2
    JOIN invoice_line AS il2 ON i2.invoice_id = il2.invoice_id
    JOIN track AS t2         ON il2.track_id = t2.track_id
    GROUP BY i2.billing_country, t2.genre_id
  ) AS cg2
  WHERE cg2.country = cg.country
)
ORDER BY cg.country;
```

**期待される結果**：国ごとの“お国柄”（USA は Rock、など）が 1 行ずつ並ぶ、見ていて楽しい結果。

---

### 問題 46: 各顧客が最も多く購入したジャンルを、顧客名・ジャンル名・そのジャンルでの購入回数で表示してください（同数は複数表示）。

**狙い**：派生表（顧客×ジャンルの購入回数）+ 相関サブクエリ（顧客内の最大回数）。

**解答:**

```sql
SELECT
  cg.customer_name,
  cg.genre_name,
  cg.buy_count
FROM (
  SELECT
    c.customer_id,
    c.first_name || ' ' || c.last_name AS customer_name,
    g.name AS genre_name,
    COUNT(*) AS buy_count
  FROM customer AS c
  JOIN invoice AS i      ON c.customer_id = i.customer_id
  JOIN invoice_line AS il ON i.invoice_id = il.invoice_id
  JOIN track AS t         ON il.track_id = t.track_id
  JOIN genre AS g         ON t.genre_id = g.genre_id
  GROUP BY c.customer_id, customer_name, g.name
) AS cg
WHERE cg.buy_count = (
  SELECT MAX(cg2.buy_count)
  FROM (
    SELECT c2.customer_id AS cid, COUNT(*) AS buy_count
    FROM customer AS c2
    JOIN invoice AS i2      ON c2.customer_id = i2.customer_id
    JOIN invoice_line AS il2 ON i2.invoice_id = il2.invoice_id
    JOIN track AS t2         ON il2.track_id = t2.track_id
    GROUP BY c2.customer_id, t2.genre_id
  ) AS cg2
  WHERE cg2.cid = cg.customer_id
)
ORDER BY cg.customer_name;
```

**期待される結果**：顧客ごとの“推しジャンル”が分かります。レコメンドの元ネタになりそうな実務的結果。

---

### 問題 47: 同じ曲を 2 回以上購入した顧客について、顧客名・曲名・購入回数を、購入回数の多い順に表示してください。

**狙い**：顧客×トラックで `GROUP BY` し `HAVING COUNT(*) >= 2`。

**解答:**

```sql
SELECT
  c.first_name || ' ' || c.last_name AS customer_name,
  t.name AS track_name,
  COUNT(*) AS times_bought
FROM customer AS c
JOIN invoice AS i      ON c.customer_id = i.customer_id
JOIN invoice_line AS il ON i.invoice_id = il.invoice_id
JOIN track AS t         ON il.track_id = t.track_id
GROUP BY c.customer_id, customer_name, t.track_id, t.name
HAVING COUNT(*) >= 2
ORDER BY times_bought DESC, customer_name;
```

**期待される結果**：「同じ曲を何度も買う」熱心なファンが炙り出されます（Chinook では稀なので少数だが確かに存在）。`HAVING` で重複購入だけを残すのがポイント。

---

### 問題 48: 「動画（単価 1.99）を 1 本でも購入したことがある顧客」の氏名・国を取得してください。`EXISTS` を使ってください。

**狙い**：`EXISTS` の中で複数テーブルを結合した存在チェック。

**解答:**

```sql
SELECT
  c.first_name || ' ' || c.last_name AS customer_name,
  c.country
FROM customer AS c
WHERE EXISTS (
  SELECT 1
  FROM invoice AS i
  JOIN invoice_line AS il ON i.invoice_id = il.invoice_id
  WHERE i.customer_id = c.customer_id
    AND il.unit_price = 1.99
)
ORDER BY c.country, customer_name;
```

**期待される結果**：映像コンテンツも買う顧客が抽出できます。`EXISTS` 内で条件付き結合を使う実戦的パターン。

---

### 問題 49: 総売上が高いアーティスト上位 10 組を、アーティスト名と売上で表示してください。

**狙い**：artist → album → track → invoice_line の 4 段結合と `SUM` ランキング。

**解答:**

```sql
SELECT
  ar.name AS artist_name,
  ROUND(SUM(il.unit_price * il.quantity), 2) AS total_sales
FROM artist AS ar
JOIN album AS al        ON ar.artist_id = al.artist_id
JOIN track AS t         ON al.album_id = t.album_id
JOIN invoice_line AS il ON t.track_id = il.track_id
GROUP BY ar.artist_id, ar.name
ORDER BY total_sales DESC
LIMIT 10;
```

**期待される結果**：一番稼いでいるアーティストの顔ぶれが並びます。「どのアーティストがビジネスを支えているか」が見える、締めにふさわしい集計。

---

### 問題 50: 顧客担当者ごとの売上合計と、それが会社全体の売上に占める割合（％, 小数第 1 位）を、売上の多い順に表示してください。

**狙い**：担当者別売上の集計 + 全社売上をスカラサブクエリで割って比率化。

**解答:**

```sql
SELECT
  e.first_name || ' ' || e.last_name AS support_rep,
  ROUND(SUM(i.total), 2) AS total_sales,
  ROUND(
    100.0 * SUM(i.total) / (SELECT SUM(total) FROM invoice),
    1
  ) AS pct_of_company
FROM employee AS e
JOIN customer AS c ON c.support_rep_id = e.employee_id
JOIN invoice AS i  ON c.customer_id = i.customer_id
GROUP BY e.employee_id, support_rep
ORDER BY total_sales DESC;
```

**期待される結果**：3 名の担当者の売上シェアが合計約 100% になり、貢献度が一目で分かります。分母（全社売上）を 1 つの値として使うスカラサブクエリで“割合”を出す締めの問題。

> ヒント：全担当者のシェアを足すとほぼ 100% になるはず。ならなければ、担当者未設定の顧客の売上が抜けている可能性を疑ってみましょう（検算の練習）。

---

## 補足：この問題集の解き方・教え方のポイント

- **「グループ内トップ 1」はウィンドウ関数を使わなくても解ける**。本問題集では相関サブクエリ（`WHERE x = (SELECT MAX(...) WHERE 同じグループ)`）で統一しています。後の講義でウィンドウ関数（`ROW_NUMBER`）を学ぶと同じことがもっと簡潔に書けるので、その対比を意識すると理解が深まります。
- **`NOT IN` より `NOT EXISTS`**（問題 34・42）。`NOT IN` はサブクエリに `NULL` が混ざると全体が空になる罠があります。存在しないものを探すときは `NOT EXISTS` が安全、と体で覚えてもらうのが狙いです。
- **単価は 0.99 / 1.99 の 2 値だけ**（問題 15・18・21・36）。この事実を踏まえていない「価格帯分析」的な問題は成立しません。逆にこの制約を利用した問題（ジャンル内平均超え＝動画曲、など）は綺麗に決まります。
- **日付は 2021〜2025 年のリテラルで**。`CURRENT_DATE - INTERVAL '...'` はデータが過去なので結果が空になります。年を明示的に指定しましょう。
```

