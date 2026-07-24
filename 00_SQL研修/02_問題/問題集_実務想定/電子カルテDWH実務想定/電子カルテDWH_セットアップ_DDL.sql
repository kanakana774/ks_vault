/*******************************************************************************
  電子カルテ DWH 実務想定 — セットアップ用 DDL / データ取り込みスクリプト
  データ元 : Synthea 合成医療データ（archive/*.csv）
  対象 DB  : PostgreSQL

  ■ このスクリプトの狙い
    archive フォルダの 10 個の CSV を PostgreSQL に取り込み、JOIN・集計・サブクエリ
    の練習ができる環境を作ります。

  ■ なぜ「ステージング（受け皿）→ 本番テーブル」の 2 段構えなのか
    CSV の日付フォーマットがファイルごとにバラバラだからです。
      - patients          : 1929/4/8      （YYYY/MM/DD, 0埋めなし）
      - allergies         : 3/11/95       （MM/DD/YY,  2桁年！）
      - それ以外          : 2008-03-11    （ISO 形式）
    これを DATE 型の列へ直接 COPY すると変換エラーで落ちます。
    そこで実務同様、まず「何でも入る TEXT だけのステージング表」に丸ごと COPY し、
    そのあと SQL で型変換・クレンジング（TRIM / NULLIF）しながら本番表へ INSERT します。
    （＝「中間テーブルを使ったデータ移行」のミニチュア体験にもなっています）

  ■ 実行方法（pgAdmin の場合）
    このファイルを丸ごとクエリツールに貼り付けて実行するだけで完了します。
    COPY はサーバ（PostgreSQL 本体）がファイルを読むため、PostgreSQL が
    archive フォルダを読める必要があります（ローカル環境なら通常OK）。
    もし COPY で権限エラーが出る場合は、各 COPY 行を psql の \copy に置き換えるか、
    pgAdmin の「Import/Export」機能で各ステージング表に取り込んでください。
********************************************************************************/


/*==============================================================================
  0. 後片付け（何度でも作り直せるように）
==============================================================================*/
DROP TABLE IF EXISTS claims        CASCADE;
DROP TABLE IF EXISTS careplans     CASCADE;
DROP TABLE IF EXISTS immunizations CASCADE;
DROP TABLE IF EXISTS procedures    CASCADE;
DROP TABLE IF EXISTS allergies     CASCADE;
DROP TABLE IF EXISTS observations  CASCADE;
DROP TABLE IF EXISTS medications   CASCADE;
DROP TABLE IF EXISTS conditions    CASCADE;
DROP TABLE IF EXISTS encounters    CASCADE;
DROP TABLE IF EXISTS patients      CASCADE;

DROP TABLE IF EXISTS stg_claims        CASCADE;
DROP TABLE IF EXISTS stg_careplans     CASCADE;
DROP TABLE IF EXISTS stg_immunizations CASCADE;
DROP TABLE IF EXISTS stg_procedures    CASCADE;
DROP TABLE IF EXISTS stg_allergies     CASCADE;
DROP TABLE IF EXISTS stg_observations  CASCADE;
DROP TABLE IF EXISTS stg_medications   CASCADE;
DROP TABLE IF EXISTS stg_conditions    CASCADE;
DROP TABLE IF EXISTS stg_encounters    CASCADE;
DROP TABLE IF EXISTS stg_patients      CASCADE;


/*==============================================================================
  1. ステージング表（全カラム TEXT・CSV の列順どおり）
     COPY は「列名」ではなく「列の並び順」で取り込むので、CSV の順番に合わせます。
==============================================================================*/
CREATE TABLE stg_patients (
    patient TEXT, birthdate TEXT, deathdate TEXT, ssn TEXT, drivers TEXT,
    passport TEXT, prefix TEXT, first TEXT, last TEXT, suffix TEXT, maiden TEXT,
    marital TEXT, race TEXT, ethnicity TEXT, gender TEXT, birthplace TEXT, address TEXT
);
CREATE TABLE stg_encounters (
    id TEXT, date TEXT, patient TEXT, code TEXT, description TEXT,
    reasoncode TEXT, reasondescription TEXT
);
CREATE TABLE stg_conditions (
    start TEXT, stop TEXT, patient TEXT, encounter TEXT, code TEXT, description TEXT
);
CREATE TABLE stg_medications (
    start TEXT, stop TEXT, patient TEXT, encounter TEXT, code TEXT, description TEXT,
    reasoncode TEXT, reasondescription TEXT
);
CREATE TABLE stg_observations (
    date TEXT, patient TEXT, encounter TEXT, code TEXT, description TEXT,
    value TEXT, units TEXT
);
CREATE TABLE stg_allergies (
    start TEXT, stop TEXT, patient TEXT, encounter TEXT, code TEXT, description TEXT
);
CREATE TABLE stg_procedures (
    date TEXT, patient TEXT, encounter TEXT, code TEXT, description TEXT,
    reasoncode TEXT, reasondescription TEXT
);
CREATE TABLE stg_immunizations (
    date TEXT, patient TEXT, encounter TEXT, code TEXT, description TEXT
);
CREATE TABLE stg_careplans (
    id TEXT, start TEXT, stop TEXT, patient TEXT, encounter TEXT, code TEXT,
    description TEXT, reasoncode TEXT, reasondescription TEXT
);
CREATE TABLE stg_claims (
    id TEXT, patient TEXT, billableperiod TEXT, organization TEXT, encounter TEXT,
    diagnosis TEXT, total TEXT
);


/*==============================================================================
  2. CSV を丸ごとステージング表へ取り込み
     ※ 下のパスは環境に合わせて書き換えてください（区切りは「/」でOK）。
     ※ pgAdmin でパス権限エラーになる場合は \copy （psql）に置き換えてください。
==============================================================================*/
COPY stg_patients      FROM 'C:/Users/kanamaru/Documents/MyWork/vaults/lecturer-vault/00_SQL研修/02_問題/問題集_実務想定/電子カルテDWH実務想定/archive/patients.csv'      WITH (FORMAT csv, HEADER true);
COPY stg_encounters    FROM 'C:/Users/kanamaru/Documents/MyWork/vaults/lecturer-vault/00_SQL研修/02_問題/問題集_実務想定/電子カルテDWH実務想定/archive/encounters.csv'    WITH (FORMAT csv, HEADER true);
COPY stg_conditions    FROM 'C:/Users/kanamaru/Documents/MyWork/vaults/lecturer-vault/00_SQL研修/02_問題/問題集_実務想定/電子カルテDWH実務想定/archive/conditions.csv'    WITH (FORMAT csv, HEADER true);
COPY stg_medications   FROM 'C:/Users/kanamaru/Documents/MyWork/vaults/lecturer-vault/00_SQL研修/02_問題/問題集_実務想定/電子カルテDWH実務想定/archive/medications.csv'   WITH (FORMAT csv, HEADER true);
COPY stg_observations  FROM 'C:/Users/kanamaru/Documents/MyWork/vaults/lecturer-vault/00_SQL研修/02_問題/問題集_実務想定/電子カルテDWH実務想定/archive/observations.csv'  WITH (FORMAT csv, HEADER true);
COPY stg_allergies     FROM 'C:/Users/kanamaru/Documents/MyWork/vaults/lecturer-vault/00_SQL研修/02_問題/問題集_実務想定/電子カルテDWH実務想定/archive/allergies.csv'     WITH (FORMAT csv, HEADER true);
COPY stg_procedures    FROM 'C:/Users/kanamaru/Documents/MyWork/vaults/lecturer-vault/00_SQL研修/02_問題/問題集_実務想定/電子カルテDWH実務想定/archive/procedures.csv'    WITH (FORMAT csv, HEADER true);
COPY stg_immunizations FROM 'C:/Users/kanamaru/Documents/MyWork/vaults/lecturer-vault/00_SQL研修/02_問題/問題集_実務想定/電子カルテDWH実務想定/archive/immunizations.csv' WITH (FORMAT csv, HEADER true);
COPY stg_careplans     FROM 'C:/Users/kanamaru/Documents/MyWork/vaults/lecturer-vault/00_SQL研修/02_問題/問題集_実務想定/電子カルテDWH実務想定/archive/careplans.csv'     WITH (FORMAT csv, HEADER true);
COPY stg_claims        FROM 'C:/Users/kanamaru/Documents/MyWork/vaults/lecturer-vault/00_SQL研修/02_問題/問題集_実務想定/電子カルテDWH実務想定/archive/claims.csv'        WITH (FORMAT csv, HEADER true);


/*==============================================================================
  3. 本番テーブル（型・主キー・外部キーつき）
     ・患者や来院を親、その他を子とする参照関係を FK で表現します。
     ・行単位の明細表（conditions など）には代理キー record_id を付けます。
==============================================================================*/
CREATE TABLE patients (
    id          VARCHAR(64) PRIMARY KEY,      -- 患者ID（UUID）
    birthdate   DATE,                         -- 生年月日
    deathdate   DATE,                         -- 死亡日（生存中は NULL）
    ssn         VARCHAR(20),                  -- 社会保障番号
    drivers     VARCHAR(20),
    passport    VARCHAR(20),
    prefix      VARCHAR(10),
    first_name  VARCHAR(100),
    last_name   VARCHAR(100),
    suffix      VARCHAR(10),
    maiden      VARCHAR(100),
    marital     CHAR(1),                      -- M:既婚, S:独身, NULL:不明
    race        VARCHAR(30),
    ethnicity   VARCHAR(30),
    gender      CHAR(1),                      -- M / F
    birthplace  VARCHAR(200),
    address     TEXT
);

CREATE TABLE encounters (
    id          VARCHAR(64) PRIMARY KEY,      -- 来院ID（UUID）
    enc_date    DATE,                         -- 来院日
    patient_id  VARCHAR(64) REFERENCES patients(id),
    code        VARCHAR(30),
    description VARCHAR(200),                 -- 来院種別（Outpatient Encounter 等）
    reasoncode  VARCHAR(30),
    reasondescription VARCHAR(200)            -- 来院理由
);

CREATE TABLE conditions (
    record_id    BIGSERIAL PRIMARY KEY,
    start_date   DATE,                        -- 発症・診断日
    stop_date    DATE,                        -- 治癒・終了日（継続中は NULL）
    patient_id   VARCHAR(64) REFERENCES patients(id),
    encounter_id VARCHAR(64) REFERENCES encounters(id),
    code         VARCHAR(30),
    description  VARCHAR(300)                 -- 病名（Diabetes 等）
);

CREATE TABLE medications (
    record_id    BIGSERIAL PRIMARY KEY,
    start_date   DATE,
    stop_date    DATE,
    patient_id   VARCHAR(64) REFERENCES patients(id),
    encounter_id VARCHAR(64) REFERENCES encounters(id),
    code         VARCHAR(30),
    description  VARCHAR(300),                -- 薬品名
    reasoncode   VARCHAR(30),
    reasondescription VARCHAR(300)            -- 処方理由（病名）
);

CREATE TABLE observations (
    record_id    BIGSERIAL PRIMARY KEY,
    obs_date     DATE,
    patient_id   VARCHAR(64) REFERENCES patients(id),
    encounter_id VARCHAR(64) REFERENCES encounters(id),
    code         VARCHAR(30),
    description  VARCHAR(200),               -- 検査項目名（Body Weight 等）
    value        VARCHAR(100),              -- 測定値（数値も文字も入るため文字型）
    units        VARCHAR(30)
);

CREATE TABLE allergies (
    record_id    BIGSERIAL PRIMARY KEY,
    start_date   DATE,
    stop_date    DATE,
    patient_id   VARCHAR(64) REFERENCES patients(id),
    encounter_id VARCHAR(64) REFERENCES encounters(id),
    code         VARCHAR(30),
    description  VARCHAR(200)               -- アレルゲン名
);

CREATE TABLE procedures (
    record_id    BIGSERIAL PRIMARY KEY,
    proc_date    DATE,
    patient_id   VARCHAR(64) REFERENCES patients(id),
    encounter_id VARCHAR(64) REFERENCES encounters(id),
    code         VARCHAR(30),
    description  VARCHAR(300),              -- 処置名
    reasoncode   VARCHAR(30),
    reasondescription VARCHAR(300)
);

CREATE TABLE immunizations (
    record_id    BIGSERIAL PRIMARY KEY,
    imm_date     DATE,
    patient_id   VARCHAR(64) REFERENCES patients(id),
    encounter_id VARCHAR(64) REFERENCES encounters(id),
    code         VARCHAR(30),
    description  VARCHAR(200)               -- ワクチン名
);

CREATE TABLE careplans (
    record_id    BIGSERIAL PRIMARY KEY,
    careplan_id  VARCHAR(64),               -- ケアプランID（複数行で重複するため PK にしない）
    start_date   DATE,
    stop_date    DATE,
    patient_id   VARCHAR(64) REFERENCES patients(id),
    encounter_id VARCHAR(64) REFERENCES encounters(id),
    code         VARCHAR(30),
    description  VARCHAR(300),
    reasoncode   VARCHAR(30),
    reasondescription VARCHAR(300)
);

CREATE TABLE claims (
    id           VARCHAR(64) PRIMARY KEY,   -- 請求ID
    patient_id   VARCHAR(64) REFERENCES patients(id),
    billableperiod DATE,
    organization VARCHAR(100),
    encounter_id VARCHAR(64) REFERENCES encounters(id),
    diagnosis    VARCHAR(100),
    total        INT                        -- 請求額（このデータでは全件 100 のダミー値）
);


/*==============================================================================
  4. ステージング → 本番へ型変換しながら投入（親→子の順で）
     ・日付は TO_DATE でフォーマットを明示（ファイルごとに書式が違う点に注意）
     ・NULLIF(TRIM(x),'') で空文字を NULL に、余計な空白を除去（データクレンジング）
==============================================================================*/

-- 4-1. patients（親）: 日付は YYYY/MM/DD
INSERT INTO patients
SELECT
    patient,
    TO_DATE(NULLIF(TRIM(birthdate), ''), 'YYYY/MM/DD'),
    TO_DATE(NULLIF(TRIM(deathdate), ''), 'YYYY/MM/DD'),
    NULLIF(TRIM(ssn), ''),      NULLIF(TRIM(drivers), ''),  NULLIF(TRIM(passport), ''),
    NULLIF(TRIM(prefix), ''),   NULLIF(TRIM(first), ''),    NULLIF(TRIM(last), ''),
    NULLIF(TRIM(suffix), ''),   NULLIF(TRIM(maiden), ''),   NULLIF(TRIM(marital), ''),
    NULLIF(TRIM(race), ''),     NULLIF(TRIM(ethnicity), ''),NULLIF(TRIM(gender), ''),
    NULLIF(TRIM(birthplace), ''),NULLIF(TRIM(address), '')
FROM stg_patients;

-- 4-2. encounters（患者の子）: ISO 日付
INSERT INTO encounters
SELECT
    id,
    NULLIF(TRIM(date), '')::date,
    patient,
    NULLIF(TRIM(code), ''),
    NULLIF(TRIM(description), ''),
    NULLIF(TRIM(reasoncode), ''),
    NULLIF(TRIM(reasondescription), '')
FROM stg_encounters;

-- 4-3. conditions: ISO 日付
INSERT INTO conditions (start_date, stop_date, patient_id, encounter_id, code, description)
SELECT
    NULLIF(TRIM(start), '')::date,
    NULLIF(TRIM(stop), '')::date,
    patient, encounter,
    NULLIF(TRIM(code), ''),
    NULLIF(TRIM(description), '')
FROM stg_conditions;

-- 4-4. medications: ISO 日付
INSERT INTO medications (start_date, stop_date, patient_id, encounter_id, code, description, reasoncode, reasondescription)
SELECT
    NULLIF(TRIM(start), '')::date,
    NULLIF(TRIM(stop), '')::date,
    patient, encounter,
    NULLIF(TRIM(code), ''),
    NULLIF(TRIM(description), ''),
    NULLIF(TRIM(reasoncode), ''),
    NULLIF(TRIM(reasondescription), '')
FROM stg_medications;

-- 4-5. observations: ISO 日付。value は文字のまま保持
INSERT INTO observations (obs_date, patient_id, encounter_id, code, description, value, units)
SELECT
    NULLIF(TRIM(date), '')::date,
    patient, encounter,
    NULLIF(TRIM(code), ''),
    NULLIF(TRIM(description), ''),
    NULLIF(TRIM(value), ''),
    NULLIF(TRIM(units), '')
FROM stg_observations;

-- 4-6. allergies: 日付は MM/DD/YY（2桁年！ここだけ書式が違う）
INSERT INTO allergies (start_date, stop_date, patient_id, encounter_id, code, description)
SELECT
    TO_DATE(NULLIF(TRIM(start), ''), 'MM/DD/YY'),
    TO_DATE(NULLIF(TRIM(stop), ''), 'MM/DD/YY'),
    patient, encounter,
    NULLIF(TRIM(code), ''),
    NULLIF(TRIM(description), '')
FROM stg_allergies;

-- 4-7. procedures: ISO 日付
INSERT INTO procedures (proc_date, patient_id, encounter_id, code, description, reasoncode, reasondescription)
SELECT
    NULLIF(TRIM(date), '')::date,
    patient, encounter,
    NULLIF(TRIM(code), ''),
    NULLIF(TRIM(description), ''),
    NULLIF(TRIM(reasoncode), ''),
    NULLIF(TRIM(reasondescription), '')
FROM stg_procedures;

-- 4-8. immunizations: ISO 日付
INSERT INTO immunizations (imm_date, patient_id, encounter_id, code, description)
SELECT
    NULLIF(TRIM(date), '')::date,
    patient, encounter,
    NULLIF(TRIM(code), ''),
    NULLIF(TRIM(description), '')
FROM stg_immunizations;

-- 4-9. careplans: ISO 日付
INSERT INTO careplans (careplan_id, start_date, stop_date, patient_id, encounter_id, code, description, reasoncode, reasondescription)
SELECT
    id,
    NULLIF(TRIM(start), '')::date,
    NULLIF(TRIM(stop), '')::date,
    patient, encounter,
    NULLIF(TRIM(code), ''),
    NULLIF(TRIM(description), ''),
    NULLIF(TRIM(reasoncode), ''),
    NULLIF(TRIM(reasondescription), '')
FROM stg_careplans;

-- 4-10. claims: ISO 日付。total は前後の空白を除いて整数化
INSERT INTO claims (id, patient_id, billableperiod, organization, encounter_id, diagnosis, total)
SELECT
    id, patient,
    NULLIF(TRIM(billableperiod), '')::date,
    NULLIF(TRIM(organization), ''),
    encounter,
    NULLIF(TRIM(diagnosis), ''),
    NULLIF(TRIM(total), '')::int
FROM stg_claims;


/*==============================================================================
  5. 索引（練習クエリの体感速度向上・任意）
==============================================================================*/
CREATE INDEX idx_enc_patient   ON encounters(patient_id);
CREATE INDEX idx_cond_patient  ON conditions(patient_id);
CREATE INDEX idx_cond_desc     ON conditions(description);
CREATE INDEX idx_med_patient   ON medications(patient_id);
CREATE INDEX idx_med_desc      ON medications(description);
CREATE INDEX idx_obs_patient   ON observations(patient_id);
CREATE INDEX idx_obs_desc      ON observations(description);


/*==============================================================================
  6. 取り込み結果の確認（件数チェック）
     期待値の目安: patients≈1462, encounters≈20524, conditions≈7040,
                   medications≈6048, observations≈79674, allergies≈571,
                   procedures≈10184, immunizations≈13189, careplans≈12125,
                   claims≈20523
==============================================================================*/
SELECT 'patients'      AS table_name, COUNT(*) AS rows FROM patients
UNION ALL SELECT 'encounters',    COUNT(*) FROM encounters
UNION ALL SELECT 'conditions',    COUNT(*) FROM conditions
UNION ALL SELECT 'medications',   COUNT(*) FROM medications
UNION ALL SELECT 'observations',  COUNT(*) FROM observations
UNION ALL SELECT 'allergies',     COUNT(*) FROM allergies
UNION ALL SELECT 'procedures',    COUNT(*) FROM procedures
UNION ALL SELECT 'immunizations', COUNT(*) FROM immunizations
UNION ALL SELECT 'careplans',     COUNT(*) FROM careplans
UNION ALL SELECT 'claims',        COUNT(*) FROM claims
ORDER BY table_name;

/*==============================================================================
  7. 後片付け（任意）: ステージング表はもう不要なので消してよい
==============================================================================*/
-- DROP TABLE stg_patients, stg_encounters, stg_conditions, stg_medications,
--            stg_observations, stg_allergies, stg_procedures, stg_immunizations,
--            stg_careplans, stg_claims;
