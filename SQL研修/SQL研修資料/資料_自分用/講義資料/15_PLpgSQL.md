# **PL/pgSQL**

## **はじめに**

PL/pgSQL は PostgreSQL に組み込まれた手続き型言語です。SQL だけでは難しい**複雑なロジックをデータベース内部で直接処理する**ために使用します。これにより、クライアントとデータベース間の通信量を減らし、パフォーマンスを向上させることができます。

## **◇ 無名ブロック (Anonymous Block)**

無名ブロックは、データベースに保存されない、一度限りの PL/pgSQL コードの塊です。主に以下のような目的で使います。

- **簡単なスクリプトの実行**: データのバックアップ、一時的な更新、メンテナンス作業などに使用します。
- **関数のテスト**: 本番環境にデプロイする前に、新しいロジックが期待通りに動作するか手軽に確認できます。

**構文例**:

```SQL
DO $$
DECLARE
 -- 変数を宣言する場所
BEGIN
 -- 実行するロジック
END;
$$ LANGUAGE plpgsql;
```

```SQL
DO $$
[ <<label>> ]
[ DECLARE ]
  -- 変数宣言
BEGIN
  -- 実行される文
END [ label ];
$$ LANGUAGE plpgsql;
```

### **変数の宣言と代入**

変数は DECLARE セクションで宣言します。

変数の宣言構文:

```SQL
変数名 データ型 [:= 初期値];
```

:= は代入演算子です。宣言時に初期値を設定しない場合は、デフォルトで NULL が代入されます。

**例：基本的な変数宣言**

```SQL
DO $$
DECLARE
 -- ユーザー ID を格納する整数型の変数
 user_id INT := 123;
 -- ユーザー名を格納するテキスト型の変数
 user_name TEXT := '山田 太郎';
 -- アカウント作成日を格納する日付型の変数
 created_at DATE := '2023-01-15';
BEGIN
 -- 変数を利用してメッセージを出力
 RAISE NOTICE 'ユーザー ID: %, 名前: %, 作成日: %', user_id, user_name, created_at;
END;
$$ LANGUAGE plpgsql;
```

---

**実践的なヒント:**

#### **%TYPE**: テーブルの列と同じデータ型を変数に割り当てることができます。これにより、テーブルのスキーマが変更されてもコードを修正する必要がなく、メンテナンス性が向上します。

この%TYPE は、以下のように**既存のテーブルの列をピンポイントで参照**します。

**具体例：users テーブルの列を参照**

```SQL
-- 毎度作り直す
DROP TABLE IF EXISTS users;
-- テーブル
CREATE TEMP TABLE users (
 id SERIAL PRIMARY KEY,
 user_name TEXT NOT NULL,
 email VARCHAR(255) UNIQUE
);
-- PL/pgSQL コード
DO $$
DECLARE
 -- users テーブルの id 列と同じデータ型（INTEGER）を持つ変数
 user_id users.id%TYPE;
 -- users テーブルの user_name 列と同じデータ型（TEXT）を持つ変数
 full_name users.user_name%TYPE;
BEGIN
 user_id := 5;
 full_name := '鈴木 一郎';
 RAISE NOTICE 'ユーザー ID: %, 名前: %', user_id, full_name;
END;
$$ LANGUAGE plpgsql;
```

---

#### **%ROWTYPE**: テーブルの行全体を一つの変数として扱うことができます。

**具体例：users テーブルの行を参照**

```SQL
-- users テーブルが存在すると仮定
DO $$
DECLARE
 -- users テーブルの行全体（id, user_name, email）を格納する変数
 users_row users%ROWTYPE;
BEGIN
 -- SELECT 文で取得した行を変数に代入
 SELECT id, user_name, email INTO users_row FROM users WHERE id = 1;
 RAISE NOTICE 'ユーザー情報: ID: %, 名前: %, メール: %', users_row.id, users_row.user_name, users_row.email;
END;
$$ LANGUAGE plpgsql;
```

---

#### RECORD 型

RECORD 型は、特定のデータ型ではなく、プレースホルダとして機能する疑似データ型です。
テーブルの列のデータ型として使用することはできませんが、PL/pgSQL の変数や関数の戻り値として、行の構造が事前に決まっていない場合や、動的に変わる可能性がある場合に非常に役立ちます。

##### 1. 型の主な特徴

- 動的な構造: RECORD 変数は、代入されるまで決まった構造を持ちません。SELECT ... INTO 文などが実行されると、その問い合わせ結果の行の構造を自動的に受け継ぎます。
- プレースホルダ: 事前にフィールドを定義するのではなく、実際の行データを保持するための一時的な器として機能します。
- NULL から開始: 宣言された時点では、RECORD 変数は構造を持たない NULL 状態です。

##### 2. 具体的な使用例

###### 例 1：PL/pgSQL ブロック内での変数としての利用

RECORD 変数の最も一般的な使い方は、異なる構造を持つテーブルの行を一時的に格納することです。
まず、例として使用する 2 つの異なる構造のテーブルを作成します。
次に、DO ブロック（匿名ブロック）内で RECORD 変数を使ってみましょう。

```SQL
DROP TABLE IF EXISTS employees;
CREATE TEMP TABLE employees (
    id INT PRIMARY KEY,
    name TEXT,
    department TEXT
);
DROP TABLE IF EXISTS products;
CREATE TEMP TABLE products (
    product_id INT PRIMARY KEY,
    product_name TEXT,
    price NUMERIC
);

INSERT INTO employees VALUES (1, '山田太郎', '営業部');
INSERT INTO products VALUES (101, '高性能キーボード', 15000);

DO $$
DECLARE
    -- recはRECORD型の変数として宣言される。この時点では構造を持たない。
    rec RECORD;
BEGIN
    -- employeesテーブルの行をrecに代入する
    SELECT * INTO rec FROM employees WHERE id = 1;
    -- recはemployeesテーブルの構造を持つようになる
    RAISE NOTICE '社員名: %, 部門: %', rec.name, rec.department;

    -- 次に、productsテーブルの行を同じ変数recに代入する
    SELECT * INTO rec FROM products WHERE product_id = 101;
    -- recの構造はproductsテーブルのものに動的に変化する
    RAISE NOTICE '製品名: %, 価格: %', rec.product_name, rec.price;
END;
$$ LANGUAGE plpgsql;

-- 実行結果:
-- NOTICE:  社員名: 山田太郎, 部門: 営業部
-- NOTICE:  製品名: 高性能キーボード, 価格: 15000
```

この例では、rec という単一の RECORD 変数が、最初に employees テーブルの行構造（id, name, department）を保持し、次に products テーブルの行構造（product_id, product_name, price）を保持しています。このように、代入操作によって変数の内部構造が動的に変わるのが RECORD の大きな特徴です。

###### 例 2：関数の戻り値としての利用

関数の返す列の集合が呼び出しごとに変わる可能性がある場合に、戻り値の型として RECORD が役立ちます。

```SQL
DROP TABLE IF EXISTS employees;
CREATE TEMP TABLE employees (
    id INT PRIMARY KEY,
    name TEXT,
    department TEXT
);
DROP TABLE IF EXISTS products;
CREATE TEMP TABLE products (
    product_id INT PRIMARY KEY,
    product_name TEXT,
    price NUMERIC
);

INSERT INTO employees VALUES (1, '山田太郎', '営業部');
INSERT INTO products VALUES (101, '高性能キーボード', 15000);

DROP FUNCTION IF EXISTS get_item(int);
CREATE FUNCTION get_item(item_id INT) RETURNS RECORD AS $$
DECLARE
    result RECORD;
BEGIN
    -- 渡されたIDに応じて、異なるテーブルから情報を取得する
    IF item_id < 100 THEN
        SELECT id, name, department INTO result FROM employees WHERE id = item_id;
    ELSE
        SELECT product_id, product_name, price INTO result FROM products WHERE product_id = item_id;
    END IF;
    RETURN result;
END;
$$ LANGUAGE plpgsql;
```

この関数を呼び出す際には、呼び出し側で戻り値の構造（列定義リスト）を明示的に指定する必要があります。
さもないと、PostgreSQL は結果の列をどう解釈すればよいか判断できません。

```SQL
-- 社員情報を取得する
SELECT * FROM get_item(1) AS (id INT, name TEXT, department TEXT);

-- 実行結果:
--  id |   name   | department
-- ----+----------+------------
--   1 | 山田太郎 | 営業部

-- 製品情報を取得する
SELECT * FROM get_item(101) AS (product_id INT, product_name TEXT, price NUMERIC);
-- 実行結果:
--  product_id |  product_name  | price
-- ------------+------------------+-------
--         101 | 高性能キーボード | 15000
```

##### 3. ROWTYPE との違い

%ROWTYPE も行全体のデータを格納する変数ですが、RECORD とは重要な違いがあります。

- **%ROWTYPE**: 特定のテーブルやビューの行構造に固定されます。宣言時に構造が決定されるため、型安全性が高いですが、柔軟性には欠けます。
- **RECORD**: 構造が固定されず、実行時に動的に決まります。柔軟性が高い反面、コンパイル時にはフィールドの存在チェックができません。

<details>
<summary>匿名レコード型とキャスト</summary>

##### 4. 匿名レコード型とキャスト

ROW()構文は、名前のない（匿名の）レコード型の値を作成します。関数がオーバーロードされているなど、どの関数を呼び出すべきか曖昧な場合、この匿名レコードを明示的にキャストして型を解決する必要があります。

```SQL
-- 2つの異なる複合型を定義
CREATE TYPE mytable AS (f1 int, f2 text);
CREATE TYPE myrowtype AS (f1 int, f2 text, f3 numeric);

-- 同じ名前で引数の型が異なる関数（オーバーロード）
CREATE FUNCTION getf1(mytable) RETURNS int AS 'SELECT $1.f1' LANGUAGE SQL;
CREATE FUNCTION getf1(myrowtype) RETURNS int AS 'SELECT $1.f1' LANGUAGE SQL;

-- この呼び出しはエラーになる。どちらのgetf1を呼ぶべきか不明確なため。
-- SELECT getf1(ROW(1, 'this is a test'));
-- ERROR:  function getf1(record) is not unique

-- mytable型にキャストして曖昧さを解決する
SELECT getf1(ROW(1, 'this is a test')::mytable);
```

</details>

## **関数とプロシージャ**

PL/pgSQL では、データベースに保存して繰り返し使用する**関数（FUNCTION）**と**プロシージャ（PROCEDURE）**を定義できます。

**関数とプロシージャの根本的な違い**:

| 特徴                 | 関数 (FUNCTION)                                       | プロシージャ (PROCEDURE)                     |
| :------------------- | :---------------------------------------------------- | :------------------------------------------- |
| **目的**             | **値を返すこと**を主目的とする。                      | **値を返さない**。複数の SQL 文の集合体。    |
| **呼び出し**         | SELECT 文の中で呼び出す。CALL も可能。                | CALL 文で呼び出す。SELECT では呼び出せない。 |
| **トランザクション** | 通常、呼び出し元のトランザクション内で実行。          | 独自に COMMIT や ROLLBACK を実行できる。     |
| **戻り値**           | RETURN で単一の値、RETURNS SETOF で結果セットを返す。 | OUT パラメータを介して値を返すことは可能。   |

## ◇ 関数

**構文例**:

```SQL
-- 毎度削除して作り直す
DROP FUNCTION IF EXISTS get_total_sales();
-- 関数 (FUNCTION) の例
CREATE FUNCTION get_total_sales() RETURNS NUMERIC(10,2) AS $$
DECLARE
 total_sales NUMERIC(10,2);
 rec RECORD;
BEGIN
 -- 売上を合計してtotal_salesに代入
 SELECT SUM(price) INTO total_sales FROM sales;

 -- 関数なのでreturnが必要
 RETURN total_sales;
END;
$$ LANGUAGE plpgsql;


-- 適当にデータを作成する
DROP TABLE IF EXISTS sales;
CREATE TEMP TABLE sales(month varchar(10),price numeric(10,2));
INSERT INTO sales VALUES ('1月', 10000), ('2月', 20000), ('3月', 15000), ('4月', 10000);
-- 関数の呼び出し
select get_total_sales();
```

---

### パターン 1: 単一の値（スカラ値）を返す ()

これは、関数が単一の値を返す最も基本的な形式です。関数は RETURNS data_type（例: RETURNS integer、RETURNS text）のように定義され、RETURN 文で指定された式の値が関数の戻り値となります。この RETURN 文が実行されると、関数の実行は即座に終了します。
具体例: 2 つの整数の和を返す関数

```SQL
-- 毎度削除して作り直す
DROP FUNCTION IF EXISTS add_integers(integer,integer);

CREATE FUNCTION add_integers(a integer, b integer) RETURNS integer AS $$
BEGIN
    -- aとbの和を計算し、その結果を返す
    RETURN a + b;
END;
$$ LANGUAGE plpgsql;


-- 呼び出し例
SELECT add_integers(5, 10);
-- 結果: 15
```

この例では、a + b という式の評価結果（integer 型）が関数の戻り値となります。

---

### パターン 2: 値を返さない ()

プロシージャのように、値を返さずに何らかの処理（例: データ挿入）のみを行う関数も定義できます。この場合、戻り値の型として void を指定します。関数内では、引数なしの RETURN;を使用して任意の時点で処理を終了できます。関数の最後に達した場合、RETURN;は省略可能です。
具体例: ログテーブルにメッセージを記録する関数

```SQL
-- メッセージを挿入するvoid関数
CREATE OR REPLACE FUNCTION log_message(msg text) RETURNS void AS $$
BEGIN
    -- 何か条件をチェックし、条件外なら何もせず終了
    IF msg IS NULL OR length(msg) = 0 THEN
        RETURN; -- ここで関数が終了しvoid（空）が帰る
    END IF;

    INSERT INTO log_messages (message, logged_at) VALUES (msg, now());
    -- 関数の最後に達したため、暗黙的にRETURNされてvoid（空）が帰る
END;
$$ LANGUAGE plpgsql;

-- 適当にデータを作成する
DROP TABLE IF EXISTS log_messages;
CREATE TEMP TABLE log_messages(log_id serial PRIMARY KEY, message text, logged_at timestamp with time zone DEFAULT now());

-- 呼び出し例
SELECT log_message('');
-- SELECT log_message('This is a test message.');
-- テーブルの結果確認用
-- SELECT * FROM log_messages;
```

この関数は値を返しませんが、log_messages テーブルへの挿入という副作用を持ちます。

---

<details>
<summary>パターン 3: 複数の値を 1 行で返す (パラメータ)</summary>

### パターン 3: 複数の値を 1 行で返す (パラメータ)

関数が複数の値をまとめて 1 つの行（レコード）として返す場合、OUT パラメータを使用する方法があります。この方法では、RETURNS 句で戻り値の型を定義する代わりに、引数リスト内で OUT 修飾子を使って出力用の変数を宣言します。関数本体でこれらの変数に値を代入し、最後に引数なしの RETURN;を実行するか、関数の終わりに達すると、これらの OUT パラメータの現在の値が 1 行として返されます。
具体例: 2 つの数値の和・差・積を返す関数

```SQL
-- 毎度削除して作り直す
DROP FUNCTION IF EXISTS arithmetic_operations();

CREATE OR REPLACE FUNCTION arithmetic_operations(
    IN a integer,
    IN b integer,
    OUT sum_val integer,
    OUT diff_val integer,
    OUT prod_val integer
) AS $$
BEGIN
    -- OUT パラメータで宣言された変数に値を代入
    sum_val := a + b;
    diff_val := a - b;
    prod_val := a * b;
    -- RETURN は不要。関数の最後で OUT パラメータの値が返される
END;
$$ LANGUAGE plpgsql;

-- 呼び出し例
SELECT * FROM arithmetic_operations(10, 5);
-- 結果:
--  sum_val | diff_val | prod_val
-- ---------+----------+----------
--       15 |        5 |       50

-- これで呼ぶと連結されて帰ってくる
-- SELECT arithmetic_operations(10, 5);
```

---

</details>

---

<details>
<summary>パターン 4: 行の集合（テーブル）を返す</summary>

### パターン 4: 行の集合（テーブル）を返す

関数がテーブルのように複数行の結果セットを返す場合は、RETURNS SETOF data_type または RETURNS TABLE(...)と宣言します。このような関数は「集合を返す関数 (Set Returning Functions)」と呼ばれ、問い合わせの FROM 句でテーブルのように使用できます。PL/pgSQL では、行セットを返すために特別な RETURN 構文が用意されています。

#### A. 1 行ずつ結果セットに追加する ()

ループ処理などを用いて、結果セットに含める行を 1 行ずつ計算し、追加していく場合に使用します。RETURN NEXT は関数を終了させず、次の処理を続行します。すべての行を返し終えたら、最後に引数なしの RETURN;で関数を終了します。
具体例: 特定の ID より大きい従業員を 1 行ずつ返す

```SQL
-- 毎度削除して作り直す（employeesテーブルを下記で削除するより前に、RETURNS SETOF employeesでemployeesに依存してる関数のほうを削除）
DROP FUNCTION IF EXISTS get_employees_after(integer) CASCADE;

-- 従業員テーブルを仮定（下記関数定義の前に、RETURNS SETOF employeeseで使うため、先にmployeesテーブルを作る必要がある）
DROP TABLE IF EXISTS employees;
CREATE TEMP TABLE employees (emp_id serial, emp_name text);
INSERT INTO employees VALUES (1, 'Alice'), (2, 'Bob'), (3, 'Carol'), (4, 'David');

-- 戻り値はsetofテーブル名
CREATE FUNCTION get_employees_after(p_emp_id integer) RETURNS SETOF employees AS $$
DECLARE
    emp_record employees%ROWTYPE;
BEGIN
    -- FOR ループで条件に合う従業員を検索
    FOR emp_record IN
	    SELECT * FROM employees WHERE emp_id > p_emp_id ORDER BY emp_id
    LOOP
        -- 見つかった行を結果セットに追加し、ループを継続
        RETURN NEXT emp_record;
    END LOOP;
    -- これ以上返す行がないことを示す
    RETURN;
END;
$$ LANGUAGE plpgsql;


-- 呼び出し例
SELECT * FROM get_employees_after(2);
-- 結果:
-- emp_id | emp_name
-- --------+----------
-- 3 | Carol
-- 4 | David
```

#### B. クエリ結果をまとめて返す ()

単一の SQL クエリの結果セット全体を、関数の戻り値として一度に追加する場合に使用します。RETURN NEXT をループで使うよりも簡潔に記述できます。
具体例: 特定の ID より大きい従業員をクエリでまとめて返す

```SQL
-- 毎度削除して作り直す
DROP FUNCTION IF EXISTS get_employees_after_query(int);

CREATE FUNCTION get_employees_after_query(p_emp_id integer) RETURNS SETOF employees AS $$
BEGIN
    -- クエリの結果全体を関数の結果セットとして返す
    RETURN QUERY SELECT * FROM employees WHERE emp_id > p_emp_id ORDER BY emp_id;
END;
$$ LANGUAGE plpgsql;

-- 呼び出し例
SELECT * FROM get_employees_after_query(1);
-- 結果は RETURN NEXT の例と同じ
```

RETURN QUERY の後に続くクエリが動的に生成される必要がある場合は、RETURN QUERY EXECUTE 構文も利用できます。

</details>

---

## **戻り値の扱い方**

関数内で値を返す方法は、返したいデータの種類によって使い分けます。

- **RETURN**:
  単一の値を返す最も一般的な方法です。
- **RETURN NEXT**:
  RETURNS SETOF を指定した関数で使用します。この文を実行するたびに、現在処理中の行を結果セットに追加し、関数は処理を継続します。すべての行を処理し終えたら、RETURN（引数なし）で関数を終了させます。
- **RETURN QUERY**:
  RETURNS SETOF を指定した関数で使用します。SELECT 文のクエリ結果全体を一度に返します。RETURN NEXT が 1 行ずつ結果を構築するのに対し、RETURN QUERY はシンプルかつ効率的に結果を返すため、結果セットを生成するロジックが単純な場合はこちらが推奨されます。

**例**:

### RETURN NEXT

```SQL
DROP TABLE IF EXISTS employees CASCADE;
CREATE TEMP TABLE employees(
	name varchar(10),salary int
);
INSERT INTO employees(name,salary) VALUES('鈴木',200000),('佐藤',100000);

-- 毎度作り直す
DROP FUNCTION IF EXISTS get_high_paid_employees();
-- RETURN NEXT を使用して結果セットを返す
CREATE FUNCTION get_high_paid_employees() RETURNS SETOF employees AS $$
DECLARE
  emp_record employees%ROWTYPE;
BEGIN
  FOR emp_record IN SELECT * FROM employees WHERE salary > 100000 LOOP
    RETURN NEXT emp_record; -- 各行を結果セットに追加
  END LOOP;
  RETURN; -- 関数を終了
END;
$$ LANGUAGE plpgsql;

-- 呼び出し
select * FROM get_high_paid_employees();

```

### RETURN QUERY

```SQL
-- 毎度削除
DROP TABLE IF EXISTS sales;
CREATE TABLE sales(
	sale_date DATE,
	sale_amount NUMERIC
);
INSERT INTO sales(sale_date, sale_amount) VALUES('2025/7/7', 10000),('2025/7/8', 20000),('2025/8/7', 10000),('2025/8/8', 200000);

-- 毎度削除
DROP FUNCTION GET_SALES_DATA_BY_MONTH();
-- RETURN QUERY を使用して結果セットを返す
CREATE FUNCTION GET_SALES_DATA_BY_MONTH() RETURNS TABLE (MONTH TEXT, TOTAL_SALES NUMERIC) AS $$
BEGIN
	RETURN QUERY SELECT to_char(sale_date, 'YYYY-MM'), SUM(sale_amount) FROM sales GROUP BY to_char(sale_date, 'YYYY-MM') ORDER BY 1;
END;
$$ LANGUAGE PLPGSQL;

SELECT * FROM GET_SALES_DATA_BY_MONTH();
```

## ◇ プロシージャ

```SQL
-- プロシージャ (PROCEDURE) の例
CREATE OR REPLACE PROCEDURE log_message(message TEXT) AS $$
BEGIN
    INSERT INTO log_table (log_text) VALUES (message);
END;
$$ LANGUAGE plpgsql;

-- プロシージャの呼び出し
CALL log_message('プロシージャが呼び出されました。');
```

---

PL/pgSQL プロシージャのパターン整理
プロシージャの利用パターンは、その目的によって整理できます。

### パターン 1: アクションのみを実行する（基本的なプロシージャ）

このパターンは、特定のアクション（データの挿入、更新、削除など）を実行するだけで、呼び出し元に値を返す必要がない場合に使用します。プロシージャの実行は、最後まで処理が進むか、途中で引数なしの RETURN;が実行されると終了します。
具体例: ログテーブルに情報を記録するプロシージャ

```SQL
-- 毎度削除して作り直す
DROP PROCEDURE IF EXISTS sp_log_message(text);
-- ログを記録するプロシージャを定義
CREATE PROCEDURE sp_log_message(p_message text) LANGUAGE plpgsql AS $$
BEGIN
    INSERT INTO log_messages (message) VALUES (p_message);
END;
$$;

-- テーブル定義する
DROP TABLE IF EXISTS log_messages;
-- 前回の関数例と同様のログテーブルを仮定
CREATE TEMP TABLE log_messages (
    log_id serial PRIMARY KEY,
    message text,
    logged_at timestamp with time zone DEFAULT now()
);

-- 呼び出し例
CALL sp_log_message('Procedure was called.');
SELECT* FROM log_messages;
```

このプロシージャは、log_messages テーブルに行を挿入するというアクションのみを実行し、値を返しません。呼び出しには CALL コマンドが使われます。

<details>
<summary>パターン 2: パラメータで値を返す</summary>

### パターン 2: パラメータで値を返す

プロシージャは RETURN 文で値を返せませんが、OUT パラメータや INOUT パラメータを使うことで、処理結果を呼び出し元に返すことができます。これは、プロシージャが実行した結果（例えば、生成された ID や更新した行数など）を呼び出し元で利用したい場合に便利です。
具体例: 従業員を追加し、その新しい ID を返すプロシージャ

```SQL
-- 適当にデータを作成する
DROP TABLE IF EXISTS employees CASCADE;
-- 従業員テーブルを仮定
CREATE TABLE employees (
    emp_id serial PRIMARY KEY,
    emp_name text
);

-- 毎度削除して作り直す
DROP PROCEDURE IF EXISTS sp_add_employee(text,int);
-- 従業員を追加し、生成されたIDをOUTパラメータで返す
CREATE PROCEDURE sp_add_employee(
    IN p_emp_name text,
    OUT p_new_emp_id integer
)
LANGUAGE plpgsql AS $$
BEGIN
    INSERT INTO employees (emp_name) VALUES (p_emp_name)
    RETURNING emp_id INTO p_new_emp_id; -- INSERTのRETURNING句でOUT変数に値を設定
END;
$$;

-- 呼び出し例 (psqlでの変数を使った呼び出し)
DO $$
DECLARE
    new_id integer;
BEGIN
    CALL sp_add_employee('New Employee', new_id);
	-- callのuot変数で値がセットされて帰ってくる
    RAISE NOTICE 'New employee ID is: %', new_id;
END;
$$;
-- NOTICE: New employee ID is: 1 (実行ごとにIDは変わります)
```

この例では、OUT パラメータ p_new_emp_id を通じて、新しく生成された emp_id を呼び出し元に返しています。

</details>

### パターン 3: トランザクションを制御する

プロシージャの最も強力な機能の一つが、プロシージャ内部でトランザクションを管理できることです。これにより、複数の SQL 文をグループ化し、一連の処理がすべて成功した場合にのみ COMMIT し、途中で問題が発生した場合には ROLLBACK する、といった複雑なロジックを単一のプロシージャ内にカプセル化できます。これは関数では実現できません。
具体例: 複数のテーブルを更新し、最後にコミットするプロシージャ

```SQL
-- 削除
DROP TABLE IF EXISTS accounts;
DROP TABLE IF EXISTS balance_log;
-- 口座テーブルと残高ログテーブルを仮定
CREATE TABLE accounts (account_no integer PRIMARY KEY, balance numeric);
CREATE TABLE balance_log (log_id serial, account_no integer, change_amount numeric, logged_at timestamptz);
INSERT INTO accounts VALUES (1, 1000), (2, 1000);

-- 口座振替を行い、トランザクションを内部で完結させるプロシージャ
CREATE or replace PROCEDURE sp_transfer_money(
    p_from_account integer,
    p_to_account integer,
    p_amount numeric
)
LANGUAGE plpgsql
AS $$
BEGIN
    -- 口座残高を更新
    UPDATE accounts SET balance = balance - p_amount WHERE account_no = p_from_account;
    UPDATE accounts SET balance = balance + p_amount WHERE account_no = p_to_account;

    -- ログを記録
    INSERT INTO balance_log (account_no, change_amount) VALUES (p_from_account, -p_amount);
    INSERT INTO balance_log (account_no, change_amount) VALUES (p_to_account, p_amount);

    -- すべての処理が成功したらコミット
    COMMIT;
END;
$$;
```

```SQL
-- 呼び出し例
CALL sp_transfer_money(1, 2, 100);
```

このプロシージャを CALL すると、4 つのデータ変更文が実行され、最後に COMMIT されて一連の処理がアトミックに完了します。もし途中でエラーが発生した場合（例えば、制約違反など）、プロシージャは中断し、COMMIT に到達しないため、それまでの変更は自動的にロールバックされます。

---

まとめ
PL/pgSQL のプロシージャは、特定の値を計算して返すのではなく、一連のアクションや手続きを実行するために設計されています。その最大の特徴は、プロシージャ内でトランザクションを制御できる点にあります。
• アクションの実行: 値を返さずにデータの挿入・更新・削除などの操作を行う。
• 値の"返却": OUT や INOUT パラメータを介して、処理結果を呼び出し元に伝える。
• トランザクション制御: COMMIT や ROLLBACK をプロシージャ内に含めることで、複雑な一連の処理を単一の完結した単位としてまとめることが可能。

## 変数とスコープ

変数は DECLARE ブロックで宣言します。データ型を指定することで、格納するデータの種類を明確にします。

DECLARE ブロックは、実行ブロック (BEGIN...END) の前に置かれ、PL/pgSQL コード内で使用するすべての変数、定数、カーソル、および行型を定義する役割を担います。

### **スコープと変数の寿命**:

PL/pgSQL の変数は、それを定義した BEGIN...END ブロック内で有効です。ブロックが終了すると、そのブロックで宣言された変数は消滅します。**ネストしたブロック内で同じ名前の変数を宣言すると、内側のブロックでは新しい変数が優先され、外側の変数は一時的に隠されます**。これは、コードの意図しない挙動を引き起こす可能性があるため、注意が必要です。

#### 1. 基本的なスコープ：内側から外側へのアクセス

PL/pgSQL では、BEGIN...END で囲まれたブロックを入れ子（ネスト）にすることができます 。内側のブロックは、外側のブロックで宣言された変数に自由にアクセスできます。
例：

```SQL
DO$$

DECLARE
-- 外側のブロックで変数を宣言
outer_variable INTEGER := 10;
BEGIN
  RAISE NOTICE '外側のブロック: outer_variable = %', outer_variable;

  -- 内側のブロックを開始
  DECLARE
    -- 内側のブロックで別の変数を宣言
    inner_variable INTEGER := 20;
  BEGIN
    -- 内側のブロックから外側の変数にアクセス
    RAISE NOTICE '内側のブロック: outer_variable = %', outer_variable;
    RAISE NOTICE '内側のブロック: inner_variable = %', inner_variable;
  END;
  -- 内側のブロックが終了

-- inner_variable はスコープ外のため、ここではアクセスできない
-- RAISE NOTICE '外側のブロック: inner_variable = %', inner_variable; -- これはエラーになる
END;
$$;
```

この例では、内側のブロックが外側の outer_variable を問題なく参照できています。変数の有効範囲は、その変数が宣言されたブロックとその内側のブロックすべてになります。

---

#### 2. 名前の衝突と解決策：ブロックラベル

内側のブロックで、外側のブロックに存在する変数と同じ名前の変数を宣言した場合、外側の変数はその内側のブロックの期間中、隠蔽（hiding）されます 。この状態では、その名前を単純に参照すると内側の変数を指すことになります。
しかし、このような状況でも外側の変数にアクセスする方法があります。それがブロックラベルです 。ブロックの先頭に<<label>>という形式でラベルを付け、そのラベル名で修飾することで、隠蔽された外側の変数を参照できます 。
ブロックの構文は以下のようになります 。

```SQL
[ <<label>> ]
DECLARE
    ...
BEGIN
    ...
END [ label ];
```

例：同じ名前の変数をラベルで修飾してアクセスする

```SQL
DO $$
<<outer_block>>
DECLARE
-- 外側のブロックで変数を宣言
x INTEGER := 10;
BEGIN
  RAISE NOTICE '外側のブロック（開始時）: x = %', x; -- -> 10

  -- 内側のブロックを開始
  DECLARE
    -- 同じ名前 'x' で内側の変数を宣言
    x INTEGER := 20;
  BEGIN
    -- 'x' は内側の変数を指す
    RAISE NOTICE ' 内側のブロック: x = %', x; -- -> 20

    -- ラベルを使って外側の変数を修飾してアクセスする
    RAISE NOTICE '  内側のブロック: outer_block.x = %', outer_block.x;  -- -> 10

  END;
  -- 内側のブロックが終了

  RAISE NOTICE '外側のブロック（終了時）: x = %', x; -- -> 10
END outer_block;

$$
;
```

後述の if 文や for 文などもブロックとしてラベルを付けることができる。

```SQL
DO $$
BEGIN
  <<outer_loop>>
  FOR i IN 1..3 LOOP
    RAISE NOTICE '外側ループ: i = %', i;

    <<inner_loop>>
    FOR j IN 1..3 LOOP

      IF i = 2 AND j = 2 THEN
        -- 'outer_loop'の次の繰り返しに進む（ブロックにラベルを付けてるとそれを指定して抜けられる）
        CONTINUE outer_loop;
      END IF;
      RAISE NOTICE ' 内側ループ: i=%, j=%', i, j;

    END LOOP;

  END LOOP;

END;
$$;
-- 実行結果：
-- NOTICE:  外側ループ: i = 1
-- NOTICE:    内側ループ: i=1, j=1
-- NOTICE:    内側ループ: i=1, j=2
-- NOTICE:    内側ループ: i=1, j=3
-- NOTICE:  外側ループ: i = 2
-- NOTICE:    内側ループ: i=2, j=1
-- NOTICE:  外側ループ: i = 3
-- NOTICE:    内側ループ: i=3, j=1
-- NOTICE:    内側ループ: i=3, j=2
-- NOTICE:    内側ループ: i=3, j=3
```

この例では、内側のブロックで x を宣言したため、外側の x は隠蔽されます。しかし、outer_block.x と記述することで、外側のブロックの x に明確にアクセスすることができています。

#### まとめ

- 通常アクセス: PL/pgSQL では、内側のブロックは外側のブロックで宣言された変数をそのまま参照できます 。
- 名前の衝突（隠蔽）: 内側のブロックで外側と同じ名前の変数を宣言すると、外側の変数はそのスコープ内で隠蔽されます 。
- 隠蔽された変数へのアクセス: ブロックにラベル（例: <<outer>>）を付け、label.variable_name（例: outer.x）という形式で修飾することで、内側のブロックから隠蔽された外側の変数にアクセスできます 。

## 制御構造（条件分岐）

PL/pgSQL では、一般的なプログラミング言語と同様に、条件分岐やループを使ってロジックの流れを制御できます。

### **IF-THEN-ELSE**

条件に基づいて処理を分岐させます。

**例**:

```SQL
DO $$
DECLARE
  EMP_SALARY NUMERIC := 120000;
BEGIN
  IF EMP_SALARY > 100000 THEN RAISE NOTICE '高給取り';
  ELSEIF EMP_SALARY > 50000 THEN RAISE NOTICE '平均的な給与';
  ELSE RAISE NOTICE '低めの給与';
  END IF;

END $$

```

### **CASE**

複数の条件分岐をより簡潔に記述します。

**例**:

```SQL
DO $$
DECLARE
	employee_department VARCHAR := 'IT';
BEGIN
	CASE employee_department
	WHEN 'Sales' THEN
		RAISE NOTICE '営業部門';
	WHEN 'IT' THEN
		RAISE NOTICE 'IT 部門';
	ELSE
		RAISE NOTICE 'その他の部門';
	END CASE;
END $$
```

### **IF 文と CASE 文の使い分け**

IF 文と CASE 文はどちらも条件分岐に使いますが、その目的と最適な使い方が異なります。

- **IF 文**: **複数の異なる条件**を評価する際に適しています。特に、条件が論理式（>,<,AND,OR など）で表現され、それぞれが独立している場合に真価を発揮します。例として、給与の範囲に応じて異なるメッセージを出力するような場合です。
- **CASE 文**: **単一の変数が持つ複数の値**に基づいて処理を分岐させる際に最適です。これにより、コードがより簡潔で読みやすくなります。特に、列挙型のような事前に決められた値（例: 'Sales', 'IT')に沿って処理を分けたい場合に非常に有効です。

CASE 文には、単一の値と比較する**シンプル CASE**と、論理式を記述する**検索 CASE**の 2 つの形式があります。機能的に見れば検索 CASE は IF-THEN-ELSEIF とほぼ同じですが、**コードの意図を明確にする**という点で使い分けが重要になります。⇒ 要は、検索 case を使用すれば if 文と同じ表現をできるけども、、、その場合は if 文を使うのが見やすい。

## 制御構造（繰り返し）

### **ループ処理のバリエーションと使い分け**

PL/pgSQL には、様々な状況に対応できるよう複数のループ構文が用意されています。それぞれの特性を理解し、適切なものを選ぶことがパフォーマンスと可読性の向上につながります。

- **LOOP ... END LOOP**:
  最も基本的なループで、条件なしで無限に繰り返します。ループを終了させるためには、EXIT または RETURN を明示的に使用する必要があります。このループは、特定の条件が満たされるまで繰り返すことが決まっている場合に適しています。
- **WHILE ... LOOP**:
  ループに入る前に条件を評価し、条件が真である限り繰り返します。条件が偽になった時点でループを終了します。ループの反復回数が事前に不明で、特定の条件が満たされるまで処理を続けたい場合に適しています。
- **FOR ... IN ... LOOP**:
  クエリの結果セットを 1 行ずつ処理するのに最適な、**最も一般的に使われるループ**です。ループ変数は自動的に宣言され、クエリが返す各行が代入されます。これにより、コードが簡潔で読みやすくなります。大量のデータを反復処理する際には、できるだけこの構文を使うことが推奨されます。
- **FOREACH ... IN ARRAY ... LOOP**:
  配列の要素を一つずつ処理するのに最適なループです。この構文は、データベースから取得した配列データや、ローカルで宣言した配列を簡単に反復処理する際に非常に役立ちます。

### **ループの制御**:

- **EXIT**: ループを完全に終了します。EXIT WHEN という形で条件を指定することもできます。
- **CONTINUE**: ループの現在のイテレーションをスキップし、次のイテレーションに進みます。CONTINUE WHEN という形で条件を指定することもできます。

<details>
<summary>1. ：無条件ループ（LOOP ... END LOOP）</summary>

### 1. ：無条件ループ（LOOP ... END LOOP）

LOOP は最も基本的なループ形式で、EXIT 文によって明示的に終了されるまで無限に処理を繰り返します。ループの終了条件が複雑な場合や、ループ処理の途中で終了判定を行いたい場合に適しています。
構文:

```SQL
[ <<label>> ]
LOOP
    statements
END LOOP [ label ];
```

具体例：カウンタが 5 を超えたらループを抜ける この例では、カウンタ変数 i を 1 ずつ増やしていき、5 を超えた時点で EXIT 文を使ってループを終了します。

```SQL
DO $$
DECLARE
  i INTEGER := 0;
BEGIN
  LOOP
    i := i + 1;
    RAISE NOTICE 'i = %', i;
    -- 終了条件をループ内で評価
    IF i > 5 THEN
      EXIT;  -- iが5を超えたらループを抜ける
    END IF;
  END LOOP;
  RAISE NOTICE 'ループを抜けました';
END;
$$;
-- 実行結果：
-- NOTICE:  i = 1
-- NOTICE:  i = 2
-- NOTICE:  i = 3
-- NOTICE:  i = 4
-- NOTICE:  i = 5
-- NOTICE:  i = 6
-- NOTICE:  ループを抜けました
-- EXIT WHEN 構文を使うと、より簡潔に記述できます。
-- EXIT WHEN i > 5;

-- DO $$
-- DECLARE
--   i INTEGER := 0;
-- BEGIN
--   LOOP
--     i := i + 1;
--     RAISE NOTICE 'i = %', i;
--     -- 終了条件をループ内で評価
--     EXIT WHEN i > 5 ;
--   END LOOP;
--   RAISE NOTICE 'ループを抜けました';
-- END;
-- $$;
```

</details>

---

<details>
<summary>2. ：条件付きループ（WHILE ... LOOP）</summary>

### 2. ：条件付きループ（WHILE ... LOOP）

WHILE ループは、指定された論理式が真（true）である間、処理を繰り返します。ループ処理に入る前に条件を評価するため、条件が最初から偽（false）の場合は一度も実行されません 。
構文:

```SQL
[ <<label>> ]
WHILE boolean-expression LOOP
    statements
END LOOP [ label ];
```

具体例：カウンタが 0 より大きい間、デクリメント（1 ずつ減算）を続ける この例では、カウンタ変数 counter が 0 より大きい間、ループ内の処理を繰り返します。

```SQL
DO $$
DECLARE
  counter INTEGER := 3;
BEGIN
  WHILE counter > 0 LOOP
    RAISE NOTICE 'Counter is %', counter;
    counter := counter - 1;
  END LOOP;
END;
$$;
-- 実行結果：
-- NOTICE:  Counter is 3
-- NOTICE:  Counter is 2
-- NOTICE:  Counter is 1
```

</details>

---

### 3. ：反復ループ（FOR ... IN ... LOOP）

FOR ループは、決まった回数や集合（整数の範囲、クエリ結果、配列など）に対して反復処理を行うための、非常に強力で便利なループです。ループ変数は自動的に宣言され、ループ終了後には破棄されます。

#### a. 整数の範囲に対するループ ()

指定された範囲の整数を反復処理します。REVERSE キーワードで降順に、BY 句でステップ（増減値）を指定することもできます。
具体例：1 から 5 まで、および 5 から 1 まで 2 ステップずつのループ

```SQL
DO $$
BEGIN
-- 1から5まで昇順にループ
RAISE NOTICE '昇順ループ:';
    FOR i IN 1..5 LOOP
        RAISE NOTICE '  i = %', i;
    END LOOP;
-- 5から1まで降順に2ステップずつループ
RAISE NOTICE '降順・2ステップループ:';
    FOR j IN REVERSE 5..1 BY 2 LOOP
        RAISE NOTICE '  j = %', j;
    END LOOP;
END;
$$;
-- 実行結果：
-- NOTICE:  昇順ループ:
-- NOTICE:    i = 1
-- NOTICE:    i = 2
-- NOTICE:    i = 3
-- NOTICE:    i = 4
-- NOTICE:    i = 5
-- NOTICE:  降順・2ステップループ:
-- NOTICE:    j = 5
-- NOTICE:    j = 3
-- NOTICE:    j = 1
```

#### b. 問い合わせ結果に対するループ ()

クエリが返す各行に対してループ処理を実行します。ループ変数はレコード型または行型変数になります。
具体例：pg_class からテーブル名を 3 つ取得して表示する

```SQL
DO $$
DECLARE
  rec RECORD;
BEGIN
  -- pg_classテーブルからリレーション名を取得
  FOR rec IN SELECT relname FROM pg_class WHERE relkind = 'r' LIMIT 3 LOOP
      RAISE NOTICE 'テーブル名: %', rec.relname;
  END LOOP;
END;
$$;
-- 実行結果（環境により異なります）：
-- NOTICE:  テーブル名: customers_mst
-- NOTICE:  テーブル名: orders_trn
-- NOTICE:  テーブル名: order_details_trn
```

---

<details>
<summary>4. ：配列に対するループ (FOREACH ... IN ARRAY ... LOOP)</summary>

### 4. ：配列に対するループ (FOREACH ... IN ARRAY ... LOOP)

配列の各要素に対して反復処理を行います。SLICE 句を使うと、多次元配列をスライス（部分配列）単位で処理することもできます。
具体例：一次元配列の各要素と、二次元配列の各スライス（行）を処理する

```SQL
DO $$
DECLARE
	-- 1 次元配列
	arr_1d INTEGER[] := ARRAY[1,2,3];
	-- 2 次元配列
	arr_2d INTEGER[][] := ARRAY[[4, 5],[6, 7]];
	val INTEGER;
	slice_val INTEGER[];
BEGIN
	-- 1 次元配列の各要素をループ
	FOREACH val IN ARRAY arr_1d LOOP
		RAISE NOTICE '1 次元配列の要素: %', val;
	END LOOP;

	-- 2 次元配列をスライス（1 次元配列）単位でループ
	FOREACH slice_val SLICE 1 IN ARRAY arr_2d LOOP
		RAISE NOTICE '2 次元配列のスライス: %', slice_val;
	END LOOP;
END
$$;

-- 実行結果：
-- NOTICE:  1次元配列の要素: 10
-- NOTICE:  1次元配列の要素: 20
-- NOTICE:  1次元配列の要素: 30
-- NOTICE:  2次元配列のスライス: {1,2}
-- NOTICE:  2次元配列のスライス: {3,4}
```

</details>

---

### ループの制御 ()

すべてのループ形式で CONTINUE 文を使用できます。これは現在の反復処理をスキップし、次の反復処理を開始します。
具体例：偶数の場合だけ処理をスキップする

```SQL
DO $$
BEGIN
    FOR i IN 1..5 LOOP
        -- i が偶数の場合は、この反復の残りの処理をスキップ
        CONTINUE WHEN i % 2 = 0;
        RAISE NOTICE 'i = % (奇数)', i;
    END LOOP;
END;
$$;
-- 実行結果：
-- NOTICE:  i = 1 (奇数)
-- NOTICE:  i = 3 (奇数)
-- NOTICE:  i = 5 (奇数)
```

---

<details>
<summary>カーソル（Cursors）の詳細</summary>

### **カーソル（Cursors）の詳細**

FOR ループが暗黙的にカーソルを使用する一方、PL/pgSQL では**明示的なカーソル**を宣言して使用することもできます。これは、より複雑なデータ処理フローや、結果セットを部分的に処理する必要がある場合に役立ちます。

**カーソルのライフサイクル**:

1. **宣言 (DECLARE)**: カーソルを宣言し、それに関連付けるクエリを定義します。
2. **オープン (OPEN)**: カーソルを開き、クエリを実行して結果セットを生成します。
3. **フェッチ (FETCH)**: 結果セットから 1 行ずつデータを取得し、変数に代入します。FETCH FORWARD n を使うと、一度に n 行を取得する**バッチ処理**が可能で、パフォーマンス改善に役立ちます。
4. **クローズ (CLOSE)**: カーソルを閉じ、使用していたリソースを解放します。

**実務での注意点**:

- **トランザクションをまたげない**: 明示的なカーソルは、それをオープンしたトランザクション内でのみ有効です。COMMIT や ROLLBACK を実行するとカーソルは自動的に閉じられます。ただし、WITH HOLD を指定したセッションカーソルは、トランザクションをまたいで利用できます。
- **ドライバのフェッチサイズ**: JDBC などのデータベースドライバは、クエリ結果を一度にすべてメモリにロードするのではなく、FETCH FORWARD と同様に、指定された行数ずつフェッチする設定を持つことが多いです。PL/pgSQL 内で明示的なカーソルを使う場合と、アプリケーション側で大量データを扱う場合とで、パフォーマンスの考慮点が異なることを理解しておきましょう。

**使い分けのベストプラクティス**:

ほとんどの場合、FOR ... IN ... LOOP が最もシンプルで効率的な選択肢です。**明示的なカーソルは、以下のような特定の状況でのみ使用します。**

- ループ中に**動的な条件**でカーソルを**再定義**する必要がある場合。
- ループの途中で、複数のカーソルを切り替える必要がある場合。
- 結果セット全体ではなく、**特定の行数だけを処理したい**場合。

</details>

---

## **例外処理 (Exception Handling)**

本番環境で堅牢なコードを記述するには、予期せぬエラーに備えることが不可欠です。EXCEPTION ブロックを使用することで、エラーが発生しても処理が中断することなく、安全にリカバリできます。

**特定の例外を捕捉する**:

PL/pgSQL では、一般的なエラー条件（no_data_found, unique_violation など）や、SQLSTATE コード（5 桁の英数字コード）を基にした**より具体的な例外**を捕捉できます。

**例**:

```SQL
DROP TABLE IF EXISTS customers;
CREATE TABLE customers(
	id int, name varchar
);

DO $$
-- 存在しない顧客 ID を指定
DECLARE
    customer_id INT := 99999;
    customer_name VARCHAR;
BEGIN
  SELECT name INTO STRICT customer_name FROM customers WHERE id = customer_id;
  RAISE NOTICE '例外発生時に到達しない処理';
EXCEPTION
  WHEN NO_DATA_FOUND THEN -- データが見つからないエラー
    RAISE NOTICE '顧客 ID % は見つかりませんでした。', customer_id;
  WHEN TOO_MANY_ROWS THEN -- 複数行が返されたエラー
    RAISE EXCEPTION 'ID % に複数の顧客が存在します。', customer_id;
  WHEN unique_violation THEN -- 一意性制約違反エラー
    RAISE NOTICE '一意性違反です。';
END;
$$ LANGUAGE plpgsql;
```

**RAISE 文とエラーのカスタマイズ**:

RAISE 文を使用すると、ユーザー定義の例外を意図的に発生させることができます。

- RAISE NOTICE/INFO/WARNING: ログレベルのメッセージを出力し、処理は継続します。デバッグに非常に役立ちます。
- RAISE EXCEPTION: 処理を中断し、エラーを発生させます。USING 句で詳細な情報を付加できます。

**例**:

```SQL
RAISE EXCEPTION '無効なユーザー ID が指定されました' USING ERRCODE = '22000', DETAIL = 'ユーザー ID は NULL にできません。';
```

**エラーの伝播とトランザクション制御**:

PL/pgSQL 関数は、通常、呼び出し元のトランザクション内で実行されます。関数内でエラーが発生すると、デフォルトでは**トランザクション全体がロールバックされます**。

部分的なロールバックが必要な場合は、SAVEPOINT を組み合わせて使用します。これにより、特定の地点まで処理を戻すことが可能になります。これは、バッチ処理で一部のレコードだけが失敗した場合に、残りのレコードの処理を継続したい場合に有効です。

**入れ子になった例外ブロック**:

BEGIN...EXCEPTION...END はネストできるため、より詳細なエラーハンドリングが可能です。外側のブロックが全体的なリカバリを担当し、内側のブロックが特定のサブ処理の失敗を捕捉する、といった使い方ができます。

**例**:

```SQL
DO $$
BEGIN
  -- 全体処理の開始
  BEGIN
    -- 特定のサブ処理
	RAISE NOTICE '内側の処理：ここでDBにinsertしたと仮定して、ユニークキーエラーが出たとする。';
    RAISE EXCEPTION unique_violation; -- ここで例外発生し、以降の処理をスキップしてExceptionブロックに移る。
	  RAISE NOTICE '内側の処理：ここはスキップされる。';
  EXCEPTION
    WHEN unique_violation THEN
    RAISE NOTICE '重複エラーが発生しました。この処理はスキップします。'; -- この後外側の処理に戻る（呼び出し元に帰る）。
  END;

  -- 内側のブロックでエラーが発生したが、内側で補足されたため、外側に処理が戻る
  RAISE NOTICE '外側の処理：ここは処理される';

END;
$$ LANGUAGE plpgsql;
```

---

## **動的 SQL（Dynamic SQL）**　詳細は補足資料参照

PL/pgSQL 内で、実行時に SQL 文を動的に生成・実行する機能です。テーブル名やカラム名が変数によって変わるような柔軟な処理を実装する際に役立ちます。

**動的 SQL のセキュリティリスク：SQL インジェクション**

ユーザーからの入力を直接 SQL 文字列に連結すると、悪意のあるユーザーが意図しないコマンドを挿入し、データベースを破壊する可能性があります。これを**SQL インジェクション**と呼びます。

**安全な動的 SQL のベストプラクティス**:

SQL インジェクションを防ぐため、**プレースホルダーを使用する**ことが絶対的なベストプラクティスです。format()関数と EXECUTE ... USING を適切に使い分けることが重要です。

- **EXECUTE ... USING 句**: **リテラル値（検索条件の値など）**をバインドする際に、最も安全で効率的な方法です。これは、値が SQL 文に直接埋め込まれるのではなく、パラメータとして渡されるため、SQL インジェクションを完全に防ぎ、実行速度も最適化されます。
- **format()関数**: **識別子（テーブル名やカラム名）**を動的に指定する際に使います。%I フォーマット指定子を使用することで、自動的に識別子を二重引用符で囲み、安全に挿入します。

**使い分けの例**:

```SQL
DECLARE
  table_name TEXT := 'user_data';
  user_id INT := 123;
  result_count INT;
BEGIN
  -- 識別子（テーブル名）には format()と%I を使用
  -- 値（user_id）には USING 句を使用
  EXECUTE format('SELECT COUNT(*) FROM %I WHERE id = $1', table_name) -- $1とすることで、usingの1つ目の値で書き換える
  INTO result_count
  USING user_id;

  RAISE NOTICE 'テーブル % にID % のユーザーが % 人見つかりました。', table_name, user_id, result_count;

END;
```

---

## **トリガプロシージャ (Trigger Procedures)**

トリガプロシージャは、INSERT、UPDATE、DELETE などの特定のイベントがテーブルで発生した際に自動的に実行される特別な関数です。データの整合性維持、監査ログの記録、複雑なビジネスルールの適用などに不可欠な機能です。

**トリガの種類**:

- **BEFORE / AFTER**: イベント発生前か後に実行するかを制御します。
  - BEFORE: データの変更前に実行され、NEW レコードを修正することで、挿入・更新される値を変更できます。
  - AFTER: データの変更が完了した後に実行され、監査ログ記録など、別のテーブルへの操作に主に使われます。
- **INSTEAD OF**: ビューに対する INSERT/UPDATE/DELETE 操作を、実際のテーブルに対する操作に置き換えます。
- **行レベル・文レベル**:
  - **FOR EACH ROW (行レベル)**: 変更される**各行ごと**にトリガが実行されます。NEW と OLD 変数にアクセスできます。
  - **FOR EACH STATEMENT (文レベル)**: INSERT や UPDATE 文全体に対して**1 回だけ**実行されます。NEW や OLD は利用できませんが、大量のデータ変更時にオーバーヘッドを減らせます。

**実務での注意点：トリガの副作用**:

トリガは強力なツールですが、不用意に使うとパフォーマンスの問題や予期しない挙動を引き起こす可能性があります。特に注意すべきは**トリガの再帰的な発火**です。

トリガ内で同じテーブルを UPDATE するロジックを記述すると、UPDATE 操作がトリガを再度発火させ、無限ループに陥る可能性があります。これを防ぐには、以下のような工夫が必要です。

- **条件の追加**: トリガ内で WHEN 句や IF 文を使って、特定の条件が満たされたときのみ処理を実行する。
- **トリガ無効化**: ALTER TABLE ... DISABLE TRIGGER を使って、一時的にトリガを無効化する。
- **トリガ関数内のチェック**: TG_OP（操作の種類）、TG_LEVEL（行レベルか文レベルか）、TG_WHEN（BEFORE か AFTER か）などの特殊変数を使い、再帰的な呼び出しを防ぐロジックを実装する。

## **実務で役立つ Tips**

- **デバッグ**:
  - RAISE NOTICE: 開発中に変数の値を確認するのに最も手軽な方法です。
  - plpgsql.print_strict_params: PostgreSQL の GUC 設定（General User Configuration）で、SELECT INTO STRICT が 0 行や複数行を返したときに、より詳細なデバッグ情報（どのパラメータが問題かなど）をログに出力できます。
- **セキュリティ定義子**:
  - **SECURITY DEFINER**: 関数を定義したユーザーの権限で実行されます。これにより、権限の低いユーザーでも、通常はアクセスできないデータにアクセスする関数を実行できます。
  - **SECURITY INVOKER**: 関数を呼び出したユーザーの権限で実行されます。これがデフォルトの挙動です。

SECURITY DEFINER を使う際は、**SQL インジェクションのリスクが非常に高くなる**ため、動的 SQL を絶対に避け、すべての入力値を厳密に検証することが不可欠です。

- **静的解析**:
  - plpgsql_check 拡張機能を使用すると、PL/pgSQL コードの静的解析を行い、実行前に潜在的なバグ（変数の使用間違いなど）を検出できます。実運用前のコードレビューに組み込むと非常に有用です。
    $$

```

```
