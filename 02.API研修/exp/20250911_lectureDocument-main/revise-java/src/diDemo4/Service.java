package diDemo4;

/**
 * Serviceに依存してるControllerクラス
 */
public class Service {

    // 依存クラス
    private final Repository repository; // コンストラクタで初期化するので、finalを付けて変更できないようにすることができる

    // 依存性注入
    public Service(Repository repository) { // 外部から依存してるクラスを注入することができる
        this.repository = repository;
    }

    /**
     * service呼び出し処理
     */
    public void callRepository() {

        // 他のクラス（service）を呼び出すときの例
        boolean result = repository.callDb();

        System.out.println("結果：" + result);
    }
}
