package diDemo2;

/**
 * Repositoryに依存してるServiceクラス
 */
public class Service {

    // 依存クラス
    private final Repository repository; // コンストラクタで初期化するので、finalを付けて変更できないようにすることができる

    // コンストラクタでインスタンス生成
    public Service() {
        this.repository = new Repository();
    }

    /**
     * service呼び出し処理
     */
    public void callService() {

        // 他のクラス（service）を呼び出すときの例
        boolean result = repository.callDb();

        // 結果表示
        System.out.println("結果：" + result);
    }
}
