import service.Service;

public class App {
    public static void main(String[] args) throws Exception {
        String v = "３";
        System.out.println(Integer.parseInt(v));

        new Service().service();
    }
}
