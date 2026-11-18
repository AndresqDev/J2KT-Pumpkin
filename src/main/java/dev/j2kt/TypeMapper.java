package dev.j2kt;

import com.github.javaparser.ast.type.ArrayType;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.IntersectionType;
import com.github.javaparser.ast.type.PrimitiveType;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.ast.type.TypeParameter;
import com.github.javaparser.ast.type.UnionType;
import com.github.javaparser.ast.type.VoidType;
import com.github.javaparser.ast.type.WildcardType;
import com.github.javaparser.resolution.types.ResolvedArrayType;
import com.github.javaparser.resolution.types.ResolvedReferenceType;
import com.github.javaparser.resolution.types.ResolvedType;
import com.github.javaparser.resolution.types.ResolvedWildcard;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Maps Java types to source-ready Kotlin types and records the imports required
 * by those types. The mapping is deliberately conservative: anything that does
 * not have a faithful Kotlin/Wasm equivalent is reported to the caller.
 */
public final class TypeMapper {
    private static final Set<String> BOXED = Set.of(
            "java.lang.Byte", "java.lang.Short", "java.lang.Integer", "java.lang.Long",
            "java.lang.Float", "java.lang.Double", "java.lang.Boolean", "java.lang.Character"
    );

    private final Target target;
    private final Imports imports;
    private final Diagnostics diagnostics;
    private final boolean compatPlugin;

    public TypeMapper(Target target, Imports imports, Diagnostics diagnostics) {
        this(target, imports, diagnostics, true);
    }

    public TypeMapper(Target target, Imports imports, Diagnostics diagnostics, boolean compatPlugin) {
        this.target = target;
        this.imports = imports;
        this.diagnostics = diagnostics;
        this.compatPlugin = compatPlugin;
    }

    public Imports imports() {
        return imports;
    }

    /**
     * A copy used only for expression typing. It must not record imports:
     * inferred types are not necessarily emitted in the source.
     */
    public TypeMapper silentCopy() {
        return new TypeMapper(target, new Imports(imports.currentPackage()), new Diagnostics(), compatPlugin);
    }

    public KType map(Type type) {
        return map(type, false);
    }

    public KType map(Type type, boolean nullableHint) {
        if (type == null) {
            return KType.unknown("<missing>").asNullable();
        }
        if (type instanceof PrimitiveType primitive) {
            return switch (primitive.getType()) {
                case BOOLEAN -> KType.BOOLEAN;
                case CHAR -> KType.CHAR;
                case BYTE -> KType.BYTE;
                case SHORT -> KType.SHORT;
                case INT -> KType.INT;
                case LONG -> KType.LONG;
                case FLOAT -> KType.FLOAT;
                case DOUBLE -> KType.DOUBLE;
            };
        }
        if (type instanceof VoidType) {
            return KType.UNIT;
        }
        if (type instanceof ArrayType arrayType) {
            KType component = map(arrayType.getComponentType());
            String javaComponent = arrayType.getComponentType().asString();
            String base;
            if (component.category() == KType.Category.NUMBER || component.category() == KType.Category.BOOLEAN
                    || component.category() == KType.Category.CHAR) {
                base = switch (component.base()) {
                    case "Int" -> "IntArray";
                    case "Long" -> "LongArray";
                    case "Short" -> "ShortArray";
                    case "Byte" -> "ByteArray";
                    case "Float" -> "FloatArray";
                    case "Double" -> "DoubleArray";
                    case "Boolean" -> "BooleanArray";
                    case "Char" -> "CharArray";
                    default -> "Array<" + component.render() + ">";
                };
            } else {
                base = "Array<" + component.render() + ">";
            }
            return KType.array(base, javaComponent + "[]", component).withNullable(nullableHint);
        }
        if (type instanceof ClassOrInterfaceType classType) {
            String name = qualifiedName(classType);
            List<KType> args = new ArrayList<>();
            classType.getTypeArguments().ifPresent(list -> {
                for (Type argument : list) {
                    args.add(map(argument));
                }
            });
            KType mapped = mapName(name, args, type);
            return nullableHint ? mapped.asNullable() : mapped;
        }
        if (type instanceof WildcardType wildcard) {
            if (wildcard.getExtendedType().isPresent()) {
                return KType.simple("out " + map(wildcard.getExtendedType().get()).render(), "? extends", KType.Category.OBJECT);
            }
            if (wildcard.getSuperType().isPresent()) {
                return KType.simple("in " + map(wildcard.getSuperType().get()).render(), "? super", KType.Category.OBJECT);
            }
            return KType.simple("*", "?", KType.Category.OBJECT);
        }
        if (type instanceof TypeParameter parameter) {
            return KType.simple(parameter.getNameAsString(), parameter.getNameAsString(), KType.Category.OBJECT)
                    .withNullable(nullableHint);
        }
        if (type instanceof UnionType || type instanceof IntersectionType) {
            diagnostics.warn("J2KT-TYPE-001", "Union/intersection type has no direct Kotlin equivalent; using its first bound.", type);
            String first = type.asString().split("[|&]")[0].trim();
            return mapName(first, List.of(), type).withNullable(nullableHint);
        }
        if (type.isVarType()) {
            return KType.unknown("var").asNullable();
        }
        diagnostics.warn("J2KT-TYPE-002", "Unrecognized Java type '" + type.asString() + "'; emitted as Any?.", type);
        return KType.unknown(type.asString()).asNullable();
    }

    public KType mapResolved(ResolvedType type) {
        if (type == null || type.isTypeVariable()) {
            return KType.unknown(type == null ? "<unknown>" : type.describe()).asNullable();
        }
        if (type.isVoid()) {
            return KType.UNIT;
        }
        if (type.isNull()) {
            return KType.NULL;
        }
        if (type.isPrimitive()) {
            return switch (type.describe()) {
                case "boolean" -> KType.BOOLEAN;
                case "char" -> KType.CHAR;
                case "byte" -> KType.BYTE;
                case "short" -> KType.SHORT;
                case "int" -> KType.INT;
                case "long" -> KType.LONG;
                case "float" -> KType.FLOAT;
                case "double" -> KType.DOUBLE;
                default -> KType.unknown(type.describe()).asNullable();
            };
        }
        if (type.isArray()) {
            ResolvedArrayType array = type.asArrayType();
            KType component = mapResolved(array.getComponentType());
            String base;
            if (component.category() == KType.Category.NUMBER || component.category() == KType.Category.BOOLEAN
                    || component.category() == KType.Category.CHAR) {
                base = switch (component.base()) {
                    case "Int" -> "IntArray";
                    case "Long" -> "LongArray";
                    case "Short" -> "ShortArray";
                    case "Byte" -> "ByteArray";
                    case "Float" -> "FloatArray";
                    case "Double" -> "DoubleArray";
                    case "Boolean" -> "BooleanArray";
                    case "Char" -> "CharArray";
                    default -> "Array<" + component.render() + ">";
                };
            } else {
                base = "Array<" + component.render() + ">";
            }
            return KType.array(base, type.describe(), component);
        }
        if (type.isWildcard()) {
            ResolvedWildcard wildcard = (ResolvedWildcard) type;
            if (!wildcard.isBounded()) {
                return KType.simple("*", "?", KType.Category.OBJECT);
            }
            KType bound = mapResolved(wildcard.getBoundedType());
            return KType.simple((wildcard.isExtends() ? "out " : "in ") + bound.render(), type.describe(), KType.Category.OBJECT);
        }
        if (type.isReferenceType()) {
            ResolvedReferenceType reference = type.asReferenceType();
            List<KType> args = new ArrayList<>();
            for (ResolvedType parameter : reference.typeParametersValues()) {
                args.add(mapResolved(parameter));
            }
            return mapName(reference.getQualifiedName(), args, null);
        }
        return KType.unknown(type.describe()).asNullable();
    }

    private KType mapName(String rawName, List<KType> args, Object node) {
        String name = rawName == null ? "" : rawName.replace('$', '.');
        String simple = Imports.simpleName(name);

        // Primitives can appear here when a TypeParameter is resolved as "int".
        KType primitive = switch (name) {
            case "boolean" -> KType.BOOLEAN;
            case "char" -> KType.CHAR;
            case "byte" -> KType.BYTE;
            case "short" -> KType.SHORT;
            case "int" -> KType.INT;
            case "long" -> KType.LONG;
            case "float" -> KType.FLOAT;
            case "double" -> KType.DOUBLE;
            case "void" -> KType.UNIT;
            default -> null;
        };
        if (primitive != null) {
            return primitive;
        }

        if (name.equals("java.lang.String") || name.equals("String")) {
            return KType.STRING;
        }
        if (name.equals("java.lang.Object") || name.equals("Object")) {
            return KType.simple("Any", "java.lang.Object", KType.Category.OBJECT).asNullable();
        }
        if (BOXED.contains(name)) {
            KType boxed = switch (simple) {
                case "Byte" -> KType.BYTE;
                case "Short" -> KType.SHORT;
                case "Integer" -> KType.INT;
                case "Long" -> KType.LONG;
                case "Float" -> KType.FLOAT;
                case "Double" -> KType.DOUBLE;
                case "Boolean" -> KType.BOOLEAN;
                case "Character" -> KType.CHAR;
                default -> KType.unknown(name).asNullable();
            };
            return boxed.asNullable();
        }
        if (name.equals("java.lang.Void")) {
            return KType.UNIT.asNullable();
        }
        if (name.equals("java.lang.Class")) {
            String arg = args.isEmpty() ? "*" : args.get(0).render();
            return KType.simple(imports.ref("kotlin.reflect.KClass") + "<" + arg + ">", name, KType.Category.OBJECT);
        }
        if (name.equals("java.lang.CharSequence")) {
            return KType.simple(imports.ref("kotlin.CharSequence"), name, KType.Category.STRING);
        }
        if (name.equals("java.lang.Throwable") || name.equals("java.lang.Exception")
                || name.equals("java.lang.RuntimeException") || name.equals("java.lang.Error")
                || name.equals("java.lang.IllegalArgumentException") || name.equals("java.lang.IllegalStateException")
                || name.equals("java.lang.NullPointerException") || name.equals("java.lang.IndexOutOfBoundsException")
                || name.equals("java.lang.UnsupportedOperationException")) {
            return KType.simple(simple, name, KType.Category.OBJECT);
        }
        if (name.equals("java.lang.Enum")) {
            String arg = args.isEmpty() ? "*" : args.get(0).render();
            return KType.simple("Enum<" + arg + ">", name, KType.Category.OBJECT);
        }
        if (name.equals("java.util.Map.Entry")) {
            String key = args.size() > 0 ? args.get(0).render() : "Any?";
            String value = args.size() > 1 ? args.get(1).render() : "Any?";
            return KType.simple("Map.Entry<" + key + ", " + value + ">", name, KType.Category.OBJECT);
        }

        KType collection = mapCollection(name, args);
        if (collection != null) {
            return collection;
        }
        KType functional = mapFunctional(name, args);
        if (functional != null) {
            return functional;
        }
        if (name.equals("java.util.Optional")) {
            KType element = args.isEmpty() ? KType.unknown("Optional") : args.get(0);
            return element.asNullable().withJavaName(name);
        }
        if (name.startsWith("java.util.stream.")) {
            if (name.endsWith("IntStream")) {
                return KType.collection("Sequence<Int>", name, KType.INT, false);
            }
            if (name.endsWith("LongStream")) {
                return KType.collection("Sequence<Long>", name, KType.LONG, false);
            }
            if (name.endsWith("DoubleStream")) {
                return KType.collection("Sequence<Double>", name, KType.DOUBLE, false);
            }
            KType element = args.isEmpty() ? KType.unknown("Stream") : args.get(0);
            return KType.collection("Sequence<" + element.render() + ">", name, element, false);
        }
        if (name.equals("java.util.regex.Pattern")) {
            return KType.simple(imports.ref("kotlin.text.Regex"), name, KType.Category.OBJECT);
        }
        if (name.equals("java.util.regex.Matcher")) {
            diagnostics.warn("J2KT-TYPE-003", "java.util.regex.Matcher has no direct Kotlin equivalent; emitted as MatchResult (review API usage).", null);
            return KType.simple(imports.ref("kotlin.text.MatchResult"), name, KType.Category.OBJECT);
        }
        if (name.equals("java.util.Random")) {
            return KType.simple(imports.ref("kotlin.random.Random"), name, KType.Category.OBJECT);
        }
        if (name.equals("java.util.UUID")) {
            if (target == Target.PUMPKIN) {
                diagnostics.warn("J2KT-TYPE-004",
                        "java.util.UUID is not available in Kotlin/Wasm. Pumpkin exposes pumpkin.Uuid.Uuid (high/low u64).",
                        null);
                return KType.simple(imports.ref("pumpkin.Uuid.Uuid"), name, KType.Category.OBJECT);
            }
            return KType.simple(imports.ref("java.util.UUID"), name, KType.Category.OBJECT);
        }
        if (name.equals("java.util.Objects")) {
            return KType.simple("Any", name, KType.Category.OBJECT).asNullable();
        }
        if (name.startsWith("java.time.") || name.startsWith("java.io.") || name.startsWith("java.nio.")
                || name.startsWith("java.util.concurrent.") || name.startsWith("java.lang.reflect.")
                || name.startsWith("java.security.") || name.startsWith("java.math.")
                || name.startsWith("java.text.") || name.startsWith("java.net.") || name.startsWith("javax.")) {
            if (target == Target.PUMPKIN) {
                diagnostics.warn("J2KT-TYPE-005",
                        "'" + name + "' is not part of the Kotlin/Wasm + Pumpkin API surface. The import was kept; this file will not compile for wasm until it is ported.",
                        null);
            }
            return KType.simple(imports.ref(name), name, KType.Category.OBJECT);
        }

        KType pumpkin = mapPumpkin(name, args);
        if (pumpkin != null) {
            return pumpkin;
        }

        // Project or third-party class.
        String rendered = imports.ref(name);
        if (!args.isEmpty()) {
            rendered += "<" + joinArgs(args) + ">";
        }
        return KType.simple(rendered, name, KType.Category.OBJECT);
    }

    private KType mapCollection(String name, List<KType> args) {
        String element = args.isEmpty() ? "Any?" : args.get(0).render();
        String value = args.size() > 1 ? args.get(1).render() : "Any?";
        return switch (name) {
            case "java.util.List", "java.util.ArrayList", "java.util.LinkedList",
                 "java.util.Collection", "java.util.AbstractList" ->
                    KType.collection("MutableList<" + element + ">", name,
                            args.isEmpty() ? KType.unknown("element").asNullable() : args.get(0), true);
            case "java.util.Set", "java.util.HashSet", "java.util.LinkedHashSet", "java.util.AbstractSet" ->
                    KType.collection("MutableSet<" + element + ">", name,
                            args.isEmpty() ? KType.unknown("element").asNullable() : args.get(0), true);
            case "java.util.SortedSet", "java.util.TreeSet", "java.util.NavigableSet" ->
                    KType.collection("MutableSet<" + element + ">", name,
                            args.isEmpty() ? KType.unknown("element").asNullable() : args.get(0), true);
            case "java.util.Map", "java.util.HashMap", "java.util.LinkedHashMap", "java.util.AbstractMap" ->
                    KType.map("MutableMap<" + (args.size() > 0 ? args.get(0).render() : "Any?") + ", " + value + ">", name,
                            args.size() > 0 ? args.get(0) : KType.unknown("key").asNullable(),
                            args.size() > 1 ? args.get(1) : KType.unknown("value").asNullable(), true);
            case "java.util.SortedMap", "java.util.TreeMap", "java.util.NavigableMap" ->
                    KType.map("MutableMap<" + (args.size() > 0 ? args.get(0).render() : "Any?") + ", " + value + ">", name,
                            args.size() > 0 ? args.get(0) : KType.unknown("key").asNullable(),
                            args.size() > 1 ? args.get(1) : KType.unknown("value").asNullable(), true);
            case "java.util.Queue", "java.util.Deque", "java.util.ArrayDeque" ->
                    KType.collection("ArrayDeque<" + element + ">", name,
                            args.isEmpty() ? KType.unknown("element").asNullable() : args.get(0), true);
            case "java.lang.Iterable" ->
                    KType.collection("Iterable<" + element + ">", name,
                            args.isEmpty() ? KType.unknown("element").asNullable() : args.get(0), false);
            case "java.util.Iterator" ->
                    KType.collection("Iterator<" + element + ">", name,
                            args.isEmpty() ? KType.unknown("element").asNullable() : args.get(0), false);
            default -> null;
        };
    }

    private KType mapFunctional(String name, List<KType> args) {
        if (name.equals("java.lang.Runnable")) {
            if (target == Target.PUMPKIN) {
                diagnostics.warn("J2KT-TYPE-006",
                        "java.lang.Runnable is not part of Kotlin/Wasm; mapped to () -> Unit.", null);
                return KType.function("() -> Unit", name);
            }
            return KType.simple(imports.ref(name), name, KType.Category.OBJECT);
        }
        String a = args.size() > 0 ? args.get(0).render() : "Any?";
        String b = args.size() > 1 ? args.get(1).render() : "Any?";
        String c = args.size() > 2 ? args.get(2).render() : "Any?";
        String rendered = switch (name) {
            case "java.util.concurrent.Callable" -> "() -> " + a;
            case "java.util.function.Supplier" -> "() -> " + a;
            case "java.util.function.Consumer" -> "(" + a + ") -> Unit";
            case "java.util.function.BiConsumer" -> "(" + a + ", " + b + ") -> Unit";
            case "java.util.function.Function" -> "(" + a + ") -> " + b;
            case "java.util.function.BiFunction" -> "(" + a + ", " + b + ") -> " + c;
            case "java.util.function.Predicate" -> "(" + a + ") -> Boolean";
            case "java.util.function.BiPredicate" -> "(" + a + ", " + b + ") -> Boolean";
            case "java.util.function.UnaryOperator" -> "(" + a + ") -> " + a;
            case "java.util.function.BinaryOperator" -> "(" + a + ", " + a + ") -> " + a;
            case "java.util.Comparator" -> "Comparator<" + a + ">";
            default -> null;
        };
        if (rendered == null) {
            return null;
        }
        if (rendered.startsWith("Comparator<")) {
            return KType.simple(rendered, name, KType.Category.OBJECT);
        }
        return KType.function(rendered, name);
    }

    private KType mapPumpkin(String name, List<KType> args) {
        if (target != Target.PUMPKIN) {
            return null;
        }
        String simple = Imports.simpleName(name);
        if (simple.equals("Position") || simple.equals("Location")) {
            return KType.simple("Triple<Double, Double, Double>", "pumpkin.Position", KType.Category.OBJECT);
        }
        if (simple.equals("Scoreboard")) {
            String outer = imports.ref("pumpkin.Scoreboard");
            return KType.simple(outer + ".Scoreboard", "pumpkin.Scoreboard.Scoreboard", KType.Category.OBJECT);
        }
        if (Set.of("RenderType", "DisplaySlot", "NametagVisibility", "CollisionRule", "TeamSettings").contains(simple)) {
            String outer = imports.ref("pumpkin.Scoreboard");
            return KType.simple(outer + "." + simple, "pumpkin.Scoreboard." + simple, KType.Category.OBJECT);
        }
        if (simple.equals("JavaDialogs")) {
            return KType.simple(imports.ref("pumpkin.JavaDialogs"), "pumpkin.JavaDialogs", KType.Category.OBJECT);
        }
        if (Set.of("DialogType", "AfterAction", "LinkType", "DialogBody", "PlainMessage", "ItemBody",
                "DialogInputBool", "DialogInputText", "DialogInputNumberRange", "DialogInputSingleOption",
                "DialogInput", "BoolInput", "TextInput", "NumberRangeInput", "SingleOptionInput",
                "CustomClickAction", "Action", "OpenUrl", "CustomClick", "ActionButton",
                "LinkLabel", "BuiltIn", "CustomLabel", "Link", "Dialog").contains(simple)) {
            String outer = imports.ref("pumpkin.JavaDialogs");
            return KType.simple(outer + "." + simple, "pumpkin.JavaDialogs." + simple, KType.Category.OBJECT);
        }
        if (simple.equals("Forms")) {
            return KType.simple(imports.ref("pumpkin.Forms"), "pumpkin.Forms", KType.Category.OBJECT);
        }
        if (Set.of("Form", "SimpleForm", "ModalForm", "CustomForm", "SimpleFormButton",
                "FormImage", "CustomFormElement", "ImageType").contains(simple)) {
            String outer = imports.ref("pumpkin.Forms");
            return KType.simple(outer + "." + simple, "pumpkin.Forms." + simple, KType.Category.OBJECT);
        }
        if (simple.equals("Recipe")) {
            return KType.simple(imports.ref("pumpkin.Recipe"), "pumpkin.Recipe", KType.Category.OBJECT);
        }
        if (Set.of("RecipeCategory", "Ingredient", "ShapedRecipe", "ShapelessRecipe",
                "CookingRecipe", "CookingType", "RecipeManager").contains(simple)) {
            String outer = imports.ref("pumpkin.Recipe");
            return KType.simple(outer + "." + simple, "pumpkin.Recipe." + simple, KType.Category.OBJECT);
        }
        if (simple.equals("Datapack")) {
            return KType.simple(imports.ref("pumpkin.Datapack"), "pumpkin.Datapack", KType.Category.OBJECT);
        }
        if (Set.of("DatapackInfo", "EnablePosition", "DatapackManager").contains(simple)) {
            String outer = imports.ref("pumpkin.Datapack");
            return KType.simple(outer + "." + simple, "pumpkin.Datapack." + simple, KType.Category.OBJECT);
        }
        if (simple.equals("Enchantments")) {
            return KType.simple(imports.ref("pumpkin.Enchantments"), "pumpkin.Enchantments", KType.Category.OBJECT);
        }
        if (Set.of("AttributeModifierSlot", "Enchantment", "CustomEnchantment", "EnchantmentManager").contains(simple)) {
            String outer = imports.ref("pumpkin.Enchantments");
            return KType.simple(outer + "." + simple, "pumpkin.Enchantments." + simple, KType.Category.OBJECT);
        }
        if (simple.equals("Display")) {
            return KType.simple(imports.ref("pumpkin.Display"), "pumpkin.Display", KType.Category.OBJECT);
        }
        if (Set.of("BillboardMode", "ItemDisplayMode", "TextAlignment", "Vector3f", "Quaternionf",
                "DisplayTransformation", "DisplayEntity", "BlockDisplayEntity", "ItemDisplayEntity",
                "TextDisplayEntity").contains(simple)) {
            String outer = imports.ref("pumpkin.Display");
            return KType.simple(outer + "." + simple, "pumpkin.Display." + simple, KType.Category.OBJECT);
        }
        if (Set.of("Block", "BlockState").contains(simple)) {
            String world = imports.ref("pumpkin.World");
            return KType.simple(world + "." + simple, "pumpkin.World." + simple, KType.Category.OBJECT);
        }
        if (Set.of("Mob", "LivingEntity").contains(simple)) {
            String world = imports.ref("pumpkin.World");
            return KType.simple(world + "." + simple, "pumpkin.World." + simple, KType.Category.OBJECT);
        }
        if (simple.equals("BossBar")) {
            String bossBar = imports.ref("pumpkin.BossBar");
            return KType.simple(bossBar + ".BossBar", "pumpkin.BossBar.BossBar", KType.Category.OBJECT);
        }
        if (simple.equals("BossBarColor") || simple.equals("BossBarDivision")) {
            String bossBar = imports.ref("pumpkin.BossBar");
            return KType.simple(bossBar + "." + simple, "pumpkin.BossBar." + simple, KType.Category.OBJECT);
        }
        if (simple.equals("EntityTypes")) {
            return KType.simple(imports.ref("pumpkin.EntityTypes"), "pumpkin.EntityTypes", KType.Category.OBJECT);
        }
        if (simple.equals("EntityType")) {
            return KType.simple(imports.ref("pumpkin.EntityTypes") + ".EntityType",
                    "pumpkin.EntityTypes.EntityType", KType.Category.OBJECT);
        }
        if (simple.equals("AiGoal")) {
            return KType.simple(imports.ref("plugin.AiGoal"), "plugin.AiGoal", KType.Category.OBJECT);
        }
        if (simple.equals("AiGoalRegistry")) {
            return KType.simple(imports.ref("plugin.AiGoalRegistry"), "plugin.AiGoalRegistry", KType.Category.OBJECT);
        }
        if (simple.equals("Gui")) {
            String outer = imports.ref("pumpkin.Gui");
            return KType.simple(outer + ".Gui", "pumpkin.Gui.Gui", KType.Category.OBJECT);
        }
        if (simple.equals("Screens")) {
            return KType.simple(imports.ref("pumpkin.Screens"), "pumpkin.Screens", KType.Category.OBJECT);
        }
        if (simple.equals("Screen")) {
            String outer = imports.ref("pumpkin.Screens");
            return KType.simple(outer + ".Screen", "pumpkin.Screens.Screen", KType.Category.OBJECT);
        }
        if (simple.equals("Permission")) {
            String outer = imports.ref("pumpkin.Permission");
            return KType.simple(outer + ".Permission", "pumpkin.Permission.Permission", KType.Category.OBJECT);
        }
        if (simple.equals("PermissionChild")) {
            String outer = imports.ref("pumpkin.Permission");
            return KType.simple(outer + ".PermissionChild", "pumpkin.Permission.PermissionChild", KType.Category.OBJECT);
        }
        if (Set.of("JavaPlayer", "BedrockPlayer").contains(simple)) {
            String player = imports.ref("pumpkin.Player");
            return KType.simple(player + "." + simple, "pumpkin.Player." + simple, KType.Category.OBJECT);
        }
        if (simple.equals("JavaPackets")) {
            return KType.simple(imports.ref("pumpkin.JavaPackets"), "pumpkin.JavaPackets", KType.Category.OBJECT);
        }
        if (simple.equals("Unknown")) {
            String packets = imports.ref("pumpkin.BedrockPackets");
            return KType.simple(packets + ".ClientboundPacket.Unknown",
                    "pumpkin.BedrockPackets.ClientboundPacket.Unknown", KType.Category.OBJECT);
        }
        if (simple.equals("BedrockPackets")) {
            return KType.simple(imports.ref("pumpkin.BedrockPackets"), "pumpkin.BedrockPackets", KType.Category.OBJECT);
        }
        if (Set.of("CPlayDisconnect", "CKeepAlive", "CGameEvent", "CTransfer").contains(simple)) {
            String packets = imports.ref("pumpkin.JavaPackets");
            return KType.simple(packets + "." + simple, "pumpkin.JavaPackets." + simple, KType.Category.OBJECT);
        }
        if (Set.of("CDisconnect", "CSetTime", "CModalFormRequest").contains(simple)) {
            String packets = imports.ref("pumpkin.BedrockPackets");
            return KType.simple(packets + "." + simple, "pumpkin.BedrockPackets." + simple, KType.Category.OBJECT);
        }
        if (simple.equals("CCustomPayload")) {
            String packets = imports.ref("pumpkin.JavaPackets");
            return KType.simple(packets + ".ClientboundPacket.CCustomPayload",
                    "pumpkin.JavaPackets.ClientboundPacket.CCustomPayload", KType.Category.OBJECT);
        }
        if (simple.equals("ClientboundPacket")) {
            String packets = imports.ref("pumpkin.JavaPackets");
            return KType.simple(packets + ".ClientboundPacket", "pumpkin.JavaPackets.ClientboundPacket", KType.Category.OBJECT);
        }
        if (Set.of("Command", "CommandSender", "CommandNode", "ConsumedArgs", "SuggestionRequest",
                "CommandSuggestion", "CommandSuggestions", "PermissionLevel").contains(simple)) {
            String outer = imports.ref("pumpkin.Command");
            return KType.simple(outer + "." + simple, "pumpkin.Command." + simple, KType.Category.OBJECT);
        }
        String fq = switch (simple) {
            case "PumpkinPlugin" -> compatPlugin
                    ? "plugin.PumpkinCompatPlugin"
                    : "plugin.PumpkinPlugin";
            case "PluginContext" -> "plugin.PluginContext";
            case "PluginMetadata" -> "plugin.PluginMetadata";
            case "TaskScheduler" -> "plugin.PluginTasks";
            case "ScheduledTask" -> "plugin.ScheduledTask";
            case "Player" -> "pumpkin.Player.Player";
            case "World" -> "pumpkin.World.World";
            case "Entity" -> "pumpkin.World.Entity";
            case "TextComponent" -> "pumpkin.Text.TextComponent";
            case "IpcRegistry" -> "pumpkin.Ipc";
            case "GameMode" -> "pumpkin.Common.GameMode";
            case "Hand" -> "pumpkin.Common.Hand";
            case "BlockPos" -> "pumpkin.Common.BlockPos";
            case "NamedColor" -> "pumpkin.Common.NamedColor";
            case "RgbColor" -> "pumpkin.Common.RgbColor";
            case "ArgbColor" -> "pumpkin.Common.ArgbColor";
            case "Server" -> "pumpkin.Server.Server";
            case "ItemStack" -> "pumpkin.ItemStack.ItemStack";
            case "Inventory" -> "pumpkin.Inventory.Inventory";
            case "PlayerInventory" -> "pumpkin.Inventory.PlayerInventory";
            case "Logging" -> "pumpkin.Logging";
            case "Event" -> "pumpkin.Event";
            case "EventType" -> "pumpkin.Event.EventType";
            case "EventPriority" -> "pumpkin.Event.EventPriority";
            case "EventSubscription" -> "plugin.EventSubscription";
            case "PermissionDefault" -> "pumpkin.Permission.PermissionDefault";
            case "Uuid", "UUID" -> "pumpkin.Uuid.Uuid";
            default -> null;
        };
        if (fq == null && name.startsWith("pumpkin.Event.")) {
            fq = simple.endsWith("EventData")
                    ? name
                    : "pumpkin.Event.Event." + simple;
        }
        if (fq == null && Set.of("PlayerJoinEvent", "PlayerLeaveEvent", "PlayerChatEvent",
                "PlayerMoveEvent", "BlockBreakEvent", "BlockPlaceEvent", "PlayerDeathEvent",
                "EntityDamageEvent").contains(simple)) {
            fq = "pumpkin.Event.Event." + simple;
        }
        if (fq == null) {
            return null;
        }
        if (fq.equals("pumpkin.Event.EventType")) {
            String eventClass = imports.ref("pumpkin.Event");
            return KType.simple(eventClass + ".EventType", fq, KType.Category.OBJECT);
        }
        if (fq.equals("pumpkin.Event.EventPriority")) {
            String eventClass = imports.ref("pumpkin.Event");
            return KType.simple(eventClass + ".EventPriority", fq, KType.Category.OBJECT);
        }
        if (fq.startsWith("pumpkin.Event.Event.")) {
            String eventClass = imports.ref("pumpkin.Event");
            return KType.simple(eventClass + ".Event." + Imports.simpleName(fq), fq, KType.Category.OBJECT);
        }
        if (fq.equals("pumpkin.Permission.PermissionDefault")) {
            String permissionClass = imports.ref("pumpkin.Permission");
            return KType.simple(permissionClass + ".PermissionDefault", fq, KType.Category.OBJECT);
        }
        String rendered = imports.ref(fq);
        if (!args.isEmpty()) {
            rendered += "<" + joinArgs(args) + ">";
        }
        return KType.simple(rendered, fq, KType.Category.OBJECT);
    }

    private String joinArgs(List<KType> args) {
        List<String> parts = new ArrayList<>();
        for (KType arg : args) {
            parts.add(arg.render());
        }
        return String.join(", ", parts);
    }

    private String qualifiedName(ClassOrInterfaceType type) {
        try {
            return type.resolve().asReferenceType().getQualifiedName();
        } catch (Throwable ignored) {
            // Symbol solving is optional; fall back to source spelling.
        }
        String scoped = type.getNameWithScope();
        if (scoped.contains(".")) {
            if (Character.isLowerCase(scoped.charAt(0))) {
                return scoped;
            }
            String outer = scoped.substring(0, scoped.indexOf('.'));
            String remaining = scoped.substring(scoped.indexOf('.'));
            String outerJdk = jdkQualifiedName(outer);
            if (outerJdk != null) {
                return outerJdk + remaining;
            }
            String outerKnown = imports.lookup(outer);
            if (outerKnown != null) {
                return outerKnown + remaining;
            }
            String pkg = currentPackageForQualification();
            return pkg.isEmpty() ? scoped : pkg + "." + scoped;
        }
        String known = imports.lookup(scoped);
        if (known != null) {
            return known;
        }
        String jdk = jdkQualifiedName(scoped);
        if (jdk != null) {
            return jdk;
        }
        return imports.qualify(scoped);
    }

    private String currentPackageForQualification() {
        return imports.currentPackage();
    }

    private String jdkQualifiedName(String simple) {
        return switch (simple) {
            case "String", "Object", "Integer", "Long", "Short", "Byte", "Double", "Float",
                 "Boolean", "Character", "Void", "Class", "CharSequence", "Throwable", "Exception",
                 "RuntimeException", "Error", "StringBuilder", "StringBuffer", "Math", "System",
                 "Thread", "Enum", "Iterable", "Runnable", "IllegalArgumentException",
                 "IllegalStateException", "NullPointerException", "IndexOutOfBoundsException",
                 "UnsupportedOperationException", "ArithmeticException" ->
                    "java.lang." + simple;
            case "List", "ArrayList", "LinkedList", "Vector", "Set", "HashSet", "LinkedHashSet",
                 "TreeSet", "SortedSet", "NavigableSet", "Map", "HashMap", "LinkedHashMap",
                 "TreeMap", "SortedMap", "NavigableMap", "Collection", "Queue", "Deque", "ArrayDeque",
                 "Iterator", "Optional", "Objects", "Arrays", "Collections", "UUID", "Random",
                 "Comparator" ->
                    "java.util." + simple;
            case "Function", "BiFunction", "Consumer", "BiConsumer", "Supplier", "Predicate",
                 "BiPredicate", "UnaryOperator", "BinaryOperator" ->
                    "java.util.function." + simple;
            case "Stream", "IntStream", "LongStream", "DoubleStream", "Collectors" ->
                    "java.util.stream." + simple;
            case "Pattern", "Matcher" -> "java.util.regex." + simple;
            case "LocalDate", "LocalDateTime", "Instant", "Duration", "ZonedDateTime", "OffsetDateTime" ->
                    "java.time." + simple;
            case "IOException", "File", "FileReader", "FileWriter", "BufferedReader", "BufferedWriter",
                 "InputStream", "OutputStream", "PrintWriter" ->
                    "java.io." + simple;
            default -> null;
        };
    }

    /** Best-effort check used to omit constructor parentheses on anonymous interfaces. */
    public boolean isInterface(ClassOrInterfaceType type) {
        try {
            return type.resolve().asReferenceType().getTypeDeclaration()
                    .map(declaration -> declaration.isInterface())
                    .orElse(false);
        } catch (Throwable ignored) {
            String simple = type.getNameAsString();
            return Set.of("Runnable", "Callable", "Comparator", "Function", "BiFunction",
                    "Consumer", "BiConsumer", "Supplier", "Predicate", "BiPredicate",
                    "UnaryOperator", "BinaryOperator", "AiGoal").contains(simple);
        }
    }

    /** Raw class spelling for anonymous subclasses (functional interfaces stay interfaces). */
    public String rawClassName(ClassOrInterfaceType type) {
        String fq = qualifiedName(type);
        String rendered = imports.ref(fq);
        if (type.getTypeArguments().isPresent() && !type.getTypeArguments().get().isEmpty()) {
            List<String> args = new ArrayList<>();
            for (Type argument : type.getTypeArguments().get()) {
                args.add(map(argument).render());
            }
            rendered += "<" + String.join(", ", args) + ">";
        }
        return rendered;
    }

    /** Maps a class name in the current compilation unit. */
    public KType mapNameForClass(String simpleName, String packageName) {
        String fq = packageName == null || packageName.isBlank()
                ? simpleName
                : packageName + "." + simpleName;
        String rendered = imports.ref(fq);
        return KType.simple(rendered, fq, KType.Category.OBJECT);
    }

    /** Default field value used when a Java field has no initializer. */
    public String defaultValue(KType type) {
        if (type.isNullable() || type.category() == KType.Category.NULL) {
            return "null";
        }
        return switch (type.base()) {
            case "Int" -> "0";
            case "Long" -> "0L";
            case "Short" -> "0";
            case "Byte" -> "0";
            case "Float" -> "0.0f";
            case "Double" -> "0.0";
            case "Boolean" -> "false";
            case "Char" -> "'\\u0000'";
            default -> "null";
        };
    }

    /** True when Kotlin needs an explicit numeric conversion. */
    public boolean needsNumericConversion(KType from, KType to) {
        if (!from.isNumeric() || !to.isNumeric()) {
            return false;
        }
        if (from.base().equals(to.base())) {
            return false;
        }
        // Widening conversions still have to be explicit in Kotlin.
        return true;
    }

    public String numericConversion(String expression, KType to) {
        return switch (to.base()) {
            case "Byte" -> expression + ".toByte()";
            case "Short" -> expression + ".toShort()";
            case "Int" -> expression + ".toInt()";
            case "Long" -> expression + ".toLong()";
            case "Float" -> expression + ".toFloat()";
            case "Double" -> expression + ".toDouble()";
            case "Char" -> expression + ".toChar()";
            default -> expression;
        };
    }

    public Target target() {
        return target;
    }

    public Diagnostics diagnostics() {
        return diagnostics;
    }

    public String lowerFirst(String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }
        return value.substring(0, 1).toLowerCase(Locale.ROOT) + value.substring(1);
    }
}
