public class Greeting {
    @MaxLength(10)
    private String personName;

    public Greeting(String string) {
        this.personName = string;
    }
}