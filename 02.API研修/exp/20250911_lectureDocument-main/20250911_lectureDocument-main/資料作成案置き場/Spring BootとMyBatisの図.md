## Spring BootとMyBatisにおけるコンポーネント関係図

この資料は、Spring Bootアプリケーションの内部で、各コンポーネント（Controller, Service, Mapperなど）やフレームワークが、どのように連携して動作するのか、その技術的な関係性を示したものです。

### 1. 全体アーキテクチャと制御の流れ

アプリケーション全体の構造は、Spring Bootの**IoCコンテナ**を中心に構成されます。IoCコンテナが各コンポーネント（Bean）の生成と管理を行い、必要に応じてそれらを結合（依存性の注入）します。

-   **IoCコンテナ (Inversion of Control Container)**:
    -   アプリケーションの骨格となる中心的な存在。
    -   `@Controller`, `@Service`, `@Component`などのアノテーションが付いたクラスのインスタンス（**Bean**）を、起動時に自動で生成し、管理します。
-   **依存性の注入 (Dependency Injection - DI)**:
    -   あるコンポーネントが別のコンポーネントを必要とする場合（例：ControllerがServiceを利用する）、IoCコンテナが適切なBeanを自動的に割り当てます。開発者は`new`を使ってインスタンスを生成する必要がありません。
```mermaid
graph TD
    subgraph SpringBoot["Spring Boot Runtime Environment"]
        direction TB
        A["IoCコンテナ / ApplicationContext"]

        subgraph Beans["Managed Beans"]
            direction LR
            B["Controller<br>@Controller"]
            C["Service<br>@Service"]
            D["Mapper<br>@Mapper"]
        end
    end

    subgraph Frameworks["Framework Integration"]
        E["MyBatis"]
    end

    %% IoCによるBean生成・管理
    A -- "生成・管理" --> B
    A -- "生成・管理" --> C
    A -- "生成・管理" --> D

    %% DI関係
    B -- "DI" --> C
    C -- "DI" --> D
    D -- "連携" --> E

    classDef container fill:#eaf7ff,stroke:#0366d6,stroke-width:2px;
    classDef bean fill:#f7fbf0,stroke:#2f9e44,stroke-width:1.5px;
    classDef external fill:#fff7e6,stroke:#ff8c00,stroke-width:1.5px;

    class A container;
    class B,C,D bean;
    class E external;

```
```mermaid
graph TD
    subgraph "Spring Boot Runtime Environment"
        A("IoCコンテナ (ApplicationContext)")

        subgraph "Application Components (Managed Beans)"
            direction LR
            B(Controller) -- DI --> D(Service)
            D -- DI --> E(Mapper)
        end
        
        A -- "生成・管理" --> B
        A -- "生成・管理" --> D
        A -- "生成・管理" --> E
    end

    subgraph "External Libraries"
        F(MyBatis)
    end
    
    subgraph "Client / Database"
        G[Client]
        H[(Database)]
    end
    
    %% 外部との連携
    G -- "1. HTTP Request" --> B
    B -- "8. HTTP Response" --> G
    
    E -- "MyBatis経由で連携" --> F
    F -- "SQL実行" --> H

    classDef spring fill:#e9f5e9,stroke:#28a745,stroke-width:2px
    classDef app fill:#e6f3ff,stroke:#007bff,stroke-width:2px
    class A,F spring
    class B,D,E app
```

### 2. リクエスト処理のシーケンス図

クライアントからリクエストが送られてから、レスポンスが返されるまでの一連の流れです。各コンポーネントは、上位層から下位層へと一方向に処理を依頼します。

```mermaid
sequenceDiagram
    participant Client
    participant DispatcherServlet
    participant Controller
    participant Service
    participant Mapper
    participant Database

    Client->>+DispatcherServlet: 1. HTTPリクエスト
    DispatcherServlet->>+Controller: 2. URLに紐づくメソッドを呼び出し
    Controller->>+Service: 3. ビジネスロジックを依頼
    Service->>+Mapper: 4. DB操作をメソッドで依頼
    Mapper->>+Database: 5. SQLを実行
    Database-->>-Mapper: 6. 実行結果を返す
    Mapper-->>-Service: 7. 結果をEntityにマッピングして返す
    Service-->>-Controller: 8. 処理結果を返す
    Controller-->>-DispatcherServlet: 9. レスポンスデータを返す
    DispatcherServlet-->>-Client: 10. HTTPレスポンス
```

### 3. MyBatisの役割とMapperの動作原理

`Mapper`はJavaの`interface`（インターフェース）として定義しますが、その実装クラスを開発者が書くことはありません。MyBatisがアプリケーション起動時に、このインターフェースを実装したプロキシ（代理）オブジェクトを動的に生成し、IoCコンテナにBeanとして登録します。

-   `Mapper`インターフェースのメソッドが呼び出されると、プロキシオブジェクトがその呼び出しを検知します。
-   プロキシは、メソッド名に対応するSQL文をXMLファイルから探し出し、引数をSQLのパラメータに設定して実行します。

```mermaid
graph TD
    subgraph "Java Code (アプリケーション層)"
        A_UserService -- "メソッド呼び出し" --> B_UserMapper;
    end

    subgraph "MyBatis Runtime"
        C(MyBatis Proxy Object) -- "インターフェースを実装" --> B;
        C -- "対応するSQLを探索・実行" --> D(UserMapper.xml);
    end

    subgraph "Database"
        E[(DB)]
    end
    
    D -- "SQL Statement" --> C
    C -- "JDBC" --> E

    classDef java fill:#e6f3ff,stroke:#007bff,stroke-width:2px
    classDef mybatis fill:#fff0e6,stroke:#fd7e14,stroke-width:2px
    class A,B java
    class C,D mybatis
```

### 4. データオブジェクトの変換フロー

各層で扱われるデータオブジェクト（DTO）は、その層の責務に応じて定義されます。層をまたぐ際には、責務に応じたオブジェクトへの変換が行われます。

-   **Form**: プレゼンテーション層の責務。クライアントからの入力形式とバリデーションルールを定義します。
-   **Entity**: データアクセス層の責務。データベースのテーブル構造を忠実に反映します。

```mermaid
graph LR
    subgraph "Presentation Layer (Controller)"
        A[HTTP Request JSON] --> B(Form Object);
        B --> C{Data Transformation};
        C --> D(Entity Object);
    end
    
    subgraph "Business & Data Access Layer (Service, Mapper)"
        D --> E(Entity Object);
    end
    
    subgraph "Database Layer"
        F[(Database Row)]
    end
    
    %% 双方向のマッピング
    E <== "MyBatisによるマッピング" ==> F

    %% レスポンスの流れ
    G[HTTP Response_JSON]
    E --> H{Response Generation};
    H --> G

    classDef presentation fill:#e6f3ff,stroke:#007bff,stroke-width:2px
    classDef business fill:#e9f5e9,stroke:#28a745,stroke-width:2px
    classDef data fill:#fff0e6,stroke:#fd7e14,stroke-width:2px
    
    class A,B,C,G,H presentation
    class D,E business
    class F data
```



```mermaid
graph TD
    subgraph クライアント
        A[ブラウザ/モバイルアプリ]
    end

    subgraph "Spring Bootアプリケーション"
        subgraph "フレームワークが提供"
            B[組み込みTomcat]
            C{DispatcherServlet}
            G[MyBatisエンジン]
            H[Mapper XML]
        end

        subgraph "開発者が実装するプログラム"
            D[Controllerクラス<br>@RestController]
            E[Serviceクラス<br>@Service]
            F[Mapperインターフェース<br>@Mapper]
        end
        
        %% リクエストフロー
        B --> C;
        C -- URLに応じて振り分け --> D;
        D -- ビジネスロジックを依頼 --> E;
        E -- DB操作を依頼 --> F;
        F -- 連携 --> G;
        H -- SQL定義を読み込み --> G;
        G -- SQLを実行 --> I[(データベース)];
        
        %% レスポンスフロー
        I -- 実行結果 --> G;
        G -- 結果をオブジェクトにマッピング --> F;
        F -- 結果を返す --> E;
        E -- 結果を返す --> D;
        D -- 結果(Javaオブジェクト)を返す --> C;
        C -- HTTPレスポンス<br>(JSON等に変換) --> A;
    end
    
    subgraph データベース
        I
    end

    A -- HTTPリクエスト --> B;

    style D fill:#cde4ff
    style E fill:#cde4ff
    style F fill:#cde4ff
```