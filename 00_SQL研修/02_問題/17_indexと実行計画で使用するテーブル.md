# 17章（indexと実行計画）で使用するテーブル

index と実行計画の効果は、**データ量がないと体感できません**。そのためこの章だけは専用のデータベースを作り、10万件・100万件のデータを投入して使います。

テーブル構造は [[00_SQL研修/02_問題/08_結合から使用するテーブル|08_結合から使用するテーブル]] の `customers` / `orders` と同じです。見慣れた構造のまま、件数だけ増やしたものだと考えてください。

> **所要時間の目安**：データ投入に10〜30秒ほどかかります。作成後のサイズは約200MBです。

---

## 1. データベースの作成

既存の研修用DBを汚さないよう、専用のデータベースを作ります。

```sql
-- ※ postgres など、別のデータベースに接続した状態で実行してください
CREATE DATABASE sql_lesson_execution_plan;
```

作成したら、接続先を `sql_lesson_execution_plan` に切り替えてから以降を実行します。

```sql
-- psql の場合
\c sql_lesson_execution_plan
```

### 並列クエリを止めておく

PostgreSQL は大きなテーブルを読むとき、複数のプロセスで分担する「並列クエリ」を使うことがあります。便利な機能ですが、実行計画に `Gather` や `Parallel Seq Scan` というノードが増え、行数が「1プロセスあたりの平均」で表示されるため、**読み方を学ぶ段階では邪魔になります**。

この章では意図的にオフにします。

```sql
ALTER DATABASE sql_lesson_execution_plan SET max_parallel_workers_per_gather = 0;
```

設定後は一度接続し直してください。実務では**オンのままが普通**なので、`Gather` が出てきたら「並列で読んでいるんだな」と読み替えてください。

---

## 2. DDL

```sql
DROP TABLE IF EXISTS orders;
DROP TABLE IF EXISTS customers;

CREATE TABLE customers (
    customer_id   INT PRIMARY KEY,
    name          VARCHAR(100),
    email         VARCHAR(100),
    phone         VARCHAR(20),
    city          VARCHAR(50),
    join_date     DATE,
    membership_id INT
);

CREATE TABLE orders (
    order_id    INT PRIMARY KEY,
    customer_id INT,
    order_date  TIMESTAMP,
    status      VARCHAR(20),
    FOREIGN KEY (customer_id) REFERENCES customers (customer_id)
);
```

> **`08_結合` との違い**（意図的な変更です）
> - `order_date` を `DATE` から **`TIMESTAMP`** に変更。「`WHERE DATE(order_date) = '...'` と書くとインデックスが効かない」という最頻出の落とし穴を扱うため、時刻を持つ型にしています。
> - `memberships` / `categories` などのテーブルは省略。`membership_id` は列としてだけ残しています（外部キー制約なし）。
> - `email` を **大文字混じり**（`User123@Example.com`）にしています。`LOWER()` を使った関数インデックスを扱うためです。

---

## 3. データ投入

```sql
-- 顧客 10万件
INSERT INTO customers (customer_id, name, email, phone, city, join_date, membership_id)
SELECT
    gs,
    'Customer_' || gs,
    'User' || gs || '@Example.com',
    '090-' || LPAD((gs % 10000)::text, 4, '0') || '-0000',
    'City_' || (gs % 500),
    DATE '2020-01-01' + (gs % 1800),
    (gs % 4) + 1
FROM GENERATE_SERIES(1, 100000) AS gs;

-- 注文 100万件
INSERT INTO orders (order_id, customer_id, order_date, status)
SELECT
    gs,
    (gs % 100000) + 1,
    TIMESTAMP '2024-01-01 00:00:00' + (gs * 60 || ' seconds')::interval,
    CASE
        WHEN gs > 980000 THEN 'Pending'      --  2%（直近の注文）
        WHEN gs > 900000 THEN 'Shipped'      --  8%
        ELSE 'Completed'                     -- 90%（過去の注文）
    END
FROM GENERATE_SERIES(1, 1000000) AS gs;
```

### 統計情報を作る（★必須・単独で実行）

投入が終わったら `VACUUM ANALYZE` を実行します。**ただし、以下の2行は「1行ずつ」または「この2行だけを選択して」実行してください。**

```sql
VACUUM ANALYZE customers;
```

```sql
VACUUM ANALYZE orders;
```

> ⚠️ **`VACUUM` は、他のSQLとまとめて実行できません。**
>
> pgAdmin などで**複数のSQLをまとめて選択して一度に実行**すると、PostgreSQL はそれらを**1つのトランザクションとして扱います**。`VACUUM` はトランザクションの中では実行できない決まりなので、次のエラーになります。
>
> ```text
> ERROR:  VACUUM cannot run inside a transaction block
> SQL state: 25001
> ```
>
> **対処**：`VACUUM ANALYZE customers;` の行だけを選択して実行 → 次に `VACUUM ANALYZE orders;` の行だけを選択して実行、という手順にしてください。`INSERT` と一緒に流すと必ず失敗します。
>
> （この後の演習でも `VACUUM` が出てきますが、すべて同じルールです。）
>
> **同じ制約を持つコマンド**（いずれも単独で実行する必要があります）
>
> | コマンド | 用途 |
> | :--- | :--- |
> | `VACUUM` / `VACUUM ANALYZE` | 不要行の回収、統計情報とVisibility Mapの更新 |
> | `CREATE INDEX CONCURRENTLY` | ロックせずにインデックスを作る（→ 17-2 §1-3） |
> | `DROP INDEX CONCURRENTLY` | ロックせずにインデックスを消す |
> | `REINDEX CONCURRENTLY` | ロックせずにインデックスを作り直す |
> | `CREATE DATABASE` | データベース作成（セクション1で実行済み） |
>
> ちなみに `ANALYZE` 単体はトランザクション内でも実行できます。制約があるのは `VACUUM` の方です。

`VACUUM ANALYZE` を忘れると、プランナが正しい判断をできず、`Index Only Scan` も効かなくなります。**データを大量に入れた後は必ず実行してください。**

---

## 4. データの「仕込み」の意図

このデータは、実行計画の各パターンが再現するように値の分布を設計しています。

| 列 | 分布 | 何を見せるための仕込みか |
| :--- | :--- | :--- |
| `customers.customer_id` | 連番（10万） | 主キーによる1行ピンポイント検索 |
| `customers.city` | 500都市 × 200件（0.2%） | 該当行が全ページに散る → **Bitmap Heap Scan** |
| `customers.membership_id` | 4種類（各25%） | 低カーディナリティ（絞り込めない列） |
| `customers.name` | 全件バラバラ | インデックスなしのソート → **work_memの溢れ** |
| `customers.email` | 大文字混在 | **関数インデックス** `LOWER(email)` |
| `orders.order_id` | 連番（100万） | 物理配置と索引順が一致（correlation = 1.0） |
| `orders.customer_id` | 1〜10万を循環 | 物理配置がバラバラ（correlation ≒ 0.1）。**同じテーブルの2列で並び順の影響を対比できる** |
| `orders.order_date` | 1分刻みで単調増加 | 範囲検索、`DATE()` の落とし穴 |
| `orders.status` | Completed 90% / Shipped 8% / Pending 2%<br>かつ**新しい注文がPending** | **同じインデックスでも、指定する値によって計画が変わる**ことを見せる |

`orders.customer_id` には**あえてインデックスを張っていません**。外部キー制約は付いていますが、**PostgreSQLは外部キーにインデックスを自動作成しない**ためです。これは実務で最も多いインデックス貼り忘れのパターンで、17-2 のケーススタディで扱います。

---

## 5. 作成後の確認

```sql
-- 件数・ページ数・サイズ
SELECT relname, relpages, pg_size_pretty(pg_relation_size(oid)) AS size
FROM pg_class WHERE relname IN ('customers','orders') ORDER BY relname;

-- 物理配置と索引順の一致度（correlation）
SELECT tablename, attname, n_distinct, round(correlation::numeric, 3) AS corr
FROM pg_stats
WHERE tablename IN ('customers','orders')
  AND attname IN ('customer_id','order_id','order_date','city','status')
ORDER BY 1, 2;

-- 自動作成されたインデックス（主キーの2本だけになっているはず）
SELECT indexrelname, pg_size_pretty(pg_relation_size(indexrelid)) AS size
FROM pg_stat_user_indexes ORDER BY 1;
```

**期待される結果**（PostgreSQL 17 で確認）

```text
  relname  | relpages |  size
-----------+----------+---------
 customers |     1235 | 9880 kB
 orders    |     7255 | 57 MB

 tablename |   attname   | n_distinct | corr
-----------+-------------+------------+-------
 customers | city        |        500 | 0.002
 customers | customer_id |         -1 | 1.000
 orders    | customer_id |      99661 | 0.103
 orders    | order_date  |         -1 | 1.000
 orders    | order_id    |         -1 | 1.000
 orders    | status      |          3 | 0.999

  indexrelname  |  size
----------------+---------
 customers_pkey | 2208 kB
 orders_pkey    | 21 MB
```

`relpages` の値（`customers` = 1,235 / `orders` = 7,255）は、実行計画の `Buffers` を読むときの**基準値**になります。「読んだページ数がこの数字と一致していたら全件走査」と判断できるので、覚えておいてください。

---

## 6. やり直したいとき

インデックスを張りすぎて分からなくなったら、インデックスだけ全部消せば初期状態に戻せます。

```sql
-- 主キー以外のインデックスをすべて削除
DO $$ DECLARE r RECORD;
BEGIN
    FOR r IN (SELECT indexname FROM pg_indexes
              WHERE schemaname='public' AND indexname NOT LIKE '%_pkey') LOOP
        EXECUTE 'DROP INDEX ' || quote_ident(r.indexname);
    END LOOP;
END $$;
```

そのあと、**1行ずつ単独で**実行します（前述の通り `VACUUM` はまとめて流せません）。

```sql
VACUUM ANALYZE customers;
```

```sql
VACUUM ANALYZE orders;
```

データごと作り直す場合は、セクション2のDDLから再実行してください。
