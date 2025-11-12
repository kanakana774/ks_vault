import java.nio.charset.StandardCharsets;

public class App {
    public static void main(String[] args) throws Exception {
        String textConteinsNbsp = "http://localhost:8080/tags​?name=T";
        String text = "http://localhost:8080/tags?name=T";
        System.out.println(textConteinsNbsp.length());
        System.out.println(text.length());

        byte[] utf8Bytes = text.getBytes(StandardCharsets.UTF_8);
        byte[] utf8BytesNbsp = textConteinsNbsp.getBytes(StandardCharsets.UTF_8);

        for (Byte b : utf8Bytes) {
            System.out.println(b);
        }
        System.out.println(utf8Bytes);
        for (Byte b : utf8BytesNbsp) {
            System.out.println(b);
        }

        System.out.println(textConteinsNbsp.replace("\u200b", "").length());
    }
}
