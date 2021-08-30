package com.zpj.json.test;

public class TestBean2 {

    public int a = 100;
    public String b = "123";
    public char c = 'a';
    public long l = 10000L;
    public double d = 2.0;
    public float f = 2.0f;
    Object object = new Object();

    @Override
    public String toString() {
        return "TestBean2{" +
                "a=" + a +
                ", b='" + b + '\'' +
                ", c=" + c +
                ", l=" + l +
                ", d=" + d +
                ", f=" + f +
                ", object=" + object +
                '}';
    }
}
