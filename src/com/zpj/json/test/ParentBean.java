package com.zpj.json.test;

import java.util.ArrayList;
import java.util.List;

public class ParentBean<T> {

    protected  Integer ii = 0;
    public TestBean2 testBean2 = new TestBean2();
    protected List<T> llll = new ArrayList<>();

}
