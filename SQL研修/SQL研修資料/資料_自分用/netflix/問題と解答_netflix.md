## PostgreSQL 問題集（Netflix エンゲージメントレポートデータ）

---

### 問題 1: 2024 年上半期に最も視聴された映画

2024 年 1 月から 6 月の期間（`duration = 'SEMI_ANNUALLY'`）で、最も視聴時間（`hours_viewed`）が長かった映画のタイトルと視聴時間を取得してください。

**回答:**

```sql
SELECT
  m.title,
  vs.hours_viewed
FROM movie AS m
JOIN view_summary AS vs
  ON m.id = vs.movie_id
WHERE
  vs.duration = 'SEMI_ANNUALLY' AND vs.start_date >= '2024-01-01' AND vs.end_date <= '2024-06-30'
ORDER BY
  vs.hours_viewed DESC
LIMIT 1;
```

---

### 問題 2: グローバルで利用可能な映画の平均ランタイム

グローバルで利用可能（`available_globally = TRUE`）な映画の平均ランタイム（`runtime`）を、小数点以下を切り捨てて整数で取得してください。

**回答:**

```sql
SELECT
  FLOOR(AVG(runtime)) AS average_runtime_globally_available_movies
FROM movie
WHERE
  available_globally = TRUE;
```

**ヒント:** 平均値を整数に変換するには `FLOOR()` 関数を使用します。

---

### 問題 3: 各月の視聴時間の合計（映画と TV 番組を区別せず）

2024 年 1 月から 6 月までの各月における、映画と TV 番組（シーズン）を合わせた総視聴時間（`hours_viewed`）を月ごとに集計してください。月のフォーマットは `YYYY-MM` としてください。

**回答:**

```sql
SELECT
  TO_CHAR(start_date, 'YYYY-MM') AS month,
  SUM(hours_viewed) AS total_hours_viewed
FROM view_summary
WHERE
  start_date >= '2024-01-01' AND end_date <= '2024-06-30'
GROUP BY
  month
ORDER BY
  month;
```

**ヒント:** 日付から年と月を抽出するには `TO_CHAR()` 関数を使用します。

---

### 問題 4: 視聴時間トップ 5 の映画と、その映画がリリースされてからの経過日数

2024 年上半期のデータで視聴時間（`hours_viewed`）がトップ 5 の映画タイトルと、その映画のリリース日（`release_date`）から 2024 年 6 月 30 日までの経過日数（日数）をそれぞれ取得してください。

**回答:**

```sql
SELECT
  m.title,
  vs.hours_viewed,
  ('2024-06-30' :: DATE - m.release_date) AS days_since_release
FROM movie AS m
JOIN view_summary AS vs
  ON m.id = vs.movie_id
WHERE
  vs.duration = 'SEMI_ANNUALLY' AND vs.start_date >= '2024-01-01' AND vs.end_date <= '2024-06-30'
ORDER BY
  vs.hours_viewed DESC
LIMIT 5;
```

---

### 問題 5: 各 TV 番組のシーズンごとの平均ランタイム

各 TV 番組について、その番組に属する各シーズン（`season`テーブル）の平均ランタイムを算出し、TV 番組のタイトル、シーズンタイトル、平均ランタイムを表示してください。ただし、ランタイムが 0 のシーズンは除外してください。

**回答:**

```sql
SELECT
  t.title AS tv_show_title,
  s.title AS season_title,
  s.season_number,
  AVG(s.runtime) AS average_season_runtime
FROM tv_show AS t
JOIN season AS s
  ON t.id = s.tv_show_id
WHERE
  s.runtime > 0
GROUP BY
  t.title,
  s.title,
  s.season_number
ORDER BY
  t.title,
  s.season_number;
```

---

### 問題 6: 視聴時間が平均より高い映画のリスト

2024 年上半期に登録された映画の中で、全体の平均視聴時間よりも視聴時間が長い映画のタイトルと視聴時間を取得してください。

**回答:**

```sql
SELECT
  m.title,
  vs.hours_viewed
FROM movie AS m
JOIN view_summary AS vs
  ON m.id = vs.movie_id
WHERE
  vs.duration = 'SEMI_ANNUALLY' AND vs.start_date >= '2024-01-01' AND vs.end_date <= '2024-06-30'
  AND vs.hours_viewed > (
    SELECT
      AVG(hours_viewed)
    FROM view_summary
    WHERE
      duration = 'SEMI_ANNUALLY' AND start_date >= '2024-01-01' AND end_date <= '2024-06-30' AND movie_id IS NOT NULL
  )
ORDER BY
  vs.hours_viewed DESC;
```

---

### 問題 7: 各 TV 番組で最も視聴時間の長いシーズン

各 TV 番組について、最も視聴時間（`hours_viewed`）が長いシーズンのタイトルとその視聴時間を取得してください。TV 番組のタイトルも表示してください。
（2024 年上半期データを使用）

**回答:**

```sql
WITH RankedSeasons AS (
  SELECT
    t.title AS tv_show_title,
    s.title AS season_title,
    vs.hours_viewed,
    ROW_NUMBER() OVER (PARTITION BY t.id ORDER BY vs.hours_viewed DESC) AS rn
  FROM tv_show AS t
  JOIN season AS s
    ON t.id = s.tv_show_id
  JOIN view_summary AS vs
    ON s.id = vs.season_id
  WHERE
    vs.duration = 'SEMI_ANNUALLY' AND vs.start_date >= '2024-01-01' AND vs.end_date <= '2024-06-30'
)
SELECT
  tv_show_title,
  season_title,
  hours_viewed
FROM RankedSeasons
WHERE
  rn = 1;
```

**ヒント:** `ROW_NUMBER()` 関数と `PARTITION BY` を使用して、各 TV 番組内でシーズンをランク付けします。

---

### 問題 8: 2024 年上半期にトップ 10 入りした映画と TV 番組の合計数

2024 年上半期（`duration = 'SEMI_ANNUALLY'`）に`view_rank`が 10 以内に入った映画と TV 番組（シーズン）のユニークなタイトルの合計数を取得してください。

**回答:**

```sql
SELECT
  COUNT(DISTINCT title) AS total_top10_titles
FROM (
  SELECT
    m.title
  FROM movie AS m
  JOIN view_summary AS vs
    ON m.id = vs.movie_id
  WHERE
    vs.duration = 'SEMI_ANNUALLY' AND vs.start_date >= '2024-01-01' AND vs.end_date <= '2024-06-30' AND vs.view_rank <= 10
  UNION ALL
  SELECT
    s.title
  FROM season AS s
  JOIN view_summary AS vs
    ON s.id = vs.season_id
  WHERE
    vs.duration = 'SEMI_ANNUALLY' AND vs.start_date >= '2024-01-01' AND vs.end_date <= '2024-06-30' AND vs.view_rank <= 10
) AS top_titles;
```

---

### 問題 9: リリース日が最も古いトップ 10 映画

2024 年上半期に視聴された映画の中で、`view_rank`が 10 以内に入った映画のうち、リリース日（`release_date`）が最も古い映画のタイトルとリリース日を取得してください。

**回答:**

```sql
SELECT
  m.title,
  m.release_date
FROM movie AS m
JOIN view_summary AS vs
  ON m.id = vs.movie_id
WHERE
  vs.duration = 'SEMI_ANNUALLY' AND vs.start_date >= '2024-01-01' AND vs.end_date <= '2024-06-30' AND vs.view_rank <= 10
ORDER BY
  m.release_date ASC
LIMIT 1;
```

---

### 問題 10: 各月の視聴時間の前月比（映画のみ）

2024 年 1 月から 6 月までの各月における映画の総視聴時間と、その前月からの視聴時間の変化率（パーセンテージ）を計算してください。最初の月は NULL を表示してください。

**回答:**

```sql
WITH MonthlyMovieViews AS (
  SELECT
    TO_CHAR(vs.start_date, 'YYYY-MM') AS month,
    SUM(vs.hours_viewed) AS current_month_hours
  FROM view_summary AS vs
  WHERE
    vs.movie_id IS NOT NULL AND vs.start_date >= '2024-01-01' AND vs.end_date <= '2024-06-30'
  GROUP BY
    month
)
SELECT
  month,
  current_month_hours,
  LAG(current_month_hours, 1, 0) OVER (ORDER BY month) AS previous_month_hours,
  CASE
    WHEN LAG(current_month_hours, 1, 0) OVER (ORDER BY month) = 0
    THEN NULL
    ELSE (
      (current_month_hours - LAG(current_month_hours, 1, 0) OVER (ORDER BY month)) :: NUMERIC / LAG(current_month_hours, 1, 0) OVER (ORDER BY month)
    ) * 100
  END AS percentage_change
FROM MonthlyMovieViews
ORDER BY
  month;
```

**ヒント:** `LAG()` 関数と `OVER (ORDER BY ...)` を使用して前月の値を取得します。パーセンテージ計算には `::NUMERIC` で型キャストが必要です。

---

### 問題 11: 各映画のグローバル利用可能性と視聴時間の関係

各映画について、その映画がグローバルで利用可能かどうか（`available_globally`）と、2024 年上半期の総視聴時間（`hours_viewed`）を一覧表示してください。グローバルで利用可能な映画とそうでない映画の視聴時間を比較しやすいように並び替えてください。

**回答:**

```sql
SELECT
  m.title,
  m.available_globally,
  COALESCE(vs.hours_viewed, 0) AS total_hours_viewed -- 視聴データがない場合は0を表示
FROM movie AS m
LEFT JOIN view_summary AS vs
  ON m.id = vs.movie_id
  AND vs.duration = 'SEMI_ANNUALLY' AND vs.start_date >= '2024-01-01' AND vs.end_date <= '2024-06-30'
ORDER BY
  m.available_globally DESC,
  total_hours_viewed DESC;
```

**ヒント:** `COALESCE()` 関数を使って NULL 値を別の値に置き換えられます。

---

### 問題 12: 各 TV 番組の最も新しいシーズンと最も古いシーズンのリリース日

各 TV 番組について、最も新しいシーズンのリリース日と最も古いシーズンのリリース日を取得してください。

**回答:**

```sql
SELECT
  t.title AS tv_show_title,
  MAX(s.release_date) AS latest_season_release_date,
  MIN(s.release_date) AS earliest_season_release_date
FROM tv_show AS t
JOIN season AS s
  ON t.id = s.tv_show_id
GROUP BY
  t.title
ORDER BY
  t.title;
```

---

### 問題 13: 視聴時間が特定の範囲内にある映画のタイトルとランタイム

2024 年上半期の視聴時間（`hours_viewed`）が 1000 万時間以上 2000 万時間未満の映画のタイトルとランタイム（`runtime`）を取得してください。

**回答:◎**

```sql
SELECT
  m.title,
  m.runtime,
  vs.hours_viewed
FROM movie AS m
JOIN view_summary AS vs
  ON m.id = vs.movie_id
WHERE
  vs.duration = 'SEMI_ANNUALLY' AND vs.start_date >= '2024-01-01' AND vs.end_date <= '2024-06-30'
  AND vs.hours_viewed >= 10000000 AND vs.hours_viewed < 20000000
ORDER BY
  vs.hours_viewed DESC;
```

```SQL
-- 上半期の説明が間に合ってなかった感じかと思いますので、問題ないです。
SELECT
	映画.TITLE,
	映画.RUNTIME,
	映画.ID,
	視聴集計.MOVIE_ID
FROM
	VIEW_SUMMARY 視聴集計
	INNER JOIN MOVIE 映画 ON 映画.ID = 視聴集計.MOVIE_ID
WHERE
	START_DATE BETWEEN DATE '2024-01-01' AND DATE  '2024-06-30'
	AND HOURS_VIEWED BETWEEN 10000000 AND 19999999
ORDER BY
	映画.TITLE,
	映画.RUNTIME
```

---

### 問題 14: 同じリリース日を持つ映画の数

同じリリース日を持つ映画が複数ある場合、そのリリース日と、その日にリリースされた映画の数を取得してください。

**回答:**

```sql
SELECT
  release_date,
  COUNT(id) AS number_of_movies
FROM movie
GROUP BY
  release_date
HAVING
  COUNT(id) > 1
ORDER BY
  number_of_movies DESC,
  release_date DESC;
```

---

### 問題 15: グローバルで利用可能な映画とそうでない映画のトップ 10 視聴時間比較

グローバルで利用可能な映画の中から視聴時間トップ 10 と、そうでない映画の中から視聴時間トップ 10 をそれぞれ取得し、一つの結果セットとして結合してください。各映画がグローバルで利用可能かどうかの情報も含めてください。

**回答:**

```sql
(
  SELECT
    m.title,
    m.available_globally,
    vs.hours_viewed,
    'Globally Available' AS category
  FROM movie AS m
  JOIN view_summary AS vs
    ON m.id = vs.movie_id
  WHERE
    m.available_globally = TRUE AND vs.duration = 'SEMI_ANNUALLY' AND vs.start_date >= '2024-01-01' AND vs.end_date <= '2024-06-30'
  ORDER BY
    vs.hours_viewed DESC
  LIMIT 10
)
UNION ALL
(
  SELECT
    m.title,
    m.available_globally,
    vs.hours_viewed,
    'Not Globally Available' AS category
  FROM movie AS m
  JOIN view_summary AS vs
    ON m.id = vs.movie_id
  WHERE
    m.available_globally = FALSE AND vs.duration = 'SEMI_ANNUALLY' AND vs.start_date >= '2024-01-01' AND vs.end_date <= '2024-06-30'
  ORDER BY
    vs.hours_viewed DESC
  LIMIT 10
)
ORDER BY
  category,
  hours_viewed DESC;
```

---

### 問題 16: 各 TV 番組の平均視聴時間（シーズン単位）と、その TV 番組で最も視聴されたシーズンの視聴時間

各 TV 番組について、その全てのシーズンの平均視聴時間と、最も視聴されたシーズンの視聴時間を取得してください。TV 番組のタイトルも表示してください。
（2024 年上半期データを使用）

**回答:**

```sql
WITH SeasonViewData AS (
  SELECT
    t.title AS tv_show_title,
    s.id AS season_id,
    s.title AS season_title,
    COALESCE(vs.hours_viewed, 0) AS hours_viewed
  FROM tv_show AS t
  JOIN season AS s
    ON t.id = s.tv_show_id
  LEFT JOIN view_summary AS vs
    ON s.id = vs.season_id
    AND vs.duration = 'SEMI_ANNUALLY' AND vs.start_date >= '2024-01-01' AND vs.end_date <= '2024-06-30'
)
SELECT
  tv_show_title,
  AVG(hours_viewed) AS average_season_hours_viewed,
  MAX(hours_viewed) AS max_season_hours_viewed
FROM SeasonViewData
GROUP BY
  tv_show_title
ORDER BY
  tv_show_title;
```

---

### 問題 17: 累積週数が 5 週以上トップ 10 にランクインした映画のタイトルと累積週数

2024 年上半期（`duration = 'WEEKLY'`）のデータで、`cumulative_weeks_in_top10`が 5 週以上である映画のタイトルと累積週数を取得してください。

**回答:**

```sql
SELECT
	M.TITLE,
	VS.CUMULATIVE_WEEKS_IN_TOP10
FROM
	MOVIE AS M
	JOIN VIEW_SUMMARY AS VS ON M.ID = VS.MOVIE_ID
WHERE
	VS.DURATION = 'WEEKLY'
	AND VS.START_DATE >= '2024-01-01'
	AND VS.END_DATE <= '2024-06-30'
	AND VS.CUMULATIVE_WEEKS_IN_TOP10 >= 5
ORDER BY
	VS.CUMULATIVE_WEEKS_IN_TOP10 DESC;
```

---

### 問題 18: 映画と TV 番組（シーズン）それぞれの総視聴時間ランキングトップ 3

映画と TV 番組（シーズン）でそれぞれ総視聴時間（`hours_viewed`）のランキングトップ 3 を取得し、コンテンツのタイプ（'Movie'または'Season'）を区別して表示してください。
（2024 年上半期データを使用）

**回答:**

```sql
(
  SELECT
    m.title AS content_title,
    vs.hours_viewed,
    'Movie' AS content_type,
    RANK() OVER (ORDER BY vs.hours_viewed DESC) AS rank
  FROM movie AS m
  JOIN view_summary AS vs
    ON m.id = vs.movie_id
  WHERE
    vs.duration = 'SEMI_ANNUALLY' AND vs.start_date >= '2024-01-01' AND vs.end_date <= '2024-06-30'
  LIMIT 3
)
UNION ALL
(
  SELECT
    s.title AS content_title,
    vs.hours_viewed,
    'Season' AS content_type,
    RANK() OVER (ORDER BY vs.hours_viewed DESC) AS rank
  FROM season AS s
  JOIN view_summary AS vs
    ON s.id = vs.season_id
  WHERE
    vs.duration = 'SEMI_ANNUALLY' AND vs.start_date >= '2024-01-01' AND vs.end_date <= '2024-06-30'
  LIMIT 3
)
ORDER BY
  content_type,
  hours_viewed DESC;
```

**別パターン:**

```sql
WITH CombinedViews AS (
  SELECT
    m.title AS content_title,
    vs.hours_viewed,
    'Movie' AS content_type
  FROM movie AS m
  JOIN view_summary AS vs
    ON m.id = vs.movie_id
  WHERE
    vs.duration = 'SEMI_ANNUALLY' AND vs.start_date >= '2024-01-01' AND vs.end_date <= '2024-06-30'
  UNION ALL
  SELECT
    s.title AS content_title,
    vs.hours_viewed,
    'Season' AS content_type
  FROM season AS s
  JOIN view_summary AS vs
    ON s.id = vs.season_id
  WHERE
    vs.duration = 'SEMI_ANNUALLY' AND vs.start_date >= '2024-01-01' AND vs.end_date <= '2024-06-30'
),
RankedCombinedViews AS (
  SELECT
    content_title,
    hours_viewed,
    content_type,
    RANK() OVER (PARTITION BY content_type ORDER BY hours_viewed DESC) AS rank_num
  FROM CombinedViews
)
SELECT
  content_title,
  hours_viewed,
  content_type
FROM RankedCombinedViews
WHERE
  rank_num <= 3
ORDER BY
  content_type,
  hours_viewed DESC;
```

---

### 問題 19: 各言語圏（locale）における平均ランタイムの映画トップ 3

各`locale`において、ランタイム（`runtime`）が長い映画のトップ 3 のタイトル、ランタイム、およびその`locale`を取得してください。

**回答:**

```sql
WITH RankedMovies AS (
  SELECT
    locale,
    title,
    runtime,
    ROW_NUMBER() OVER (PARTITION BY locale ORDER BY runtime DESC) AS rn
  FROM movie
  WHERE
    locale IS NOT NULL AND runtime IS NOT NULL
)
SELECT
  locale,
  title,
  runtime
FROM RankedMovies
WHERE
  rn <= 3
ORDER BY
  locale,
  runtime DESC;
```

---

### 問題 20: 2024 年上半期にトップ 10 入りしたが、累積週数が 0 の映画

2024 年上半期（`duration = 'WEEKLY'`）のデータで、`view_rank`が 10 以内に入っているにも関わらず、`cumulative_weeks_in_top10`が 1 である映画のタイトルを取得してください。
（訳：１０位圏内だったがそれが１週間で終わってしまった）

**回答:**

```sql
SELECT
	M.TITLE
FROM
	MOVIE AS M
	JOIN VIEW_SUMMARY AS VS ON M.ID = VS.MOVIE_ID
WHERE
	VS.DURATION = 'WEEKLY'
	AND VS.START_DATE >= '2024-01-01'
	AND VS.END_DATE <= '2024-06-30'
	AND VS.VIEW_RANK <= 10
	AND VS.CUMULATIVE_WEEKS_IN_TOP10 = 1;
```

---

### 問題 21: 各 TV 番組の最も視聴されたシーズンとその次のシーズンの視聴時間の差

各 TV 番組について、最も視聴時間（`hours_viewed`）が長いシーズンと、その次に視聴時間が長いシーズンの視聴時間の差を計算してください。
（2024 年上半期データを使用）

**回答:**

```sql
WITH RankedSeasonViews AS (
  SELECT
    t.title AS tv_show_title,
    s.title AS season_title,
    vs.hours_viewed,
    ROW_NUMBER() OVER (PARTITION BY t.id ORDER BY vs.hours_viewed DESC) AS rn
  FROM tv_show AS t
  JOIN season AS s
    ON t.id = s.tv_show_id
  JOIN view_summary AS vs
    ON s.id = vs.season_id
  WHERE
    vs.duration = 'SEMI_ANNUALLY' AND vs.start_date >= '2024-01-01' AND vs.end_date <= '2024-06-30'
)
SELECT
  tsv1.tv_show_title,
  tsv1.season_title AS first_ranked_season,
  tsv1.hours_viewed AS first_ranked_hours,
  tsv2.season_title AS second_ranked_season,
  tsv2.hours_viewed AS second_ranked_hours,
  (tsv1.hours_viewed - COALESCE(tsv2.hours_viewed, 0)) AS hours_viewed_difference
FROM RankedSeasonViews AS tsv1
LEFT JOIN RankedSeasonViews AS tsv2
  ON tsv1.tv_show_title = tsv2.tv_show_title AND tsv2.rn = 2
WHERE
  tsv1.rn = 1;
```

**ヒント:** 自己結合と`ROW_NUMBER()`を使って、各 TV 番組のトップ 2 のシーズンを取得します。

---

### 問題 22: 視聴回数（`views`）が平均以上の映画と、平均以下の TV 番組

2024 年上半期データで、視聴回数（`views`）が映画全体の平均視聴回数以上である映画のタイトルと、視聴回数が TV 番組（シーズン）全体の平均視聴回数以下である TV 番組（シーズン）のタイトルをそれぞれ取得し、一つの結果セットに統合してください。
コンテンツの種類（'Movie' or 'Season'）も表示してください。

**回答:**

```sql
WITH AvgMovieViews AS (
  SELECT
    AVG(views) AS avg_views
  FROM view_summary
  WHERE
    movie_id IS NOT NULL AND duration = 'SEMI_ANNUALLY' AND start_date >= '2024-01-01' AND end_date <= '2024-06-30'
), AvgSeasonViews AS (
  SELECT
    AVG(views) AS avg_views
  FROM view_summary
  WHERE
    season_id IS NOT NULL AND duration = 'SEMI_ANNUALLY' AND start_date >= '2024-01-01' AND end_date <= '2024-06-30'
)
(
  SELECT
    m.title AS content_title,
    vs.views,
    'Movie' AS content_type
  FROM movie AS m
  JOIN view_summary AS vs
    ON m.id = vs.movie_id
  WHERE
    vs.duration = 'SEMI_ANNUALLY' AND vs.start_date >= '2024-01-01' AND vs.end_date <= '2024-06-30'
    AND vs.views >= (
      SELECT
        avg_views
      FROM AvgMovieViews
    )
)
UNION ALL
(
  SELECT
    s.title AS content_title,
    vs.views,
    'Season' AS content_type
  FROM season AS s
  JOIN view_summary AS vs
    ON s.id = vs.season_id
  WHERE
    vs.duration = 'SEMI_ANNUALLY' AND vs.start_date >= '2024-01-01' AND vs.end_date <= '2024-06-30'
    AND vs.views <= (
      SELECT
        avg_views
      FROM AvgSeasonViews
    )
)
ORDER BY
  content_type,
  views DESC;
```

---

### 問題 23: 各映画の視聴時間が、その映画がリリースされた年の平均視聴時間と比較してどうか

2024 年上半期（`duration = 'SEMI_ANNUALLY'`）のデータで、各映画の視聴時間（`hours_viewed`）とその映画がリリースされた年の全ての映画の平均視聴時間を比較して、その差を表示してください。

**回答:**

```sql
WITH MovieYearlyAvg AS (
  SELECT
    EXTRACT(
      YEAR
      FROM
        m.release_date
    ) AS release_year,
    AVG(vs.hours_viewed) AS avg_yearly_hours_viewed
  FROM movie AS m
  JOIN view_summary AS vs
    ON m.id = vs.movie_id
  WHERE
    vs.duration = 'SEMI_ANNUALLY' AND vs.start_date >= '2024-01-01' AND vs.end_date <= '2024-06-30'
  GROUP BY
    release_year
)
SELECT
  m.title,
  EXTRACT(
    YEAR
    FROM
      m.release_date
  ) AS release_year,
  vs.hours_viewed AS movie_hours_viewed,
  mya.avg_yearly_hours_viewed,
  (vs.hours_viewed - mya.avg_yearly_hours_viewed) AS difference_from_yearly_avg
FROM movie AS m
JOIN view_summary AS vs
  ON m.id = vs.movie_id
JOIN MovieYearlyAvg AS mya
  ON EXTRACT(
    YEAR
    FROM
      m.release_date
  ) = mya.release_year
WHERE
  vs.duration = 'SEMI_ANNUALLY' AND vs.start_date >= '2024-01-01' AND vs.end_date <= '2024-06-30'
ORDER BY
  m.title;
```

こっちが正しいかも。。。映画がリリースされた年のすべての

```SQL
WITH
	平均視聴時間 AS (
		SELECT
			映画.ID,
			映画.TITLE,
			EXTRACT(
				YEAR
				FROM
					映画.RELEASE_DATE
			)AS 公開年,
			視聴集計.HOURS_VIEWED AS 視聴時間,
			AVG(視聴集計.HOURS_VIEWED) OVER (
				PARTITION BY
					EXTRACT(
						YEAR
						FROM
							映画.RELEASE_DATE
					)
			) AS 平均,
			視聴集計.START_DATE,
			視聴集計.END_DATE,
			視聴集計.DURATION,
			視聴集計.MOVIE_ID
		FROM
			VIEW_SUMMARY 視聴集計
			INNER JOIN MOVIE 映画 ON 映画.ID = 視聴集計.MOVIE_ID
		WHERE
			視聴集計.MOVIE_ID IS NOT NULL
			AND EXTRACT(
				YEAR
				FROM
					映画.RELEASE_DATE
			) IS NOT NULL
	)
SELECT
	ID,
	TITLE,
	公開年,
	視聴時間,
	平均,
	視聴時間 - 平均	AS 差分
FROM
	平均視聴時間
WHERE
	START_DATE >= DATE '2024-01-01'
	AND END_DATE <= DATE '2024-06-30'
	AND DURATION = 'SEMI_ANNUALLY'
	AND MOVIE_ID IS NOT NULL

ORDER BY id
```

**ヒント:** `EXTRACT(YEAR FROM date_column)` で日付から年を取得できます。

---

### 問題 24: 各映画の視聴時間の累計（リリース日順）

各映画について、その映画のリリース日順に視聴時間（`hours_viewed`）の累計を計算してください。
（2024 年上半期データを使用）

**回答:**

```sql
SELECT
  m.title,
  m.release_date,
  vs.hours_viewed,
  SUM(vs.hours_viewed) OVER (ORDER BY m.release_date) AS cumulative_hours_viewed
FROM movie AS m
JOIN view_summary AS vs
  ON m.id = vs.movie_id
WHERE
  vs.duration = 'SEMI_ANNUALLY' AND vs.start_date >= '2024-01-01' AND vs.end_date <= '2024-06-30'
ORDER BY
  m.release_date;
```

---

### 問題 25: 2024 年上半期に最も多くの映画がリリースされた月

2024 年 1 月から 6 月までの期間で、最も多くの映画がリリースされた月とその数を取得してください。
（`release_date`を使用）

**回答:**

```sql
SELECT
  TO_CHAR(release_date, 'YYYY-MM') AS release_month,
  COUNT(id) AS movie_count
FROM movie
WHERE
  release_date >= '2024-01-01' AND release_date <= '2024-06-30'
GROUP BY
  release_month
ORDER BY
  movie_count DESC
LIMIT 1;
```

---

### 問題 26: グローバルで利用可能かつランタイムが平均以上の映画

グローバルで利用可能（`available_globally = TRUE`）であり、かつ全体の映画の平均ランタイム（`runtime`）よりもランタイムが長い映画のタイトルとランタイムを取得してください。

**回答:**

```sql
SELECT
  title,
  runtime
FROM movie
WHERE
  available_globally = TRUE AND runtime > (
    SELECT
      AVG(runtime)
    FROM movie
    WHERE
      runtime IS NOT NULL
  )
ORDER BY
  runtime DESC;
```

---

### 問題 27: 各 TV 番組の最も視聴されたシーズンのタイトルと、その TV 番組が持つ全シーズンの数

各 TV 番組について、最も視聴時間（`hours_viewed`）が長かったシーズンのタイトルと、その TV 番組に存在する全シーズンの数を取得してください。
（2024 年上半期データを使用）

**回答:**

```sql
WITH RankedSeasons AS (
  SELECT
    t.id AS tv_show_id,
    t.title AS tv_show_title,
    s.title AS season_title,
    vs.hours_viewed,
    ROW_NUMBER() OVER (PARTITION BY t.id ORDER BY vs.hours_viewed DESC) AS rn
  FROM tv_show AS t
  JOIN season AS s
    ON t.id = s.tv_show_id
  JOIN view_summary AS vs
    ON s.id = vs.season_id
  WHERE
    vs.duration = 'SEMI_ANNUALLY' AND vs.start_date >= '2024-01-01' AND vs.end_date <= '2024-06-30'
),
SeasonCounts AS (
  SELECT
    tv_show_id,
    COUNT(id) AS total_seasons
  FROM season
  GROUP BY
    tv_show_id
)
SELECT
  rs.tv_show_title,
  rs.season_title AS most_viewed_season,
  rs.hours_viewed,
  sc.total_seasons
FROM RankedSeasons AS rs
JOIN SeasonCounts AS sc
  ON rs.tv_show_id = sc.tv_show_id
WHERE
  rs.rn = 1
ORDER BY
  rs.tv_show_title;
```

---

### 問題 28: 映画と TV 番組（シーズン）の中で、ランタイムが最も長いコンテンツと短いコンテンツ

全映画と全シーズンの中で、最もランタイム（`runtime`）が長いコンテンツのタイトルとランタイム、および最も短いコンテンツのタイトルとランタイムをそれぞれ取得してください。コンテンツの種類（'Movie' or 'Season'）も表示してください。

**回答:**

```sql
(
  SELECT
    title,
    runtime,
    'Movie' AS content_type
  FROM movie
  WHERE
    runtime IS NOT NULL
  ORDER BY
    runtime DESC
  LIMIT 1
)
UNION ALL
(
  SELECT
    title,
    runtime,
    'Season' AS content_type
  FROM season
  WHERE
    runtime IS NOT NULL
  ORDER BY
    runtime DESC
  LIMIT 1
)
UNION ALL
(
  SELECT
    title,
    runtime,
    'Movie' AS content_type
  FROM movie
  WHERE
    runtime IS NOT NULL
  ORDER BY
    runtime ASC
  LIMIT 1
)
UNION ALL
(
  SELECT
    title,
    runtime,
    'Season' AS content_type
  FROM season
  WHERE
    runtime IS NOT NULL
  ORDER BY
    runtime ASC
  LIMIT 1
)
ORDER BY
  runtime DESC;
```

---

### 問題 29: 各 TV 番組のシーズンごとの視聴時間と、その TV 番組内での視聴時間ランキング

各 TV 番組について、各シーズンの視聴時間（`hours_viewed`）と、その TV 番組内での視聴時間のランキング（同順位がある場合は次の順位をスキップしない`RANK()`）を取得してください。
（2024 年上半期データを使用）

**回答:**

```sql
SELECT
  t.title AS tv_show_title,
  s.title AS season_title,
  vs.hours_viewed,
  RANK() OVER (PARTITION BY t.id ORDER BY vs.hours_viewed DESC) AS view_rank_within_tv_show
FROM tv_show AS t
JOIN season AS s
  ON t.id = s.tv_show_id
JOIN view_summary AS vs
  ON s.id = vs.season_id
WHERE
  vs.duration = 'SEMI_ANNUALLY' AND vs.start_date >= '2024-01-01' AND vs.end_date <= '2024-06-30'
ORDER BY
  t.title,
  view_rank_within_tv_show;
```

---

### 問題 30: 2024 年上半期にトップ 10 入りした映画のうち、リリースから 1 年以内の映画

2024 年上半期（`duration = 'SEMI_ANNUALLY'`）に`view_rank`が 10 以内に入った映画の中で、2024 年 6 月 30 日時点でリリースから 1 年以内（365 日以内）の映画のタイトルとリリース日、視聴時間を取得してください。

**回答:**

```sql
SELECT
  m.title,
  m.release_date,
  vs.hours_viewed
FROM movie AS m
JOIN view_summary AS vs
  ON m.id = vs.movie_id
WHERE
  vs.duration = 'SEMI_ANNUALLY' AND vs.start_date >= '2024-01-01' AND vs.end_date <= '2024-06-30'
  AND vs.view_rank <= 10
  AND (
    '2024-06-30' :: DATE - m.release_date
  ) <= 365
ORDER BY
  m.release_date DESC;
```

---

### 問題 31: 各 TV 番組のシーズン数が平均以上の TV 番組

全ての TV 番組のシーズン数の平均を算出し、その平均よりも多くのシーズンを持つ TV 番組のタイトルとシーズン数を取得してください。

**回答:**

```sql
WITH TvShowSeasonCounts AS (
  SELECT
    tv_show_id,
    COUNT(id) AS season_count
  FROM season
  GROUP BY
    tv_show_id
), AvgSeasonCount AS (
  SELECT
    AVG(season_count) AS average_seasons
  FROM TvShowSeasonCounts
)
SELECT
  t.title AS tv_show_title,
  tsc.season_count
FROM tv_show AS t
JOIN TvShowSeasonCounts AS tsc
  ON t.id = tsc.tv_show_id
WHERE
  tsc.season_count > (
    SELECT
      average_seasons
    FROM AvgSeasonCount
  )
ORDER BY
  tsc.season_count DESC;
```

---

### 問題 32: オリジナルタイトルと通常のタイトルが異なる映画のリスト

`original_title`と`title`が異なる映画のタイトル、オリジナルタイトル、ランタイムを取得してください。

**回答:**

```sql
SELECT
  title,
  original_title,
  runtime
FROM movie
WHERE
  original_title IS NOT NULL AND original_title <> title;
```

---

### 問題 33: 2024 年上半期に視聴回数（`views`）の総和が最も高かった週

2024 年 1 月から 6 月までの週ごとのデータ（`duration = 'WEEKLY'`）で、視聴回数（`views`）の総和が最も高かった週の`start_date`と`end_date`、そして総視聴回数を取得してください。

**回答:**

```sql
SELECT
  start_date,
  end_date,
  SUM(views) AS total_weekly_views
FROM view_summary
WHERE
  duration = 'WEEKLY' AND start_date >= '2024-01-01' AND end_date <= '2024-06-30'
GROUP BY
  start_date,
  end_date
ORDER BY
  total_weekly_views DESC
LIMIT 1;
```

---

### 問題 34: 各映画の視聴回数と、その映画が属する view_summary テーブルの行数

各映画について、2024 年上半期（`duration = 'SEMI_ANNUALLY'`）の視聴回数（`views`）と、その映画が`view_summary`テーブルに登場する回数を取得してください。

**回答:**

```sql
SELECT
  m.title,
  vs.views AS semi_annual_views,
  COUNT(vs_all.id) AS total_summary_entries
FROM movie AS m
LEFT JOIN view_summary AS vs
  ON m.id = vs.movie_id
  AND vs.duration = 'SEMI_ANNUALLY' AND vs.start_date >= '2024-01-01' AND vs.end_date <= '2024-06-30'
LEFT JOIN view_summary AS vs_all
  ON m.id = vs_all.movie_id
GROUP BY
  m.title,
  vs.views
ORDER BY
  m.title;
```

---

### 問題 35: 映画と TV 番組（シーズン）の中で、2024 年上半期に一度もトップ 10 に入らなかったコンテンツ

2024 年上半期（`duration = 'SEMI_ANNUALLY'`）のデータで、`view_rank`が一度も 10 以内に入らなかった映画と TV 番組（シーズン）のタイトルをそれぞれ取得し、結合して表示してください。
コンテンツの種類（'Movie' or 'Season'）も表示してください。

**回答:**

```sql
(
  SELECT
    m.title AS content_title,
    'Movie' AS content_type
  FROM movie AS m
  LEFT JOIN view_summary AS vs
    ON m.id = vs.movie_id
    AND vs.duration = 'SEMI_ANNUALLY' AND vs.start_date >= '2024-01-01' AND vs.end_date <= '2024-06-30' AND vs.view_rank <= 10
  GROUP BY
    m.title
  HAVING
    COUNT(vs.id) = 0
)
UNION ALL
(
  SELECT
    s.title AS content_title,
    'Season' AS content_type
  FROM season AS s
  LEFT JOIN view_summary AS vs
    ON s.id = vs.season_id
    AND vs.duration = 'SEMI_ANNUALLY' AND vs.start_date >= '2024-01-01' AND vs.end_date <= '2024-06-30' AND vs.view_rank <= 10
  GROUP BY
    s.title
  HAVING
    COUNT(vs.id) = 0
)
ORDER BY
  content_type,
  content_title;
```

---

### 問題 36: 各月の視聴時間と累積視聴時間（TV 番組シーズンのみ）

2024 年 1 月から 6 月までの各月における TV 番組（シーズン）の総視聴時間と、その時点までの累積視聴時間を月ごとに取得してください。

**回答:**

```sql
SELECT
  TO_CHAR(vs.start_date, 'YYYY-MM') AS month,
  SUM(vs.hours_viewed) AS monthly_season_hours_viewed,
  SUM(SUM(vs.hours_viewed)) OVER (ORDER BY TO_CHAR(vs.start_date, 'YYYY-MM')) AS cumulative_season_hours_viewed
FROM view_summary AS vs
WHERE
  vs.season_id IS NOT NULL AND vs.start_date >= '2024-01-01' AND vs.end_date <= '2024-06-30'
GROUP BY
  month
ORDER BY
  month;
```

---

### 問題 37: 視聴回数がそのカテゴリ（映画または TV 番組シーズン）の平均視聴回数を上回るコンテンツ

2024 年上半期（`duration = 'SEMI_ANNUALLY'`）のデータで、映画であれば映画全体の平均視聴回数（`views`）を、TV 番組シーズンであれば TV 番組シーズン全体の平均視聴回数を上回るコンテンツのタイトル、視聴回数、コンテンツタイプ（'Movie'または'Season'）を取得してください。

**回答:**

```sql
WITH AvgViews AS (
  SELECT
    AVG(CASE WHEN movie_id IS NOT NULL THEN views ELSE NULL END) AS avg_movie_views,
    AVG(CASE WHEN season_id IS NOT NULL THEN views ELSE NULL END) AS avg_season_views
  FROM view_summary
  WHERE
    duration = 'SEMI_ANNUALLY' AND start_date >= '2024-01-01' AND end_date <= '2024-06-30'
)
SELECT
  COALESCE(m.title, s.title) AS content_title,
  vs.views,
  CASE
    WHEN vs.movie_id IS NOT NULL
    THEN 'Movie'
    ELSE 'Season'
  END AS content_type
FROM view_summary AS vs
LEFT JOIN movie AS m
  ON vs.movie_id = m.id
LEFT JOIN season AS s
  ON vs.season_id = s.id
CROSS JOIN AvgViews AS av
WHERE
  vs.duration = 'SEMI_ANNUALLY' AND vs.start_date >= '2024-01-01' AND vs.end_date <= '2024-06-30'
  AND (
    (vs.movie_id IS NOT NULL AND vs.views > av.avg_movie_views)
    OR (vs.season_id IS NOT NULL AND vs.views > av.avg_season_views)
  )
ORDER BY
  content_type,
  vs.views DESC;
```

---

### 問題 38: 各月にリリースされた映画の数と、その月までのリリースされた映画の累積数

2024 年 1 月から 6 月までの各月において、その月にリリースされた映画の数と、その月までのリリースされた映画の累積数を取得してください。

**回答:**

```sql
WITH MonthlyMovieReleases AS (
  SELECT
    TO_CHAR(release_date, 'YYYY-MM') AS release_month,
    COUNT(id) AS monthly_releases
  FROM movie
  WHERE
    release_date >= '2024-01-01' AND release_date <= '2024-06-30'
  GROUP BY
    release_month
)
SELECT
  release_month,
  monthly_releases,
  SUM(monthly_releases) OVER (ORDER BY release_month) AS cumulative_releases
FROM MonthlyMovieReleases
ORDER BY
  release_month;
```

---

### 問題 39: 各 TV 番組のシーズンごとの`view_rank`と、その TV 番組内で最も高い`view_rank`の差

各 TV 番組について、各シーズンの`view_rank`と、その TV 番組内で最も高い`view_rank`（数値が小さいほど高い）との差を計算してください。
（2024 年上半期データを使用）

**回答:**

```sql
WITH RankedSeasonViews AS (
  SELECT
    t.title AS tv_show_title,
    s.title AS season_title,
    vs.view_rank,
    MIN(vs.view_rank) OVER (PARTITION BY t.id) AS min_view_rank_in_tv_show
  FROM tv_show AS t
  JOIN season AS s
    ON t.id = s.tv_show_id
  JOIN view_summary AS vs
    ON s.id = vs.season_id
  WHERE
    vs.duration = 'SEMI_ANNUALLY' AND vs.start_date >= '2024-01-01' AND vs.end_date <= '2024-06-30' AND vs.view_rank IS NOT NULL
)
SELECT
  tv_show_title,
  season_title,
  view_rank,
  min_view_rank_in_tv_show,
  (view_rank - min_view_rank_in_tv_show) AS rank_difference_from_best
FROM RankedSeasonViews
ORDER BY
  tv_show_title,
  view_rank;
```

---

### 問題 40: 視聴回数が偶数である映画と、視聴回数が奇数である TV 番組シーズン

2024 年上半期（`duration = 'SEMI_ANNUALLY'`）のデータで、視聴回数（`views`）が偶数である映画のタイトルと、視聴回数が奇数である TV 番組（シーズン）のタイトルをそれぞれ取得し、コンテンツの種類（'Movie'または'Season'）も表示して結合してください。

**回答:**

```sql
(
  SELECT
    m.title AS content_title,
    vs.views,
    'Movie' AS content_type
  FROM movie AS m
  JOIN view_summary AS vs
    ON m.id = vs.movie_id
  WHERE
    vs.duration = 'SEMI_ANNUALLY' AND vs.start_date >= '2024-01-01' AND vs.end_date <= '2024-06-30'
    AND vs.views % 2 = 0 -- 偶数
)
UNION ALL
(
  SELECT
    s.title AS content_title,
    vs.views,
    'Season' AS content_type
  FROM season AS s
  JOIN view_summary AS vs
    ON s.id = vs.season_id
  WHERE
    vs.duration = 'SEMI_ANNUALLY' AND vs.start_date >= '2024-01-01' AND vs.end_date <= '2024-06-30'
    AND vs.views % 2 <> 0 -- 奇数
)
ORDER BY
  content_type,
  views DESC;
```

**ヒント:** `%`演算子で剰余を計算できます。

---

### 問題 41: 各月の`cumulative_weeks_in_top10`の合計と、その前月との差（映画のみ）

2024 年 1 月から 6 月までの各月における映画の`cumulative_weeks_in_top10`の合計と、その前月との差を計算してください。最初の月は NULL を表示してください。

**回答:**

```sql
WITH MonthlyMovieCumulativeWeeks AS (
  SELECT
    TO_CHAR(vs.start_date, 'YYYY-MM') AS month,
    SUM(vs.cumulative_weeks_in_top10) AS current_month_cumulative_weeks
  FROM view_summary AS vs
  WHERE
    vs.movie_id IS NOT NULL AND vs.start_date >= '2024-01-01' AND vs.end_date <= '2024-06-30'
  GROUP BY
    month
)
SELECT
  month,
  current_month_cumulative_weeks,
  LAG(current_month_cumulative_weeks, 1) OVER (ORDER BY month) AS previous_month_cumulative_weeks,
  (current_month_cumulative_weeks - LAG(current_month_cumulative_weeks, 1, 0) OVER (ORDER BY month)) AS difference_from_previous_month
FROM MonthlyMovieCumulativeWeeks
ORDER BY
  month;
```

---

### 問題 42: ランタイムが短い映画トップ 5 と、ランタイムが長い TV 番組シーズンワースト 5

ランタイム（`runtime`）が最も短い映画トップ 5 のタイトルとランタイム、およびランタイムが最も長い TV 番組シーズンワースト 5（つまり短い方から 5 つ）のタイトルとランタイムをそれぞれ取得し、一つの結果セットとして結合してください。コンテンツの種類（'Movie' or 'Season'）も表示してください。

**回答:**

```sql
(
  SELECT
    title,
    runtime,
    'Movie' AS content_type,
    ROW_NUMBER() OVER (ORDER BY runtime ASC) AS rn
  FROM movie
  WHERE
    runtime IS NOT NULL
  LIMIT 5
)
UNION ALL
(
  SELECT
    s.title,
    s.runtime,
    'Season' AS content_type,
    ROW_NUMBER() OVER (ORDER BY s.runtime DESC) AS rn -- TV番組シーズンは長い方からワースト5、つまり短い方から5つ
  FROM season AS s
  WHERE
    s.runtime IS NOT NULL
  ORDER BY
    s.runtime ASC
  LIMIT 5
)
ORDER BY
  content_type,
  runtime ASC;
```

---

### 問題 43: 各 TV 番組の最も古いリリース日のシーズンと、そのシーズンのリリースから現在までの経過日数

各 TV 番組について、最も古いリリース日を持つシーズンのタイトルとリリース日、そしてそのリリース日から 2024 年 6 月 30 日までの経過日数（日数）を取得してください。

**回答:**

```sql
WITH EarliestSeason AS (
  SELECT
    tv_show_id,
    MIN(release_date) AS earliest_release_date
  FROM season
  GROUP BY
    tv_show_id
)
SELECT
  t.title AS tv_show_title,
  s.title AS season_title,
  s.release_date,
  ('2024-06-30' :: DATE - s.release_date) AS days_since_earliest_season_release
FROM tv_show AS t
JOIN season AS s
  ON t.id = s.tv_show_id
JOIN EarliestSeason AS es
  ON t.id = es.tv_show_id AND s.release_date = es.earliest_release_date
ORDER BY
  t.title;
```

---

### 問題 44: 2024 年上半期にトップ 10 入りした映画の平均ランタイムと、トップ 10 入りしなかった映画の平均ランタイム

2024 年上半期（`duration = 'SEMI_ANNUALLY'`）のデータで、`view_rank`が 10 以内に入った映画の平均ランタイムと、10 以内に入らなかった映画の平均ランタイムをそれぞれ取得してください。

**回答:**

```sql
SELECT
  'Top 10 Movies' AS category,
  AVG(m.runtime) AS average_runtime
FROM movie AS m
JOIN view_summary AS vs
  ON m.id = vs.movie_id
WHERE
  vs.duration = 'SEMI_ANNUALLY' AND vs.start_date >= '2024-01-01' AND vs.end_date <= '2024-06-30'
  AND vs.view_rank <= 10
UNION ALL
SELECT
  'Non-Top 10 Movies' AS category,
  AVG(m.runtime) AS average_runtime
FROM movie AS m
WHERE
  m.id NOT IN (
    SELECT
      movie_id
    FROM view_summary
    WHERE
      duration = 'SEMI_ANNUALLY' AND start_date >= '2024-01-01' AND end_date <= '2024-06-30' AND view_rank <= 10 AND movie_id IS NOT NULL
  ) AND m.runtime IS NOT NULL;
```

---

### 問題 45: 各 TV 番組のシーズンごとのリリース日と、その TV 番組内でリリース日順に何番目のシーズンか

各 TV 番組について、各シーズンのリリース日と、その TV 番組内でリリース日が早い順に何番目のシーズンであるか（`season_number`ではない）を取得してください。

**回答:**

```sql
SELECT
  t.title AS tv_show_title,
  s.title AS season_title,
  s.release_date,
  ROW_NUMBER() OVER (PARTITION BY t.id ORDER BY s.release_date ASC) AS release_order_within_tv_show
FROM tv_show AS t
JOIN season AS s
  ON t.id = s.tv_show_id
ORDER BY
  t.title,
  release_order_within_tv_show;
```

---

### 問題 46: 2024 年上半期に視聴時間（`hours_viewed`）が合計で 1 億時間を超えた映画と TV 番組（シーズン）

2024 年上半期（`duration = 'SEMI_ANNUALLY'`）のデータで、視聴時間（`hours_viewed`）の合計が 1 億時間を超えた映画と TV 番組（シーズン）のタイトル、およびその合計視聴時間を取得してください。コンテンツの種類（'Movie' or 'Season'）も表示してください。

**回答:**

```sql
SELECT
  content_title,
  content_type,
  SUM(hours_viewed) AS total_hours_viewed
FROM (
  SELECT
    m.title AS content_title,
    vs.hours_viewed,
    'Movie' AS content_type
  FROM movie AS m
  JOIN view_summary AS vs
    ON m.id = vs.movie_id
  WHERE
    vs.duration = 'SEMI_ANNUALLY' AND vs.start_date >= '2024-01-01' AND vs.end_date <= '2024-06-30'
  UNION ALL
  SELECT
    s.title AS content_title,
    vs.hours_viewed,
    'Season' AS content_type
  FROM season AS s
  JOIN view_summary AS vs
    ON s.id = vs.season_id
  WHERE
    vs.duration = 'SEMI_ANNUALLY' AND vs.start_date >= '2024-01-01' AND vs.end_date <= '2024-06-30'
) AS combined_views
GROUP BY
  content_title,
  content_type
HAVING
  SUM(hours_viewed) > 100000000
ORDER BY
  total_hours_viewed DESC;
```

---

### 問題 47: 各月の視聴回数（`views`）と、その月における最大視聴回数と最小視聴回数（映画のみ）

2024 年 1 月から 6 月までの各月において、映画の総視聴回数（`views`）と、その月に記録された映画の最大視聴回数、最小視聴回数を取得してください。

**回答:**

```sql
SELECT
  TO_CHAR(vs.start_date, 'YYYY-MM') AS month,
  SUM(vs.views) AS total_monthly_views,
  MAX(vs.views) AS max_monthly_views,
  MIN(vs.views) AS min_monthly_views
FROM view_summary AS vs
WHERE
  vs.movie_id IS NOT NULL AND vs.start_date >= '2024-01-01' AND vs.end_date <= '2024-06-30'
GROUP BY
  month
ORDER BY
  month;
```

---

### 問題 48: グローバルで利用可能な TV 番組と、そうでない TV 番組の総視聴時間（シーズン単位）の比較

グローバルで利用可能（`available_globally = TRUE`）な TV 番組の全シーズンの総視聴時間と、そうでない TV 番組の全シーズンの総視聴時間をそれぞれ取得して比較してください。
（2024 年上半期データを使用）

**回答:**

```sql
SELECT
  CASE
    WHEN t.available_globally = TRUE
    THEN 'Globally Available TV Shows'
    ELSE 'Not Globally Available TV Shows'
  END AS category,
  SUM(vs.hours_viewed) AS total_hours_viewed
FROM tv_show AS t
JOIN season AS s
  ON t.id = s.tv_show_id
JOIN view_summary AS vs
  ON s.id = vs.season_id
WHERE
  vs.duration = 'SEMI_ANNUALLY' AND vs.start_date >= '2024-01-01' AND vs.end_date <= '2024-06-30'
GROUP BY
  category
ORDER BY
  category;
```

---

### 問題 49: 2024 年上半期にトップ 10 にランクインした回数が最も多い映画

2024 年上半期（`duration = 'WEEKLY'`）のデータで、`view_rank`が 10 以内に入った回数が最も多い映画のタイトルと、その回数を取得してください。

**回答:**

```sql
SELECT
  m.title,
  COUNT(vs.id) AS top10_count
FROM movie AS m
JOIN view_summary AS vs
  ON m.id = vs.movie_id
WHERE
  vs.duration = 'WEEKLY' AND vs.start_date >= '2024-01-01' AND vs.end_date <= '2024-06-30'
  AND vs.view_rank <= 10
GROUP BY
  m.title
ORDER BY
  top10_count DESC
LIMIT 1;
```

---

### 問題 50: 映画と TV 番組（シーズン）全体で、最も視聴時間が多いコンテンツと最も視聴回数が多いコンテンツ

全映画と全シーズンを合わせた中で、2024 年上半期（`duration = 'SEMI_ANNUALLY'`）のデータで最も視聴時間（`hours_viewed`）が多かったコンテンツと、最も視聴回数（`views`）が多かったコンテンツのタイトル、値、コンテンツタイプをそれぞれ取得してください。

**回答:**

```sql
WITH CombinedContent AS (
  SELECT
    m.title AS content_title,
    vs.hours_viewed,
    vs.views,
    'Movie' AS content_type
  FROM movie AS m
  JOIN view_summary AS vs
    ON m.id = vs.movie_id
  WHERE
    vs.duration = 'SEMI_ANNUALLY' AND vs.start_date >= '2024-01-01' AND vs.end_date <= '2024-06-30'
  UNION ALL
  SELECT
    s.title AS content_title,
    vs.hours_viewed,
    vs.views,
    'Season' AS content_type
  FROM season AS s
  JOIN view_summary AS vs
    ON s.id = vs.season_id
  WHERE
    vs.duration = 'SEMI_ANNUALLY' AND vs.start_date >= '2024-01-01' AND vs.end_date <= '2024-06-30'
), MaxHoursViewed AS (
  SELECT
    content_title,
    hours_viewed,
    content_type
  FROM CombinedContent
  ORDER BY
    hours_viewed DESC
  LIMIT 1
), MaxViews AS (
  SELECT
    content_title,
    views,
    content_type
  FROM CombinedContent
  ORDER BY
    views DESC
  LIMIT 1
)
SELECT
  content_title,
  hours_viewed AS value,
  content_type,
  'Max Hours Viewed' AS metric
FROM MaxHoursViewed
UNION ALL
SELECT
  content_title,
  views AS value,
  content_type,
  'Max Views' AS metric
FROM MaxViews;
```
