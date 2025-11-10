package diDemo3;

/**
 * Serviceに依存されてるRepositoryクラス（テスト用のstubクラス）
 */
public class RepositoryStub extends Repository {

    /**
     * DBアクセス
     */
    @Override
    public boolean callDb() {
        System.out.println("スタブが呼び出されました。");
        System.out.println("ダミーを返します。");
        return true; // serviceをテストしたいのでDBからの処理結果はダミーを返せばよい
    }
}
