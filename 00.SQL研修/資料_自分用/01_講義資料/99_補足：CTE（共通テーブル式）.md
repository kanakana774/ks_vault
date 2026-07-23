### SQLの共通テーブル式（CTE）をマスターしよう！

複雑なSQLクエリを、まるで物語を読むように分かりやすく記述できる強力な機能、それが**共通テーブル式（Common Table Expressions - CTE）**です。


### 1. CTEの基本：WITH句の使い方

CTEは、`WITH`句を使ってクエリの先頭で定義する、名前付きの一時的な結果セットです。複雑な処理を論理的なステップに分割することで、クエリの可読性とメンテナンス性を飛躍的に向上させます。

#### 1-1. 基本構文

基本はとてもシンプルです。`WITH`の後ろに名前を付けたSELECT文を書き、その後のメインクエリでその名前を使って参照します。

```sql
WITH CTE名 AS (
  -- ここに中間的な結果セットを作成するクエリを記述
  SELECT ...
)
-- メインクエリで、上で定義したCTE名をテーブルのように利用する
SELECT ...
FROM CTE名;
```

複数のCTEを定義する場合は、カンマ（`,`）でつなげて記述できます。後ろのCTEから前のCTEを参照することも可能です。

```sql
WITH CTE1 AS (
  SELECT ...
),
CTE2 AS (
  -- CTE2の中からCTE1を参照
  SELECT ...
  FROM CTE1
)
SELECT ...
FROM CTE2;
```

#### 1-2. 具体例：顧客ごとの合計購入額を計算する

ステップを踏んだ計算にCTEは最適です。ここでは、「①顧客ごとの合計購入金額を計算し、②その結果を使って合計額が1000ドルを超える優良顧客を抽出する」という処理をCTEで記述します。

**【元データ】**

<details>
<summary>customers テーブル & orders テーブル</summary>

**customers テーブル:**

| customer_id | customer_name |
| :---------- | :------------ |
| 1           | Alice         |
| 2           | Bob           |
| 3           | Charlie       |

**orders テーブル:**

| order_id | customer_id | amount |
| :------- | :---------- | :----- |
| 101      | 1           | 500    |
| 102      | 2           | 700    |
| 103      | 1           | 600    |
| 104      | 3           | 300    |
| 105      | 2           | 400    |

</details>

**【SQLコード】**

```sql
-- ① 顧客ごとの合計購入金額を計算するCTEを定義
WITH customer_total_sales AS (
  SELECT
    customer_id,
    SUM(amount) AS total_amount
  FROM
    orders
  GROUP BY
    customer_id
)
-- ② ①のCTEと顧客テーブルを結合し、条件に合う顧客を抽出
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

**【実行結果】**

| customer_name | total_amount |
| :------------ | :----------- |
| Alice         | 1100         |

**【解説】**
`customer_total_sales`というCTEで、まず「顧客ごとの合計購入金額」という中間結果を準備しました。その後のメインクエリでは、この準備された結果を一つのテーブルのように扱い、最終的な絞り込みと結合を行っています。このように処理を分割することで、クエリ全体のロジックが非常に明確になります。


### 2. CTE vs サブクエリ vs ビュー：使い分けのヒント

CTEと似た機能に「サブクエリ」や「ビュー」があります。それぞれの特徴を理解し、適切に使い分けることが重要です。

| 特徴 | サブクエリ (派生テーブル) | ビュー (VIEW) | **CTE (共通テーブル式)** |
| :--- | :--- | :--- | :--- |
| **定義場所** | `FROM`句や`WHERE`句の中 | データベース内に永続的に定義 | `WITH`句でクエリの先頭に一時的に定義 |
| **再利用性** | その場限り（クエリ内でも1回） | データベース内で恒久的に再利用可能 | **定義したクエリ内でのみ複数回参照可能** |
| **可読性** | ネストが深くなると著しく低下 | シンプル | **段階的に記述でき、非常に高い** |
| **メンテナンス性**| クエリが複雑化しやすい | 管理が必要だが、共通ロジックは一元化できる | クエリ内で完結し、メンテナンスしやすい |
| **最適な用途** | 使い捨ての単純な中間クエリ | 複数クエリで共通利用する定番のデータセット | 1つのクエリ内での複雑な処理の分割・整理 |

#### 💡 パフォーマンスに関する注意点

CTEは一般的に良好なパフォーマンスを発揮しますが、データベースの種類や使い方によっては注意が必要です。

*   **CTEの内部処理**: データベースによっては、CTEの中間結果を一時的に物理ファイルとして書き出す（**マテリアライズ**する）ことがあります。この場合、CTEの処理が重いと、クエリ全体のパフォーマンスが低下する可能性があります。
*   **インライン化**: 最近のデータベースでは、CTEをメインクエリに賢く組み込んで最適化（**インライン化**）してくれる傾向が強いです。
*   **習慣づけ**: CTEを使用する際は、**実行計画を確認する**習慣をつけると、パフォーマンスの問題を早期に発見できます。

---

### 3. 応用編：再帰CTEで階層構造を扱う

CTEの強力な機能の一つが**再帰（Recursive）**です。自分自身を繰り返し参照することで、組織図や部品の親子関係といった階層構造（ツリー構造）のデータを簡単に展開できます。

#### 3-1. 再帰CTEの基本構文

再帰CTEは、2つのパートから構成されます。

```sql
WITH RECURSIVE CTE名 AS (
  -- 1. 非再帰部分（アンカーメンバー）
  --    ⇒ 再帰の「開始点」となる最初の行を取得する
  SELECT ...
  
  UNION ALL
  
  -- 2. 再帰部分（リカージョンメンバー）
  --    ⇒ CTE自身を参照し、前のステップの結果から次の階層の行を取得する
  SELECT ...
  FROM CTE名 -- ここで自分自身を参照！
  WHERE ...  -- 再帰を停止させる終了条件
)
SELECT ...
FROM CTE名;
```

#### 3-2. 具体例：従業員の階層リストを作成する

従業員テーブルから、誰が誰の上司で、階層の深さはどのくらいか、というレポートを作成します。

**【元データ】**

<details>
<summary>employees テーブル</summary>

**employees テーブル:**

| employee_id | employee_name | manager_id |
| :---------- | :------------ | :--------- |
| 1           | Alice (CEO)   | NULL       |
| 2           | Bob           | 1          |
| 3           | Charlie       | 1          |
| 4           | David         | 2          |
| 5           | Eve           | 2          |
| 6           | Frank         | 3          |
</details>

**【SQLコード】**

```sql
WITH RECURSIVE employee_hierarchy AS (
  -- 【アンカーメンバー】: まず階層のトップ（CEO）を見つける
  SELECT
    employee_id,
    employee_name,
    manager_id,
    1 AS level -- 階層レベルを1で初期化
  FROM
    employees
  WHERE
    manager_id IS NULL -- トップの人は上司(manager_id)がいない

  UNION ALL

  -- 【再帰部分】: 上の階層の従業員を元に、その部下を見つけていく
  SELECT
    e.employee_id,
    e.employee_name,
    e.manager_id,
    eh.level + 1 AS level -- 親の階層レベルに+1する
  FROM
    employees AS e
  JOIN
    employee_hierarchy AS eh ON e.manager_id = eh.employee_id
  WHERE
    eh.level < 10 -- 💡無限ループを避けるための安全装置
)
-- 最後に、作成した階層リストを全件表示
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

**【実行結果】**

| employee_id | employee_name | manager_id | level |
| :---------- | :------------ | :--------- | :---- |
| 1           | Alice (CEO)   | NULL       | 1     |
| 2           | Bob           | 1          | 2     |
| 3           | Charlie       | 1          | 2     |
| 4           | David         | 2          | 3     |
| 5           | Eve           | 2          | 3     |
| 6           | Frank         | 3          | 3     |

**【解説】**
1.  **アンカーメンバー**が、`manager_id`がNULLのAlice (CEO) を `level=1`として見つけ出します。
2.  **再帰部分**が、1の結果（Alice）を元に、彼女の`employee_id`を`manager_id`に持つ従業員（BobとCharlie）を`level=2`として見つけます。
3.  このプロセスが、新たに従業員が見つからなくなるまで繰り返され、最終的に全ての従業員の階層が展開されます。

#### 💡【最重要】無限ループを防ぐ方法
再帰CTEでは、意図しないデータの循環参照などにより、処理が終わらない**無限ループ**に陥る危険性があります。これを防ぐため、`WHERE eh.level < 10` のように、**必ず最大階層数を設けて処理を停止させる条件**を入れることが非常に重要です。