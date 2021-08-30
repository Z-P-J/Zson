package com.zpj.json.test;

import com.zpj.json.Zson;

import java.util.ArrayList;
import java.util.function.IntConsumer;
import java.util.stream.IntStream;

public class Test {

    public static void main(String[] args) throws Exception {
        final Zson serializer = new Zson();
        int[] arr = {1, 3, 5, 7, 8};
        System.out.println(serializer.serialize(arr));
        IntStream.range(1, 10).forEach(new IntConsumer() {
            @Override
            public void accept(int value)  {
                try {
                    long start = System.currentTimeMillis();

                    TestBean bean = new TestBean();
                    bean.llll.add(1);
                    bean.llll.add(100);
                    bean.list.add("123");
                    bean.list.add("456");
                    bean.list.add("789");
                    bean.list2.add(bean.list);
                    bean.map.put("1", true);
                    bean.map.put("2", false);
                    bean.map.put("3", false);
                    bean.map2.put("1", bean.list);
                    bean.map2.put("2", new ArrayList<>());
                    bean.map2.put("3", bean.list);
                    bean.map3.put("zpj", bean.map2);
                    String json = serializer.serialize(bean);
                    System.out.println(json);
                    long end = System.currentTimeMillis();
                    System.out.println("deltaTime1=" + (end - start));

                    start = System.currentTimeMillis();
                    json = "{\"a\":100,\"b\":\"1\\\"23\",\"bb\":null,\"integer\":100,\"c\":99,\"l\":10000,\"d\":2.0,\"f\":2.0,\"bean\":{\"a\":100,\"b\":\"123\",\"c\":97,\"l\":10000,\"d\":2.0,\"f\":2.0,\"object\":{}},\"list\":[\"123\",\"456\",\"789\"],\"list2\":[[\"123\",\"456\",\"789\"]],\"obj\":null,\"arr\":[1,2,3],\"strs\":[\"1\",\"2\",\"3\",\"4\"],\"map\":{\"1\":true,\"2\":false,\"3\":false},\"map2\":{\"1\":[\"123\",\"456\",\"789\"],\"2\":[],\"3\":[\"123\",\"456\",\"789\"]},\"map3\":{\"zpj\":{\"1\":[\"123\",\"456\",\"789\"],\"2\":[],\"3\":[\"123\",\"456\",\"789\"]}},\"ii\":0,\"testBean2\":{\"a\":100,\"b\":\"123\",\"c\":97,\"l\":10000,\"d\":2.0,\"f\":2.0,\"object\":{}},\"llll\":[1,100]}";
                    System.out.println(json.substring(524));
                    System.out.println(serializer.deserialize(json, TestBean.class));
                    end = System.currentTimeMillis();
                    System.out.println("deltaTime2=" + (end - start));
                    System.out.println();
                } catch (Exception e) {
                    e.printStackTrace();
                }

            }
        });

    }

}
