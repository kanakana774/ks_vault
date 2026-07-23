# Lesson 11 - オブジェクト指向の三原則と実践パターン

## 1. 抽象クラス (abstract)
「だいたい共通だけど、肝心な部分は役職や種類ごとに後で決めてね」という未完成のクラスです。

#### 文法ルール
- **`abstract`** をクラス名の前に付ける。
- **抽象メソッド**：メソッドの前に `abstract` を付け、中身 `{ }` を書かずに `;` で終わらせる。子クラスに「実装（中身の記述）」を強制させます。
- 使う側は`extends`と書きます。
- **`new` してインスタンス化することはできない**（未完成なので実体化できない）。

#### 例：通知機能（メールやLINEで送り方が違う）
システムからユーザーにメッセージを送る際、「件名を表示する」といった処理は共通ですが、「実際にどう送るか」は手段によって異なります。

**抽象クラス：**
```java
// 抽象クラス：通知の設計図（これ単体では送り方がわからないので未完成）
public abstract class Notification {
    protected String message;

    public Notification(String message) {
        this.message = message;
    }

    // 共通の処理
    public void showPreview() {
        System.out.println("--- 通知内容のプレビュー ---");
        System.out.println("内容：" + this.message);
    }

    // 抽象メソッド
    // 具体的な送り方は子クラス（メールやSNS）に強制させる
    public abstract void send(); 
}
```

**具象クラス：**
```java
// 子クラス1：メール通知
public class EmailNotification extends Notification {
    public EmailNotification(String message) {
        super(message); // 親のコンストラクタを呼び出す
    }

    @Override
    public void send() {
        System.out.println("メールサーバーを経由して送信しました。");
    }
}

// 子クラス2：SNS通知
public class SnsNotification extends Notification {
    public SnsNotification(String message) {
        super(message);
    }

    @Override
    public void send() {
        System.out.println("アプリのAPIを使用してプッシュ通知を送信しました。");
    }
}
```

#### Mainメソッドでの使用例
```java
public class Main {
    public static void main(String[] args) {
        // 1. 抽象クラスは new できない
        // Notification n = new Notification("こんにちは"); 

        // 2. 子クラスをインスタンス化する
        EmailNotification email = new EmailNotification("明日の会議は10時からです。");
        SnsNotification sns = new SnsNotification("メッセージが届きました！");

        // 3. 共通メソッドの呼び出し
        email.showPreview();
        // 4. 上書きしたメソッドの呼び出し
        email.send();

        sns.showPreview();
        sns.send();
    }
}
```

---

## 2. インターフェース (interface)
「種類（親子関係）はどうでもいいから、この**機能**（**メソッド**）だけは必ず持っていてね」という**共通規格**（**能力**）です。

#### 文法ルール
- `class` の代わりに **`interface`** と書く。
- フィールドは持てない。
- **抽象メソッド**：メソッドの前に `abstract` を付け、中身 `{ }` を書かずに `;` で終わらせる。子クラスに「実装（中身の記述）」を強制させます。
- 使う側は`implements`（実装する）と書きます。
- **多重継承が可能**：1つのクラスに複数のルール（免許）を持たせられます。
- **`new` してインスタンス化することはできない**（未完成なので実体化できない）。

#### 実例：レビュー機能（星をつけて評価できる）
サイトでは「商品」に対して星をつけて評価しますが、「出品者（ショップ）」や「配送業者」に対しても評価をすることがあります。
**「商品」と「出品者」は全く別の種類**（**親子ではない**）ですが、どちらも「**レビューを受け付ける**」という共通の機能（能力）を持たせます。

**インタフェース：**
```java
// インターフェース：この「免許」を持つなら、必ず評価を受けられるようにしてね
public interface Reviewable {
    void postReview(int star); // 星の数（1〜5）を受け取る
}
```

**抽象クラス：**
```java
// 抽象クラス：サイト内のコンテンツ（これ単体では中身が決まらないので未完成）
public abstract class EcContent {
    protected String title;

    public EcContent(String title) {
        this.title = title;
    }

    // 共通の処理：タイトルの表示
    public void showTitle() {
        System.out.println("【表示中】" + this.title);
    }

    // 抽象メソッド：ページの表示（具体的な中身は子クラスで決める）
    public abstract void showPage(); 
}
```

**実装/具象クラス：**
```java
// 1. 商品（EcContentを継承しつつ、レビュー免許も持つ）
public class Product extends EcContent implements Reviewable {
    public Product(String title, int price) {
        super(title);
    }

    @Override
    public void showPage() {
	    System.out.println("商品詳細を表示します。");
	}

    @Override
    public void postReview(int star) {
        System.out.println("商品「" + this.title + "」に星" + star + "を付けました。");
    }
}

// 2. 出品者（コンテンツではないが、レビュー免許を持つ）
public class Seller implements Reviewable {
    private String shopName;

    public Seller(String shopName) {
        this.shopName = shopName;
    }

    @Override
    public void postReview(int star) {
        System.out.println("出品者「" + this.shopName + "」に星" + star + "を付けました。");
    }
}
```
#### 評価受付システム（呼び出し側）
```java
public class ReviewSystem {
    // 引数にインターフェースを指定！
    // 「Reviewableを実装しているものなら、商品でも出品者でも何でもOK」
    public void receiveReview(Reviewable target, int star) {
        System.out.println("[システム] レビューを受け付けました。");
        target.postReview(star); // 相手が何であれ、postReviewを持っていることが保証されている（＝ポリモーフィズム）
    }
}
```

#### Mainメソッドでの使用例
```java
public class Main {
    public static void main(String[] args) {
        ReviewSystem system = new ReviewSystem();

        // 商品と出品者（種類が全く違う2つのインスタンス）
        Product tv = new Product("4Kテレビ", 50000);
        Seller shop = new Seller("ヨドバシカメラ");

        // 同じ「評価窓口（メソッド）」で、どちらも処理できる！
        system.receiveReview(tv, 5);
        system.receiveReview(shop, 4);
    }
}
```

> ここで示すべきは、インタフェースが抽象クラスとどう異なるか。ポリモーフィズムは一旦飛ばす。

### 文法的な違いまとめ

| 項目            | 抽象クラス            | インターフェース                |
| :------------ | :--------------- | :---------------------- |
| **キーワード**     | `abstract class` | `interface`             |
| **継承/実装**     | `extends` (1つだけ) | `implements` (**複数OK**) |
| **中身のあるメソッド** | 持てる              | **原則**持てない              |
| **フィールド（変数）** | 持てる              | 持てない（定数のみ）              |
| **目的**        | 共通部分をまとめて楽をする    | 呼び出し方のルールを共通化する         |

---

## 3. カプセル化（データの保護）
**目的：外部からの「想定外の操作」を防ぎ、バグを防ぐ**

### 良くない例：誰でも書き換えられる
```java
public class Employee {
    public int salary; // 誰でもマイナス100万などを入れられてしまう
}
```

### 良い例：チェック機能付きの窓口
```java
public class Employee {
    private int salary = 200000;

    /** 給与を更新する（マイナスは受け付けない） */
    public void updateSalary(int salary) {
        if (salary > 0) {
            this.salary = salary;
        } else {
            System.out.println("エラー：不正な金額です");
        }
    }

    public int getSalary() {
        return this.salary;
    }
}
```

**コツ：** 
単純な読み書き（getter/setter）はツールで自動生成し、特別なルールがある場合のみ独自のメソッド（`updateSalary`など）を作ることが多いです。

<font color="#ff0000">※注意：getter/setter はツールで自動生成することが多いため、基本的に中身は書き換えません。
</font>ただ、それだと入力を検証できないためカプセル化ができません。そのためgetter/setterは現代ではかなりアンチパターンですが、実務ではまだ使われるため覚えておきましょう。

---

## 4. 継承 (extends)
**目的：共通の「属性（データ）」と「振る舞い」を1箇所にまとめ、重複を排除する**

複数のクラスで共通する項目がある場合、それを「親（スーパークラス）」に逃がすことで、子クラスは「自分だけの差分」に集中できます。

### 良くない例：同じようなクラスをバラバラに作る
「事務員」と「外回り」のクラスを別々に作った場合です。名前やIDの定義、出勤の処理が全く同じなのに、2箇所に書かなければなりません。

```java
// 事務員クラス
public class OfficeWorker {
    protected String name;    // 重複
    protected String staffId; // 重複

    public void clockIn() {
	    System.out.println(name + "が出勤しました");
	} // 重複
    public void work() {
	    System.out.println("事務作業をします");
	}
}

// 外回りクラス
public class SalesWorker {
    protected String name;    // 重複
    protected String staffId; // 重複

    public void clockIn() {
	    System.out.println(name + "が出勤しました");
	} // 重複
    public void work() {
	    System.out.println("営業に行きます");
	}
}
```
**この設計の欠点：**
もし「出勤時に時刻も記録するように仕様変更して」と言われたら、**すべてのクラスを修正して回る**必要があり、修正漏れ（バグ）の原因になります。

### 良い例：共通部分を「親」にまとめる
「どちらもスタッフ（Staff）である」という共通点を見つけ、共通部分を親クラスへ逃がします。

```java
// 親：共通のデータと動きを定義
public class Staff {
    protected String name;
    protected String staffId;

    public Staff(String name, String staffId) {
        this.name = name;
        this.staffId = staffId;
    }

    public void clockIn() {
        System.out.println("ID:" + staffId + " " + name + "が出勤しました");
    }

    public void work() {
        System.out.println("業務を行います");
    }
}

// 子：自分の「個性」だけを書く（extends）
public class SalesWorker extends Staff {
    public SalesWorker(String name, String staffId) {
        super(name, staffId); // 親のコンストラクタを呼び出す
    }

    @Override
    public void work() {
        System.out.println("営業へ出かけます"); // 動きを上書き（オーバーライド）
    }
}
```

### Mainメソッドでの使用例（実務的な型の使い方）
ここで重要なのは、「**左側の変数定義の型は広く、右側の実体は詳しく**」という書き方です。

```java
public class Main {
    public static void main(String[] args) {
        // 1. 普通の書き方
        SalesWorker sw = new SalesWorker("田中", "S001");
        sw.clockIn(); // 親の機能が使える
        sw.work();    // 自分の機能が動く

        // 2. 実務的な書き方（親の型に格納する）
        // 「SalesWorker は Staff の一種」なので、Staff型の変数に入れられます
        Staff staff = new SalesWorker("佐藤", "S002");

        // staff変数は「Staff型」なので、Staffが持っているメソッドは確実に呼べることが保証される
        staff.clockIn();
        staff.work(); // 実際に動くのは SalesWorker の work() 
    }
}
```

> **講師メモ：なぜわざわざ「Staff staff = ...」と書くのか？**
> 使う側（Main）が「彼は事務員か？営業か？」と細かく気にしなくても、**「Staff（社員）なら誰でも出勤（clockIn）できて、仕事（work）ができるはずだ」**という共通の扱いができるからです。
> 
> 変数の型をあえて「親」にしておくことで、将来新しい職種が増えても、使う側のコード（Main）を書き換えずに済むようになります。これが次で学ぶ「ポリモーフィズム」の基礎です。


---

### オーバーライド と オーバーロード
名前が似ていますが、全く別の道具です。混同しないよう整理しましょう。

| 特徴          | **オーバーライド**（上書き）   | **オーバーロード**（多重定義）   |
| :---------- | :----------------- | :------------------ |
| **関係**      | **親クラス vs 子クラス**   | **同じクラス内**          |
| **仕組み**     | 親のメソッドを「書き換える」     | シグネチャが違う「同名メソッド」を作る |
| **目的**      | 子独自の動きに変更するため      | 呼び出す側の利便性を高めるため     |
| **アノテーション** | **必須：`@Override`** | なし                  |

### オーバーロードの例（同じクラス内）
```java
public class Calculator {
    // 整数同士の足し算
    public int add(int a, int b) {
	    return a + b;
	}
    
    // 小数同士の足し算（名前が同じでも引数が違えばOK）
    public double add(double a, double b) {
	    return a + b;
	}
}
```

> オーバーライド、オーバーロードの例を見せる。
> オーバーライドの例は継承の例を見せる。

---

## 5. ポリモーフィズム (多態性)
**目的：呼び出し側をシンプルにし、将来の変更に強くする**
これがオブジェクト指向の最大のメリットです。**「相手の具体的な正体が何であれ、共通の型（窓口）で扱う」**考え方です。

### 活用例：レジの支払いシステム
「現金」「クレジットカード」「QR決済」…支払い方は増え続けますが、レジ側は「支払う（pay）」という共通ボタンだけ知っていればOK、という状態を作ります。

#### インターフェースの準備
```java
public interface Payment {
    void pay(int amount);
}
```
#### 具体的な支払い方法
```java
public class Cash implements Payment {
    public void pay(int amount) {
	    System.out.println("現金で" + amount + "円払いました");
	}
}

public class CreditCard implements Payment {
    public void pay(int amount) {
	    System.out.println("カードで" + amount + "円払いました"); 
	}
}
```


#### ポリモーフィズムの真価（呼び出し側）
支払い方法が増えても、この `Register` クラスは**1文字も修正する必要がありません。**

```java
// レジ
public class Register {
    // 引数をインターフェース（共通規格）にする
    public void checkout(Payment payment, int price) {
        System.out.println("会計を開始します");
        payment.pay(price); // 相手が「現金」か「カード」かは気にせず、payボタンを押すだけ
    }
}
```

#### mainメソッドでの利用
```java
public class Main {
	public static void main(String[] args) {
	    
		Register reg = new Register();
		
		Payment p1 = new Cash();
		Payment p2 = new CreditCard();
		
		reg.checkout(p1, 1000); // 現金払い
		reg.checkout(p2, 5000); // カード払い
    }
}
```

