package com.example;

import java.util.*;

public class Features {
    private final List<String> names = new ArrayList<>();
    private int[] numbers = new int[]{1, 2, 3};

    public void add(String name) {
        names.add(name);
    }

    public int classify(int value) {
        switch (value) {
            case 1:
            case 2:
                return 10;
            case 3:
                return 30;
            default:
                return 0;
        }
    }

    public String describe(Object value) {
        if (value instanceof String s && !s.isEmpty()) {
            return s.toUpperCase();
        }
        return String.valueOf(value);
    }

    public long widen(int a, long b) {
        return a + b;
    }

    public int bitwise(int a, int b) {
        return (a & b) | (a << 2) ^ (b >>> 1);
    }

    public String join() {
        return String.join(",", names);
    }

    public Map<String, Integer> counts() {
        Map<String, Integer> result = new HashMap<>();
        for (String name : names) {
            result.put(name, result.getOrDefault(name, 0) + 1);
        }
        return result;
    }

    public void read(java.io.BufferedReader reader) throws java.io.IOException {
        try (reader) {
            String line = reader.readLine();
            System.out.println(line);
        } catch (java.io.IOException e) {
            System.err.println(e.getMessage());
        }
    }

    public Runnable task() {
        return () -> System.out.println("done");
    }

    public int[] copy(int[] input) {
        return Arrays.copyOf(input, input.length);
    }
}
