PostgreSQLの実行計画、特に`EXPLAIN (ANALYZE, BUFFERS)`を使用することで得られる情報から、メモリ（バッファ）の状態とボトルネックを読み解く方法を整理します。

事前準備：
```sql
-- テーブル作成
CREATE TABLE buffer_test (
    id SERIAL PRIMARY KEY,
    val TEXT,
    created_at TIMESTAMP
);

-- 10万件のランダムデータを挿入
INSERT INTO buffer_test (val, created_at)
SELECT 
    md5(random()::text), -- ランダムな文字列
    now() - (random() * interval '365 days')
FROM generate_series(1, 100000);

-- 統計情報を最新にする
ANALYZE buffer_test;
```

---
### 1. バッファ統計（BUFFERS）の読み取りと物理挙動のリンク

`BUFFERS`オプションを有効にすると、クエリ実行中のI/Oアクティビティが具体的な「ブロック（ページ）数」として表示されます。これは、データがメモリ（Shared Buffers）とディスクの間でどのように動いたかを示す直接的な指標です。

- **shared hit（共有ヒット）**: 要求されたデータブロックが既にメモリ（共有バッファ）内に存在し、**ディスク読み取りを回避できた**回数です。
    - **実際の挙動**: メモリ速度でデータにアクセスできているため、この数値が高いほど実行時間は短縮されます。
- **shared read（共有読み取り）**: データがメモリになく、**ディスク（ストレージ）から読み取った**回数です。
    - **実際の挙動**: 重いディスクI/Oが発生しています。特に「Index Scan」でこの数値が高い場合、ランダムアクセスによる遅延が顕著になります。
- **shared dirtied / written**:
    - **dirtied**: クエリによって内容が変更（更新）されたメモリ上のブロック数です。
    - **written**: クエリ実行中に、メモリ不足などで古いデータ（ダーティページ）をディスクに書き出した回数です。

---

### 2. メモリ不足によるボトルネック：work_memとディスク溢れ

ソートやハッシュ操作において、計算用のメモリ領域である `work_mem` が不足すると、データが一時ファイルとしてディスクに書き出されます（Disk Spill）。

#### 2-1 **Sortノードでの挙動**:
- **Memory（quicksort）**: 
	- すべてのデータが `work_mem` 内に収まり、高速に処理されています。
- **external merge Disk**: 
	- データがメモリに収まりきらず、ディスクを使用した外部ソートが発生しています。

external merge Disk：
```SQL
show work_mem

SET work_mem = '64kB'; -- メモリを極端に制限（最小値付近）

EXPLAIN (ANALYZE, BUFFERS)
SELECT * FROM buffer_test ORDER BY val LIMIT 50000;

RESET work_mem; -- 終わったら戻す
```

```
"QUERY PLAN"
"Limit  (cost=19469.32..19594.32 rows=50000 width=45) (actual time=137.465..153.082 rows=50000 loops=1)"
"  Buffers: shared hit=935, temp read=1786 written=2317"
"  ->  Sort  (cost=19469.32..19719.32 rows=100000 width=45) (actual time=137.450..150.202 rows=50000 loops=1)"
"        Sort Key: val"
"        Sort Method: external merge  Disk: 5752kB"
"        Buffers: shared hit=935, temp read=1786 written=2317"
"        ->  Seq Scan on buffer_test  (cost=0.00..1935.00 rows=100000 width=45) (actual time=1.365..37.867 rows=100000 loops=1)"
"              Buffers: shared hit=935"
"Planning:"
"  Buffers: shared hit=27"
"Planning Time: 3.373 ms"
"Execution Time: 157.369 ms"
```

##### ディスク溢れの証拠
> `Sort Method: external merge  Disk: 5752kB`

*   **解説**: 本来はメモリ上で行いたかったソートですが、`work_mem` が 64kB しかないため、**約5.7MB分、データがメモリから溢れてディスクに書き出された**ことを示しています。
*   `external merge`（外部マージソート）という言葉が出たら、「メモリ不足でディスクを使った」という確定診断です。

##### バッファ統計（temp）
> `Buffers: shared hit=935, temp read=1786 written=2317`

*   **`shared hit=935`**: テーブルのデータ自体（約1000ページ分）は、メモリ（Shared Buffers）に載っています。読み込み自体は高速でした。
*   **`temp written=2317`**: メモリに収まりきらないソートの途中のデータを、一時ファイルとしてディスクに **2317ブロック書き出した** ことを示します。
*   **`temp read=1786`**: 書き出したデータを、最終的な並べ替えのために **1786ブロック読み戻した** ことを示します。


#### 2-2 **Hashノードでの挙動**:
- **Batches**:
	- **Batches: 1**: ハッシュテーブルがメモリ内に完全に構築されています。
	- **Batches: 5以上**: メモリ不足により、ハッシュテーブルを分割してディスクに待避させながら処理しています。
- **BUFFERSへの影響**: `temp read` や `temp write` という項目に数値が現れます。これは一時的な作業領域としてディスクを使用したことを意味し、パフォーマンス低下の強力なサインです。

```SQL
show work_mem

SET work_mem = '64kB'; -- メモリを極端に制限（最小値付近）

EXPLAIN (ANALYZE, BUFFERS)
SELECT COUNT(*) 
FROM buffer_test t1
JOIN buffer_test t2 ON t1.val = t2.val;

RESET work_mem; -- 終わったら戻す
```

```
"QUERY PLAN"
"Aggregate  (cost=9873.00..9873.01 rows=1 width=8) (actual time=151.497..151.501 rows=1 loops=1)"
"  Buffers: shared hit=1870, temp read=648 written=648"
"  ->  Hash Join  (cost=3967.00..9623.00 rows=100000 width=0) (actual time=66.476..146.330 rows=100000 loops=1)"
"        Hash Cond: (t1.val = t2.val)"
"        Buffers: shared hit=1870, temp read=648 written=648"
"        ->  Seq Scan on buffer_test t1  (cost=0.00..1935.00 rows=100000 width=33) (actual time=0.257..8.594 rows=100000 loops=1)"
"              Buffers: shared hit=935"
"        ->  Hash  (cost=1935.00..1935.00 rows=100000 width=33) (actual time=65.173..65.174 rows=100000 loops=1)"
"              Buckets: 131072  Batches: 2  Memory Usage: 4199kB"
"              Buffers: shared hit=935, temp written=323"
"              ->  Seq Scan on buffer_test t2  (cost=0.00..1935.00 rows=100000 width=33) (actual time=0.028..25.706 rows=100000 loops=1)"
"                    Buffers: shared hit=935"
"Planning:"
"  Buffers: shared hit=154"
"Planning Time: 2.596 ms"
"Execution Time: 152.267 ms"
```

##### 「Batches: 2」の意味
> `Buckets: 131072  Batches: 2  Memory Usage: 4199kB`

ここが最大の注目点です。
*   **本来なら**: `Batches: 1` であれば、すべてのハッシュテーブルがメモリ内に収まり、一瞬で結合が終わります。
*   **今回の挙動**: `work_mem` を制限したため、PostgreSQLは「あ、これメモリに入り切らないな」と判断し、データを2つのグループ（Batch）に分割しました。
*   **挙動の裏側**: 1つ目のグループを処理している間、もう1つのグループは**一旦ディスクに待避**させています。

##### 「temp written」と「temp read」の連動
> `Buffers: shared hit=1870, temp read=648 written=648`

*   **`temp written=648`**: ハッシュテーブルを作る際、メモリに入り切らなかった分を一時ファイルとしてディスクに書き出したブロック数です。
*   **`temp read=648`**: 後で結合（Join）するために、ディスクに待避させていたデータを読み戻したブロック数です。
*   **パフォーマンスへの影響**: `shared hit` はメモリ内なので超高速ですが、この `temp` がついた項目はストレージ（SSD/HDD）へのアクセスなので、桁違いに時間がかかります。

##### 【豆知識】work_mem を超えている？
> `Memory Usage: 4199kB` （設定したのは 64kB なのに）

「`work_mem` を 64kB にしたのに、なぜ 4MB も使っているの？」と思われたかもしれません。
実は PostgreSQL のハッシュ結合には以下のような特性があります。

*   **最小限のバケツ（Buckets）**: ハッシュテーブルを作るための「枠組み（バケツ）」自体がメモリを消費します。
*   **ソフトリミット**: `work_mem` は「これを超えたらディスクを使い始めよう」という目安ですが、データの分割（Batch化）が追いつかない場合や、ハッシュの枠組み自体で設定値を超えてしまうことがあります。
*   しかし、**`Batches: 2` になっていること自体が、設定を守ろうとしてディスクに逃がした証拠**です。

---

### 3. 集約アルゴリズムの選択とメモリの関係

プランナは、利用可能なメモリ（`work_mem`）の量に応じて、集約（GROUP BY等）の手法を切り替えます。

- **HashAggregate**:
    - メモリ上にハッシュテーブルを作成して集約します。
    - **挙動**: 高速ですがメモリを多く消費します。`work_mem` が十分な場合に選択されます。
- **GroupAggregate**:
    - 入力データがソートされていることを前提に、順次集約します。
    - **挙動**: メモリ消費は非常に少ないですが、データのソート（またはインデックススキャン）が必要です。
- **ボトルネックの見分け方**: 本来 `HashAggregate` が適したクエリで `GroupAggregate` + `Sort` が選ばれている場合、`work_mem` を増やすことで `HashAggregate` へ誘導し、高速化できる可能性があります。

---

### 4. メモリ効率を高める特殊ノード：Materialize と Memoize

繰り返し同じデータにアクセスする際、メモリを効率的に使うための仕組みが実行計画に現れます。

- **Materialize**:
    - 下位ノードの結果を一時的にメモリにキャッシュし、親ノード（Nested Loop等）が何度も再利用できるようにします。
    - **実際の挙動**: 内側のテーブルを何度もスキャンするコストを抑え、2回目以降のアクセスをメモリ速度にします。
- **Memoize（PostgreSQL 14以降）**:
    - Nested Loop Joinにおいて、外側のテーブルの特定のキーに対する「内側のスキャン結果」をキャッシュします。
    - **実際の挙動**: 外側のテーブルに重複するキーが多い場合、インデックススキャン自体をスキップできるため、劇的な高速化が期待できます。
    - 実行計画上の `Hits`（キャッシュ的中数）と `Misses`（キャッシュ漏れ数）で、その有効性を確認できます。

---

**例え話** `BUFFERS` の読み取りは、**「机の広さ（メモリ）」と「本棚（ディスク）の往復」を監視すること**に似ています。 `shared hit` は必要な資料がすべて机の上にある状態ですが、`shared read` は資料を取りに本棚まで歩いている状態です。また、`work_mem` 不足によるディスク溢れ（`temp write`）は、机が狭すぎて書きかけの書類を一度床に並べて整理しているようなもので、作業効率が極端に落ちていることを示しています。