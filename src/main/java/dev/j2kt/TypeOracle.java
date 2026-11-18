package dev.j2kt;

import com.github.javaparser.ast.expr.ArrayAccessExpr;
import com.github.javaparser.ast.expr.ArrayCreationExpr;
import com.github.javaparser.ast.expr.AssignExpr;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.BooleanLiteralExpr;
import com.github.javaparser.ast.expr.CastExpr;
import com.github.javaparser.ast.expr.CharLiteralExpr;
import com.github.javaparser.ast.expr.ClassExpr;
import com.github.javaparser.ast.expr.ConditionalExpr;
import com.github.javaparser.ast.expr.DoubleLiteralExpr;
import com.github.javaparser.ast.expr.EnclosedExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.InstanceOfExpr;
import com.github.javaparser.ast.expr.IntegerLiteralExpr;
import com.github.javaparser.ast.expr.LambdaExpr;
import com.github.javaparser.ast.expr.LiteralExpr;
import com.github.javaparser.ast.expr.LongLiteralExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.MethodReferenceExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.NullLiteralExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.expr.SuperExpr;
import com.github.javaparser.ast.expr.SwitchExpr;
import com.github.javaparser.ast.expr.ThisExpr;
import com.github.javaparser.ast.expr.UnaryExpr;
import com.github.javaparser.ast.expr.VariableDeclarationExpr;
import com.github.javaparser.ast.type.Type;

import java.util.IdentityHashMap;
import java.util.Map;

/**
 * Best-effort expression typing. It prefers the JavaParser symbol solver when a
 * type is resolvable, then falls back to local flow-insensitive inference.
 * Unknown is a first-class result: the emitter turns it into a conservative
 * translation and, when it matters, a diagnostic.
 */
public final class TypeOracle {
    private final TypeMapper types;
    private final Scope scope;
    private final Diagnostics diagnostics;
    private final boolean enabled;
    private final Map<Expression, KType> resolvedCache = new IdentityHashMap<>();
    private KType thisType = KType.unknown("this");

    public TypeOracle(TypeMapper types, Scope scope, Diagnostics diagnostics, boolean enabled) {
        this.types = types;
        this.scope = scope;
        this.diagnostics = diagnostics;
        this.enabled = enabled;
    }

    public void setThisType(KType thisType) {
        this.thisType = thisType;
    }

    public boolean enabled() {
        return enabled;
    }

    public KType infer(Expression expression) {
        if (expression == null) {
            return KType.unknown("<missing>").asNullable();
        }
        if (expression instanceof EnclosedExpr enclosed) {
            return infer(enclosed.getInner());
        }
        if (expression instanceof NameExpr name) {
            KType local = scope.lookup(name.getNameAsString());
            if (local != null) {
                return local;
            }
            return resolve(expression);
        }
        if (expression instanceof FieldAccessExpr field) {
            if (field.getNameAsString().equals("PI") || field.getNameAsString().equals("E")) {
                return KType.DOUBLE;
            }
            if (field.getScope().isThisExpr()) {
                KType known = scope.lookup(field.getNameAsString());
                if (known != null) {
                    return known;
                }
            }
            KType resolved = resolve(expression);
            if (!resolved.isUnknown()) {
                return resolved;
            }
            return infer(field.getScope());
        }
        if (expression instanceof MethodCallExpr call) {
            return inferCall(call);
        }
        if (expression instanceof ObjectCreationExpr creation) {
            return types.map(creation.getType());
        }
        if (expression instanceof CastExpr cast) {
            Type type = cast.getType();
            boolean nullable = !type.isPrimitiveType() && !type.isVoidType();
            return types.map(type, nullable);
        }
        if (expression instanceof ArrayCreationExpr arrayCreation) {
            return inferArrayCreation(arrayCreation);
        }
        if (expression instanceof ArrayAccessExpr access) {
            KType array = infer(access.getName());
            return array.element() != null ? array.element() : KType.unknown("array element").asNullable();
        }
        if (expression instanceof BinaryExpr binary) {
            return inferBinary(binary);
        }
        if (expression instanceof UnaryExpr unary) {
            return switch (unary.getOperator()) {
                case LOGICAL_COMPLEMENT -> KType.BOOLEAN;
                case POSTFIX_INCREMENT, POSTFIX_DECREMENT, PREFIX_INCREMENT, PREFIX_DECREMENT ->
                        infer(unary.getExpression());
                case BITWISE_COMPLEMENT -> {
                    KType operand = infer(unary.getExpression());
                    yield "Long".equals(operand.base()) ? KType.LONG : KType.INT;
                }
                default -> infer(unary.getExpression());
            };
        }
        if (expression instanceof ConditionalExpr conditional) {
            return commonType(infer(conditional.getThenExpr()), infer(conditional.getElseExpr()));
        }
        if (expression instanceof AssignExpr assign) {
            return infer(assign.getTarget());
        }
        if (expression instanceof VariableDeclarationExpr declaration) {
            if (declaration.getVariables().isEmpty()) {
                return KType.UNIT;
            }
            var variable = declaration.getVariable(declaration.getVariables().size() - 1);
            if (variable.getType().isVarType() && variable.getInitializer().isPresent()) {
                return infer(variable.getInitializer().get());
            }
            return types.map(variable.getType());
        }
        if (expression instanceof ThisExpr) {
            return thisType;
        }
        if (expression instanceof SuperExpr) {
            return thisType;
        }
        if (expression instanceof ClassExpr classExpr) {
            KType type = types.map(classExpr.getType());
            return KType.simple(types.imports().ref("kotlin.reflect.KClass") + "<" + type.render() + ">",
                    "java.lang.Class", KType.Category.OBJECT);
        }
        if (expression instanceof InstanceOfExpr) {
            return KType.BOOLEAN;
        }
        if (expression instanceof LambdaExpr || expression instanceof MethodReferenceExpr) {
            return KType.unknown("function").asNullable();
        }
        if (expression instanceof SwitchExpr switchExpr) {
            KType result = null;
            for (var entry : switchExpr.getEntries()) {
                for (var value : entry.getStatements()) {
                    if (value instanceof com.github.javaparser.ast.stmt.ExpressionStmt statement) {
                        KType type = infer(statement.getExpression());
                        result = result == null ? type : commonType(result, type);
                    } else if (value instanceof com.github.javaparser.ast.stmt.YieldStmt yield) {
                        KType type = infer(yield.getExpression());
                        result = result == null ? type : commonType(result, type);
                    }
                }
            }
            return result == null ? KType.unknown("switch expression").asNullable() : result;
        }
        if (expression instanceof LiteralExpr) {
            return inferLiteral(expression);
        }
        KType resolved = resolve(expression);
        if (!resolved.isUnknown()) {
            return resolved;
        }
        return KType.unknown(expression.toString()).asNullable();
    }

    private KType inferCall(MethodCallExpr call) {
        String name = call.getNameAsString();
        KType receiver = call.getScope().map(this::infer).orElse(null);

        if (!call.getScope().isPresent()) {
            KType local = scope.lookup(name);
            if (local != null) {
                return local;
            }
        }

        if (receiver != null) {
            if (receiver.isString()) {
                switch (name) {
                    case "length" -> { return KType.INT; }
                    case "charAt", "first", "last" -> { return KType.CHAR; }
                    case "substring", "trim", "toLowerCase", "toUpperCase", "replace", "concat", "intern",
                         "strip", "stripLeading", "stripTrailing", "repeat", "formatted" -> {
                        return KType.STRING;
                    }
                    case "isEmpty", "isBlank", "contains", "startsWith", "endsWith", "matches", "equals",
                         "equalsIgnoreCase", "contentEquals" -> {
                        return KType.BOOLEAN;
                    }
                    case "indexOf", "lastIndexOf", "compareTo", "compareToIgnoreCase", "hashCode" -> {
                        return KType.INT;
                    }
                    case "toCharArray" -> {
                        return KType.array("CharArray", "char[]", KType.CHAR);
                    }
                    case "getBytes" -> {
                        return KType.array("ByteArray", "byte[]", KType.BYTE);
                    }
                    case "split" -> {
                        return KType.array("Array<String>", "java.lang.String[]", KType.STRING);
                    }
                    default -> { }
                }
            }
            if (receiver.isCollection()) {
                switch (name) {
                    case "size", "indexOf", "lastIndexOf" -> { return KType.INT; }
                    case "isEmpty", "contains", "containsAll", "add", "addAll", "remove", "removeAll",
                         "retainAll", "removeIf" -> {
                        return "add".equals(name) || "addAll".equals(name) || "remove".equals(name)
                                || "removeAll".equals(name) || "retainAll".equals(name) || "removeIf".equals(name)
                                ? KType.BOOLEAN : KType.BOOLEAN;
                    }
                    case "get", "getFirst", "getLast", "removeAt", "removeFirst", "removeLast" -> {
                        return receiver.element() == null ? KType.unknown("list element").asNullable() : receiver.element();
                    }
                    case "iterator", "listIterator" -> {
                        return KType.collection("MutableIterator<" + (receiver.element() == null ? "Any?" : receiver.element().render()) + ">",
                                "java.util.Iterator", receiver.element(), true);
                    }
                    case "toArray" -> {
                        KType element = receiver.element() == null ? KType.unknown("element").asNullable() : receiver.element();
                        return KType.array("Array<" + element.render() + ">", "java.lang.Object[]", element);
                    }
                    default -> { }
                }
            }
            if (receiver.isMap()) {
                switch (name) {
                    case "size" -> { return KType.INT; }
                    case "isEmpty", "containsKey", "containsValue", "remove", "replace" -> { return KType.BOOLEAN; }
                    case "get", "getOrDefault" ->
                            { return receiver.element() == null ? KType.unknown("map value").asNullable() : receiver.element(); }
                    case "keySet" -> {
                        KType key = receiver.key() == null ? KType.unknown("key").asNullable() : receiver.key();
                        return KType.collection("MutableSet<" + key.render() + ">", "java.util.Set", key, true);
                    }
                    case "values" -> {
                        KType value = receiver.element() == null ? KType.unknown("value").asNullable() : receiver.element();
                        return KType.collection("MutableCollection<" + value.render() + ">", "java.util.Collection", value, true);
                    }
                    case "entrySet" -> {
                        return KType.collection("MutableSet<Map.Entry<" + (receiver.key() == null ? "Any?" : receiver.key().render())
                                + ", " + (receiver.element() == null ? "Any?" : receiver.element().render()) + ">>",
                                "java.util.Set", KType.unknown("entry").asNullable(), true);
                    }
                    default -> { }
                }
            }
            if (receiver.isArray()) {
                switch (name) {
                    case "clone" -> { return receiver; }
                    case "length" -> { return KType.INT; }
                    default -> { }
                }
            }
        }

        if (name.equals("getMessage") || name.equals("getLocalizedMessage")) {
            return KType.STRING.asNullable();
        }
        if (name.equals("getKey") || name.equals("getValue")) {
            return KType.unknown("map entry value").asNullable();
        }
        if (name.equals("asJava") && receiver != null
                && receiver.javaName().startsWith("pumpkin.Player")) {
            return KType.simple("JavaPlayer", "pumpkin.Player.JavaPlayer", KType.Category.OBJECT).asNullable();
        }
        if (name.equals("asBedrock") && receiver != null
                && receiver.javaName().startsWith("pumpkin.Player")) {
            return KType.simple("BedrockPlayer", "pumpkin.Player.BedrockPlayer", KType.Category.OBJECT).asNullable();
        }
        if (name.equals("getServer") && receiver != null
                && (receiver.javaName().startsWith("pumpkin.Context")
                || receiver.javaName().startsWith("plugin.PluginContext"))) {
            return KType.simple("Server", "pumpkin.Server.Server", KType.Category.OBJECT);
        }
        if (name.equals("getCause")) {
            return KType.simple("Throwable", "java.lang.Throwable", KType.Category.OBJECT).asNullable();
        }

        // Static utility calls that are common enough to type without resolving.
        String scopeText = call.getScope().map(Object::toString).orElse("");
        if (scopeText.endsWith("Integer") || scopeText.endsWith("Long") || scopeText.endsWith("Short")
                || scopeText.endsWith("Byte") || scopeText.endsWith("Double") || scopeText.endsWith("Float")) {
            if (name.startsWith("parse") || name.equals("valueOf") || name.equals("compare")
                    || name.equals("sum") || name.equals("max") || name.equals("min")) {
                return switch (Imports.simpleName(scopeText)) {
                    case "Long" -> KType.LONG;
                    case "Double" -> KType.DOUBLE;
                    case "Float" -> KType.FLOAT;
                    case "Short" -> KType.SHORT;
                    case "Byte" -> KType.BYTE;
                    case "Integer" -> KType.INT;
                    default -> KType.INT;
                };
            }
            if (name.equals("toString")) {
                return KType.STRING;
            }
        }
        if (scopeText.endsWith("Math")) {
            if (name.equals("abs")) {
                return call.getArguments().isEmpty() ? KType.INT : infer(call.getArgument(0));
            }
            if (name.equals("max") || name.equals("min")) {
                return call.getArguments().isEmpty() ? KType.INT : infer(call.getArgument(0));
            }
            if (name.equals("round")) {
                return KType.LONG;
            }
            if (name.equals("random")) {
                return KType.DOUBLE;
            }
            return KType.DOUBLE;
        }
        if (scopeText.endsWith("Objects") && name.equals("requireNonNull")) {
            return call.getArguments().isEmpty() ? KType.unknown("Objects.requireNonNull").asNullable() : infer(call.getArgument(0));
        }

        if (receiver != null) {
            String receiverName = receiver.javaName();
            if ((name.equals("getPlayer") && receiverName.startsWith("pumpkin.Server"))
                    || (name.equals("getWorld") && receiverName.startsWith("pumpkin.Server"))
                    || name.equals("getBlockByName") || name.equals("getBlockById")
                    || name.equals("getPack") || name.equals("getTarget")
                    || name.equals("fromEntity")
                    || name.equals("asPlayer")
                    || ((name.equals("get") || name.equals("remove")) && receiver.isMap())
                    || (name.equals("getItem") && (receiverName.startsWith("pumpkin.Inventory")
                    || receiverName.startsWith("pumpkin.Gui")))) {
                KType nullableResult = resolve(call);
                if (!nullableResult.isUnknown()) {
                    return nullableResult.asNullable();
                }
            }
            if (name.equals("asMob") && (receiverName.startsWith("pumpkin.World.Entity")
                    || receiverName.startsWith("pumpkin.World.LivingEntity"))) {
                KType nullableResult = resolve(call);
                if (!nullableResult.isUnknown()) {
                    return nullableResult.asNullable();
                }
            }
        }

        KType resolved = resolve(call);
        if (!resolved.isUnknown()) {
            return resolved;
        }
        if (scope != null) {
            KType method = scope.methodReturn(name);
            if (method != null) {
                return method;
            }
        }
        return KType.unknown(call.toString()).asNullable();
    }

    private KType inferArrayCreation(ArrayCreationExpr creation) {
        KType component = types.map(creation.getElementType());
        int levels = creation.getLevels().size();
        KType result = component;
        for (int i = 0; i < levels; i++) {
            String base;
            if (i == 0 && (component.category() == KType.Category.NUMBER
                    || component.category() == KType.Category.BOOLEAN
                    || component.category() == KType.Category.CHAR)) {
                base = switch (component.base()) {
                    case "Int" -> "IntArray";
                    case "Long" -> "LongArray";
                    case "Short" -> "ShortArray";
                    case "Byte" -> "ByteArray";
                    case "Float" -> "FloatArray";
                    case "Double" -> "DoubleArray";
                    case "Boolean" -> "BooleanArray";
                    case "Char" -> "CharArray";
                    default -> "Array<" + result.render() + ">";
                };
            } else {
                base = "Array<" + result.render() + ">";
            }
            result = KType.array(base, creation.getElementType() + "[]", result);
        }
        return result;
    }

    private KType inferBinary(BinaryExpr binary) {
        KType left = infer(binary.getLeft());
        KType right = infer(binary.getRight());
        return switch (binary.getOperator()) {
            case PLUS -> {
                if (left.isString() || right.isString()) {
                    yield KType.STRING;
                }
                yield numericPromotion(left, right);
            }
            case MINUS, MULTIPLY, DIVIDE, REMAINDER -> numericPromotion(left, right);
            case LEFT_SHIFT, SIGNED_RIGHT_SHIFT, UNSIGNED_RIGHT_SHIFT -> {
                if ("Long".equals(left.base())) {
                    yield KType.LONG;
                }
                yield KType.INT;
            }
            case BINARY_AND, BINARY_OR, XOR -> {
                if (left.isBoolean() && right.isBoolean()) {
                    yield KType.BOOLEAN;
                }
                yield numericPromotion(left, right);
            }
            case EQUALS, NOT_EQUALS, LESS, LESS_EQUALS, GREATER, GREATER_EQUALS,
                 OR, AND -> KType.BOOLEAN;
        };
    }

    private KType numericPromotion(KType left, KType right) {
        if ("Double".equals(left.base()) || "Double".equals(right.base())) {
            return KType.DOUBLE;
        }
        if ("Float".equals(left.base()) || "Float".equals(right.base())) {
            return KType.FLOAT;
        }
        if ("Long".equals(left.base()) || "Long".equals(right.base())) {
            return KType.LONG;
        }
        return KType.INT;
    }

    private KType commonType(KType a, KType b) {
        if (a == null) return b;
        if (b == null) return a;
        if (a.category() == KType.Category.NULL) return b.asNullable();
        if (b.category() == KType.Category.NULL) return a.asNullable();
        if (a.base().equals(b.base())) {
            return a.withNullable(a.nullable() || b.nullable());
        }
        if (a.isNumeric() && b.isNumeric()) {
            return numericPromotion(a, b);
        }
        if (a.isString() || b.isString()) {
            return KType.STRING;
        }
        return KType.unknown("common(" + a + ", " + b + ")").asNullable();
    }

    private KType inferLiteral(Expression expression) {
        if (expression instanceof StringLiteralExpr || expression instanceof com.github.javaparser.ast.expr.TextBlockLiteralExpr) {
            return KType.STRING;
        }
        if (expression instanceof CharLiteralExpr) {
            return KType.CHAR;
        }
        if (expression instanceof BooleanLiteralExpr) {
            return KType.BOOLEAN;
        }
        if (expression instanceof NullLiteralExpr) {
            return KType.NULL;
        }
        if (expression instanceof LongLiteralExpr) {
            return KType.LONG;
        }
        if (expression instanceof DoubleLiteralExpr literal) {
            String value = literal.getValue();
            if (value.endsWith("f") || value.endsWith("F")) {
                return KType.FLOAT;
            }
            return KType.DOUBLE;
        }
        if (expression instanceof IntegerLiteralExpr literal) {
            String value = literal.getValue().replace("_", "");
            try {
                long parsed = Long.decode(value);
                if (parsed > Integer.MAX_VALUE || parsed < Integer.MIN_VALUE) {
                    return KType.LONG;
                }
            } catch (NumberFormatException ignored) {
                // Keep Int; malformed literals are a Java compiler problem.
            }
            return KType.INT;
        }
        return KType.unknown(expression.toString()).asNullable();
    }

    private KType resolve(Expression expression) {
        if (!enabled) {
            return KType.unknown(expression.toString()).asNullable();
        }
        KType cached = resolvedCache.get(expression);
        if (cached != null) {
            return cached;
        }
        try {
            KType mapped = types.mapResolved(expression.calculateResolvedType());
            resolvedCache.put(expression, mapped);
            return mapped;
        } catch (Throwable failure) {
            KType unknown = KType.unknown(expression.toString()).asNullable();
            resolvedCache.put(expression, unknown);
            return unknown;
        }
    }

    /** Returns the fully-qualified declaring type of a resolved method call, or null. */
    public String declaringType(MethodCallExpr call) {
        if (!enabled) {
            return null;
        }
        try {
            return call.resolve().declaringType().getQualifiedName();
        } catch (Throwable failure) {
            return null;
        }
    }

    /** Returns the resolved parameter type, or null. */
    public KType parameterType(MethodCallExpr call, int index) {
        if (!enabled) {
            return null;
        }
        try {
            var resolved = call.resolve();
            if (index < 0 || index >= resolved.getNumberOfParams()) {
                return null;
            }
            return types.mapResolved(resolved.getParam(index).getType());
        } catch (Throwable failure) {
            return null;
        }
    }

    public Diagnostics diagnostics() {
        return diagnostics;
    }
}
