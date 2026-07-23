# React開発で頻出のJavaScript要素（TypeScriptで記述）

> JSの機能だが、React開発で特によく使う要素を厳選。
> サンプルコードはすべてTypeScriptで記述しています。

---

## 目次

1. [分割代入（Destructuring）](#1-分割代入destructuring)
2. [スプレッド構文（Spread）](#2-スプレッド構文spread)
3. [テンプレートリテラル](#3-テンプレートリテラル)
4. [短絡評価・三項演算子](#4-短絡評価三項演算子)
5. [オプショナルチェーン / Nullish Coalescing](#5-オプショナルチェーン--nullish-coalescing)
6. [配列メソッド（map / filter / find / reduce）](#6-配列メソッドmap--filter--find--reduce)
7. [Arrow Function](#7-arrow-function)
8. [Promise / async・await](#8-promise--asyncawait)
9. [モジュール（import / export）](#9-モジュールimport--export)
10. [クロージャ・スコープ](#10-クロージャスコープ)

---

## 1. 分割代入（Destructuring）

### なぜReactで頻出？
- Propsの受け取りに毎回使う
- `useState` の戻り値受け取りに使う
- APIレスポンスのデータ取り出しに使う

```typescript
// ---- オブジェクトの分割代入 ----
type User = { id: number; name: string; email: string };

const user: User = { id: 1, name: "田中", email: "tanaka@example.com" };

// 通常のアクセス
console.log(user.name);
console.log(user.email);

// 分割代入（Reactのprops受け取りと同じ書き方）
const { name, email } = user;
console.log(name);  // "田中"
console.log(email); // "tanaka@example.com"


// ---- 別名をつける（rename）----
const { name: userName, id: userId } = user;
console.log(userName); // "田中"
console.log(userId);   // 1


// ---- デフォルト値を設定 ----
type Config = { host: string; port?: number };
const config: Config = { host: "localhost" };

const { host, port = 3000 } = config; // portがundefinedなら3000を使う
console.log(host); // "localhost"
console.log(port); // 3000


// ---- 配列の分割代入 ----
// useState の戻り値受け取りがまさにこの形
const [first, second, ...rest] = [1, 2, 3, 4, 5];
console.log(first);  // 1
console.log(second); // 2
console.log(rest);   // [3, 4, 5]


// ---- ネストしたオブジェクトの分割代入 ----
type Order = {
  id: number;
  user: { name: string; address: { city: string } };
};

const order: Order = {
  id: 100,
  user: { name: "佐藤", address: { city: "東京" } },
};

const { user: { name: orderUserName, address: { city } } } = order;
console.log(orderUserName); // "佐藤"
console.log(city);          // "東京"


// ---- 関数の引数で直接分割代入（Reactコンポーネントのprops受け取りと同じ）----
type ButtonProps = { label: string; disabled?: boolean };

function renderButton({ label, disabled = false }: ButtonProps): string {
  return `<button disabled=${disabled}>${label}</button>`;
}

console.log(renderButton({ label: "送信" }));
// "<button disabled=false>送信</button>"
```

---

## 2. スプレッド構文（Spread）

### なぜReactで頻出？
- stateの更新で「元のオブジェクトを壊さずにコピー＋変更」するために必須
- Propsのまとめ渡し（`<Component {...props} />`）に使う

```typescript
// ---- オブジェクトのスプレッド：コピー＋上書き ----
// Reactのstateを更新するとき毎回使うパターン

type UserProfile = { name: string; bio: string; age: number };

const original: UserProfile = { name: "田中", bio: "エンジニア", age: 30 };

// 元を壊さずに name だけ変えた新しいオブジェクトを作る
const updated = { ...original, name: "佐藤" };

console.log(original); // { name: "田中", bio: "エンジニア", age: 30 }（変わらない）
console.log(updated);  // { name: "佐藤", bio: "エンジニア", age: 30 }

// ❌ こう書くと元のオブジェクトが変わってしまう（Reactでは禁止）
// original.name = "佐藤";


// ---- 複数オブジェクトのマージ ----
type A = { x: number };
type B = { y: number };
type C = A & B;

const a: A = { x: 1 };
const b: B = { y: 2 };
const merged: C = { ...a, ...b };
console.log(merged); // { x: 1, y: 2 }


// ---- 配列のスプレッド：要素を追加した新配列を作る ----
// Reactのリスト更新で頻出

const items: string[] = ["apple", "banana"];

// 末尾に追加
const added = [...items, "cherry"];
console.log(added); // ["apple", "banana", "cherry"]

// 先頭に追加
const prepended = ["mango", ...items];
console.log(prepended); // ["mango", "apple", "banana"]

// 途中に挿入（index=1の位置に）
const index = 1;
const inserted = [
  ...items.slice(0, index),
  "grape",
  ...items.slice(index),
];
console.log(inserted); // ["apple", "grape", "banana"]


// ---- 配列から特定要素を削除した新配列 ----
// Reactのリスト削除で頻出（filterと組み合わせることも多い）

const numbers: number[] = [1, 2, 3, 4, 5];
const removed = numbers.filter((n) => n !== 3);
console.log(removed); // [1, 2, 4, 5]


// ---- ネストしたオブジェクトの更新（注意点）----
type UserWithAddress = {
  name: string;
  address: { city: string; zip: string };
};

const userNested: UserWithAddress = {
  name: "田中",
  address: { city: "東京", zip: "100-0001" },
};

// ⚠️ 浅いコピーなので address は同じ参照になってしまう
const shallowCopy = { ...userNested, name: "佐藤" };

// ✅ ネストも含めて正しく更新するには内側もスプレッドする
const deepUpdated: UserWithAddress = {
  ...userNested,
  address: { ...userNested.address, city: "大阪" },
};

console.log(deepUpdated);
// { name: "田中", address: { city: "大阪", zip: "100-0001" } }
```

---

## 3. テンプレートリテラル

### なぜReactで頻出？
- CSSクラス名の動的生成
- 表示テキストへの変数埋め込み
- APIエンドポイントのURL生成

```typescript
// ---- 基本：変数の埋め込み ----
const name = "田中";
const age = 30;

// 文字列結合（古い書き方）
console.log("こんにちは、" + name + "さん（" + age + "歳）");

// テンプレートリテラル（読みやすい）
console.log(`こんにちは、${name}さん（${age}歳）`);


// ---- 式を埋め込む ----
const price = 1000;
const tax = 0.1;

console.log(`税込価格: ${price * (1 + tax)}円`); // "税込価格: 1100円"
console.log(`消費税: ${tax * 100}%`);            // "消費税: 10%"


// ---- 三項演算子との組み合わせ（Reactで超頻出）----
// CSSクラス名の動的生成によく使う

type ButtonSize = "sm" | "md" | "lg";

function buttonClass(size: ButtonSize, isActive: boolean): string {
  return `btn btn-${size} ${isActive ? "btn-active" : "btn-inactive"}`;
}

console.log(buttonClass("md", true));  // "btn btn-md btn-active"
console.log(buttonClass("sm", false)); // "btn btn-sm btn-inactive"


// ---- URLの生成 ----
const baseUrl = "https://api.example.com";
const userId = 42;
const page = 2;

const endpoint = `${baseUrl}/users/${userId}/posts?page=${page}`;
console.log(endpoint);
// "https://api.example.com/users/42/posts?page=2"


// ---- 複数行文字列 ----
const html = `
  <div class="card">
    <h2>${name}</h2>
    <p>${age}歳</p>
  </div>
`;
console.log(html);
```

---

## 4. 短絡評価・三項演算子

### なぜReactで頻出？
- JSXの条件付きレンダリングに直接使う
  - `{isLoggedIn && <Dashboard />}` ← `&&` での表示/非表示
  - `{isLoading ? <Spinner /> : <Content />}` ← 三項演算子での切り替え

```typescript
// ---- 三項演算子：条件 ? 真 : 偽 ----
const isLoggedIn = true;

const message = isLoggedIn ? "ログイン中" : "ログアウト中";
console.log(message); // "ログイン中"


// ---- ネストした三項演算子（読みにくくなるので注意）----
type Status = "loading" | "success" | "error";
const status: Status = "success";

const label =
  status === "loading" ? "読み込み中..."
  : status === "success" ? "完了"
  : "エラーが発生しました";

console.log(label); // "完了"
// ※ 3分岐以上は switch や Record を使うほうが読みやすい


// ---- && （AND）短絡評価：左が truthy なら右を返す ----
// Reactでの「条件が真のときだけ表示する」パターンと同じ

const hasError = true;
const errorMessage = "入力が不正です";

// hasError が true のとき errorMessage を表示
const result1 = hasError && errorMessage;
console.log(result1); // "入力が不正です"

const hasError2 = false;
const result2 = hasError2 && errorMessage;
console.log(result2); // false（hasError2 が falsy なので右は評価されない）

// ⚠️ 0 や "" も falsy なので注意
const count = 0;
const countResult = count && "件あります";
console.log(countResult); // 0（"0件あります"にならない！）

// ✅ 対策：Boolean に変換してから使う
const countResult2 = count > 0 && "件あります";
console.log(countResult2); // false（意図通り）


// ---- || （OR）短絡評価：左が falsy なら右を返す ----
// デフォルト値の設定によく使う

const userName: string | undefined = undefined;
const displayName = userName || "ゲスト";
console.log(displayName); // "ゲスト"

const emptyString = "";
const display2 = emptyString || "名無し";
console.log(display2); // "名無し"（"" も falsy なので注意）
```

---

## 5. オプショナルチェーン / Nullish Coalescing

### なぜReactで頻出？
- APIのレスポンスはnullやundefinedが混じることが多い
- ネストしたデータへの安全なアクセスに必須

```typescript
// ---- ?. （オプショナルチェーン）----
// null / undefined のときエラーを出さず undefined を返す

type Address = { city: string; zip?: string };
type UserData = { name: string; address?: Address };

const user1: UserData = { name: "田中", address: { city: "東京" } };
const user2: UserData = { name: "佐藤" }; // address がない

// ❌ 安全でない（user2.address が undefined なので実行時エラー）
// console.log(user2.address.city);

// ✅ ?. を使うと undefined を返すだけでエラーにならない
console.log(user1.address?.city); // "東京"
console.log(user2.address?.city); // undefined
console.log(user2.address?.zip);  // undefined


// ---- メソッドにも使える ----
const maybeArray: number[] | null = Math.random() > 0.5 ? [1, 2, 3] : null;
console.log(maybeArray?.map((n) => n * 2)); // undefined（エラーにならない）

const realArray: number[] | null = [1, 2, 3];
console.log(realArray?.map((n) => n * 2)); // [2, 4, 6]


// ---- ?? （Nullish Coalescing）----
// null / undefined のときだけデフォルト値を使う（|| との違いに注意）

const value1: string | null = null;
console.log(value1 ?? "デフォルト"); // "デフォルト"

const value2: string | null = "";
console.log(value2 ?? "デフォルト"); // ""（空文字はnullishでないのでそのまま）
console.log(value2 || "デフォルト"); // "デフォルト"（|| は空文字も falsy 扱い）

// ↑ これが || と ?? の一番大事な違い
// ?? → null と undefined のときだけデフォルト値
// || → falsy（0, "", false, null, undefined）のときデフォルト値


// ---- ?. と ?? の組み合わせ（実務頻出）----
type ApiUser = { profile?: { displayName?: string } };

const apiUser1: ApiUser = { profile: { displayName: "田中太郎" } };
const apiUser2: ApiUser = { profile: {} };
const apiUser3: ApiUser = {};

function getDisplayName(u: ApiUser): string {
  return u.profile?.displayName ?? "名無しユーザー";
}

console.log(getDisplayName(apiUser1)); // "田中太郎"
console.log(getDisplayName(apiUser2)); // "名無しユーザー"
console.log(getDisplayName(apiUser3)); // "名無しユーザー"


// ---- ??= （Nullish代入）----
// null / undefined のときだけ代入する

let cachedData: string | null = null;
cachedData ??= "初期データ";
console.log(cachedData); // "初期データ"

cachedData ??= "上書きしない";
console.log(cachedData); // "初期データ"（すでに値があるので変わらない）
```

---

## 6. 配列メソッド（map / filter / find / reduce）

### なぜReactで頻出？
- `map`：リストをJSXに変換する（最頻出）
- `filter`：条件に合う要素だけ残す（リスト絞り込み）
- `find`：条件に合う最初の1件を取得
- `reduce`：集計・変換（合計・グルーピングなど）

```typescript
type Product = {
  id: number;
  name: string;
  price: number;
  category: string;
  inStock: boolean;
};

const products: Product[] = [
  { id: 1, name: "りんご", price: 100, category: "fruit", inStock: true },
  { id: 2, name: "バナナ", price: 80,  category: "fruit", inStock: false },
  { id: 3, name: "にんじん", price: 60, category: "vegetable", inStock: true },
  { id: 4, name: "じゃがいも", price: 50, category: "vegetable", inStock: true },
];


// ---- map：各要素を変換した新配列を返す ----
// Reactでは JSXの配列生成に毎回使う

const names: string[] = products.map((p) => p.name);
console.log(names); // ["りんご", "バナナ", "にんじん", "じゃがいも"]

// 税込価格に変換
const withTax = products.map((p) => ({
  ...p,
  price: Math.round(p.price * 1.1),
}));
console.log(withTax.map((p) => `${p.name}: ${p.price}円`));
// ["りんご: 110円", "バナナ: 88円", "にんじん: 66円", "じゃがいも: 55円"]


// ---- filter：条件を満たす要素だけ残した新配列を返す ----
const inStock = products.filter((p) => p.inStock);
console.log(inStock.map((p) => p.name)); // ["りんご", "にんじん", "じゃがいも"]

const fruits = products.filter((p) => p.category === "fruit");
console.log(fruits.map((p) => p.name)); // ["りんご", "バナナ"]

// map と filter の組み合わせ（Reactで頻出）
const inStockNames = products
  .filter((p) => p.inStock)
  .map((p) => p.name);
console.log(inStockNames); // ["りんご", "にんじん", "じゃがいも"]


// ---- find：条件を満たす最初の1件を返す（なければ undefined）----
const found = products.find((p) => p.id === 3);
console.log(found?.name); // "にんじん"

const notFound = products.find((p) => p.id === 99);
console.log(notFound); // undefined


// ---- findIndex：条件を満たす最初の要素のインデックスを返す ----
const index = products.findIndex((p) => p.id === 3);
console.log(index); // 2


// ---- some / every：条件チェック ----
const hasOutOfStock = products.some((p) => !p.inStock);
console.log(hasOutOfStock); // true（バナナが在庫なし）

const allInStock = products.every((p) => p.inStock);
console.log(allInStock); // false


// ---- reduce：集計・変換 ----
// 合計金額
const total = products.reduce((acc, p) => acc + p.price, 0);
console.log(`合計: ${total}円`); // "合計: 290円"

// カテゴリ別にグルーピング
const grouped = products.reduce<Record<string, Product[]>>((acc, p) => {
  const key = p.category;
  if (!acc[key]) acc[key] = [];
  acc[key].push(p);
  return acc;
}, {});

console.log(grouped.fruit.map((p) => p.name));     // ["りんご", "バナナ"]
console.log(grouped.vegetable.map((p) => p.name)); // ["にんじん", "じゃがいも"]


// ---- sort：並び替え（元の配列を変更するので注意）----
// ✅ スプレッドでコピーしてからソート（元を壊さない）
const byPrice = [...products].sort((a, b) => a.price - b.price);
console.log(byPrice.map((p) => `${p.name}:${p.price}`));
// ["じゃがいも:50", "にんじん:60", "バナナ:80", "りんご:100"]
```

---

## 7. Arrow Function

### なぜReactで頻出？
- コンポーネント定義がほぼ全て Arrow Function
- `map` / `filter` のコールバックに使う
- イベントハンドラの定義に使う

```typescript
// ---- 基本の書き方 ----
// function宣言
function add1(a: number, b: number): number {
  return a + b;
}

// Arrow Function（同じ意味）
const add2 = (a: number, b: number): number => {
  return a + b;
};

// 式が1つなら {} と return を省略できる
const add3 = (a: number, b: number): number => a + b;

console.log(add1(1, 2)); // 3
console.log(add3(1, 2)); // 3


// ---- オブジェクトを返すときは () で囲む ----
// {} がブロックと紛らわしいため

const makeUser = (name: string, age: number) => ({ name, age });
console.log(makeUser("田中", 30)); // { name: "田中", age: 30 }


// ---- 引数が1つなら () を省略できる ----
const double = (n: number): number => n * 2;
const names = ["田中", "佐藤", "鈴木"];
console.log(names.map((name) => name.toUpperCase()));
// ["田中", "佐藤", "鈴木"] ※日本語は変化しないが英字なら大文字化


// ---- this の扱いが function と違う（重要）----
// Arrow Function は自分の this を持たず、外側の this を使う

class Timer {
  seconds = 0;

  // ❌ function を使うと this が変わって動かない
  // startBad() {
  //   setInterval(function() { this.seconds++; }, 1000); // this が undefined
  // }

  // ✅ Arrow Function は外側の this（Timerインスタンス）を使う
  start() {
    setInterval(() => {
      this.seconds++;
      console.log(this.seconds);
    }, 1000);
  }
}


// ---- 即時実行関数（IIFE）----
// 初期化処理などで使う

const result = ((x: number, y: number) => x + y)(3, 4);
console.log(result); // 7


// ---- 高階関数（関数を返す関数）----
// イベントハンドラのファクトリでよく使う

function makeMultiplier(factor: number) {
  return (n: number) => n * factor;
}

const triple = makeMultiplier(3);
const quadruple = makeMultiplier(4);

console.log(triple(5));    // 15
console.log(quadruple(5)); // 20

// Reactでのイメージ（クリック時に特定のIDを渡すパターン）
const handleDelete = (id: number) => () => {
  console.log(`ID:${id} を削除`);
};

const deleteUser = handleDelete(42);
deleteUser(); // "ID:42 を削除"
```

---

## 8. Promise / async・await

### なぜReactで頻出？
- APIからデータを取得するときに必ず使う
- `useEffect` の中で非同期処理を行うときに使う

```typescript
// ---- Promise の基本 ----
function wait(ms: number): Promise<void> {
  return new Promise((resolve) => setTimeout(resolve, ms));
}

// .then チェーン（古い書き方）
wait(100).then(() => console.log("100ms後に実行"));


// ---- async / await（現代的な書き方）----
async function fetchUser(id: number): Promise<{ id: number; name: string }> {
  // fetch は Promise を返す
  const res = await fetch(`https://jsonplaceholder.typicode.com/users/${id}`);
  const data = await res.json();
  return data;
}

// async関数はPromiseを返すので await で待つ
const user = await fetchUser(1);
console.log(user.name);


// ---- エラーハンドリング ----
async function safeGetUser(id: number): Promise<string> {
  try {
    const res = await fetch(`https://jsonplaceholder.typicode.com/users/${id}`);

    if (!res.ok) {
      throw new Error(`HTTP error: ${res.status}`);
    }

    const data: { name: string } = await res.json();
    return data.name;
  } catch (e) {
    console.error("取得失敗:", e);
    return "不明なユーザー";
  }
}

console.log(await safeGetUser(1));   // "Leanne Graham"
console.log(await safeGetUser(999)); // "不明なユーザー"


// ---- Promise.all：複数の非同期処理を並列実行 ----
// 順番に実行すると遅いので、依存関係がなければ並列にする

async function fetchMultiple() {
  // ❌ 直列：合計待ち時間 = A + B + C
  // const userA = await fetchUser(1);
  // const userB = await fetchUser(2);
  // const userC = await fetchUser(3);

  // ✅ 並列：合計待ち時間 = max(A, B, C)
  const [userA, userB, userC] = await Promise.all([
    fetchUser(1),
    fetchUser(2),
    fetchUser(3),
  ]);

  console.log(userA.name, userB.name, userC.name);
}


// ---- Promise.allSettled：1つ失敗しても全部待つ ----
// Promise.all はどれか1つが失敗すると全体が失敗する
// allSettled はすべての結果（成功・失敗問わず）を受け取れる

const results = await Promise.allSettled([
  fetchUser(1),
  fetchUser(99999), // 存在しないID
  fetchUser(2),
]);

results.forEach((result) => {
  if (result.status === "fulfilled") {
    console.log("成功:", result.value.name);
  } else {
    console.log("失敗:", result.reason);
  }
});
```

---

## 9. モジュール（import / export）

### なぜReactで頻出？
- コンポーネント・関数・型を別ファイルに分けるために毎回使う

```typescript
// ---- named export（名前付きエクスポート）----
// utils.ts
export type Color = "red" | "green" | "blue";

export const PI = 3.14159;

export function formatPrice(price: number): string {
  return `¥${price.toLocaleString()}`;
}

export const add = (a: number, b: number): number => a + b;


// ---- named import ----
// main.ts
import { PI, formatPrice, add } from "./utils";
// import { formatPrice as fp } from "./utils"; // 別名をつけることも可能

console.log(PI);               // 3.14159
console.log(formatPrice(1000)); // "¥1,000"
console.log(add(1, 2));        // 3


// ---- default export（デフォルトエクスポート）----
// Reactコンポーネントはこれが多い
// Button.ts
type ButtonProps = { label: string };

function Button({ label }: ButtonProps): string {
  return `<button>${label}</button>`;
}

export default Button;


// ---- default import ----
// main.ts
import Button from "./Button";     // 名前は自由につけられる
import MyButton from "./Button";   // これも同じものを指す


// ---- 再エクスポート（バレルエクスポート）----
// index.ts でまとめて export するパターン
// components/index.ts
export { default as Button } from "./Button";
export { default as Input } from "./Input";
export type { Color } from "./utils";

// 使う側
import { Button, Input } from "./components"; // パスが短くなる


// ---- 名前空間インポート ----
import * as Utils from "./utils";
console.log(Utils.PI);
console.log(Utils.formatPrice(500));
```

---

## 10. クロージャ・スコープ

### なぜReactで頻出？
- `useState` / `useEffect` の動作理解に必須
- カスタムフックの仕組みがクロージャで成り立っている

```typescript
// ---- クロージャ：関数が定義時のスコープを「閉じ込める」----
function makeCounter(initial: number = 0) {
  let count = initial; // この変数がクロージャに閉じ込められる

  return {
    increment: () => ++count,
    decrement: () => --count,
    getCount: () => count,
    reset: () => { count = initial; },
  };
}

const counter = makeCounter(10);
console.log(counter.increment()); // 11
console.log(counter.increment()); // 12
console.log(counter.decrement()); // 11
counter.reset();
console.log(counter.getCount()); // 10


// ---- スコープ：変数がアクセスできる範囲 ----
const globalVar = "グローバル";

function outer() {
  const outerVar = "外側";

  function inner() {
    const innerVar = "内側";
    // inner は outerVar と globalVar にアクセスできる
    console.log(innerVar, outerVar, globalVar);
  }

  inner();
  // console.log(innerVar); // ❌ エラー：inner の外からはアクセスできない
}

outer();


// ---- let vs const のスコープ ----
// var は関数スコープ、let/const はブロックスコープ

for (let i = 0; i < 3; i++) {
  // let はブロックスコープなのでループごとに独立した i
  setTimeout(() => console.log(i), 100);
}
// 0, 1, 2（期待通り）

// var だと…
for (var j = 0; j < 3; j++) {
  setTimeout(() => console.log(j), 100);
}
// 3, 3, 3（var はスコープを突き抜けて共有されてしまう）


// ---- クロージャと非同期（useEffectの動作理解に重要）----
function setupTimer(initialCount: number) {
  let count = initialCount;

  // この関数が「クロージャ」として count を閉じ込める
  const tick = () => {
    count++;
    console.log(`tick: ${count}`);
  };

  return tick;
}

const tick = setupTimer(0);
tick(); // "tick: 1"
tick(); // "tick: 2"
tick(); // "tick: 3"
// count はそれぞれの呼び出し間で保持されている


// ---- メモ化パターン（クロージャの応用）----
// ReactのuseMemoやuseCallbackの仕組みと同じ考え方

function memoize<T>(fn: (arg: number) => T): (arg: number) => T {
  const cache = new Map<number, T>();

  return (arg: number) => {
    if (cache.has(arg)) {
      console.log(`キャッシュヒット: ${arg}`);
      return cache.get(arg)!;
    }
    const result = fn(arg);
    cache.set(arg, result);
    return result;
  };
}

const expensiveCalc = memoize((n: number) => {
  console.log(`計算中: ${n}`);
  return n * n;
});

console.log(expensiveCalc(5)); // "計算中: 5" → 25
console.log(expensiveCalc(5)); // "キャッシュヒット: 5" → 25（再計算なし）
console.log(expensiveCalc(6)); // "計算中: 6" → 36
```

---

## まとめ：Reactで使う頻度の目安

| 要素 | 頻度 | 特に使う場面 |
|------|------|-------------|
| 分割代入 | ★★★ | Props受け取り・useState |
| スプレッド構文 | ★★★ | State更新・Props引き継ぎ |
| 配列メソッド（map/filter） | ★★★ | リスト表示・データ変換 |
| 短絡評価・三項演算子 | ★★★ | 条件付きレンダリング |
| オプショナルチェーン / ?? | ★★★ | APIデータへの安全なアクセス |
| async / await | ★★★ | API通信（useEffect内） |
| Arrow Function | ★★★ | コンポーネント・コールバック |
| モジュール | ★★★ | ファイル分割 |
| テンプレートリテラル | ★★ | クラス名・URL生成 |
| クロージャ・スコープ | ★★ | フックの動作理解 |
