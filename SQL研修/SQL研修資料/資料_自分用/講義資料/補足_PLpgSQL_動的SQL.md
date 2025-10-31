## トランザクションの確認

```SQL
-- トランザクション状況の確認
SELECT txid_current_if_assigned(); -- NULLなら未開始
SELECT state, backend_xid FROM pg_stat_activity WHERE pid = pg_backend_pid();

-- これで調べればいいか。
DO $$
BEGIN
	RAISE NOTICE 'トランザクションID：%',txid_current_if_assigned(); -- NULLなら未開始
END;
$$ LANGUAGE plpgsql;
```

```SQL
DO $$
BEGIN
    -- 学習用一時テーブルを毎回作り直す
    EXECUTE 'DROP TABLE IF EXISTS tmp_data';
    EXECUTE 'CREATE TEMP TABLE tmp_data (id int, val text)';

    INSERT INTO tmp_data VALUES (1, 'aaa'), (2, 'bbb');
    RAISE NOTICE '件数: %', (SELECT COUNT(*) FROM tmp_data);
END;
$$;
```

## PL/pgSQL における SQL の実行方法

### 1. 静的 SQL（普通に書く）

```SQL
DO $$
DECLARE
    v_id int := 1;
    v_name text := 'taro';
BEGIN
    INSERT INTO users (id, name) VALUES (v_id, v_name);
    RAISE NOTICE '登録完了: %, %', v_id, v_name;
END;
$$;
```

#### 特徴

- SQL は 関数や DO ブロックのコンパイル時点で解析 される。
- 変数は パラメータとして安全に埋め込まれる。
- 事前に型チェックされるため、タイプミスや構文エラーは 関数作成時に検出 される。
- 速い（一度解析された SQL をキャッシュして再利用）。

#### 制限

テーブル名・列名・演算子など 識別子は変数で渡せない。

```SQL
-- NG: これは動かない
DECLARE
    tbl text := 'users';
BEGIN
    INSERT INTO tbl VALUES (1, 'aaa'); -- エラー
END;
```

---

### 2. 動的 SQL（EXECUTE）

```SQL
DO $$
DECLARE
    tbl text := 'users';
    v_id int := 2;
    v_name text := 'jiro';
BEGIN
    EXECUTE format(
        'INSERT INTO %I (id, name) VALUES ($1, $2)',
        tbl
    )
    USING v_id, v_name;
END;
$$;
```

#### 特徴

- SQL 文を 文字列として組み立てて実行。
- テーブル名や列名など、静的 SQL では変数にできない部分を可変にできる。
- USING 句を使えば値を安全にバインドでき、SQL インジェクションも防げる。

#### 制限・注意

- 実行時に文字列をパースするため 静的 SQL より遅い。
- SQL の構文や型のエラーは 実行時にならないと分からない。
- 動的に組み立てる部分は format('%I', ident) や format('%L', literal) を使わないと SQL インジェクションの危険。

---

### 3. 違いの比較表

| 項目                 | 静的 SQL                 | 動的 SQL (EXECUTE)                       |
| -------------------- | ------------------------ | ---------------------------------------- |
| SQL 確定のタイミング | コンパイル時             | 実行時                                   |
| 速度                 | 速い（キャッシュされる） | 遅い（毎回パース）                       |
| エラーチェック       | 事前にチェック           | 実行時まで不明                           |
| 変数展開             | 値のみ可能               | 値 + テーブル名・列名・演算子など何でも  |
| 安全性               | 高い（型チェックあり）   | 注意必要（SQL インジェクション対策必須） |
| 主な用途             | 通常の CRUD 処理         | 可変テーブル・可変列を扱う処理           |

---

#### 実務的な使い分け

- 静的 SQL を基本にする

  - パフォーマンスも良く、安全性も高い
  - 9 割以上の処理はこれで済む

- 動的 SQL が必要になる場面
  - 日付ごとに分割されたログテーブルに INSERT する
    - log_202509, log_202510, ...
  - 引数で渡された列に応じて ORDER BY を変える
    - データマート構築やマスタメンテで「テーブル名・列名自体が変数で決まる」処理

---

#### 例：静的 SQL ではできない → 動的 SQL で解決

「テーブル名を引数で渡して INSERT したい」

```SQL
CREATE OR REPLACE PROCEDURE insert_any(tbl text, v_id int, v_name text)
LANGUAGE plpgsql
AS $$
BEGIN
    EXECUTE format('INSERT INTO %I (id, name) VALUES ($1, $2)', tbl)
    USING v_id, v_name;
END;
$$;

CALL insert_any('users', 10, 'hanako');
CALL insert_any('customers', 20, 'yamada');
```

---

- まとめ
  - 静的 SQL（普通に書く） → 安全・速い・推奨
  - EXECUTE（動的 SQL） → テーブル名や列名を変えたい特殊ケース用

---

### USING でできること・できないこと

- USING で渡せるのは 値（リテラル）やパラメータ部分 だけ。
  - WHERE id = $1 の $1 に数値を渡す
  - VALUES ($1, $2) の $1, $2 に変数を渡す
- テーブル名・列名など識別子は渡せません。

理由は、PostgreSQL のパーサが SQL の構造（テーブル・列・演算子など）と値の部分 を別々に扱うから。
識別子は「構造」に属するので、文字列組み立てでしか扱えない。

---

### 動的 SQL の文字列組み立てパターン集

#### ① 値だけ動的（USING を使う）

```SQL
DO $$
DECLARE
    v_id int := 100;
    v_name text := 'taro';
BEGIN
    EXECUTE 'INSERT INTO users (id, name) VALUES ($1, $2)'
    USING v_id, v_name;
END;
$$;
```

- SQL の形は固定
- 値だけ変える
- 一番安全で速い

---

#### ② テーブル名を動的（format + %I）

```SQL
DO $$
DECLARE
    tbl text := 'users';
    v_id int := 200;
BEGIN
    EXECUTE format('INSERT INTO %I (id) VALUES ($1)', tbl)
    USING v_id;
END;
$$;
```

- %I → 識別子（Identifier） を安全に埋め込む
- テーブル名・列名などはこちらで扱う

---

#### ③ 列名を動的

```SQL
DO $$
DECLARE
    col text := 'name';
    v_val text := 'jiro';
BEGIN
    EXECUTE format('UPDATE users SET %I = $1 WHERE id = 1', col)
    USING v_val;
END;
$$;
```

- 列名を変数化する場合も %I

---

#### ④ リテラル文字列を安全に埋め込む

```SQL
DO $$
DECLARE
    val text := 'O''Reilly'; -- シングルクォートを含む値
BEGIN
    EXECUTE format('INSERT INTO users (name) VALUES (%L)', val);
END;
$$;
```

- %L → Literal（リテラル値）を安全に埋め込む
- SQL インジェクション防止

---

#### ⑤ ORDER BY 句を動的に

```SQL
DO $$
DECLARE
    sort_col text := 'name';
    sort_dir text := 'DESC';
    result record;
BEGIN
    FOR result IN
        EXECUTE format('SELECT * FROM users ORDER BY %I %s', sort_col, sort_dir)
    LOOP
        RAISE NOTICE 'id=%, name=%', result.id, result.name;
    END LOOP;
END;
$$;
```

- 列名 → %I
- キーワード（ASC/DESC など） → %s

---

#### ⑥ DROP TABLE IF EXISTS を動的に

```SQL
DO $$
DECLARE
    tbl text := 'tmp_data';
BEGIN
    EXECUTE format('DROP TABLE IF EXISTS %I', tbl);
END;
$$;
```

- DROP TABLE も同じく %I

---

- まとめ
  - 値（リテラル） → USING または format('%L', val)
  - テーブル名・列名（識別子） → format('%I', ident)
  - SQL キーワードや句（ASC/DESC など） → format('%s', text)

⇒ テーブル名を USING で渡すことはできない。必ず文字列組み立てが必要。

---

### format('%L', val) と USING の違い

#### 1. format('%L', val) を使う場合

```SQL
DO $$
DECLARE
    v_name text := 'O''Reilly'; -- シングルクォートを含む
BEGIN
    EXECUTE format('INSERT INTO users (name) VALUES (%L)', v_name);
END;
$$;
```

- 特徴

  - %L は リテラル（文字列や数値） を SQL 文に直接埋め込む。
  - PostgreSQL が自動的にエスケープするので安全（O'Reilly → 'O''Reilly'）。
  - SQL が組み立てられた時点で値が文字列化されている。

- 注意点
  - 値が文の一部に直書きされるため、SQL をキャッシュして再利用できない。
  - 毎回 SQL 全体をパースする必要がある（パフォーマンス面でやや不利）。

---

#### 2. USING を使う場合

```SQL
DO $$
DECLARE
    v_name text := 'O''Reilly';
BEGIN
    EXECUTE 'INSERT INTO users (name) VALUES ($1)'
    USING v_name;
END;
$$;
```

- 特徴

  - 値は バインド変数 として渡される。
  - SQL 文の構造は変わらず、値だけ別に処理される。
  - パーサの再利用が可能で パフォーマンスが良い。
  - SQL インジェクション対策としても安全。

- 注意点
  - 使えるのは 値の部分だけ。
  - テーブル名・列名などの識別子には使えない。

---

違いの比較表

| 項目           | format('%L', val)      | USING val              |
| -------------- | ---------------------- | ---------------------- |
| 埋め込み方法   | SQL 文字列に直接展開   | バインド変数           |
| 型チェック     | 実行時に解釈           | 実行時だが型情報を保持 |
| SQL キャッシュ | 効かない（毎回パース） | 効きやすい（構造一定） |
| 安全性         | 安全（エスケープ済み） | 安全（バインド処理）   |
| 使える範囲     | 値（リテラル）         | 値（リテラル）のみ     |
| パフォーマンス | やや不利               | 有利                   |

---

- 実務的な指針
  - 基本は USING を使う
    - パフォーマンス良いし、SQL の構造が保たれるから可読性も高い。
  - 例外的に format('%L', val) を使う場合
    - USING では表現できない場所にリテラルを埋め込みたいとき。
      - 例えば、動的に作った SQL の一部を強引に埋め込むような場合。

例：

```SQL
DO $$
DECLARE
    tbl text := 'users';
    col text := 'name';
    val text := 'jiro';
BEGIN
    -- col は識別子なので USING できない → format('%I')
    -- val はリテラルなので USING でもよいが、ここでは format('%L')
    EXECUTE format('UPDATE %I SET %I = %L WHERE id = 1', tbl, col, val);
END;
$$;
```

---

- まとめ：
  - 値を入れるなら → USING が推奨（速い・安全・シンプル）
  - どうしても文字列の中に埋め込むなら → format('%L', val)

---

### 1. PL/pgSQL の「特殊条件（condition variables）」

PL/pgSQL では、SQL 実行の成否や影響件数を 例外ではなく状態変数に格納 して知らせます。
これは Oracle PL/SQL に似た仕組みです。

代表的なのが次の 内部変数（システム変数） です：

| 変数名          | 意味                                                                    |
| --------------- | ----------------------------------------------------------------------- |
| FOUND           | 直前の SQL が 1 行以上にマッチしたら true、0 行なら false               |
| NOT FOUND       | FOUND の逆。構文糖衣であり「変数」ではない（IF NOT FOUND と書けるだけ） |
| ROW_COUNT       | 直前の SQL が影響した行数（例：UPDATE で何行更新したか）                |
| GET DIAGNOSTICS | SQLSTATE、行数、エラーコードなどをより詳細に取得できる                  |

---

### 2. FOUND と ROW_COUNT の動作

```SQL
DO $$
DECLARE
    v_name text;
BEGIN
    -- 存在しない行を取得
    SELECT name INTO v_name FROM users WHERE id = 999;

    IF NOT FOUND THEN
        RAISE NOTICE 'FOUND = %, ROW_COUNT = %', FOUND, ROW_COUNT;
    END IF;

    -- UPDATE 実行例
    UPDATE users SET name = 'test' WHERE id = 1;
    RAISE NOTICE 'UPDATE件数: %, FOUND = %', ROW_COUNT, FOUND;
END;
$$;
```

- FOUND → 直前の SQL に 1 行でもマッチしたかどうか
- ROW_COUNT → 直前の SQL で何行が影響を受けたか

---

### 3. より詳細な情報を得る (GET DIAGNOSTICS)

```SQL
DO $$
DECLARE
    v_count int;
    v_state text;
BEGIN
    UPDATE users SET name = 'abc' WHERE id = 999;

    GET DIAGNOSTICS v_count = ROW_COUNT; -- 更新件数
    GET DIAGNOSTICS v_state = PG_EXCEPTION_DETAIL; -- 例外発生時の詳細（通常はNULL）

    RAISE NOTICE '更新件数: %', v_count;
END;
$$;
```

- GET DIAGNOSTICS では ROW_COUNT, PG_EXCEPTION_CONTEXT, PG_EXCEPTION_DETAIL なども取得可能です。

---

### 4. 例外と状態変数の違い

- 状態変数（FOUND / ROW_COUNT など）

  - 正常系の分岐制御用
    - SQL が「0 行マッチ」や「n 行影響」を通知する
    - 例外ではないので EXCEPTION ブロックに飛ばない

- 例外（EXCEPTION）
  - 型不一致や制約違反など、本当にエラーが発生した場合
    - RAISE EXCEPTION で自作も可能
    - EXCEPTION ... WHEN ... で捕捉可能

---

### 5. 実務での使い分け

- 存在チェック → IF NOT FOUND THEN ... END IF;
- 更新件数チェック → ROW_COUNT を確認してログ出し
- 障害対応や再試行処理 → EXCEPTION でハンドリング

---

- まとめ
  - NOT FOUND は「特殊条件」＝ FOUND という内部変数の syntactic sugar
  - FOUND, ROW_COUNT は直前の SQL の状態を教えてくれる システム変数
  - 本当のエラーは EXCEPTION でしか検知できない

---

### 1. シンタックスシュガー（Syntax Sugar）とは

- 直訳: 文法の砂糖 → 曖昧に言えば「書きやすくするための文法」
- 意味: 本来の仕組みでは複雑に書かないといけないことを、より短く・わかりやすく書ける便利な構文
  ⇒ コンパイラや DB が内部で「本来の処理」に変換して実行する

---

例 1: PL/pgSQL の NOT FOUND

```SQL
SELECT name INTO v_name FROM users WHERE id = 999;

IF NOT FOUND THEN
    RAISE NOTICE 'データなし';
END IF;
```

NOT FOUND は実際には FOUND = false を意味しているだけ
書きやすくするための「糖衣構文（シンタックスシュガー）」

本当はこう書けます：

```SQL
IF FOUND = false THEN
    RAISE NOTICE 'データなし';
END IF;
```

---

例 2: PostgreSQL の SERIAL

```SQL
CREATE TABLE users (
    id SERIAL PRIMARY KEY,
    name TEXT
);
```

SERIAL は実際には自動でシーケンスを作って DEFAULT で紐づける仕組み

内部ではこんな処理が自動で行われている：

```SQL
CREATE SEQUENCE users_id_seq;
CREATE TABLE users (
    id integer NOT NULL DEFAULT nextval('users_id_seq'),
    name text
);
ALTER SEQUENCE users_id_seq OWNED BY users.id;
```

ユーザーから見ると「ただ SERIAL と書くだけで OK」 → シンタックスシュガー

---

例 3: += や ++ （プログラミング言語）

```SQL
x += 1; // シンタックスシュガー
x = x + 1; // 本来の処理
```

書きやすくするだけで、実際の処理は同じ

---
