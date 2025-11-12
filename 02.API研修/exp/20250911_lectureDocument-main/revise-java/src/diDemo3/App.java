package diDemo3;

/**
 * 使う側
 */
public class App {

    /**
     * エントリポイント
     * 
     * @param args
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {

        // インスタンス生成
        Repository repository = new Repository();
        Service service = new Service(repository); // 依存性注入することで動的にクラスを入れ替えることができる
        // 呼び出し
        service.callRepository();

        // 例えばテストするとき
        Repository repositoryStub = new RepositoryStub();
        Service serviceTest = new Service(repositoryStub); // 依存性注入することで動的にクラスを入れ替えることができる（テストをしよう）
        // 呼び出し
        serviceTest.callRepository();

    }
}
