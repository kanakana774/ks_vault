# SQL 学習トピック一覧（学習順）⇒ 全２０日（１か月間）

## ステージ 1: 基礎構文 ⇒ 1 週目（4 日）40 問

### データベースとテーブル

- CREATE DATABASE
- CREATE TABLE（データ型, NOT NULL, PRIMARY KEY, UNIQUE, DEFAULT, CHECK, - FOREIGN KEY）
- DROP TABLE, ALTER TABLE（列追加・削除・型変更）⇒ ここでいいかも

### 基本操作（CRUD）

- INSERT
- SELECT（列指定, \*, WHERE, ORDER BY, LIMIT/OFFSET）
- UPDATE
- DELETE

### 基本的な演算子

- 比較演算子 =, <>, <, >, <=, >=
- 論理演算子 AND, OR, NOT
- 範囲・集合 BETWEEN, IN, NOT IN
- 部分一致 LIKE（%, \_）
- NULL の扱い IS NULL, IS NOT NULL

## ステージ 2: 集計とグループ化 ⇒1 ～ 2 週目（2 日）20 問

### 集計関数

- COUNT, SUM, AVG, MIN, MAX

### グループ化

- GROUP BY
- HAVING（集計後の条件）

### 日付・文字列関数

- 文字列操作: CONCAT, SUBSTRING, UPPER/LOWER, TRIM
- 日付操作: NOW(), DATEADD, DATEDIFF, EXTRACT, 曜日抽出

## ステージ 3: 複数テーブル操作 ⇒2~3 週目（7 日）35 問 くらい？

### JOIN

- INNER JOIN
- LEFT JOIN / RIGHT JOIN
- FULL OUTER JOIN
- 自己結合
  - on と where で条件を書くのはどう違うか

### 集合演算

- UNION, UNION ALL
- ~~INTERSECT~~
- ~~EXCEPT / MINUS（DB 依存）~~

### サブクエリ ⇒ ここ激重かも

- 非相関サブクエリ
- 相関サブクエリ
- EXISTS / NOT EXISTS

## ステージ 4: 高度な SELECT⇒3 ～ 4 週目（3 日）15 問くらい？

### CASE 式 ⇒ 条件分岐ここでいいかも

- 条件分岐で値を変換
- クロス集計（例: 勤怠区分ごとの人数を列化）

### CTE（共通テーブル式）⇒ これは新しい version じゃないと対応してないことあり

- WITH 句
- 再帰 CTE（階層構造やツリー展開）

### ウィンドウ関数 ⇒ ここまでやるか微妙

- ROW_NUMBER(), RANK(), DENSE_RANK()
- 集計ウィンドウ SUM() OVER(PARTITION BY ...)
- 移動平均 AVG() OVER(ORDER BY ROWS BETWEEN ...)

## ステージ 5: データ更新と管理 ⇒4 週目の総合問題（3 日）15 問くらい？

### 複雑な INSERT/UPDATE⇒ これを総合問題にすればいいか？

- INSERT ... SELECT
- UPDATE ... JOIN
- MERGE（UPSERT）

### トランザクション

- BEGIN, COMMIT, ROLLBACK
- 自動コミット
- 隔離レベル（READ COMMITTED, REPEATABLE READ, SERIALIZABLE）
- ロックの概念

### ビュー

- CREATE VIEW
- 更新可能ビューと制約

## ステージ 6: パフォーマンス・運用 ⇒ ４週目に概念だけでも紹介（1 日）５問くらい？

### インデックス ⇒ ここはなんとか組み込みたい

- 単一列インデックス
- 複合インデックス
- カバリングインデックス
- インデックスが効かないパターン（ワイルドカード前方一致, 関数適用）

### 実行計画 ⇒ ここはなんとか組み込みたい

- EXPLAIN, EXPLAIN ANALYZE
- テーブルスキャン / インデックススキャンの違い
- JOIN 方式（Nested Loop, Hash Join, Merge Join）

### 正規化と非正規化

- ~~第 1〜3 正規形~~
- ~~非正規化の実務例（集計テーブル, 履歴テーブル）~~

## ~~ステージ 7: 実務応用~~ ⇒ これは絶対やらない

### セキュリティ

- ユーザー作成と権限 GRANT, REVOKE
- 行レベルセキュリティ（RLS）

### バックアップとリストア ⇒ これは必要かもしれんけども、、、

- pg_dump, mysqldump
- リストア方法

### 実務ケーススタディ

- 勤怠集計（月次レポート, 部署別残業時間）
- 給与ランキング
- 部署階層の展開（CTE）

パフォーマンス改善（インデックス設計 → 実行計画比較）

✅ カリキュラム全体像

- 基礎（CRUD, 演算子, 制約）
- 集計・JOIN（業務に必要な基本集計）
- 応用（サブクエリ, CASE, CTE, ウィンドウ関数）
- 管理（トランザクション, ビュー）
- 実務（インデックス, 実行計画, 権限, 正規化/非正規化）
