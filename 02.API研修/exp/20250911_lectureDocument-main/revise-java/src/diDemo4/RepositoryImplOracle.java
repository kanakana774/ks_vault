package diDemo4;

import java.util.Random;

/**
 * Oracle用とか
 */
public class RepositoryImplOracle implements Repository {

    /**
     * controllerに呼び出される処理
     * 
     * @return
     */
    public boolean callDb() {
        System.out.println("Repositoryが呼び出されました。");
        System.out.println("～～～～～～～～～～～～～～");
        System.out.println("DB呼び出し処理");
        System.out.println("～～～～～～～～～～～～～～");

        // 処理結果
        boolean result = new Random().nextBoolean();
        return result; // 戻り値が安定しない
    }
}
