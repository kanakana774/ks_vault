### I. PL/pgSQL入門 (Introduction to PL/pgSQL)

この単元では、PL/pgSQLがどのようなもので、なぜ必要なのか、そして基本的なコードの書き方について学びます。

#### 1. 概要と目的

##### PL/pgSQLとは何か？

PL/pgSQLは「Procedural Language/PostgreSQL」の略で、PostgreSQLで利用できる手続き型言語の一つです。標準のSQLに、変数、条件分岐（IF文）、ループ（FOR文）といった一般的なプログラミング言語の機能を追加したものです。

##### なぜSQLだけでは不十分なのか？

SQLはデータベースからデータを取り出したり、更新したりするのに非常に強力な言語です。しかし、以下のような複雑な処理はSQLだけでは記述が困難、あるいは不可能です。

*   **複雑な条件分岐:** 「もしAという条件ならXという処理、Bという条件ならYという処理、それ以外ならZという処理をする」といった制御。
*   **繰り返し処理:** クエリの結果一件一件に対して、何らかの連続した処理を行う。
*   **エラーハンドリング:** 処理の途中でエラーが発生した場合に、処理を中断せずに別の処理に切り替える。
*   **ロジックの集約:** 複数のSQL文を一つにまとめ、データベースサーバー側で一連の処理として実行させたい。これにより、アプリケーションとデータベース間の通信回数が減り、パフォーマンスが向上します。

PL/pgSQLは、これらの複雑なロジックをデータベース内で完結させるために使用されます。

#### 2. 基本構造

PL/pgSQLのコードは「ブロック」という単位で構成されます。このブロック構造を理解することが非常に重要です。

##### ブロック構造：`DECLARE`, `BEGIN`, `END`

PL/pgSQLの基本は、以下のキーワードで構成されるブロックです。

```sql
[ DECLARE ]
    -- 変数やカーソルの宣言をここで行う（省略可能）
BEGIN
    -- 実行したい処理（SQL文や制御構造）をここに記述する
[ EXCEPTION ]
    -- エラーが発生した場合の処理をここに記述する（省略可能）
END;
```

*   `DECLARE`: このブロック内で使用する変数を宣言するセクションです。
*   `BEGIN`: 実際の処理を記述するメインセクションです。最低限、`BEGIN`と`END`は必須です。
*   `EXCEPTION`: `BEGIN`ブロック内でエラーが発生したときに、そのエラーを捕捉して特定の処理を行うためのセクションです。
*   `END`: ブロックの終わりを示します。

##### ブロックのネスト（入れ子構造）
PL/pgSQLのブロックは、**入れ子（ネスト）にすることができます**。つまり、`BEGIN ... END`ブロックの中に、さらに別の`DECLARE ... BEGIN ... END`ブロックを記述できます。これにより、変数が有効な範囲（**スコープ**）を限定し、より構造化されたコードを書くことが可能になります。

**スコープのルール:**
*   外側のブロックで宣言された変数は、内側のブロックからも参照できます。
*   内側のブロックで宣言された変数は、そのブロックの中でしか使えず、外側のブロックからは参照できません。

**具体例：ネストしたブロックと変数のスコープ**
```sql
DO $$
DECLARE
    v_outer_scope_var text := 'I am in the outer block';
BEGIN
    RAISE NOTICE '%', v_outer_scope_var;

    -- 内側のブロックを開始
    DECLARE
        v_inner_scope_var text := 'I am in the inner block';
    BEGIN
        RAISE NOTICE '  (Inner) -> %', v_outer_scope_var; -- 外側の変数を参照できる
        RAISE NOTICE '  (Inner) -> %', v_inner_scope_var;
    END;
    -- 内側のブロックが終了

    RAISE NOTICE '%', v_outer_scope_var;
    -- 下の行のコメントを外して実行するとエラーになる
    -- RAISE NOTICE '%', v_inner_scope_var; -- ERROR: "v_inner_scope_var" does not exist
END;
$$ LANGUAGE plpgsql;
```

##### 匿名ブロック (`DO`)

一度きりの処理や、テスト的にコードを実行したい場合に便利なのが「匿名ブロック」です。`DO`コマンドを使うことで、関数を作成しなくても直接PL/pgSQLコードを実行できます。

**具体例： "Hello, PL/pgSQL!" と表示する**
```sql
DO $$
BEGIN
    RAISE NOTICE 'Hello, PL/pgSQL!';
END;
$$ LANGUAGE plpgsql;
```
**【補足】`$$` (ドル記号クォーティング) とは？**
PL/pgSQLのコード本体は、`' ... '` というシングルクォートで囲むのが伝統的な方法ですが、コード内にシングルクォートが含まれる場合にエスケープ (`''`) が必要になり、非常に読みにくくなります。

ドル記号クォーティング (`$$ ... $$`) は、このようなエスケープを不要にするための、より現代的で推奨される記法です。`$tag$` のようにタグを付けることもでき、ネストしたコードを書く際に便利です。

（エスケープして書いてた例）
```SQL
DO '
DECLARE
    v_outer_scope_var text := ''I am in the outer block'';
BEGIN
    RAISE NOTICE ''%'', v_outer_scope_var;

    -- 内側のブロックを開始
    DECLARE
        v_inner_scope_var text := ''I am in the inner block'';
    BEGIN
        RAISE NOTICE ''  (Inner) -> %'', v_outer_scope_var; -- 外側の変数を参照できる
        RAISE NOTICE ''  (Inner) -> %'', v_inner_scope_var;
    END;
    -- 内側のブロックが終了

    RAISE NOTICE ''%'', v_outer_scope_var;
    -- 下の行のコメントを外して実行するとエラーになる
    -- RAISE NOTICE ''%'', v_inner_scope_var; -- ERROR: "v_inner_scope_var" does not exist
END;
'LANGUAGE plpgsql;
```
#### 3. 関数とプロシージャ

PL/pgSQLのロジックは、再利用可能な「関数」または「プロシージャ」としてデータベースに保存するのが一般的です。

##### `CREATE FUNCTION` (関数の作成)

関数は、特定の処理を行い、**結果として値を返す**ものです。SQLのクエリ（`SELECT`文など）の中から呼び出すことができます。

**具体例：2つの数値を足し算して結果を返す関数**
```sql
CREATE FUNCTION add_numbers(a integer, b integer)
RETURNS integer AS $$
BEGIN
    RETURN a + b;
END;
$$ LANGUAGE plpgsql;
```
**関数の呼び出し方:**
```sql
SELECT add_numbers(10, 20);
```

##### `CREATE PROCEDURE` (プロシージャの作成)

プロシージャは、一連の処理を実行することを目的とし、**値を返しません**。SQLの`CALL`コマンドを使って呼び出します。プロシージャの大きな特徴は、**内部でトランザクションを制御できる（`COMMIT`や`ROLLBACK`を発行できる）**点です。

**具体例：テーブルに新しいレコードを挿入するプロシージャ**
```sql
CREATE TABLE employees (id serial PRIMARY KEY, name text);

CREATE PROCEDURE add_employee(employee_name text) AS $$
BEGIN
    INSERT INTO employees (name) VALUES (employee_name);
END;
$$ LANGUAGE plpgsql;
```
**プロシージャの呼び出し方:**
```sql
CALL add_employee('Taro Yamada');
```

##### 【重要】関数とプロシージャの主な違い

| 項目 | 関数 (FUNCTION) | プロシージャ (PROCEDURE) |
| :--- | :--- | :--- |
| **主な目的** | 計算や処理を行い、**値を返す** | 一連のデータ操作や処理を**実行する** |
| **戻り値** | **必須** (`RETURNS`句で指定) | **なし** (返せない) |
| **呼び出し方** | `SELECT`, `WHERE`句などSQLの一部として | `CALL` コマンドで単独で呼び出す |
| **トランザクション制御** | **できない** (`COMMIT`, `ROLLBACK`は不可) | **できる** |

まずはこの違いをしっかり押さえておきましょう。「何かを計算して結果が欲しいときは関数」、「一連の更新処理などをまとめて実行したいときはプロシージャ」と覚えるのが最初のステップとして有効です。