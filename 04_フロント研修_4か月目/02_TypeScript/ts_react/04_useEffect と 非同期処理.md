第4回は、React開発で最も間違いやすく、かつ重要な**「副作用（Side Effects）の管理」と「非同期処理（API通信）」**について解説します。

Javaではオブジェクトの生成時やメソッド呼び出しで逐次処理を行いますが、Reactでは「レンダリング（見た目の描画）」と「外部との接続（API通信など）」を明確に分ける必要があります。

---

# 第4回：useEffect と 非同期処理
**〜外部データとの連携とライフサイクル管理〜**

## 1. 副作用（Side Effect）とは？

Reactにおける「副作用」とは、**コンポーネントのレンダリング結果を返すこと以外の処理**を指します。
- 例：APIからのデータ取得、タイマーの設定、手動でのDOM操作、ログ出力。

これらを `useEffect` フックの中に閉じ込めることで、Reactのレンダリングサイクルを壊さずに実行できます。

---

## 2. `useEffect` の基本構文 [JS]

`useEffect` は、第1引数に実行したい関数、第2引数に「実行のタイミングを制御する配列（依存配列）」を渡します。

### サンプルコード
```tsx
import { useEffect, useState } from 'react';

const EffectExample = () => {
  const [count, setCount] = useState(0);

  // コンポーネントが描画された後に実行される
  useEffect(() => {
    console.log("レンダリングされました");

    // 依存配列 [count] を指定しているため、count が変わるたびに実行される
  }, [count]); 

  return (
    <button onClick={() => setCount(count + 1)}>
      Count: {count}
    </button>
  );
};
```

### 比較：依存配列による挙動の違い
| 第2引数（依存配列） | 実行タイミング | 主な用途 |
| :--- | :--- | :--- |
| **なし** (`useEffect(() => ... )`) | **毎回のレンダリング後** | 基本的に使わない（無限ループのリスクあり） |
| **空の配列** (`[]`) | **初回レンダリング後の1回のみ** | APIからの初期データ取得、イベントリスナー登録 |
| **変数の配列** (`[count]`) | **初回 + その変数が変化した時** | 特定のデータに連動した処理（検索ワードが変わったら再取得など） |

---

## 3. 非同期処理（API通信）の実装 [JS/TS]

Javaの `Future` や `CompletableFuture` に相当するのが、JSの `Promise` と `async/await` です。

### サンプルコード（JSONPlaceholderを使用したデータ取得）
```tsx
import { useState, useEffect } from 'react';

// 1. APIレスポンスの型を定義 [TS]
type Post = {
  id: number;
  title: string;
  body: string;
};

const PostList = () => {
  const [posts, setPosts] = useState<Post[]>([]);
  const [loading, setLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    // useEffectの中で async関数を定義するのが一般的
    const fetchPosts = async () => {
      try {
        setLoading(true);
        const response = await fetch('https://jsonplaceholder.typicode.com/posts');
        
        if (!response.ok) throw new Error('Network response was not ok');

        const data: Post[] = await response.json();
        setPosts(data.slice(0, 5)); // 最初の5件だけ保存
      } catch (err) {
        setError(err instanceof Error ? err.message : 'Unknown error');
      } finally {
        setLoading(false);
      }
    };

    fetchPosts();
  }, []); // 初回のみ実行

  if (loading) return <p>Loading...</p>;
  if (error) return <p>Error: {error}</p>;

  return (
    <ul>
      {posts.map(post => (
        <li key={post.id}><strong>{post.title}</strong></li>
      ))}
    </ul>
  );
};
```

---

## 4. クリーンアップ関数（後片付け） [JS]

タイマー（`setInterval`）や監視（WebSocketなど）を行う場合、コンポーネントが破棄される時に「後片付け」をしないと、メモリリークの原因になります。

### サンプルコード
```tsx
useEffect(() => {
  const timer = setInterval(() => {
    console.log("1秒経過");
  }, 1000);

  // return で関数を返すと、コンポーネントが消える(unmount)時に実行される
  return () => {
    clearInterval(timer);
    console.log("タイマーを停止しました");
  };
}, []);
```

---

## 5. 比較：fetch API vs Axios [JS]

HTTPクライアントとして、標準の `fetch` と外部ライブラリの `axios` がよく比較されます。

| 特徴 | `fetch` (標準機能) | `Axios` (外部ライブラリ) |
| :--- | :--- | :--- |
| **導入** | 不要（ブラウザ標準） | `npm install axios` が必要 |
| **JSON変換** | `res.json()` の呼び出しが必要 | 自動で変換される |
| **エラー判定** | HTTP 404/500でも `catch` に入らない | 400/500番台で自動的に `catch` に入る |
| **推奨度** | 小規模なコードなら十分 | **実務開発ではこちらが主流** |

---

## 6. ベストプラクティスとアンチパターン

### ✅ ベストプラクティス
1.  **データの状態（Loading, Error, Data）をセットで管理する**: ユーザーに「今何が起きているか」を伝えるため。
2.  **useEffectを小さく保つ**: 1つの `useEffect` に複数の役割（API取得とタイマー開始など）を持たせず、用途ごとに分ける。
3.  **カスタムフックへの抽出**: API通信ロジックが長くなったら、別ファイルに切り出す（JavaでいうService層への切り出しに近い）。

### ❌ アンチパターン
1.  **依存配列を嘘をつく**: `useEffect` 内で使っている変数を依存配列に入れないと、古いデータ（クロージャの罠）を参照し続け、バグの原因になります。
2.  **useEffect内での無限ループ**: `useEffect` の中で依存配列に含まれる変数を更新すると、 `更新 -> 発火 -> 更新...` と無限ループします。
3.  **何でも `useEffect` で解決しようとする**: Propsから計算できる値は、わざわざ `useEffect` と `state` を使わずに、関数の本体で変数として計算してください。

---

## まとめ：

1.  **「レンダリング」は純粋に**: 関数のトップレベルでAPIを叩いてはいけません（再描画のたびにAPIが飛んでしまいます）。必ず `useEffect` を使います。
2.  **非同期の例外処理**: `try-catch` を忘れないでください。フロントエンドではネットワーク切断やタイムアウトが頻繁に起こります。
3.  **型安全なAPI通信**: `await response.json() as Post[]` のように、外部から来たデータに型を当てることで、それ以降のコードでエディタの補完が効くようになります。

