# Java研修 OOP基礎講義
## 抽象クラス・インタフェース・カプセル化・継承・ポリモーフィズム

---

## 全体のテーマ：「ECサイトの通知システムを作る」

注文確定・発送完了・キャンセルなどのイベントを顧客に通知するシステムを例に、OOPの各概念を順番に学びます。

---

## 1. カプセル化（Encapsulation）

### 考え方

> フィールドを `private` にして外から直接触らせない。読み書きは getter / setter 経由に統一する。

「なぜ隠すのか？」→ **不正な値の混入を防ぎ、クラス自身が状態を守るため。**

### ❌ カプセル化なし

```java
public class OrderNotification {
    public String customerEmail;
    public int orderId;
}

OrderNotification notif = new OrderNotification();
notif.customerEmail = "これはメアドではない"; // 不正な値が入ってしまう
notif.orderId = -1;                           // 負のIDが入ってしまう
```

### ✅ カプセル化あり

```java
public class OrderNotification {
    private String customerEmail;
    private int orderId;

    public void setCustomerEmail(String email) {
        if (email == null || !email.contains("@")) {
            throw new IllegalArgumentException("メールアドレスの形式が正しくありません");
        }
        this.customerEmail = email;
    }

    public void setOrderId(int orderId) {
        if (orderId <= 0) {
            throw new IllegalArgumentException("注文IDは1以上で指定してください");
        }
        this.orderId = orderId;
    }

    public String getCustomerEmail() { return customerEmail; }
    public int getOrderId()          { return orderId; }
}
```

```java
OrderNotification notif = new OrderNotification();
notif.setCustomerEmail("これはメアドではない"); // → 例外発生
notif.setCustomerEmail("tanaka@example.com");   // → OK
notif.setOrderId(-1);                           // → 例外発生
notif.setOrderId(1001);                         // → OK
```

### ポイント

- `private` フィールド ＋ getter/setter がカプセル化の基本形
- setter にバリデーションを入れて「クラスが自分の状態を守る」

---

## 2. 継承（Inheritance）

### 考え方

> 共通処理を**親クラス**にまとめ、**子クラス**が引き継ぐ。

### 題材：注文確定・発送完了・キャンセルはどれも「通知」

```java
// 親クラス：全通知に共通する情報と処理
public class Notification {
    protected int orderId;
    protected String customerName;

    public Notification(int orderId, String customerName) {
        this.orderId = orderId;
        this.customerName = customerName;
    }

    public void printHeader() {
        System.out.println("========================================");
        System.out.println("宛先   ：" + customerName + " 様");
        System.out.println("注文ID ：" + orderId);
        System.out.println("========================================");
    }
}
```

```java
// 子クラス：注文確定通知
public class OrderConfirmedNotification extends Notification {
    private String itemName;

    public OrderConfirmedNotification(int orderId, String customerName, String itemName) {
        super(orderId, customerName);
        this.itemName = itemName;
    }

    public void send() {
        printHeader();
        System.out.println("【注文確定】ご注文商品：" + itemName);
    }
}
```

```java
// 子クラス：発送完了通知
public class ShippedNotification extends Notification {
    private String trackingNumber;

    public ShippedNotification(int orderId, String customerName, String trackingNumber) {
        super(orderId, customerName);
        this.trackingNumber = trackingNumber;
    }

    public void send() {
        printHeader();
        System.out.println("【発送完了】追跡番号：" + trackingNumber);
    }
}
```

```java
OrderConfirmedNotification confirmed = new OrderConfirmedNotification(1001, "田中太郎", "ワイヤレスイヤホン");
confirmed.send();
```

**出力：**
```
========================================
宛先   ：田中太郎 様
注文ID ：1001
========================================
【注文確定】ご注文商品：ワイヤレスイヤホン
```

### ポイント

- `extends` で継承、`super(...)` で親のコンストラクタを呼ぶ
- `protected` フィールドは子クラスから直接アクセスできる

---

## 3. オーバーライドとオーバーロード

### 3-1. オーバーライド（Override）

> **親クラスのメソッドを子クラスで上書きする。** 同じメソッド名・同じ引数で再定義する。

親クラスに `send()` を定義し、各子クラスで上書きする。

```java
public class Notification {
    // ...（フィールド・コンストラクタ・printHeader は同じ）

    // 子クラスがオーバーライドするメソッド
    public void send() {
        System.out.println("（通知を送信します）");
    }
}
```

```java
public class OrderConfirmedNotification extends Notification {
    private String itemName;
    // ...コンストラクタ省略

    @Override // オーバーライドであることを明示（必ず書く）
    public void send() {
        printHeader();
        System.out.println("【注文確定】ご注文商品：" + itemName);
    }
}
```

```java
public class CancelledNotification extends Notification {
    private String reason;
    // ...コンストラクタ省略

    @Override
    public void send() {
        printHeader();
        System.out.println("【キャンセル】理由：" + reason);
    }
}
```

---

### 3-2. オーバーロード（Overload）

> **同じクラス内で、同じメソッド名・異なる引数のメソッドを複数定義する。**

#### 比較表

| | オーバーライド | オーバーロード |
|---|---|---|
| 場所 | 親クラスと子クラス | 同じクラス内 |
| 引数 | 同じ | **違う** |
| 目的 | 動作を上書き | 呼び出し方を増やす |

#### 題材：通知確認メッセージの出力

```java
public class NotificationPrinter {

    // 注文IDだけ表示
    public void print(int orderId) {
        System.out.println("注文ID：" + orderId + " の通知を送信しました");
    }

    // 注文IDと通知種別を表示（引数が違う → オーバーロード）
    public void print(int orderId, String type) {
        System.out.println("注文ID：" + orderId + "【" + type + "】を送信しました");
    }
}
```

```java
NotificationPrinter printer = new NotificationPrinter();
printer.print(1001);          // → 注文ID：1001 の通知を送信しました
printer.print(1001, "注文確定"); // → 注文ID：1001【注文確定】を送信しました
```

### ポイント

- オーバーライド：**縦の関係**（親→子）、動作を置き換える
- オーバーロード：**横の関係**（同クラス内）、呼び出し方を増やす

---

## 4. 抽象クラス（Abstract Class）

### 考え方

> 「必ずこのメソッドを実装してね」と子クラスに**義務付ける**仕組み。自分自身はインスタンス化できない。

### いつ使う？

- 共通の処理は親クラスに持たせたい
- でも一部の処理は**子クラスに必ず実装させたい**

```java
public abstract class Notification {
    protected int orderId;
    protected String customerName;

    public Notification(int orderId, String customerName) {
        this.orderId = orderId;
        this.customerName = customerName;
    }

    // 共通処理：そのまま使える
    public void printHeader() {
        System.out.println("宛先：" + customerName + " 様 / 注文ID：" + orderId);
    }

    // 抽象メソッド：子クラスに実装を義務付ける
    public abstract void send();
    public abstract String getNotificationType();
}
```

```java
public class OrderConfirmedNotification extends Notification {
    private String itemName;

    public OrderConfirmedNotification(int orderId, String customerName, String itemName) {
        super(orderId, customerName);
        this.itemName = itemName;
    }

    @Override
    public void send() {
        printHeader();
        System.out.println("【" + getNotificationType() + "】商品：" + itemName);
    }

    @Override
    public String getNotificationType() { return "注文確定"; }
}
```

```java
// ❌ 抽象クラスはインスタンス化できない
// Notification n = new Notification(1001, "田中"); // コンパイルエラー！
```

### ポイント

- `abstract class` / `abstract` メソッドで宣言
- 子クラスは抽象メソッドを**全部実装**しないとコンパイルエラー

---

## 5. インタフェース（Interface）

### 考え方

> 「このメソッドを持っていることを保証する」という**契約**。  
> 抽象クラスと異なり、**複数実装（多重実装）が可能**。

### 抽象クラスとの違い

| | 抽象クラス | インタフェース |
|---|---|---|
| フィールド | 持てる | 定数のみ |
| コンストラクタ | 持てる | 持てない |
| 多重継承 | 不可 | **複数実装可能** |
| 向いている表現 | 「〜である」(is-a) | 「〜できる」(can-do) |

---

### 基本：インタフェース単体で使う

注文が確定したとき、**メール・SMS・プッシュ通知**など複数のチャンネルで顧客に届けたい。  
チャンネルごとに送り方は異なるが「通知を送れる」という能力は共通。

```java
// インタフェース：通知を送れる
public interface Notifiable {
    void send(String destination, String message);
}
```

```java
public class EmailNotifier implements Notifiable {
    @Override
    public void send(String destination, String message) {
        System.out.println("[メール] 宛先：" + destination + " / 本文：" + message);
    }
}
```

```java
public class SmsNotifier implements Notifiable {
    @Override
    public void send(String destination, String message) {
        // SMSは文字数制限があるため先頭20文字に絞る
        String shortMsg = message.length() > 20 ? message.substring(0, 20) + "…" : message;
        System.out.println("[SMS] 電話番号：" + destination + " / 本文：" + shortMsg);
    }
}
```

```java
public class PushNotifier implements Notifiable {
    @Override
    public void send(String destination, String message) {
        System.out.println("[プッシュ] トークン：" + destination + " / " + message);
    }
}
```

---

### 多重実装：複数のインタフェースを同時に実装する

「通知を送れる」かつ「配信停止の設定を受け付けられる」クラスを作りたい場合。

```java
// インタフェース①：通知を送れる（先ほどと同じ）
public interface Notifiable {
    void send(String destination, String message);
}

// インタフェース②：配信停止を受け付けられる
public interface Unsubscribable {
    void unsubscribe(String destination);
}
```

```java
// メール通知：送信も配信停止も両方できる（多重実装）
public class EmailNotifier implements Notifiable, Unsubscribable {

    @Override
    public void send(String destination, String message) {
        System.out.println("[メール] 宛先：" + destination + " / 本文：" + message);
    }

    @Override
    public void unsubscribe(String destination) {
        System.out.println("[配信停止] " + destination + " をメール配信停止リストに追加しました");
    }
}
```

```java
EmailNotifier notifier = new EmailNotifier();

// Notifiable 型として扱える
Notifiable n = notifier;
n.send("tanaka@example.com", "ご注文が確定しました");

// Unsubscribable 型としても扱える
Unsubscribable u = notifier;
u.unsubscribe("tanaka@example.com");
```

**出力：**
```
[メール] 宛先：tanaka@example.com / 本文：ご注文が確定しました
[配信停止] tanaka@example.com をメール配信停止リストに追加しました
```

### ポイント

- `interface` で定義し、`implements` で実装する
- 複数インタフェースは `implements Notifiable, Unsubscribable` のようにカンマ区切りで指定する

---

## 6. ポリモーフィズム（Polymorphism）

### 考え方

> **同じ型で異なるオブジェクトを扱い、呼び出し時に適切な処理が自動で実行される。**  
> 呼び出し側がいちいち種類を気にしなくていい。

---

### パターン①：引数をインタフェース型にする

注文が確定したとき、「どのチャンネルで送るか」を外から渡せるようにする。

```java
public class OrderService {

    // 引数を Notifiable 型にする → どのチャンネルが来ても同じ処理でOK
    public void confirmOrder(int orderId, String itemName, Notifiable notifier) {
        System.out.println("注文ID：" + orderId + " を確定しました");
        notifier.send("tanaka@example.com", "【注文確定】" + itemName + " のご注文を受け付けました");
    }
}
```

```java
OrderService service = new OrderService();

// メールで送る
service.confirmOrder(1001, "ワイヤレスイヤホン", new EmailNotifier());

// SMSで送る
service.confirmOrder(1001, "ワイヤレスイヤホン", new SmsNotifier());

// プッシュ通知で送る
service.confirmOrder(1001, "ワイヤレスイヤホン", new PushNotifier());
```

**出力：**
```
注文ID：1001 を確定しました
[メール] 宛先：tanaka@example.com / 本文：【注文確定】ワイヤレスイヤホン のご注文を受け付けました
注文ID：1001 を確定しました
[SMS] 電話番号：tanaka@example.com / 本文：【注文確定】ワイヤレスイヤホン…
注文ID：1001 を確定しました
[プッシュ] トークン：tanaka@example.com / 【注文確定】ワイヤレスイヤホン のご注文を受け付けました
```

`OrderService` は `EmailNotifier` も `SmsNotifier` も知らなくていい。  
**「通知を送れるもの（`Notifiable`）」が来ることだけを知っていればいい。**

---

### パターン②：新しい通知種別を追加しても呼び出し側を変えなくていい

```java
// 新しい通知種別（Notification の抽象クラスを継承）
public class ReviewRequestNotification extends Notification {
    private String itemName;

    public ReviewRequestNotification(int orderId, String customerName, String itemName) {
        super(orderId, customerName);
        this.itemName = itemName;
    }

    @Override
    public void send() {
        printHeader();
        System.out.println("【" + getNotificationType() + "】「" + itemName + "」のレビューをお願いします");
    }

    @Override
    public String getNotificationType() { return "レビューのお願い"; }
}
```

```java
List<Notification> notifications = new ArrayList<>();
notifications.add(new OrderConfirmedNotification(1001, "田中太郎", "ワイヤレスイヤホン"));
notifications.add(new ShippedNotification(1001, "田中太郎", "TRK-9876543"));
notifications.add(new ReviewRequestNotification(1001, "田中太郎", "ワイヤレスイヤホン")); // 追加するだけ

for (Notification n : notifications) {
    n.send(); // 呼び出し側は何も変えなくていい
}
```

### ポイント

- 引数をインタフェース型・親クラス型にすることで、**渡すオブジェクトを後から自由に差し替えられる**
- 新しい種類を追加しても**呼び出し側のコードを変更しなくていい** → 拡張しやすい設計

---

## 7. まとめ

```
【通知イベントテーマ（1〜4章・6章パターン②）】

         ┌──────────────────────────┐
         │       <<abstract>>       │
         │        Notification      │
         │  + printHeader()         │
         │  + send()        [抽象]  │
         │  + getNotificationType() [抽象] │
         └────────────┬─────────────┘
                      │ extends
          ┌───────────┼────────────────┐
          ▼           ▼               ▼
   OrderConfirmed  Shipped       Cancelled
   Notification    Notification  Notification


【通知チャンネルテーマ（5章・6章パターン①）】

  <<interface>>       <<interface>>
   Notifiable          Unsubscribable
  + send()            + unsubscribe()
       ↑ implements        ↑ implements
  EmailNotifier  ─────────────┘（多重実装）
  SmsNotifier
  PushNotifier

  ↑ ポリモーフィズム（パターン①）：
    OrderService の引数を Notifiable 型にすることで
    どのチャンネルが来ても同じコードで動く
```

| 概念 | 一言まとめ | キーワード |
|---|---|---|
| カプセル化 | 内部を隠してバリデーションで守る | `private` + getter/setter |
| 継承 | 共通処理を親にまとめて再利用 | `extends` / `super` |
| オーバーライド | 親のメソッドを子で上書き | `@Override`、縦の関係 |
| オーバーロード | 同クラスで引数違いの同名メソッド | 横の関係、引数の型・数が異なる |
| 抽象クラス | 実装を子クラスに義務付ける | `abstract class` / `abstract` |
| インタフェース | 能力・役割を契約として定義 | `interface` / `implements`、多重実装可能 |
| ポリモーフィズム | 同じ型で異なる動作、呼び出し側を変えなくていい | 引数をインタフェース型・親クラス型にする |

---

## 演習課題

### 課題1：カプセル化
`ShippedNotification` の `trackingNumber` フィールドをカプセル化してください。  
setter では「`null` でないこと、かつ `"TRK-"` で始まること」をバリデーションすること。

### 課題2：オーバーロード
`NotificationPrinter` に「送信先メールアドレスも一緒に表示できる」オーバーロードを追加してください。

```java
printer.print(1001, "注文確定", "tanaka@example.com"); // 追加するオーバーロード
```

### 課題3：継承・抽象クラス・ポリモーフィズム
`Notification` を継承した `ReviewRequestNotification` を作成し、`send()` と `getNotificationType()` を実装してください。  
作成後、`List<Notification>` に追加して一括送信できることを確認すること。

### 課題4：インタフェース
以下の `Retryable` インタフェースを定義し、`SmsNotifier` に実装してください。

```java
public interface Retryable {
    void retry(String destination); // 失敗した送信をリトライする
}
```

実装の `retry()` では「`[SMS再送信]` 送信先：（宛先）」と出力すること。
