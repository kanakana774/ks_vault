package diDemo1;

import java.util.Random;

/**
 * controllerに依存されてるServiceクラス
 */
public class Repository {

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
        return result;
    }
}
