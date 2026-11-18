package com.example;

import java.util.*;
import java.util.function.Function;

public class Advanced {
    public interface Greeter {
        String greet(String name);
        default String greetTwice(String name) {
            return greet(name) + " " + greet(name);
        }
        static Greeter upper() {
            return name -> name.toUpperCase();
        }
    }

    public static abstract class Base<T> {
        protected final T value;
        protected Base(T value) { this.value = value; }
        public abstract T get();
    }

    public static class Derived extends Base<String> implements Greeter {
        private static int created;
        static { created = 0; }
        private final List<String> history = new ArrayList<>();

        public Derived(String value) {
            super(value);
        }

        @Override
        public String get() { return value; }

        @Override
        public String greet(String name) {
            history.add(name);
            return "Hello " + name;
        }

        public static int created() { return created; }
    }

    public enum Color {
        RED("r"), GREEN("g");
        private final String code;
        Color(String code) { this.code = code; }
        public String getCode() { return code; }
    }

    public record Point(int x, int y) {
        public Point {
            if (x < 0) throw new IllegalArgumentException("x");
        }
        public int sum() { return x + y; }
    }

    public sealed interface Shape permits Circle, Square {}
    public record Circle(double radius) implements Shape {}
    public record Square(double side) implements Shape {}

    public static double area(Shape shape) {
        return switch (shape) {
            case Circle c -> Math.PI * c.radius() * c.radius();
            case Square s -> s.side() * s.side();
        };
    }

    public static String pick(int value) {
        return switch (value) {
            case 1, 2 -> "low";
            case 3 -> "three";
            default -> "high";
        };
    }

    public static int[][] matrix(int rows, int cols) {
        int[][] result = new int[rows][cols];
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                result[i][j] = i * j;
            }
        }
        return result;
    }

    public static <T extends Comparable<T>> T max(List<T> values) {
        T best = values.get(0);
        for (T value : values) {
            if (value.compareTo(best) > 0) best = value;
        }
        return best;
    }

    public static String apply(Function<String, String> fn, String value) {
        return fn.apply(value);
    }

    public static Runnable runnable() {
        return new Runnable() {
            @Override
            public void run() {
                System.out.println("run");
            }
        };
    }
}
