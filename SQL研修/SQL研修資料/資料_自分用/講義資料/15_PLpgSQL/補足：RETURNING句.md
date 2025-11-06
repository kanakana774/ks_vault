## 1. はじめに

PL/pgSQLにおける`RETURNING`句は、`INSERT`、`UPDATE`、`DELETE`といったデータ操作言語（DML）のステートメント実行時に、変更された行の情報を返すための非常に強力な機能です。 この句を利用することで、データベースへの問い合わせ回数を減らし、アプリケーションのパフォーマンス向上とコードの簡潔化を実現できます。

本資料では、`RETURNING`句の基本的な使い方から、PL/pgSQLの関数やプロシージャ内での応用例まで、具体的なコードを交えて分かりやすく解説します。

## 2. RETURNING句とは？

`RETURNING`句は、`INSERT`、`UPDATE`、`DELETE`文の末尾に追加することで、操作の対象となった行の値を即座に取得できるPostgreSQLの拡張機能です。

例えば、新しいデータを`INSERT`した際に自動採番されたIDを取得したり、`UPDATE`した後の最新の値を確認したり、`DELETE`した行の情報をログとして保存したりといった場合に非常に役立ちます。

### メリット

*   **パフォーマンスの向上**: データ操作と結果の取得を1つのクエリで完結できるため、データベースとのラウンドトリップ（通信の往復）が削減され、アプリケーションのパフォーマンスが向上します。
*   **コードの簡潔化**: これまで2つのクエリ（データ操作とデータ取得）に分かれていた処理を1つにまとめられるため、コードがよりシンプルで読みやすくなります。
*   **データの一貫性**: 同一トランザクション内でデータ操作と結果取得を行うため、データの一貫性が保証されます。特に、更新された行を確実に特定するのが難しい場合に価値を発揮します。
*   **自動採番キーの取得**: `serial`型などで自動的に生成された主キーの値を、追加の`SELECT`文なしで簡単に取得できます。

## 3. 基本的な構文

`RETURNING`句は、`INSERT`、`UPDATE`、`DELETE`文の最後に記述します。`SELECT`文の出力リストと同様に、取得したい列名や式を指定できます。

### INSERT ... RETURNING

挿入された行の情報を返します。特に、デフォルト値が設定されている列や、`serial`型で自動採番されたIDを取得する際に便利です。

**構文:**
```sql
INSERT INTO table_name (column1, column2, ...)
VALUES (value1, value2, ...)
RETURNING expression | *;
```

**例：新しいユーザーを登録し、自動採番されたIDを取得する**
```sql
CREATE TABLE users (
    id SERIAL PRIMARY KEY,
    name TEXT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

INSERT INTO users (name) VALUES ('Taro Yamada')
RETURNING id, name, created_at;
```
このクエリは、新しいユーザーを`users`テーブルに挿入し、その結果として自動的に割り当てられた`id`、挿入された`name`、そしてデフォルト値として設定された`created_at`を返します。

### UPDATE ... RETURNING

更新後の行の新しい内容を返します。

**構文:**
```sql
UPDATE table_name
SET column1 = value1, column2 = value2, ...
WHERE condition
RETURNING expression | *;
```

**例：商品の価格を10%値上げし、更新後の商品名と新しい価格を取得する**
```sql
UPDATE products
SET price = price * 1.10
WHERE price <= 1000
RETURNING name, price AS new_price;
```この例では、価格が1000以下の商品の価格を更新し、その商品名と更新後の価格（`new_price`という別名で）を返します。

### DELETE ... RETURNING

削除された行の内容を返します。

**構文:**
```sql
DELETE FROM table_name
WHERE condition
RETURNING expression | *;
```

**例：特定の条件に一致する商品を削除し、削除された商品の情報をすべて取得する**
```sql
DELETE FROM products
WHERE obsoletion_date = 'today'
RETURNING *;
```
`RETURNING *` を使用すると、削除された行のすべての列が返されます。

## 4. PL/pgSQLでの応用

`RETURNING`句はPL/pgSQLの関数やプロシージャ内で`INTO`句と組み合わせることで、その真価を発揮します。取得した値を直接変数に格納し、その後の処理に利用することができます。

### RETURNING ... INTO

`RETURNING`句で取得した値を、指定した変数に格納します。

**注意点:**
`UPDATE`や`DELETE`が複数行に影響する場合、`RETURNING ... INTO`はエラーを引き起こします。これは、複数の行の結果を単一の変数に格納できないためです。 複数行を扱う場合は、`RETURN QUERY`やループ処理を使用する必要があります。

#### INSERT ... RETURNING INTO の例

```sql
CREATE OR REPLACE FUNCTION create_user_and_get_id(p_name TEXT)
RETURNS INTEGER AS $$
DECLARE
    new_user_id INTEGER;
BEGIN
    INSERT INTO users (name) VALUES (p_name)
    RETURNING id INTO new_user_id;

    -- new_user_id を使った別の処理...
    RAISE NOTICE 'New user created with ID: %', new_user_id;

    RETURN new_user_id;
END;
$$ LANGUAGE plpgsql;
```
この関数は、引数で受け取った名前で新しいユーザーを作成し、`RETURNING id INTO new_user_id`によって自動採番されたIDを変数`new_user_id`に格納しています。

#### UPDATE ... RETURNING INTO の例

```sql
CREATE OR REPLACE FUNCTION update_user_name(p_id INTEGER, p_new_name TEXT)
RETURNS TEXT AS $$
DECLARE
    updated_name TEXT;
BEGIN
    UPDATE users
    SET name = p_new_name
    WHERE id = p_id
    RETURNING name INTO updated_name;

    IF NOT FOUND THEN
        RAISE EXCEPTION 'User with ID % not found.', p_id;
    END IF;

    RETURN updated_name;
END;
$$ LANGUAGE plpgsql;
```
この関数は、指定されたIDのユーザー名を更新し、更新後の名前を変数`updated_name`に格納して返します。`UPDATE`が対象行を見つけられなかった場合（`FOUND`が`false`になる）、例外を発生させます。

#### DELETE ... RETURNING INTO の例

```sql
CREATE OR REPLACE FUNCTION delete_user(p_id INTEGER)
RETURNS TEXT AS $$
DECLARE
    deleted_user_name TEXT;
BEGIN
    DELETE FROM users
    WHERE id = p_id
    RETURNING name INTO deleted_user_name;

    IF deleted_user_name IS NULL THEN
        RETURN 'No user was deleted.';
    ELSE
        RETURN 'Deleted user: ' || deleted_user_name;
    END IF;
END;
$$ LANGUAGE plpgsql;
```
この関数は、指定されたIDのユーザーを削除し、削除されたユーザーの名前を返します。

### 複数行を返す場合 (RETURN QUERY)

`RETURNING`句が複数行を返す可能性がある場合、`SETOF <record>`や`TABLE(...)`を返す関数内で`RETURN QUERY`を使用します。

```sql
CREATE OR REPLACE FUNCTION delete_inactive_users(p_last_login_date DATE)
RETURNS TABLE(deleted_id INTEGER, deleted_name TEXT) AS $$
BEGIN
    RETURN QUERY
    DELETE FROM users
    WHERE last_login < p_last_login_date
    RETURNING id, name;
END;
$$ LANGUAGE plpgsql;
```
この関数は、指定された日付よりも前に最終ログインした全ての非アクティブユーザーを削除し、削除されたユーザーのIDと名前のセットを返します。

## 5. トリガーとの連携

テーブルにトリガーが設定されている場合、`RETURNING`句が返す値は、トリガーによって変更が加えられた**後**の行データとなります。 これにより、トリガーによって計算された列の値を簡単に取得でき、非常に便利です。
