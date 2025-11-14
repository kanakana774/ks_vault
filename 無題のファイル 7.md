SpringのBean（ビーン）とは、**Springコンテナによってインスタンスの生成や管理が行われるJavaオブジェクト**のことです。 通常のJavaオブジェクト（POJO: Plain Old Java Object）を、Springフレームワークが特定の役割を持たせて管理するものがBeanと呼ばれます。

Beanを理解する上で重要なのが、**DI（Dependency Injection：依存性の注入）**という考え方です。

### Beanの役割とメリット

Springで開発を行う際、クラスAがクラスBの機能を利用したい場合、通常はクラスAの中で`new`を使ってクラスBのインスタンスを生成します。

```java
// 通常のオブジェクト生成
public class ClassA {
    private ClassB classB = new ClassB(); // ClassAがClassBに依存している

    public void someMethod() {
        classB.doSomething();
    }
}
```

しかし、この方法ではクラスAとクラスBが密接に結びつきすぎてしまい（密結合）、単体テストがしにくくなったり、将来的な仕様変更に対応しにくくなったりします。

SpringのBeanは、この問題を解決します。クラスをBeanとしてSpringコンテナに登録しておくと、開発者が`new`でインスタンスを生成する代わりに、Springが必要な場所で自動的にインスタンスを「注入（Injection）」してくれます。

```java
// Spring (DI) を使った場合
@Component // ClassAをBeanとして登録
public class ClassA {
    @Autowired // SpringにClassBのインスタンスを注入してもらう
    private ClassB classB;

    public void someMethod() {
        classB.doSomething();
    }
}

@Component // ClassBをBeanとして登録
public class ClassB {
    public void doSomething() {
        // ...
    }
}
```

このようにすることで、以下のようなメリットが生まれます。

*   **依存関係の管理:** Springコンテナがオブジェクトの生成と依存関係の解決を行ってくれるため、コードがシンプルになり、保守性が向上します。
*   **疎結合:** クラス間の結合度が下がり、各コンポーネントが独立しやすくなるため、単体テストが容易になります。
*   **再利用性の向上:** オブジェクトのライフサイクルをSpringに任せることで、コードの再利用性が高まります。

### Beanのスコープ

Beanは、そのインスタンスがどの範囲で共有されるかを「スコープ」で定義できます。 主なスコープには以下の種類があります。

| スコープ | 説明 |
| :--- | :--- |
| **singleton** | （デフォルト）Springコンテナ内で**インスタンスが1つだけ**生成され、アプリケーション全体で共有されます。設定情報やユーティリティクラスなど、状態を持たないクラスに適しています。 |
| **prototype** | Beanがリクエストされるたびに**毎回新しいインスタンスが生成**されます。状態を持つ（ステートフルな）クラスに適しています。 |
| **request** | （Webアプリケーションのみ）1つのHTTPリクエストの間だけ有効なインスタンスが生成されます。 |
| **session** | （Webアプリケーションのみ）1つのユーザーセッションの間だけ有効なインスタンスが生成されます。ログイン情報など、ユーザーごとのデータ管理に適しています。 |
| **application** | （Webアプリケーションのみ）Webアプリケーション全体で1つのインスタンスが共有されます。 |

これらのスコープを適切に使い分けることで、Beanのライフサイクルを効率的に管理し、アプリケーションをより柔軟に設計することができます。