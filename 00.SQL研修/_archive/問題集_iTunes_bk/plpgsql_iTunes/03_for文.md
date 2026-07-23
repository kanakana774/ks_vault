### PL/pgSQL の基本的な構文と制御構造 (続き)

#### 2.5. ループ

PL/pgSQL では、一連のステートメントを繰り返し実行するためにループ構造を使用します。

##### `LOOP ... EXIT WHEN condition; ... END LOOP;`

これは最も基本的な無限ループの形式で、`EXIT WHEN` 句を使って明示的にループを抜ける必要があります。`EXIT WHEN` が指定されない場合、ループは永遠に実行されます。

**構文:**

```sql
LOOP
    statements;
    EXIT WHEN condition; -- 条件が真になったときにループを終了
END LOOP;
```

**例:** カウンターが `5` になるまでループし、現在のカウンター値を出力します。

```sql
DO $$
DECLARE
    counter INTEGER := 0;
BEGIN
    LOOP
        RAISE NOTICE 'Current counter: %', counter;
        counter := counter + 1;
        EXIT WHEN counter > 5; -- counter が 5 を超えたらループを抜ける
    END LOOP;
    RAISE NOTICE 'Loop finished.';
END
$$;
```

##### `WHILE condition LOOP ... END LOOP;`

`WHILE` ループは、指定された条件が真 (`TRUE`) である限り、ステートメントブロックを繰り返し実行します。ループに入る前に条件が評価され、条件が偽であればループは一度も実行されません。

**構文:**

```sql
WHILE condition LOOP
    statements;
END LOOP;
```

**例:** カウンターが `5` 以下の間ループし、現在のカウンター値を出力します。

```sql
DO $$
DECLARE
    counter INTEGER := 0;
BEGIN
    WHILE counter <= 5 LOOP
        RAISE NOTICE 'Current counter: %', counter;
        counter := counter + 1;
    END LOOP;
    RAISE NOTICE 'Loop finished.';
END
$$;
```

##### `FOR variable IN [REVERSE] start .. end LOOP ... END LOOP;` (数値範囲ループ)

この `FOR` ループは、指定された数値範囲を反復するために使用されます。`variable` はループカウンタとなり、`start` から `end` までの整数値が順番に代入されます。`REVERSE` キーワードを指定すると、`end` から `start` へと逆順に反復します。

**構文:**

```sql
FOR loop_variable IN [REVERSE] start_value .. end_value LOOP
    statements;
END LOOP;
```

**例:** `1` から `3` までの数値を順に出力します。

```sql
DO $$
BEGIN
    FOR i IN 1 .. 3 LOOP
        RAISE NOTICE 'Count: %', i;
    END LOOP;
    RAISE NOTICE 'Loop finished.';
END
$$;
```

**例 (REVERSE):** `3` から `1` までの数値を逆順に出力します。

```sql
DO $$
BEGIN
    FOR i IN REVERSE 3 .. 1 LOOP
        RAISE NOTICE 'Reverse Count: %', i;
    END LOOP;
    RAISE NOTICE 'Loop finished.';
END
$$;
```

##### `FOR record_var IN query LOOP ... END LOOP;` (カーソルループ)

この `FOR` ループは、SQL クエリの結果セットを反復処理するのに非常に便利です。クエリの各行が `record_var`（レコード型変数）に順次代入され、ループ内でその行の列にアクセスできます。

**構文:**

```sql
FOR record_variable IN query LOOP
    statements; -- record_variable.column_name で列にアクセス
END LOOP;
```

**例:** `artist` テーブルから最初の `3` 人のアーティストの名前と ID を取得し、出力します。

```sql
DO $$
DECLARE
    artist_record artist%ROWTYPE; -- artistテーブルの行全体を保持する変数
BEGIN
    RAISE NOTICE '--- Listing Top 3 Artists ---';
    FOR artist_record IN SELECT * FROM artist ORDER BY artist_id LIMIT 3 LOOP
        RAISE NOTICE 'Artist ID: %, Name: %', artist_record.artist_id, artist_record.name;
    END LOOP;
    RAISE NOTICE '--- End of List ---';
END
$$;
```

---

### 練習問題

以下の指示に従って PL/pgSQL の `DO` ブロックを作成してください。

#### 問題 1: `LOOP ... EXIT WHEN` の使用

1.  `DO` ブロック内で、`customer_count` という名前の `INTEGER` 型の変数を宣言し、初期値として `0` を代入してください。
2.  `customer_id_val` という名前の `INTEGER` 型の変数を宣言し、初期値として `1` を代入してください。
3.  `customer` テーブルから顧客の数をカウントし、それが `5` に達するまでループを続けます。
    - ループ内で `customer_id_val` を使って顧客の `first_name` と `last_name` を取得し、`RAISE NOTICE` で出力してください。
    - `customer_count` を `1` ずつ増やし、`customer_id_val` も `1` ずつ増やしてください。
    - `customer_count` が `5` になったらループを終了してください。

#### 問題 2: `WHILE` ループの使用

1.  `DO` ブロック内で、`employee_id_counter` という名前の `INTEGER` 型の変数を宣言し、初期値として `1` を代入してください。
2.  `employee_name_val` という名前の `TEXT` 型の変数を宣言してください。
3.  `employee_id_counter` が `5` 以下の間ループを続けます。
    - ループ内で `employee_id_counter` に対応する従業員の `first_name` と `last_name` を結合して `employee_name_val` に代入してください。
    - `RAISE NOTICE` で `employee_id_counter` と `employee_name_val` を出力してください。
    - `employee_id_counter` を `1` ずつ増やしてください。

#### 問題 3: `FOR ... IN start .. end` (数値範囲ループ) の使用

1.  `DO` ブロック内で、`5` から `1` までの数値を逆順に反復する `FOR` ループを作成してください。
2.  ループの各ステップで、現在の数値に `2` を乗算した結果を `RAISE NOTICE` で出力してください。

#### 問題 4: `FOR record_var IN query` (カーソルループ) の使用

1.  `DO` ブロック内で、`customer` テーブルから `country` が `'Canada'` であるすべての顧客を対象とする `FOR` ループを作成してください。
2.  ループ内で、各顧客の `first_name`、`last_name`、および `email` を `RAISE NOTICE` で出力してください。
3.  ループ開始前と終了後に、それぞれ適切なメッセージを `RAISE NOTICE` で出力してください。

---

### 解答

#### 問題 1: `LOOP ... EXIT WHEN` の使用

```sql
DO $$
DECLARE
    customer_count INTEGER := 0;
    customer_id_val INTEGER := 1;
    fname customer.first_name%TYPE;
    lname customer.last_name%TYPE;
BEGIN
    RAISE NOTICE '--- Listing first 5 Customers using LOOP ---';
    LOOP
        SELECT first_name, last_name
        INTO fname, lname
        FROM customer
        WHERE customer_id = customer_id_val;

        RAISE NOTICE 'Customer ID: %, Name: % %', customer_id_val, fname, lname;

        customer_count := customer_count + 1;
        customer_id_val := customer_id_val + 1;

        EXIT WHEN customer_count >= 5; -- 5人処理したらループを抜ける
    END LOOP;
    RAISE NOTICE '--- Loop finished ---';
END
$$;
```

#### 問題 2: `WHILE` ループの使用

```sql
DO $$
DECLARE
    employee_id_counter INTEGER := 1;
    employee_name_val TEXT;
BEGIN
    RAISE NOTICE '--- Listing first 5 Employees using WHILE ---';
    WHILE employee_id_counter <= 5 LOOP
        SELECT first_name || ' ' || last_name
        INTO employee_name_val
        FROM employee
        WHERE employee_id = employee_id_counter;

        RAISE NOTICE 'Employee ID: %, Name: %', employee_id_counter, employee_name_val;

        employee_id_counter := employee_id_counter + 1;
    END LOOP;
    RAISE NOTICE '--- Loop finished ---';
END
$$;
```

#### 問題 3: `FOR ... IN start .. end` (数値範囲ループ) の使用

```sql
DO $$
BEGIN
    RAISE NOTICE '--- Multiplying numbers from 5 down to 1 by 2 ---';
    FOR i IN REVERSE 5 .. 1 LOOP
        RAISE NOTICE 'Number: %, Multiplied by 2: %', i, (i * 2);
    END LOOP;
    RAISE NOTICE '--- Loop finished ---';
END
$$;
```

#### 問題 4: `FOR record_var IN query` (カーソルループ) の使用

```sql
DO $$
DECLARE
    canadian_customer_record customer%ROWTYPE;
BEGIN
    RAISE NOTICE '--- Listing Customers from Canada ---';
    FOR canadian_customer_record IN SELECT * FROM customer WHERE country = 'Canada' ORDER BY customer_id LOOP
        RAISE NOTICE 'ID: %, Name: % %, Email: %',
                     canadian_customer_record.customer_id,
                     canadian_customer_record.first_name,
                     canadian_customer_record.last_name,
                     canadian_customer_record.email;
    END LOOP;
    RAISE NOTICE '--- End of Canadian Customers List ---';
END
$$;
```
