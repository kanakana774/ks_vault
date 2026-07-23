# React開発で頻出のTypeScript要素 完全ガイド

---

## 目次

1. [type / interface](#1-type--interface)
2. [Reactコンポーネントの型](#2-reactコンポーネントの型)
3. [イベント型](#3-イベント型)
4. [ユーティリティ型](#4-ユーティリティ型)
5. [ユニオン型・リテラル型](#5-ユニオン型リテラル型)
6. [ジェネリクス](#6-ジェネリクス)
7. [typeof / keyof](#7-typeof--keyof)
8. [型アサーション・Non-null assertion](#8-型アサーションnon-null-assertion)

---

## 1. type / interface

### どんな時に使う？

- コンポーネントの **Props** に型をつけるとき（毎回使う）
- API レスポンスなど **データ構造** を定義するとき
- 複数ファイルで共有したい型を定義するとき

### 基本の違い

| | `type` | `interface` |
|--|--------|-------------|
| ユニオン型 | ✅ | ❌ |
| extends で拡張 | △（`&` で交差型） | ✅ |
| 同名で宣言マージ | ❌ | ✅ |
| React界隈の主流 | ⭐ よく使う | どちらも使われる |

### サンプルコード

```tsx
// ---- type の基本 ----
type ButtonProps = {
  label: string;
  onClick: () => void;
  disabled?: boolean; // ? をつけると省略可能（Optional）
};

const Button = ({ label, onClick, disabled = false }: ButtonProps) => (
  <button onClick={onClick} disabled={disabled}>
    {label}
  </button>
);

// 使用例
<Button label="送信" onClick={() => alert("送信！")} />
```

```tsx
// ---- interface の基本（extendで拡張しやすい） ----
interface User {
  id: number;
  name: string;
}

// extends で既存の型を拡張（継承に近い感覚）
interface AdminUser extends User {
  role: "admin" | "superadmin";
}

const showUser = (user: AdminUser) => {
  console.log(`${user.name} (${user.role})`);
};

showUser({ id: 1, name: "田中", role: "admin" });
```

```tsx
// ---- type で intersection（&）を使った拡張 ----
type BaseCardProps = {
  title: string;
  description: string;
};

type ClickableCardProps = BaseCardProps & {
  onClick: () => void;
};

const ClickableCard = ({ title, description, onClick }: ClickableCardProps) => (
  <div onClick={onClick}>
    <h2>{title}</h2>
    <p>{description}</p>
  </div>
);
```

---

## 2. Reactコンポーネントの型

### どんな時に使う？

- `children` を受け取るコンポーネントを作るとき
- コンポーネントの戻り値型を明示したいとき

### サンプルコード

```tsx
import { ReactNode, ReactElement } from "react";

// ---- ReactNode：children に使う最頻出の型 ----
// JSX・文字列・数値・null・配列など、レンダリングできるものすべて受け付ける

type LayoutProps = {
  children: ReactNode;
  title: string;
};

const Layout = ({ children, title }: LayoutProps) => (
  <div>
    <h1>{title}</h1>
    <main>{children}</main>
  </div>
);

// どんな子要素でも渡せる
<Layout title="ホーム">
  <p>テキストも</p>
  <div>JSXも</div>
  {null} {/* nullもOK */}
</Layout>
```

```tsx
// ---- React.FC（関数コンポーネント型）----
// React.FC は children が暗黙に含まれていたが v18以降は非推奨になりつつある
// 明示的な型定義のほうが推奨されている

// ❌ 古い書き方（React.FC に children が自動で入っていた時代）
const Old: React.FC<{ name: string }> = ({ name }) => <div>{name}</div>;

// ✅ 現在の推奨（Props型を明示して関数定義するだけ）
type GreetProps = {
  name: string;
  children?: ReactNode; // 必要なら明示する
};

const Greet = ({ name, children }: GreetProps) => (
  <div>
    <p>こんにちは、{name}さん</p>
    {children}
  </div>
);
```

```tsx
// ---- JSX.Element vs ReactNode vs ReactElement ----

// JSX.Element   → <div>... のような JSX のみ（null は含まない）
// ReactElement  → JSX.Element の別名に近い
// ReactNode     → 上記すべて + string + number + null + undefined なども含む

// 関数の戻り値型として明示したい場合
const Title = ({ text }: { text: string }): JSX.Element => <h1>{text}</h1>;

// children 受け取りは ReactNode が無難
type CardProps = { children: ReactNode };
const Card = ({ children }: CardProps): JSX.Element => (
  <div className="card">{children}</div>
);
```

---

## 3. イベント型

### どんな時に使う？

- `onChange`, `onClick`, `onSubmit` などのハンドラを関数として切り出すとき
- `e.target.value` などにアクセスする際に型を正確に伝えたいとき

### サンプルコード

```tsx
// ---- クリックイベント ----
// React.MouseEvent<対象のHTML要素>

const handleClick = (e: React.MouseEvent<HTMLButtonElement>) => {
  e.preventDefault();
  console.log("クリックされた座標:", e.clientX, e.clientY);
};

<button onClick={handleClick}>クリック</button>
```

```tsx
// ---- inputの変更イベント（最頻出） ----
// React.ChangeEvent<対象のHTML要素>

import { useState } from "react";

const TextInput = () => {
  const [value, setValue] = useState("");

  const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    setValue(e.target.value); // e.target.value が string と推論される
  };

  return <input type="text" value={value} onChange={handleChange} />;
};
```

```tsx
// ---- selectの変更イベント ----
const handleSelect = (e: React.ChangeEvent<HTMLSelectElement>) => {
  console.log(e.target.value);
};

<select onChange={handleSelect}>
  <option value="a">A</option>
  <option value="b">B</option>
</select>
```

```tsx
// ---- フォームのsubmitイベント ----
// React.FormEvent<HTMLFormElement>

const handleSubmit = (e: React.FormEvent<HTMLFormElement>) => {
  e.preventDefault(); // デフォルトのページリロードを防ぐ
  console.log("送信された");
};

<form onSubmit={handleSubmit}>
  <button type="submit">送信</button>
</form>
```

```tsx
// ---- よく使うイベント型の早見表 ----

// onClick   → React.MouseEvent<HTMLButtonElement>
// onChange  → React.ChangeEvent<HTMLInputElement | HTMLSelectElement | HTMLTextAreaElement>
// onSubmit  → React.FormEvent<HTMLFormElement>
// onKeyDown → React.KeyboardEvent<HTMLInputElement>
// onFocus   → React.FocusEvent<HTMLInputElement>
// onDrop    → React.DragEvent<HTMLDivElement>
```

---

## 4. ユーティリティ型

### どんな時に使う？

既存の型を変形させて新しい型を作りたいとき。「毎回ゼロから定義しなくていい」のが強み。

### サンプルコード

```tsx
// ---- Partial<T>：すべてのプロパティをOptionalにする ----
// 使いどころ：更新フォームで「変更したフィールドだけ送る」とき

type UserProfile = {
  name: string;
  bio: string;
  avatarUrl: string;
};

// すべて省略可能になる
type UpdateUserInput = Partial<UserProfile>;
// = { name?: string; bio?: string; avatarUrl?: string }

const updateProfile = (userId: number, input: UpdateUserInput) => {
  // nameだけ、bioだけ、など部分的な更新ができる
  console.log(userId, input);
};

updateProfile(1, { name: "新しい名前" }); // bioやavatarUrlを省略してOK
```

```tsx
// ---- Required<T>：すべてのプロパティを必須にする ----
// Partialの逆

type DraftPost = {
  title?: string;
  content?: string;
};

type PublishedPost = Required<DraftPost>;
// = { title: string; content: string }
```

```tsx
// ---- Pick<T, K>：特定のプロパティだけ抜き出す ----
// 使いどころ：一覧表示では全フィールド不要なとき

type User = {
  id: number;
  name: string;
  email: string;
  password: string; // 画面には出したくない
};

// 一覧表示用：idとnameだけ使う
type UserListItem = Pick<User, "id" | "name">;
// = { id: number; name: string }

const UserCard = ({ id, name }: UserListItem) => (
  <div>
    #{id}: {name}
  </div>
);
```

```tsx
// ---- Omit<T, K>：特定のプロパティだけ除く ----
// 使いどころ：passwordなど不要なフィールドを除きたいとき

type SafeUser = Omit<User, "password">;
// = { id: number; name: string; email: string }

// Pick vs Omit の使い分け
// → 「残したいものが少ない」なら Pick
// → 「除きたいものが少ない」なら Omit
```

```tsx
// ---- React.HTMLAttributes の継承（超頻出パターン） ----
// 使いどころ：独自コンポーネントに標準HTMLの属性を引き継がせたいとき

type InputProps = React.InputHTMLAttributes<HTMLInputElement> & {
  label: string; // 独自のPropsを追加
};

const LabeledInput = ({ label, ...rest }: InputProps) => (
  <div>
    <label>{label}</label>
    {/* ...rest で placeholder, disabled, value など標準属性をそのまま使える */}
    <input {...rest} />
  </div>
);

// 使用例：placeholder など HTMLの属性をそのまま渡せる
<LabeledInput label="名前" placeholder="田中太郎" maxLength={20} />
```

```tsx
// ---- Record<K, V>：キーと値の型を指定したオブジェクト型 ----
// 使いどころ：ステータス→ラベルのマッピングなど

type Status = "loading" | "success" | "error";

const statusLabel: Record<Status, string> = {
  loading: "読み込み中...",
  success: "完了",
  error: "エラーが発生しました",
};

const StatusBadge = ({ status }: { status: Status }) => (
  <span>{statusLabel[status]}</span>
);
```

---

## 5. ユニオン型・リテラル型

### どんな時に使う？

- Props で取りうる値を限定したいとき（`size="sm" | "md" | "lg"` など）
- APIレスポンスが複数の形を持つとき

### サンプルコード

```tsx
// ---- リテラル型：特定の文字列・数値だけ受け付ける ----

type Size = "sm" | "md" | "lg";
type Variant = "primary" | "secondary" | "danger";

type ButtonProps = {
  size: Size;
  variant: Variant;
  label: string;
};

const sizeClass: Record<Size, string> = {
  sm: "text-sm px-2 py-1",
  md: "text-base px-4 py-2",
  lg: "text-lg px-6 py-3",
};

const Button = ({ size, variant, label }: ButtonProps) => (
  <button className={`${sizeClass[size]} btn-${variant}`}>{label}</button>
);

// ✅ OK
<Button size="md" variant="primary" label="送信" />

// ❌ コンパイルエラー（"xl" は Size に含まれない）
<Button size="xl" variant="primary" label="送信" />
```

```tsx
// ---- ユニオン型：異なる型の「どちらか」 ----

type ApiResponse<T> =
  | { status: "success"; data: T }
  | { status: "error"; message: string };

const fetchUser = async (): Promise<ApiResponse<User>> => {
  try {
    const res = await fetch("/api/user");
    const data = await res.json();
    return { status: "success", data };
  } catch {
    return { status: "error", message: "取得に失敗しました" };
  }
};

// 使う側：status で絞り込むと data や message の型が自動で絞られる（型の絞り込み）
const response = await fetchUser();
if (response.status === "success") {
  console.log(response.data); // ここでは data が使える
} else {
  console.log(response.message); // ここでは message が使える
}
```

```tsx
// ---- discriminated union（タグ付きユニオン）----
// 使いどころ：コンポーネントが複数の「モード」を持つとき

type ModalProps =
  | { type: "confirm"; onConfirm: () => void; onCancel: () => void }
  | { type: "alert"; onClose: () => void };

const Modal = (props: ModalProps) => {
  if (props.type === "confirm") {
    return (
      <div>
        <button onClick={props.onConfirm}>OK</button>
        <button onClick={props.onCancel}>キャンセル</button>
      </div>
    );
  }
  return <button onClick={props.onClose}>閉じる</button>;
};
```

---

## 6. ジェネリクス

### どんな時に使う？

- 「型は違うけど処理は同じ」ものを汎用化したいとき
- カスタムフック（`useFetch<T>` など）でよく使う

### サンプルコード

```tsx
// ---- 基本のジェネリクス ----
// <T> が「型の引数」。呼び出し側が型を決める

function identity<T>(value: T): T {
  return value;
}

identity<string>("hello"); // T = string
identity<number>(42);      // T = number
```

```tsx
// ---- Fetchカスタムフック（実務で超頻出） ----

import { useState, useEffect } from "react";

type FetchState<T> = {
  data: T | null;
  loading: boolean;
  error: string | null;
};

function useFetch<T>(url: string): FetchState<T> {
  const [state, setState] = useState<FetchState<T>>({
    data: null,
    loading: true,
    error: null,
  });

  useEffect(() => {
    fetch(url)
      .then((res) => res.json())
      .then((data: T) => setState({ data, loading: false, error: null }))
      .catch((e) => setState({ data: null, loading: false, error: e.message }));
  }, [url]);

  return state;
}

// ---- 使い方 ----
type Post = { id: number; title: string; body: string };

const PostList = () => {
  // T に Post[] を渡すことで、data が Post[] 型になる
  const { data, loading, error } = useFetch<Post[]>(
    "https://jsonplaceholder.typicode.com/posts"
  );

  if (loading) return <p>読み込み中...</p>;
  if (error) return <p>エラー: {error}</p>;

  return (
    <ul>
      {data?.map((post) => (
        <li key={post.id}>{post.title}</li> // post.title が string と推論される
      ))}
    </ul>
  );
};
```

```tsx
// ---- ジェネリクスを使ったリストコンポーネント ----

type ListProps<T> = {
  items: T[];
  renderItem: (item: T) => ReactNode;
  keyExtractor: (item: T) => string;
};

function List<T>({ items, renderItem, keyExtractor }: ListProps<T>) {
  return (
    <ul>
      {items.map((item) => (
        <li key={keyExtractor(item)}>{renderItem(item)}</li>
      ))}
    </ul>
  );
}

// 使い方
type Product = { id: string; name: string; price: number };

<List<Product>
  items={[{ id: "1", name: "りんご", price: 100 }]}
  keyExtractor={(item) => item.id}
  renderItem={(item) => <span>{item.name}: ¥{item.price}</span>}
/>
```

---

## 7. typeof / keyof

### どんな時に使う？

- 既存のオブジェクトから型を生成したいとき（`typeof`）
- オブジェクトのキー名を型として使いたいとき（`keyof`）

### サンプルコード

```tsx
// ---- typeof：変数・オブジェクトから型を抽出 ----
// 使いどころ：定数から型を自動生成（手動で型を二重管理しなくてよい）

const theme = {
  colors: {
    primary: "#3B82F6",
    secondary: "#6B7280",
    danger: "#EF4444",
  },
  spacing: {
    sm: "8px",
    md: "16px",
    lg: "24px",
  },
} as const; // as const で値をリテラル型として固定

type Theme = typeof theme;
// = { colors: { primary: "#3B82F6"; ... }; spacing: { sm: "8px"; ... } }
```

```tsx
// ---- keyof：オブジェクトのキーをユニオン型に ----

type ColorKey = keyof typeof theme.colors;
// = "primary" | "secondary" | "danger"

// 使い方：コンポーネントのPropsでテーマカラーを指定
type TextProps = {
  color: ColorKey;
  children: ReactNode;
};

const ThemedText = ({ color, children }: TextProps) => (
  <span style={{ color: theme.colors[color] }}>{children}</span>
);

// ✅ OK
<ThemedText color="primary">メインテキスト</ThemedText>

// ❌ エラー（"blue" は ColorKey に含まれない）
<ThemedText color="blue">テキスト</ThemedText>
```

```tsx
// ---- 組み合わせパターン：設定オブジェクトからPropsを自動生成 ----

const BUTTON_VARIANTS = {
  primary: "bg-blue-500 text-white",
  secondary: "bg-gray-200 text-gray-800",
  danger: "bg-red-500 text-white",
} as const;

type ButtonVariant = keyof typeof BUTTON_VARIANTS;
// = "primary" | "secondary" | "danger"

const Button = ({
  variant,
  children,
}: {
  variant: ButtonVariant;
  children: ReactNode;
}) => (
  <button className={BUTTON_VARIANTS[variant]}>{children}</button>
);
```

---

## 8. 型アサーション・Non-null assertion

### どんな時に使う？

- TypeScriptが型を正確に推論できないとき（DOMアクセスなど）
- `useRef` でDOM要素にアクセスするとき

> ⚠️ 乱用注意！型安全が壊れるので「どうしても必要なとき」だけ使う

### サンプルコード

```tsx
// ---- as（型アサーション）----
// 「私はこの型だと分かっています」とTypeScriptに伝える

// DOMへのアクセス時
const el = document.getElementById("app") as HTMLDivElement;
// getElementById の戻り値は HTMLElement | null → as で絞り込む

// APIレスポンスの型指定
const data = await res.json() as User;
```

```tsx
// ---- !（Non-null assertion）----
// 「nullでもundefinedでもない」と断言する

const el = document.getElementById("app")!;
// HTMLElement | null → HTMLElement として扱う

// ⚠️ 実際に null の可能性があるなら使ってはいけない
```

```tsx
// ---- useRef でのよくある使い方 ----
import { useRef, useEffect } from "react";

const VideoPlayer = () => {
  // 初期値は null だが、マウント後は HTMLVideoElement が入る
  const videoRef = useRef<HTMLVideoElement>(null);

  useEffect(() => {
    // ?. でnullチェック（これが安全な方法）
    videoRef.current?.play();

    // または ! を使う（マウント後は必ず存在すると確信できる場合）
    // videoRef.current!.play();
  }, []);

  return <video ref={videoRef} src="video.mp4" />;
};
```

```tsx
// ---- as const：オブジェクトや配列をリテラル型で固定 ----
// typeof / keyof と組み合わせてよく使う

const ROUTES = ["home", "about", "contact"] as const;
type Route = (typeof ROUTES)[number]; // "home" | "about" | "contact"

// as const がないと string[] になってしまい、リテラル型が消える
const ROUTES_BAD = ["home", "about", "contact"];
type RouteBad = (typeof ROUTES_BAD)[number]; // string（意味がない）
```

---

## 優先順位

```
（最重要）
├── type / interface でPropsを定義する
├── ? でOptionalにする
├── ReactNode で children の型をつける
└── React.ChangeEvent / MouseEvent でイベント型をつける

（重要）
├── Omit / Pick / Partial でPropsを変形する
├── HTMLAttributes<T> を継承して標準属性を引き継ぐ
├── ユニオン型でPropsの選択肢を限定する
└── Record<K, V> でマッピングオブジェクトを型付けする

（中級）
├── typeof / keyof で定数から型を自動生成する
├── as const でリテラル型を守る
├── useRef<T> でDOM要素に型をつける
└── ジェネリクスでカスタムフックを汎用化する
```
