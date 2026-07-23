第3回はいよいよReactの本体、**JSX/TSXとコンポーネント**に入ります。

Javaエンジニアにとって、HTML（のようなもの）をプログラムの中に直接書くJSXのスタイルは最初驚くかもしれませんが、これがReactの「UIを状態（データ）の関数として定義する」という強力な考え方の核になります。

---

# 第3回：Reactの基礎（JSX/TSXとコンポーネント）
**〜UIを型安全なコンポーネントとして定義する〜**

## 1. JSX / TSX とは？ [JS / TS拡張]

JSX (JavaScript XML) は、JavaScriptの中にHTMLのようなタグを記述できる構文です。
TypeScript環境では **TSX** と呼び、タグの属性や中身に強力な型チェックが働きます。

### 基本的な記述ルール
- **JavaScriptの埋め込み**: `{ }` を使う。
- **属性名**: `class` ではなく `className`、`onclick` ではなく `onClick` と書く（Javaのキャメルケースに近い）。
- **タグの閉鎖**: `<img />` や `<br />` のように必ず閉じタグが必要。

---

## 2. 関数コンポーネントの定義 [TS]

Reactのコンポーネントは、**「Props（引数）を受け取り、JSX（戻り値）を返す関数」**です。

### サンプルコード
```tsx
import React from 'react';

// 1. Props（引数）の型を定義
type WelcomeProps = {
  name: string;
  isVip: boolean;
};

// 2. コンポーネントを定義（アロー関数が主流）
const WelcomeMessage: React.FC<WelcomeProps> = ({ name, isVip }) => {
  return (
    <div className="container">
      <h1>Hello, {name}さん！</h1>
      {isVip && <p>VIP会員様、いつもありがとうございます。</p>}
    </div>
  );
};

export default WelcomeMessage;
```

### 比較：コンポーネントの定義方法
| 方法 | 特徴 | 推奨度 |
| :--- | :--- | :--- |
| **関数コンポーネント (FC)** | 簡潔。Hooksが使え、現在の主流。 | **推奨** |
| **クラスコンポーネント** | Javaの `class extends` に近いが、記述が冗長。 | 非推奨（既存改修のみ） |

**ベストプラクティス：**
- ファイル名は必ず **PascalCase**（例: `WelcomeMessage.tsx`）にします。
- コンポーネント名はファイル名と一致させます。

---

## 3. Props：コンポーネント間のデータ受け渡し [TS]

Javaでメソッドに引数を渡すのと同じ感覚ですが、Reactでは「親から子へ」の一方向にのみデータを渡せます。

### サンプルコード
```tsx
// 子コンポーネント
type UserCardProps = {
  id: number;
  label: string;
  onDelete: (id: number) => void; // 関数もPropsとして渡せる
};

const UserCard = ({ id, label, onDelete }: UserCardProps) => (
  <div>
    <span>{label}</span>
    <button onClick={() => onDelete(id)}>削除</button>
  </div>
);

// 親コンポーネント
const App = () => {
  const handleDelete = (id: number) => {
    console.log(`${id}を削除します`);
  };

  return (
    <section>
      {/* 子コンポーネントの呼び出し */}
      <UserCard id={1} label="田中" onDelete={handleDelete} />
      <UserCard id={2} label="佐藤" onDelete={handleDelete} />
    </section>
  );
};
```

---

## 4. 条件分岐とループの描画 [JS]

JSX内では `if` や `for` は使えません。代わりに第1回・第2回で学んだ **三項演算子** や **map** を使います。

### サンプルコード
```tsx
const UserList = () => {
  const users = [
    { id: 1, name: "Alice" },
    { id: 2, name: "Bob" },
  ];

  return (
    <ul>
      {/* 1. mapを使ったループ処理 */}
      {users.map((user) => (
        <li key={user.id}> { /* key属性は必須（Reactの最適化のため） */ }
          {user.name}
          {/* 2. 三項演算子による条件分岐 */}
          {user.name === "Alice" ? " (Admin)" : " (Guest)"}
        </li>
      ))}
    </ul>
  );
};
```

**アンチパターン：**
- `map` の中の `key` に、配列のインデックス（0, 1, 2...）を安易に使わないこと。データの並び替えが起きた際にバグの原因になります。可能な限り、DBのIDなどユニークな値を使います。

---

## 5. イベントハンドリングと型 [TS]

ボタンクリックや入力変更などのイベント処理です。TSではイベントオブジェクトの型指定が重要になります。

### サンプルコード
```tsx
const SearchForm = () => {
  // changeイベントの型：React.ChangeEvent<HTMLInputElement>
  const handleChange = (event: React.ChangeEvent<HTMLInputElement>) => {
    console.log(event.target.value);
  };

  // clickイベントの型：React.MouseEvent<HTMLButtonElement>
  const handleClick = (event: React.MouseEvent<HTMLButtonElement>) => {
    alert("検索しました");
  };

  return (
    <div>
      <input type="text" onChange={handleChange} />
      <button onClick={handleClick}>検索</button>
    </div>
  );
};
```

---

## 6. useState：コンポーネントの状態管理 [JS/TS]

Reactで最も重要なHooksの一つです。変数の値が変わったことをReactに知らせ、UIを自動で再描画させます。

### サンプルコード
```tsx
import { useState } from 'react';

const Counter = () => {
  // const [状態変数, 更新関数] = useState<型>(初期値);
  const [count, setCount] = useState<number>(0);

  const increment = () => {
    // count = count + 1; // ❌ 直接代入は厳禁（再描画されない）
    setCount(count + 1);  // ✅ 更新関数を使う
  };

  return (
    <div>
      <p>現在の値: {count}</p>
      <button onClick={increment}>+1</button>
    </div>
  );
};
```

---

## まとめ：

1.  **コンポーネントは「純粋な関数」を目指す**: 同じPropsを渡せば常に同じJSXを返すのが理想です。
2.  **型定義 (Props) がドキュメントになる**: `type Props = { ... }` を見るだけで、そのコンポーネントが何を必要としているか一目でわかるようになります。これがTSを使う最大のメリットです。
3.  **HTMLをJSで操るのではなく、データを更新してReactに任せる**: 
    - ❌ `document.getElementById(...).innerText = "..."`
    - ✅ `setCount(prev => prev + 1)` すると勝手にHTMLが変わる
4.  **ロジックと見た目の分離**: JSX内にはなるべく複雑な計算を書かず、`map` や三項演算子など最小限に留めます。

