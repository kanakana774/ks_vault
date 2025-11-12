package diDemo2;

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
        Service service = new Service();

        // 呼び出し
        service.callService();

    }
}
