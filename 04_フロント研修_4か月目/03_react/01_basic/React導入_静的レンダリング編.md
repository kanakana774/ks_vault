# React導入（静的レンダリング編）― TS経験者向け

> 一本のストーリー：**部品化して、値を渡して、組み立てる**。
> そこから **自動生成（map）→ 条件で出し分け → 中身を渡す（children）** まで。
> ここでは state / イベントは扱わない（すべて静的な画面）。

## 全体像

| # | テーマ | 一言 |
|---|--------|------|
| 1 | Reactとは | 画面を「部品（コンポーネント）」の組み合わせで作る |
| 2 | なぜ部品にするか | 再利用・修正一か所・役割分担 |
| 3 | コンポーネント | 値ではなく「画面(JSX)」を返す**関数** |
| 4 | JSX | HTML風だがHTMLではない。最終的にJSに変換される |
| 5 | Props | 部品に渡す「値」。関数の引数と同じ |
| 6 | リストレンダリング | 配列を `map` でカードの列に変換 |
| 7 | 条件付きレンダリング | `&&`（出す/出さない）と 三項（AかB） |
| 8 | children | タグの間に書いた「中身」を受け取る |

※コードはすべて `.tsx`／インラインstyle（ライブラリ非依存）。CSSの他の当て方は末尾の補足を参照。

---

## 1. Reactとは

> Reactは、画面を **コンポーネントという部品** の組み合わせで作るライブラリです。

```
画面
├─ Header
├─ Menu
├─ UserList
│   ├─ UserCard
│   ├─ UserCard
│   └─ UserCard
└─ Footer
```

## 2. なぜ部品にするか

ボタンが100個ある画面を、素のHTML/JSで書くと同じコードを量産しがち。Reactなら部品を何度でも使える。

```tsx
<Button />
<Button />
<Button />
```

- **再利用できる**
- **修正箇所が一か所**になる
- **役割ごとにファイルを分けられる**

---

## 3. コンポーネント ＝ 関数

> コンポーネントは JavaScript(TypeScript) の**関数**です。ただし「値」ではなく「画面(JSX)」を返します。

```ts
function total(): number { return 100; }        // 値を返す ← 知ってる関数
function UserCard() { return <div>…</div>; }     // 画面を返す ← コンポーネント
```

**トーク**：「関数なのは同じ。**返すものが数値ではなく画面**なだけ」

```tsx
import React from "react";

// ───────────────────────────────────────────
// コンポーネント = 関数。
// ただし「値」ではなく「画面(JSX)」を返す関数。
//
//   普通の関数      →  function total(): number { return 100; }
//   コンポーネント  →  function UserCard() { return <div>…</div>; }
// ───────────────────────────────────────────
function UserCard() {
  return (
    <div style={cardStyle}>
      <div style={avatarStyle}>田</div>
      <div>
        <p style={nameStyle}>田中 太郎</p>
        <p style={roleStyle}>フロントエンド</p>
      </div>
    </div>
  );
}

// 画面：さっき定義した関数を、部品として置くだけ。
export default function App() {
  return (
    <div style={screenStyle}>
      <UserCard />
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
  alignItems: "center",
  gap: 16,
  width: 320,
  padding: 16,
  background: "#ffffff",
  border: "1px solid #e2e8f0",
  borderRadius: 16,
  boxShadow: "0 1px 3px rgba(0,0,0,0.08)",
};

const avatarStyle: React.CSSProperties = {
  display: "flex",
  alignItems: "center",
  justifyContent: "center",
  width: 48,
  height: 48,
  borderRadius: "50%",
  background: "#6366f1",
  color: "#ffffff",
  fontSize: 18,
  fontWeight: "bold",
};

const nameStyle: React.CSSProperties = {
  margin: 0,
  fontWeight: 600,
  color: "#1e293b",
};

const roleStyle: React.CSSProperties = {
  margin: 0,
  fontSize: 14,
  color: "#64748b",
};
```

---

## 4. JSX

`return <button>登録</button>;` は **HTMLっぽいが、HTMLではない**。JSの中に書ける「HTML風の構文」で、最終的に **JavaScriptに変換（トランスパイル）** される。

![JSXはJavaScriptに変換される](Step4_JSX_transform.svg)

変換後は `React.createElement("button", null, "登録")` という**ただの関数呼び出し**。

**トーク**：「JSXは魔法じゃなくて、`React.createElement(...)` の**短い書き方**」

```tsx
import React from "react";

// ───────────────────────────────────────────
// JSX（JavaScript XML）= JS の中に書ける「HTML風の構文」。
//   ・HTMLっぽいが、HTMLではない
//   ・最終的に JavaScript に変換（トランスパイル）される
//
//   数値を返す関数  →  return 100;
//   JSXを返す関数   →  return <button>登録</button>;   ← コンポーネント
// ───────────────────────────────────────────
function RegisterButton() {
  return <button style={buttonStyle}>登録</button>;
}

export default function App() {
  return (
    <div style={screenStyle}>
      <RegisterButton />
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

const buttonStyle: React.CSSProperties = {
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

## 5. Props

部品が固定だと意味がない。同じ `UserCard` でも名前を変えて表示したい。**部品に渡す値が Props（プロパティ＝部品に渡す値）**。関数の引数と同じ考え方。

![Propsは関数の引数と同じ](Step5_Props_as_args.svg)

```ts
greet("田中");            // 関数に値を渡す
<UserCard name="田中" />  // 部品に値を渡す
```

TSなので **Propsに型**を付けられる（＝引数に型を付けるのと同じ）。

```tsx
import React from "react";

// ───────────────────────────────────────────
// 固定だと、同じ名前しか出せない：
//   function UserCard() { return <p>田中 太郎</p>; }   // ← 中身が固定
//
// name を「受け取る」ようにすると、渡した値で変わる：
//   <UserCard name="田中 太郎" />
//   <UserCard name="佐藤 花子" />
//
// Props（プロパティ = 部品に渡す値）は、関数の引数と同じ考え方。
//   greet("田中")            ← 関数に値を渡す
//   <UserCard name="田中" />  ← 部品に値を渡す
// ───────────────────────────────────────────

// 受け取る値の「型」。TSの関数引数に型を付けるのと同じ。
type UserCardProps = {
  name: string;
};

function UserCard({ name }: UserCardProps) {
  return (
    <div style={cardStyle}>
      <div style={avatarStyle}>{name.charAt(0)}</div>
      <p style={nameStyle}>{name}</p>
    </div>
  );
}

// 同じ部品に、違う値を渡す。
export default function App() {
  return (
    <div style={screenStyle}>
      <UserCard name="田中 太郎" />
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
  alignItems: "center",
  gap: 16,
  width: 300,
  padding: 16,
  background: "#ffffff",
  border: "1px solid #e2e8f0",
  borderRadius: 16,
  boxShadow: "0 1px 3px rgba(0,0,0,0.08)",
};

const avatarStyle: React.CSSProperties = {
  display: "flex",
  alignItems: "center",
  justifyContent: "center",
  width: 48,
  height: 48,
  borderRadius: "50%",
  background: "#6366f1",
  color: "#ffffff",
  fontSize: 18,
  fontWeight: "bold",
};

const nameStyle: React.CSSProperties = {
  margin: 0,
  fontWeight: 600,
  color: "#1e293b",
};
```

### 着地：複数Propsを渡して並べる

`name` だけでなく `role`・`color` も渡せる（Propsは複数OK）。ここまでで「**部品化して値を渡して組み立てる**」が完成。

```tsx
import React from "react";

// ───────────────────────────────────────────
// 着地点：同じ UserCard に、違う値（Props）を渡して並べる。
//   <UserCard name="田中 太郎" role="フロントエンド" color="#6366f1" />
//   <UserCard name="佐藤 花子" role="バックエンド"   color="#ec4899" />
//   <UserCard name="鈴木 一郎" role="デザイナー"     color="#14b8a6" />
//
// → Reactは「部品化して、値を渡して、組み立てる」
// ───────────────────────────────────────────

// 受け取る値の型（Propsは複数でもOK）。
type UserCardProps = {
  name: string;
  role: string;
  color: string;
};

// 部品①：ユーザー1人分のカード。
function UserCard({ name, role, color }: UserCardProps) {
  return (
    <div style={cardStyle}>
      <div style={{ ...avatarStyle, background: color }}>{name.charAt(0)}</div>
      <div>
        <p style={nameStyle}>{name}</p>
        <p style={roleStyle}>{role}</p>
      </div>
    </div>
  );
}

// 部品②：ヘッダー。
function Header() {
  return (
    <header style={headerStyle}>
      <p style={eyebrowStyle}>TEAM</p>
      <h1 style={titleStyle}>メンバー一覧</h1>
    </header>
  );
}

// 画面：部品を組み合わせるだけ。
export default function App() {
  return (
    <div style={screenStyle}>
      <div style={containerStyle}>
        <Header />
        <div style={listStyle}>
          <UserCard name="田中 太郎" role="フロントエンド" color="#6366f1" />
          <UserCard name="佐藤 花子" role="バックエンド" color="#ec4899" />
          <UserCard name="鈴木 一郎" role="デザイナー" color="#14b8a6" />
        </div>
      </div>
    </div>
  );
}

// ── 見た目（インラインstyle） ────────────────
const screenStyle: React.CSSProperties = {
  minHeight: "100vh",
  background: "#f8fafc",
  padding: 24,
};

const containerStyle: React.CSSProperties = {
  maxWidth: 420,
  margin: "0 auto",
};

const headerStyle: React.CSSProperties = {
  marginBottom: 20,
};

const eyebrowStyle: React.CSSProperties = {
  margin: 0,
  fontSize: 12,
  fontWeight: 600,
  letterSpacing: 1,
  color: "#6366f1",
};

const titleStyle: React.CSSProperties = {
  margin: "4px 0 0",
  fontSize: 24,
  fontWeight: 700,
  color: "#1e293b",
};

const listStyle: React.CSSProperties = {
  display: "flex",
  flexDirection: "column",
  gap: 12,
};

const cardStyle: React.CSSProperties = {
  display: "flex",
  alignItems: "center",
  gap: 16,
  padding: 16,
  background: "#ffffff",
  border: "1px solid #e2e8f0",
  borderRadius: 16,
  boxShadow: "0 1px 3px rgba(0,0,0,0.08)",
};

const avatarStyle: React.CSSProperties = {
  display: "flex",
  alignItems: "center",
  justifyContent: "center",
  width: 48,
  height: 48,
  borderRadius: "50%",
  color: "#ffffff",
  fontSize: 18,
  fontWeight: "bold",
  flexShrink: 0,
};

const nameStyle: React.CSSProperties = {
  margin: 0,
  fontWeight: 600,
  color: "#1e293b",
};

const roleStyle: React.CSSProperties = {
  margin: 0,
  fontSize: 14,
  color: "#64748b",
};
```

---

## 6. リストレンダリング

手書きで3枚並べるのは、100人いたら破綻する。**データ配列を `map` で回して、1件ずつカードに変換**する。

> `配列.map()` はいつもの配列変換。**返すのがJSXなだけ**。

![データ配列がカードの列になる](List_render_diagram.svg)

**`key` について**：`key` は各要素を見分ける目印。**静的な画面では効果は体感できない**（本質は画面が変化する state の回で回収）。今は「配列で並べるときの目印」とだけ伝える。無いとReactが警告を出す。

```tsx
import React from "react";

// ───────────────────────────────────────────
// 手書きで3枚並べていた：
//   <UserCard name="田中 太郎" ... />
//   <UserCard name="佐藤 花子" ... />
//   <UserCard name="鈴木 一郎" ... />
//
// → 100人いたら書ききれない。
//   データ(配列)を map で回して、1件ずつカードに変換する。
//
//   配列.map() = いつもの配列変換。返すのが JSX なだけ。
// ───────────────────────────────────────────

type User = {
  name: string;
  role: string;
  color: string;
};

// データ（本来はAPIから来る想定。今は固定）。
const users: User[] = [
  { name: "田中 太郎", role: "フロントエンド", color: "#6366f1" },
  { name: "佐藤 花子", role: "バックエンド", color: "#ec4899" },
  { name: "鈴木 一郎", role: "デザイナー", color: "#14b8a6" },
];

function UserCard({ name, role, color }: User) {
  return (
    <div style={cardStyle}>
      <div style={{ ...avatarStyle, background: color }}>{name.charAt(0)}</div>
      <div>
        <p style={nameStyle}>{name}</p>
        <p style={roleStyle}>{role}</p>
      </div>
    </div>
  );
}

export default function App() {
  return (
    <div style={screenStyle}>
      <div style={containerStyle}>
        {/* 配列を map で回して、User 1件を <UserCard /> 1枚に変換 */}
        {/* key = 各要素を見分ける目印（理由の詳細は state の回で） */}
        {users.map((user) => (
          <UserCard
            key={user.name}
            name={user.name}
            role={user.role}
            color={user.color}
          />
        ))}
      </div>
    </div>
  );
}

// ── 見た目（インラインstyle） ────────────────
const screenStyle: React.CSSProperties = {
  minHeight: "100vh",
  background: "#f8fafc",
  padding: 24,
};

const containerStyle: React.CSSProperties = {
  maxWidth: 420,
  margin: "0 auto",
  display: "flex",
  flexDirection: "column",
  gap: 12,
};

const cardStyle: React.CSSProperties = {
  display: "flex",
  alignItems: "center",
  gap: 16,
  padding: 16,
  background: "#ffffff",
  border: "1px solid #e2e8f0",
  borderRadius: 16,
  boxShadow: "0 1px 3px rgba(0,0,0,0.08)",
};

const avatarStyle: React.CSSProperties = {
  display: "flex",
  alignItems: "center",
  justifyContent: "center",
  width: 48,
  height: 48,
  borderRadius: "50%",
  color: "#ffffff",
  fontSize: 18,
  fontWeight: "bold",
  flexShrink: 0,
};

const nameStyle: React.CSSProperties = {
  margin: 0,
  fontWeight: 600,
  color: "#1e293b",
};

const roleStyle: React.CSSProperties = {
  margin: 0,
  fontSize: 14,
  color: "#64748b",
};
```

---

## 7. 条件付きレンダリング

条件によって表示を変える。2パターンある。

![条件付きレンダリングの2パターン](Conditional_render_diagram.svg)

```tsx
{ 条件 && <JSX> }      // 出す / 出さない       （&&）
{ 条件 ? <A> : <B> }   // A か B のどちらか      （三項演算子）
```

**落とし穴（率直に）**：`&&` の左が**数値**だと事故る。

```tsx
{ items.length && <List /> }    // items が空だと、画面に 0 が出てしまう
{ items.length > 0 && <List /> } // ← boolean にすれば安全
```

「`&&` の左は必ず boolean に」と伝えておくと後でハマらない。

```tsx
import React from "react";

// ───────────────────────────────────────────
// 全員同じ見た目だった → 条件で表示を出し分けたい。
//
// 条件付きレンダリング（条件によって表示を変える）は2パターン：
//
//   { 条件 && <JSX> }      → 出す / 出さない       （&&）
//   { 条件 ? <A> : <B> }   → A か B のどちらか      （三項演算子）
//
// どちらも JSX の中に「JSの式」を { } で埋め込んでいるだけ。
// ───────────────────────────────────────────

type User = {
  name: string;
  role: string;
  color: string;
  online: boolean; // 追加：オンライン状態
};

const users: User[] = [
  { name: "田中 太郎", role: "フロントエンド", color: "#6366f1", online: true },
  { name: "佐藤 花子", role: "バックエンド", color: "#ec4899", online: false },
  { name: "鈴木 一郎", role: "デザイナー", color: "#14b8a6", online: true },
];

function UserCard({ name, role, color, online }: User) {
  return (
    <div style={cardStyle}>
      <div style={{ ...avatarStyle, background: color }}>
        {name.charAt(0)}
        {/* 三項：online なら緑、そうでなければ灰のドット（常にどちらか出る） */}
        <span
          style={{ ...dotStyle, background: online ? "#22c55e" : "#cbd5e1" }}
        />
      </div>

      <div style={{ flex: 1 }}>
        <p style={nameStyle}>{name}</p>
        <p style={roleStyle}>{role}</p>
      </div>

      {/* && ：online のときだけバッジを出す（false なら何も出ない） */}
      {online && <span style={badgeStyle}>オンライン</span>}
    </div>
  );
}

export default function App() {
  return (
    <div style={screenStyle}>
      <div style={containerStyle}>
        {users.map((user) => (
          <UserCard
            key={user.name}
            name={user.name}
            role={user.role}
            color={user.color}
            online={user.online}
          />
        ))}
      </div>
    </div>
  );
}

// ── 見た目（インラインstyle） ────────────────
const screenStyle: React.CSSProperties = {
  minHeight: "100vh",
  background: "#f8fafc",
  padding: 24,
};

const containerStyle: React.CSSProperties = {
  maxWidth: 420,
  margin: "0 auto",
  display: "flex",
  flexDirection: "column",
  gap: 12,
};

const cardStyle: React.CSSProperties = {
  display: "flex",
  alignItems: "center",
  gap: 16,
  padding: 16,
  background: "#ffffff",
  border: "1px solid #e2e8f0",
  borderRadius: 16,
  boxShadow: "0 1px 3px rgba(0,0,0,0.08)",
};

const avatarStyle: React.CSSProperties = {
  position: "relative",
  display: "flex",
  alignItems: "center",
  justifyContent: "center",
  width: 48,
  height: 48,
  borderRadius: "50%",
  color: "#ffffff",
  fontSize: 18,
  fontWeight: "bold",
  flexShrink: 0,
};

const dotStyle: React.CSSProperties = {
  position: "absolute",
  right: 0,
  bottom: 0,
  width: 14,
  height: 14,
  borderRadius: "50%",
  border: "2px solid #ffffff",
};

const nameStyle: React.CSSProperties = {
  margin: 0,
  fontWeight: 600,
  color: "#1e293b",
};

const roleStyle: React.CSSProperties = {
  margin: 0,
  fontSize: 14,
  color: "#64748b",
};

const badgeStyle: React.CSSProperties = {
  fontSize: 12,
  fontWeight: 600,
  color: "#15803d",
  background: "#dcfce7",
  padding: "2px 10px",
  borderRadius: 999,
  flexShrink: 0,
};
```

---

## 8. children

`name="田中"` のように props で「値」を渡すのに対し、**タグの間に書いた「中身（JSXのかたまり）」を丸ごと渡す**のが children（子要素）。

> `<Card>ここに書いた中身</Card>` の**中身**が、`{children}` の位置に差し込まれる。

![見出しはprops、中身はchildren](Children_diagram.svg)

実アプリの「見出し付きパネル」が好例。**見出しバー付きの箱（枠）は共通、中身だけ違う**。役割分担は「`title` は props（値）／中身は children」。

**トーク**：「`Panel` は"入れ物"。**何を入れるかは使う側の自由**。だからアプリ全体で見た目を揃えられる」

```tsx
import React from "react";

// ───────────────────────────────────────────
// 実アプリでよく見る「見出し付きパネル」を Panel 部品にする。
//   ・見出しバー付きの箱（枠）は、どこでも共通
//   ・中身は場所ごとに全然違う（数字 / リスト / メンバー…）
//
// 役割分担：
//   title    … props で渡す「値」（見出しの文字）
//   children … タグの間に書く「中身」（JSXのかたまり）
// ───────────────────────────────────────────

type PanelProps = {
  title: string; // 見出し（props）
  children: React.ReactNode; // 中身（children）= JSXなど画面に出せるもの
};

// Panel =「見出しバー付きの枠」。中身は使う側が決める。
function Panel({ title, children }: PanelProps) {
  return (
    <div style={panelStyle}>
      <div style={panelHeaderStyle}>{title}</div>
      <div style={panelBodyStyle}>{children}</div>
    </div>
  );
}

export default function App() {
  return (
    <div style={screenStyle}>
      <div style={containerStyle}>
        {/* 中身① 数字 */}
        <Panel title="今月の売上">
          <p style={bigNumberStyle}>¥1,240,000</p>
          <p style={subTextStyle}>前月比 +12%</p>
        </Panel>

        {/* 中身② チェックリスト */}
        <Panel title="やること">
          <ul style={listStyle}>
            <li style={itemStyle}>
              <span style={checkStyle} />設計レビュー
            </li>
            <li style={itemStyle}>
              <span style={checkStyle} />API 動作確認
            </li>
            <li style={itemStyle}>
              <span style={checkStyle} />ドキュメント更新
            </li>
          </ul>
        </Panel>

        {/* 中身③ メンバー */}
        <Panel title="メンバー">
          <div style={rowStyle}>
            <div style={{ ...avatarStyle, background: "#6366f1" }}>田</div>
            <div style={{ ...avatarStyle, background: "#ec4899" }}>佐</div>
            <div style={{ ...avatarStyle, background: "#14b8a6" }}>鈴</div>
          </div>
        </Panel>
      </div>
    </div>
  );
}

// ── 見た目（インラインstyle） ────────────────
const screenStyle: React.CSSProperties = {
  minHeight: "100vh",
  background: "#f8fafc",
  padding: 24,
};

const containerStyle: React.CSSProperties = {
  maxWidth: 420,
  margin: "0 auto",
  display: "flex",
  flexDirection: "column",
  gap: 14,
};

// 枠（共通）──────────────
const panelStyle: React.CSSProperties = {
  overflow: "hidden",
  background: "#ffffff",
  border: "1px solid #e2e8f0",
  borderRadius: 14,
  boxShadow: "0 1px 3px rgba(0,0,0,0.08)",
};

const panelHeaderStyle: React.CSSProperties = {
  padding: "12px 16px",
  background: "#f8fafc",
  borderBottom: "1px solid #e2e8f0",
  fontSize: 14,
  fontWeight: 700,
  color: "#334155",
};

const panelBodyStyle: React.CSSProperties = {
  padding: 16,
};

// 中身用（それぞれ違う）──────────────
const bigNumberStyle: React.CSSProperties = {
  margin: 0,
  fontSize: 26,
  fontWeight: 700,
  color: "#1e293b",
};

const subTextStyle: React.CSSProperties = {
  margin: "4px 0 0",
  fontSize: 13,
  color: "#16a34a",
  fontWeight: 600,
};

const listStyle: React.CSSProperties = {
  listStyleType: "none",
  margin: 0,
  padding: 0,
  display: "flex",
  flexDirection: "column",
  gap: 8,
};

const itemStyle: React.CSSProperties = {
  display: "flex",
  alignItems: "center",
  gap: 8,
  fontSize: 14,
  color: "#475569",
};

const checkStyle: React.CSSProperties = {
  width: 14,
  height: 14,
  borderRadius: 4,
  border: "1.5px solid #cbd5e1",
  flexShrink: 0,
};

const rowStyle: React.CSSProperties = {
  display: "flex",
  gap: 10,
};

const avatarStyle: React.CSSProperties = {
  display: "flex",
  alignItems: "center",
  justifyContent: "center",
  width: 40,
  height: 40,
  borderRadius: "50%",
  color: "#ffffff",
  fontSize: 15,
  fontWeight: "bold",
};
```

---

## 補足：CSSの当て方パターン

この教材は **インラインstyle**（ライブラリ非依存）で統一している。実務では他の当て方もある。用途で選ぶ。

| 方法 | hover / メディアクエリ | クラス名の衝突 | 動的な値 | 使いどころ |
|------|:--:|:--:|:--:|------|
| インラインstyle | ✕ | 起きない | ◎（`{...s, background: color}`） | 小さな部品、プロトタイプ、動的スタイル |
| CSSファイル分離 | ○ | 起きうる（グローバル） | △ | Vite標準・小〜中規模 |
| CSS Modules | ○ | 起きない（自動ユニーク化） | △ | 中〜大規模・部品単位で管理 |
| Tailwind等 ユーティリティ | ○ | 起きない | △ | チーム方針次第・高速開発 |

### ① インラインstyle（今回採用）

```tsx
const boxStyle: React.CSSProperties = { padding: 16, background: "#fff" };

function Box() {
  return <div style={boxStyle}>…</div>;
}
```

- **長所**：1ファイル完結。スコープ衝突なし。propsで色を変えるなど**動的な値が楽**。
- **短所**：`:hover` やメディアクエリが**書けない**。再利用しにくい。

### ② CSSファイル分離（Vite標準）

```tsx
// Box.tsx
import "./Box.css";

function Box() {
  return <div className="box">…</div>;
}
```

```css
/* Box.css */
.box {
  padding: 16px;
  background: #fff;
}
.box:hover {           /* hover が書ける */
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}
```

- **長所**：`hover`・メディアクエリOK。CSSとロジックが分離。実務で一番オーソドックス。
- **短所**：クラス名が**グローバル**。大規模だと名前衝突に注意。

### ③ CSS Modules

```tsx
// Box.tsx
import styles from "./Box.module.css";

function Box() {
  return <div className={styles.box}>…</div>;
}
```

```css
/* Box.module.css */
.box {
  padding: 16px;
  background: #fff;
}
```

- **長所**：クラス名が**自動でユニーク化**され衝突しない。`hover` もOK。
- **短所**：ファイルが増える。`styles.box` という書き方に慣れが要る。

### ④ ユーティリティ / CSS-in-JS（参考）

- **Tailwind CSS**：`className="p-4 bg-white rounded-xl"` のようにクラスを組み合わせる。クラス名を覚える学習コストはあるが、開発は速い。
- **styled-components / emotion**：CSSをJS内に書く（CSS-in-JS）。動的スタイルに強いが、依存ライブラリが増える。

**まとめの指針**：まずは **②CSSファイル分離** が実務の基本形。小さな部品や動的スタイルは **①インライン**、部品単位で厳密に管理したくなったら **③CSS Modules**。

---

## 次回予告：state / イベント

ここまでは **渡された値を表示するだけ**（静的）。次は——

> 「今は表示するだけ。次は **押したら変わる**（イベント＋state）」

`key` の本当の意味（差分検出）も、画面が変化する state の回で回収する。

---

## 付録：図ファイル一覧

同じフォルダに置くと本文の画像が表示される。

- `Step4_JSX_transform.svg`
- `Step5_Props_as_args.svg`
- `List_render_diagram.svg`
- `Conditional_render_diagram.svg`
- `Children_diagram.svg`
