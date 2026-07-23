### 画面遷移の方法

Reactアプリケーションでルーティングを実現するためのライブラリに、React Routerがあります。  
useNavigateはReact Routerの提供するフックの一つで、プログラム的にページ遷移を実行するために使われます。

```typescript
// インポートします
import { useNavigate } from "react-router-dom";
```

```typescript
// navigate関数を受け取り
const navigate = useNavigate();
```
react hooksなのでトップレベルに書くように。

```typescript
// 好きなタイミングで画面遷移
navigate("/path");
```

### ログインユーザーの取得
画面でログインユーザーIDを取得する際は下記のようにして取得してください。
`useGetLoginUserId()`は、研修側で用意してある関数です。ログインユーザーを取得できます。
カスタムフックと呼ばれるユーザーオリジナルのフックになります。
react hooksなのでトップレベルに書くように。
```typescript
// 保存しているログインユーザIDを取得
const loginUserId = useGetLoginUserId();
```

実装は下記のようになります。
ポイントは、フロントエンド画面は認証情報にはノータッチな点です。
あくまで、過去にログインした際に取得したユーザーIDをブラウザに保存していて、そのユーザーが現在ログインしてる前提でリクエストを飛ばします。
```typescript
// useGetLoginUserId.ts

import { useNavigate } from 'react-router-dom';

/**
 * localStorageに保存したユーザーIDを取得する関数
 */
export const useGetLoginUserId = (): string => {
  const navigate = useNavigate();
  const loginUserId = localStorage.getItem('loginUserId'); // ブラウザのlocalStorageに保存されてるユーザー名を確認してます
  if (loginUserId === null) { // userIdが保存されてなければ、ログイン画面に遷移
    navigate('/');
  }
  return loginUserId ?? '';
};

```

#### ログイン画面のログイン処理
```typescript
// Login.tsx

  //ログイン処理
  const onClickLogin = useCallback(async () => {
    try {
      await login(userId, password); // 
      // ログインユーザIDを取得し、メニュー画面に遷移
      const loginUser = await getLoginUser();
      localStorage.setItem('loginUserId', loginUser.userId);
      navigate('/menu');
    } catch (error) {
      if (error instanceof Error) {
        //スローされたエラーのメッセージをセット
        setErrorMessageDisplay(error.message);
      }
    }
  }, [navigate, password, userId]);

```

#### `login`関数の実装
ログインのエンドポイントに向けてユーザーIDとパスワードを飛ばします。
spring-security側で`status：304`でheaderのlocationのパスまでデフォルトでリダイレクトをブラウザにさせますが、それを無視します。
ただ、認証情報である`sessionId`は発行してブラウザに持たせるため、セッションが切れない限り、リクエストに`sessionId`を付与して送ります。
```typescript
// login.ts

export const login = async (userId: string, password: string): Promise<Response> => {
  try {
    const res = await fetch(`${import.meta.env.VITE_API_ENDPOINT}/login`, {
      method: 'POST',
      credentials: 'include',
      body: `userId=${userId}&password=${password}`,
      redirect: 'manual',
      headers: {
        'Content-Type': 'application/x-www-form-urlencoded',
      },
    });
    if (res.status !== 0) {
      throw new Error(API_ERROR_MESSAGE[res.status]);
    }
    return res;
  } catch (error) {
    if (error instanceof TypeError) {
      throw new Error(uiMessage['ui.error.network']);
    }
    // tryで明示的にthrowしたエラーはそのままthrowする
    throw error;
  }
};
```

#### `getLoginUser`の実装
今ログイン中のユーザーを取得します。
```typescript
// loginUser.ts

export const getLoginUser = async (): Promise<{ userId: string }> => {
  try {
    const res = await fetch(`${import.meta.env.VITE_API_ENDPOINT}/login-users`, {
      method: 'GET',
      credentials: 'include',
    });
    if (!res.ok) {
      throw new Error(API_ERROR_MESSAGE[res.status]);
    }
    return await res.json();
  } catch (error) {
    if (error instanceof TypeError) {
      throw new Error(uiMessage['ui.error.network']);
    }
    // tryで明示的にthrowしたエラーはそのままthrowする
    throw error;
  }
};
```


### 定数コンポーネントの活用
```typescript
// errorMessage.ts

/**
 * APIからエラーレスポンスが返却されたときに表示するメッセージ
 */
export const API_ERROR_MESSAGE: { [key: number]: string } = {
  400: 'リクエストが不正です。',
  401: 'ユーザIDまたはパスワードが違います。',
  403: '権限がありません。',
  404: '存在しません。',
  408: 'タイムオーバーです。',
  409: 'ほかのユーザによって更新されています。ページを再度読み込んでください。',
  500: 'サーバーエラー',
} as const;

/**
 * クライアント側で制御するメッセージ
 */
export const uiMessage = {
  'ui.validation.empty': (target: string) => `${target}が未入力です。`,
  'ui.validation.invalid-size': (target: string, minNumber: number, maxNumber: number) =>
    `${target}は${minNumber}文字以上${maxNumber}文字以下で入力してください。`,
  'ui.validation.password-invalid-pattern': 'パスワードはアルファベットの大文字・小文字と数字を組み合わせてください。',
  'ui.validation.password-same-user': 'ユーザIDと同じパスワードは使用できません。',
  'ui.validation.mismatch': (source: string, destination: string) => `${source}と${destination}が一致しません。`,
  'ui.info.no-result': (target: string) => `${target}は現在ありません。`,
  'ui.info.complete-update': (target: string) => `${target}の更新が完了しました。`,
  'ui.info.message-reached-max-limit':
    'メッセージが最大件数に達しています。「メッセージ」メニューから、不要なメッセージは削除してください。',
  'ui.error.network': '通信エラーが発生しました',
} as const;

```

まず、使うときはimportが必要
```typescript
import { API_ERROR_MESSAGE, uiMessage } from '../../../base/constants/errorMessage';
```

例えば、400番のエラーメッセージが欲しければ、下記のように。
```typescript
const message = API_ERROR_MESSAGE[400]; // 'リクエストが不正です。'
```
[インデックス型について](https://typescriptbook.jp/reference/values-types-variables/object/index-signature)

メッセージ
```typescript
const message = uiMessage['ui.validation.empty']('タイトル'); // `タイトルが未入力です。`
const message = uiMessage['ui.error.network']; // '通信エラーが発生しました'
```


### fetchAPIのURLの指定の仕方
`.env`ファイルでまとめてURLを管理しています。
使用の際は下記のように。

```ts
const res = await fetch(`${import.meta.env.VITE_API_ENDPOINT}/users/${loginUserId}?`);
```

### 既存コンポーネントの利用
下記コンポーネントが予め用意されてるみたいです。
見た目に統一感が出ます。使い方など考えてみてください。

- CommonHeader
- Main
- Button

### フォルダ構成について
- components
	- UI（＝見た目）に関する関数コンポーネントの置き場
- usecase
	- ビジネスロジック（＝ビジネス上の操作単位）に関する処理の置き場
		- 例：
			- todoを登録する
			- messageを送る

下記のような関数をreactから使います。
```ts
// message.ts

/**
 * fetch API(未読メッセージ取得)
 * @param loginUserId
 * @returns
 */
export const getUnreadMessages = async (loginUserId: string): Promise<MessageShowList[]> => {
  try {
    const res = await fetch(
      `${import.meta.env.VITE_API_ENDPOINT}/users/${loginUserId}?` + new URLSearchParams({ read_status: '0' }),
      {
        method: 'GET',
        credentials: 'include',
      },
    );
    if (res.ok) {
      console.log(res.json());
      return await res.json();
    } else {
      throw new Error(API_ERROR_MESSAGE[res.status]);
    }
  } catch (error) {
    if (error instanceof TypeError) {
      throw new Error(uiMessage['ui.error.network']);
    }
    // tryで明示的にthrowしたエラーはそのままthrowする
    throw error;
  }
};
```

見た目（react）とビジネスロジック（ts）を分離することで、下記のようなメリットがあります。（＝変更に強くなる）
- ドメインロジックのテストが容易
- React 以外でもロジックを再利用可能

[参考](https://zenn.dev/yoshi333/articles/b25d7ff4915a57)