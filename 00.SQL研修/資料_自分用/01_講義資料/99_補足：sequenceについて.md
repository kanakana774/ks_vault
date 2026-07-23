---
tags:
  - SQL/補足
---
## PostgreSQLのシーケンス解説

PostgreSQLにおけるシーケンスは、一意の連続した整数を生成するためのオブジェクトです。 主にテーブルの主キー（プライマリキー）のように、重複しないユニークな番号を自動で割り振りたい場合などに非常に役立ちます。

### 1. シーケンスとは？

シーケンスは、`CREATE SEQUENCE`コマンドによって作成される、番号を生成するための特別なデータベースオブジェクトです。 テーブルとは独立しており、複数のテーブルから共通で利用することも可能です。

主な特徴は以下の通りです。

*   **一意な数値の生成**: 呼び出されるたびに、重複しない数値を返します。
*   **自動採番**: INSERT文で値を指定しなくても、自動的に連番を割り当てることができます。
*   **独立したオブジェクト**: テーブルとは別に管理されるため、異なるテーブル間で共有できます。

### 2. シーケンスの作成 (`CREATE SEQUENCE`)

シーケンスは`CREATE SEQUENCE`文を使用して作成します。

**基本的な構文:**
```sql
CREATE SEQUENCE シーケンス名
    [ INCREMENT [ BY ] 増分値 ]
    [ MINVALUE 最小値 | NO MINVALUE ]
    [ MAXVALUE 最大値 | NO MAXVALUE ]
    [ START [ WITH ] 開始値 ]
    [ CACHE キャッシュ数 ]
    [ [ NO ] CYCLE ];
```

**主要なオプション:**

| オプション | 説明 | デフォルト値 |
| --- | --- | --- |
| `INCREMENT BY` | 一度に増加または減少する値を指定します。 | 1 |
| `MINVALUE` | シーケンスが生成する最小値を指定します。 | 昇順の場合は1、降順の場合は-9223372036854775807 |
| `MAXVALUE` | シーケンスが生成する最大値を指定します。 | 昇順の場合は9223372036854775807、降順の場合は-1 |
| `START WITH` | 最初の値を指定します。 | `MINVALUE`（昇順）または`MAXVALUE`（降順） |
| `CACHE` | 高速化のためにメモリ上に事前に確保しておくシーケンスの数。 | 1 |
| `CYCLE` / `NO CYCLE` | 最大値または最小値に達した際に、値を繰り返す(`CYCLE`)か、エラーを発生させる(`NO CYCLE`)か指定します。 | `NO CYCLE` |

**作成例:**
```sql
-- 1から始まり1ずつ増加する最もシンプルなシーケンス
CREATE SEQUENCE user_id_seq;

-- 100から始まり、最大値9999まで5ずつ増加するシーケンス
CREATE SEQUENCE product_code_seq
    INCREMENT BY 5
    START WITH 100
    MAXVALUE 9999;
```

### 3. シーケンスの使い方

シーケンスを操作するには、主に`nextval()`と`currval()`という2つの関数を使用します。

*   **`nextval('シーケンス名')`**: シーケンスを進めて、**次の新しい値**を取得します。 `nextval()`を呼び出すと、シーケンスの値は実際に進みます。
*   **`currval('シーケンス名')`**: 現在のセッションで`nextval()`によって取得された**最後の値**を取得します。 `currval()`を使用するには、そのセッション内で少なくとも一度`nextval()`を呼び出している必要があります。

**使用例:**
```sql
-- 次のシーケンス番号を取得
SELECT nextval('user_id_seq');

-- 現在のシーケンス番号を取得
SELECT currval('user_id_seq');
```

### 4. テーブルでの利用方法

シーケンスは、テーブルの列のデフォルト値として設定することで、データの挿入時に自動で採番されるようにするのが最も一般的な使い方です。

#### `DEFAULT`句で指定する方法

`CREATE TABLE`文の列定義で、`DEFAULT nextval('シーケンス名')`と指定します。

```sql
CREATE TABLE users (
    id INTEGER PRIMARY KEY DEFAULT nextval('user_id_seq'),
    username VARCHAR(50)
);

-- idを省略してINSERTすると、シーケンスから自動で値が設定される
INSERT INTO users (username) VALUES ('Alice');
INSERT INTO users (username) VALUES ('Bob');
```

#### `SERIAL`型を使用する方法

PostgreSQLには、シーケンスの作成と列への設定を自動的に行ってくれる`SERIAL`、`BIGSERIAL`という便利な疑似データ型があります。

*   `SERIAL`: 4バイト整数（INTEGER）のシーケンスを作成します。
*   `BIGSERIAL`: 8バイト整数（BIGINT）のシーケンスを作成します。

`SERIAL`型を指定すると、内部的に「`テーブル名_列名_seq`」という名前のシーケンスが自動で作成され、その列に割り当てられます。

```sql
CREATE TABLE products (
    product_id SERIAL PRIMARY KEY,
    product_name VARCHAR(100)
);

-- product_idを指定せずにINSERTする
INSERT INTO products (product_name) VALUES ('Laptop');
INSERT INTO products (product_name) VALUES ('Keyboard');
```

### 5. シーケンスの管理

#### 現在値の確認と設定

*   **現在値の確認**: `currval()`の他に、シーケンス自体を`SELECT`文で問い合わせることで、`last_value`（最後に払い出された値）を確認できます。
    ```sql
    SELECT last_value FROM user_id_seq;
    ```
*   **値の設定・リセット**: `setval()`関数を使うことで、シーケンスの現在値を任意の値に設定できます。
    ```sql
    -- シーケンスの現在値を100に設定する
    -- 第3引数をfalseにすると、次にnextval()を呼び出した際に設定した値そのものが返される
    SELECT setval('user_id_seq', 100, false);
    ```

#### シーケンスの定義変更 (`ALTER SEQUENCE`)

作成済みのシーケンスの各種パラメータは`ALTER SEQUENCE`文で変更できます。

```sql
-- user_id_seqの増分値を10に変更
ALTER SEQUENCE user_id_seq INCREMENT BY 10;

-- user_id_seqの開始値を1000からにリスタートする
ALTER SEQUENCE user_id_seq RESTART WITH 1000;
```

#### 所有権の関連付け (`OWNED BY`)

`SERIAL`型を使用した場合、テーブルの列が削除されると関連するシーケンスも自動的に削除されます。手動でシーケンスを作成した場合も、`OWNED BY`句を使うことで同様の関連付けが可能です。

```sql
-- user_id_seqをusersテーブルのid列に関連付ける
ALTER SEQUENCE user_id_seq OWNED BY users.id;
```

### 6. 注意点

*   **欠番の発生**: `nextval()`を呼び出したトランザクションがロールバックされた場合でも、シーケンスの値は元に戻りません。 そのため、シーケンスには欠番が発生する可能性があります。
*   **`currval()`のセッション**: `currval()`は、`nextval()`を呼び出したセッション内でのみ有効です。 別のセッションからでは値を取得できません。
*   **直接指定によるズレ**: テーブルの主キーにシーケンスを設定していても、INSERT時に値を直接指定すると、シーケンスの現在値との間にズレが生じることがあります。