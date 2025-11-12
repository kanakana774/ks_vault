public class App {
    public static void main(String[] args) throws Exception {
        System.out.println("Hello, World!");
        Greeting greeting = new Greeting("Foooooooooo"); // 11文字
        AnnotationProcessor.checkMaxLength(greeting);
    }
}
