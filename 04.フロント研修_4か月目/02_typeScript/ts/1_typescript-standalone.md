# TypeScript 基礎ガイド（React不要）

> すべてのコードは [TypeScript Playground](https://www.typescriptlang.org/play) に貼ればそのまま動きます。

---

## 目次

1. [type / interface](#1-type--interface)
2. [ユニオン型・リテラル型](#2-ユニオン型リテラル型)
3. [ユーティリティ型](#3-ユーティリティ型)
4. [ジェネリクス](#4-ジェネリクス)
5. [typeof / keyof / as const](#5-typeof--keyof--as-const)
6. [型アサーション](#6-型アサーション)

---

## 1. type / interface

### ポイント
- オブジェクトの「形」を定義する
- `?` をつけるとOptional（省略可能）になる
- `type` はユニオン型など複雑な型に強い
- `interface` は `extends` で拡張しやすい

```typescript
// ---- type の基本 ----
type User = {
  id: number;
  name: string;
  email?: string; // ? = 省略可能
};

const user: User = { id: 1, name: "田中" }; // emailなしでもOK
console.log(user.name); // "田中"


// ---- 関数の引数・戻り値に使う ----
type Point = {
  x: number;
  y: number;
};

function distance(a: Point, b: Point): number {
  return Math.sqrt((a.x - b.x) ** 2 + (a.y - b.y) ** 2);
}

console.log(distance({ x: 0, y: 0 }, { x: 3, y: 4 })); // 5


// ---- interface の基本 ----
interface Animal {
  name: string;
  sound(): string; // メソッドの定義
}

// extends で拡張
interface Dog extends Animal {
  breed: string;
}

const dog: Dog = {
  name: "ポチ",
  breed: "柴犬",
  sound: () => "ワン！",
};

console.log(`${dog.name}（${dog.breed}）: ${dog.sound()}`);
// "ポチ（柴犬）: ワン！"


// ---- type で intersection（&）を使って拡張 ----
type Named = { name: string };
type Aged = { age: number };
type Person = Named & Aged; // 両方のプロパティを持つ

const person: Person = { name: "佐藤", age: 30 };
console.log(person); // { name: "佐藤", age: 30 }
```

---

## 2. ユニオン型・リテラル型

### ポイント
- リテラル型：特定の値だけを許可する（`"sm" | "md" | "lg"` など）
- ユニオン型：複数の型のどれかを受け付ける（`string | number` など）
- `if` や `switch` で絞り込むと型が自動で狭まる（型の絞り込み）

```typescript
// ---- リテラル型：取りうる値を限定する ----
type Direction = "north" | "south" | "east" | "west";

function move(dir: Direction, steps: number): string {
  return `${dir}に${steps}歩移動`;
}

console.log(move("north", 3)); // "northに3歩移動"
// move("up", 3); // ❌ エラー："up" は Direction に含まれない


// ---- ユニオン型：複数の型を受け付ける ----
type ID = number | string;

function findUser(id: ID) {
  if (typeof id === "number") {
    console.log(`数値IDで検索: ${id}`);
  } else {
    console.log(`文字列IDで検索: ${id.toUpperCase()}`); // string に絞られるので toUpperCase が使える
  }
}

findUser(42);      // "数値IDで検索: 42"
findUser("abc");   // "文字列IDで検索: ABC"


// ---- discriminated union（タグ付きユニオン）----
// 「type」などの共通プロパティで型を見分けるパターン
type Shape =
  | { kind: "circle"; radius: number }
  | { kind: "rect"; width: number; height: number };

function area(shape: Shape): number {
  switch (shape.kind) {
    case "circle":
      return Math.PI * shape.radius ** 2; // ここでは radius が使える
    case "rect":
      return shape.width * shape.height;  // ここでは width, height が使える
  }
}

console.log(area({ kind: "circle", radius: 5 }).toFixed(2)); // "78.54"
console.log(area({ kind: "rect", width: 4, height: 6 }));    // 24


// ---- null / undefined とのユニオン ----
type MaybeString = string | null;

function greet(name: MaybeString): string {
  if (name === null) return "名無しさん、こんにちは";
  return `${name}さん、こんにちは`; // ここでは string に絞られる
}

console.log(greet("田中")); // "田中さん、こんにちは"
console.log(greet(null));   // "名無しさん、こんにちは"
```

---

## 3. ユーティリティ型

### ポイント
- 既存の型を変形して新しい型を作る組み込み機能
- 「毎回ゼロから定義しなくていい」のが強み

```typescript
// ---- ベースになる型 ----
type User = {
  id: number;
  name: string;
  email: string;
  password: string;
};


// ---- Partial<T>：全プロパティをOptionalに ----
// 使いどころ：「変更したい項目だけ渡す」更新処理

type UpdateInput = Partial<User>;
// = { id?: number; name?: string; email?: string; password?: string }

function updateUser(id: number, input: UpdateInput) {
  console.log(`ID:${id} を更新`, input);
}

updateUser(1, { name: "新しい名前" }); // nameだけ渡してOK


// ---- Required<T>：全プロパティを必須に（Partialの逆） ----
type Config = {
  host?: string;
  port?: number;
};

type StrictConfig = Required<Config>;
// = { host: string; port: number }

const config: StrictConfig = { host: "localhost", port: 3000 }; // 両方必須


// ---- Pick<T, K>：指定したプロパティだけ抜き出す ----
// 使いどころ：一覧表示では全フィールド不要なとき

type UserSummary = Pick<User, "id" | "name">;
// = { id: number; name: string }

const summary: UserSummary = { id: 1, name: "田中" };
console.log(summary); // { id: 1, name: "田中" }


// ---- Omit<T, K>：指定したプロパティだけ除く ----
// 使いどころ：passwordなど不要なフィールドを除く

type SafeUser = Omit<User, "password">;
// = { id: number; name: string; email: string }

const safeUser: SafeUser = { id: 1, name: "田中", email: "tanaka@example.com" };
console.log(safeUser);

// Pick vs Omit の使い分け
// → 残したいプロパティが少ない → Pick
// → 除きたいプロパティが少ない → Omit


// ---- Record<K, V>：キーと値の型を指定したオブジェクト ----
// 使いどころ：コードとラベルのマッピングなど

type StatusCode = "200" | "404" | "500";

const statusMessage: Record<StatusCode, string> = {
  "200": "OK",
  "404": "Not Found",
  "500": "Internal Server Error",
};

console.log(statusMessage["404"]); // "Not Found"


// ---- Readonly<T>：全プロパティを読み取り専用に ----
type ImmutableUser = Readonly<User>;

const u: ImmutableUser = { id: 1, name: "田中", email: "t@example.com", password: "xxx" };
// u.name = "変更"; // ❌ エラー：読み取り専用なので変更不可
console.log(u.name); // "田中"


// ---- ReturnType<T>：関数の戻り値の型を取得 ----
function createSession() {
  return { token: "abc123", expiresAt: new Date() };
}

type Session = ReturnType<typeof createSession>;
// = { token: string; expiresAt: Date }

const session: Session = createSession();
console.log(session.token); // "abc123"
```

---

## 4. ジェネリクス

### ポイント
- `<T>` は「型の引数」。呼び出し側が型を決める
- 「処理は同じだけど型が違う」関数やクラスを汎用化できる
- カスタムフック（React）や汎用ライブラリで多用される

```typescript
// ---- 基本：型引数を受け取る関数 ----
function identity<T>(value: T): T {
  return value;
}

console.log(identity<string>("hello")); // "hello"
console.log(identity<number>(42));      // 42
// 型引数は省略して推論させることも多い
console.log(identity("world")); // TypeScriptが T = string と推論


// ---- 配列の先頭要素を返す汎用関数 ----
function first<T>(arr: T[]): T | undefined {
  return arr[0];
}

console.log(first([1, 2, 3]));         // 1（number と推論）
console.log(first(["a", "b", "c"]));   // "a"（string と推論）
console.log(first([]));                // undefined


// ---- 複数の型引数 ----
function zip<A, B>(a: A[], b: B[]): [A, B][] {
  return a.map((item, i) => [item, b[i]]);
}

const result = zip([1, 2, 3], ["one", "two", "three"]);
console.log(result); // [[1, "one"], [2, "two"], [3, "three"]]


// ---- 制約付きジェネリクス（extends） ----
// T が特定のプロパティを持つことを保証する

type HasId = { id: number };

function findById<T extends HasId>(items: T[], id: number): T | undefined {
  return items.find((item) => item.id === id);
}

const users = [
  { id: 1, name: "田中" },
  { id: 2, name: "佐藤" },
];

const found = findById(users, 2);
console.log(found); // { id: 2, name: "佐藤" }


// ---- ジェネリクスを使ったAPIラッパー ----
// 実務でよく使うパターン

type ApiResult<T> =
  | { ok: true; data: T }
  | { ok: false; error: string };

async function fetchJson<T>(url: string): Promise<ApiResult<T>> {
  try {
    const res = await fetch(url);
    const data: T = await res.json();
    return { ok: true, data };
  } catch (e) {
    return { ok: false, error: String(e) };
  }
}

// 使う側：T に期待する型を渡す
type Post = { id: number; title: string };

const result2 = await fetchJson<Post[]>("https://jsonplaceholder.typicode.com/posts");

if (result2.ok) {
  console.log(result2.data[0].title); // data が Post[] 型として使える
} else {
  console.log(result2.error);
}


// ---- ジェネリクスを使ったスタック（データ構造）----
class Stack<T> {
  private items: T[] = [];

  push(item: T): void {
    this.items.push(item);
  }

  pop(): T | undefined {
    return this.items.pop();
  }

  peek(): T | undefined {
    return this.items[this.items.length - 1];
  }

  get size(): number {
    return this.items.length;
  }
}

const numStack = new Stack<number>();
numStack.push(1);
numStack.push(2);
numStack.push(3);
console.log(numStack.peek()); // 3
console.log(numStack.pop());  // 3
console.log(numStack.size);   // 2
```

---

## 5. typeof / keyof / as const

### ポイント
- `typeof`：変数・オブジェクトから型を取り出す
- `keyof`：オブジェクト型のキーをユニオン型に変換する
- `as const`：値をリテラル型として固定する（変更不可）
- 3つを組み合わせると「定数から型を自動生成」できる

```typescript
// ---- typeof：変数から型を抽出 ----
const config = {
  host: "localhost",
  port: 3000,
  debug: true,
};

type Config = typeof config;
// = { host: string; port: number; debug: boolean }

// 型を手動で二重定義しなくてよい
function startServer(cfg: Config) {
  console.log(`${cfg.host}:${cfg.port} で起動 (debug: ${cfg.debug})`);
}

startServer(config); // "localhost:3000 で起動 (debug: true)"


// ---- keyof：オブジェクト型のキーをユニオン型に ----
type User = { id: number; name: string; email: string };

type UserKey = keyof User;
// = "id" | "name" | "email"

// 使いどころ：オブジェクトのキーを引数に取る汎用関数
function getProperty<T, K extends keyof T>(obj: T, key: K): T[K] {
  return obj[key];
}

const user = { id: 1, name: "田中", email: "tanaka@example.com" };
console.log(getProperty(user, "name"));  // "田中"（string と推論）
console.log(getProperty(user, "id"));    // 1（number と推論）
// getProperty(user, "age"); // ❌ エラー："age" は keyof User に含まれない


// ---- as const：値をリテラル型で固定 ----
// as const がないと string[] になってしまう

const DIRECTIONS_BAD = ["north", "south", "east", "west"];
type DirectionBad = (typeof DIRECTIONS_BAD)[number]; // string（意味がない）

const DIRECTIONS = ["north", "south", "east", "west"] as const;
type Direction = (typeof DIRECTIONS)[number];
// = "north" | "south" | "east" | "west"

function move(dir: Direction) {
  console.log(`${dir}に移動`);
}

move("north"); // ✅
// move("up"); // ❌ エラー


// ---- 3つの組み合わせ：定数から型を自動生成（実務頻出）----
const HTTP_STATUS = {
  OK: 200,
  NOT_FOUND: 404,
  SERVER_ERROR: 500,
} as const;

type StatusCode = keyof typeof HTTP_STATUS;
// = "OK" | "NOT_FOUND" | "SERVER_ERROR"

type StatusValue = (typeof HTTP_STATUS)[StatusCode];
// = 200 | 404 | 500

function handleStatus(status: StatusCode) {
  const code = HTTP_STATUS[status];
  console.log(`${status}: ${code}`);
}

handleStatus("OK");        // "OK: 200"
handleStatus("NOT_FOUND"); // "NOT_FOUND: 404"
// handleStatus("FORBIDDEN"); // ❌ エラー


// ---- as const でオブジェクトを完全に固定 ----
const COLORS = {
  primary: "#3B82F6",
  secondary: "#6B7280",
  danger: "#EF4444",
} as const;

// as const がない場合
// COLORS.primary の型 → string（どんな文字列にも変更できる）

// as const がある場合
// COLORS.primary の型 → "#3B82F6"（この値しか入れられない）

type ColorName = keyof typeof COLORS; // "primary" | "secondary" | "danger"
type ColorValue = (typeof COLORS)[ColorName]; // "#3B82F6" | "#6B7280" | "#EF4444"
```

---

## 6. 型アサーション

### ポイント
- `as`：「この型として扱ってください」とTypeScriptに伝える
- `!`（Non-null assertion）：「nullでもundefinedでもない」と断言する
- 型安全が壊れるので**乱用は厳禁**。どうしても必要な場面だけ使う

```typescript
// ---- as（型アサーション）----
// TypeScriptが型を推論しきれない場面で使う

// 例：JSONのパース（any になる）
const json = '{"id": 1, "name": "田中"}';
const parsed = JSON.parse(json) as { id: number; name: string };
console.log(parsed.name); // "田中"（string として扱える）


// ---- as を使わない安全な代替手法（推奨）----
// 可能な限り型ガード関数を使うほうがベター

type User = { id: number; name: string };

function isUser(value: unknown): value is User {
  return (
    typeof value === "object" &&
    value !== null &&
    "id" in value &&
    "name" in value
  );
}

const data: unknown = JSON.parse('{"id": 1, "name": "田中"}');

if (isUser(data)) {
  console.log(data.name); // 安全に name にアクセスできる
}


// ---- !（Non-null assertion）----
// 「絶対にnullじゃない」と確信できるときだけ使う

const map = new Map<string, number>();
map.set("apple", 1);

// map.get() の戻り値は number | undefined
// "apple" を確実にセットしているので ! で断言
const count = map.get("apple")!;
console.log(count + 10); // 11


// ---- ?. （オプショナルチェーン）との使い分け ----
// null の可能性があるなら ! より ?. を使う（こちらが安全）

const maybeUser: User | null = null;

// ❌ これは実行時エラーになる可能性がある
// console.log(maybeUser!.name);

// ✅ ?. を使えばnullでも安全
console.log(maybeUser?.name); // undefined（エラーにならない）


// ---- as const との組み合わせ（再掲：実務頻出）----
const roles = ["admin", "editor", "viewer"] as const;
type Role = (typeof roles)[number]; // "admin" | "editor" | "viewer"

function checkRole(role: Role) {
  if (role === "admin") {
    console.log("管理者権限があります");
  } else {
    console.log("一般権限です");
  }
}

checkRole("admin"); // "管理者権限があります"
```

