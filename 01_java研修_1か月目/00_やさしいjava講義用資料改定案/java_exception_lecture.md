# Java研修 例外処理講義
## try-catch-finally / checked・unchecked例外 / throws / カスタム例外 / 多重catch / 再スロー

---

## 全体のテーマ：「銀行ATMの操作システムを作る」

ATM操作では「残高不足」「暗証番号誤り」「ネットワーク障害」など、様々なエラーが発生します。  
これらを適切に処理する仕組みが**例外処理**です。

---

## 0. 例外とは何か

### プログラムが「想定外の状況」に出会ったとき

通常、プログラムは上から順に処理を実行します。  
しかし「残高が足りない」「存在しないファイルを読もうとした」など、**処理を正常に続けられない状況**が起きることがあります。  
このような状況を Java では **例外（Exception）** と呼びます。

### 例外処理がないと何が起きるか

```java
public class Main {
    public static void main(String[] args) {
        int balance = 10000;
        int amount = 50000;

        // 例外処理なし：残高不足のチェックがなければそのまま計算が続く
        int result = balance - amount;
        System.out.println("残高：" + result + "円"); // → 残高：-40000円（異常な状態のまま続く）
    }
}
```

残高がマイナスになるという**異常な状態のまま処理が続いてしまいます。**  
実際のシステムで放置すれば、データの不整合や重大な障害につながります。

例外が発生してもどこにも `catch` がなければ、プログラムは**強制終了**します。

### 例外は「異常な状況をオブジェクトで表したもの」

Java では例外もオブジェクトです。例外が発生すると、Java は**例外オブジェクト**を生成し、プログラムの通常の流れを中断して呼び出し元へ伝えます。

```
通常の流れ：  メソッドA → メソッドB → メソッドC → 正常終了

例外発生時：  メソッドA ← メソッドB ← メソッドC  （例外が呼び出し元へ伝播）
                ↓
           catch があれば対処、なければ強制終了
```

### if文との違い

「残高が足りなければ if で分岐すればよいのでは？」と思うかもしれません。

| | if文による分岐 | 例外処理 |
|---|---|---|
| 対処できる場所 | 同じメソッド内のみ | 呼び出し元など**別の場所**でも対処できる |
| 対処の強制 | しない | checked例外なら**対処を強制できる** |
| エラー情報の伝達 | 戻り値や出力のみ | 例外オブジェクトに詳細情報を持たせられる |
| 複数エラーの使い分け | 分岐が増えて複雑になりがち | 例外の種類ごとに明確に分けられる |

→ **業務システムではエラーの種類が多く、呼び出し元も複数あるため、例外処理が不可欠。**

---

## 1. 例外処理の基本：try-catch-finally

### 考え方

> 「失敗するかもしれない処理」を `try` で囲み、失敗したときの処理を `catch` に書く。  
> `finally` は成功・失敗に関わらず**必ず実行される**。

### 題材：ATMから出金する

```java
public class AtmService {

    public void withdraw(int balance, int amount) {
        try {
            // 失敗するかもしれない処理
            if (amount > balance) {
                throw new Exception("残高不足です（残高：" + balance + "円、出金額：" + amount + "円）");
            }
            System.out.println("出金しました：" + amount + "円");

        } catch (Exception e) {
            // 例外が発生したときの処理
            System.out.println("エラー：" + e.getMessage());

        } finally {
            // 成功・失敗に関わらず必ず実行
            System.out.println("ATM操作を終了します");
        }
    }
}
```

```java
AtmService atm = new AtmService();
atm.withdraw(10000, 3000);  // 成功
System.out.println();
atm.withdraw(10000, 50000); // 残高不足
```

**出力：**
```
出金しました：3000円
ATM操作を終了します

エラー：残高不足です（残高：10000円、出金額：50000円）
ATM操作を終了します
```

### ポイント

- `try`：失敗するかもしれない処理を囲む
- `catch`：例外が発生したときの処理を書く
- `finally`：必ず実行したい処理を書く（ログ出力・接続の後片付けなど）
- `throw`：例外を意図的に発生させる

---

## 2. checked例外 と unchecked例外

### 考え方

Javaの例外には2種類ある。**コンパイラが処理を強制するかどうか**が違い。

| | checked例外 | unchecked例外 |
|---|---|---|
| 継承元 | `Exception` | `RuntimeException` |
| catch / throws | **必須**（書かないとコンパイルエラー） | 任意 |
| 主な用途 | 呼び出し側に対処を求めたいとき | プログラムのバグ・想定外の状態 |
| 例 | `IOException`、独自の業務例外 | `NullPointerException`、`IllegalArgumentException` |

---

### checked例外：呼び出し側に対処を強制する

「残高不足」は呼び出し側が**必ず対処すべき**業務上のエラー。→ checked例外にする。

```java
// checked例外：Exception を継承する
public class InsufficientBalanceException extends Exception {
    public InsufficientBalanceException(String message) {
        super(message);
    }
}
```

```java
// throws で「この例外が発生しうる」ことを宣言する
public void withdraw(int balance, int amount) throws InsufficientBalanceException {
    if (amount > balance) {
        throw new InsufficientBalanceException(
            "残高不足です（残高：" + balance + "円、出金額：" + amount + "円）");
    }
    System.out.println("出金しました：" + amount + "円");
}
```

```java
// 呼び出し側は catch しないとコンパイルエラーになる
try {
    atm.withdraw(10000, 50000);
} catch (InsufficientBalanceException e) {
    System.out.println("エラー：" + e.getMessage());
}
```

---

### unchecked例外：プログラムのバグを表す

「出金額に負の値が渡された」はそもそもプログラムの呼び出し方が間違っている。→ unchecked例外にする。

```java
// unchecked例外：RuntimeException を継承する
public class InvalidAmountException extends RuntimeException {
    public InvalidAmountException(String message) {
        super(message);
    }
}
```

```java
public void withdraw(int balance, int amount) throws InsufficientBalanceException {
    if (amount <= 0) {
        // バグなので RuntimeException → catch の強制なし
        throw new InvalidAmountException("出金額は1円以上で指定してください：" + amount);
    }
    if (amount > balance) {
        throw new InsufficientBalanceException(
            "残高不足です（残高：" + balance + "円、出金額：" + amount + "円）");
    }
    System.out.println("出金しました：" + amount + "円");
}
```

### ポイント

- **業務ルール上のエラー**（残高不足・暗証番号誤りなど）→ checked例外
- **呼び出し方のバグ**（nullを渡した・負の値を渡したなど）→ unchecked例外
- `throws` は「このメソッドはこの例外を投げる可能性がある」という宣言

---

## 3. カスタム例外クラスの作成

### 考え方

> Javaの標準例外だけでは「何のエラーか」が伝わりにくい。  
> 業務に合った名前の例外クラスを作ることで、コードの意図が明確になる。

### ATMで発生しうる例外を整理する

```
Exception
├── InsufficientBalanceException  （残高不足）        ← checked
├── DailyLimitExceededException   （1日の限度額超過）  ← checked
└── RuntimeException
    ├── InvalidAmountException    （不正な金額）        ← unchecked
    └── InvalidPinException       （暗証番号の形式不正）← unchecked
```

```java
// 残高不足（checked）
public class InsufficientBalanceException extends Exception {
    private int balance; // 残高
    private int amount;  // 出金額

    public InsufficientBalanceException(int balance, int amount) {
        super("残高不足です（残高：" + balance + "円、出金額：" + amount + "円）");
        this.balance = balance;
        this.amount = amount;
    }

    public int getBalance() { return balance; }
    public int getAmount()  { return amount; }
}
```

```java
// 1日の限度額超過（checked）
public class DailyLimitExceededException extends Exception {
    public DailyLimitExceededException(int limit) {
        super("1日の出金限度額（" + limit + "円）を超えています");
    }
}
```

```java
// 不正な金額（unchecked）
public class InvalidAmountException extends RuntimeException {
    public InvalidAmountException(int amount) {
        super("出金額は1円以上で指定してください：" + amount);
    }
}
```

### カスタム例外を使ったメソッド

```java
public class AtmService {
    private static final int DAILY_LIMIT = 100000; // 1日の限度額
    private int todayWithdrawn = 0;                // 本日の出金合計

    public void withdraw(int balance, int amount)
            throws InsufficientBalanceException, DailyLimitExceededException {

        if (amount <= 0) {
            throw new InvalidAmountException(amount); // unchecked：throws 宣言不要
        }
        if (amount > balance) {
            throw new InsufficientBalanceException(balance, amount);
        }
        if (todayWithdrawn + amount > DAILY_LIMIT) {
            throw new DailyLimitExceededException(DAILY_LIMIT);
        }

        todayWithdrawn += amount;
        System.out.println("出金しました：" + amount + "円（本日合計：" + todayWithdrawn + "円）");
    }
}
```

---

## 4. 多重catch

### 考え方

> 複数の例外が発生しうるとき、例外の種類ごとに対応を変えられる。

```java
AtmService atm = new AtmService();

try {
    atm.withdraw(10000, 50000); // どのエラーが起きるかはシナリオ次第

} catch (InsufficientBalanceException e) {
    // 残高不足のときだけの対応
    System.out.println("残高不足：あと " + (e.getAmount() - e.getBalance()) + "円 足りません");

} catch (DailyLimitExceededException e) {
    // 限度額超過のときだけの対応
    System.out.println("限度額超過：" + e.getMessage());

} catch (Exception e) {
    // 上記以外の想定外エラー（最後に書く）
    System.out.println("予期せぬエラーが発生しました：" + e.getMessage());

} finally {
    System.out.println("ATM操作を終了します");
}
```

### ポイント

- `catch` は**上から順に**マッチするか判定される
- より具体的な例外を**上に**、より汎用的な例外（`Exception`）を**下に**書く
- 逆にすると「具体的な catch に到達しない」バグになる

#### ❌ 順序が逆（バグ）

```java
} catch (Exception e) {              // 全部ここで拾われてしまう
    System.out.println("エラー");
} catch (InsufficientBalanceException e) { // ← 絶対に到達しない！
    System.out.println("残高不足");
}
```

---

## 5. 例外の再スロー

### 考え方

> 一度 `catch` した例外を、**さらに上の呼び出し元へ投げ直す**。  
> 「ここでは対処しきれないので、上位に任せる」ときに使う。

### 題材：ATM処理を管理する上位サービス

```java
// 下位：ATM操作の処理
public class AtmService {
    public void withdraw(int balance, int amount)
            throws InsufficientBalanceException, DailyLimitExceededException {
        // ...（前章と同じ）
    }
}
```

```java
// 上位：ATM操作を呼び出す窓口
public class BankService {

    private AtmService atm = new AtmService();

    public void processWithdrawal(int balance, int amount) throws InsufficientBalanceException {

        try {
            atm.withdraw(balance, amount);

        } catch (DailyLimitExceededException e) {
            // 限度額超過はここで対処（ログを出してから再スロー）
            System.out.println("[銀行システム] 限度額超過を検知しました：" + e.getMessage());
            throw new InsufficientBalanceException(balance, amount); // 別の例外に変換して再スロー

        } catch (InsufficientBalanceException e) {
            // 残高不足はそのまま呼び出し元へ投げ直す
            System.out.println("[銀行システム] 残高不足を検知しました");
            throw e; // 再スロー
        }
    }
}
```

```java
// 最上位：画面や操作端末に近い層
BankService bank = new BankService();

try {
    bank.processWithdrawal(10000, 50000);
} catch (InsufficientBalanceException e) {
    System.out.println("[ATM画面] " + e.getMessage());
    System.out.println("[ATM画面] 金額を変更してもう一度お試しください");
}
```

**出力：**
```
[銀行システム] 残高不足を検知しました
[ATM画面] 残高不足です（残高：10000円、出金額：50000円）
[ATM画面] 金額を変更してもう一度お試しください
```

### ポイント

- `throw e` でそのまま再スロー
- `throw new XxxException(...)` で別の例外に変換して再スロー（例外の**ラップ**）
- 再スローするときは `throws` の宣言も必要
- 「**どの層で何を知っているか**」に応じて、対処する場所を分ける

---

## 6. まとめ

```
try {
    // ① 失敗するかもしれない処理
    throw new InsufficientBalanceException(...);  // ② 例外を投げる

} catch (InsufficientBalanceException e) {        // ③ 具体的な例外を先に
    throw e;                                       //    再スロー

} catch (DailyLimitExceededException e) {         // ④ 別の例外
    // 対処する

} catch (Exception e) {                           // ⑤ 汎用は最後
    // 想定外

} finally {                                       // ⑥ 必ず実行
    // 後片付け
}
```

| 概念 | 一言まとめ |
|---|---|
| try-catch-finally | 失敗するかもしれない処理を囲み、失敗時の処理と後片付けを書く |
| checked例外 | 呼び出し側に対処を強制する（業務ルール上のエラー） |
| unchecked例外 | 対処を強制しない（プログラムのバグ・想定外の呼び出し） |
| throws | このメソッドはこの例外を投げる可能性があると宣言する |
| カスタム例外 | 業務に合った名前の例外クラスを作り、意図を明確にする |
| 多重catch | 例外の種類ごとに対応を変える。具体的な例外を上に書く |
| 再スロー | catch した例外を上位に投げ直す。層ごとに責任を分ける |

---

## 演習課題

### 課題1：try-catch-finally
暗証番号の入力を検証するメソッドを作成してください。  
暗証番号が4桁でない場合に例外を投げ、`finally` で「暗証番号の検証を終了します」と出力すること。

### 課題2：checked / unchecked例外
以下の2つの例外クラスを作成してください。

- `AccountFrozenException`（口座凍結）→ checked例外
- `NullAccountException`（口座番号がnull）→ unchecked例外

### 課題3：多重catch
課題2で作った例外を使い、口座からの出金処理を実装してください。  
`AccountFrozenException` と `NullAccountException` それぞれで異なるメッセージを出力すること。  
catch の順序を意図的に逆にしたときにどうなるか確認すること。

### 課題4：再スロー
`AtmService.withdraw()` を呼び出す `BankService.processWithdrawal()` を実装してください。  
`InsufficientBalanceException` をキャッチしてメッセージを出力したあと、そのまま再スローすること。
