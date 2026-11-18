package com.example;

import java.util.ArrayList;
import java.util.List;

public class Hello {
    private final String name;
    private int count;

    public Hello(String name) {
        this.name = name;
    }

    public String greet(boolean formal) {
        String prefix = formal ? "Good day" : "Hi";
        return prefix + ", " + name + "!";
    }

    public int sum(int[] values) {
        int total = 0;
        for (int value : values) {
            total += value;
        }
        return total;
    }

    public static void main(String[] args) {
        Hello hello = new Hello("Andres");
        System.out.println(hello.greet(true));
    }
}
