# 電子カルテ DWH 実務想定 — 発展 10 問（問題と解答）

> **対象**：講義資料 `00`〜`10`（サブクエリ）まで学習した人向け。iTunes 発展問題集と同じ出題範囲（結合・集約・`HAVING`・`CASE`・集合演算子・サブクエリ／相関サブクエリ／`EXISTS`）で、**医療 SES の現場で実際に頼まれそうな調査・レポート業務**を題材にしています。
>
> **前提**：`電子カルテDWH_セットアップ_DDL.sql` を実行して、Synthea 医療データ（`patients` / `encounters` / `conditions` / `medications` / `observations` / `allergies` など）を取り込んだ状態で解いてください。
>
> **使わない技法**：ウィンドウ関数（`OVER()` / `ROW_NUMBER()` など）と CTE（`WITH`）は範囲外なので、解答では使いません。「グループ内トップ」や「〜がない患者」も、相関サブクエリ・`EXISTS` / `NOT EXISTS`・派生表で表現します。
>
> **このデータで知っておくこと**
> - **ID は UUID**（長い英数字）。結合はこの UUID 同士で行います。
> - `encounters`（来院）が**ハブ**。`conditions`（病名）・`medications`（処方）・`observations`（検査値）などはすべて患者ID・来院IDで紐づきます。
> - `observations.value` は**文字型**（数値も文字も入る）。計算するときは `CAST(value AS NUMERIC)` が必要で、対象を数値項目（`Body Weight` など）に絞るのが安全です。
> - `patients.deathdate` が `NULL` = 生存中、`conditions.stop_date` が `NULL` = その病気が継続中、という **NULL の意味**が重要です。
> - `claims.total` は**全件 100 のダミー値**なので、金額の集計対象にはしません（件数のカウントには使えます）。
> - 来院データはおおむね **2011〜2017 年**中心。期間で絞るときは `CURRENT_DATE` ではなく年をリテラルで指定します。

---

## 想定シーン別・目次

| 区分 | 問題 | 主に使う技法 |
|---|---|---|
| レポート業務 | 1〜4 | 多テーブル結合・集約・`HAVING`・`CASE` |
| 移行・整合性調査 | 5〜7 | 重複検知・集合演算・`CAST`・クレンジング |
| 診療支援・高度な抽出 | 8〜10 | `EXISTS` / `NOT EXISTS`・相関サブクエリ |

---

### 問題 1: 【レポート】救急搬送された患者の一覧を作ってください

来院種別が「救急（`Emergency room admission`）」の来院について、**患者氏名（姓名を連結）・来院日・来院理由**を、来院日の新しい順で表示してください。

**狙い**：`patients` と `encounters` の結合＋文字列連結。実務で最初に頼まれる「リスト作成」。

**解答:**

```sql
SELECT
  p.first_name || ' ' || p.last_name AS patient_name,
  e.enc_date,
  e.reasondescription AS reason
FROM encounters AS e
JOIN patients AS p ON e.patient_id = p.id
WHERE e.description = 'Emergency room admission'
ORDER BY e.enc_date DESC;
```

**期待される結果**：救急来院の一覧（約 700 件）。「いつ・誰が・なぜ救急に来たか」が 1 枚の表になります。まずは 2 テーブル結合＋`||` の連結に慣れる問題。

---

### 問題 2: 【レポート】来院種別ごとの件数と構成比を出してください

`encounters.description`（来院種別）ごとに**件数**を数え、さらに**全来院に占める割合（％, 小数第 1 位）**を付けて、多い順に表示してください。

**狙い**：`GROUP BY` + `COUNT` に、全体件数を返すスカラサブクエリを組み合わせて「割合」を出す。

**解答:**

```sql
SELECT
  e.description AS encounter_type,
  COUNT(*) AS cnt,
  ROUND(100.0 * COUNT(*) / (SELECT COUNT(*) FROM encounters), 1) AS pct
FROM encounters AS e
GROUP BY e.description
ORDER BY cnt DESC;
```

**期待される結果**：`Outpatient Encounter`（外来）が最多で全体の 4 割強、以下 `Encounter for symptom`… と続きます。分母（全件数）を 1 つの値として使うスカラサブクエリで構成比を出す定番パターン。

---

### 問題 3: 【レポート】診断された病名のトップ 15 を出してください

`conditions.description`（病名）ごとに、**延べ件数**と**その病名を持つ患者数（重複なし）**を数え、延べ件数の多い順に上位 15 件を表示してください。

**狙い**：`COUNT(*)` と `COUNT(DISTINCT patient_id)` の使い分け。

**解答:**

```sql
SELECT
  c.description AS disease,
  COUNT(*) AS total_cases,
  COUNT(DISTINCT c.patient_id) AS patient_count
FROM conditions AS c
GROUP BY c.description
ORDER BY total_cases DESC
LIMIT 15;
```

**期待される結果**：`Viral sinusitis`（ウイルス性副鼻腔炎）が最多、`Prediabetes`・`Hypertension` などの生活習慣病が続きます。延べ件数（何回診断されたか）と実患者数（何人が持つか）が別物であることを体感する問題。

---

### 問題 4: 【レポート】人種区分を日本語ラベルに変換して患者数を集計してください

`patients.race` を `CASE` で日本語（`白人` / `黒人` / `アジア系` / `ヒスパニック系` / `その他`）に変換し、区分ごとの**患者数**と、そのうち**死亡している人数**を表示してください。

**狙い**：コード値の日本語マッピング（移行で頻出）＋ `CASE` を `COUNT` の中に入れる条件付き集計。

**解答:**

```sql
SELECT
  CASE p.race
    WHEN 'white'    THEN '白人'
    WHEN 'black'    THEN '黒人'
    WHEN 'asian'    THEN 'アジア系'
    WHEN 'hispanic' THEN 'ヒスパニック系'
    ELSE 'その他'
  END AS race_label,
  COUNT(*) AS patient_count,
  COUNT(p.deathdate) AS deceased_count
FROM patients AS p
GROUP BY race_label
ORDER BY patient_count DESC;
```

**期待される結果**：区分ごとの人数と死亡数が並びます。`COUNT(p.deathdate)` は「`deathdate` が `NULL` でない行だけ数える」＝死亡者数、というテクニック（`COUNT(*)` との違いがポイント）。旧コードを新ラベルへ変換する `CASE` は移行作業そのもの。

---

### 問題 5: 【移行調査】社会保障番号（SSN）が重複している患者を洗い出してください

移行前のデータ点検です。`patients.ssn` が**2 件以上重複している**番号を特定し、その SSN・重複数、および該当する患者の氏名を一覧にしてください。

**狙い**：`GROUP BY ssn` + `HAVING COUNT(*) > 1` で重複検知 → その結果を使って本人を特定（`IN` サブクエリ）。

**解答:**

```sql
-- まず重複している SSN を特定
SELECT ssn, COUNT(*) AS dup_count
FROM patients
WHERE ssn IS NOT NULL
GROUP BY ssn
HAVING COUNT(*) > 1;

-- 重複 SSN を持つ患者の氏名まで出す
SELECT p.ssn, p.id, p.first_name || ' ' || p.last_name AS patient_name
FROM patients AS p
WHERE p.ssn IN (
  SELECT ssn
  FROM patients
  WHERE ssn IS NOT NULL
  GROUP BY ssn
  HAVING COUNT(*) > 1
)
ORDER BY p.ssn;
```

**期待される結果**：重複 SSN がピンポイントで数件だけ出ます（データ上は 2 番号）。「集計で怪しいキーを見つけ→その明細を引く」という調査の王道 2 段構え。移行現場で最初に叩く “名寄せ” クエリです。

---

### 問題 6: 【移行調査】糖尿病と高血圧を併発している患者を抽出してください

`conditions` で「`Diabetes`（糖尿病）」の病名を持つ患者集合と、「`Hypertension`（高血圧）」の病名を持つ患者集合の**共通部分**（併発患者）の患者IDを取得し、氏名も付けてください。

**狙い**：集合演算子 `INTERSECT`。

**解答:**

```sql
SELECT p.first_name || ' ' || p.last_name AS patient_name, p.id
FROM patients AS p
WHERE p.id IN (
  SELECT patient_id FROM conditions WHERE description = 'Diabetes'
  INTERSECT
  SELECT patient_id FROM conditions WHERE description = 'Hypertension'
)
ORDER BY patient_name;
```

**期待される結果**：糖尿病・高血圧の両方を診断された「重点フォロー対象」患者が抽出されます。「両方に含まれる」＝`INTERSECT` の代表例。`EXCEPT` に変えれば「糖尿病だが高血圧はない患者」も作れます。

---

### 問題 7: 【保守・調査】検査値（体重）の統計を出してください

`observations` のうち検査項目が「`Body Weight`（体重）」のレコードについて、**測定件数・平均・最小・最大（各 kg, 小数第 1 位）**を求めてください。

**狙い**：文字型 `value` を `CAST(... AS NUMERIC)` して集計する。対象を数値項目に絞る重要性。

**解答:**

```sql
SELECT
  COUNT(*) AS measurements,
  ROUND(AVG(CAST(value AS NUMERIC)), 1) AS avg_kg,
  ROUND(MIN(CAST(value AS NUMERIC)), 1) AS min_kg,
  ROUND(MAX(CAST(value AS NUMERIC)), 1) AS max_kg
FROM observations
WHERE description = 'Body Weight';
```

**期待される結果**：数千件の体重測定の統計が 1 行で出ます。`observations.value` は文字型なので、`WHERE description = 'Body Weight'` で**確実に数値だけに絞ってから** `CAST` するのが安全（絞らず全 `observations` を `CAST` すると、血圧のような非数値文字でエラーになる）。医療データの「値は文字で持っている」あるあるを体験する問題。

---

### 問題 8: 【診療支援】糖尿病なのにメトホルミン未処方の患者を探してください

「治療漏れ」の調査です。`Diabetes`（糖尿病）と診断されている患者のうち、**`Metformin`（メトホルミン）系の薬を一度も処方されていない**患者の氏名を抽出してください。

**狙い**：相関サブクエリを使った `NOT EXISTS`（`NOT IN` より安全）。`LIKE` での部分一致。

**解答:**

```sql
SELECT DISTINCT
  p.id,
  p.first_name || ' ' || p.last_name AS patient_name
FROM patients AS p
JOIN conditions AS c ON c.patient_id = p.id
WHERE c.description = 'Diabetes'
  AND NOT EXISTS (
    SELECT 1
    FROM medications AS m
    WHERE m.patient_id = p.id
      AND m.description LIKE '%Metformin%'
  )
ORDER BY patient_name;
```

**期待される結果**：糖尿病と診断されているのに標準薬が出ていない患者リスト＝フォローすべき対象が浮かび上がります。`NOT EXISTS` は「その患者に一致する処方が 1 件も無ければ真」。医療データは薬品名が長い（`24 HR Metformin hydrochloride 500 MG ...`）ため、`= 'Metformin'` ではヒットせず `LIKE '%Metformin%'` が必要、という現場の勘所も学べます。

---

### 問題 9: 【診療支援】来院回数が平均より多い「ヘビーユーザー」患者を出してください

患者ごとの来院回数を数え、**全患者の平均来院回数を上回る**患者の氏名と来院回数を、多い順に上位 20 名表示してください。

**狙い**：派生表で患者別来院回数を作り、その平均をスカラサブクエリで比較。

**解答:**

```sql
SELECT
  p.first_name || ' ' || p.last_name AS patient_name,
  COUNT(e.id) AS visit_count
FROM patients AS p
JOIN encounters AS e ON e.patient_id = p.id
GROUP BY p.id, patient_name
HAVING COUNT(e.id) > (
  SELECT AVG(v.cnt)
  FROM (
    SELECT COUNT(*) AS cnt
    FROM encounters
    GROUP BY patient_id
  ) AS v
)
ORDER BY visit_count DESC
LIMIT 20;
```

**期待される結果**：受診回数が突出して多い患者（＝医療資源をよく使う層）が並びます。「集計した結果（患者別回数）をさらに集計（平均）する」には派生表が必要、という重要パターン。

---

### 問題 10: 【総合】病名別の患者数と死亡者数・死亡割合を出してください

経営・臨床レポートの総合問題です。`conditions` の病名ごとに、**その病名を持つ患者数**・**そのうち死亡している患者数**・**死亡割合（％, 小数第 1 位）**を求め、患者数が 50 人以上の病名に絞って、死亡割合の高い順に表示してください。

**狙い**：結合＋条件付き集計（`CASE` を `COUNT(DISTINCT ...)` の中に）＋ `HAVING` ＋割合計算の総合。

**解答:**

```sql
SELECT
  c.description AS disease,
  COUNT(DISTINCT c.patient_id) AS patient_count,
  COUNT(DISTINCT CASE WHEN p.deathdate IS NOT NULL THEN c.patient_id END) AS deceased_count,
  ROUND(
    100.0 * COUNT(DISTINCT CASE WHEN p.deathdate IS NOT NULL THEN c.patient_id END)
          / COUNT(DISTINCT c.patient_id),
    1
  ) AS death_rate_pct
FROM conditions AS c
JOIN patients AS p ON p.id = c.patient_id
GROUP BY c.description
HAVING COUNT(DISTINCT c.patient_id) >= 50
ORDER BY death_rate_pct DESC, patient_count DESC;
```

**期待される結果**：`Stroke`（脳卒中）や心疾患系など、重篤な病名ほど死亡割合が高く上位に来る、直感に合う結果になります。`CASE` を `COUNT(DISTINCT ...)` の中に入れて「条件を満たす患者だけを重複なく数える」テクニックが山場。分母・分子ともに `DISTINCT` を付けないと、1 人が複数回診断された分だけ数字が膨らむ点にも注意。

---

## 補足：この問題集のねらい・教え方のポイント

- **医療データの NULL には意味がある**。`deathdate IS NULL`＝生存、`stop_date IS NULL`＝治療継続中。集計時に `COUNT(列名)`（NULL を除いて数える）と `COUNT(*)`（全行数える）を使い分けると、「死亡者数」などが 1 行で出せます（問 4・10）。
- **`= ` より `LIKE`**。薬品名・病名は規格や別名で長くなりがち（`24 HR Metformin hydrochloride 500 MG ...`）。完全一致では取りこぼすので、部分一致 `LIKE '%キーワード%'` を使う癖をつけましょう（問 8）。
- **文字型の値は絞ってから `CAST`**。`observations.value` は文字型。数値計算する前に `WHERE description = '…'` で数値項目に限定しないと、非数値文字が混ざって `CAST` がエラーになります（問 7）。
- **「〜がない」は `NOT EXISTS`**。`NOT IN` はサブクエリ側に `NULL` があると全体が空になる罠があります。治療漏れ・未登録の抽出は `NOT EXISTS` が安全（問 8）。
- **`CURRENT_DATE` 相対で書かない**。データは 2011〜2017 年中心の履歴。期間は年をリテラルで指定します。
- **セットアップ SQL 自体が教材**。`電子カルテDWH_セットアップ_DDL.sql` は、日付フォーマットのばらつきを「TEXT ステージング → `TO_DATE` で型変換 → `TRIM`/`NULLIF` でクレンジング」して取り込む構成にしてあり、これはそのまま**データ移行作業のミニチュア**になっています。余裕があれば中身も読んでみてください。
```

