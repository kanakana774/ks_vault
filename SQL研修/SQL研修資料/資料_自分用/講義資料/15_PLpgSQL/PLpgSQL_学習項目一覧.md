
### PL/pgSQL 学習ロードマップ【改訂版】

このロードマップは、基礎から応用、そして実践的な開発手法までを段階的に学べるように構成されています。

#### I. PL/pgSQL入門 (Introduction to PL/pgSQL)

まずはPL/pgSQLが何であるか、その基本構造と役割を理解します。

1.  **概要と目的**
    *   PL/pgSQLとは何か：手続き型言語としての位置づけと利点。
    *   なぜ必要か：SQLだけでは難しい複雑な条件分岐や繰り返し処理、エラーハンドリングを実現する。
2.  **基本構造**
    *   ブロック構造：`DECLARE`, `BEGIN`, `EXCEPTION`, `END`の役割。
    *   匿名ブロック (`DO`)：手軽にコードを実行する方法。
3.  **関数とプロシージャ**
    *   `CREATE FUNCTION` と `CREATE PROCEDURE` の基本的な構文。
    *   **【追加】** 関数とプロシージャの主な違い（戻り値の有無、トランザクション制御の可否）。

#### II. 基本構文とデータ型 (Basic Syntax and Data Types)

変数やデータ型を扱い、基本的な処理を記述する方法を学びます。

1.  **宣言とスコープ**
    *   変数の宣言とデータ型指定。
    *   **【追加】** スコープ：ブロック構造内での変数の有効範囲。
    *   便利な属性：`%TYPE`（列の型を継承）、`%ROWTYPE`（行全体の型を継承）。
2.  **【拡充】主要なデータ型と操作**
    *   基本データ型（`INTEGER`, `TEXT`, `TIMESTAMP`など）。
    *   複合型 (`CREATE TYPE`) とレコード型 (`RECORD`)。
    *   **【追加】** 配列 (`ARRAY`)：宣言、要素へのアクセス、ループ処理 (`FOREACH`)。
    *   **【追加】** JSON/JSONB：JSONデータの構築、アクセス、操作関数 (`->`, `->>`, `jsonb_build_object`など)。
3.  **代入と式**
    *   代入演算子 (`:=`, `=`)。
    *   PL/pgSQL内でのSQL式の評価。

#### III. SQLの実行と制御フロー (Executing SQL and Control Flow)

PL/pgSQLの核となる、SQLの実行とプログラムの流れを制御する方法を学びます。

1.  **静的SQLの実行**
    *   `SELECT INTO`：クエリ結果を1行だけ変数に格納する。
    *   `PERFORM`：クエリを実行するが、結果は破棄する。
    *   特殊変数 `FOUND`：直前の`SELECT INTO`や`UPDATE`等が対象行を見つけたかを確認する。
2.  **動的SQLの実行**
    *   `EXECUTE`：文字列から動的にSQL文を組み立てて実行する。
    *   **【追加】** `format()`関数：`%I` (識別子), `%L` (リテラル) を使った安全な動的SQLの構築。
    *   SQLインジェクション対策：`quote_literal`, `quote_ident` の重要性。
3.  **制御構造**
    *   条件分岐：`IF-THEN-ELSIF-ELSE-END IF`、`CASE`。
    *   ループ：`LOOP`, `WHILE`, `FOR`（整数、クエリ結果）、`EXIT`, `CONTINUE`。

#### IV. 関数とプロシージャの詳解 (Deep Dive into Functions and Procedures)

より柔軟で強力な関数・プロシージャを作成するための詳細な機能を学びます。

1.  **【拡充】引数と戻り値**
    *   引数モード：`IN`（入力）、`OUT`（出力）、`INOUT`（入出力）。複数の値を返すテクニック。
    *   **【追加】** デフォルト引数：引数の省略を可能にする。
    *   **【追加】** 多重定義 (Overloading)：同じ名前で引数の型や数が違う関数を定義する。
    *   **【追加】** 可変長引数 (`VARIADIC`)。
    *   多様な戻り値：
        *   `RETURNS SETOF <type>`：行の集合を返す。
        *   `RETURNS TABLE(...)`：表形式で結果を返す。
        *   `RETURNS void`：値を返さない。
2.  **【追加】セキュリティと実行権限**
    *   `SECURITY INVOKER`（デフォルト）：呼び出し元の権限で実行。
    *   `SECURITY DEFINER`：定義者の権限で実行。権限管理上の重要な概念。
3.  **トランザクション制御**
    *   プロシージャ内でのトランザクション制御 (`COMMIT`, `ROLLBACK`)。
    *   関数内でのサブトランザクション (`BEGIN...EXCEPTION...END`ブロック)。

#### V. エラーハンドリングとデバッグ (Error Handling and Debugging)

堅牢なコードを書くためのエラー処理と、開発効率を上げるデバッグ手法を学びます。

1.  **エラーとメッセージの通知**
    *   `RAISE`：メッセージ、警告、エラーを意図的に発生させる (`NOTICE`, `WARNING`, `EXCEPTION`)。
2.  **例外処理**
    *   `EXCEPTION` ブロック：エラーを捕捉して特定の処理を行う。
    *   **【追加】** `SQLSTATE` と `SQLERRM`：発生したエラーの詳細情報を取得する。
    *   特定の例外の捕捉（例: `WHEN no_data_found THEN ...`）。
3.  **【追加】デバッグ手法**
    *   `RAISE NOTICE` を利用した変数の値の確認。
    *   `ASSERT` 文による条件チェック（開発時に有効）。

#### VI. 応用編：トリガーとカーソル (Advanced Usage: Triggers and Cursors)

特定のイベントに応じた自動処理や、大量のデータを一行ずつ処理する方法を学びます。

1.  **トリガープロシージャ**
    *   `DMLトリガー` (`INSERT`, `UPDATE`, `DELETE`時に発動)。
    *   行トリガー (`FOR EACH ROW`) と文トリガー (`FOR EACH STATEMENT`)。
    *   特殊変数：`NEW`, `OLD`, `TG_OP`などの活用。
    *   **【追加】** `イベントトリガー` (`DDL`イベント時に発動)。
2.  **カーソル**
    *   大量の結果セットをメモリにすべてロードせず、一行ずつ処理する。
    *   宣言、`OPEN`, `FETCH`, `CLOSE` の一連の流れ。
    *   カーソルを使ったループ (`FOR record IN cursor LOOP`)。

#### VII. 最適化とベストプラクティス (Optimization and Best Practices)

パフォーマンスを意識した質の高いコードを書くための知識を深めます。

1.  **【拡充】パフォーマンスチューニング**
    *   内部動作：PL/pgSQLの実行計画のキャッシュについて。
    *   **【追加】** 関数の揮発性：`IMMUTABLE`, `STABLE`, `VOLATILE` の違いとオプティマイザへの影響。
    *   **【追加】** `STRICT` オプション：`NULL`引数が渡された場合に即座に`NULL`を返す設定。
    *   **【追加】** 並列処理の安全性：`PARALLEL SAFE`, `PARALLEL RESTRICTED`, `PARALLEL UNSAFE`。
2.  **開発のヒントとコーディング規約**
    *   安全なコーディングプラクティス。
    *   **【追加】** 可読性を高めるコーディング規約（命名規則、インデント、コメントなど）。
3.  **システムカタログの活用**
    *   `information_schema` や `pg_catalog` を参照して、既存の関数やプロシージャの情報を取得する。
4.  **互換性と移行**
    *   Oracle PL/SQLからの移植に関する考慮事項。