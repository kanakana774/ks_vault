# React導入 hooks編

この資料は、パフォーマンス系・仕組み系の React フックをまとめたリファレンスです。
各フックは **「なぜ要るのか（動機）→ 仕組み → コード → 観察ポイント」** の順で並べています。
先に "なぜ" を押さえてから、書き方に進んでください。

各節には、実行して手を動かすための `.tsx`（`Hook1`〜`Hook6`）が対応しています。
文章で理解 → コードで観察、の往復で身につけるのが狙いです。

---

## この資料の地図

| 部 | テーマ | フック | 論点 |
|----|--------|--------|------|
| 第1部 | パフォーマンス（無駄を減らす） | React.memo / useCallback / useMemo | 「やらなくていい処理」を省く |
| 第2部 | 仕組み・ライフサイクル | useContext / useRef / useEffect | 値の届け方・保持・実行タイミング |

**大前提（第1部の共通注意）**
第1部の3つは最初から何にでも使うものではありません。まず素直に書き、**実測で遅い所だけ**に使います。早すぎる最適化はコードを複雑にするだけです。

---

# 第1部 パフォーマンス系

## 1. React.memo — 子の再レンダーをスキップする

対応コード: `Hook1_memo.tsx`（画面の枠が光る＝再レンダーが起きた印）

### なぜ要るのか
React は既定で「親が再レンダーすると、子も全部再レンダー」します。ふだんはこれで十分速いので気にしなくてよいのですが、子の描画が重いとき、**props が変わっていないのに毎回作り直す**のは無駄になります。

`React.memo` は「props が前回と同じなら、その子の再レンダーをスキップ」させる仕組みです。判定は shallow compare（浅い比較）で行われます。

### 仕組み
- 親が再レンダー → memo なしの子は毎回再レンダー
- memo ありの子は、props が前回と同じならスキップ

### コード
```tsx
// ✕ memo なし：親が再レンダーするたび、自分も再レンダー
function NormalChild() { /* ... */ }

// ○ memo あり：props が前回と同じならスキップ
const MemoChild = memo(function MemoChild() { /* ... */ });
```

### 観察ポイント
「再レンダー」ボタンを押すと、`NormalChild`（赤）は毎回光り、`MemoChild`（緑）は最初の一度きりで光らなくなります。**再レンダーの波及がどこで止まるか**を目で見られます。

---

## 2. useCallback — 関数の参照を固定する

対応コード: `Hook2_useCallback.tsx`（3列: なし / [] / [dep]）

### なぜ要るのか（1. との接続）
`React.memo` した緑の子は光らなくなりました。ところが**親から「関数」を props で渡すと、緑がまた光り出します**。なぜか？

関数は再レンダーのたびに新しく作り直されます。中身は同じでも参照は別物です。memo の浅い比較は「前回と違う props」と判定し、スキップをやめてしまうのです。

`useCallback` は関数の参照を保持して、これを防ぎます。

### 仕組み（3パターン）
```tsx
const handle = () => {};                     // ① 毎回新しい関数 → memo が効かない
const handle = useCallback(() => {}, []);    // ② 参照を固定 → memo が効く
const handle = useCallback(() => {}, [dep]); // ③ dep が変わった時だけ作り直す
```

### 観察ポイント
- **① なし**：再レンダーで子も光る（関数が毎回別物）
- **② []**：再レンダーでも子は光らない（参照固定）
- **③ [dep]**：再レンダーでは光らない／「dep 変更」で光る

---

## 3. useMemo — 重い計算をスキップする

対応コード: `Hook3_useMemo.tsx`（重い計算＝3秒待機、3パターン）

### なぜ要るのか
コンポーネントは色々な理由で再レンダーします（無関係な state 変更など）。そのたびに**重い計算まで走るのは無駄**です。入力が同じなら結果も同じはず。

`useMemo` は計算結果をキャッシュし、依存配列に入れた値が変わった時だけ計算し直します。

### memo との違い（論点が別）
| | 何をスキップする？ |
|---|---|
| React.memo | 子の**再レンダー** |
| useMemo | 重い**計算** |

### 仕組み（3パターン）
違うのは第2引数（依存配列）だけです。
```tsx
useMemo(() => heavyCompute(input), undefined); // ① 依存配列なし → 毎回計算（書いても無意味）
useMemo(() => heavyCompute(input), []);        // ② []          → 最初の1回だけ（結果が固まる＝罠）
useMemo(() => heavyCompute(input), [input]);   // ③ [input]     → input が変わった時だけ（正しい）
```

### 観察ポイント
計算が走ると 3秒フリーズし、`console` に `計算実行: n = ○` が出ます。
- **①** どのボタンでも毎回3秒
- **②** 初回だけ3秒。以降 input を変えても再計算されず、**結果が古いまま**（`結果` と `input` がズレる）
- **③** input 変更時だけ3秒。無関係な再レンダーは即時

### useCallback との関係
発想も依存配列の考え方も useCallback と同じで、**包む対象が違うだけ**です。
- 関数を包む → `useCallback`
- 計算結果（値）を包む → `useMemo`
- 実際 `useCallback(fn, deps)` は `useMemo(() => fn, deps)` と等価

---

## ＜第1部のまとめ＞ 依存配列の共通ルール

`useCallback` / `useMemo`（そして第2部の `useEffect`）は、**同じ依存配列のルール**で動きます。ここを一度で押さえると3つまとめて理解できます。

| 依存配列 | 意味 |
|----------|------|
| なし（第2引数を書かない） | 毎回実行し直す |
| `[]` | 最初の1回だけ |
| `[a, b]` | `a` か `b` が変わった時だけ |

「使う値は必ず依存配列に入れる」が原則です。入れ忘れると、useMemo の②のように**結果が古いまま固まる**バグになります。

---

# 第2部 仕組み・ライフサイクル系

## 4. useContext — prop のバケツリレーを消す

対応コード: `Hook4_useContext.tsx`（4階層の構造図で比較）

### なぜ要るのか
これは性能最適化ではなく、**値の届け方**の話です。

深くネストしたコンポーネントに値を渡すとき、間の層が自分では使わないのに props を下へ下へと素通しさせる —— これが「バケツリレー（prop drilling）」です。層が増えるほど、関係ない中間コンポーネントが props で汚れていきます。

`useContext` は、`Provider` で配った値を、間を飛ばして必要な所が**直接**読めるようにします。

### 仕組み
```tsx
const UserContext = createContext("");

// ✕ バケツリレー：App → MiddleA → MiddleB → Leaf と user を素通しで渡す
// ○ Context：App を Provider で包み、Leaf が直接読む
function Leaf() {
  const user = useContext(UserContext); // 間を飛ばして直接読む
  return <p>こんにちは、{user} さん</p>;
}
```

### 観察ポイント
- **① バケツリレー**：中間の MiddleA / MiddleB に「user を素通し」チップが付く＝使わないのに受け渡すノイズが階層分だけ増える
- **② useContext**：中間は「props なし」で空。Leaf だけが直接読む

「名前変更」で Leaf の表示が変わるのは両方同じ。違うのは**中間層の汚れ方**だけです。

---

## 5. useRef — 再レンダーを起こさずに値を保持する

対応コード: `Hook5_useRef.tsx`（useState との対比）

### なぜ要るのか
再レンダーをまたいで値を覚えておきたいが、**変更のたびに再描画はしたくない**、という場面があります。普通の変数は再レンダーで毎回 0 に戻るので使えず、かといって `useState` にすると値を変えるたびに再レンダーが走ってしまいます。

`useRef` はその中間 —— **値は保持、変更しても再レンダーは起こさない**。

| | 変更すると再レンダー？ | 用途 |
|---|---|---|
| useState | する | 画面に出す値 |
| useRef | しない | 裏で持つ値・前回値の記憶・DOM 参照 |

### 仕組み
```tsx
const [count, setCount] = useState(0); // 変更 → 再レンダー → 画面も更新
const countRef = useRef(0);            // countRef.current を変えても再レンダーしない（値は保持）
```

### 観察ポイント
- **useState**：「+1」で数字が即増え、再レンダーが起きる
- **useRef**：「+1」しても画面は据え置き（再レンダーしていない）。でも裏では `ref.current` が増えている（console）。**「再レンダー」ボタンを押すと、貯まっていた値が一気に反映される** ＝「値は保持されていた」証拠

（補足：`useRef` にはもう一つ「DOM 参照」の用途があります。DOM 要素を直接つかんで操作したいときに使います。）

---

## 6. useEffect — 実行タイミングを理解する

対応コード: `Hook6_useEffect.tsx`（console に実行順が出る）

### なぜ要るのか
`useEffect` は、レンダー中にやってはいけない**外の世界との同期**（データ取得・購読・タイマー・DOM 操作など）を、画面が更新された**後**に安全に行う仕組みです。

### 仕組み（タイミングが肝）
実行の順序は必ずこうなります。
```
① レンダー（JSX を計算）
② コミット（画面の DOM が更新される）
③ effect 実行   ← 必ず ② の後
```
そして依存配列が「次にいつ再実行するか」を決め、`cleanup`（後片付け）は「次の effect の前」または「アンマウント時」に走ります。

```tsx
useEffect(() => {
  console.log("effect 実行 dep=" + dep);
  return () => {
    console.log("cleanup dep=" + dep); // 後片付け
  };
}, [dep]); // ← この依存配列が「いつ再実行するか」を決める
```

### 観察ポイント（console を見る）
- **マウント** → コミット → effect 実行
- **dep 変更** → コミット → cleanup(旧) → effect(新)
- **無関係な再レンダー** → コミットは出るが **effect は出ない**（毎回ではない）
- **アンマウント** → cleanup だけ（後片付け）

（注意：開発時 StrictMode では、マウント時に effect が意図的に2回走ります。console が二重に見えたらそれが原因です。）

---

## 全体まとめ：いつ何を使うか

| フック | ひとことで | 使いどき |
|--------|-----------|----------|
| React.memo | 子の再レンダーをスキップ | 重い子が、変わらない props で毎回再レンダーされている |
| useCallback | 関数の参照を固定 | memo した子に関数を渡すとき |
| useMemo | 重い計算をスキップ | 再レンダーのたびに重い計算が走っている |
| useContext | バケツリレーを消す | 深い階層へ値を届けたい／中間が props で汚れている |
| useRef | 再レンダーせず値を保持 | 裏で値を覚えたい・前回値・DOM 参照 |
| useEffect | 画面更新後に外部と同期 | データ取得・購読・タイマーなど副作用 |

**共通の心構え**
- 第1部（memo / useCallback / useMemo）は「最初から使わない、実測で遅い所だけ」。
- `useCallback` / `useMemo` / `useEffect` の依存配列は同じルール。「使う値は必ず入れる」。

---

---

# 付録：全コード（コピペ用）

各フックの完成コード（.tsx）です。そのままコピーして動かせます。
スタイル定義はファイル下部にまとめ、上のロジック（教えたい部分）を先に読めるようにしています。

## Hook1_memo.tsx（React.memo）

```tsx
/**
 * Hook1_memo.tsx — React.memo（メモ化 その1）
 *
 * ■ なぜ React.memo が要るのか（動機づけ：まず「なぜ」）
 *   React は既定で「親が再レンダーすると、子も全部再レンダー」する。
 *   ふだんは十分速い。だが子の描画が重いとき、props が変わっていないのに
 *   毎回作り直すのは“無駄足”になる。
 *   React.memo は「props が前回と同じなら、その子の再レンダーをスキップ」させる仕組み。
 *
 * ■ 画面で観察すること（仕組み）
 *   「再レンダー」を押すと App が再レンダーする。そのとき枠が一瞬光る＝再レンダーが走った印。
 *     ・子 ✕（memo なし）：App のたびに光る（毎回再レンダー）
 *     ・子 ○（memo）    ：最初の一度きり。以降は光らない（スキップ）
 *   → 再レンダーの波及がどこで止まるかが、光り方で見える。
 *
 *   （curriculum メモ：3兄弟共通の注意「最初から包まず、実測で遅い所だけ」は口頭/別途で補う）
 */

import React, { useState, useRef, useEffect, memo } from "react";

// 再レンダー（commit）のたびに枠を一瞬光らせる。
// DOM を直接触るだけなので、これ自体は再レンダーを誘発しない。
function useFlash(color: string) {
  const ref = useRef<HTMLDivElement>(null);
  useEffect(() => {
    const el = ref.current;
    if (!el) return;
    el.style.boxShadow = "0 0 0 5px " + color + "66"; // 光る（枠と同系色）
    const id = setTimeout(() => {
      if (ref.current) ref.current.style.boxShadow = "0 0 0 0 transparent";
    }, 400);
    return () => clearTimeout(id);
  }); // 依存配列なし＝毎回の再レンダー後に実行

  return ref;
}

// ✕ memo なし：親が再レンダーするたび、自分も再レンダー → 毎回光る
function NormalChild() {
  const ref = useFlash("#ef4444");
  console.log("NormalChild re-render");
  return (
    <div ref={ref} style={childBox("#ef4444")}>
      <span style={tagStyle}>子 ✕（memo なし）</span>
    </div>
  );
}

// ○ memo あり：props が前回と同じならスキップ → 最初以外は光らない
const MemoChild = memo(function MemoChild() {
  const ref = useFlash("#22c55e");
  console.log("MemoChild re-render");
  return (
    <div ref={ref} style={childBox("#22c55e")}>
      <span style={tagStyle}>子 ○（memo）</span>
    </div>
  );
});

// state を持つ親。画面の枠がそのままコンポーネント構造になっている。
export default function App() {
  const [, setTick] = useState(0); // 値は使わない。再レンダーを起こすためだけの state
  const ref = useFlash("#6366f1");
  return (
    <div style={screenStyle}>
      <div ref={ref} style={appBox}>
        <div style={appHeader}>
          <span style={tagStyle}>App（state を持つ親）</span>
          <button style={btnStyle} onClick={() => setTick((t) => t + 1)}>
            再レンダー
          </button>
        </div>

        {/* 親枠の中に子枠を物理的にネスト */}
        <NormalChild />
        <MemoChild />
      </div>
    </div>
  );
}

/* ===== 以下はスタイル定義（教えたいロジックは上、見た目は下） ===== */

const screenStyle: React.CSSProperties = {
  minHeight: "100vh",
  display: "flex",
  alignItems: "center",
  justifyContent: "center",
  background: "#f8fafc",
  padding: 24,
};

// state を持つ親枠（primary の青）
const appBox: React.CSSProperties = {
  width: 300,
  padding: 16,
  background: "#eef2ff",
  border: "2px solid #6366f1",
  borderRadius: 16,
  display: "flex",
  flexDirection: "column",
  gap: 12,
  transition: "box-shadow 0.35s ease",
};

const appHeader: React.CSSProperties = {
  display: "flex",
  justifyContent: "space-between",
  alignItems: "center",
};

// 子枠（枠線色を引数で：赤=再レンダーする / 緑=スキップ）
function childBox(color: string): React.CSSProperties {
  return {
    padding: 16,
    background: "#ffffff",
    border: "2px solid " + color,
    borderRadius: 12,
    transition: "box-shadow 0.35s ease",
  };
}

// 枠の左上のタグ（コンポーネント名）
const tagStyle: React.CSSProperties = {
  fontSize: 12,
  fontWeight: 700,
  color: "#475569",
};

const btnStyle: React.CSSProperties = {
  padding: "8px 16px",
  background: "#6366f1",
  color: "#ffffff",
  fontSize: 14,
  fontWeight: 600,
  border: "none",
  borderRadius: 10,
  cursor: "pointer",
};
```

## Hook2_useCallback.tsx（useCallback）

```tsx
/**
 * Hook2_useCallback.tsx — useCallback（関数のメモ化）
 *
 * ■ なぜ useCallback が要るのか（動機づけ：まず「なぜ」）
 *   ①で memo した緑の子は光らなくなった（＝再レンダーがスキップされた）。
 *   ところが親から「関数」を props で渡すと、緑がまた光り出す。なぜ？
 *     → 関数は再レンダーのたびに“新しく作り直される”。中身は同じでも参照は別物。
 *     → memo の浅い比較は「前回と違う props」と判定し、スキップをやめてしまう。
 *   useCallback は「関数の参照を保持」して、これを防ぐ。
 *   依存配列は「この値が変わったら関数を作り直す」を指定する。
 *
 * ■ 画面で観察すること（仕組み）— 子はすべて memo 済み(緑)。列(親)は毎回光る。
 *   ① useCallback なし  ：再レンダーで子も光る（関数が毎回別物 → memo 効かない）
 *   ② useCallback []    ：再レンダーでも子は光らない（参照が固定 → memo 効く）
 *   ③ useCallback [dep] ：再レンダーでは光らない。「dep 変更」を押した時だけ光る
 *
 *   （curriculum メモ：useCallback も最初から全部ではなく、
 *     「memo した子に関数/オブジェクトを渡す時」に使う。理由は口頭/別途で補う）
 */

import React, { useState, useRef, useEffect, useCallback, memo } from "react";

// 再レンダー（commit）のたびに枠を一瞬光らせる。DOM を直接触るだけ＝再レンダーは誘発しない。
function useFlash(color: string) {
  const ref = useRef<HTMLDivElement>(null);
  useEffect(() => {
    const el = ref.current;
    if (!el) return;
    el.style.boxShadow = "0 0 0 5px " + color + "66"; // 光る
    const id = setTimeout(() => {
      if (ref.current) ref.current.style.boxShadow = "0 0 0 0 transparent";
    }, 400);
    return () => clearTimeout(id);
  }); // 依存配列なし＝毎回の再レンダー後に実行
  return ref;
}

// 3列で共通に使う、memo 済みの子。props は「関数」だけ。
// → 渡される関数の参照が変わったかどうかで、光る／光らないが決まる。
const MemoChild = memo(function MemoChild({ onAction }: { onAction: () => void }) {
  const ref = useFlash("#22c55e");
  console.log("MemoChild re-render");
  return (
    <div ref={ref} style={childBox("#22c55e")} onClick={onAction}>
      <span style={tagStyle}>子 ○（memo）</span>
    </div>
  );
});

// ① useCallback なし：関数を毎回そのまま作る → 参照が毎回変わる
function ColNone() {
  const [, setTick] = useState(0);
  const ref = useFlash("#6366f1");
  const handle = () => {}; // 毎レンダーで新しい関数
  return (
    <div ref={ref} style={colBox}>
      <span style={titleStyle}>① useCallback なし</span>
      <button style={btnStyle} onClick={() => setTick((t) => t + 1)}>再レンダー</button>
      <MemoChild onAction={handle} />
    </div>
  );
}

// ② useCallback []：依存が空 → 参照を最初のまま固定
function ColEmpty() {
  const [, setTick] = useState(0);
  const ref = useFlash("#6366f1");
  const handle = useCallback(() => {}, []); // 参照が変わらない
  return (
    <div ref={ref} style={colBox}>
      <span style={titleStyle}>② useCallback [ ]</span>
      <button style={btnStyle} onClick={() => setTick((t) => t + 1)}>再レンダー</button>
      <MemoChild onAction={handle} />
    </div>
  );
}

// ③ useCallback [dep]：dep が変わった時だけ関数を作り直す
function ColDep() {
  const [, setTick] = useState(0);
  const [dep, setDep] = useState(0);
  const ref = useFlash("#6366f1");
  const handle = useCallback(() => {}, [dep]); // dep 変化時のみ新しい参照
  return (
    <div ref={ref} style={colBox}>
      <span style={titleStyle}>③ useCallback [dep]</span>
      <div style={btnRow}>
        <button style={btnStyle} onClick={() => setTick((t) => t + 1)}>再レンダー</button>
        <button style={btnGhost} onClick={() => setDep((d) => d + 1)}>dep 変更</button>
      </div>
      <MemoChild onAction={handle} />
    </div>
  );
}

export default function App() {
  return (
    <div style={screenStyle}>
      <ColNone />
      <ColEmpty />
      <ColDep />
    </div>
  );
}

/* ===== 以下はスタイル定義（教えたいロジックは上、見た目は下） ===== */

const screenStyle: React.CSSProperties = {
  minHeight: "100vh",
  display: "flex",
  flexWrap: "wrap",
  alignItems: "flex-start",
  justifyContent: "center",
  gap: 20,
  background: "#f8fafc",
  padding: 24,
};

// state を持つ親（列）＝青。子はこの中にネスト。
const colBox: React.CSSProperties = {
  width: 190,
  padding: 16,
  background: "#eef2ff",
  border: "2px solid #6366f1",
  borderRadius: 16,
  display: "flex",
  flexDirection: "column",
  gap: 12,
  transition: "box-shadow 0.35s ease",
};

const titleStyle: React.CSSProperties = {
  fontSize: 13,
  fontWeight: 700,
  color: "#334155",
};

// memo 済みの子＝緑
function childBox(color: string): React.CSSProperties {
  return {
    padding: 16,
    background: "#ffffff",
    border: "2px solid " + color,
    borderRadius: 12,
    cursor: "pointer",
    transition: "box-shadow 0.35s ease",
  };
}

const tagStyle: React.CSSProperties = {
  fontSize: 12,
  fontWeight: 700,
  color: "#475569",
};

const btnRow: React.CSSProperties = {
  display: "flex",
  gap: 8,
};

const btnStyle: React.CSSProperties = {
  flex: 1,
  padding: "8px 0",
  background: "#6366f1",
  color: "#ffffff",
  fontSize: 13,
  fontWeight: 600,
  border: "none",
  borderRadius: 10,
  cursor: "pointer",
};

const btnGhost: React.CSSProperties = {
  flex: 1,
  padding: "8px 0",
  background: "#ffffff",
  color: "#6366f1",
  fontSize: 13,
  fontWeight: 600,
  border: "2px solid #6366f1",
  borderRadius: 10,
  cursor: "pointer",
};
```

## Hook3_useMemo.tsx（useMemo）

```tsx
/**
 * Hook3_useMemo.tsx — useMemo（重い計算のメモ化）
 *
 * ■ なぜ useMemo が要るのか（動機づけ：まず「なぜ」）
 *   コンポーネントは色々な理由で再レンダーする（無関係な state 変更など）。
 *   そのたびに重い計算まで走るのは無駄 —— 入力が同じなら結果も同じはず。
 *   useMemo は計算結果をキャッシュし、依存配列に入れた値が変わった時だけ計算し直す。
 *
 * ■ memo との違い（論点が別）
 *   React.memo … 子を「再レンダー」しないか
 *   useMemo    … 「重い計算」をやり直さないか
 *
 * ■ 3パターン（違うのは useMemo の第2引数＝依存配列だけ）
 *   ① 依存配列なし  useMemo(fn, undefined) → 毎回計算（書いても無意味）
 *   ② []            useMemo(fn, [])        → 最初の1回だけ（input を変えても古いまま＝罠）
 *   ③ [input]       useMemo(fn, [input])   → input が変わった時だけ計算（正しい）
 *
 * ■ 観察：計算が走ると 3秒フリーズする（体感）＋ console に「計算実行: n = ○」が出る。
 *   ①は毎回3秒。③は input 変更時だけ3秒。②は初回だけで、以降は結果が固まる。
 *   （注意：マウント時は3パターンとも1回ずつ計算するので、初回表示までに数秒かかる）
 */

import React, { useState, useMemo } from "react";

const WAIT_MS = 3000; // 重い処理の代用：ここを短くすれば待ち時間を減らせる

// 重い計算のつもりで、単に WAIT_MS だけ待つ。どの値で計算したかは console に出す。
function heavyCompute(n: number) {
  console.log("計算実行: n =", n);
  const start = Date.now();
  while (Date.now() - start < WAIT_MS) {
    // 3秒間、同期的に待つ（この間 UI はフリーズする＝重さを体感）
  }
  return n * 2;
}

// ① 依存配列なし：毎レンダーで計算が走る（useMemo を書いても無意味）
function NoDeps() {
  const [input, setInput] = useState(1);
  const [other, setOther] = useState(0);

  const result = useMemo(() => heavyCompute(input), undefined);

  return <Card title="① 依存配列なし" sub="毎回計算（書いても無意味）"
    input={input} other={other} result={result} setInput={setInput} setOther={setOther} />;
}

// ② []：最初の1回だけ計算。input を変えても再計算されず、結果が古いまま
function EmptyDeps() {
  const [input, setInput] = useState(1);
  const [other, setOther] = useState(0);

  const result = useMemo(() => heavyCompute(input), []); // input を使うのに依存に入れていない＝罠

  return <Card title="② []" sub="最初の1回だけ（結果が固まる）"
    input={input} other={other} result={result} setInput={setInput} setOther={setOther} />;
}

// ③ [input]：input が変わった時だけ計算（正しい使い方）
function WithDep() {
  const [input, setInput] = useState(1);
  const [other, setOther] = useState(0);

  const result = useMemo(() => heavyCompute(input), [input]);

  return <Card title="③ [input]" sub="input 変更時だけ計算（正しい）"
    input={input} other={other} result={result} setInput={setInput} setOther={setOther} />;
}

// 表示部分は共通（教えたい差分＝上の useMemo の1行だけに集中させる）
function Card(props: {
  title: string; sub: string; input: number; other: number; result: number;
  setInput: (f: (n: number) => number) => void;
  setOther: (f: (n: number) => number) => void;
}) {
  const { title, sub, input, other, result, setInput, setOther } = props;
  return (
    <div style={colBox}>
      <span style={titleStyle}>{title}</span>
      <span style={subStyle}>{sub}</span>
      <div style={valueBox}>結果: {result}</div>
      <button style={btnStyle} onClick={() => setInput((n) => n + 1)}>input +1（今: {input}）</button>
      <button style={btnGhost} onClick={() => setOther((n) => n + 1)}>無関係な再レンダー（{other}）</button>
    </div>
  );
}

export default function App() {
  return (
    <div style={screenStyle}>
      <NoDeps />
      <EmptyDeps />
      <WithDep />
    </div>
  );
}

/* ===== 以下はスタイル定義（教えたいロジックは上、見た目は下） ===== */

const screenStyle: React.CSSProperties = {
  minHeight: "100vh",
  display: "flex",
  flexWrap: "wrap",
  alignItems: "flex-start",
  justifyContent: "center",
  gap: 20,
  background: "#f8fafc",
  padding: 24,
};

const colBox: React.CSSProperties = {
  width: 200,
  padding: 16,
  background: "#eef2ff",
  border: "2px solid #6366f1",
  borderRadius: 16,
  display: "flex",
  flexDirection: "column",
  gap: 8,
};

const titleStyle: React.CSSProperties = {
  fontSize: 14,
  fontWeight: 700,
  color: "#334155",
};

const subStyle: React.CSSProperties = {
  fontSize: 12,
  color: "#64748b",
  minHeight: 30,
};

const valueBox: React.CSSProperties = {
  padding: "10px 0",
  textAlign: "center",
  fontSize: 18,
  fontWeight: 800,
  color: "#1e293b",
  background: "#ffffff",
  border: "2px solid #cbd5e1",
  borderRadius: 12,
};

const btnStyle: React.CSSProperties = {
  padding: "8px 12px",
  background: "#6366f1",
  color: "#ffffff",
  fontSize: 13,
  fontWeight: 600,
  border: "none",
  borderRadius: 10,
  cursor: "pointer",
};

const btnGhost: React.CSSProperties = {
  padding: "8px 12px",
  background: "#ffffff",
  color: "#6366f1",
  fontSize: 13,
  fontWeight: 600,
  border: "2px solid #6366f1",
  borderRadius: 10,
  cursor: "pointer",
};
```

## Hook4_useContext.tsx（useContext）

```tsx
/**
 * Hook4_useContext.tsx — useContext（値の受け渡し構造）
 *
 * ■ ここは論点が違う（memo/useMemo とは別カテゴリ）
 *   ・memo    … 子を再レンダーしないか
 *   ・useMemo … 重い計算をやり直さないか
 *   ・useContext … “値をどう届けるか” の構造の話（性能最適化ではない）
 *   よってビジュアルも「光らせ」ではなく構造図にする。
 *
 * ■ なぜ useContext が要るのか（動機づけ：まず「なぜ」）
 *   深くネストしたコンポーネントに値を渡すとき、間の層が自分では使わないのに
 *   props を下へ下へと素通しさせる —— これが「バケツリレー（prop drilling）」。
 *   層が増えるほど、関係ない中間コンポーネントが props で汚れていく。
 *   useContext は、Provider で配った値を、間を飛ばして必要な所が“直接”読める。
 *
 * ■ 画面で見ること（App → MiddleA → MiddleB → Leaf の4階層）
 *   ① バケツリレー：MiddleA も MiddleB も、使わない user を受け取って下へ渡す（橙 = 素通し）
 *   ② useContext ：中間は props なし（空）。Leaf だけが useContext で直接読む
 *   「名前変更」で Leaf の表示が変わるのは両方同じ。違うのは“中間層の汚れ方”。
 */

import React, { useState, useContext, createContext } from "react";

const UserContext = createContext("");

/* ---------- ① prop バケツリレー ---------- */

function LeafDrill({ user }: { user: string }) {
  return (
    <div style={levelBox("#22c55e")}>
      <span style={nameStyle}>Leaf</span>
      <div style={greeting}>こんにちは、{user} さん</div>
    </div>
  );
}

function MiddleBDrill({ user }: { user: string }) {
  return (
    <div style={levelBox("#94a3b8")}>
      <div style={header}>
        <span style={nameStyle}>MiddleB</span>
        <span style={chipRelay}>user を素通し</span>
      </div>
      <LeafDrill user={user} />
    </div>
  );
}

function MiddleADrill({ user }: { user: string }) {
  return (
    <div style={levelBox("#94a3b8")}>
      <div style={header}>
        <span style={nameStyle}>MiddleA</span>
        <span style={chipRelay}>user を素通し</span>
      </div>
      <MiddleBDrill user={user} />
    </div>
  );
}

function ColDrill() {
  const [user, setUser] = useState("Sato");
  return (
    <div style={colBox}>
      <span style={titleStyle}>① prop バケツリレー</span>
      <span style={subStyle}>中間層が user を素通しさせられる</span>
      <button style={btnStyle} onClick={() => setUser((u) => (u === "Sato" ? "Suzuki" : "Sato"))}>
        名前変更
      </button>
      <div style={levelBox("#6366f1")}>
        <div style={header}>
          <span style={nameStyle}>App</span>
          <span style={chipSource}>user = "{user}"</span>
        </div>
        <MiddleADrill user={user} />
      </div>
    </div>
  );
}

/* ---------- ② useContext ---------- */

function LeafCtx() {
  const user = useContext(UserContext); // 間を飛ばして直接読む
  return (
    <div style={levelBox("#22c55e")}>
      <div style={header}>
        <span style={nameStyle}>Leaf</span>
        <span style={chipRead}>useContext</span>
      </div>
      <div style={greeting}>こんにちは、{user} さん</div>
    </div>
  );
}

function MiddleBCtx() {
  return (
    <div style={levelBox("#94a3b8")}>
      <div style={header}>
        <span style={nameStyle}>MiddleB</span>
        <span style={chipEmpty}>props なし</span>
      </div>
      <LeafCtx />
    </div>
  );
}

function MiddleACtx() {
  return (
    <div style={levelBox("#94a3b8")}>
      <div style={header}>
        <span style={nameStyle}>MiddleA</span>
        <span style={chipEmpty}>props なし</span>
      </div>
      <MiddleBCtx />
    </div>
  );
}

function ColCtx() {
  const [user, setUser] = useState("Sato");
  return (
    <div style={colBox}>
      <span style={titleStyle}>② useContext</span>
      <span style={subStyle}>中間は空・Leaf が直接読む</span>
      <button style={btnStyle} onClick={() => setUser((u) => (u === "Sato" ? "Suzuki" : "Sato"))}>
        名前変更
      </button>
      <UserContext.Provider value={user}>
        <div style={levelBox("#6366f1")}>
          <div style={header}>
            <span style={nameStyle}>App + Provider</span>
            <span style={chipSource}>value = "{user}"</span>
          </div>
          <MiddleACtx />
        </div>
      </UserContext.Provider>
    </div>
  );
}

export default function App() {
  return (
    <div style={screenStyle}>
      <ColDrill />
      <ColCtx />
    </div>
  );
}

/* ===== 以下はスタイル定義（教えたいロジックは上、見た目は下） ===== */

const screenStyle: React.CSSProperties = {
  minHeight: "100vh",
  display: "flex",
  flexWrap: "wrap",
  alignItems: "flex-start",
  justifyContent: "center",
  gap: 24,
  background: "#f8fafc",
  padding: 24,
};

const colBox: React.CSSProperties = {
  width: 260,
  display: "flex",
  flexDirection: "column",
  gap: 8,
};

const titleStyle: React.CSSProperties = {
  fontSize: 14,
  fontWeight: 700,
  color: "#334155",
};

const subStyle: React.CSSProperties = {
  fontSize: 12,
  color: "#64748b",
};

// ネストした階層の枠（枠線色でコンポーネント種別を表す）
function levelBox(color: string): React.CSSProperties {
  return {
    marginTop: 8,
    padding: 12,
    background: "#ffffff",
    border: "2px solid " + color,
    borderRadius: 12,
    display: "flex",
    flexDirection: "column",
    gap: 6,
  };
}

const header: React.CSSProperties = {
  display: "flex",
  alignItems: "center",
  justifyContent: "space-between",
  gap: 8,
};

const nameStyle: React.CSSProperties = {
  fontSize: 12,
  fontWeight: 700,
  color: "#475569",
};

const greeting: React.CSSProperties = {
  fontSize: 13,
  fontWeight: 600,
  color: "#166534",
};

// 橙チップ＝使わないのに素通しさせられる props（ノイズ）
const chipRelay: React.CSSProperties = {
  fontSize: 11,
  fontWeight: 700,
  color: "#ffffff",
  background: "#f59e0b",
  padding: "2px 8px",
  borderRadius: 999,
};

// 空チップ＝props なし（きれい）
const chipEmpty: React.CSSProperties = {
  fontSize: 11,
  fontWeight: 700,
  color: "#64748b",
  background: "#e2e8f0",
  padding: "2px 8px",
  borderRadius: 999,
};

// 供給元
const chipSource: React.CSSProperties = {
  fontSize: 11,
  fontWeight: 700,
  color: "#ffffff",
  background: "#6366f1",
  padding: "2px 8px",
  borderRadius: 999,
};

// 直接読む
const chipRead: React.CSSProperties = {
  fontSize: 11,
  fontWeight: 700,
  color: "#ffffff",
  background: "#22c55e",
  padding: "2px 8px",
  borderRadius: 999,
};

const btnStyle: React.CSSProperties = {
  alignSelf: "flex-start",
  padding: "8px 16px",
  background: "#6366f1",
  color: "#ffffff",
  fontSize: 13,
  fontWeight: 600,
  border: "none",
  borderRadius: 10,
  cursor: "pointer",
};
```

## Hook5_useRef.tsx（useRef）

```tsx
/**
 * Hook5_useRef.tsx — useRef（再レンダーを起こさず値を保持する）
 *
 * ■ ここも論点が別
 *   useState … 変更すると必ず再レンダーする。画面に出す値に使う。
 *   useRef   … .current を書き換えても再レンダーしない。でも値は保持される。
 *              「裏で持っておきたい値」「前回の値の記憶」「DOM 参照」などに使う。
 *
 * ■ なぜ useRef が要るのか（動機づけ：まず「なぜ」）
 *   再レンダーをまたいで値を覚えておきたいが、変更のたびに再描画はしたくない、
 *   という場面がある。普通の変数は再レンダーで毎回 0 に戻るので使えない。
 *   かといって useState にすると、値を変えるたびに再レンダーが走ってしまう。
 *   useRef はその中間 —— 値は保持、変更しても再レンダーは起こさない。
 *
 * ■ 画面で見ること（青 = 再レンダーが起きた印）
 *   ① useState：「+1」→ 画面の数字が即増える＋青く光る（再レンダー）
 *   ② useRef  ：「+1」→ 画面の数字は動かない・光らない（再レンダーしていない）
 *               でも裏では ref.current が増えている（console 参照）。
 *               「再レンダー」を押すと、貯まっていた値が一気に画面へ反映される。
 *
 *   （余談：この教材の“光らせ”＝useFlash も、DOM をつかむのに useRef を使っている。
 *     値の保持だけでなく DOM 参照も useRef の用途。DOM 参照は別途扱う。）
 */

import React, { useState, useRef, useEffect } from "react";

// 青：コンポーネントが再レンダー（commit）するたびに枠を光らせる。
function useRenderFlash() {
  const ref = useRef<HTMLDivElement>(null);
  useEffect(() => {
    const el = ref.current;
    if (!el) return;
    el.style.boxShadow = "0 0 0 5px #6366f166";
    const id = setTimeout(() => {
      if (ref.current) ref.current.style.boxShadow = "0 0 0 0 transparent";
    }, 400);
    return () => clearTimeout(id);
  });
  return ref;
}

// ① useState：値を変えると再レンダーし、画面も更新される
function ColState() {
  const [count, setCount] = useState(0);
  const boxRef = useRenderFlash();
  return (
    <div ref={boxRef} style={colBox}>
      <span style={titleStyle}>① useState</span>
      <span style={subStyle}>変更 → 再レンダー → 画面も更新</span>
      <div style={valueBox}>{count}</div>
      <button style={btnStyle} onClick={() => setCount((c) => c + 1)}>+1</button>
    </div>
  );
}

// ② useRef：値は変わるが再レンダーしない → 画面は据え置き
function ColRef() {
  const countRef = useRef(0);
  const [, force] = useState(0); // 再レンダーを起こすためだけの state
  const boxRef = useRenderFlash();
  return (
    <div ref={boxRef} style={colBox}>
      <span style={titleStyle}>② useRef</span>
      <span style={subStyle}>変更しても再レンダーしない → 画面は据え置き</span>
      <div style={valueBox}>{countRef.current}</div>
      <button
        style={btnStyle}
        onClick={() => {
          countRef.current += 1; // 値は増える（保持される）が、再レンダーは起きない
          console.log("ref.current =", countRef.current);
        }}
      >
        +1（画面は変わらない）
      </button>
      <button style={btnGhost} onClick={() => force((n) => n + 1)}>
        再レンダー（今の値を反映）
      </button>
    </div>
  );
}

export default function App() {
  return (
    <div style={screenStyle}>
      <ColState />
      <ColRef />
    </div>
  );
}

/* ===== 以下はスタイル定義（教えたいロジックは上、見た目は下） ===== */

const screenStyle: React.CSSProperties = {
  minHeight: "100vh",
  display: "flex",
  flexWrap: "wrap",
  alignItems: "flex-start",
  justifyContent: "center",
  gap: 20,
  background: "#f8fafc",
  padding: 24,
};

const colBox: React.CSSProperties = {
  width: 210,
  padding: 16,
  background: "#eef2ff",
  border: "2px solid #6366f1",
  borderRadius: 16,
  display: "flex",
  flexDirection: "column",
  gap: 10,
  transition: "box-shadow 0.35s ease",
};

const titleStyle: React.CSSProperties = {
  fontSize: 14,
  fontWeight: 700,
  color: "#334155",
};

const subStyle: React.CSSProperties = {
  fontSize: 12,
  color: "#64748b",
  minHeight: 32,
};

// 画面に出る値（大きく表示して「動く/動かない」を分かりやすく）
const valueBox: React.CSSProperties = {
  padding: "12px 0",
  textAlign: "center",
  fontSize: 40,
  fontWeight: 800,
  color: "#1e293b",
  background: "#ffffff",
  border: "2px solid #cbd5e1",
  borderRadius: 12,
};

const btnStyle: React.CSSProperties = {
  padding: "8px 0",
  background: "#6366f1",
  color: "#ffffff",
  fontSize: 13,
  fontWeight: 600,
  border: "none",
  borderRadius: 10,
  cursor: "pointer",
};

const btnGhost: React.CSSProperties = {
  padding: "8px 0",
  background: "#ffffff",
  color: "#6366f1",
  fontSize: 13,
  fontWeight: 600,
  border: "2px solid #6366f1",
  borderRadius: 10,
  cursor: "pointer",
};
```

## Hook6_useEffect.tsx（useEffect）

```tsx
/**
 * Hook6_useEffect.tsx — useEffect（実行タイミングを見る）
 *
 * ■ ここの主役は「タイミング（順序）」
 *   useEffect は、レンダー中にやってはいけない“外の世界との同期”
 *   （データ取得・購読・タイマー・DOM 操作など）を、
 *   画面が更新された“後”に安全に行うための仕組み。
 *
 *   実行の順序：
 *     ① レンダー（JSX を計算）
 *     ② コミット（画面の DOM が更新される）
 *     ③ effect 実行  ← 必ず ② の後
 *   依存配列が「次にいつ再実行するか」を決め、
 *   cleanup は「次の effect の前／アンマウント時」に走る。
 *
 * ■ 観察のしかた：ブラウザの console を開いて、出力される順序を読む。
 *   ・マウント          → コミット → effect 実行
 *   ・dep 変更          → コミット → cleanup(旧) → effect(新)
 *   ・無関係な再レンダー → コミットは出るが effect は出ない（毎回ではない）
 *   ・アンマウント        → cleanup だけ（後片付け）
 *   （青い光＝コミットが起きた印。effect はその後に走るのが console で分かる）
 *
 * ■ 注意：開発時 StrictMode では、マウント時に effect が意図的に2回走る
 *   （effect→cleanup→effect）。console が二重に見えたらそれが原因。
 */

import React, { useState, useRef, useEffect, useLayoutEffect } from "react";

// 計測用：コミットのたびに console へ記録し、枠を青く光らせる。
// useLayoutEffect はコミット直後（effect より前）に走るので、
// console でも「コミット → effect」の順序が正しく並ぶ。
function useCommitMarker(boxRef: React.RefObject<HTMLDivElement>) {
  useLayoutEffect(() => {
    console.log("コミット（画面が更新された）");
    const el = boxRef.current;
    if (!el) return;
    el.style.boxShadow = "0 0 0 5px #6366f166";
    const id = setTimeout(() => {
      if (boxRef.current) boxRef.current.style.boxShadow = "0 0 0 0 transparent";
    }, 400);
    return () => clearTimeout(id);
  });
}

function Child() {
  const [dep, setDep] = useState(0);
  const [, force] = useState(0); // 再レンダーを起こすためだけの state（dep は変えない）
  const boxRef = useRef<HTMLDivElement>(null);

  useCommitMarker(boxRef); // 計測用

  // ★ 今回の主役：この useEffect の実行タイミングを console で観察する
  useEffect(() => {
    console.log("effect 実行 dep=" + dep);
    return () => {
      console.log("cleanup dep=" + dep);
    };
  }, [dep]); // ← この依存配列が「いつ再実行するか」を決める

  return (
    <div ref={boxRef} style={childBox}>
      <span style={tagStyle}>Child</span>
      <div style={depBox}>dep: {dep}</div>
      <div style={btnRow}>
        <button style={btnStyle} onClick={() => setDep((d) => d + 1)}>dep 変更</button>
        <button style={btnGhost} onClick={() => force((n) => n + 1)}>無関係な再レンダー</button>
      </div>
    </div>
  );
}

export default function App() {
  const [mounted, setMounted] = useState(true);
  return (
    <div style={screenStyle}>
      <span style={hintStyle}>操作して console を見る（実行順が出ます）</span>
      <button style={btnStyle} onClick={() => setMounted((m) => !m)}>
        {mounted ? "アンマウント" : "マウント"}
      </button>
      {mounted ? <Child /> : <div style={placeholder}>Child はアンマウント中</div>}
    </div>
  );
}

/* ===== 以下はスタイル定義（教えたいロジックは上、見た目は下） ===== */

const screenStyle: React.CSSProperties = {
  minHeight: "100vh",
  display: "flex",
  flexDirection: "column",
  alignItems: "center",
  justifyContent: "center",
  gap: 12,
  background: "#f8fafc",
  padding: 24,
};

const hintStyle: React.CSSProperties = {
  fontSize: 12,
  color: "#64748b",
};

// state を持つ子（青）
const childBox: React.CSSProperties = {
  width: 220,
  padding: 16,
  background: "#eef2ff",
  border: "2px solid #6366f1",
  borderRadius: 16,
  display: "flex",
  flexDirection: "column",
  gap: 12,
  transition: "box-shadow 0.35s ease",
};

const placeholder: React.CSSProperties = {
  width: 220,
  padding: 24,
  textAlign: "center",
  color: "#94a3b8",
  fontSize: 13,
  background: "#f1f5f9",
  border: "2px dashed #cbd5e1",
  borderRadius: 16,
};

const tagStyle: React.CSSProperties = {
  fontSize: 12,
  fontWeight: 700,
  color: "#475569",
};

const depBox: React.CSSProperties = {
  padding: "8px 0",
  textAlign: "center",
  fontSize: 28,
  fontWeight: 800,
  color: "#1e293b",
  background: "#ffffff",
  border: "2px solid #cbd5e1",
  borderRadius: 12,
};

const btnRow: React.CSSProperties = {
  display: "flex",
  flexDirection: "column",
  gap: 8,
};

const btnStyle: React.CSSProperties = {
  padding: "8px 12px",
  background: "#6366f1",
  color: "#ffffff",
  fontSize: 13,
  fontWeight: 600,
  border: "none",
  borderRadius: 10,
  cursor: "pointer",
};

const btnGhost: React.CSSProperties = {
  padding: "8px 12px",
  background: "#ffffff",
  color: "#6366f1",
  fontSize: 13,
  fontWeight: 600,
  border: "2px solid #6366f1",
  borderRadius: 10,
  cursor: "pointer",
};
```
