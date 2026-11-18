package dev.j2kt;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;

/** Lexical name/type environment used for local inference. */
public final class Scope {
    private final Deque<Map<String, KType>> locals = new ArrayDeque<>();
    private final Map<String, KType> fields = new HashMap<>();
    private final Map<String, KType> methods = new HashMap<>();

    public Scope() {
        locals.push(new HashMap<>());
    }

    public void push() {
        locals.push(new HashMap<>());
    }

    public void pop() {
        if (locals.size() > 1) {
            locals.pop();
        }
    }

    public void declare(String name, KType type) {
        locals.peek().put(name, type);
    }

    public KType lookup(String name) {
        for (Map<String, KType> frame : locals) {
            KType type = frame.get(name);
            if (type != null) {
                return type;
            }
        }
        return fields.get(name);
    }

    public void declareField(String name, KType type) {
        fields.put(name, type);
    }

    public void declareMethod(String name, KType returnType) {
        methods.putIfAbsent(name, returnType);
    }

    public KType methodReturn(String name) {
        return methods.get(name);
    }

    public Map<String, KType> fields() {
        return fields;
    }
}
