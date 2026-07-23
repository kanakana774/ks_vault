
# PL/pgSQL 実践講座：第1章 PL/pgSQLの基礎と構造

## 0. 準備：デモ実行用環境

まずは、本章のコード例で使用する共通テーブルとサンプルデータを作成します。これらは、後の章でも継続して使用します。

```sql
-- テーブルのクリーンアップ（既存のものがあれば削除）
DROP TABLE IF EXISTS employees CASCADE;
DROP TABLE IF EXISTS departments CASCADE;

-- 部署テーブル
CREATE TABLE departments (
    dept_id serial PRIMARY KEY,
    dept_name text NOT NULL
);

-- 従業員テーブル
CREATE TABLE employees (
    emp_id serial PRIMARY KEY,
    name text NOT NULL,
    dept_id integer REFERENCES departments(dept_id),
    salary numeric(10, 2),
    created_at timestamptz DEFAULT now()
);

-- サンプルデータの挿入
INSERT INTO departments (dept_name) VALUES ('Sales'), ('Engineering'), ('HR');

INSERT INTO employees (name, dept_id, salary) VALUES
('Taro Tanaka', 1, 500000),
('Hanako Suzuki', 2, 700000),
('Ichiro Sato', 2, 650000),
('Jiro Yamada', 3, 450000);
```

---

## 1.1 PL/pgSQLとは何か：使うべき理由

PL/pgSQL (Procedural Language/PostgreSQL) は、SQLにプログラミング言語特有の**制御構造（分岐、ループ）**や**変数**、**エラーハンドリング**を追加した手続き型言語です。

PL/pgSQLを採用する最大のメリットは以下の3点に集約されます。

1.  **ネットワークトラフィックの削減**: 複雑なビジネスロジックをアプリケーション側で実装すると、DBとの間で何度もクエリの往復が発生します。PL/pgSQLでカプセル化することで、クライアント・サーバー間の通信を一回で済ませることができます。
2.  **実行計画の再利用（キャッシュ）**: PL/pgSQL内の各クエリは、最初に実行される際に解析され、実行計画がキャッシュされます。これにより、動的にSQLを構築して投げるよりも効率的に実行される場合があります。
3.  **トランザクションの一貫性**: 複数の更新処理を含むロジックをデータベース内で完結させることで、トランザクションの境界を明確にし、データの整合性をより強固に保つことができます。

---

## 1.2 PL/pgSQLの基本構造：ブロック

PL/pgSQLのコードは「ブロック」という単位で構成されます。これが最小の実行単位となります。

### 基本構文
```sql
[ DECLARE
    -- 変数、型、カーソルの宣言
]
BEGIN
    -- 実行コード（ロジック）
[ EXCEPTION
    -- 例外処理（エラーハンドリング）
]
END;
```


---

## 1.3 匿名ブロック (`DO` 文)

エンジニアがちょっとしたスクリプトを実行したり、ロジックをテストしたりする際に最も多用するのが `DO` 文です。関数やプロシージャとして永続化せず、その場で実行されます。

### 実演：基本的な匿名ブロックの実行
以下のコードをコピーして実行してください。

```sql
DO $$
DECLARE
    -- 変数の宣言
    v_target_name text := 'Hanako Suzuki';
    v_salary numeric;
BEGIN
    -- SQLの結果を変数に代入 (SELECT INTO)
    SELECT salary INTO v_salary 
    FROM employees 
    WHERE name = v_target_name;

    -- メッセージの出力 (RAISE NOTICE)
    -- % はプレースホルダ
    RAISE NOTICE 'Employee: %, Salary: %', v_target_name, v_salary;

EXCEPTION
    WHEN OTHERS THEN
        RAISE WARNING '予期せぬエラーが発生しました: %', SQLERRM;
END;
$$ LANGUAGE plpgsql;
```

### 重要なルール
*   **セミコロン (`;`)**: `END` の後、およびブロック内の各文（ステートメント）の末尾には必ずセミコロンが必要です。
*   **ドル引用符 (`$$`)**: PL/pgSQLのコード本体をシングルクォートで囲む代わりに、`$$` で囲むのが標準的です。これにより、コード内のシングルクォートをエスケープする必要がなくなります。
*   **大文字小文字**: SQLと同様、キーワードは大文字小文字を区別しないですが、可読性のためにキーワードは大文字で書くのが一般的です。

---

## 1.4 スコープとラベル：ネストしたブロック

PL/pgSQLのブロックは入れ子（ネスト）にできます。変数の有効範囲（スコープ）を限定したい場合や、例外処理を特定の処理群にだけ適用したい場合に有用です。

### 変数の隠蔽とラベル
内側のブロックで外側と同じ名前の変数を宣言すると、外側の変数は「隠蔽」されます。この時、**「ラベル」**を使うことで、明示的に外側のブロックの変数を参照することができます。

### 実演：ラベルを用いた変数参照の解決
以下のコードは、内側のブロックから外側の変数にアクセスする方法を示しています。

```sql
DO $$
<<main_block>> -- 外側ブロックのラベル
DECLARE
    v_counter integer := 10;
BEGIN
    RAISE NOTICE 'Outer Block - Start: counter = %', v_counter;

    -- 内側ブロックの開始
    DECLARE
        v_counter integer := 999; -- 外側と同じ名前で宣言
    BEGIN
        RAISE NOTICE 'Inner Block: counter (local) = %', v_counter;
        
        -- ラベルを使って外側の変数にアクセス
        RAISE NOTICE 'Inner Block: counter (from main_block) = %', main_block.v_counter;
        
        -- 外側の変数を書き換えることも可能
        main_block.v_counter := main_block.v_counter + 5;
    END;

    RAISE NOTICE 'Outer Block - End: counter = %', v_counter;
END;
$$ LANGUAGE plpgsql;
```

### ラベルの役割
1.  **名前の衝突回避**: 上記の通り、同名変数の区別に使用します。
2.  **制御構造の脱出**: 多重ループの中から、一気に外側のループを `EXIT` する際にラベルを指定します（詳細は第4章で解説）。
---

## 1.5 まとめ

*   PL/pgSQLは `DECLARE`, `BEGIN`, `EXCEPTION`, `END` のブロック構造を持つ。
*   匿名ブロック (`DO`) は開発中のロジック検証に最適。
*   セミコロンの打ち忘れや `$$` の使い方など、細かい箇所でエラーが発生しがちなので、気を付ける。