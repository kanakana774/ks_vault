# **SQL 基礎：複数テーブル操作とサブクエリ**

## **導入：複数テーブル操作の必要性**

データベースを設計する際、データの重複を避け、整合性を保つために、関連するデータを複数のテーブルに分けて格納することが一般的です。これを「正規化」と呼びます。

正規化の必要性 (概念):  
データを一つの大きなテーブルにまとめてしまうと、以下のような問題が発生しやすくなります。

- **データの重複 (Redundancy)**: 同じ情報が複数の場所に存在すると、ストレージを無駄にするだけでなく、更新時の不整合（例: 顧客の住所変更があった際、複数のレコードを修正し忘れる）を引き起こす可能性があります。
- **データの不整合 (Inconsistency)**: 重複するデータ間で内容が異なる状態が発生しやすくなり、信頼性が低下します。
- **更新異常 (Update Anomaly)**: データの更新、挿入、削除が複雑になり、意図しない結果を招くことがあります。

正規化は、これらの問題を最小限に抑え、データベースのデータが論理的かつ効率的に格納されるようにするプロセスです。具体的には、関連するデータを分離し、テーブル間の関係を外部キーで定義することで、データの重複を減らし、整合性を高めます。

しかし、分析やアプリケーションの目的によっては、これらの分散されたデータを組み合わせて取得する必要があります。

ここで、複数のテーブルを結合する\*\*JOIN**、複数のクエリ結果を縦に結合する**集合演算\*\*、そしてクエリの中に別のクエリを埋め込む**サブクエリ**が非常に重要になります。

## **テーブルの結合：JOIN (実務での重要度：非常に高)**

JOIN 句は、共通の列（キー）を持つ複数のテーブルの行を結合し、新しい結果セットを作成するために使用します。

### **結合の基本**

データは通常、以下のような形で複数のテーブルに分かれて格納されています。

- **products テーブル**: product_id, product_name, price, category_id
- **categories テーブル**: category_id, category_name

products テーブルの category_id は categories テーブルの category_id を参照する**外部キー**となっています。これにより、商品がどのカテゴリに属するかを関連付けることができます。

### **INNER JOIN：内部結合**

両方のテーブルで結合条件に一致する行のみを返します。最も一般的な結合の種類です。

#### **基本構文**

```SQL
SELECT 列リスト
FROM テーブル 1
INNER JOIN テーブル 2 ON 結合条件;
```

#### **例: 商品名とカテゴリ名を取得**

**事前データ:**

products テーブル:

| product_id | product_name | price   | category_id |
| :--------- | :----------- | :------ | :---------- |
| 1          | Laptop       | 1200.00 | 1           |
| 2          | Mouse        | 25.50   | 1           |
| 3          | Keyboard     | 75.00   | 1           |
| 4          | Monitor      | 300.00  | NULL        |
| 5          | Webcam       | 50.00   | 2           |

categories テーブル:

| category_id | category_name |
| :---------- | :------------ |
| 1           | Electronics   |
| 2           | Peripherals   |
| 3           | Mobile        |

**SQL:**

```SQL
SELECT
 p.product_name,
 p.price,
 c.category_name
FROM
 products AS p
INNER JOIN
 categories AS c ON p.category_id = c.category_id;
```

**実行結果:**

| product_name | price   | category_name |
| :----------- | :------ | :------------ |
| Laptop       | 1200.00 | Electronics   |
| Mouse        | 25.50   | Electronics   |
| Keyboard     | 75.00   | Electronics   |
| Webcam       | 50.00   | Peripherals   |

このクエリでは、products テーブルの category_id と categories テーブルの category_id が一致する行のみが結合されます。products テーブルの Monitor（category_id が NULL）は、categories テーブルに一致する行がないため結果に含まれません。

### **LEFT JOIN / RIGHT JOIN：外部結合**

片方のテーブルの全ての行を保持し、もう片方のテーブルから一致する行があれば結合します。一致する行がない場合は、結合先のテーブルの列は NULL になります。

- **LEFT JOIN (または LEFT OUTER JOIN)**: FROM 句で指定した**左側のテーブル**の全ての行を返します。
- **RIGHT JOIN (または RIGHT OUTER JOIN)**: JOIN 句で指定した**右側のテーブル**の全ての行を返します。

#### **基本構文**

```SQL
-- LEFT JOIN
SELECT 列リスト
FROM テーブル 1
LEFT JOIN テーブル 2 ON 結合条件;

-- RIGHT JOIN
SELECT 列リスト
FROM テーブル 1
RIGHT JOIN テーブル 2 ON 結合条件;
```

#### **例: 全ての商品と、それに紐づくカテゴリ名を取得（カテゴリが未登録の商品も含む）**

事前データ:  
（INNER JOIN の例と同じ products と categories テーブルを使用）  
**SQL:**

```SQL
SELECT
 p.product_name,
 p.price,
 c.category_name
FROM
 products AS p
LEFT JOIN
 categories AS c ON p.category_id = c.category_id;
```

**実行結果:**

| product_name | price   | category_name |
| :----------- | :------ | :------------ |
| Laptop       | 1200.00 | Electronics   |
| Mouse        | 25.50   | Electronics   |
| Keyboard     | 75.00   | Electronics   |
| Monitor      | 300.00  | NULL          |
| Webcam       | 50.00   | Peripherals   |

この例では、products テーブルの Monitor のように category_id が NULL である商品や、categories テーブルに存在しない category_id が設定されている商品でも、その行は結果に含まれ、category_name は NULL になります。

RIGHT JOIN は LEFT JOIN の左右を逆にしたものであり、LEFT JOIN で書き換え可能です。例えば、上記の LEFT JOIN は以下のように RIGHT JOIN で同じ結果を得られます。

```SQL
SELECT
 p.product_name,
 p.price,
 c.category_name
FROM
 categories AS c -- カテゴリテーブルを左に持ってきた
RIGHT JOIN
 products AS p ON p.category_id = c.category_id; -- プロダクトテーブルを右に持ってきた
```

そのため、実務では LEFT JOIN が使われることが多いです。

### **FULL OUTER JOIN：完全外部結合 (実務での重要度：中)**

両方のテーブルの全ての行を返します。結合条件に一致する行がない場合は、対応するテーブルの列は NULL になります。

#### **基本構文**

```SQL
SELECT 列リスト
FROM テーブル 1
FULL OUTER JOIN テーブル 2 ON 結合条件;
```

#### **例: 全ての商品と全てのカテゴリを結合（双方に一致しないものも含む）**

もし products テーブルに存在しないカテゴリ ID を持つ商品があったり、categories テーブルにあるが商品が一つもないカテゴリがあったりする場合に有効です。

**事前データ:**

products テーブル:

| product_id | product_name | price   | category_id |
| :--------- | :----------- | :------ | :---------- |
| 1          | Laptop       | 1200.00 | 1           |
| 2          | Mouse        | 25.50   | 1           |
| 3          | Keyboard     | 75.00   | 1           |
| 4          | Monitor      | 300.00  | NULL        |
| 5          | Webcam       | 50.00   | 2           |

categories テーブル:

| category_id | category_name |
| :---------- | :------------ |
| 1           | Electronics   |
| 2           | Peripherals   |
| 3           | Mobile        |
| 4           | Books         |

**SQL:**

```SQL
SELECT
 p.product_name,
 c.category_name
FROM
 products AS p
FULL OUTER JOIN
 categories AS c ON p.category_id = c.category_id;
```

**実行結果:**

| product_name | category_name |
| :----------- | :------------ |
| Laptop       | Electronics   |
| Mouse        | Electronics   |
| Keyboard     | Electronics   |
| Webcam       | Peripherals   |
| Monitor      | NULL          |
| NULL         | Mobile        |
| NULL         | Books         |

この結果では、Monitor のようにカテゴリに紐づかない商品（category_name が NULL）も、Mobile や Books のように商品が一つもないカテゴリ（product_name が NULL）も表示されます。

#### **💡 コラム: FULL OUTER JOIN がサポートされない RDBMS (MySQL の例)**

MySQL などの一部の RDBMS では FULL OUTER JOIN が直接サポートされていません。その場合、以下のように LEFT JOIN と RIGHT JOIN を UNION ALL で組み合わせることで同等の結果を得られます。

```SQL
-- MySQL などで FULL OUTER JOIN の代替として使用
SELECT
 p.product_name,
 c.category_name
FROM
 products AS p
LEFT JOIN
 categories AS c ON p.category_id = c.category_id
UNION ALL
SELECT
 p.product_name,
 c.category_name
FROM
 products AS p
RIGHT JOIN
 categories AS c ON p.category_id = c.category_id
WHERE
 p.product_id IS NULL; -- LEFT JOIN で得られない右側の NULL 行のみを抽出
```

### **自己結合 (Self-Join) (実務での重要度：中)**

同じテーブルを 2 回以上、異なるエイリアス（別名）で JOIN することです。テーブル内の行同士の関係性を表現したい場合によく使われます。

#### **例: 従業員とその上司の名前を取得**

employees テーブルが employee_id と employee_name、そして上司の employee_id を指す manager_id 列を持っていると仮定します。

**事前データ:**

employees テーブル:

| employee_id | employee_name | manager_id |
| :---------- | :------------ | :--------- |
| 1           | Alice         | NULL       |
| 2           | Bob           | 1          |
| 3           | Charlie       | 1          |
| 4           | David         | 2          |

**SQL:**

```SQL
with employees as (
   select 1 as employee_id	, 'Alice' as employee_name, NULL as manager_id  union all
   select 2 as employee_id	, 'Bob'as employee_name, 1 as manager_id union all
   select 3 as employee_id	, 'Charlie' as employee_name,	1 as manager_id union all
   select 4 as employee_id	, 'David' as employee_name, 2 as manager_id
)

SELECT
 e.employee_name AS employee, -- 従業員自身の名前
 m.employee_name AS manager -- 上司の名前
FROM
 employees AS e
LEFT JOIN -- 上司がいない従業員（CEO など）も表示するため LEFT JOIN
 employees AS m ON e.manager_id = m.employee_id;
```

**実行結果:**

| employee | manager |
| :------- | :------ |
| Alice    | NULL    |
| Bob      | Alice   |
| Charlie  | Alice   |
| David    | Bob     |

このクエリでは、employees テーブルを e（従業員）と m（上司）という 2 つの論理的なテーブルとして扱い、それぞれの employee_id と manager_id を結合しています。Alice は manager_id が NULL なので、LEFT JOIN によって manager が NULL で表示されます。

### **ON 句と WHERE 句の違い (実務での重要度：非常に高)**

JOIN における ON 句と WHERE 句は、どちらも条件を指定しますが、その**評価されるタイミングと効果**が大きく異なります。この違いは特に INNER JOIN 以外の結合（LEFT/RIGHT/FULL OUTER JOIN）で顕著になります。

| 特徴                 | ON 句                                      | WHERE 句                                                            |
| :------------------- | :----------------------------------------- | :------------------------------------------------------------------ |
| **評価タイミング**   | **結合前**（テーブルを結合する際の条件）   | **結合後**（結合された結果セット全体に対するフィルタリング）        |
| **効果**             | 結合の方法や結合対象となる行を定義する     | 結合された結果から、最終的に表示する行を絞り込む                    |
| **外部結合での違い** | 一致しない行も元のテーブルの側は保持される | 一致しない行（NULL になっている行）も条件に合致しなければ削除される |

#### **例: LEFT JOIN における違い**

products テーブルと categories テーブルを考えます。

**シナリオ:** カテゴリ ID が 1 の商品、およびカテゴリに紐づかない全ての商品を表示したい。

事前データ:  
（INNER JOIN の例と同じ products と categories テーブルを使用）

| product_id | product_name | price   | category_id |
| :--------- | :----------- | :------ | :---------- |
| 1          | Laptop       | 1200.00 | 1           |
| 2          | Mouse        | 25.50   | 1           |
| 3          | Keyboard     | 75.00   | 1           |
| 4          | Monitor      | 300.00  | NULL        |
| 5          | Webcam       | 50.00   | 2           |

categories テーブル:

| category_id | category_name |
| :---------- | :------------ |
| 1           | Electronics   |
| 2           | Peripherals   |
| 3           | Mobile        |

```SQL
with p as(
select 1 as product_id, 'Laptop' as product_name, 1200.00 as price, 1    as category_id union all
select 2 as product_id, 'Mouse' as product_name, 25.50   as price, 1    as category_id union all
select 3 as product_id, 'Keyboard' as product_name, 75.00   as price, 1    as category_id union all
select 4 as product_id, 'Monitor' as product_name, 300.00  as price, NULL as category_id union all
select 5 as product_id, 'Webcam' as product_name, 50.00   as price, 2    as category_id
), c as(
select 1 as category_id, 'Electronics' as category_name union all
select 2 as category_id, 'Peripherals' as category_name union all
select 3 as category_id, 'Mobile     ' as category_name
)
```

1. **ON 句で条件を指定した場合:**  
    **SQL:**

   ```SQL
   SELECT
    p.product_name,
    c.category_name,
    p.category_id
   FROM
    products AS p
   LEFT JOIN
    categories AS c ON p.category_id = c.category_id AND c.category_id = 1;
   ```

   **実行結果:**

   | product_name | category_name | category_id |
   | :----------- | :------------ | :---------- |
   | Laptop       | Electronics   | 1           |
   | Mouse        | Electronics   | 1           |
   | Keyboard     | Electronics   | 1           |
   | Monitor      | NULL          | NULL        |
   | Webcam       | NULL          | 2           |

2. 説明:
   products テーブルの全ての行が保持されます。categories テーブルからは、category_id = 1 の行だけが結合の対象になります。products.category_id が 1 ではない商品（例: Webcam category_id=2）や、products.category_id が NULL の商品（例: Monitor category_id=NULL）の場合、categories テーブルとの結合条件 c.category_id = 1 を満たさないため、c.category_name は NULL になります。つまり、左側のテーブルの行は維持され、右側のテーブル側の結合条件が満たされない場合は NULL が埋められます。

3. **WHERE 句で条件を指定した場合:**
   **SQL:**

   ```SQL
   SELECT
    p.product_name,
    c.category_name,
    p.category_id
   FROM
    products AS p
   LEFT JOIN
    categories AS c ON p.category_id = c.category_id
   WHERE
    c.category_id = 1;
   ```

   **実行結果:**

   | product_name | category_name | category_id |
   | :----------- | :------------ | :---------- |
   | Laptop       | Electronics   | 1           |
   | Mouse        | Electronics   | 1           |
   | Keyboard     | Electronics   | 1           |

4. 説明:
   まず products と categories が category_id で LEFT JOIN されます。この時点では Monitor と Webcam も category_name が NULL として含まれます。その後、結合された結果セット全体に対して c.category_id = 1 という条件が適用されます。これにより、categories テーブルからの結合結果が NULL であった行（Monitor）や、category_id が 1 以外だった行（Webcam）は、WHERE c.category_id = 1 の条件を満たさないため、結果セットから除外されます。これは実質的に INNER JOIN の結果と同じになってしまうことがよくあります。

この違いを理解することは、複雑なクエリを作成する上で非常に重要です。ON 句は「結合の仕方を定義する」ために、WHERE 句は「結合された結果を最終的にフィルタリングする」ために使われます。

