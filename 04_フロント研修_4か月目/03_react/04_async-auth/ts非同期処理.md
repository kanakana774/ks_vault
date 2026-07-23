# js/ts 非同期処理


## 1. 問題：時間のかかる処理をどう扱うか

### JavaScriptはシングルスレッド

JavaScriptはコードを**1行ずつ、上から順番に**実行する。
同時に複数の処理を実行することはできない。

これは通常の処理では問題ない：

```ts
const x: number = 1 + 2;
console.log(x);        // 3
console.log("終わり"); // 終わり
```

### 問題が起きるケース

ネットワーク通信・ファイル読み込みなど、**結果が出るまでに時間がかかる処理**が存在する。
もしこれを「待つ」方式で書いたら：

```ts
// ※ これは実際のJSの書き方ではない（説明のための擬似コード）
const data = 時間のかかる通信();  // 3秒待つ
console.log(data);               // ← その間、ブラウザが完全に固まる
```

3秒間、ブラウザはクリックにも反応できず、画面も更新できない。

### 解決策：「終わったら教えて」方式

「終わるまで待つ」のではなく、「終わったらこの処理を動かして」と**先に予約だけ登録**する。
その間、JSは他の処理を続けられる。

```
【同期（待つ）】                   【非同期（予約する）】
処理A ──→ 長い処理 ──→ 処理B      処理A ──→ 長い処理への予約
           ↑                                ↓（JSは次へ）
           3秒固まる                処理B ──→ 処理C
                                           ↓
                                    長い処理が完了 → 予約した処理を実行
```

---

## 2. コールバック（昔の書き方）

「終わったら教えて」を実現する最もシンプルな方法が**コールバック関数**。
「終わったときにこの関数を呼んでください」と渡す。

### setTimeout で体験する

```ts
console.log("1: 開始");

setTimeout(() => {
  console.log("3: 1秒後に実行");
}, 1000); // 1000ms後に呼んでほしい関数を渡す

console.log("2: setTimeoutの後");

// 出力:
// 1: 開始
// 2: setTimeoutの後
// 3: 1秒後に実行
```

`setTimeout` に渡した関数は「1秒後に呼んでください」という**予約**。
JSは予約だけ登録して次の行に進む。だから "2" が先に出力される。

### コールバック地獄

コールバックを入れ子にすると、処理が増えるほど読みにくくなる。

```ts
// ユーザー取得 → 投稿取得 → コメント取得・・・ の連鎖
// 全て前の処理結果に依存するので、前の処理完了後に予約としてcallbackを渡してくとこのようなことに、、、
getUser(1, (user) => {
  getPosts(user.id, (posts) => {
    getComments(posts[0].id, (comments) => {
      // さらに処理...
      getAuthor(comments[0].authorId, (author) => {
        console.log(author.name); // ← ここまで4段入れ子
      });
    });
  });
});
```

右に向かって増え続けるこの形を**コールバック地獄**と呼ぶ。
エラー処理を加えるとさらに複雑になる。

**→ この問題を解決するために登場したのが Promise。**

---

## 3. Promise

### Promise とは

**Promise（約束）** は「非同期処理の結果を入れる箱」。
処理が完了すると、箱に「成功した値」または「失敗したエラー」が入る。

Promise は必ず以下の3つの状態のどれかにある：

```
pending   → 処理中（まだ結果が出ていない）
fulfilled → 成功（値が確定）
rejected  → 失敗（エラーが確定）
```

一度 fulfilled か rejected になったら、もう状態は変わらない。

### fetchAPI
`fetch`はjsが提供する標準的なhttpリクエストを送るための関数。
URLを指定して、getリクエストが送れる。
※引数を変えることで、http methodを変更したり、headerやbodyを指定して送ることもできる。⇒**詳しくは講義資料で**

```ts
fetch("https://jsonplaceholder.typicode.com/users/1");
```

### Promise を受け取る
`fetch` は通信結果を Promise で返す。（外部のスレッドに投げて、券をもらうイメージ）

```ts
const promise: Promise<Response> = fetch("https://jsonplaceholder.typicode.com/users/1");

console.log(promise); // Promise { <pending> } ← まだ通信中
```

処理を丸投げした直後に見てみても、、、
箱（Promise）は手元にあるが、中身はまだ入っていない。

### .then() で値を取り出す

Promise の中の値は、**直接取り出せない**。
`.then()` で「fulfilled になったらこの処理をして」と**予約**する。

```ts
fetch("https://jsonplaceholder.typicode.com/users/1")
  .then((response) => {
    return response.json(); // レスポンスをJSONに変換（これも Promise を返す）
  })
  .then((user) => {
    console.log(user.name); // "Leanne Graham"
  });
```

`.then()` はチェーンできる。前の `.then()` が返した値が、次の `.then()` に渡される。

### .catch() でエラーを処理する

```ts
fetch("https://存在しないURL.example.com/api")
  .then((response) => {
    return response.json();
  })
  .then((data) => {
    console.log(data);
  })
  .catch((error: Error) => {
    // ↑ どこかで失敗したらここに来る
    console.log("エラー:", error.message);
  });
```

`.catch()` はチェーンの**どこで失敗しても**受け取れる。

### .finally() で後片付け

成功でも失敗でも必ず実行したい処理（ローディング非表示など）。

```ts
let isLoading: boolean = true;

fetch("https://jsonplaceholder.typicode.com/users/1")
  .then((response) => {
    return response.json();
  })
  .then((user) => {
    console.log(user.name);
  })
  .catch((error: Error) => {
    console.log("エラー:", error.message);
  })
  .finally(() => {
    isLoading = false; // 成功でも失敗でも必ず実行
    console.log("通信終了");
  });
```

### コールバック地獄が解消される

先ほどの入れ子が、チェーンでフラットに書ける：

```ts
// ⇒まず、ユーザーを検索
// ⇒完了後、そのユーザーの投稿を検索
// ⇒完了後、その投稿についてるコメントを検索
// ⇒完了後、そのコメントを書いた人を検索・・・

getUser(1)
  .then((user) => {
    return getPosts(user.id);
  })
  .then((posts) => {
    return getComments(posts[0].id);
  })
  .then((comments) => {
    return getAuthor(comments[0].authorId);
  })
  .then((author) => {
    console.log(author.name);
  })
  .catch((error: Error) => {
    console.log("どこかで失敗:", error.message);
  });
```

---

## 4. async / await

### 概要

`async` / `await` は Promise をさらに読みやすく書くための構文。
内部的には Promise と同じ仕組みで動いている。

| キーワード | 役割 |
|---|---|
| `async` | 関数を「Promise を返す関数」にする |
| `await` | Promise が解決されるまで待ち、値を取り出す |

### 基本の書き方

```ts
interface User {
  name: string;
}

const getUser = async (): Promise<User> => {
  const response = await fetch("https://jsonplaceholder.typicode.com/users/1");
  const user: User = await response.json();
  console.log(user.name); // "Leanne Graham"
  return user;
};

getUser();
console.log('ブロッキングされずに実行される');
```

`await` を使うと、Promise の結果が出るまで**その関数の中だけ**止まって待ってくれる。
※ブラウザ全体が固まるわけではない。正確にはawait以降がcallbackとして予約に連なる。

### async/await でエラー処理

`.catch()` の代わりに `try/catch` が使える。

```ts
const getUser = async (): Promise<User | null> => {
  try {
    const response = await fetch("https://存在しないURL.example.com/api");
    const user: User = await response.json();
    return user;
  } catch (error) {
    console.log("エラー:", (error as Error).message);
    return null;
  } finally {
    console.log("通信終了"); // 成功でも失敗でも実行
  }
};

getUser();
```

通常の `try/catch` と同じ感覚で書けるのが利点。

### Promise チェーンとの比較

同じ処理を2つの書き方で：

```ts
// Promise チェーン
const fetchUserName = (id: number): Promise<string | void> => {
  return fetch(`https://jsonplaceholder.typicode.com/users/${id}`)
    .then((res) => res.json())
    .then((user: User) => user.name)
    .catch((err: Error) => console.log(err));
};

// async/await
const fetchUserName = async (id: number): Promise<string | void> => {
  try {
    const res = await fetch(`https://jsonplaceholder.typicode.com/users/${id}`);
    const user: User = await res.json();
    return user.name;
  } catch (err) {
    console.log(err);
  }
};
```

どちらを使っても動作は同じ。コードの読みやすさで選ぶ。

---

## 5. 実行順序を理解する（イベントループ）⇒省略

「非同期処理はいつ実行されるの？」を理解するための最小限の知識。

### 3つの場所

JSのコードは以下の3か所から順番に取り出されて実行される：

```
① コールスタック    ： 今すぐ実行するコード（同期処理）
② マイクロタスク    ： .then / await の後続処理
③ マクロタスク      ： setTimeout / UIイベント など
```

### 優先順位のルール

```
コールスタック（同期処理）をすべて実行
        ↓
マイクロタスクをすべて実行（.then / await の再開）
        ↓
マクロタスクを1つだけ実行（setTimeout など）
        ↓
また最初に戻る（繰り返し）
```

**マイクロタスク（Promise の .then）はマクロタスク（setTimeout）より先に動く。**

### 実行順序の例

```ts
console.log("1");

setTimeout(() => {
  console.log("4"); // マクロタスク：最後
}, 0);

Promise.resolve()
  .then(() => {
    console.log("3"); // マイクロタスク：setTimeoutより先
  });

console.log("2");

// 出力: 1 → 2 → 3 → 4
```

`setTimeout(fn, 0)` は「0ミリ秒後」だが、マイクロタスク（Promise）より後になる。

### await で止まる位置

```ts
const main = async (): Promise<void> => {
  console.log("A");
  await Promise.resolve();  // ← ここで関数を一時中断、続きをマイクロタスクに積む
  console.log("C");         // ← 再開後に実行
};

main();
console.log("B"); // main が中断している間に実行される

// 出力: A → B → C
```

`await` は「関数の中だけ一時停止して、スレッドを他の処理に渡す」。
`B` は `main` が中断している間に実行される。

---

## 6. よくある落とし穴（JS / TS 編）

### <span style="background:#fff88f">落とし穴①：await のつけ忘れ</span>

```ts
const wrong = async (): Promise<void> => {
  const response = fetch("https://jsonplaceholder.typicode.com/users/1"); // awaitなし！
  const user = response.json(); // responseはPromise。.json()は存在しない → エラー
  console.log(user.name);       // undefinedまたはエラー
};

const correct = async (): Promise<void> => {
  const response = await fetch("https://jsonplaceholder.typicode.com/users/1");
  const user: User = await response.json();
  console.log(user.name); // "Leanne Graham"
};
```

`await` を忘れると Promise オブジェクトそのものが代入される。

### 落とし穴②：逐次 await で遅くなる

```ts
// 遅い：ユーザー取得が終わるまで投稿の取得が始まらない
const slow = async (): Promise<void> => {
  const user = await getUser(1);  // 1秒待つ
  const post = await getPost(1);  // さらに1秒待つ
  // 合計 2秒
};

// 速い：両方同時に開始する
const fast = async (): Promise<void> => {
  const [user, post] = await Promise.all([
    getUser(1),  // 同時に開始
    getPost(1),  // 同時に開始
  ]);
  // 合計 約1秒（長い方）
};
```

互いに依存していない処理は `Promise.all` でまとめて並列実行する。

### 落とし穴③：エラーのキャッチ漏れ

```ts
// catch を忘れると、エラーが握りつぶされることがある
const noHandling = async (): Promise<void> => {
  const data = await fetch("https://存在しないURL.example.com");
  console.log(data); // ここに来ない（エラーが発生しているが、どこにも伝わらない）
};

// 必ず catch する
const withHandling = async (): Promise<void> => {
  try {
    const data = await fetch("https://存在しないURL.example.com");
    console.log(data);
  } catch (error) {
    console.log("エラーをキャッチ:", (error as Error).message);
  }
};
```

非同期関数のエラーは、呼び出し元でも `.catch()` を付けることで捕捉できる：

```ts
withHandling().catch((err: Error) => {
  console.log("呼び出し元でもキャッチできる");
});
```

### <span style="background:#fff88f">落とし穴④：Promise の状態確認ミス</span>

```ts
const checkStatus = async (): Promise<void> => {
  const response = await fetch("https://jsonplaceholder.typicode.com/users/999");

  // fetch は 404 でも rejected にはならない！
  // response.ok を自分でチェックする必要がある
  if (!response.ok) {
    throw new Error(`HTTP エラー: ${response.status}`);
  }

  const user: User = await response.json();
  console.log(user);
};

checkStatus().catch((err: Error) => {
  console.log(err.message); // "HTTP エラー: 404"
});
```

`fetch` はネットワークエラー時のみ reject する（＝errorを投げてくる）。
404 や 500 などのHTTPエラーは `response.ok` で自分でチェックが必要。

---

## 7. よくある落とし穴（React + TypeScript 編）

### <span style="background:#fff88f">落とし穴⑤：useEffect 内で async 関数を直接渡す</span>

`useEffect` のコールバックは「**クリーンアップ関数 または undefined**」を返す必要がある。  
`async` 関数は必ず **Promise を返す** ため、Reactがクリーンアップを正しく処理できない。

```tsx
// useEffectに直接async関数を渡している
useEffect(async () => {
  const res = await fetch("/api/user");
  const data: User = await res.json();
  setUser(data);
}, []);
// useEffect が Promise を受け取ってしまう → クリーンアップが機能しない
```

```tsx
// 内部でasync関数を定義して即呼び出す
useEffect(() => {
  const fetchUser = async (): Promise<void> => {
    const res = await fetch("/api/user");
    const data: User = await res.json();
    setUser(data);
  };

  fetchUser();
}, []);
```

### 落とし穴⑥：アンマウント後の setState

コンポーネントが**画面から消えた後**に非同期処理が完了して `setState` を呼ぶと、
存在しないコンポーネントへの更新が発生する（処理が無駄になる）。

```tsx
// フェッチ中にページ遷移すると、消えた後にsetUserが呼ばれる
useEffect(() => {
  fetch("/api/user")
    .then((res) => res.json())
    .then((data: User) => setUser(data)); // ← アンマウント後に呼ばれることがある
}, []);
```

```tsx
// AbortController でリクエスト自体をキャンセルする
useEffect(() => {
  const controller = new AbortController();

  fetch("/api/user", { signal: controller.signal })
    .then((res) => res.json())
    .then((data: User) => setUser(data))
    .catch(() => {
      // abort時に発生するエラーは無視する
    });

  return () => controller.abort(); // アンマウント時にキャンセル
}, []);
```

### 落とし穴⑦：依存配列と非同期の競合（古いデータで上書き）

依存配列の値が**高速で切り替わる**とき（例：検索ワードが変わるたびにfetch）、
**古いリクエストが後から返ってきて新しいデータを上書き**することがある。

```
ユーザーが "a" を入力  → fetch開始（サーバーが重くて遅い）
ユーザーが "ab" を入力 → fetch開始（すぐ返ってくる）

"ab" の結果が先に返る → 表示: "ab" の結果  ✅
"a"  の結果が後から返る → 表示: "a"  の結果  ❌ 巻き戻る！
```

```tsx
// クリーンアップで前のリクエストをキャンセル
useEffect(() => {
  const controller = new AbortController();

  const search = async (): Promise<void> => {
    try {
      const res = await fetch(`/api/search?q=${query}`, {
        signal: controller.signal,
      });
      const data: SearchResult[] = await res.json();
      setResults(data);
    } catch {
      // abort によるエラーは無視
    }
  };

  search();

  return () => controller.abort(); // queryが変わるたびに前のをキャンセル
}, [query]);
```

> ⑥と⑦はどちらも `AbortController` で解決できる。  
> クリーンアップ関数で `controller.abort()` を呼ぶのが基本パターン。

### 落とし穴⑧：イベントハンドラの async でエラーが握りつぶされる

`onClick` などに `async` 関数を渡すと、内部でエラーが発生しても **React は関知しない**。
Promise が rejected のまま誰にも伝わらず、UIも変化しない。

```tsx
// エラーが握りつぶされる
<button
  onClick={async () => {
    const res = await fetch("/api/broken"); // エラーが起きても...
    setData(await res.json());              // ここに来ないが、誰にも伝わらない
  }}
>
  送信
</button>
```

```tsx
// try/catch でエラーを明示的に処理する
const [error, setError] = useState<string | null>(null);

<button
  onClick={async () => {
    try {
      const res = await fetch("/api/broken");
      setData(await res.json());
    } catch (err) {
      setError("通信に失敗しました"); // UIにエラーを反映する
    }
  }}
>
  送信
</button>
```


⇒<span style="background:#fff88f">fetchAPIの詳細な書き方は講義資料参照</span>
