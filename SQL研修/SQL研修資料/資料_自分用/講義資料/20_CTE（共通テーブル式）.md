## **CTE（共通テーブル式 \- Common Table Expressions）**

CTE は、WITH 句を使用して定義される一時的な結果セットです。複雑な SQL クエリを複数の論理的なステップに分割し、クエリの可読性とメンテナンス性を大幅に向上させることができます。CTE は、その CTE が定義された単一の SELECT, INSERT, UPDATE, DELETE 文の中でしか参照できません。

### **WITH 句：基本的な CTE の定義**

#### **基本構文**

```SQL
WITH CTE 名 AS (
	SELECT ... -- CTE の定義クエリ
)

SELECT ... -- CTE を使用したメインクエリ
FROM CTE 名
[JOIN ...]
[WHERE ...]
```

複数の CTE をカンマで区切って定義することも可能です。

```SQL
WITH CTE1 AS (
	SELECT ...
),
CTE2 AS (
	SELECT ...
FROM CTE1 -- CTE2 から CTE1 を参照することも可能
)

SELECT ...
FROM CTE2;
```

### **例 1: 複数ステップの計算を CTE で分かりやすく**

顧客ごとの合計購入金額を算出し、その中で合計金額が 1000 ドルを超える顧客だけを抽出します。

**事前データ:**

customers テーブル:

| customer_id | customer_name |
| :---------- | :------------ |
| 1           | Alice         |
| 2           | Bob           |
| 3           | Charlie       |

orders テーブル:

| order_id | customer_id | amount |
| :------- | :---------- | :----- |
| 101      | 1           | 500    |
| 102      | 2           | 700    |
| 103      | 1           | 600    |
| 104      | 3           | 300    |
| 105      | 2           | 400    |

**SQL:**

```SQL
WITH customer_total_sales AS (
  SELECT
    customer_id,
    SUM(amount) AS total_amount
  FROM
    orders
  GROUP BY
    customer_id
)
SELECT
  c.customer_name,
  cts.total_amount
FROM
  customers AS c
JOIN
  customer_total_sales AS cts ON c.customer_id = cts.customer_id
WHERE
  cts.total_amount > 1000;
```

**実行結果:**

| customer_name | total_amount |
| :------------ | :----------- |
| Alice         | 1100         |

説明:
customer_total_sales という CTE を定義することで、「顧客ごとの合計購入金額」という中間結果を明確に分離し、その結果を使って最終的なフィルタリングと結合を行っています。これにより、クエリ全体のロジックが理解しやすくなります。

#### **💡 コラム: CTE、サブクエリ、ビューの使い分けとパフォーマンス**

CTE は、**サブクエリ**や**ビュー**と似たような目的で使われますが、それぞれ特性が異なります。

| 特徴               | サブクエリ (派生テーブル)                                                  | ビュー (VIEW)                                          | CTE (共通テーブル式)                                                                      |
| :----------------- | :------------------------------------------------------------------------- | :----------------------------------------------------- | :---------------------------------------------------------------------------------------- |
| **定義場所**       | FROM 句内 (SELECT ... FROM (SELECT ...) AS alias)                          | データベースオブジェクトとして永続的に定義             | クエリの WITH 句内で一時的に定義                                                          |
| **再利用性**       | クエリ内では 1 回のみ (ネスト可能だが、同じ結果を複数回計算する可能性あり) | 複数クエリから参照可能（データベース全体で再利用）     | クエリ内でのみ複数回参照可能                                                              |
| **最適化**         | RDBMS がクエリ全体を最適化することが多いが、複雑になると難しい場合も       | RDBMS のオプティマイザが元のビュー定義を展開して最適化 | 多くの RDBMS でクエリ全体を最適化するが、一部のケースでは**実行計画のバリア**となることも |
| **パフォーマンス** | クエリの複雑さによる                                                       | 通常は良好（オプティマイザが頑張る）                   | 一般的に良好だが、特に大規模データでは注意が必要な場合も                                  |
| **メンテナンス性** | ネストが深くなると低下                                                     | 永続的な定義なので管理が必要                           | クエリのロジックを段階的に表現でき、可読性が高い                                          |

CTE のパフォーマンスに関する注意点:
特に PostgreSQL の場合、CTE はデフォルトで最適化されない（マテリアライズされる、つまり中間結果が一時的にディスクに書き込まれる）ことがあります。これにより、CTE の内部クエリが複雑で大量のデータを生成する場合、実行計画のバリアとなり、クエリ全体のパフォーマンスが低下する可能性があります。MySQL 8.0 以降や SQL Server、Oracle などでは CTE がより積極的にインライン化（中間結果が物理的に生成されずに、メインクエリに組み込まれる）される傾向があります。
したがって、CTE を使用する際は、そのパフォーマンス特性を理解し、クエリの実行計画を確認する習慣をつけることが重要です。

### **再帰 CTE（Recursive CTE） (実務での重要度：中 △)**

再帰 CTE は、階層構造やツリー構造のデータを展開するために使用されます。例えば、組織図の上下関係、部品構成表、SNS のフォロワー関係などです。

#### **基本構文**

```SQL
WITH RECURSIVE CTE 名 AS (
  -- 非再帰部分（アンカーメンバー）: 再帰の開始点となる行を選択
  SELECT ...
  UNION [ALL]
  -- 再帰部分（リカージョンメンバー）: CTE 自身を参照して次の行を生成
  SELECT ...
  FROM CTE 名 -- ここで CTE 自身を参照
  WHERE ... -- 終了条件
  )

SELECT ... -- CTE を使用したメインクエリ
FROM CTE 名;
```

- UNION（重複排除）または UNION ALL（重複保持）を使用します。パフォーマンスを考慮し、重複排除が不要な場合は UNION ALL を使用します。
- 再帰部分には、再帰を停止するための明確な終了条件が必要です。条件がない場合、無限ループに陥り、エラーになる可能性があります。

### **例 2: 階層構造の展開（従業員とその直属の上司・部下の関係）**

**事前データ:**

employees テーブル:

| employee_id | employee_name | manager_id |
| :---------- | :------------ | :--------- |
| 1           | Alice (CEO)   | NULL       |
| 2           | Bob           | 1          |
| 3           | Charlie       | 1          |
| 4           | David         | 2          |
| 5           | Eve           | 2          |
| 6           | Frank         | 3          |

**SQL:**

```SQL
WITH RECURSIVE employee_hierarchy AS (
-- アンカーメンバー: CEO (manager_id が NULL の従業員) を取得
SELECT
  employee_id,
  employee_name,
  manager_id,
  1 AS level -- 階層レベルを初期化
FROM
  employees
WHERE
  manager_id IS NULL

    UNION ALL

    -- 再帰部分: 上位の階層から次のレベルの部下を取得
    SELECT
        e.employee_id,
        e.employee_name,
        e.manager_id,
        eh.level + 1 AS level -- 階層レベルをインクリメント
    FROM
        employees AS e
    JOIN
        employee_hierarchy AS eh ON e.manager_id = eh.employee_id
    WHERE
        eh.level < 10 -- 💡実務Tips: 無限ループ回避とパフォーマンスのために最大階層制限

)
SELECT
  employee_id,
  employee_name,
  manager_id,
  level
FROM
  employee_hierarchy
ORDER BY
  level, employee_id;
```

実行確認用

```SQL
with RECURSIVE
employees as  (
	select 1 as employee_id, 'Alice (CEO)' as employee_name, NULL as manager_id union all
	select 2 as employee_id, 'Bob        ' as employee_name, 1    as manager_id union all
	select 3 as employee_id, 'Charlie    ' as employee_name, 1    as manager_id union all
	select 4 as employee_id, 'David      ' as employee_name, 2    as manager_id union all
	select 5 as employee_id, 'Eve        ' as employee_name, 2    as manager_id union all
	select 6 as employee_id, 'Frank      ' as employee_name, 3    as manager_id
),
employee_hierarchy AS (
-- アンカーメンバー: CEO (manager_id が NULL の従業員) を取得
SELECT
  employee_id,
  employee_name,
  manager_id,
  1 AS level -- 階層レベルを初期化
FROM
  employees
WHERE
  manager_id IS NULL

    UNION ALL

    -- 再帰部分: 上位の階層から次のレベルの部下を取得
    SELECT
        e.employee_id,
        e.employee_name,
        e.manager_id,
        eh.level + 1 AS level -- 階層レベルをインクリメント
    FROM
        employees AS e
    JOIN
        employee_hierarchy AS eh ON e.manager_id = eh.employee_id
    WHERE
        eh.level < 10 -- 💡実務Tips: 無限ループ回避とパフォーマンスのために最大階層制限

)
SELECT
  employee_id,
  employee_name,
  manager_id,
  level
FROM
  employee_hierarchy
ORDER BY
  level, employee_id;
```

**実行結果:**

| employee_id | employee_name | manager_id | level |
| :---------- | :------------ | :--------- | :---- |
| 1           | Alice (CEO)   | NULL       | 1     |
| 2           | Bob           | 1          | 2     |
| 3           | Charlie       | 1          | 2     |
| 4           | David         | 2          | 3     |
| 5           | Eve           | 2          | 3     |
| 6           | Frank         | 3          | 3     |

**説明:**

1. **アンカーメンバー**: 最初に manager_id が NULL である Alice を level=1 として選択します。
2. **再帰部分**: employee_hierarchy（一つ前の結果セット）と employees テーブルを JOIN し、employee_hierarchy の employee_id が employees の manager_id と一致する行（部下）を検索します。見つかった部下は level が+1 されて追加されます。
3. このプロセスは、新たな部下が見つからなくなるまで繰り返され、最終的に全従業員の階層が展開されます。
   💡 実務 Tips: 無限ループとパフォーマンスの問題を回避するため、再帰 CTE には WHERE eh.level < \[最大階層数\]のような最大階層制限を設けることが非常に重要です。予期せぬ循環参照が存在する場合でも、クエリが暴走するのを防ぐことができます。
