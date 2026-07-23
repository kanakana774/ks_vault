# Lesson 11 - オブジェクト指向の三原則と実践パターン
## 1.抽象クラス
「だいたい共通だけど、肝心なところは子クラスで決めてね」という未完成のクラスです。

#### 文法ルール
- クラス名の前に **`abstract`** を付ける。
- **`new` してインスタンス化することはできない**（未完成だから）。
- **抽象メソッド**：メソッドの前に `abstract` を付け、中身 `{ }` を書かずに `;` で終わらせる。

#### コード例
```java
// 抽象クラス：中身のないメソッドを一つでも持つなら、クラス自体もabstractにする
public abstract class Character {
    protected String name;

    // 共通の処理（実装）を持つことができる
    public void showName() {
        System.out.println("名前は" + name);
    }

    // 抽象メソッド：具体的な攻撃方法は子クラスに強制させる
    public abstract void attack(); 
}
```

## 2.インターフェース
「中身（データ）はどうでもいいから、この機能（メソッド）だけは必ず持っててね」という**ルール**（**規約**）です。

#### 文法ルール
- `class` の代わりに **`interface`** と書く。
- 原則として、**中身のあるメソッドやフィールドは持てない**（※定数などは可）。
- 使う側は `extends` ではなく **`implements`**（実装する）と書く。
- **多重継承が可能**：複数のインターフェースを同時に守らせることができる。

#### コード例
```java
// インターフェース：この免許を持つなら、必ず「save」ボタンが押せること
public interface Saveable {
    void save(); // 自動的に public abstract になる
}

// 実装：インターフェースというルールに従う
public class Player extends Character implements Saveable {
    @Override
    public void attack() { System.out.println("斬りつける"); }

    @Override
    public void save() { System.out.println("セーブしました"); }
}
```

### 文法的な違いまとめ

| 項目 | 抽象クラス | インターフェース |
| :--- | :--- | :--- |
| **キーワード** | `abstract class` | `interface` |
| **継承/実装** | `extends` (1つだけ) | `implements` (**複数OK**) |
| **中身のあるメソッド** | 持てる | 原則持てない |
| **フィールド（変数）** | 持てる | 持てない（定数のみ） |
| **目的** | 共通部分をまとめて楽をする | 呼び出し方のルールを共通化する |

---
## 3. カプセル化（復習）
**目的：データの整合性を守り、壊れにくい部品にする**
フィールドを `private` にし、専用のメソッドを「門番」として機能させます。

### 良くない例（無防備な状態）
```java
public class Player {
    public int level; // 誰でも、どんな値でも入れられてしまう
}

// 利用側
Player p = new Player();
p.level = -999; // 業務ルール上ありえない数値だが、防げない
```

### 良い例（カプセル化）
```java
public class Player {
    private int level = 1;

    /** レベルを更新する専用メソッド */
    public void updateLevel(int level) {
        // 門番：正しいデータだけを通す
        if (level >= 1 && level <= 99) {
            this.level = level;
        } else {
            System.out.println("エラー：レベルは1〜99の間で設定してください");
        }
    }

    public int getLevel() {
        return this.level;
    }
}
```

<font color="#ff0000">※注意： 実務では getter/setter はツールで自動生成することが多いため、基本的に中身は書き換えません。上記のようなチェック機能が必要な場合は、`updateLevel` のように別の適切な名前を付けたメソッドを実装するのがよいでしょう。</font>

---


## 4. 継承
**目的：重複を排除し、共通機能を一括管理する**
似たようなクラスが複数ある場合、共通部分を「親（スーパークラス）」にまとめます。

### 良くない例（同じコードを何度も書く）
```java
public class Warrior {
    String name; // 重複
    int hp;      // 重複
    void attack() {
     ... 
    }
}

public class Wizard {
    String name; // 重複
    int hp;      // 重複
    void attack() {
     ... 
    }
}
```

### 良い例（継承の利用）
```java
// 親：共通のデータと動きを定義
public class Character {
    protected String name; 
    protected int hp;

    public void attack() {
        System.out.println(name + "の通常攻撃！");
    }
}

// 子：差分（自分だけの機能）だけを書く
public class Warrior extends Character {
    @Override // オーバーライド
    public void attack() {
        System.out.println(name + "の強烈な剣撃！"); 
    }
}
```

**発展：**
 最近は、継承は「密結合（影響が大きすぎる）」になりやすいため、必要最小限に留め、**「合成（他のクラスを部品として持つ）」や「委譲（処理を任せる）」を使うことが推奨されます。まずは基本の継承を学び、慣れてきたらこれらのキーワードを調べてみてください。

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

> オーバーライドの例は継承の例を見せる。

---

## 5. ポリモーフィズム
**目的：呼び出し側をシンプルにし、将来の変更に強くする**
相手の正体が何であれ、共通の「型（共通規格）」でひとまとめに扱います。
スマホを使う際に、OSがiosなのかandroidなのか仕組みを意識しなくても同じような共通規格（インターフェース）のおかげで、電話をかけることができますよね？

### 活用法①：リストにまとめて一括処理する
```java
List<Character> party = new ArrayList<>();
party.add(new Warrior("戦士"));
party.add(new Wizard("魔法使い"));

for (Character c : party) {
    c.attack(); // 相手が誰でも、Character型の共通ボタン「attack」を押すだけ
}
```
使う側はCharacterができること（＝定義されてるメソッド）の呼び方だけ知っていれば中身の実装を知らずに呼び出すことができます。

### 活用法②：【最重要】メソッドの引数で使う
これがポリモーフィズムの真価です。メソッドの受け口を「親の型」にすることで、どんな子クラスが来ても受け入れられる「汎用的な**窓口**」になります。

#### 良くない例（ポリモーフィズムなし）
新職業が増えるたびに、宿屋クラスに新しいメソッドを追加し続けなければなりません。
```java
public class Inn {
    public void stay(Warrior w) {
	    w.setHp(100);
	}
    public void stay(Wizard w) {
	    w.setHp(100); 
	}
    // 職業が増えるたびにここを改造するの…？
}
```

#### 良い例（ポリモーフィズム活用）
引数を親の型（Character）にすれば、1つのメソッドで未来永劫、すべての職業に対応できます。
```java
public interface Character {

}

public class Warrior implements Character {

}
```

```java
public class Inn {
    // 「Characterの規格」に合うものなら誰でも泊まれる窓口
    public void stay(Character c) {
        System.out.println(c.getName() + "は宿に泊まった");
        c.setHp(100); 
    }
}
```

```java
List<Character> party = new ArrayList<>();
party.add(new Warrior("剣士"));
party.add(new Wizard("魔法使い"));

Inn inn = new Inn();
for (Character c : party) {
    inn.stay(c);
}
```


---

## 6. 適切なパターンの選び方（逆引き）

### ケース1：複数のクラスで「名前」や「ID」が被っている
- **選ぶ機能**：**継承（extends）**
- **理由**：共通部分は親に1回書くだけ。修正時も親を直すだけで全員に反映されます。

### ケース2：職業によって「攻撃」の演出だけ変えたい
- **選ぶ機能**：**オーバーライド（@Override）**
- **理由**：呼び出し側は `c.attack()` と変えずに、中身だけを個性化できます。

### ケース3：同じ処理を、誰に対しても行いたい
- **選ぶ機能**：**ポリモーフィズム（メソッドの引数を親の型にする）**
- **理由**：呼び出す側が「相手が誰か」を気にする必要がなくなり、コードが劇的にシンプルになります。

### ケース4：全く違うモノ同士（例：人間と車）に同じ「走る」という命令を出したい
- **選ぶ道具**：**インターフェース（interface）**
- **理由**：親子関係はないが、「走れる機能」だけを共通化したい場合に最適です。

---

## 7. まとめ

「**自分にしか読めない独創的なコード**」は書かないようにしましょう。

1.  **カプセル化**でデータを守り、
2.  **継承**で無駄な記述を削り、
3.  **ポリモーフィズム**で「使い方のルール」を共通化する。
