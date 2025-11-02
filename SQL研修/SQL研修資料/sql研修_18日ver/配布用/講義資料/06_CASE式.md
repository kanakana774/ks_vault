# **SQL 基礎：高度な SELECT**

## **導入：より複雑なデータ操作のために**

これまでの章では、データの追加・更新・削除の基本、単一テーブル内での集計やフィルタリング、そして複数テーブルの結合方法を学びました。本章では、SQL のより高度な機能に焦点を当てます。

## **条件分岐：CASE 式**

CASE 式は、SQL クエリ内で条件分岐ロジックを実装するために使用されます。プログラミング言語における if-else if-else 文や switch-case 文に似ています。SELECT 句で新しい列を作成したり、WHERE 句で条件を指定したり、ORDER BY 句で並び替え順を制御したりと、様々な場所で利用できます。

### **基本構文**

CASE 式には、主に 2 つの形式があります。

#### **1\. シンプル CASE 式 (Simple CASE Expression)**

特定の列の値に基づいて条件分岐する場合に使用します。

```SQL
CASE 列名
 WHEN 値 1 THEN 結果 1
 WHEN 値 2 THEN 結果 2
 ...
 [ELSE デフォルト結果]
END
```

#### **2\. 検索 CASE 式 (Searched CASE Expression)**

複数の条件式（比較演算子や論理演算子など）に基づいて条件分岐する場合に使用します。より柔軟な条件指定が可能です。

```SQL
CASE
 WHEN 条件式 1 THEN 結果 1
 WHEN 条件式 2 THEN 結果 2
 ...
 [ELSE デフォルト結果]
END
```

- ELSE 句は省略可能ですが、指定しなかった場合、どの WHEN 条件にも一致しない行の結果は NULL になります。
- 最初の TRUE と評価された WHEN 句に対応する結果が返され、それ以降の WHEN 句は評価されません。**WHEN 句の記述順序は非常に重要です。**

### **例 1: 条件分岐で値を変換（評価の付与）**

学生の点数に基づいて、'優', '良', '可', '不可'の評価を付与します。

**事前データ:**

students テーブル:

| student_id | name    | score |
| :--------- | :------ | :---- |
| 1          | Alice   | 95    |
| 2          | Bob     | 78    |
| 3          | Charlie | 62    |
| 4          | David   | 40    |
| 5          | Eve     | NULL  |

**SQL:**

```SQL
SELECT
 name,
 score,
 CASE
  WHEN score >= 90 THEN '優'
  WHEN score >= 80 THEN '良'
  WHEN score >= 60 THEN '可'
  ELSE '不可' -- どの条件にも合致しない場合（NULL も含む）
 END AS grade
FROM
 students;
```

**実行結果:**

| name    | score | grade |
| :------ | :---- | :---- |
| Alice   | 95    | 優    |
| Bob     | 78    | 良    |
| Charlie | 62    | 可    |
| David   | 40    | 不可  |
| Eve     | NULL  | 不可  |

説明:  
CASE 式は、WHEN 句の条件式を上から順に評価します。最初に TRUE と評価された WHEN 句に対応する結果が返され、それ以降の WHEN 句は評価されません。  
score が NULL の Eve の行では、score \>= 90 も score \>= 80 も score \>= 60 もすべて FALSE と評価されるため（NULL との比較結果は NULL となり、WHERE 句などでは FALSE 扱いされる）、最終的に ELSE 句の'不可'が適用されます。CASE 式において、NULL との比較結果は常に UNKNOWN となり、UNKNOWN は WHEN 句では FALSE と同じように扱われ、次の WHEN 句または ELSE 句に処理が移ります。

### **例 2: クロス集計（勤怠区分ごとの人数を列化）** ← これは省略しよう（問題 3 がクロス集計なのでそれも飛ばしてもらう）

CASE 式を集計関数と組み合わせることで、テーブルの行データを列データに変換する\*\*クロス集計（ピボット）\*\*のような操作が可能です。これはレポート作成で非常に役立ちます。

**事前データ:**

attendance テーブル:

| employee_id | attendance_date | status |
| :---------- | :-------------- | :----- |
| 101         | '2023-10-26'    | 出勤   |
| 102         | '2023-10-26'    | 欠勤   |
| 103         | '2023-10-26'    | 出勤   |
| 101         | '2023-10-27'    | 早退   |
| 102         | '2023-10-27'    | 出勤   |
| 103         | '2023-10-27'    | 欠勤   |

**SQL (CASE 式と SUM/COUNT を使用):**

```SQL
SELECT
 attendance_date,
 COUNT(CASE WHEN status = '出勤' THEN 1 END) AS 出勤者数_COUNT,
 COUNT(CASE WHEN status = '欠勤' THEN 1 END) AS 欠勤者数_COUNT,
 COUNT(CASE WHEN status = '早退' THEN 1 END) AS 早退者数_COUNT,
 SUM(CASE WHEN status = '出勤' THEN 1 ELSE 0 END) AS 出勤者数_SUM,
 SUM(CASE WHEN status = '欠勤' THEN 1 ELSE 0 END) AS 欠勤者数_SUM,
 SUM(CASE WHEN status = '早退' THEN 1 ELSE 0 END) AS 早退者数_SUM
FROM
 attendance
GROUP BY
 attendance_date
ORDER BY
 attendance_date;
```

**実行結果:**

| attendance_date | 出勤者数\_COUNT | 出勤者数\_SUM | 欠勤者数\_COUNT | 欠勤者数\_SUM | 早退者数\_COUNT | 早退者数\_SUM |
| :-------------- | :-------------- | :------------ | :-------------- | :------------ | :-------------- | :------------ |
| 2023-10-26      | 2               | 2             | 1               | 1             | 0               | 0             |
| 2023-10-27      | 1               | 1             | 1               | 1             | 1               | 1             |

**説明:**

- COUNT(CASE WHEN status \= '出勤' THEN 1 END)の挙動に注目してください。status が'出勤'の場合に 1 が返され、それ以外の場合は ELSE 句がないため NULL が返されます。COUNT(列名)は NULL を無視するため、結果的に'出勤'の行数のみがカウントされます。
- SUM(CASE WHEN status \= '出勤' THEN 1 ELSE 0 END)の場合、status が'出勤'でなければ 0 が返されます。SUM()関数は 0 も合計に含めるため、結果は COUNT()と同じになります。  
  どちらの書き方でも結果は同じになりますが、SUM(CASE WHEN ... THEN 1 ELSE 0 END)は、NULL を考慮せず全ての行で 0 または 1 を返すため、より明示的でわかりやすいと考える人も多いです。また、特定の DB システムによっては SUM の方が最適化されやすい場合もあります。

説明用

```SQL
with attendance as (
	select 101 as employee_id ,'2023-10-26' as attendance_date,'出勤' as status union all
	select 102 as employee_id ,'2023-10-26' as attendance_date,'欠勤' as status union all
	select 103 as employee_id ,'2023-10-26' as attendance_date,'出勤' as status union all
	select 101 as employee_id ,'2023-10-27' as attendance_date,'早退' as status union all
	select 102 as employee_id ,'2023-10-27' as attendance_date,'出勤' as status union all
	select 103 as employee_id ,'2023-10-27' as attendance_date,'欠勤' as status
)
-- ① まずはこんなデータ
-- select* from attendance

-- ② 日付とステータスで集計（日ごとの勤怠状況を集計）してみるけど、見づらい
-- SELECT
--   attendance_date,
--   status,
--   COUNT(*) AS cnt
-- FROM attendance
-- GROUP BY attendance_date, status
-- ORDER BY attendance_date, status

-- ③ クロス集計にしてみる（勤怠状況列を横に展開する）
-- SELECT
--  attendance_date,
--  COUNT(CASE WHEN status = '出勤' THEN 1 END) AS 出勤者数_COUNT,
--  COUNT(CASE WHEN status = '欠勤' THEN 1 END) AS 欠勤者数_COUNT,
--  COUNT(CASE WHEN status = '早退' THEN 1 END) AS 早退者数_COUNT,
--  SUM(CASE WHEN status = '出勤' THEN 1 ELSE 0 END) AS 出勤者数_SUM,
--  SUM(CASE WHEN status = '欠勤' THEN 1 ELSE 0 END) AS 欠勤者数_SUM,
--  SUM(CASE WHEN status = '早退' THEN 1 ELSE 0 END) AS 早退者数_SUM
-- FROM
--  attendance
-- GROUP BY
--  attendance_date
-- ORDER BY
--  attendance_date;
```

<details>
<summary>半分統計分析のだけど、クロス集計の例</summary>

```sql

-- Passengers CTE: ランダムな乗客IDと基本属性を生成
WITH Passengers AS (
    SELECT
        ROW_NUMBER() OVER (ORDER BY (SELECT NULL)) AS PassengerID,
        -- Sexを先に決定
        CASE
            WHEN (FLOOR(RANDOM() * 100) + 1)::INT % 100 < 65 THEN 'male' -- 約65%が男性
            ELSE 'female'
        END AS Sex,
        CASE
            WHEN (FLOOR(RANDOM() * 100) + 1)::INT % 100 < 25 THEN 1 -- 約25%が1等客室
            WHEN (FLOOR(RANDOM() * 100) + 1)::INT % 100 < 50 THEN 2 -- 約25%が2等客室
            ELSE 3                       -- 残りが3等客室
        END AS Pclass,
        (FLOOR(RANDOM() * 80) + 1)::INT AS Age -- 1歳から80歳まで
    FROM
        (SELECT 1 FROM GENERATE_SERIES(1, 1000)) AS a -- 1000件のダミーレコードを生成
),
-- AgeGroups CTE: Passengersの年齢に基づいて年齢層を分類 (Survived計算のために先に移動)
AgeGroups AS (
    SELECT
        p.PassengerID,
        p.Sex,
        p.Pclass,
        p.Age,
        CASE
            WHEN p.Age < 12 THEN 'Child'
            WHEN p.Age BETWEEN 12 AND 17 THEN 'Teenager'
            WHEN p.Age BETWEEN 18 AND 59 THEN 'Adult'
            ELSE 'Senior'
        END AS AgeGroup
    FROM
        Passengers p
),
-- SurvivedAdjusted CTE: Sex, Pclass, AgeGroupに基づいてSurvivedを調整
SurvivedAdjusted AS (
    SELECT
        ag.PassengerID,
        ag.Sex,
        ag.Pclass,
        ag.Age,
        ag.AgeGroup,
        CASE
            WHEN ag.Sex = 'female' THEN -- 女性の生存率
                CASE
                    WHEN ag.Pclass = 1 AND ag.AgeGroup = 'Child' AND (FLOOR(RANDOM() * 100) + 1)::INT % 100 < 95 THEN 1 -- 女性、1等客室、子供: 約95%生存
                    WHEN ag.Pclass = 1 AND (FLOOR(RANDOM() * 100) + 1)::INT % 100 < 90 THEN 1 -- 女性、1等客室、非子供: 約90%生存
                    WHEN ag.Pclass = 2 AND ag.AgeGroup = 'Child' AND (FLOOR(RANDOM() * 100) + 1)::INT % 100 < 85 THEN 1 -- 女性、2等客室、子供: 約85%生存
                    WHEN ag.Pclass = 2 AND (FLOOR(RANDOM() * 100) + 1)::INT % 100 < 75 THEN 1 -- 女性、2等客室、非子供: 約75%生存
                    WHEN ag.Pclass = 3 AND ag.AgeGroup = 'Child' AND (FLOOR(RANDOM() * 100) + 1)::INT % 100 < 60 THEN 1 -- 女性、3等客室、子供: 約60%生存
                    WHEN ag.Pclass = 3 AND (FLOOR(RANDOM() * 100) + 1)::INT % 100 < 45 THEN 1 -- 女性、3等客室、非子供: 約45%生存
                    ELSE 0 -- 上記条件に合致しない女性は死亡
                END
            WHEN ag.Sex = 'male' THEN -- 男性の生存率
                CASE
                    WHEN ag.Pclass = 1 AND ag.AgeGroup = 'Child' AND (FLOOR(RANDOM() * 100) + 1)::INT % 100 < 50 THEN 1 -- 男性、1等客室、子供: 約50%生存
                    WHEN ag.Pclass = 1 AND (FLOOR(RANDOM() * 100) + 1)::INT % 100 < 35 THEN 1 -- 男性、1等客室、非子供: 約35%生存
                    WHEN ag.Pclass = 2 AND ag.AgeGroup = 'Child' AND (FLOOR(RANDOM() * 100) + 1)::INT % 100 < 30 THEN 1 -- 男性、2等客室、子供: 約30%生存
                    WHEN ag.Pclass = 2 AND (FLOOR(RANDOM() * 100) + 1)::INT % 100 < 15 THEN 1 -- 男性、2等客室、非子供: 約15%生存
                    WHEN ag.Pclass = 3 AND ag.AgeGroup = 'Child' AND (FLOOR(RANDOM() * 100) + 1)::INT % 100 < 20 THEN 1 -- 男性、3等客室、子供: 約20%生存
                    WHEN ag.Pclass = 3 AND (FLOOR(RANDOM() * 100) + 1)::INT % 100 < 10 THEN 1 -- 男性、3等客室、非子供: 約10%生存
                    ELSE 0 -- 上記条件に合致しない男性は死亡
                END
            ELSE 0 -- その他（理論上は発生しないはず）
        END AS Survived
    FROM
        AgeGroups ag
),
-- EmbarkedPorts CTE: PassengerIDに基づいてランダムに乗船港を割り当て
EmbarkedPorts AS (
    SELECT
        sa.PassengerID,
        sa.Survived,
        sa.Pclass,
        sa.Sex,
        sa.Age,
        sa.AgeGroup,
        CASE (FLOOR(RANDOM() * 3) + 1)::INT
            WHEN 1 THEN 'S' -- Southampton
            WHEN 2 THEN 'C' -- Cherbourg
            ELSE 'Q'        -- Queenstown
        END AS Embarked
    FROM
        SurvivedAdjusted sa
)

-- 最終的な結果の選択: クロス集計に最適な形式
-- SELECT
-- PassengerID,
-- Survived,
-- Pclass,
-- Sex,
-- AgeGroup,
-- Embarked
-- FROM
-- EmbarkedPorts
-- ORDER BY
-- PassengerID;

-- 動機：生存者の特徴を調べたい（通説の検証）
-- ①pk を削除して、group by キーで集計
-- select
-- Survived,
-- Pclass,
-- Sex,
-- AgeGroup,
-- Embarked,
-- count(1) AS 合計
-- from EmbarkedPorts
-- group by Survived,Pclass,Sex,AgeGroup,Embarked

-- ② 女性の方が生存率が高かったてきな通説を見てみる
-- ,crosstab as(
-- select
-- Sex,
-- count(case when Survived=1 then 1 end) as 生存,
-- count(case when Survived=0 then 1 end) as 没
-- from EmbarkedPorts
-- group by Sex
-- )
-- select
-- Sex,
-- concat(ROUND(生存::numeric / (生存 + 没)*100,1),'%') as 生存割合,
-- concat(ROUND(没::numeric / (生存 + 没)*100,1),'%') as 没割合
-- from crosstab
-- order by 生存割合 desc

-- ③ 1 等客室の生存率が高かった説を見てみる
-- ,crosstab as(
-- select
-- Pclass,
-- count(case when Survived=1 then 1 end) as 生存,
-- count(case when Survived=0 then 1 end) as 没
-- from EmbarkedPorts
-- group by Pclass
-- )
-- select
-- Pclass,
-- concat(ROUND(生存::numeric / (生存 + 没)*100,1),'%') as 生存割合,
-- concat(ROUND(没::numeric / (生存 + 没)*100,1),'%') as 没割合
-- from crosstab
-- order by 生存割合 desc

-- ④ 客室と性別で
,crosstab as(
  select
    Sex,
    Pclass,
    count(case when Survived=1 then 1 end) as 生存,
    count(case when Survived=0 then 1 end) as 没
  from EmbarkedPorts
  group by Sex,Pclass
)
select
  Sex,
  Pclass,
  concat(ROUND(生存::numeric / (生存 + 没)*100,1),'%') as 生存割合,
  concat(ROUND(没::numeric / (生存 + 没)*100,1),'%') as 没割合
from crosstab
order by 生存割合 desc

```

</details>

#### **💡 コラム: PostgreSQL の FILTER 句を使ったクロス集計 (PostgreSQL 特有)**

PostgreSQL では、WHERE 句とは別に集計関数に対して直接フィルタリング条件を指定できる FILTER 句が提供されています。これはクロス集計において非常に簡潔で効率的な書き方として推奨されます。

**SQL (PostgreSQL の FILTER 句を使用):**

```SQL
SELECT
  attendance_date,
  COUNT(*) FILTER (WHERE status = '出勤') AS 出勤者数,
  COUNT(*) FILTER (WHERE status = '欠勤') AS 欠勤者数,
  COUNT(*) FILTER (WHERE status = '早退') AS 早退者数
FROM
  attendance
GROUP BY
  attendance_date
ORDER BY
  attendance_date;
```

**実行結果:**

| attendance_date | 出勤者数 | 欠勤者数 | 早退者数 |
| :-------------- | :------- | :------- | :------- |
| 2023-10-26      | 2        | 1        | 0        |
| 2023-10-27      | 1        | 1        | 1        |

説明:
FILTER (WHERE ...)句を使うことで、CASE 式をネストするよりも直感的に、特定の条件に合致する行のみを集計の対象とすることができます。PostgreSQL を使用する環境では、この FILTER 句を積極的に活用することが推奨されます。

### **例 3: ORDER BY 句での CASE 式の利用（カスタムソートと NULL の優先度制御）**

特定のカテゴリの商品を優先的に表示し、その後は通常の価格順に並び替えるなど、カスタムな並び替え順を定義できます。また、NULL 値を常に最後や最初に持ってくるような制御も可能です。

**事前データ:**

products テーブル:

| product_id | product_name | price   | category    |
| :--------- | :----------- | :------ | :---------- |
| 1          | Laptop       | 1200.00 | Electronics |
| 2          | Mouse        | 25.50   | Electronics |
| 3          | Keyboard     | 75.00   | Electronics |
| 4          | Monitor      | 300.00  | NULL        |
| 5          | Webcam       | 50.00   | Peripherals |
| 6          | Headset      | 150.00  | Peripherals |
| 7          | Bookshelf    | 80.00   | Home        |
| 8          | Projector    | 700.00  | NULL        |

**SQL:**

```SQL
SELECT
  product_name,
  category,
  price
FROM
  products_mst
ORDER BY
  CASE category
    WHEN 'Electronics' THEN 1
    WHEN 'Peripherals' THEN 2
    WHEN 'Home' THEN 3
    ELSE 4 -- その他のカテゴリまたは NULL
  END,
  CASE
    WHEN category IS NULL THEN 1
    ELSE 0
  END, -- NULL を最後に持ってくるための追加条件（0:NULL ではない, 1:NULL）
  price DESC; -- 同じグループ内では価格が高い順
```

**実行結果:**

| product_name | category    | price   |
| :----------- | :---------- | :------ |
| Laptop       | Electronics | 1200.00 |
| Keyboard     | Electronics | 75.00   |
| Mouse        | Electronics | 25.50   |
| Headset      | Peripherals | 150.00  |
| Webcam       | Peripherals | 50.00   |
| Bookshelf    | Home        | 80.00   |
| Projector    | NULL        | 700.00  |
| Monitor      | NULL        | 300.00  |

説明:
ORDER BY 句内の最初の CASE 式が各行に 1 から 4 の数値を割り当て、その数値の昇順で並べ替えます。
二つ目の CASE 式 CASE WHEN category IS NULL THEN 1 ELSE 0 END は、category が NULL の行には 1 を、そうでない行には 0 を返します。これにより、最初の CASE 式で ELSE 4 とされたグループ（NULL カテゴリを含む）内で、NULL の行が非 NULL の行よりも後に並ぶようになります。
最後に price DESC で、同じグループ内の行は価格が高い順に並び替えられます。
💡 実務では、NULL 値をソート順の最初や最後に持ってきたいというニーズが頻繁に発生します。このような場合に CASE WHEN 列 IS NULL THEN ... ELSE ... END が非常に役立ちます。
⇒ ソート条件をカンマ区切りで順番に書けるので、case 式で応用的な書き方ができますよと。

```SQL
SELECT
  product_name,
  category,
  price
FROM
  products_mst
ORDER BY
  CASE category
    WHEN 'Electronics' THEN 'あ'
    WHEN 'Books' THEN 'い'
    WHEN 'Home & Kitchen' THEN 'う'
    ELSE 'あ' -- その他のカテゴリまたは NULL
  END,
  CASE
    WHEN category IS NULL THEN 1
    ELSE 0
  END, -- NULL を最後に持ってくるための追加条件（0:NULL ではない, 1:NULL）
  price DESC; -- 同じグループ内では価格が高い順
```

⇒order by は同一の型であればそれを比べてソートできます。文字列を渡せば辞書順に。

### **例 4: WHERE 句での CASE 式の利用（動的フィルタリング）**

条件によってフィルタリングのロジックを変えたい場合に CASE 式を活用できます。

**シナリオ:** 在庫が多い商品（100 個以上）は価格が 500 ドルより高いものだけを表示し、それ以外の在庫数の商品は価格に関わらず表示する。

**事前データ:**

products テーブル:

| product_id | product_name | price   | stock_quantity | category    |
| :--------- | :----------- | :------ | :------------- | :---------- |
| 1          | Laptop       | 1200.00 | 150            | Electronics |
| 2          | Mouse        | 25.50   | 20             | Electronics |
| 3          | Keyboard     | 75.00   | 30             | Electronics |
| 4          | Monitor      | 300.00  | 120            | NULL        |
| 5          | Webcam       | 50.00   | 10             | Peripherals |
| 6          | Headset      | 150.00  | 200            | Peripherals |
| 7          | Bookshelf    | 80.00   | 5              | Home        |

**SQL (CASE 式を使用):**

```SQL
SELECT
  product_name,
  price,
  stock_quantity
FROM
  products_mst
WHERE
  CASE
    WHEN stock_quantity >= 100 THEN price > 500 -- 在庫が 100 以上の場合は価格が 500 より大きい
    ELSE TRUE -- それ以外の場合は常に TRUE（フィルタリングしない）
  END;
```

**実行結果:**

| product_name | price   | stock_quantity |
| :----------- | :------ | :------------- |
| Laptop       | 1200.00 | 150            |
| Mouse        | 25.50   | 20             |
| Keyboard     | 75.00   | 30             |
| Webcam       | 50.00   | 10             |
| Bookshelf    | 80.00   | 5              |

**説明:**

- Laptop (stock_quantity=150, price=1200) は stock_quantity \>= 100 に合致し、かつ price \> 500 も満たすため抽出されます。
- Monitor (stock_quantity=120, price=300) は stock_quantity \>= 100 に合致しますが、price \> 500 を満たさないため抽出されません。
- Headset (stock_quantity=200, price=150) も同様に抽出されません。
- 残りの商品は stock_quantity \>= 100 に合致しないため、ELSE TRUE の条件が適用され、price に関わらず全て抽出されます。

```SQL
SELECT
	CATEGORY,
	PRODUCT_NAME,
	PRICE,
	STOCK_QUANTITY
FROM
	PRODUCTS_MST
WHERE
	CASE
		WHEN STOCK_QUANTITY >= 100 THEN (
			CASE
				WHEN PRICE > 500 THEN (
					CASE
						WHEN CATEGORY = 'Electoronics' THEN TRUE
						ELSE FALSE
					END
				)
				ELSE FALSE
			END
		)
		ELSE TRUE
	END;
```

⇒case で表現すると複雑な条件でどんどんネストしてしまう、、、読みづらい

#### **⚠️ 実務でのアンチパターン: WHERE 句での CASE 式の利用**

WHERE 句に直接 CASE 式を使用することは可能ですが、**実務的には非推奨**とされることが多いです。その理由は以下の通りです。

1. **可読性の低下**: CASE 式が複雑になると、WHERE 句の条件が非常に読みにくくなります。
2. **パフォーマンスの低下**: CASE 式は行ごとに評価されるため、インデックスが効率的に使用されにくくなる場合があります。これにより、大量のデータを扱う際にパフォーマンスが低下する可能性があります。

推奨される書き方:
上記のシナリオは、通常、論理演算子 (AND, OR) を使って以下のように記述するのが推奨されます。

```SQL
-- 推奨される書き方（論理演算子で制御）
SELECT
  product_name,
  price,
  stock_quantity
FROM
  products
WHERE
  (stock_quantity >= 100 AND price > 500) -- 在庫 100 以上かつ価格 500 超
OR
  (stock_quantity < 100); -- または在庫 100 未満（価格は問わない）
```

この書き方の方が、条件の意図が明確で、データベースのオプティマイザもより効率的に処理を実行しやすい傾向があります。CASE 式は便利ですが、乱用するとかえってクエリの可読性やパフォーマンスを損なう可能性があるため、使用場面を慎重に選ぶことが重要です。
💡 実務の現場では、ユーザーが指定した動的な検索条件（例えば、特定のパラメータが入力されたら A の条件、されなかったら B の条件）を SQL で表現するために CASE 式を使いたくなることがあります。しかし、多くの場合、そのような動的条件はアプリケーション層で SQL を組み立てるか、COALESCE 関数や論理演算子を組み合わせることで、よりパフォーマンス良く、かつ明確に表現できます。

### **例 5: 複数の CASE 式を使用した複雑なカテゴリ分類**

複数の条件を組み合わせて、より詳細なカテゴリ分類を行う例です。

**事前データ:**

transactions テーブル:

| transaction_id | amount | item_name  |
| :------------- | :----- | :--------- |
| 1              | 1500   | Laptop     |
| 2              | 50     | Coffee     |
| 3              | 300    | Books      |
| 4              | 2000   | Smartphone |
| 5              | 10     | Pencil     |
| 6              | 120    | Dinner     |
| 7              | 700    | Tablet     |

**SQL:**

```SQL
SELECT
  transaction_id,
  item_name,
  amount,
  CASE
    WHEN amount >= 1000 THEN '高額商品' -- 最も優先度の高い条件
    WHEN item_name IN ('Laptop', 'Smartphone', 'Tablet') THEN '電子機器'
    WHEN item_name IN ('Coffee', 'Dinner') THEN '飲食費'
    WHEN amount < 100 THEN '低額文具・雑貨'
    ELSE 'その他'
  END AS classified_category
FROM
  transactions;
```

**実行結果:**

| transaction_id | item_name  | amount | classified_category |
| :------------- | :--------- | :----- | :------------------ |
| 1              | Laptop     | 1500   | 高額商品            |
| 2              | Coffee     | 50     | 飲食費              |
| 3              | Books      | 300    | その他              |
| 4              | Smartphone | 2000   | 高額商品            |
| 5              | Pencil     | 10     | 低額文具・雑貨      |
| 6              | Dinner     | 120    | 飲食費              |
| 7              | Tablet     | 700    | 電子機器            |

**説明:**

- Laptop (amount=1500) は最初の WHEN amount >= 1000 で'高額商品'に分類されます。次の WHEN item_name IN ('Laptop', 'Smartphone')には到達しません。
- Coffee (amount=50) は最初の条件を満たさず、次の WHEN item_name IN ('Coffee', 'Dinner')を満たすため'飲食費'に分類されます。
- Books (amount=300) はどの WHEN 句も満たさないため、ELSE 'その他'に分類されます。
- Smartphone (amount=2000) も Laptop と同様に最初の条件で'高額商品'に分類されます。
- Tablet (amount=700) は最初の条件を満たさず、次の WHEN item_name IN ('Laptop', 'Smartphone', 'Tablet')を満たすため'電子機器'に分類されます。

このように、CASE 式は、複数の条件を複合的に評価し、複雑なビジネスロジックを SQL 内で表現する強力な手段となります。

#### 💡 実務でよくある活用例としては、以下のようなものがあります。

- **請求明細の税区分**:
  ```SQL
  CASE WHEN product_type = 'FOOD' THEN 0.08 ELSE 0.10 END AS tax_rate
  ```
- **受注ステータスの集約**:
  ```SQL
  CASE WHEN status IN ('PENDING', 'PROCESSING') THEN '進行中' WHEN status = 'COMPLETED' THEN '完了' ELSE 'キャンセル' END AS simplified_status
  ```
- **年齢層の分類**:
  ```SQL
  CASE WHEN age < 20 THEN '10 代以下' WHEN age BETWEEN 20 AND 30 THEN '20 代' ... ELSE 'その他' END AS age_group
  ```
