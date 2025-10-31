PL/pgSQL 学習チェックリスト

1. 基礎 (Basics)
   PL/pgSQL の全体像と基本的な書き方を理解します。
   • [○] PL/pgSQL の概要と利点
   • [○] PL/pgSQL の基本構造 (DECLARE, BEGIN, END ブロック)
   • [○] 関数、プロシージャ、DO ブロック（匿名ブロック）での利用方法
   • [▲] コメントの書き方 (--, /_ ... _/)
2. 宣言とデータ型 (Declarations and Data Types)
   変数や定数を扱い、様々なデータ型を宣言する方法を学びます。
   • [○] 変数の宣言とデータ型（数値、文字列、日付など）
   • [○] テーブルの列や行の型を利用 (%TYPE, %ROWTYPE)
   • [×] 複合型 (RECORD) の宣言と利用
   • [×] 定数の宣言 (CONSTANT)
   • [▲] 関数の引数 (IN, OUT, INOUT) の扱い
   • [-] 変数のデフォルト値の設定
3. 基本的な文と式 (Basic Statements and Expressions)
   PL/pgSQL ブロック内で SQL を実行し、結果を変数に格納する方法を学びます。
   • [○] 代入 (:= または =)
   • [ ] 結果を伴わない SQL の実行 (PERFORM)
   • [ ] 単一行の結果を持つクエリの実行 (SELECT INTO)
   • [○] 動的コマンドの実行 (EXECUTE)
   • [○] 診断情報の取得 (GET DIAGNOSTICS)
   • [-] NULL の扱い (IS NULL, IS NOT NULL)
4. 制御構造 (Control Structures)
   条件分岐やループなど、プログラムの流れを制御する方法を学びます。
   • [○] 関数からのリターン (RETURN)
   • [○] 条件分岐 (IF-THEN-ELSIF-ELSE-END IF)
   • [○] 条件分岐 (CASE)
   • [○] ループ (LOOP, WHILE, FOR, EXIT, CONTINUE)
   ◦ [?] FOR ループによるクエリ結果のループ処理
   ◦ [?] FOR ループによる整数範囲のループ処理
   ◦ [○] FOREACH ループによる配列のループ処理
5. エラーハンドリング (Error Handling)
   エラーを捕捉し、適切に処理する方法や、意図的にエラーやメッセージを発生させる方法を学びます。
   • [○] エラーの捕捉 (EXCEPTION 句と WHEN)
   • [?] エラー情報の取得 (GET STACKED DIAGNOSTICS)
   • [○] エラーとメッセージの報告 (RAISE)
   • [ ] アサーション (ASSERT)
6. 高度なトピック (Advanced Topics)
   カーソルやトランザクション制御など、より高度な機能を学びます。
   • [ ] カーソルの利用
   ◦ [ ] カーソルの宣言
   ◦ [ ] カーソルのオープン (OPEN)
   ◦ [ ] カーソルからのデータ取得 (FETCH)
   ◦ [ ] カーソルのクローズ (CLOSE)
   • [○] トランザクション制御 (COMMIT, ROLLBACK in procedures)
   • [ ] トリガとイベントトリガの作成
   ◦ [ ] データ変更トリガ (BEFORE/AFTER INSERT/UPDATE/DELETE)
   ◦ [ ] INSTEAD OF トリガ（ビューに対する操作）
   ◦ [ ] DDL イベントトリガ
7. 開発のヒントとベストプラクティス
   PL/pgSQL をより効率的かつ安全に開発するためのヒントを学びます。
   • [ ] 引用符の適切な使い方（特に動的クエリ内）
   • [ ] パフォーマンス向上のためのヒント
   • [ ] Oracle PL/SQL からの移植
