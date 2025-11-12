package diDemo1;

/**
 * Repositoryに依存してるServiceクラス
 */
public class Service {

    /**
     * repository呼び出し処理
     */
    public void callRepository() {

        // 他のクラス（Repository）を呼び出すときの例
        Repository repository = new Repository();
        boolean result = repository.callDb();

        // 結果表示
        System.out.println("結果：" + result);
    }
}
