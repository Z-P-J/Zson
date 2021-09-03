# Zson
 Java轻量级JSON序列化/反序列化库


## Zson的使用
[Zson源代码](./src/com/zpj/json)
~~~java
// 创建Zson对象
Zson zson = new Zson();

// 序列化
TestBean bean = new TestBean();
String json = zson.serialize(bean);

// 反序列化
String json = "{\"a\":100,\"b\":\"1\\\"23\",\"bb\":null,\"integer\":100,\"c\":99,\"l\":10000,\"d\":2.0,\"f\":2.0,\"bean\":{\"a\":100,\"b\":\"123\",\"c\":97,\"l\":10000,\"d\":2.0,\"f\":2.0,\"object\":{}},\"list\":[\"123\",\"456\",\"789\"],\"list2\":[[\"123\",\"456\",\"789\"]],\"obj\":null,\"arr\":[1,2,3],\"strs\":[\"1\",\"2\",\"3\",\"4\"],\"map\":{\"1\":true,\"2\":false,\"3\":false},\"map2\":{\"1\":[\"123\",\"456\",\"789\"],\"2\":[],\"3\":[\"123\",\"456\",\"789\"]},\"map3\":{\"zpj\":{\"1\":[\"123\",\"456\",\"789\"],\"2\":[],\"3\":[\"123\",\"456\",\"789\"]}},\"testEnum\":\"ONE\",\"testEnum2\":\"FIVE\",\"ii\":0,\"testBean2\":{\"a\":100,\"b\":\"123\",\"c\":97,\"l\":10000,\"d\":2.0,\"f\":2.0,\"object\":{}},\"llll\":[1,100]}"
TestBean bean = zson.deserialize(json, TestBean.class);
~~~
