import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class TestBean extends ParentBean<Integer> {

    public int a = 100;
    public String b = "1\"23";
    private String bb = null;
    private final AtomicInteger integer = new AtomicInteger(100);
    public char c = 'c';
    public long l = 10000L;
    public Double d = 2.0;
    public float f = 2.0f;
    public TestBean2 bean = new TestBean2();
    public List<String> list = new ArrayList<>();
    public List<List<String>> list2 = new ArrayList<>();
    private Object obj;
    private int[] arr = {1, 2, 3};
    String[] strs = {"1", "2", "3", "4"};
    Map<String, Boolean> map = new HashMap<>();
    Map<String, List<String>> map2 = new HashMap<>();
    Map<String, Map<String, List<String>>> map3 = new HashMap<>();

    private TestEnum testEnum = TestEnum.ONE;
    private final TestEnum2 testEnum2 = TestEnum2.FIVE;

    @Override
    public String toString() {
        return "TestBean{" +
                "ii=" + ii +
                ", testBean2=" + testBean2 +
                ", llll=" + llll +
                ", a=" + a +
                ", b='" + b + '\'' +
                ", bb='" + bb + '\'' +
                ", integer=" + integer +
                ", c=" + c +
                ", l=" + l +
                ", d=" + d +
                ", f=" + f +
                ", bean=" + bean +
                ", list=" + list +
                ", list2=" + list2 +
                ", obj=" + obj +
                ", arr=" + Arrays.toString(arr) +
                ", strs=" + Arrays.toString(strs) +
                ", map=" + map +
                ", map2=" + map2 +
                ", map3=" + map3 +
                ", testEnum=" + testEnum +
                ", testEnum2=" + testEnum2 +
                '}';
    }
}
