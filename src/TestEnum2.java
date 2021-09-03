/**
 * @author Z-P-J
 */
public enum TestEnum2 {

    ONE("1"),

    TWO("2"),

    THREE("3"),

    FOUR("4"),

    FIVE("5");

    private String name;

    TestEnum2(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
