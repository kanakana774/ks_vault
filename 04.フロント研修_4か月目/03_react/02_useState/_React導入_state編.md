# React導入（state編）― TS経験者向け

> 静的編（部品・Props・リスト・条件・children）の続き。
> ここでは **「押したら変わる」** を扱う。
> 一本のストーリー：**変わる値を state で持ち → 変わったら描き直され → その state を"最小限で素直な形"に設計する**。

## 全体像

| 章 | テーマ | 一言 |
|---|---|---|
| 0 | 導入 | 素の変数だと画面は変わらない（＝宣言的UIの動機） |
| 1 | useState | 変わる値を Reactに覚えさせる |
| 2 | イベントハンドラ ＋ スナップショット / 関数型更新 | イベント→setState→再描画。stateはその場の写真 |
| 3 | Render・Commit（＋key回収・keyリセット） | setStateで関数が再実行され差分だけ反映 |
| 前提 | イミュータブル更新 | 直接書き換えず、新しく作る |
| 4 | stateの構造の選択（アンチパターン5） | stateは最小限で素直な形に |
| ＋ | stateのリフトアップ | 共有する値は共通の親へ |
| 5 | まとめ＆演習 | 判断チェックリスト |

※コードはすべて `.tsx` ／インラインstyle（ライブラリ非依存）。


---

# 第4章：stateの構造の選択（アンチパターン5）

> stateは「**最小限で、素直な形**」がいい。各パターンを「症状 → なぜ悪い → 直し方」で見る。


---


## 0. 導入：静的 → 動的（宣言的UI）

静的編では画面は「渡された値を表示するだけ」だった。実アプリはボタンで数が増える。初学者が最初にやる「素のローカル変数を書き換える」は **動かない** ——この失敗を先に見せると useState の必要性が腹落ちする。

**なぜ動かないか**：Reactは「関数を呼び直して」画面を作るが、ただの変数を書き換えても "描き直すきっかけ" を知らない。しかも呼び直されても `let count = 0` で毎回リセットされる。だから2つ必要 ——「値を覚えておく」「変わったら描き直す」。

**宣言的UI**：素のJSは画面を直接いじる（命令的）。Reactは状態を宣言し、画面はその結果（宣言的）。だから "画面を書き換える" のではなく "状態を変える"。


![## 0. 導入：静的 → 動的（宣言的UI）](State0_why_no_rerender.svg)


```tsx
import React from "react";

// ───────────────────────────────────────────
// 【わざと動かない例】
// 素のローカル変数 count を書き換えても、画面は変わらない。
//   ・変数は増える（console には 1, 2, 3... と出る）
//   ・でも画面は 0 のまま
//
// なぜ？
//   Reactは「関数をもう一度呼んで」画面を作り直す。
//   でも、ただの変数を書き換えても Reactは "描き直すきっかけ" を知らない。
//   しかも仮に呼び直されても、let count = 0 で毎回リセットされてしまう。
//
// → 必要なのは2つ：
//     ① 値を再レンダーをまたいで「覚えておく」
//     ② 値が変わったら「描き直す」きっかけ
//   これを与えるのが useState（次章）。
// ───────────────────────────────────────────
function Counter() {
  let count = 0; // ただのローカル変数

  const handleClick = () => {
    count = count + 1; // 変数は増えるが…
    console.log("count =", count); // console には増えて見える
    // …画面は 0 のまま！
  };

  return (
    <div style={cardStyle}>
      <p style={numberStyle}>{count}</p>
      <button style={buttonStyle} onClick={handleClick}>
        +1
      </button>
    </div>
  );
}

export default function App() {
  return (
    <div style={screenStyle}>
      <Counter />
    </div>
  );
}

// ── 見た目（インラインstyle） ────────────────
const screenStyle: React.CSSProperties = {
  minHeight: "100vh",
  display: "flex",
  alignItems: "center",
  justifyContent: "center",
  background: "#f8fafc",
};

const cardStyle: React.CSSProperties = {
  display: "flex",
  flexDirection: "column",
  alignItems: "center",
  gap: 16,
  padding: 32,
  background: "#ffffff",
  border: "1px solid #e2e8f0",
  borderRadius: 16,
  boxShadow: "0 1px 3px rgba(0,0,0,0.08)",
};

const numberStyle: React.CSSProperties = {
  margin: 0,
  fontSize: 48,
  fontWeight: 700,
  color: "#1e293b",
};

const buttonStyle: React.CSSProperties = {
  padding: "10px 28px",
  background: "#6366f1",
  color: "#ffffff",
  fontSize: 16,
  fontWeight: 600,
  border: "none",
  borderRadius: 10,
  cursor: "pointer",
};
```


## 1. useState

前章の2つの必要を `useState` が両方満たす。変わったのは3か所（import・`let`→`useState`・`count=count+1`→`setCount(...)`）だけ。

- `count` … 今の値（画面に表示）
- `setCount` … 更新関数（呼ぶと「値を変える＋再レンダー」）
- `useState(0)` … 初期値（最初のレンダーだけ）

TS補足：`const [count, setCount]` は **配列の分割代入**。型は `useState(0)` から `number` に推論される。


![## 1. useState](State1_useState_anatomy.svg)


```tsx
import React, { useState } from "react";

// ───────────────────────────────────────────
// 前章の「動かないカウンター」を useState で直す。
//
//   const [count, setCount] = useState(0);
//         └値┘  └更新関数┘        └初期値┘
//
// useState は「フック（Hook）」= 関数コンポーネントで
// state などReactの機能を使うための関数。
//
//   ・count    … 今の値（画面に表示する）
//   ・setCount … 更新関数。呼ぶと「値を変える＋再レンダー」
//   ・useState(0) … 初期値。最初のレンダーのときだけ使われる
//
// これで①値を覚えておく ②変わったら描き直す の両方を満たす。
// ───────────────────────────────────────────
function Counter() {
  const [count, setCount] = useState(0);

  const handleClick = () => {
    setCount(count + 1); // 値を変えて、描き直してもらう
  };

  return (
    <div style={cardStyle}>
      <p style={numberStyle}>{count}</p>
      <button style={buttonStyle} onClick={handleClick}>
        +1
      </button>
    </div>
  );
}

export default function App() {
  return (
    <div style={screenStyle}>
      <Counter />
    </div>
  );
}

// ── 見た目（インラインstyle） ────────────────
const screenStyle: React.CSSProperties = {
  minHeight: "100vh",
  display: "flex",
  alignItems: "center",
  justifyContent: "center",
  background: "#f8fafc",
};

const cardStyle: React.CSSProperties = {
  display: "flex",
  flexDirection: "column",
  alignItems: "center",
  gap: 16,
  padding: 32,
  background: "#ffffff",
  border: "1px solid #e2e8f0",
  borderRadius: 16,
  boxShadow: "0 1px 3px rgba(0,0,0,0.08)",
};

const numberStyle: React.CSSProperties = {
  margin: 0,
  fontSize: 48,
  fontWeight: 700,
  color: "#1e293b",
};

const buttonStyle: React.CSSProperties = {
  padding: "10px 28px",
  background: "#6366f1",
  color: "#ffffff",
  fontSize: 16,
  fontWeight: 600,
  border: "none",
  borderRadius: 10,
  cursor: "pointer",
};
```


## 2. イベントハンドラ ＋ スナップショット / 関数型更新

**イベントハンドラ**（出来事に応じて呼ばれる関数）は `onClick={handleClick}` のように **関数を渡す**（`handleClick()` と呼ぶと結果を渡す罠）。

**stateはスナップショット**：1回のレンダー中、stateはずっと同じ値。だから `setCount(count + 1)` を3回書いても、3回とも `count = 0` を見て `setCount(1)` するだけで +1 にしかならない。

**関数型更新** `setCount(c => c + 1)`：`c` に最新の値が渡るので `0→1→2→3` で +3。前の値から次を計算するときに使う。


![## 2. イベントハンドラ ＋ スナップショット / 関数型更新](State2_snapshot.svg)


```tsx
import React, { useState } from "react";

// ───────────────────────────────────────────
// イベントハンドラ（出来事に応じて呼ばれる関数）を onClick に「渡す」。
//   onClick={handleClick}   ○ 関数そのものを渡す
//   onClick={handleClick()} ✕ 呼び出した結果を渡してしまう（罠）
//
// そして state の大事な性質：
//   ・stateは「スナップショット」= 1回のレンダー中はずっと同じ値
//   ・だから同じ計算を重ねても、元の値を見続けてしまう
//   ・「前の値から次を計算」したいときは "関数型更新" を使う
// ───────────────────────────────────────────
function Counter() {
  const [count, setCount] = useState(0);

  // ✕ スナップショットの罠：
  //   count はこのレンダー中ずっと 0。3回とも setCount(0 + 1) になり +1 だけ。
  const addThreeWrong = () => {
    setCount(count + 1);
    setCount(count + 1);
    setCount(count + 1);
  };

  // ○ 関数型更新：
  //   c には「最新の値」が渡る。0→1→2→3 で +3 になる。
  const addThreeRight = () => {
    setCount((c) => c + 1);
    setCount((c) => c + 1);
    setCount((c) => c + 1);
  };

  return (
    <div style={cardStyle}>
      <p style={numberStyle}>{count}</p>
      <div style={rowStyle}>
        <button style={btnStyle} onClick={() => setCount(count + 1)}>
          +1
        </button>
        <button style={ghostStyle} onClick={addThreeWrong}>
          +3（罠）
        </button>
        <button style={btnStyle} onClick={addThreeRight}>
          +3（正）
        </button>
      </div>
    </div>
  );
}

export default function App() {
  return (
    <div style={screenStyle}>
      <Counter />
    </div>
  );
}

// ── 見た目（インラインstyle） ────────────────
const screenStyle: React.CSSProperties = {
  minHeight: "100vh",
  display: "flex",
  alignItems: "center",
  justifyContent: "center",
  background: "#f8fafc",
};

const cardStyle: React.CSSProperties = {
  display: "flex",
  flexDirection: "column",
  alignItems: "center",
  gap: 20,
  padding: 32,
  background: "#ffffff",
  border: "1px solid #e2e8f0",
  borderRadius: 16,
  boxShadow: "0 1px 3px rgba(0,0,0,0.08)",
};

const numberStyle: React.CSSProperties = {
  margin: 0,
  fontSize: 48,
  fontWeight: 700,
  color: "#1e293b",
};

const rowStyle: React.CSSProperties = {
  display: "flex",
  gap: 8,
};

const btnStyle: React.CSSProperties = {
  padding: "10px 18px",
  background: "#6366f1",
  color: "#ffffff",
  fontSize: 15,
  fontWeight: 600,
  border: "none",
  borderRadius: 10,
  cursor: "pointer",
};

const ghostStyle: React.CSSProperties = {
  padding: "10px 18px",
  background: "#ffffff",
  color: "#475569",
  fontSize: 15,
  fontWeight: 600,
  border: "1px solid #cbd5e1",
  borderRadius: 10,
  cursor: "pointer",
};
```


## 3. 再レンダリングの仕組み（Trigger → Render → Commit）

setStateすると3フェーズで画面が更新される（この図は Excalidraw で作成）。

- **Trigger（きっかけ）**：`setCount(1)` が呼ばれる
- **Render（描き直し）**：`Counter()` を呼び直して新しいJSXを作る
- **Commit（反映）**：前回のJSXと見比べ、**変わった所だけ**DOMへ

**key の回収（リスト編の宿題）**：Renderで新旧JSXを照合するとき、`map` で並べた要素の「どれが前回のどれと同じ物か」を見分ける目印が `key`。だからkeyが安定しないと要素を取り違え、入力途中の値や選択状態がズレる。`index` をkeyにすると並び替え・削除で事故るのはこのため（安定したID `user.id` を使う）。


### 3-補足. key で state をリセット / 保持

`key` は「同じ部品か別部品か」の判定にも使われる。key が同じ→state保持、key が違う→別部品として作り直し（リセット）。下は `key={tab}` で、タブ切り替えのたびに入力がリセットされる（keyを外すと前の入力が残る）。


![### 3-補足. key で state をリセット / 保持](State3_key_reset.svg)


```tsx
import React, { useState } from "react";

// ───────────────────────────────────────────
// key のもう一つの顔：state のリセット / 保持
//
//   ・key が同じ → 同じ部品とみなす → state は保持される
//   ・key が違う → 別の部品とみなす → state は作り直される（リセット）
//
// 下では key={tab} にしているので、Aさん↔Bさん を切り替えると
// Form の入力が毎回リセットされる。
// （key を外すと、切り替えても前の入力が残ってしまう）
// ───────────────────────────────────────────
function Form({ label }: { label: string }) {
  const [text, setText] = useState(""); // この state が key でリセットされる

  return (
    <div style={formStyle}>
      <p style={labelStyle}>{label}</p>
      <input
        style={inputStyle}
        value={text}
        onChange={(e) => setText(e.target.value)}
        placeholder="入力してみて"
      />
    </div>
  );
}

export default function App() {
  const [tab, setTab] = useState<"a" | "b">("a");

  return (
    <div style={screenStyle}>
      <div style={cardStyle}>
        <div style={rowStyle}>
          <button style={tabBtn(tab === "a")} onClick={() => setTab("a")}>
            Aさん
          </button>
          <button style={tabBtn(tab === "b")} onClick={() => setTab("b")}>
            Bさん
          </button>
        </div>

        {/* key={tab} → 切り替えると Form が作り直され、入力がリセットされる */}
        <Form
          key={tab}
          label={tab === "a" ? "Aさんへの返信" : "Bさんへの返信"}
        />
      </div>
    </div>
  );
}

// ── 見た目（インラインstyle） ────────────────
const screenStyle: React.CSSProperties = {
  minHeight: "100vh",
  display: "flex",
  alignItems: "center",
  justifyContent: "center",
  background: "#f8fafc",
};

const cardStyle: React.CSSProperties = {
  display: "flex",
  flexDirection: "column",
  gap: 16,
  width: 300,
  padding: 24,
  background: "#ffffff",
  border: "1px solid #e2e8f0",
  borderRadius: 16,
  boxShadow: "0 1px 3px rgba(0,0,0,0.08)",
};

const rowStyle: React.CSSProperties = {
  display: "flex",
  gap: 8,
};

const tabBtn = (active: boolean): React.CSSProperties => ({
  flex: 1,
  padding: "8px 0",
  background: active ? "#6366f1" : "#f1f5f9",
  color: active ? "#ffffff" : "#475569",
  fontSize: 14,
  fontWeight: 600,
  border: "none",
  borderRadius: 8,
  cursor: "pointer",
});

const formStyle: React.CSSProperties = {
  display: "flex",
  flexDirection: "column",
  gap: 6,
};

const labelStyle: React.CSSProperties = {
  margin: 0,
  fontSize: 14,
  fontWeight: 600,
  color: "#1e293b",
};

const inputStyle: React.CSSProperties = {
  padding: "8px 10px",
  fontSize: 14,
  border: "1px solid #cbd5e1",
  borderRadius: 8,
  boxSizing: "border-box",
};
```


```typescript
import React, { useState, memo } from "react";

type Item = { id: number; label: string };

const initial: Item[] = [
  { id: 1, label: "A" },
  { id: 2, label: "B" },
  { id: 3, label: "C" },
];

// memo: props が前回と同じなら再レンダーをスキップする。
// レンダーが走ったら console に出す → 先頭追加時の「件数」で違いが見える。
const Row = memo(function Row({ label }: { label: string }) {
  console.log("render:", label);
  return <li style={rowStyle}>{label}</li>;
});

let seq = 100; // デモ用の簡易ID採番

// ✕ key = index
//   先頭に追加すると、各位置の label がずれる（位置0:A→Z, 位置1:B→A ...）。
//   → 既存 Row の props(label) が全部変わり、全行が再レンダーされる。
//   → console: render Z, render A, render B, render C …（全件）
function BadList() {
  const [items, setItems] = useState(initial);
  const addTop = () => setItems([{ id: ++seq, label: "Z" }, ...items]);

  return (
    <div style={cardStyle}>
      <p style={badTitleStyle}>✕ key = index</p>
      <ul style={ulStyle}>
        {items.map((item, index) => (
          <Row key={index} label={item.label} />
        ))}
      </ul>
      <button style={btnStyle} onClick={addTop}>
        先頭に追加
      </button>
    </div>
  );
}

// ○ key = item.id
//   id で対応するので、既存 Row の props(label) は変わらない。
//   → 追加した1件だけ再レンダー。既存は memo でスキップ。
//   → console: render Z（1件だけ）
function GoodList() {
  const [items, setItems] = useState(initial);
  const addTop = () => setItems([{ id: ++seq, label: "Z" }, ...items]);

  return (
    <div style={cardStyle}>
      <p style={goodTitleStyle}>○ key = item.id</p>
      <ul style={ulStyle}>
        {items.map((item) => (
          <Row key={item.id} label={item.label} />
        ))}
      </ul>
      <button style={btnStyle} onClick={addTop}>
        先頭に追加
      </button>
    </div>
  );
}

export default function App() {
  return (
    <div style={screenStyle}>
      <p style={hintStyle}>
        DevToolsのConsoleを開いて「先頭に追加」を押し、render の件数を比べる
      </p>
      <div style={rowWrapStyle}>
        <BadList />
        <GoodList />
      </div>
    </div>
  );
}

// ── 見た目（インラインstyle） ────────────────
const screenStyle: React.CSSProperties = {
  minHeight: "100vh",
  display: "flex",
  flexDirection: "column",
  alignItems: "center",
  justifyContent: "center",
  gap: 16,
  background: "#f8fafc",
  padding: 24,
};

const hintStyle: React.CSSProperties = {
  margin: 0,
  fontSize: 13,
  color: "#64748b",
};

const rowWrapStyle: React.CSSProperties = {
  display: "flex",
  gap: 16,
};

const cardStyle: React.CSSProperties = {
  width: 180,
  padding: 20,
  background: "#ffffff",
  border: "1px solid #e2e8f0",
  borderRadius: 16,
  boxShadow: "0 1px 3px rgba(0,0,0,0.08)",
};

const badTitleStyle: React.CSSProperties = {
  margin: "0 0 8px",
  fontSize: 14,
  fontWeight: 700,
  fontFamily: "monospace",
  color: "#dc2626",
};

const goodTitleStyle: React.CSSProperties = {
  margin: "0 0 8px",
  fontSize: 14,
  fontWeight: 700,
  fontFamily: "monospace",
  color: "#16a34a",
};

const ulStyle: React.CSSProperties = {
  margin: 0,
  padding: 0,
  listStyleType: "none",
  display: "flex",
  flexDirection: "column",
  gap: 6,
};

const rowStyle: React.CSSProperties = {
  padding: "6px 12px",
  fontSize: 14,
  background: "#f1f5f9",
  borderRadius: 8,
  color: "#334155",
};

const btnStyle: React.CSSProperties = {
  marginTop: 12,
  width: "100%",
  padding: "8px 0",
  background: "#6366f1",
  color: "#ffffff",
  fontSize: 13,
  fontWeight: 600,
  border: "none",
  borderRadius: 8,
  cursor: "pointer",
};

```

```typescript
import React, { useState } from "react";

type Item = { id: number; label: string };

const initial: Item[] = [
  { id: 1, label: "りんご" },
  { id: 2, label: "みかん" },
  { id: 3, label: "ぶどう" },
];

// 各行は「行ローカルの状態（チェック）」を持つ。
function Row({ label }: { label: string }) {
  const [checked, setChecked] = useState(false);
  return (
    <label style={rowStyle}>
      <input
        type="checkbox"
        checked={checked}
        onChange={(e) => setChecked(e.target.checked)}
      />
      {label}
    </label>
  );
}

// ✕ key に index を使う
//   先頭に追加すると index がずれ、Reactが「同じ index = 同じ行」とみなす。
//   → チェック状態が別の項目に残ってしまう。
function BadList() {
  const [items, setItems] = useState(initial);
  const addTop = () => setItems([{ id: Date.now(), label: "新item" }, ...items]);

  return (
    <div style={cardStyle}>
      <p style={badTitleStyle}>✕ key = index</p>
      {items.map((item, index) => (
        <Row key={index} label={item.label} />
      ))}
      <button style={btnStyle} onClick={addTop}>
        先頭に追加
      </button>
    </div>
  );
}

// ○ key に id（安定したID）を使う
//   位置が変わっても id で行を追跡できるので、状態が正しく追従する。
function GoodList() {
  const [items, setItems] = useState(initial);
  const addTop = () => setItems([{ id: Date.now(), label: "新item" }, ...items]);

  return (
    <div style={cardStyle}>
      <p style={goodTitleStyle}>○ key = item.id</p>
      {items.map((item) => (
        <Row key={item.id} label={item.label} />
      ))}
      <button style={btnStyle} onClick={addTop}>
        先頭に追加
      </button>
    </div>
  );
}

export default function App() {
  return (
    <div style={screenStyle}>
      <p style={hintStyle}>
        「りんご」にチェック → 「先頭に追加」を押して見比べる
      </p>
      <div style={rowWrapStyle}>
        <BadList />
        <GoodList />
      </div>
    </div>
  );
}

// ── 見た目（インラインstyle） ────────────────
const screenStyle: React.CSSProperties = {
  minHeight: "100vh",
  display: "flex",
  flexDirection: "column",
  alignItems: "center",
  justifyContent: "center",
  gap: 16,
  background: "#f8fafc",
  padding: 24,
};

const hintStyle: React.CSSProperties = {
  margin: 0,
  fontSize: 13,
  color: "#64748b",
};

const rowWrapStyle: React.CSSProperties = {
  display: "flex",
  gap: 16,
};

const cardStyle: React.CSSProperties = {
  display: "flex",
  flexDirection: "column",
  gap: 8,
  width: 200,
  padding: 20,
  background: "#ffffff",
  border: "1px solid #e2e8f0",
  borderRadius: 16,
  boxShadow: "0 1px 3px rgba(0,0,0,0.08)",
};

const badTitleStyle: React.CSSProperties = {
  margin: 0,
  fontSize: 14,
  fontWeight: 700,
  fontFamily: "monospace",
  color: "#dc2626",
};

const goodTitleStyle: React.CSSProperties = {
  margin: 0,
  fontSize: 14,
  fontWeight: 700,
  fontFamily: "monospace",
  color: "#16a34a",
};

const rowStyle: React.CSSProperties = {
  display: "flex",
  alignItems: "center",
  gap: 8,
  fontSize: 14,
  color: "#334155",
};

const btnStyle: React.CSSProperties = {
  marginTop: 4,
  padding: "8px 0",
  background: "#6366f1",
  color: "#ffffff",
  fontSize: 13,
  fontWeight: 600,
  border: "none",
  borderRadius: 8,
  cursor: "pointer",
};

```

## 前提. オブジェクト/配列のイミュータブル更新

アンチパターンの「直し方」を書くための土台。**イミュータブル＝不変**。stateを直接書き換えず、新しく作り直す。

Reactは「参照が変わったか」で更新を判断する。同じオブジェクトを直接書き換えると参照が同じ→気づかない（画面が変わらない）。新しく作れば参照が変わる→再レンダーされる。

配列の型：

| 操作 | ✕ 元を書き換え | ○ 新しく作る |
|---|---|---|
| 追加 | `arr.push(x)` | `[...arr, x]` |
| 削除 | `arr.splice(i,1)` | `arr.filter(a => a.id !== id)` |
| 変更 | `arr[i] = x` | `arr.map(a => a.id === id ? {...a, ...} : a)` |

オブジェクトは `{...obj, key: 値}`。ネストは各層をコピー（この面倒さが④⑤の動機）。


![## 前提. オブジェクト/配列のイミュータブル更新](State_immutable_ref.svg)


```tsx
import React, { useState } from "react";

// ───────────────────────────────────────────
// イミュータブル更新（不変＝直接書き換えず、新しく作り直す）
//
// Reactは「参照が変わったか」で更新を判断する。
//   ・同じオブジェクトを直接書き換える → 参照が同じ → 気づかない（画面が変わらない）
//   ・新しいオブジェクトを作る         → 参照が変わる → 気づく（再レンダー）
// ───────────────────────────────────────────
function Profile() {
  const [user, setUser] = useState({ name: "田中 太郎", age: 28 });

  // ✕ 直接書き換え：同じ参照のまま。Reactが気づかず画面が更新されない。
  const badBirthday = () => {
    user.age = user.age + 1;
    setUser(user);
  };

  // ○ 新しいオブジェクトを作る（スプレッドでコピー＋一部だけ上書き）。
  const goodBirthday = () => {
    setUser({ ...user, age: user.age + 1 });
  };

  return (
    <div style={cardStyle}>
      <p style={nameStyle}>{user.name}</p>
      <p style={ageStyle}>{user.age} 歳</p>
      <div style={rowStyle}>
        <button style={ghostStyle} onClick={badBirthday}>
          誕生日（✕直接）
        </button>
        <button style={btnStyle} onClick={goodBirthday}>
          誕生日（○新しく）
        </button>
      </div>
    </div>
  );
}

export default function App() {
  return (
    <div style={screenStyle}>
      <Profile />
    </div>
  );
}

// ── 見た目（インラインstyle） ────────────────
const screenStyle: React.CSSProperties = {
  minHeight: "100vh",
  display: "flex",
  alignItems: "center",
  justifyContent: "center",
  background: "#f8fafc",
};

const cardStyle: React.CSSProperties = {
  display: "flex",
  flexDirection: "column",
  alignItems: "center",
  gap: 8,
  padding: 32,
  background: "#ffffff",
  border: "1px solid #e2e8f0",
  borderRadius: 16,
  boxShadow: "0 1px 3px rgba(0,0,0,0.08)",
};

const nameStyle: React.CSSProperties = {
  margin: 0,
  fontSize: 20,
  fontWeight: 700,
  color: "#1e293b",
};

const ageStyle: React.CSSProperties = {
  margin: "0 0 12px",
  fontSize: 16,
  color: "#64748b",
};

const rowStyle: React.CSSProperties = {
  display: "flex",
  gap: 8,
};

const btnStyle: React.CSSProperties = {
  padding: "10px 16px",
  background: "#6366f1",
  color: "#ffffff",
  fontSize: 14,
  fontWeight: 600,
  border: "none",
  borderRadius: 10,
  cursor: "pointer",
};

const ghostStyle: React.CSSProperties = {
  padding: "10px 16px",
  background: "#ffffff",
  color: "#475569",
  fontSize: 14,
  fontWeight: 600,
  border: "1px solid #cbd5e1",
  borderRadius: 10,
  cursor: "pointer",
};
```


## ① 関連する値は、1つにまとめる

**症状**：`x`,`y` のように常に一緒に更新するstateが別々。**なぜ悪い**：片方だけ更新する事故／関連が見えない。**直し方**：1つのオブジェクトに（`position: {x, y}`）。

見分け方：2つのstateをいつも同時に更新している。


![## ① 関連する値は、1つにまとめる](State4_1_group.svg)


```tsx
import React, { useState } from "react";

// ───────────────────────────────────────────
// ① 関連する値は、1つのオブジェクトにまとめる
//
// ✕ バラバラ（常に一緒に更新するのに別々）
//     const [x, setX] = useState(0);
//     const [y, setY] = useState(0);
//     // 更新のたびに setX と setY の両方を書く必要がある（片方忘れの事故）
//
// ○ まとめる
//     const [position, setPosition] = useState({ x: 0, y: 0 });
//     // 常にセットで更新。意図も明確
// ───────────────────────────────────────────
function DotArea() {
  const [position, setPosition] = useState({ x: 160, y: 120 });

  const handleMove = (e: React.PointerEvent<HTMLDivElement>) => {
    // x と y をまとめて新しいオブジェクトで更新
    setPosition({ x: e.nativeEvent.offsetX, y: e.nativeEvent.offsetY });
  };

  return (
    <div style={areaStyle} onPointerMove={handleMove}>
      <div style={{ ...dotStyle, left: position.x, top: position.y }} />
      <span style={labelStyle}>
        x: {Math.round(position.x)} / y: {Math.round(position.y)}
      </span>
    </div>
  );
}

export default function App() {
  return (
    <div style={screenStyle}>
      <DotArea />
    </div>
  );
}

// ── 見た目（インラインstyle） ────────────────
const screenStyle: React.CSSProperties = {
  minHeight: "100vh",
  display: "flex",
  alignItems: "center",
  justifyContent: "center",
  background: "#f8fafc",
};

const areaStyle: React.CSSProperties = {
  position: "relative",
  width: 320,
  height: 240,
  background: "#ffffff",
  border: "1px solid #e2e8f0",
  borderRadius: 16,
  boxShadow: "0 1px 3px rgba(0,0,0,0.08)",
  overflow: "hidden",
  cursor: "crosshair",
};

const dotStyle: React.CSSProperties = {
  position: "absolute",
  width: 24,
  height: 24,
  borderRadius: "50%",
  background: "#6366f1",
  transform: "translate(-50%, -50%)",
  pointerEvents: "none",
};

const labelStyle: React.CSSProperties = {
  position: "absolute",
  left: 12,
  bottom: 10,
  fontSize: 13,
  color: "#64748b",
  pointerEvents: "none",
};
```


## ② 矛盾する状態を作らない

**症状**：boolフラグが複数あり、あり得ない組み合わせ（赤と青が同時点灯）が作れる。**なぜ悪い**：矛盾状態がバグの温床。**直し方**：取り得る状態を1つの変数に列挙（`light: "red" | "yellow" | "green"`）。

TS補足：`"red" | "yellow" | "green"` は **ユニオン型**。タイプミスもコンパイラが弾く。
見分け方：同時に true になってはいけないフラグが複数ある。


![## ② 矛盾する状態を作らない](State4_2_status.svg)


```tsx
import React, { useState } from "react";

// ───────────────────────────────────────────
// ② 矛盾する状態を作らない（信号機）
//
// ✕ 3つの bool フラグ
//     const [isRed, setIsRed]     = useState(true);
//     const [isYellow, setIsYellow] = useState(false);
//     const [isGreen, setIsGreen]   = useState(false);
//     // isRed=true かつ isGreen=true =「赤と青が同時に光る」
//     // あり得ない状態を作れてしまう
//
// ○ 取り得る状態を1つに列挙（ユニオン型 = 複数候補のどれか、という型）
//     const [light, setLight] = useState<"red" | "yellow" | "green">("red");
//     // 常に1色。赤と青が同時、は表現できない
// ───────────────────────────────────────────
function TrafficLight() {
  const [light, setLight] = useState<"red" | "yellow" | "green">("red");

  return (
    <div style={cardStyle}>
      <div style={boardStyle}>
        <div style={{ ...lampStyle, background: light === "red" ? "#ef4444" : "#374151" }} />
        <div style={{ ...lampStyle, background: light === "yellow" ? "#f59e0b" : "#374151" }} />
        <div style={{ ...lampStyle, background: light === "green" ? "#22c55e" : "#374151" }} />
      </div>
      <div style={rowStyle}>
        <button style={btnStyle} onClick={() => setLight("red")}>赤</button>
        <button style={btnStyle} onClick={() => setLight("yellow")}>黄</button>
        <button style={btnStyle} onClick={() => setLight("green")}>青</button>
      </div>
    </div>
  );
}

export default function App() {
  return (
    <div style={screenStyle}>
      <TrafficLight />
    </div>
  );
}

// ── 見た目（インラインstyle） ────────────────
const screenStyle: React.CSSProperties = {
  minHeight: "100vh",
  display: "flex",
  alignItems: "center",
  justifyContent: "center",
  background: "#f8fafc",
};

const cardStyle: React.CSSProperties = {
  display: "flex",
  flexDirection: "column",
  alignItems: "center",
  gap: 20,
  padding: 28,
  background: "#ffffff",
  border: "1px solid #e2e8f0",
  borderRadius: 16,
  boxShadow: "0 1px 3px rgba(0,0,0,0.08)",
};

const boardStyle: React.CSSProperties = {
  display: "flex",
  flexDirection: "column",
  gap: 12,
  padding: 14,
  background: "#1f2937",
  borderRadius: 16,
};

const lampStyle: React.CSSProperties = {
  width: 48,
  height: 48,
  borderRadius: "50%",
};

const rowStyle: React.CSSProperties = {
  display: "flex",
  gap: 8,
};

const btnStyle: React.CSSProperties = {
  padding: "8px 20px",
  background: "#6366f1",
  color: "#ffffff",
  fontSize: 15,
  fontWeight: 600,
  border: "none",
  borderRadius: 10,
  cursor: "pointer",
};
```


## ③ 計算で出せる値は、state にしない

**症状**：他のstateから計算できる値（`fullName`）をstate化。**なぜ悪い**：更新し忘れで表示がズレる（二重管理）。**直し方**：stateから消してレンダー時に計算。

**＋ propsをstateにコピーしない**：`useState(props.xxx)` は、親がpropsを変えても子が古いまま。初期値としてしか使わないか要確認。

見分け方：他のstateから作れる値を持っている。


![## ③ 計算で出せる値は、state にしない](State4_3_derived.svg)


```tsx
import React, { useState } from "react";

// ───────────────────────────────────────────
// ③ 計算で出せる値は、state にしない
//
// ✕ fullName も state にする（冗長）
//     const [fullName, setFullName] = useState("田中 太郎");
//     // lastName を変えたら fullName も setFullName し直す必要がある（忘れるとズレる）
//
// ○ 計算で出す（レンダーのたびに作る）
//     const fullName = lastName + " " + firstName;
//     // 常に最新。ズレようがない
// ───────────────────────────────────────────
function NameForm() {
  const [lastName, setLastName] = useState("田中");
  const [firstName, setFirstName] = useState("太郎");

  // state にしない。ただの計算。
  const fullName = lastName + " " + firstName;

  return (
    <div style={cardStyle}>
      <label style={labelStyle}>
        姓
        <input
          style={inputStyle}
          value={lastName}
          onChange={(e) => setLastName(e.target.value)}
        />
      </label>
      <label style={labelStyle}>
        名
        <input
          style={inputStyle}
          value={firstName}
          onChange={(e) => setFirstName(e.target.value)}
        />
      </label>
      <p style={resultStyle}>表示名：{fullName}</p>
    </div>
  );
}

export default function App() {
  return (
    <div style={screenStyle}>
      <NameForm />
    </div>
  );
}

// ── 見た目（インラインstyle） ────────────────
const screenStyle: React.CSSProperties = {
  minHeight: "100vh",
  display: "flex",
  alignItems: "center",
  justifyContent: "center",
  background: "#f8fafc",
};

const cardStyle: React.CSSProperties = {
  display: "flex",
  flexDirection: "column",
  gap: 14,
  width: 280,
  padding: 24,
  background: "#ffffff",
  border: "1px solid #e2e8f0",
  borderRadius: 16,
  boxShadow: "0 1px 3px rgba(0,0,0,0.08)",
};

const labelStyle: React.CSSProperties = {
  display: "flex",
  flexDirection: "column",
  gap: 4,
  fontSize: 13,
  color: "#64748b",
};

const inputStyle: React.CSSProperties = {
  padding: "8px 10px",
  fontSize: 15,
  border: "1px solid #cbd5e1",
  borderRadius: 8,
  boxSizing: "border-box",
};

const resultStyle: React.CSSProperties = {
  margin: "4px 0 0",
  fontSize: 16,
  fontWeight: 700,
  color: "#1e293b",
};
```


## ④ 同じデータを2箇所に持たない

**症状**：選択中をオブジェクトごとコピー（`selectedMember`）。**なぜ悪い**：同じデータが2箇所にあり、片方だけ古くなってズレる。**直し方**：`id` だけ持ち、表示時にリストから探す。

下のコードは ✕版と○版を並記。同じ「年齢+1」で、✕版は詳細が古いまま／○版は一致する。
見分け方：同じオブジェクトが複数のstateに入っている。


![## ④ 同じデータを2箇所に持たない](State4_4_dedupe.svg)


```tsx
import React, { useState } from "react";

type Member = { id: number; name: string; age: number };

const initial: Member[] = [
  { id: 1, name: "田中", age: 28 },
  { id: 2, name: "佐藤", age: 34 },
];

// ───────────────────────────────────────────
// ✕ 選択中を「オブジェクトごと」コピーして持つ（重複）
//    同じ田中さんが members と selected の2箇所に存在。
//    → リストを更新しても selected は古いコピーのまま = ズレる
// ───────────────────────────────────────────
function BadExample() {
  const [members, setMembers] = useState(initial);
  const [selected, setSelected] = useState<Member>(initial[0]);

  const addAge = () => {
    // リスト側だけ更新される。selected（コピー）は取り残される。
    setMembers(
      members.map((m) => (m.id === selected.id ? { ...m, age: m.age + 1 } : m))
    );
  };

  return (
    <div style={cardStyle}>
      <p style={badTitleStyle}>✕ selected をオブジェクトごと持つ</p>
      <div style={listStyle}>
        {members.map((m) => (
          <button key={m.id} style={rowStyle} onClick={() => setSelected(m)}>
            {m.name}（{m.age}歳）
          </button>
        ))}
      </div>
      <div style={detailStyle}>
        詳細：{selected.name}（{selected.age}歳）
      </div>
      <button style={btnStyle} onClick={addAge}>
        選択中の年齢 +1
      </button>
      <p style={badNoteStyle}>→ リストは増えるのに、詳細は古いまま！</p>
    </div>
  );
}

// ───────────────────────────────────────────
// ○ 選択中は id だけ持つ。表示はリストから探す。
//    データは members の1箇所だけ（single source of truth）。
// ───────────────────────────────────────────
function GoodExample() {
  const [members, setMembers] = useState(initial);
  const [selectedId, setSelectedId] = useState<number>(1);

  // state にしない。毎回リストから探す（常に最新）。
  const selected = members.find((m) => m.id === selectedId)!;

  const addAge = () => {
    setMembers(
      members.map((m) => (m.id === selectedId ? { ...m, age: m.age + 1 } : m))
    );
  };

  return (
    <div style={cardStyle}>
      <p style={goodTitleStyle}>○ selectedId だけ持つ</p>
      <div style={listStyle}>
        {members.map((m) => (
          <button key={m.id} style={rowStyle} onClick={() => setSelectedId(m.id)}>
            {m.name}（{m.age}歳）
          </button>
        ))}
      </div>
      <div style={detailStyle}>
        詳細：{selected.name}（{selected.age}歳）
      </div>
      <button style={btnStyle} onClick={addAge}>
        選択中の年齢 +1
      </button>
      <p style={goodNoteStyle}>→ リストも詳細も一緒に更新される</p>
    </div>
  );
}

export default function App() {
  return (
    <div style={screenStyle}>
      <BadExample />
      <GoodExample />
    </div>
  );
}

// ── 見た目（インラインstyle） ────────────────
const screenStyle: React.CSSProperties = {
  minHeight: "100vh",
  display: "flex",
  flexDirection: "column",
  alignItems: "center",
  justifyContent: "center",
  gap: 16,
  background: "#f8fafc",
  padding: 24,
};

const cardStyle: React.CSSProperties = {
  display: "flex",
  flexDirection: "column",
  gap: 10,
  width: 280,
  padding: 20,
  background: "#ffffff",
  border: "1px solid #e2e8f0",
  borderRadius: 16,
  boxShadow: "0 1px 3px rgba(0,0,0,0.08)",
};

const badTitleStyle: React.CSSProperties = {
  margin: 0,
  fontSize: 14,
  fontWeight: 700,
  color: "#dc2626",
};

const goodTitleStyle: React.CSSProperties = {
  margin: 0,
  fontSize: 14,
  fontWeight: 700,
  color: "#16a34a",
};

const listStyle: React.CSSProperties = {
  display: "flex",
  gap: 8,
};

const rowStyle: React.CSSProperties = {
  flex: 1,
  padding: "8px 10px",
  fontSize: 14,
  background: "#f1f5f9",
  border: "1px solid #cbd5e1",
  borderRadius: 8,
  cursor: "pointer",
};

const detailStyle: React.CSSProperties = {
  padding: "10px 12px",
  fontSize: 15,
  fontWeight: 600,
  color: "#1e293b",
  background: "#eef2ff",
  borderRadius: 8,
};

const btnStyle: React.CSSProperties = {
  padding: "8px 0",
  background: "#6366f1",
  color: "#ffffff",
  fontSize: 14,
  fontWeight: 600,
  border: "none",
  borderRadius: 8,
  cursor: "pointer",
};

const badNoteStyle: React.CSSProperties = {
  margin: 0,
  fontSize: 12,
  color: "#b91c1c",
};

const goodNoteStyle: React.CSSProperties = {
  margin: 0,
  fontSize: 12,
  color: "#15803d",
};
```


## ⑤ 深いネストを避ける（フラット化）

**症状**：stateが深い入れ子。**なぜ悪い**：深い所を1つ変えるだけで各層を全部コピーする羽目に。**直し方**：フラット化（正規化）——IDキーの辞書に分け、子はIDの配列で参照。

TS補足：`Record<number, T>`（IDで引く辞書型）が正規化と相性がいい。
見分け方：更新のたびに `{...}` が何段も重なる。


![## ⑤ 深いネストを避ける（フラット化）](State4_5_flatten.svg)


```tsx
import React, { useState } from "react";

// ───────────────────────────────────────────
// ⑤ 深いネストを避ける（フラット化＝正規化）
//
// ✕ 深いネスト：開発チームにメンバーを1人足すだけで、各層を全部コピー
//     setOrg({
//       ...org,
//       teams: org.teams.map((t) =>
//         t.id === 1 ? { ...t, members: [...t.members, newMember] } : t
//       ),
//     });
//     // org → teams → 対象team → members … 深いほどコピー地獄
//
// ○ フラット化：ID をキーにした辞書に分け、子は ID の配列で参照
//     teams:   { 1: { id, name, memberIds: [11, 12] }, ... }
//     members: { 11: { id, name }, 12: { id, name }, ... }
//     // 更新は「該当テーブルだけ」浅いコピーで済む
// ───────────────────────────────────────────

type Member = { id: number; name: string };
type Team = { id: number; name: string; memberIds: number[] };

// Record<K, V> = TSの辞書型（キーKで値Vを引く）
let seq = 100; // デモ用の簡易ID採番（本来はサーバー採番など）

function OrgView() {
  const [members, setMembers] = useState<Record<number, Member>>({
    11: { id: 11, name: "田中" },
    12: { id: 12, name: "佐藤" },
    13: { id: 13, name: "鈴木" },
  });
  const [teams, setTeams] = useState<Record<number, Team>>({
    1: { id: 1, name: "開発", memberIds: [11, 12] },
    2: { id: 2, name: "営業", memberIds: [13] },
  });

  const addMember = (teamId: number) => {
    const id = ++seq;
    // 1) members テーブルに追加（浅いコピー）
    setMembers({ ...members, [id]: { id, name: "新人" } });
    // 2) 対象チームの memberIds に id を足す（該当チームだけ）
    setTeams({
      ...teams,
      [teamId]: {
        ...teams[teamId],
        memberIds: [...teams[teamId].memberIds, id],
      },
    });
  };

  return (
    <div style={cardStyle}>
      {Object.values(teams).map((team) => (
        <div key={team.id} style={teamStyle}>
          <p style={teamNameStyle}>{team.name}</p>
          <div style={memberRowStyle}>
            {team.memberIds.map((mid) => (
              <span key={mid} style={chipStyle}>
                {members[mid].name}
              </span>
            ))}
          </div>
          <button style={btnStyle} onClick={() => addMember(team.id)}>
            ＋メンバー追加
          </button>
        </div>
      ))}
    </div>
  );
}

export default function App() {
  return (
    <div style={screenStyle}>
      <OrgView />
    </div>
  );
}

// ── 見た目（インラインstyle） ────────────────
const screenStyle: React.CSSProperties = {
  minHeight: "100vh",
  display: "flex",
  alignItems: "center",
  justifyContent: "center",
  background: "#f8fafc",
  padding: 24,
};

const cardStyle: React.CSSProperties = {
  display: "flex",
  flexDirection: "column",
  gap: 14,
  width: 300,
  padding: 20,
  background: "#ffffff",
  border: "1px solid #e2e8f0",
  borderRadius: 16,
  boxShadow: "0 1px 3px rgba(0,0,0,0.08)",
};

const teamStyle: React.CSSProperties = {
  display: "flex",
  flexDirection: "column",
  gap: 8,
  padding: 14,
  background: "#f8fafc",
  borderRadius: 12,
};

const teamNameStyle: React.CSSProperties = {
  margin: 0,
  fontSize: 15,
  fontWeight: 700,
  color: "#1e293b",
};

const memberRowStyle: React.CSSProperties = {
  display: "flex",
  gap: 6,
  flexWrap: "wrap",
};

const chipStyle: React.CSSProperties = {
  padding: "4px 12px",
  fontSize: 13,
  fontWeight: 600,
  color: "#4338ca",
  background: "#eef2ff",
  borderRadius: 999,
};

const btnStyle: React.CSSProperties = {
  alignSelf: "flex-start",
  padding: "6px 14px",
  background: "#6366f1",
  color: "#ffffff",
  fontSize: 13,
  fontWeight: 600,
  border: "none",
  borderRadius: 8,
  cursor: "pointer",
};
```


## ＋ stateのリフトアップ（状態の持ち上げ）

2つの子で同じ値を共有したいとき、子がそれぞれstateを持つとバラバラ。**共有したい状態を共通の親に上げて1つにする**。親が値と更新関数を持ち、子にはpropsで渡す（静的編のPropsと地続き）。


![## ＋ stateのリフトアップ（状態の持ち上げ）](State_lift_up.svg)


```tsx
import React, { useState } from "react";

// ───────────────────────────────────────────
// stateのリフトアップ（状態の持ち上げ）
//
// 2つの子で「同じ値」を共有したい。
//   ✕ 子がそれぞれ自分の state を持つ → バラバラで一致しない
//   ○ 共有したい state を「共通の親」に上げて1つにする
//     ・親が値(count)と更新関数(setCount)を持つ
//     ・子には props で「今の値」と「増やす関数」を渡す
// ───────────────────────────────────────────

// 子：自分では state を持たない。props で受け取るだけ。
type ButtonProps = {
  label: string;
  count: number;
  onAdd: () => void;
};

function AddButton({ label, count, onAdd }: ButtonProps) {
  return (
    <button style={btnStyle} onClick={onAdd}>
      {label}：{count}
    </button>
  );
}

// 親：共有 state をここで1つだけ持つ。
export default function App() {
  const [count, setCount] = useState(0);
  const add = () => setCount((c) => c + 1);

  return (
    <div style={screenStyle}>
      <div style={cardStyle}>
        <p style={totalStyle}>合計：{count}</p>
        {/* 2つの子が同じ count を共有する */}
        <div style={rowStyle}>
          <AddButton label="A" count={count} onAdd={add} />
          <AddButton label="B" count={count} onAdd={add} />
        </div>
      </div>
    </div>
  );
}

// ── 見た目（インラインstyle） ────────────────
const screenStyle: React.CSSProperties = {
  minHeight: "100vh",
  display: "flex",
  alignItems: "center",
  justifyContent: "center",
  background: "#f8fafc",
};

const cardStyle: React.CSSProperties = {
  display: "flex",
  flexDirection: "column",
  alignItems: "center",
  gap: 16,
  padding: 32,
  background: "#ffffff",
  border: "1px solid #e2e8f0",
  borderRadius: 16,
  boxShadow: "0 1px 3px rgba(0,0,0,0.08)",
};

const totalStyle: React.CSSProperties = {
  margin: 0,
  fontSize: 28,
  fontWeight: 700,
  color: "#1e293b",
};

const rowStyle: React.CSSProperties = {
  display: "flex",
  gap: 10,
};

const btnStyle: React.CSSProperties = {
  padding: "10px 24px",
  background: "#6366f1",
  color: "#ffffff",
  fontSize: 15,
  fontWeight: 600,
  border: "none",
  borderRadius: 10,
  cursor: "pointer",
};
```


---

## 5. まとめ＆演習

### state を足す前のチェックリスト（1つでもYESなら見直す）

1. 他のstate/propsから計算できる? → 計算で出す（③）
2. 常に別のstateと一緒に更新する? → まとめる（①）
3. 同時に成り立ってはいけない組み合わせがある? → status に列挙（②）
4. 同じデータが他のstateにもある? → IDで参照（④）
5. 入れ子が深い? → フラット化（⑤）

貫く一言：**「stateは最小限で素直な形。迷ったら "それ、計算で出せない?"」**

### 5原則 早見表

| # | 症状 | 直し方 |
|---|---|---|
| ① | 常に一緒に更新する複数state | オブジェクトにまとめる |
| ② | 同時にtrueになり得るフラグ | 1つの status に列挙 |
| ③ | 他から計算できる値をstate化 | 計算で出す（＋propsをコピーしない） |
| ④ | 同じ物を2箇所に保持 | ID/indexで参照 |
| ⑤ | 深いネスト | フラット化（正規化） |

### 小演習

各シナリオで「どこが問題? どう直す?」を考える。

- A：`firstName`, `lastName`, `initials` の3つをstateで持つ
- B：`isLoading`, `isError`, `isSuccess` の3フラグでAPI状態を管理
- C：カゴ `items` と `totalPrice` を別々のstateで管理
- D：選択中の商品を `selectedProduct`（オブジェクト全体）で保持

**想定解（講師用）**：A→③（initialsは計算） / B→②（1つのstatus） / C→③（totalPriceは計算） / D→④（selectedIdに）

---

## 補足：入力フォームとイベントオブジェクト

フォーム周りは新人がつまずきやすい。**まず1つずつ → 次に汎用化**の順で扱う。

### まず1つずつ（個別）

使用頻度の高い入力を、独立した state と「その場の1行ハンドラ」で。ハンドラが受け取る `e` から値を取り出す。

| 入力 | state型 | 読み取り方 |
|---|---|---|
| テキスト | `string` | `e.target.value` |
| チェックボックス | `boolean` | `e.target.checked`（value ではない） |
| セレクト | `string` | `e.target.value` |
| 数値 | `number` | `Number(e.target.value)`（value は文字列） |
| ラジオ | `string` | `e.target.value`（checked は `value === state`） |

![入力の型と読み取り方](State_inputs_basic.svg)

- **チェック vs ラジオ**：チェックはON/OFFだから`checked`（各自の真偽）。ラジオはどれか1つだから`value`（グループで1つの文字列）。
- **数値の罠**：`type="number"` でも `e.target.value` は文字列。`Number(...)` で変換する。

```tsx
import React, { useState } from "react";

// ───────────────────────────────────────────
// 使用頻度の高い入力を「1つずつ」。
// まだ汎用ハンドラは使わず、各例が自分の state と1行ハンドラを持つ。
// ───────────────────────────────────────────

// ① テキスト：string / e.target.value
function TextExample() {
  const [name, setName] = useState("");

  return (
    <div style={boxStyle}>
      <label style={labelStyle}>① テキスト</label>
      <input
        style={inputStyle}
        value={name}
        onChange={(e) => setName(e.target.value)}
      />
      <p style={resultStyle}>name: "{name}"</p>
    </div>
  );
}

// ② チェックボックス：boolean / e.target.checked（value ではない）
function CheckboxExample() {
  const [agree, setAgree] = useState(false);

  return (
    <div style={boxStyle}>
      <label style={checkRowStyle}>
        <input
          type="checkbox"
          checked={agree}
          onChange={(e) => setAgree(e.target.checked)}
        />
        ② 同意する
      </label>
      <p style={resultStyle}>agree: {String(agree)}</p>
    </div>
  );
}

// ③ セレクト：string / e.target.value
function SelectExample() {
  const [dept, setDept] = useState("dev");

  return (
    <div style={boxStyle}>
      <label style={labelStyle}>③ セレクト</label>
      <select
        style={inputStyle}
        value={dept}
        onChange={(e) => setDept(e.target.value)}
      >
        <option value="dev">開発</option>
        <option value="sales">営業</option>
        <option value="design">デザイン</option>
      </select>
      <p style={resultStyle}>dept: "{dept}"</p>
    </div>
  );
}

// ④ 数値：number / Number(e.target.value)（value は文字列なので変換）
function NumberExample() {
  const [age, setAge] = useState(20);

  return (
    <div style={boxStyle}>
      <label style={labelStyle}>④ 数値</label>
      <input
        type="number"
        style={inputStyle}
        value={age}
        onChange={(e) => setAge(Number(e.target.value))}
      />
      <p style={resultStyle}>age: {age}（型は number）</p>
    </div>
  );
}

// ⑤ ラジオ：string / e.target.value
//    checkbox と違い「グループで1つ」を選ぶ。state は選択中の value 1つ。
//    各ボタンの checked は「value === state」で判定する。
function RadioExample() {
  const [plan, setPlan] = useState("free");

  return (
    <div style={boxStyle}>
      <label style={labelStyle}>⑤ ラジオ</label>
      <label style={radioRowStyle}>
        <input
          type="radio"
          name="plan"
          value="free"
          checked={plan === "free"}
          onChange={(e) => setPlan(e.target.value)}
        />
        無料
      </label>
      <label style={radioRowStyle}>
        <input
          type="radio"
          name="plan"
          value="pro"
          checked={plan === "pro"}
          onChange={(e) => setPlan(e.target.value)}
        />
        Pro
      </label>
      <p style={resultStyle}>plan: "{plan}"</p>
    </div>
  );
}

export default function App() {
  return (
    <div style={screenStyle}>
      <TextExample />
      <CheckboxExample />
      <SelectExample />
      <NumberExample />
      <RadioExample />
    </div>
  );
}

// ── 見た目（インラインstyle） ────────────────
const screenStyle: React.CSSProperties = {
  minHeight: "100vh",
  display: "flex",
  flexDirection: "column",
  alignItems: "center",
  gap: 12,
  background: "#f8fafc",
  padding: 24,
};

const boxStyle: React.CSSProperties = {
  display: "flex",
  flexDirection: "column",
  gap: 6,
  width: 260,
  padding: 16,
  background: "#ffffff",
  border: "1px solid #e2e8f0",
  borderRadius: 12,
  boxShadow: "0 1px 3px rgba(0,0,0,0.08)",
};

const labelStyle: React.CSSProperties = {
  fontSize: 13,
  fontWeight: 600,
  color: "#334155",
};

const checkRowStyle: React.CSSProperties = {
  display: "flex",
  alignItems: "center",
  gap: 8,
  fontSize: 14,
  fontWeight: 600,
  color: "#334155",
};

const radioRowStyle: React.CSSProperties = {
  display: "flex",
  alignItems: "center",
  gap: 8,
  fontSize: 14,
  color: "#475569",
};

const inputStyle: React.CSSProperties = {
  padding: "8px 10px",
  fontSize: 14,
  border: "1px solid #cbd5e1",
  borderRadius: 8,
  boxSizing: "border-box",
};

const resultStyle: React.CSSProperties = {
  margin: 0,
  fontSize: 13,
  color: "#6366f1",
  fontFamily: "monospace",
};
```

### フィールドが増えたら：1オブジェクト＋汎用ハンドラ

フィールドごとにハンドラを書くのは大変。各 input に `name` 属性を付け、**1つのオブジェクト state ＋ 汎用ハンドラ**にまとめる。

イベントオブジェクト（`e`）で使うのは実質この5つ：

- `e.target` … 発火した要素
- `e.target.value` … 入力文字列（number でも文字列）
- `e.target.checked` … 真偽（checkbox / radio）
- `e.target.name` … 要素の `name` 属性（**どのフィールドかを判定**）
- `e.preventDefault()` … 送信時のリロード防止

TSの型：`React.ChangeEvent<HTMLInputElement>`（select は `HTMLSelectElement`、textarea は `HTMLTextAreaElement`）、送信は `React.FormEvent`。

![汎用ハンドラの仕組み](State_form_inputs.svg)

値を取り出すときの3つの注意：

1. 数値入力も `value` は文字列 → `Number(value)`
2. checkbox は `value` でなく `checked`
3. 複数フィールドは `name` 属性＋ `setForm({...form, [e.target.name]: 値})`（計算されたキー）で1関数にまとめる

```tsx
import React, { useState } from "react";

// ───────────────────────────────────────────
// 複数項目フォーム：1つのオブジェクト state ＋ 汎用ハンドラ
//
// ポイント：各 input に name 属性を付け、ハンドラ1つで捌く。
//   e.target.name  … どのフィールドか
//   e.target.value … 入力値（number でも文字列！ → Number 変換）
//   e.target.checked … checkbox の真偽（value ではない）
// ───────────────────────────────────────────
type FormData = {
  name: string;
  age: number;
  dept: string;
  subscribe: boolean;
  bio: string;
};

function ProfileForm() {
  const [form, setForm] = useState<FormData>({
    name: "",
    age: 20,
    dept: "dev",
    subscribe: false,
    bio: "",
  });

  // 1つのハンドラで全フィールドを更新（name で行き先を決める）
  const handleChange = (
    e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement | HTMLTextAreaElement>
  ) => {
    const { name, value, type } = e.target;
    setForm({
      ...form,
      [name]:
        type === "checkbox"
          ? (e.target as HTMLInputElement).checked // checkbox は checked
          : type === "number"
          ? Number(value) // number も value は文字列 → 変換
          : value,
    });
  };

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault(); // 送信でページがリロードするのを防ぐ
    alert("送信内容:\n" + JSON.stringify(form, null, 2));
  };

  return (
    <form style={cardStyle} onSubmit={handleSubmit}>
      <label style={labelStyle}>
        名前（text）
        <input style={inputStyle} name="name" value={form.name} onChange={handleChange} />
      </label>

      <label style={labelStyle}>
        年齢（number）
        <input
          style={inputStyle}
          type="number"
          name="age"
          value={form.age}
          onChange={handleChange}
        />
      </label>

      <label style={labelStyle}>
        部署（select）
        <select style={inputStyle} name="dept" value={form.dept} onChange={handleChange}>
          <option value="dev">開発</option>
          <option value="sales">営業</option>
          <option value="design">デザイン</option>
        </select>
      </label>

      <label style={checkRowStyle}>
        <input
          type="checkbox"
          name="subscribe"
          checked={form.subscribe}
          onChange={handleChange}
        />
        メール通知を受け取る（checkbox）
      </label>

      <label style={labelStyle}>
        自己紹介（textarea）
        <textarea
          style={{ ...inputStyle, height: 60, resize: "none" }}
          name="bio"
          value={form.bio}
          onChange={handleChange}
        />
      </label>

      <button style={btnStyle} type="submit">
        送信
      </button>

      {/* 学習用：state が入力に追従するのを可視化 */}
      <pre style={preStyle}>{JSON.stringify(form, null, 2)}</pre>
    </form>
  );
}

export default function App() {
  return (
    <div style={screenStyle}>
      <ProfileForm />
    </div>
  );
}

// ── 見た目（インラインstyle） ────────────────
const screenStyle: React.CSSProperties = {
  minHeight: "100vh",
  display: "flex",
  alignItems: "center",
  justifyContent: "center",
  background: "#f8fafc",
  padding: 24,
};

const cardStyle: React.CSSProperties = {
  display: "flex",
  flexDirection: "column",
  gap: 12,
  width: 300,
  padding: 24,
  background: "#ffffff",
  border: "1px solid #e2e8f0",
  borderRadius: 16,
  boxShadow: "0 1px 3px rgba(0,0,0,0.08)",
};

const labelStyle: React.CSSProperties = {
  display: "flex",
  flexDirection: "column",
  gap: 4,
  fontSize: 13,
  color: "#64748b",
};

const checkRowStyle: React.CSSProperties = {
  display: "flex",
  alignItems: "center",
  gap: 8,
  fontSize: 13,
  color: "#475569",
};

const inputStyle: React.CSSProperties = {
  padding: "8px 10px",
  fontSize: 14,
  border: "1px solid #cbd5e1",
  borderRadius: 8,
  boxSizing: "border-box",
};

const btnStyle: React.CSSProperties = {
  padding: "10px 0",
  background: "#6366f1",
  color: "#ffffff",
  fontSize: 15,
  fontWeight: 600,
  border: "none",
  borderRadius: 10,
  cursor: "pointer",
};

const preStyle: React.CSSProperties = {
  margin: 0,
  padding: 12,
  fontSize: 12,
  background: "#f1f5f9",
  borderRadius: 8,
  color: "#334155",
  overflowX: "auto",
};
```


---

## 付録：図ファイル一覧

MDと同じフォルダに置くと本文の画像が表示される（第3章の3フェーズ図のみ Excalidraw で別途作成）。

- State0_why_no_rerender.svg
- State1_useState_anatomy.svg
- State2_snapshot.svg
- State3_key_reset.svg
- State_immutable_ref.svg
- State4_1_group.svg 〜 State4_5_flatten.svg
- State_lift_up.svg
- State_inputs_basic.svg（補足：入力の型と読み取り方）
- State_form_inputs.svg（補足：汎用ハンドラ）