### PL/pgSQL の基本的な構文と制御構造 (続き)

#### 2.6. `SELECT INTO`

`SELECT INTO` は、SQL クエリの結果を PL/pgSQL の変数に格納するために使用されます。

##### 単一行の結果を変数に格納 (`SELECT column INTO variable FROM table WHERE condition;`)

クエリが単一の行を返す場合に、その行の列の値を個別の変数または `%ROWTYPE` 変数に格納します。

**構文:**

```sql
SELECT column1, column2 INTO variable1, variable2 FROM table WHERE condition;
-- または
SELECT * INTO record_variable FROM table WHERE condition;
```

**注意点:**

- クエリが**行を返さない**場合、変数は `NULL` に設定され、例外は発生しません。
- クエリが**複数行を返す**場合、エラー (`TOO_MANY_ROWS`) が発生します。この場合、後述のカーソルや `FOR` ループを使用する必要があります。

**例:** `artist` テーブルから特定の `artist_id` の名前を取得します。

```sql
DO $$
DECLARE
    target_artist_id INTEGER := 5;
    artist_name_val artist.name%TYPE;
BEGIN
    SELECT name INTO artist_name_val
    FROM artist
    WHERE artist_id = target_artist_id;

    IF artist_name_val IS NOT NULL THEN
        RAISE NOTICE 'Artist ID % の名前: %', target_artist_id, artist_name_val;
    ELSE
        RAISE NOTICE 'Artist ID % は見つかりませんでした。', target_artist_id;
    END IF;
END
$$;
```

##### 複数行の結果を処理する方法 (カーソル、`FOR` ループ)

`SELECT INTO` は単一行の結果に限定されます。複数行の結果を処理するには、`FOR record_var IN query LOOP ... END LOOP;`（カーソルループ）または明示的なカーソルを使用します。ここでは、既に学んだカーソルループを使用します。

**例:** 複数行の結果を `FOR` ループで処理します（再掲ですが、重要なのでここでも触れます）。

```sql
DO $$
DECLARE
    customer_record customer%ROWTYPE;
BEGIN
    RAISE NOTICE '--- Listing first 3 Customers ---';
    FOR customer_record IN SELECT * FROM customer ORDER BY customer_id LIMIT 3 LOOP
        RAISE NOTICE 'Customer ID: %, Name: % %',
                     customer_record.customer_id,
                     customer_record.first_name,
                     customer_record.last_name;
    END LOOP;
END
$$;
```

#### 2.7. `INSERT`, `UPDATE`, `DELETE`

PL/pgSQL スクリプト内で DML (データ操作言語) ステートメントを実行できます。

##### PL/pgSQL スクリプト内での実行

これらのステートメントは、SQL と同じ構文で直接記述できます。

**例 (INSERT):** 新しいジャンルを `genre` テーブルに挿入します。

```sql
DO $$
DECLARE
    new_genre_name TEXT := 'New Age';
BEGIN
    INSERT INTO genre (name) VALUES (new_genre_name);
    RAISE NOTICE 'ジャンル "%" が追加されました。', new_genre_name;
END
$$;
```

**例 (UPDATE):** 特定のアーティストの名前を更新します。

```sql
DO $$
DECLARE
    target_artist_id INTEGER := 1; -- AC/DC (既存のデータによる)
    new_artist_name TEXT := 'AC/DC (Updated)';
BEGIN
    UPDATE artist SET name = new_artist_name WHERE artist_id = target_artist_id;
    RAISE NOTICE 'Artist ID % の名前を "%" に更新しました。', target_artist_id, new_artist_name;
END
$$;
```

**例 (DELETE):** 特定のジャンルを `genre` テーブルから削除します。(外部キー制約に注意)

```sql
DO $$
DECLARE
    genre_to_delete_id INTEGER;
    genre_to_delete_name TEXT := 'New Age'; -- 上のINSERTで追加したジャンル
BEGIN
    -- 削除するジャンルのIDを取得
    SELECT genre_id INTO genre_to_delete_id FROM genre WHERE name = genre_to_delete_name;

    IF genre_to_delete_id IS NOT NULL THEN
        DELETE FROM genre WHERE genre_id = genre_to_delete_id;
        RAISE NOTICE 'ジャンル "%" (ID: %) を削除しました。', genre_to_delete_name, genre_to_delete_id;
    ELSE
        RAISE NOTICE 'ジャンル "%" は見つかりませんでした。', genre_to_delete_name;
    END IF;
END
$$;
```

**注:** 上記の `DELETE` は、`New Age` ジャンルが他のテーブル（例: `track`）から参照されていない場合にのみ成功します。外部キー制約がある場合、関連レコードを先に削除するか、制約を一時的に無効にする必要がありますが、この範囲では触れません。

##### 影響を受けた行数の取得 (`GET DIAGNOSTICS variable = ROW_COUNT;`)

`INSERT`, `UPDATE`, `DELETE` ステートメントが実行された後、`GET DIAGNOSTICS` コマンドを使用して影響を受けた行数を `ROW_COUNT` 変数で取得できます。

**構文:**

```sql
GET DIAGNOSTICS variable_name = ROW_COUNT;
```

**例:** `customer` の `city` を更新し、影響を受けた行数を出力します。

```sql
DO $$
DECLARE
    affected_rows INTEGER;
    new_city TEXT := 'Tokyo';
    target_country TEXT := 'Japan';
BEGIN
    UPDATE customer SET city = new_city WHERE country = target_country;
    GET DIAGNOSTICS affected_rows = ROW_COUNT;

    RAISE NOTICE 'Country "%" の顧客の都市を "%" に更新しました。', target_country, new_city;
    RAISE NOTICE '影響を受けた行数: %', affected_rows;
END
$$;
```

#### 2.8. `EXECUTE` (動的 SQL)

`EXECUTE` コマンドを使用すると、実行時に SQL 文字列を構築し、それを実行できます。これは、テーブル名や列名などが実行時までわからないような、より柔軟なクエリを構築するのに役立ちます。

##### SQL 文字列の構築

実行したい SQL クエリを `TEXT` 型の変数として構築します。

**例:**

```sql
DO $$
DECLARE
    table_name TEXT := 'artist';
    sql_query TEXT;
BEGIN
    sql_query := 'SELECT COUNT(*) FROM ' || table_name;
    RAISE NOTICE '実行するSQL: %', sql_query;
    -- EXECUTE sql_query; -- 実際には実行しないが、これが実行方法
END
$$;
```

##### パラメータの渡し方 (`USING` 句)

動的 SQL には、SQL インジェクションを防ぐために、変数を直接文字列連結するのではなく、`USING` 句を使ってパラメータを渡すことが非常に重要です。

**構文:**

```sql
EXECUTE dynamic_sql_string [ INTO target ] [ USING expression [, ...] ];
```

`USING` 句を使用すると、プレースホルダ `$1`, `$2`, ... を動的 SQL 文字列内で使用し、`USING` の後に続く式でそれらのプレースホルダに値をバインドします。

**例 (SELECT INTO + EXECUTE + USING):** 特定のアーティストの名前を動的に取得します。

```sql
DO $$
DECLARE
    target_artist_id INTEGER := 8;
    dynamic_table_name TEXT := 'artist';
    dynamic_column_name TEXT := 'name';
    sql_query TEXT;
    artist_name_result TEXT;
BEGIN
    -- SQL文字列を構築。テーブル名と列名は動的に、WHERE句の値はプレースホルダで
    sql_query := 'SELECT ' || dynamic_column_name || ' FROM ' || dynamic_table_name || ' WHERE artist_id = $1';

    RAISE NOTICE 'Executing SQL: %', sql_query;

    -- EXECUTE で動的SQLを実行し、結果を INTO に格納、パラメータは USING で渡す
    EXECUTE sql_query INTO artist_name_result USING target_artist_id;

    RAISE NOTICE 'Dynamic Artist Name for ID %: %', target_artist_id, artist_name_result;
END
$$;
```

**例 (INSERT + EXECUTE + USING):** 新しいジャンルを動的に挿入します。

```sql
DO $$
DECLARE
    dynamic_table_name TEXT := 'genre';
    new_genre_name TEXT := 'Ambient (Dynamic)';
    sql_query TEXT;
    affected_rows INTEGER;
BEGIN
    sql_query := 'INSERT INTO ' || dynamic_table_name || ' (name) VALUES ($1)';

    RAISE NOTICE 'Executing SQL: %', sql_query;

    EXECUTE sql_query USING new_genre_name;
    GET DIAGNOSTICS affected_rows = ROW_COUNT;

    RAISE NOTICE '動的にジャンル "%" を追加しました。影響を受けた行数: %', new_genre_name, affected_rows;
END
$$;
```

##### セキュリティ上の注意点 (SQL インジェクション対策)

**重要:** `EXECUTE` を使用する際は、SQL インジェクションに対して特に注意が必要です。ユーザーからの入力など、信頼できないデータを直接 SQL 文字列に連結することは絶対に避けてください。

- **安全な方法:** ユーザーが提供する**値**をクエリに含める場合は、常に `USING` 句のプレースホルダ (`$1`, `$2`, ...) を使用してください。
  - 例: `EXECUTE 'SELECT * FROM my_table WHERE id = $1' USING user_id;`
- **危険な方法:** ユーザーが提供する**値**を文字列連結で直接含める。
  - 例: `EXECUTE 'SELECT * FROM my_table WHERE id = ' || user_id_from_input;` (ユーザーが `'1 OR 1=1'` のような文字列を入力すると危険)
- **注意が必要なケース:** テーブル名や列名のように、**SQL 構造の一部**を実行時に変更する必要がある場合は、`format()` 関数と `%I` (識別子) または `%L` (リテラル) プレースホルダを使用することが推奨されます。これらは文字列を適切に引用符で囲み、エスケープしてくれます。
  - 例: `sql_query := format('SELECT %I FROM %I WHERE %I = $1', column_name, table_name, id_column);`
  - ここでは `format()` 関数は範囲外なので、簡単な文字列連結でテーブル名や列名を変える例に留めていますが、本番環境では注意が必要です。

---

### 練習問題

以下の指示に従って PL/pgSQL の `DO` ブロックを作成してください。

#### 問題 1: `SELECT INTO` と `IF NOT FOUND` の使用

1.  `DO` ブロック内で、`target_track_id` という名前の `INTEGER` 型の変数を宣言し、初期値として `100` を代入してください。
2.  `track_name_val` 変数を `track.name%TYPE` で、`track_price_val` 変数を `track.unit_price%TYPE` で宣言してください。
3.  `target_track_id` に対応するトラックの `name` と `unit_price` を `SELECT INTO` で取得し、それぞれの変数に格納してください。
4.  もしトラックが見つかった場合（`track_name_val IS NOT NULL` などで確認）、`RAISE NOTICE` でトラックの名前と価格を出力してください。
5.  もしトラックが見つからなかった場合、`RAISE NOTICE` で `'トラックID % は見つかりませんでした。'` と出力してください。

#### 問題 2: `INSERT` と `GET DIAGNOSTICS ROW_COUNT` の使用

1.  `DO` ブロック内で、`new_artist_name` という名前の `TEXT` 型の変数を宣言し、初期値として `'New Artist Name'` を代入してください。
2.  `affected_rows` という名前の `INTEGER` 型の変数を宣言してください。
3.  `artist` テーブルに `new_artist_name` を挿入してください。
4.  `GET DIAGNOSTICS` を使って、影響を受けた行数を `affected_rows` に格納してください。
5.  `RAISE NOTICE` で、挿入されたアーティスト名と影響を受けた行数を出力してください。

#### 問題 3: `UPDATE` と `GET DIAGNOSTICS ROW_COUNT` の使用

1.  `DO` ブロック内で、`target_customer_id` という名前の `INTEGER` 型の変数を宣言し、初期値として `1` を代入してください。
2.  `new_email` という名前の `TEXT` 型の変数を宣言し、初期値として `'new.email@example.com'` を代入してください。
3.  `affected_rows` という名前の `INTEGER` 型の変数を宣言してください。
4.  `target_customer_id` に対応する顧客の `email` を `new_email` に更新してください。
5.  `GET DIAGNOSTICS` を使って、影響を受けた行数を `affected_rows` に格納してください。
6.  `RAISE NOTICE` で、更新された顧客の ID、新しいメールアドレス、および影響を受けた行数を出力してください。
7.  **ヒント**: この問題は、更新前に一度その顧客の現在のメールアドレスを出力してから更新すると、変更が分かりやすいです。

#### 問題 4: `EXECUTE` (動的 SQL) と `USING` の使用 (`SELECT INTO` 形式)

1.  `DO` ブロック内で、`dynamic_table` という名前の `TEXT` 型の変数を宣言し、初期値として `'employee'` を代入してください。
2.  `target_employee_id` という名前の `INTEGER` 型の変数を宣言し、初期値として `2` を代入してください。
3.  `employee_full_name` という名前の `TEXT` 型の変数を宣言してください。
4.  `dynamic_table` と `target_employee_id` を使用して、`employee` テーブルから `target_employee_id` の従業員の `first_name` と `last_name` を結合した名前を動的に取得する SQL クエリ文字列を構築してください。
    - ヒント: SQL 文字列内で `first_name || ' ' || last_name` を使って結合できます。
    - `WHERE` 句の `employee_id` の値には `$1` プレースホルダを使用してください。
5.  `EXECUTE` を使用して構築した動的 SQL を実行し、結果を `employee_full_name` に格納し、`USING` 句で `target_employee_id` を渡してください。
6.  `RAISE NOTICE` で、`target_employee_id` と取得した `employee_full_name` を出力してください。

---

### 解答

#### 問題 1: `SELECT INTO` と `IF NOT FOUND` の使用

```sql
DO $$
DECLARE
    target_track_id INTEGER := 100;
    track_name_val track.name%TYPE;
    track_price_val track.unit_price%TYPE;
BEGIN
    SELECT name, unit_price
    INTO track_name_val, track_price_val
    FROM track
    WHERE track_id = target_track_id;

    IF track_name_val IS NOT NULL THEN -- 変数がNULLでないことでレコードの有無を確認
        RAISE NOTICE 'トラックID %: 名前 = %, 価格 = %', target_track_id, track_name_val, track_price_val;
    ELSE
        RAISE NOTICE 'トラックID % は見つかりませんでした。', target_track_id;
    END IF;
END
$$;
```

#### 問題 2: `INSERT` と `GET DIAGNOSTICS ROW_COUNT` の使用

```sql
DO $$
DECLARE
    new_artist_name TEXT := 'New Artist Name PLPGSQL'; -- 衝突を避けるためユニークな名前に
    affected_rows INTEGER;
BEGIN
    INSERT INTO artist (name) VALUES (new_artist_name);
    GET DIAGNOSTICS affected_rows = ROW_COUNT;

    RAISE NOTICE 'アーティスト "%" を追加しました。影響を受けた行数: %', new_artist_name, affected_rows;
END
$$;
```

#### 問題 3: `UPDATE` と `GET DIAGNOSTICS ROW_COUNT` の使用

```sql
DO $$
DECLARE
    target_customer_id INTEGER := 1;
    new_email TEXT := 'new.email.updated@example.com';
    current_email TEXT;
    affected_rows INTEGER;
BEGIN
    -- 更新前のメールアドレスを取得 (オプション)
    SELECT email INTO current_email
    FROM customer
    WHERE customer_id = target_customer_id;

    RAISE NOTICE '顧客ID % の現在のメールアドレス: %', target_customer_id, current_email;

    UPDATE customer
    SET email = new_email
    WHERE customer_id = target_customer_id;

    GET DIAGNOSTICS affected_rows = ROW_COUNT;

    RAISE NOTICE '顧客ID % のメールアドレスを "%" に更新しました。', target_customer_id, new_email;
    RAISE NOTICE '影響を受けた行数: %', affected_rows;
END
$$;
```

#### 問題 4: `EXECUTE` (動的 SQL) と `USING` の使用 (`SELECT INTO` 形式)

```sql
DO $$
DECLARE
    dynamic_table TEXT := 'employee';
    target_employee_id INTEGER := 2;
    employee_full_name TEXT;
    sql_query TEXT;
BEGIN
    -- SQL クエリ文字列を構築
    -- テーブル名は動的に、WHERE 句の値はプレースホルダ ($1) を使用
    sql_query := 'SELECT first_name || '' '' || last_name FROM ' || dynamic_table || ' WHERE employee_id = $1';

    RAISE NOTICE '実行する動的SQL: %', sql_query;

    -- EXECUTE で動的 SQL を実行し、結果を employee_full_name に格納
    -- パラメータは USING 句で渡す
    EXECUTE sql_query INTO employee_full_name USING target_employee_id;

    RAISE NOTICE '従業員ID % の氏名: %', target_employee_id, employee_full_name;
END
$$;
```
