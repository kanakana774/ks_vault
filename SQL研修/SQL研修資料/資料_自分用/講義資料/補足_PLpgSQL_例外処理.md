# エラーハンドリング

PL/pgSQL では、BEGIN ブロック内に EXCEPTION 句を設けることで、実行時エラーを捕捉し、対処することができます。
これにより、エラーが発生してもプロシージャや関数が即座に終了するのを防ぎ、エラーからの復旧処理やエラー情報の記録などが可能になります。

## エラーハンドリングの基本構文

PL/pgSQL の基本的なエラーハンドリング構文は以下の通りです。

```SQL
BEGIN
    -- 保護したい文（エラーが発生する可能性のある処理）
EXCEPTION
    WHEN condition [ OR condition ... ] THEN
        -- エラーを捕捉した場合の処理
    [ WHEN condition [ OR condition ... ] THEN
        -- 他のエラーを捕捉した場合の処理 ]
    ...
END;
```

- BEGIN...END ブロック内に、エラーが発生する可能性のある SQL 文や処理を記述します。
- エラーが発生すると、EXCEPTION 句に処理が移り、WHEN 句で指定された condition に一致するエラーが捕捉され、対応する処理が実行されます。
- EXCEPTION 句はサブトランザクションとして動作します。エラーが捕捉されると、そのサブトランザクションはロールバックされますが、外側のトランザクションは継続できます。捕捉されないエラーが発生した場合、トランザクション全体がアボート(実行中のプロセスや計画などを途中で強制的に中止・中断・終了させること)状態になります。

## エラーの捕捉

WHEN 句の condition には、特定のエラーを指定するために以下のいずれかを使用できます。

- **条件名:** division_by_zero や unique_violation など、PostgreSQL が定義した特定の条件名。
- **SQLSTATE コード**: 22012（ゼロ除算）や 23505（一意性制約違反）など、5 文字の SQLSTATE コード。
- **OTHERS**: 上記のいずれの条件にも一致しない、すべてのエラーを捕捉します。
  PostgreSQL のエラーコードの完全なリストは、下記。
  https://www.postgresql.jp/docs/9.4/errcodes-appendix.html

## エラー情報の取得

EXCEPTION ブロック内では、**GET STACKED DIAGNOSTICS**コマンドを使用して、発生したエラーに関する詳細な情報を取得できます。
これにより、エラーメッセージ、詳細、ヒントなどを変数に格納して利用できます。

```SQL
GET STACKED DIAGNOSTICS variable_name = item_name [, ...];
```

取得できる主な item_name は以下の通りです。

- **MESSAGE_TEXT**: エラーメッセージのテキスト。
- **PG_EXCEPTION_DETAIL**: エラーの詳細。
- **PG_EXCEPTION_HINT**: エラー解決のためのヒント。
- **PG_EXCEPTION_CONTEXT**: エラーが発生した関数のコールスタック。

## エラーの報告とメッセージの生成

RAISE 文を使用すると、独自のメッセージを報告したり、意図的にエラーを発生させたりすることができます。

```SQL
RAISE [ level ] 'format' [, expression ...];
```

- level には、**DEBUG**, **LOG**, **INFO**, **NOTICE**, **WARNING**, **EXCEPTION**などを指定できます。
  - EXCEPTION（デフォルト）はトランザクションを中断させるエラーを発生させます。
  - NOTICE や WARNING はエラーを発生させずにメッセージをクライアントに送信します。
- USING 句を追加することで、エラーレポートに詳細情報を含めることができます。
  - **MESSAGE**: エラーメッセージのテキスト。
  - **DETAIL**: エラーの詳細情報。
  - **HINT**: エラー解決のヒント。
  - **ERRCODE**: SQLSTATE コード。

## 具体的なコード例

以下に、PL/pgSQL のエラーハンドリング機能を示す実行可能なコード例を挙げます。

### 例 1: ゼロ除算エラーの捕捉

この例では、ゼロ除算エラーを EXCEPTION 句で捕捉し、カスタムメッセージを NOTICE として出力します。

```SQL
DO $$
DECLARE
    x int := 10;
    y int := 0;
    z int;
BEGIN
    z := x / y;
    RAISE NOTICE 'The result is %', z;
EXCEPTION
    WHEN division_by_zero THEN
        RAISE NOTICE 'Caught a division-by-zero error. Cannot divide by zero.';
END;
$$;
```

実行結果:
NOTICE: Caught a division-by-zero error. Cannot divide by zero.
DO

このコードはエラーで中断することなく、NOTICE メッセージを出力して正常に終了します。

### 例 2: 一意性制約違反の捕捉とエラー情報の取得

この例では、一意性制約違反（unique_violation）を捕捉し、GET STACKED DIAGNOSTICS を使用してエラーの詳細情報を取得し、表示します。
まず、テスト用のテーブルを作成します。
次に、重複したメールアドレスを挿入しようとし、エラーを捕捉する関数を作成します。

```SQL
DROP TABLE IF EXISTS employees;
CREATE TABLE employees (
    id SERIAL PRIMARY KEY,
    email TEXT UNIQUE NOT NULL
);

CREATE OR REPLACE FUNCTION add_employee(p_email TEXT) RETURNS void AS $$
DECLARE
    err_msg TEXT;
    err_detail TEXT;
    err_hint TEXT;
BEGIN
    INSERT INTO employees (email) VALUES (p_email);
EXCEPTION
    WHEN unique_violation THEN
        GET STACKED DIAGNOSTICS
            err_msg = MESSAGE_TEXT,
            err_detail = PG_EXCEPTION_DETAIL,
            err_hint = PG_EXCEPTION_HINT;
        RAISE NOTICE '---- Error Details ----';
        RAISE NOTICE 'Message: %', err_msg;
        RAISE NOTICE 'Detail: %', err_detail;
        RAISE NOTICE 'Hint: %', err_hint;
        RAISE NOTICE 'The email address [%] already exists.', p_email;
END;
$$ LANGUAGE plpgsql;

-- 最初の挿入は成功する
SELECT add_employee('test@example.com');

-- 2回目の挿入でエラーが捕捉される（Messagesタブを見てみる）
SELECT add_employee('test@example.com');
-- 実行結果（2回目）:
-- NOTICE:  ---- Error Details ----
-- NOTICE:  duplicate key value violates unique constraint "employees_email_key"
-- NOTICE:  Key (email)=(test@example.com) already exists.
-- NOTICE:  <NULL>
-- NOTICE:  The email address [test@example.com] already exists.
--  add_employee
-- --------------
-- (1 row)
```

この例では、エラーが発生しても関数は中断せず、詳細なエラー情報を NOTICE として出力します。

### 例 3: 文によるカスタムエラーの生成

この例では、特定の条件が満たされない場合に RAISE EXCEPTION を使用して独自のカスタムエラーを発生させます。

```SQL
CREATE OR REPLACE FUNCTION check_positive(num INT) RETURNS void AS $$
BEGIN
    IF num <= 0 THEN
        RAISE EXCEPTION 'The number must be positive. Received: %', num
            USING
                HINT = 'Please provide a number greater than zero.',
                ERRCODE = '22012'; -- division_by_zero (例として)
    END IF;
    RAISE NOTICE 'The number % is positive.', num;
END;
$$ LANGUAGE plpgsql;
```

実行:

```SQL
-- 成功するケース
SELECT check_positive(10);
NOTICE:  The number 10 is positive.

-- エラーが発生するケース
SELECT check_positive(-5);
-- ERROR:  The number must be positive. Received: -5
-- HINT:  Please provide a number greater than zero.
-- SQL state: 22012
```

このように、RAISE 文と USING 句を使うことで、アプリケーションに対してより具体的で分かりやすいエラー情報を提供できます [27, 41.9, 2546, 41.9]。
