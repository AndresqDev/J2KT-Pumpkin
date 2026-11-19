package dev.j2kt;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.AnnotationDeclaration;
import com.github.javaparser.ast.body.AnnotationMemberDeclaration;
import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.CompactConstructorDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.EnumConstantDeclaration;
import com.github.javaparser.ast.body.EnumDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.InitializerDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.body.RecordDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.comments.Comment;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.ArrayAccessExpr;
import com.github.javaparser.ast.expr.ArrayCreationExpr;
import com.github.javaparser.ast.expr.ArrayInitializerExpr;
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
import com.github.javaparser.ast.expr.MarkerAnnotationExpr;
import com.github.javaparser.ast.expr.MemberValuePair;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.MethodReferenceExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.NormalAnnotationExpr;
import com.github.javaparser.ast.expr.NullLiteralExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.expr.SingleMemberAnnotationExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.expr.SuperExpr;
import com.github.javaparser.ast.expr.SwitchExpr;
import com.github.javaparser.ast.expr.TextBlockLiteralExpr;
import com.github.javaparser.ast.expr.ThisExpr;
import com.github.javaparser.ast.expr.UnaryExpr;
import com.github.javaparser.ast.expr.VariableDeclarationExpr;
import com.github.javaparser.ast.stmt.AssertStmt;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.BreakStmt;
import com.github.javaparser.ast.stmt.CatchClause;
import com.github.javaparser.ast.stmt.ContinueStmt;
import com.github.javaparser.ast.stmt.DoStmt;
import com.github.javaparser.ast.stmt.EmptyStmt;
import com.github.javaparser.ast.stmt.ExplicitConstructorInvocationStmt;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.ForEachStmt;
import com.github.javaparser.ast.stmt.ForStmt;
import com.github.javaparser.ast.stmt.IfStmt;
import com.github.javaparser.ast.stmt.LabeledStmt;
import com.github.javaparser.ast.stmt.LocalClassDeclarationStmt;
import com.github.javaparser.ast.stmt.ReturnStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.stmt.SwitchEntry;
import com.github.javaparser.ast.stmt.SwitchStmt;
import com.github.javaparser.ast.stmt.SynchronizedStmt;
import com.github.javaparser.ast.stmt.ThrowStmt;
import com.github.javaparser.ast.stmt.TryStmt;
import com.github.javaparser.ast.stmt.WhileStmt;
import com.github.javaparser.ast.stmt.YieldStmt;
import com.github.javaparser.ast.type.ArrayType;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.ast.type.TypeParameter;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.UnaryOperator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Java AST -> Kotlin source emitter.
 *
 * The emitter optimises for reviewability and semantic transparency, not for
 * producing the prettiest Kotlin possible. When Java semantics have no direct
 * Kotlin equivalent, it emits a conservative translation and a diagnostic
 * instead of silently changing behaviour.
 */
public final class KotlinEmitter {
    private static final Pattern GETTER = Pattern.compile("^(get|is)([A-Z_].*)$");
    private static final Pattern SETTER = Pattern.compile("^set([A-Z_].*)$");

    private record Promotion(ConstructorDeclaration constructor, List<String> parameters, Set<String> fields) {
    }

    private final Target target;
    private final TranslateOptions options;
    private final Diagnostics diagnostics;
    private final Imports imports;
    private final TypeMapper types;
    private final Scope scope;
    private final TypeOracle oracle;
    private KotlinWriter out;
    private CommentRenderer comments;
    private final String currentPackage;

    private final Deque<String> classStack = new ArrayDeque<>();
    private final Deque<Map<String, String>> aliases = new ArrayDeque<>();
    private final Deque<Promotion> promotions = new java.util.LinkedList<>();
    private final Map<String, Set<String>> recordComponents = new HashMap<>();
    private final Set<String> overriddenMethods = new LinkedHashSet<>();
    private boolean currentClassFinal;
    private boolean currentClassInterface;
    private boolean pumpkinPluginSubclass;
    private boolean inResultBlock;
    private boolean switchFallthroughMode;
    private KType currentMethodReturn = KType.UNIT;
    private String pendingLabel;
    private String currentLambdaLabel;
    private UnaryOperator<String> currentLambdaReturnWrapper;
    private int lambdaCounter;
    private String currentClass = "";
    private Node currentBody;

    public KotlinEmitter(Target target, TranslateOptions options, Diagnostics diagnostics, String currentPackage) {
        this.target = target;
        this.options = options;
        this.diagnostics = diagnostics;
        this.currentPackage = currentPackage == null ? "" : currentPackage;
        this.imports = new Imports(this.currentPackage);
        this.types = new TypeMapper(target, imports, diagnostics, options.compatPlugin());
        this.scope = new Scope();
        this.oracle = new TypeOracle(types.silentCopy(), scope, diagnostics, options.useSymbolSolver());
        this.out = new KotlinWriter();
        this.comments = new CommentRenderer(out);
        this.aliases.push(new HashMap<>());
    }

    public String emit(CompilationUnit unit) {
        String packageName = unit.getPackageDeclaration().map(p -> p.getNameAsString()).orElse("");

        registerOriginalImports(unit);
        for (RecordDeclaration record : unit.findAll(RecordDeclaration.class)) {
            Set<String> components = new LinkedHashSet<>();
            for (Parameter parameter : record.getParameters()) {
                components.add(parameter.getNameAsString());
            }
            recordComponents.put(record.getNameAsString(), components);
        }

        // Package and imports must precede the body, so emit the body first into a
        // separate buffer and splice the header at the end.
        String body = emitTypes(unit);

        StringBuilder result = new StringBuilder();
        if (options.emitHeader()) {
            result.append("// Generated by j2kt-pumpkin.\n");
            result.append("// Review TODO(j2kt) comments and diagnostics before compiling.\n\n");
        }
        if (!packageName.isBlank()) {
            result.append("package ").append(packageName).append("\n\n");
        }
        List<String> renderedImports = imports.render();
        for (String importLine : renderedImports) {
            result.append(importLine).append('\n');
        }
        if (!renderedImports.isEmpty()) {
            result.append('\n');
        }
        result.append(body);
        return result.toString();
    }

    private void registerOriginalImports(CompilationUnit unit) {
        for (ImportDeclaration declaration : unit.getImports()) {
            String name = declaration.getNameAsString();
            if (declaration.isStatic()) {
                if (name.startsWith("java.lang.Math.") || name.startsWith("java.lang.Integer.")
                        || name.startsWith("java.lang.Long.") || name.startsWith("java.lang.Double.")
                        || name.startsWith("java.lang.Float.") || name.startsWith("java.lang.Boolean.")
                        || name.startsWith("java.lang.Character.") || name.startsWith("java.util.Objects.")
                        || name.startsWith("java.util.Arrays.") || name.startsWith("java.util.Collections.")) {
                    continue;
                }
                imports.raw(name + (declaration.isAsterisk() ? ".*" : ""));
                continue;
            }
            if (target == Target.PUMPKIN && name.startsWith("pumpkin.")) {
                continue;
            }
            if (target == Target.PUMPKIN && (name.startsWith("java.time.")
                    || name.startsWith("java.io.") || name.startsWith("java.nio.")
                    || name.startsWith("java.util.concurrent.") || name.startsWith("java.lang.reflect.")
                    || name.startsWith("java.math.") || name.startsWith("java.text.")
                    || name.startsWith("java.net.") || name.startsWith("javax."))) {
                diagnostics.warn("J2KT-TYPE-005",
                        "'" + name + "' is not part of Kotlin/Wasm + Pumpkin; the import was kept for review.",
                        declaration);
            }
            if (shouldDropImport(name)) {
                continue;
            }
            if (declaration.isAsterisk()) {
                imports.wildcard(name);
            } else {
                imports.preserve(name);
            }
        }
    }

    private boolean shouldDropImport(String fq) {
        if (fq.equals("java.lang") || fq.startsWith("java.lang.")) {
            return true;
        }
        if (fq.equals("java.util")) {
            return true;
        }
        if (fq.startsWith("java.util.") && !fq.startsWith("java.util.regex.")
                && !fq.startsWith("java.util.concurrent.") && !fq.startsWith("java.util.stream.")) {
            return true;
        }
        if (fq.equals("java.util.function") || fq.equals("java.util.stream") || fq.equals("java.util.regex")
                || fq.startsWith("java.util.function.") || fq.startsWith("java.util.stream.")) {
            return true;
        }
        if (fq.equals("java.util.Objects") || fq.equals("java.util.Arrays") || fq.equals("java.util.Collections")
                || fq.equals("java.util.Optional")) {
            return true;
        }
        if (target == Target.PUMPKIN && (fq.startsWith("pumpkin.") || fq.startsWith("plugin."))) {
            return true;
        }
        return false;
    }

    private String emitTypes(CompilationUnit unit) {
        for (TypeDeclaration<?> type : unit.getTypes()) {
            emitType(type);
            out.blankLine();
        }
        return out.toString();
    }

    private void emitType(TypeDeclaration<?> type) {
        if (type instanceof ClassOrInterfaceDeclaration declaration) {
            emitClassOrInterface(declaration);
        } else if (type instanceof EnumDeclaration declaration) {
            emitEnum(declaration);
        } else if (type instanceof RecordDeclaration declaration) {
            emitRecord(declaration);
        } else if (type instanceof AnnotationDeclaration declaration) {
            emitAnnotation(declaration);
        } else {
            diagnostics.warn("J2KT-TYPE-010", "Unsupported top-level declaration: " + type.getClass().getSimpleName(), type);
        }
    }

    private void emitClassOrInterface(ClassOrInterfaceDeclaration declaration) {
        comments.javadoc(declaration);
        emitAnnotations(declaration.getAnnotations());
        String name = name(declaration.getNameAsString());
        boolean isInterface = declaration.isInterface();

        currentClassFinal = declaration.isFinal() && !isInterface;
        currentClassInterface = isInterface;
        boolean previousPumpkinSubclass = pumpkinPluginSubclass;
        pumpkinPluginSubclass = isPumpkinPluginSubclass(declaration);
        classStack.push(declaration.getNameAsString());
        currentClass = declaration.getNameAsString();
        oracle.setThisType(types.mapNameForClass(declaration.getNameAsString(), currentPackage));

        String modifiers = classModifiers(declaration, isInterface);
        String typeParameters = typeParameters(declaration.getTypeParameters());
        boolean hasSuperclass = !isInterface && !declaration.getExtendedTypes().isEmpty();
        Promotion promotion = isInterface ? null : detectPromotion(declaration);
        String supertypes = supertypes(declaration, isInterface, promotion != null);
        promotions.push(promotion);
        String primaryParens;
        if (promotion != null) {
            primaryParens = "(" + String.join(", ", promotion.parameters()) + ")";
        } else {
            primaryParens = !isInterface && !declaration.getConstructors().isEmpty() && !hasSuperclass
                    ? "()"
                    : "";
        }

        out.line(modifiers + (isInterface ? "interface " : "class ") + name + typeParameters
                + primaryParens + supertypes + " {");
        out.indent();

        emitClassBody(declaration, isInterface);

        out.dedent();
        out.line("}");
        promotions.pop();
        classStack.pop();
        currentClass = classStack.peek() == null ? "" : classStack.peek();
        pumpkinPluginSubclass = previousPumpkinSubclass;
    }

    private boolean isPumpkinPluginSubclass(ClassOrInterfaceDeclaration declaration) {
        if (target != Target.PUMPKIN) {
            return false;
        }
        return declaration.getExtendedTypes().stream()
                .anyMatch(type -> type.getNameAsString().equals("PumpkinPlugin")
                        || type.getNameAsString().equals("PumpkinCompatPlugin")
                        || type.getNameWithScope().equals("pumpkin.PumpkinPlugin"));
    }

    /**
     * Detects the common Java pattern "final field assigned from a constructor
     * parameter" and promotes it to a Kotlin primary-constructor property.
     */
    private Promotion detectPromotion(ClassOrInterfaceDeclaration declaration) {
        if (declaration.getConstructors().size() != 1) {
            return null;
        }
        ConstructorDeclaration constructor = declaration.getConstructors().get(0);
        if (!constructor.getThrownExceptions().isEmpty()
                || constructor.getComment().isPresent()
                || !constructor.getAnnotations().isEmpty()) {
            return null;
        }
        List<Statement> statements = constructor.getBody().getStatements();

        Map<String, FieldDeclaration> fieldsByName = new HashMap<>();
        for (FieldDeclaration field : declaration.getFields()) {
            if (field.isStatic() || field.getVariables().size() != 1) {
                continue;
            }
            VariableDeclarator variable = field.getVariable(0);
            fieldsByName.put(variable.getNameAsString(), field);
        }

        Set<String> promotedFields = new LinkedHashSet<>();
        Statement first = statements.isEmpty() ? null : statements.get(0);
        boolean hasConstructorInvocation = first instanceof ExplicitConstructorInvocationStmt invocation;
        if (hasConstructorInvocation && ((ExplicitConstructorInvocationStmt) first).isThis()) {
            return null;
        }
        boolean hasSuperArguments = hasConstructorInvocation
                && !((ExplicitConstructorInvocationStmt) first).getArguments().isEmpty();

        for (int i = 0; i < statements.size(); i++) {
            Statement statement = statements.get(i);
            if (i == 0 && hasConstructorInvocation) {
                continue;
            }
            if (!(statement instanceof ExpressionStmt expressionStmt)
                    || !(expressionStmt.getExpression() instanceof AssignExpr assignment)
                    || assignment.getOperator() != AssignExpr.Operator.ASSIGN) {
                return null;
            }
            String fieldName;
            Expression target = assignment.getTarget();
            if (target instanceof FieldAccessExpr fieldAccess && fieldAccess.getScope().isThisExpr()) {
                fieldName = fieldAccess.getNameAsString();
            } else {
                return null;
            }
            if (!(assignment.getValue() instanceof NameExpr value)
                    || !value.getNameAsString().equals(fieldName)) {
                return null;
            }
            FieldDeclaration field = fieldsByName.get(fieldName);
            if (field == null
                    || field.getVariables().size() != 1
                    || field.getVariable(0).getInitializer().isPresent()
                    || field.getComment().isPresent()
                    || !field.getAnnotations().isEmpty()
                    || field.isTransient()
                    || field.isVolatile()) {
                return null;
            }
            if (!promotedFields.add(fieldName)) {
                return null;
            }
        }

        if (promotedFields.isEmpty() && !hasSuperArguments) {
            return null;
        }

        List<String> parameters = new ArrayList<>();
        for (Parameter parameter : constructor.getParameters()) {
            String parameterName = parameter.getNameAsString();
            FieldDeclaration field = fieldsByName.get(parameterName);
            if (field != null && promotedFields.contains(parameterName)) {
                VariableDeclarator variable = field.getVariable(0);
                KType type = types.map(variable.getType());
                String visibility = visibility(field);
                String keyword = field.isFinal() ? "val" : "var";
                parameters.add((visibility.isEmpty() ? "" : visibility + " ") + keyword + " "
                        + name(parameterName) + ": " + type.render());
            } else if (hasSuperArguments) {
                parameters.add(name(parameterName) + ": " + types.map(parameter.getType()).render());
            } else {
                return null;
            }
        }
        return new Promotion(constructor, parameters, promotedFields);
    }

    private boolean isPromotedAccessor(MethodDeclaration method, Set<String> promotedFields) {
        if (promotedFields.isEmpty() || method.getBody().isEmpty()) {
            return false;
        }
        Matcher getter = GETTER.matcher(method.getNameAsString());
        if (getter.matches() && getter.group(1).equals("get") && method.getParameters().isEmpty()) {
            String property = propertyName(getter.group(2));
            if (!promotedFields.contains(property)) {
                return false;
            }
            List<Statement> statements = method.getBody().get().getStatements();
            return statements.size() == 1 && statements.get(0) instanceof ReturnStmt returnStmt
                    && returnStmt.getExpression().isPresent()
                    && isFieldReference(returnStmt.getExpression().get(), property);
        }
        Matcher setter = SETTER.matcher(method.getNameAsString());
        if (setter.matches() && method.getParameters().size() == 1) {
            String property = propertyName(setter.group(1));
            if (!promotedFields.contains(property)) {
                return false;
            }
            List<Statement> statements = method.getBody().get().getStatements();
            return statements.size() == 1
                    && statements.get(0) instanceof ExpressionStmt expressionStmt
                    && expressionStmt.getExpression() instanceof AssignExpr assignment
                    && assignment.getTarget() instanceof FieldAccessExpr fieldAccess
                    && fieldAccess.getScope().isThisExpr()
                    && fieldAccess.getNameAsString().equals(property);
        }
        return false;
    }

    private boolean isFieldReference(Expression expression, String field) {
        if (expression instanceof NameExpr name) {
            return name.getNameAsString().equals(field);
        }
        if (expression instanceof FieldAccessExpr access) {
            return access.getScope().isThisExpr() && access.getNameAsString().equals(field);
        }
        return false;
    }

    private boolean isSamInterface(ClassOrInterfaceDeclaration declaration) {
        if (!declaration.isInterface()) {
            return false;
        }
        long abstractMethods = declaration.getMethods().stream()
                .filter(method -> method.getBody().isEmpty() && !method.isStatic() && !method.isPrivate())
                .count();
        return abstractMethods == 1;
    }

    private String classModifiers(ClassOrInterfaceDeclaration declaration, boolean isInterface) {
        StringBuilder result = new StringBuilder();
        String visibility = visibility(declaration);
        if (!visibility.isEmpty()) {
            result.append(visibility).append(' ');
        }
        if (isInterface) {
            if (isSealed(declaration)) {
                result.append("sealed ");
            }
            if (hasAnnotation(declaration, "FunctionalInterface") || isSamInterface(declaration)) {
                result.append("fun ");
            }
            return result.toString();
        }
        if (declaration.isAbstract()) {
            result.append("abstract ");
        } else if (!declaration.isFinal() && !isSealed(declaration)) {
            // Java classes and their methods are open by default.
            result.append("open ");
        }
        if (isSealed(declaration)) {
            result.append("sealed ");
        }
        return result.toString();
    }

    private String supertypes(ClassOrInterfaceDeclaration declaration, boolean isInterface,
                              boolean promotedConstructor) {
        List<String> entries = new ArrayList<>();
        List<ClassOrInterfaceType> extended = declaration.getExtendedTypes();
        List<ClassOrInterfaceType> implemented = declaration.getImplementedTypes();

        if (isInterface) {
            for (ClassOrInterfaceType type : extended) {
                entries.add(types.map(type).render());
            }
        } else {
            String superArguments = superArguments(declaration, promotedConstructor);
            for (ClassOrInterfaceType type : extended) {
                String simple = type.getNameAsString();
                if (simple.equals("Object")) {
                    continue;
                }
                entries.add(types.map(type).render() + "(" + superArguments + ")");
            }
            for (ClassOrInterfaceType type : implemented) {
                entries.add(types.map(type).render());
            }
        }
        if (entries.isEmpty()) {
            return "";
        }
        return " : " + String.join(", ", entries);
    }

    private void emitClassBody(ClassOrInterfaceDeclaration declaration, boolean isInterface) {
        List<BodyDeclaration<?>> members = declaration.getMembers();
        boolean companionUsed = members.stream().anyMatch(this::belongsToCompanion);
        registerMembers(members);
        Promotion promotion = promotions.peek();

        // Instance fields and initializer blocks must keep their relative order.
        for (BodyDeclaration<?> member : members) {
            if (member instanceof FieldDeclaration field && !field.isStatic()) {
                if (promotion != null && field.getVariables().size() == 1
                        && promotion.fields().contains(field.getVariable(0).getNameAsString())) {
                    continue;
                }
                emitField(field, false, isInterface);
                out.blankLine();
            } else if (member instanceof InitializerDeclaration initializer && !initializer.isStatic()) {
                comments.leading(initializer);
                out.line("init {");
                out.indent();
                emitBlockContents(initializer.getBody());
                out.dedent();
                out.line("}");
                out.blankLine();
            }
        }

        for (BodyDeclaration<?> member : members) {
            if (member instanceof ConstructorDeclaration constructor) {
                if (promotion != null && constructor == promotion.constructor()) {
                    continue;
                }
                emitConstructor(constructor, isInterface);
                out.blankLine();
            }
        }

        for (BodyDeclaration<?> member : members) {
            if (member instanceof MethodDeclaration method && !method.isStatic()) {
                if (promotion != null && isPromotedAccessor(method, promotion.fields())) {
                    continue;
                }
                emitMethod(method, false, isInterface);
                out.blankLine();
            }
        }

        for (BodyDeclaration<?> member : members) {
            if (member instanceof EnumDeclaration nestedEnum) {
                emitEnum(nestedEnum);
                out.blankLine();
            } else if (member instanceof RecordDeclaration nestedRecord) {
                emitRecord(nestedRecord);
                out.blankLine();
            } else if (member instanceof TypeDeclaration<?> nested) {
                emitNestedType(nested);
                out.blankLine();
            }
        }

        if (companionUsed) {
            emitCompanion(declaration);
        }
    }

    private void registerMembers(List<BodyDeclaration<?>> members) {
        for (BodyDeclaration<?> member : members) {
            if (member instanceof FieldDeclaration field) {
                for (VariableDeclarator variable : field.getVariables()) {
                    scope.declareField(variable.getNameAsString(), fieldType(field, variable));
                }
            } else if (member instanceof MethodDeclaration method) {
                KType returnType = method.getType().isVoidType()
                        ? KType.UNIT
                        : types.map(method.getType());
                scope.declareMethod(method.getNameAsString(), returnType);
            }
        }
    }

    private void emitNestedType(TypeDeclaration<?> type) {
        if (type instanceof ClassOrInterfaceDeclaration declaration) {
            emitClassOrInterface(declaration);
        } else if (type instanceof AnnotationDeclaration declaration) {
            emitAnnotation(declaration);
        }
    }

    private boolean belongsToCompanion(BodyDeclaration<?> member) {
        return (member instanceof FieldDeclaration field && field.isStatic())
                || (member instanceof MethodDeclaration method && method.isStatic())
                || (member instanceof InitializerDeclaration initializer && initializer.isStatic());
    }

    private void emitCompanion(ClassOrInterfaceDeclaration declaration) {
        out.line("companion object {");
        out.indent();
        for (BodyDeclaration<?> member : declaration.getMembers()) {
            if (member instanceof FieldDeclaration field && field.isStatic()) {
                emitField(field, true, declaration.isInterface());
                out.blankLine();
            } else if (member instanceof InitializerDeclaration initializer && initializer.isStatic()) {
                out.line("init {");
                out.indent();
                emitBlockContents(initializer.getBody());
                out.dedent();
                out.line("}");
                out.blankLine();
            }
        }
        for (BodyDeclaration<?> member : declaration.getMembers()) {
            if (member instanceof MethodDeclaration method && method.isStatic()) {
                emitMethod(method, true, declaration.isInterface());
                out.blankLine();
            }
        }
        out.dedent();
        out.line("}");
    }

    private void emitField(FieldDeclaration field, boolean inCompanion, boolean inInterface) {
        comments.javadoc(field);
        emitAnnotations(field.getAnnotations());
        if (field.isVolatile()) {
            if (target == Target.PUMPKIN) {
                diagnostics.warn("J2KT-PUMPKIN-001", "volatile has no meaning in single-threaded Kotlin/Wasm.", field);
            } else {
                out.line("@Volatile");
            }
        }
        if (field.isTransient() && target == Target.JVM) {
            out.line("@Transient");
        }

        for (VariableDeclarator variable : field.getVariables()) {
            KType type = fieldType(field, variable);
            boolean isConst = inCompanion && field.isStatic() && field.isFinal() && isCompileTimeConstant(variable, type);
            boolean hasInitializer = variable.getInitializer().isPresent();
            boolean isVal = isConst || (field.isFinal() && hasInitializer);
            if (field.isFinal() && !hasInitializer && !inInterface) {
                diagnostics.warn("J2KT-FIELD-001",
                        "Java final field '" + variable.getNameAsString() + "' is assigned in a constructor; Kotlin cannot assign a val in a secondary constructor, so it was emitted as var.",
                        variable);
            }
            String keyword = isConst ? "const val" : (isVal || inInterface ? "val" : "var");
            String visibility = inInterface ? "" : visibility(field);
            String declaration = (visibility.isEmpty() ? "" : visibility + " ")
                    + keyword + " " + name(variable.getNameAsString()) + ": " + type.render();

            if (hasInitializer) {
                String initializer = emitExpression(variable.getInitializer().get(), type);
                out.line(declaration + " = " + initializer);
            } else if (isConst || isVal) {
                if (inInterface) {
                    diagnostics.warn("J2KT-FIELD-002", "Interface field without initializer emitted with a default value.", variable);
                }
                out.line(declaration + " = " + types.defaultValue(type));
            } else if (canLateinit(type)) {
                String prefix = visibility.isEmpty() ? "" : visibility + " ";
                out.line(prefix + "lateinit " + keyword + " " + name(variable.getNameAsString()) + ": " + type.render());
            } else {
                out.line(declaration + " = " + types.defaultValue(type));
            }
        }
    }

    private boolean canLateinit(KType type) {
        return !type.isNullable() && !type.isKotlinPrimitive() && !type.isUnit()
                && !type.isArray() && !type.isCollection() && !type.isMap();
    }

    private boolean isCompileTimeConstant(VariableDeclarator variable, KType type) {
        if (variable.getInitializer().isEmpty()) {
            return false;
        }
        if (!(type.isKotlinPrimitive() || type.isString())) {
            return false;
        }
        Expression expression = variable.getInitializer().get();
        return expression instanceof LiteralExpr
                || expression instanceof UnaryExpr unary && unary.getExpression() instanceof LiteralExpr
                || expression instanceof BinaryExpr binary
                && binary.getLeft() instanceof LiteralExpr && binary.getRight() instanceof LiteralExpr;
    }

    private KType fieldType(FieldDeclaration field, VariableDeclarator variable) {
        if (variable.getType().isVarType() && variable.getInitializer().isPresent()) {
            return oracle.infer(variable.getInitializer().get());
        }
        KType type = types.map(variable.getType());
        if (variable.getInitializer().isPresent()
                && variable.getInitializer().get() instanceof NullLiteralExpr) {
            type = type.asNullable();
        }
        return type;
    }

    private void emitConstructor(ConstructorDeclaration constructor, boolean inInterface) {
        if (inInterface) {
            return;
        }
        comments.javadoc(constructor);
        emitAnnotations(constructor.getAnnotations());
        String visibility = visibility(constructor);
        String params = parameters(constructor.getParameters(), constructor.getBody());

        String delegation = "this()";
        Optional<ExplicitConstructorInvocationStmt> invocation = constructorInvocation(constructor);
        if (invocation.isPresent() && invocation.get().isThis()) {
            delegation = "this(" + arguments(invocation.get().getArguments(), null) + ")";
        }

        out.line((visibility.isEmpty() ? "" : visibility + " ") + "constructor(" + params + ") : " + delegation + " {");
        out.indent();
        scope.push();
        declareParameters(constructor.getParameters());
        Node previousBody = currentBody;
        currentBody = constructor.getBody();
        emitParameterCopies(constructor.getParameters(), constructor.getBody());
        emitBlockContents(constructor.getBody(), invocation.orElse(null));
        currentBody = previousBody;
        scope.pop();
        out.dedent();
        out.line("}");
    }

    private String superArguments(ClassOrInterfaceDeclaration declaration, boolean promotedConstructor) {
        for (BodyDeclaration<?> member : declaration.getMembers()) {
            if (!(member instanceof ConstructorDeclaration constructor)) {
                continue;
            }
            Optional<ExplicitConstructorInvocationStmt> invocation = constructorInvocation(constructor);
            if (invocation.isPresent() && !invocation.get().isThis()) {
                String args = arguments(invocation.get().getArguments(), null);
                if (!args.isBlank() && !promotedConstructor) {
                    diagnostics.warn("J2KT-CTOR-001",
                            "Superclass constructor arguments were lifted into the class header, but no primary constructor could be synthesized. Review the hierarchy.",
                            invocation.get());
                }
                return args;
            }
        }
        return "";
    }

    private String arguments(NodeList<Expression> arguments, KType expected) {
        List<String> result = new ArrayList<>();
        for (Expression argument : arguments) {
            result.add(emitExpression(argument, expected));
        }
        return String.join(", ", result);
    }

    private Optional<ExplicitConstructorInvocationStmt> constructorInvocation(ConstructorDeclaration constructor) {
        if (constructor.getBody().getStatements().isEmpty()) {
            return Optional.empty();
        }
        Statement first = constructor.getBody().getStatement(0);
        if (first instanceof ExplicitConstructorInvocationStmt invocation) {
            return Optional.of(invocation);
        }
        return Optional.empty();
    }

    private void emitMethod(MethodDeclaration method, boolean inCompanion, boolean inInterface) {
        comments.javadoc(method);
        emitAnnotations(method.getAnnotations());
        boolean lifecycle = isPumpkinLifecycle(method);
        boolean metadata = pumpkinPluginSubclass && method.getNameAsString().equals("metadata")
                && method.getParameters().isEmpty();
        boolean override = hasAnnotation(method, "Override") || lifecycle || metadata;
        String visibility = visibility(method);
        String modifiers = methodModifiers(method, inInterface, override);
        String typeParams = typeParameters(method.getTypeParameters());
        String typePrefix = typeParams.isEmpty() ? "" : typeParams + " ";
        String name = name(method.getNameAsString());
        String params = parameters(method.getParameters(), method.getBody().orElse(null));
        String returnType;
        if (lifecycle) {
            returnType = ": Result<Unit>";
        } else {
            returnType = method.getType().isVoidType() ? "" : ": " + types.map(method.getType()).render();
        }
        String throwsAnnotation = throwsAnnotation(method);
        if (!throwsAnnotation.isEmpty()) {
            out.line(throwsAnnotation);
        }
        if (method.isSynchronized()) {
            if (target == Target.PUMPKIN) {
                diagnostics.warn("J2KT-PUMPKIN-002",
                        "synchronized has no direct meaning in single-threaded Kotlin/Wasm; use a Mutex if the code becomes suspend.",
                        method);
            } else {
                out.line("@Synchronized");
            }
        }
        if (inCompanion && target == Target.JVM) {
            out.line("@JvmStatic");
        }

        if (method.getBody().isEmpty()) {
            out.line((visibility.isEmpty() ? "" : visibility + " ") + modifiers + "fun " + typePrefix + name + "(" + params + ")" + returnType);
            return;
        }

        if (lifecycle) {
            out.line((visibility.isEmpty() ? "" : visibility + " ") + modifiers + "fun " + typePrefix + name
                    + "(" + params + ")" + returnType + " = runCatching {");
            out.indent();
            scope.push();
            declareParameters(method.getParameters());
            Node previousBody = currentBody;
            currentBody = method.getBody().orElseThrow();
            boolean previousResultBlock = inResultBlock;
            inResultBlock = true;
            emitParameterCopies(method.getParameters(), method.getBody().orElse(null));
            emitBlockContents(method.getBody().orElseThrow());
            inResultBlock = previousResultBlock;
            currentBody = previousBody;
            scope.pop();
            out.line("return@runCatching Unit");
            out.dedent();
            out.line("}");
            return;
        }

        out.line((visibility.isEmpty() ? "" : visibility + " ") + modifiers + "fun " + typePrefix + name + "(" + params + ")" + returnType + " {");
        out.indent();
        scope.push();
        declareParameters(method.getParameters());
        Node previousBody = currentBody;
        KType previousReturn = currentMethodReturn;
        currentBody = method.getBody().orElseThrow();
        currentMethodReturn = method.getType().isVoidType() ? KType.UNIT : types.map(method.getType());
        emitParameterCopies(method.getParameters(), method.getBody().orElse(null));
        emitBlockContents(method.getBody().orElseThrow());
        currentMethodReturn = previousReturn;
        currentBody = previousBody;
        scope.pop();
        out.dedent();
        out.line("}");
    }

    private boolean isPumpkinLifecycle(MethodDeclaration method) {
        if (target != Target.PUMPKIN || !pumpkinPluginSubclass) {
            return false;
        }
        String name = method.getNameAsString();
        if (!name.equals("onLoad") && !name.equals("onUnload")) {
            return false;
        }
        return method.getParameters().size() == 1
                && method.getParameter(0).getType().asString().endsWith("PluginContext");
    }

    private String methodModifiers(MethodDeclaration method, boolean inInterface, boolean override) {
        StringBuilder result = new StringBuilder();
        if (method.isNative()) {
            result.append("external ");
            if (target == Target.PUMPKIN) {
                diagnostics.warn("J2KT-PUMPKIN-022",
                        "native methods require a Wasm import and cannot be translated mechanically.", method);
            }
        }
        if (method.isAbstract()) {
            result.append("abstract ");
        } else if (!inInterface && !override && !method.isStatic() && !method.isPrivate() && !method.isFinal()
                && !currentClassFinal && !currentClassInterface) {
            result.append("open ");
        }
        if (override) {
            result.append("override ");
        }
        if (method.isStatic()) {
            // Static members live in a companion object; no Kotlin modifier is needed.
        }
        return result.toString();
    }

    private String throwsAnnotation(MethodDeclaration method) {
        if (method.getThrownExceptions().isEmpty() || target == Target.PUMPKIN) {
            return "";
        }
        List<String> entries = new ArrayList<>();
        for (var thrown : method.getThrownExceptions()) {
            entries.add(types.map(thrown).render() + "::class");
        }
        return "@Throws(" + String.join(", ", entries) + ")";
    }

    private String parameters(NodeList<Parameter> parameters, Node body) {
        List<String> result = new ArrayList<>();
        for (Parameter parameter : parameters) {
            emitParameterAnnotations(parameter.getAnnotations());
            KType type = types.map(parameter.getType());
            if (parameter.getType() instanceof ArrayType arrayType && parameter.isVarArgs()) {
                type = types.map(arrayType.getComponentType());
            }
            String prefix = parameter.isVarArgs() ? "vararg " : "";
            result.add(prefix + name(parameter.getNameAsString()) + ": " + type.render());
        }
        return String.join(", ", result);
    }

    private void declareParameters(NodeList<Parameter> parameters) {
        for (Parameter parameter : parameters) {
            KType type;
            if (parameter.isVarArgs() && parameter.getType() instanceof ArrayType arrayType) {
                type = types.map(arrayType.getComponentType());
            } else {
                type = types.map(parameter.getType());
            }
            scope.declare(parameter.getNameAsString(), type);
        }
    }

    private void emitParameterAnnotations(NodeList<AnnotationExpr> annotations) {
        // Annotations are emitted inline for fields/classes; parameters need their own writer path.
        // Kept as a no-op for now to avoid putting a line break inside a parameter list.
        if (!annotations.isEmpty()) {
            // String-level annotation support is intentionally limited in v1.
        }
    }

    private void emitParameterCopies(NodeList<Parameter> parameters, Node body) {
        if (body == null) {
            return;
        }
        for (Parameter parameter : parameters) {
            if (isAssigned(parameter.getNameAsString(), body)) {
                String name = name(parameter.getNameAsString());
                out.line("var " + name + " = " + name);
            }
        }
    }

    private boolean isAssigned(String name, Node body) {
        for (AssignExpr assign : body.findAll(AssignExpr.class)) {
            if (assign.getTarget() instanceof NameExpr target && target.getNameAsString().equals(name)) {
                return true;
            }
        }
        for (UnaryExpr unary : body.findAll(UnaryExpr.class)) {
            if ((unary.getOperator() == UnaryExpr.Operator.PREFIX_INCREMENT
                    || unary.getOperator() == UnaryExpr.Operator.PREFIX_DECREMENT
                    || unary.getOperator() == UnaryExpr.Operator.POSTFIX_INCREMENT
                    || unary.getOperator() == UnaryExpr.Operator.POSTFIX_DECREMENT)
                    && unary.getExpression() instanceof NameExpr target
                    && target.getNameAsString().equals(name)) {
                return true;
            }
        }
        return false;
    }

    private String typeParameters(NodeList<TypeParameter> parameters) {
        if (parameters.isEmpty()) {
            return "";
        }
        List<String> result = new ArrayList<>();
        for (TypeParameter parameter : parameters) {
            String bounds = typeBounds(parameter);
            result.add(name(parameter.getNameAsString()) + bounds);
        }
        return "<" + String.join(", ", result) + ">";
    }

    private String typeBounds(TypeParameter parameter) {
        if (parameter.getTypeBound().isEmpty()) {
            return "";
        }
        return " : " + types.map(parameter.getTypeBound().get(0)).render();
    }

    private void emitAnnotations(NodeList<AnnotationExpr> annotations) {
        for (AnnotationExpr annotation : annotations) {
            String name = annotation.getNameAsString();
            if (name.endsWith("Override")) {
                continue;
            }
            if (target == Target.PUMPKIN && name.endsWith("EventHandler")) {
                continue;
            }
            if (name.endsWith("Nullable") || name.endsWith("NotNull") || name.endsWith("NonNull")
                    || name.endsWith("FunctionalInterface")) {
                continue;
            }
            if (name.endsWith("Deprecated") && annotation.isMarkerAnnotationExpr()) {
                out.line("@Deprecated(\"Deprecated in Java\")");
                continue;
            }
            if (name.endsWith("SuppressWarnings") && annotation instanceof SingleMemberAnnotationExpr single) {
                out.line("@Suppress(" + suppressNames(single.getMemberValue()) + ")");
                continue;
            }
            out.line(annotation.toString());
        }
    }

    private String suppressNames(Expression expression) {
        if (expression instanceof ArrayInitializerExpr initializer) {
            List<String> values = new ArrayList<>();
            for (Expression value : initializer.getValues()) {
                if (value instanceof StringLiteralExpr string) {
                    values.add("\"" + string.asString() + "\"");
                }
            }
            return String.join(", ", values);
        }
        if (expression instanceof StringLiteralExpr string) {
            return "\"" + string.asString() + "\"";
        }
        return expression.toString();
    }

    private void emitEnum(EnumDeclaration declaration) {
        boolean previousFinal = currentClassFinal;
        currentClassFinal = true;
        comments.javadoc(declaration);
        emitAnnotations(declaration.getAnnotations());
        String visibility = visibility(declaration);
        String name = name(declaration.getNameAsString());
        Map<String, String> promoted = enumConstructorPromotions(declaration);
        String primary = enumPrimaryConstructor(declaration, promoted);
        String supertypes = declaration.getImplementedTypes().isEmpty() ? ""
                : " : " + declaration.getImplementedTypes().stream()
                .map(type -> types.map(type).render()).reduce((a, b) -> a + ", " + b).orElse("");

        out.line((visibility.isEmpty() ? "" : visibility + " ") + "enum class " + name + primary + supertypes + " {");
        out.indent();

        List<EnumConstantDeclaration> entries = declaration.getEntries();
        for (int i = 0; i < entries.size(); i++) {
            EnumConstantDeclaration entry = entries.get(i);
            comments.leading(entry);
            StringBuilder line = new StringBuilder(name(entry.getNameAsString()));
            if (!entry.getArguments().isEmpty()) {
                line.append("(").append(arguments(entry.getArguments(), null)).append(")");
            }
            if (!entry.getClassBody().isEmpty()) {
                line.append(" {");
                out.line(line.toString());
                out.indent();
                for (BodyDeclaration<?> member : entry.getClassBody()) {
                    if (member instanceof MethodDeclaration method) {
                        emitMethod(method, false, false);
                    } else if (member instanceof FieldDeclaration field) {
                        emitField(field, false, false);
                    }
                }
                out.dedent();
                out.line("}");
            } else {
                line.append(i == entries.size() - 1 ? "" : ",");
                out.line(line.toString());
            }
        }
        if (!entries.isEmpty()) {
            out.line(";");
        }

        // Fields and methods. Constructor assignments are emitted in an init block.
        for (BodyDeclaration<?> member : declaration.getMembers()) {
            if (member instanceof FieldDeclaration field) {
                if (field.getVariables().size() == 1
                        && promoted.containsKey(field.getVariable(0).getNameAsString())) {
                    continue;
                }
                emitField(field, false, false);
                out.blankLine();
            }
        }
        for (BodyDeclaration<?> member : declaration.getMembers()) {
            if (member instanceof ConstructorDeclaration constructor) {
                emitEnumConstructorBody(constructor, promoted.keySet());
            } else if (member instanceof MethodDeclaration method) {
                if (isPromotedAccessor(method, promoted.keySet())) {
                    continue;
                }
                emitMethod(method, false, false);
                out.blankLine();
            }
        }
        out.dedent();
        out.line("}");
        currentClassFinal = previousFinal;
    }

    private Map<String, String> enumConstructorPromotions(EnumDeclaration declaration) {
        Map<String, String> promoted = new HashMap<>();
        for (BodyDeclaration<?> member : declaration.getMembers()) {
            if (!(member instanceof ConstructorDeclaration constructor)) {
                continue;
            }
            for (Statement statement : constructor.getBody().getStatements()) {
                if (statement instanceof ExpressionStmt expressionStmt
                        && expressionStmt.getExpression() instanceof AssignExpr assignment
                        && assignment.getOperator() == AssignExpr.Operator.ASSIGN
                        && assignment.getTarget() instanceof FieldAccessExpr fieldAccess
                        && fieldAccess.getScope().isThisExpr()
                        && assignment.getValue() instanceof NameExpr value
                        && value.getNameAsString().equals(fieldAccess.getNameAsString())) {
                    promoted.put(fieldAccess.getNameAsString(), value.getNameAsString());
                }
            }
        }
        return promoted;
    }

    private String enumPrimaryConstructor(EnumDeclaration declaration, Map<String, String> promoted) {
        for (BodyDeclaration<?> member : declaration.getMembers()) {
            if (member instanceof ConstructorDeclaration constructor) {
                List<String> params = new ArrayList<>();
                for (Parameter parameter : constructor.getParameters()) {
                    String parameterName = parameter.getNameAsString();
                    if (promoted.containsValue(parameterName)) {
                        String fieldName = promoted.entrySet().stream()
                                .filter(entry -> entry.getValue().equals(parameterName))
                                .map(Map.Entry::getKey)
                                .findFirst().orElse(parameterName);
                        FieldDeclaration field = declaration.getFieldByName(fieldName).orElse(null);
                        String keyword = field != null && field.isFinal() ? "val" : "var";
                        params.add(keyword + " " + name(fieldName) + ": "
                                + types.map(parameter.getType()).render());
                    } else {
                        params.add(name(parameterName) + ": "
                                + types.map(parameter.getType()).render());
                    }
                }
                return params.isEmpty() ? "" : "(" + String.join(", ", params) + ")";
            }
        }
        return "";
    }

    private void emitEnumConstructorBody(ConstructorDeclaration constructor, Set<String> promotedFields) {
        List<Statement> remaining = new ArrayList<>();
        for (Statement statement : constructor.getBody().getStatements()) {
            boolean promotedAssignment = statement instanceof ExpressionStmt expressionStmt
                    && expressionStmt.getExpression() instanceof AssignExpr assignment
                    && assignment.getTarget() instanceof FieldAccessExpr fieldAccess
                    && fieldAccess.getScope().isThisExpr()
                    && promotedFields.contains(fieldAccess.getNameAsString());
            if (!promotedAssignment) {
                remaining.add(statement);
            }
        }
        if (remaining.isEmpty()) {
            return;
        }
        out.line("init {");
        out.indent();
        for (Statement statement : remaining) {
            emitStatement(statement);
        }
        out.dedent();
        out.line("}");
    }

    private void emitRecord(RecordDeclaration declaration) {
        boolean previousFinal = currentClassFinal;
        currentClassFinal = true;
        comments.javadoc(declaration);
        emitAnnotations(declaration.getAnnotations());
        String visibility = visibility(declaration);
        String name = name(declaration.getNameAsString());
        Set<String> reassigned = new LinkedHashSet<>();
        for (CompactConstructorDeclaration compact : declaration.getCompactConstructors()) {
            for (AssignExpr assignment : compact.getBody().findAll(AssignExpr.class)) {
                if (assignment.getTarget() instanceof NameExpr target) {
                    reassigned.add(target.getNameAsString());
                }
            }
        }
        List<String> params = new ArrayList<>();
        for (Parameter parameter : declaration.getParameters()) {
            String keyword = reassigned.contains(parameter.getNameAsString()) ? "var" : "val";
            params.add(keyword + " " + name(parameter.getNameAsString()) + ": "
                    + types.map(parameter.getType()).render());
        }
        String supertypes = declaration.getImplementedTypes().isEmpty() ? ""
                : " : " + declaration.getImplementedTypes().stream()
                .map(type -> types.map(type).render()).reduce((a, b) -> a + ", " + b).orElse("");
        out.line((visibility.isEmpty() ? "" : visibility + " ") + "data class " + name
                + "(" + String.join(", ", params) + ")" + supertypes + " {");
        out.indent();
        scope.push();
        for (Parameter parameter : declaration.getParameters()) {
            scope.declare(parameter.getNameAsString(), types.map(parameter.getType()));
        }
        for (CompactConstructorDeclaration compact : declaration.getCompactConstructors()) {
            out.line("init {");
            out.indent();
            emitBlockContents(compact.getBody());
            out.dedent();
            out.line("}");
            out.blankLine();
        }
        for (BodyDeclaration<?> member : declaration.getMembers()) {
            if (member instanceof MethodDeclaration method) {
                emitMethod(method, false, false);
                out.blankLine();
            } else if (member instanceof FieldDeclaration field && field.isStatic()) {
                // emitted in companion below
            } else if (member instanceof FieldDeclaration field) {
                emitField(field, false, false);
                out.blankLine();
            }
        }
        if (declaration.getMembers().stream().anyMatch(this::belongsToCompanion)) {
            emitRecordCompanion(declaration);
        }
        scope.pop();
        out.dedent();
        out.line("}");
        currentClassFinal = previousFinal;
    }

    private void emitRecordCompanion(RecordDeclaration declaration) {
        out.line("companion object {");
        out.indent();
        for (BodyDeclaration<?> member : declaration.getMembers()) {
            if (member instanceof FieldDeclaration field && field.isStatic()) {
                emitField(field, true, false);
                out.blankLine();
            }
        }
        for (BodyDeclaration<?> member : declaration.getMembers()) {
            if (member instanceof MethodDeclaration method && method.isStatic()) {
                emitMethod(method, true, false);
                out.blankLine();
            }
        }
        out.dedent();
        out.line("}");
    }

    private void emitAnnotation(AnnotationDeclaration declaration) {
        comments.javadoc(declaration);
        emitAnnotations(declaration.getAnnotations());
        String visibility = visibility(declaration);
        String name = name(declaration.getNameAsString());
        List<String> members = new ArrayList<>();
        for (BodyDeclaration<?> member : declaration.getMembers()) {
            if (member instanceof AnnotationMemberDeclaration annotationMember) {
                String defaultValue = annotationMember.getDefaultValue()
                        .map(value -> " = " + emitExpression(value, types.map(annotationMember.getType())))
                        .orElse("");
                members.add("val " + name(annotationMember.getNameAsString()) + ": "
                        + types.map(annotationMember.getType()).render() + defaultValue);
            }
        }
        out.line((visibility.isEmpty() ? "" : visibility + " ") + "annotation class " + name
                + "(" + String.join(", ", members) + ")");
    }

    // ---------------------------------------------------------------------
    // Statements
    // ---------------------------------------------------------------------

    private void emitBlockContents(BlockStmt block) {
        emitBlockContents(block, null);
    }

    private void emitBlockContents(BlockStmt block, ExplicitConstructorInvocationStmt skip) {
        scope.push();
        for (Statement statement : block.getStatements()) {
            if (skip != null && statement == skip) {
                continue;
            }
            emitStatement(statement);
        }
        scope.pop();
    }

    private void emitStatement(Statement statement) {
        comments.leading(statement);
        if (statement instanceof BlockStmt block) {
            out.line("{");
            out.indent();
            emitBlockContents(block);
            out.dedent();
            out.line("}");
        } else if (statement instanceof ExpressionStmt expressionStmt) {
            if (expressionStmt.getExpression() instanceof VariableDeclarationExpr declaration) {
                emitLocalDeclaration(declaration);
            } else {
                out.line(emitExpression(expressionStmt.getExpression(), null));
            }
        } else if (statement instanceof IfStmt ifStmt) {
            emitIf(ifStmt);
        } else if (statement instanceof ForStmt forStmt) {
            emitFor(forStmt);
        } else if (statement instanceof ForEachStmt forEachStmt) {
            emitForEach(forEachStmt);
        } else if (statement instanceof WhileStmt whileStmt) {
            String label = takePendingLabel();
            out.line((label == null ? "" : label + "@ ") + "while ("
                    + emitExpression(whileStmt.getCondition(), KType.BOOLEAN) + ") {");
            out.indent();
            emitStatementBody(whileStmt.getBody());
            out.dedent();
            out.line("}");
        } else if (statement instanceof DoStmt doStmt) {
            String label = takePendingLabel();
            out.line((label == null ? "" : label + "@ ") + "do {");
            out.indent();
            emitStatementBody(doStmt.getBody());
            out.dedent();
            out.line("} while (" + emitExpression(doStmt.getCondition(), KType.BOOLEAN) + ")");
        } else if (statement instanceof SwitchStmt switchStmt) {
            emitSwitch(switchStmt);
        } else if (statement instanceof TryStmt tryStmt) {
            emitTry(tryStmt);
        } else if (statement instanceof ReturnStmt returnStmt) {
            String label = currentLambdaLabel != null ? currentLambdaLabel : (inResultBlock ? "runCatching" : null);
            if (returnStmt.getExpression().isPresent()) {
                KType expected = currentMethodReturn;
                String value = emitExpression(returnStmt.getExpression().get(), expected);
                value = coerce(value, oracle.infer(returnStmt.getExpression().get()), expected);
                if (label != null && currentLambdaReturnWrapper != null) {
                    value = currentLambdaReturnWrapper.apply(value);
                }
                out.line(label == null ? "return " + value : "return@" + label + " " + value);
            } else {
                String value = currentLambdaReturnWrapper == null ? "Unit"
                        : currentLambdaReturnWrapper.apply("0");
                out.line(label == null ? "return " + value : "return@" + label + " " + value);
            }
        } else if (statement instanceof ThrowStmt throwStmt) {
            out.line("throw " + emitExpression(throwStmt.getExpression(), null));
        } else if (statement instanceof BreakStmt breakStmt) {
            if (switchFallthroughMode && breakStmt.getLabel().isEmpty()) {
                out.line("return@switch");
            } else {
                out.line(breakStmt.getLabel().isPresent()
                        ? "break@" + breakStmt.getLabel().get().asString()
                        : "break");
            }
        } else if (statement instanceof ContinueStmt continueStmt) {
            out.line(continueStmt.getLabel().isPresent()
                    ? "continue@" + continueStmt.getLabel().get().asString()
                    : "continue");
        } else if (statement instanceof LabeledStmt labeled) {
            String previous = pendingLabel;
            pendingLabel = labeled.getLabel().asString();
            emitStatement(labeled.getStatement());
            if (pendingLabel != null) {
                diagnostics.warn("J2KT-LABEL-001",
                        "Labels on non-loop statements are not lowered; review this block.", labeled);
                pendingLabel = null;
            }
            pendingLabel = previous;
        } else if (statement instanceof SynchronizedStmt synchronizedStmt) {
            if (target == Target.PUMPKIN) {
                out.line("// TODO(j2kt/pumpkin): synchronized is not available in single-threaded wasm.");
                diagnostics.warn("J2KT-PUMPKIN-003", "synchronized block requires manual porting (Mutex/coroutines).", synchronizedStmt);
            }
            out.line("synchronized(" + emitExpression(synchronizedStmt.getExpression(), null) + ") {");
            out.indent();
            emitBlockContents(synchronizedStmt.getBody());
            out.dedent();
            out.line("}");
        } else if (statement instanceof AssertStmt assertStmt) {
            String condition = emitExpression(assertStmt.getCheck(), KType.BOOLEAN);
            String function = target == Target.JVM ? "assert" : "check";
            if (assertStmt.getMessage().isPresent()) {
                out.line(function + "(" + condition + ") { "
                        + emitExpression(assertStmt.getMessage().get(), null) + " }");
            } else {
                out.line(function + "(" + condition + ")");
            }
        } else if (statement instanceof EmptyStmt) {
            // no-op
        } else if (statement instanceof LocalClassDeclarationStmt localClass) {
            emitNestedType(localClass.getClassDeclaration());
        } else if (statement instanceof ExplicitConstructorInvocationStmt invocation) {
            // Handled at constructor level; a bare invocation here is invalid Java.
            diagnostics.warn("J2KT-STMT-001", "Constructor invocation should have been handled in the constructor header.", invocation);
        } else if (statement instanceof YieldStmt yield) {
            out.line(emitExpression(yield.getExpression(), null));
        } else {
            out.line("// TODO(j2kt): unsupported statement " + statement.getClass().getSimpleName());
            diagnostics.warn("J2KT-STMT-002", "Unsupported statement: " + statement.getClass().getSimpleName(), statement);
        }
    }

    private void emitLocalDeclaration(VariableDeclarationExpr declaration) {
        for (VariableDeclarator variable : declaration.getVariables()) {
            KType type;
            if (variable.getType().isVarType() && variable.getInitializer().isPresent()) {
                type = oracle.infer(variable.getInitializer().get());
            } else {
                type = types.map(variable.getType());
            }
            if (variable.getInitializer().isPresent()
                    && variable.getInitializer().get() instanceof NullLiteralExpr) {
                type = type.asNullable();
            }
            if (variable.getInitializer().isPresent()) {
                KType initializerType = oracle.infer(variable.getInitializer().get());
                if (initializerType.isNullable() && !initializerType.isUnknown() && !type.isNullable()) {
                    type = type.asNullable();
                }
                if (isPumpkinNullableCall(variable.getInitializer().get())) {
                    type = type.asNullable();
                }
                String initializer = emitExpression(variable.getInitializer().get(), type);
                initializer = coerce(initializer, initializerType, type);
                boolean reassigned = isAssigned(variable.getNameAsString(),
                        currentBody == null ? declaration : currentBody);
                String keyword = reassigned ? "var" : "val";
                out.line(keyword + " " + name(variable.getNameAsString()) + ": " + type.render()
                        + " = " + initializer);
            } else {
                out.line("var " + name(variable.getNameAsString()) + ": " + type.render());
            }
            scope.declare(variable.getNameAsString(), type);
        }
    }

    private boolean isPumpkinNullableCall(Expression expression) {
        if (!(expression instanceof MethodCallExpr call)) {
            return false;
        }
        String name = call.getNameAsString();
        KType receiver = call.getScope().map(oracle::infer).orElse(null);
        String receiverName = receiver == null ? "" : receiver.javaName();
        return name.equals("getBlockByName") || name.equals("getBlockById")
                || name.equals("getPack") || name.equals("getTarget")
                || (name.equals("getPlayer") && receiverName.startsWith("pumpkin.Server"))
                || (name.equals("getWorld") && receiverName.startsWith("pumpkin.Server"))
                || (name.equals("getItem") && (receiverName.startsWith("pumpkin.Inventory")
                || receiverName.startsWith("pumpkin.Gui")))
                || name.equals("asMob") || name.equals("fromEntity")
                || name.equals("asPlayer")
                || ((name.equals("get") || name.equals("remove"))
                && receiver != null && receiver.isMap());
    }

    private void emitStatementBody(Statement body) {
        if (body instanceof BlockStmt block) {
            emitBlockContents(block);
        } else {
            emitStatement(body);
        }
    }

    private Map<String, String> collectPatternAliases(Expression condition) {
        Map<String, String> result = new HashMap<>();
        for (InstanceOfExpr instanceOf : condition.findAll(InstanceOfExpr.class)) {
            if (instanceOf.getPattern().isPresent()
                    && instanceOf.getPattern().get() instanceof com.github.javaparser.ast.expr.TypePatternExpr pattern) {
                Expression value = instanceOf.getExpression();
                if (value instanceof NameExpr || value instanceof FieldAccessExpr || value instanceof ThisExpr) {
                    result.put(pattern.getNameAsString(), emitExpression(value, null));
                } else {
                    diagnostics.warn("J2KT-PATTERN-001",
                            "Pattern variable '" + pattern.getNameAsString() + "' is bound to a complex expression; Kotlin smart-cast may not preserve it.",
                            instanceOf);
                }
            }
        }
        return result;
    }

    private void emitIf(IfStmt ifStmt) {
        Map<String, String> patternAliases = collectPatternAliases(ifStmt.getCondition());
        aliases.push(patternAliases);
        scope.push();
        for (InstanceOfExpr instanceOf : ifStmt.getCondition().findAll(InstanceOfExpr.class)) {
            if (instanceOf.getPattern().isPresent()
                    && instanceOf.getPattern().get() instanceof com.github.javaparser.ast.expr.TypePatternExpr pattern) {
                scope.declare(pattern.getNameAsString(), types.map(pattern.getType()));
            }
        }
        String condition = emitExpression(ifStmt.getCondition(), KType.BOOLEAN);
        out.line("if (" + condition + ") {");
        out.indent();
        emitStatementBody(ifStmt.getThenStmt());
        out.dedent();
        scope.pop();
        aliases.pop();
        if (ifStmt.getElseStmt().isPresent()) {
            Statement elseStmt = ifStmt.getElseStmt().get();
            if (elseStmt instanceof IfStmt nested) {
                out.line("} else {");
                out.indent();
                emitIf(nested);
                out.dedent();
                out.line("}");
            } else {
                out.line("} else {");
                out.indent();
                emitStatementBody(elseStmt);
                out.dedent();
                out.line("}");
            }
        } else {
            out.line("}");
        }
    }

    private void emitFor(ForStmt forStmt) {
        String loopLabel = takePendingLabel();
        // Canonical numeric loop -> Kotlin range loop.
        if (forStmt.getInitialization().size() == 1
                && forStmt.getInitialization().get(0) instanceof VariableDeclarationExpr declaration
                && declaration.getVariables().size() == 1
                && forStmt.getCompare().isPresent()
                && forStmt.getUpdate().size() == 1) {
            VariableDeclarator variable = declaration.getVariable(0);
            String varName = variable.getNameAsString();
            Optional<Expression> compare = forStmt.getCompare();
            if (compare.isPresent() && compare.get() instanceof BinaryExpr binary
                    && binary.getLeft() instanceof NameExpr left
                    && left.getNameAsString().equals(varName)
                    && isSimpleIntegerUpdate(forStmt.getUpdate().get(0), varName)) {
                String start = variable.getInitializer().map(value -> emitExpression(value, KType.INT)).orElse("0");
                String end = emitExpression(binary.getRight(), KType.INT);
                int step = loopStep(forStmt.getUpdate().get(0), varName);
                if (step <= 0) {
                    // Not a simple positive step; fall through to the while-loop form.
                } else {
                    boolean inclusive = binary.getOperator() == BinaryExpr.Operator.LESS_EQUALS
                            || binary.getOperator() == BinaryExpr.Operator.GREATER_EQUALS;
                    boolean descending = binary.getOperator() == BinaryExpr.Operator.GREATER
                            || binary.getOperator() == BinaryExpr.Operator.GREATER_EQUALS;
                    String endValue = end;
                    String range;
                    if (!descending) {
                        range = (start.equals("0") ? "0" : start) + (inclusive ? ".." : " until ") + endValue;
                    } else {
                        if (binary.getOperator() == BinaryExpr.Operator.GREATER
                                && !(binary.getRight() instanceof IntegerLiteralExpr)) {
                            endValue = "(" + endValue + " + 1)";
                        } else if (binary.getOperator() == BinaryExpr.Operator.GREATER) {
                            try {
                                endValue = Integer.toString(Integer.parseInt(endValue) + 1);
                            } catch (NumberFormatException ignored) {
                                endValue = "(" + endValue + " + 1)";
                            }
                        }
                        range = start + " downTo " + endValue;
                    }
                    if (step != 1) {
                        range += " step " + step;
                    }
                    out.line((loopLabel == null ? "" : loopLabel + "@ ")
                            + "for (" + name(varName) + " in " + range + ") {");
                    out.indent();
                    scope.push();
                    scope.declare(varName, KType.INT);
                    emitStatementBody(forStmt.getBody());
                    scope.pop();
                    out.dedent();
                    out.line("}");
                    return;
                }
            }
        }

        diagnostics.warn("J2KT-FOR-001",
                "Non-canonical Java for-loop emitted as a while-loop. Verify continue/break behaviour.",
                forStmt);
        out.line("run {");
        out.indent();
        for (Expression initialization : forStmt.getInitialization()) {
            out.line(emitExpression(initialization, null));
        }
        String condition = forStmt.getCompare().map(value -> emitExpression(value, KType.BOOLEAN)).orElse("true");
        out.line((loopLabel == null ? "" : loopLabel + "@ ") + "while (" + condition + ") {");
        out.indent();
        emitStatementBody(forStmt.getBody());
        for (Expression update : forStmt.getUpdate()) {
            out.line(emitExpression(update, null));
        }
        out.dedent();
        out.line("}");
        out.dedent();
        out.line("}");
    }

    private int loopStep(Expression update, String varName) {
        if (update instanceof UnaryExpr unary) {
            if (unary.getExpression() instanceof NameExpr name && name.getNameAsString().equals(varName)
                    && (unary.getOperator() == UnaryExpr.Operator.POSTFIX_INCREMENT
                    || unary.getOperator() == UnaryExpr.Operator.PREFIX_INCREMENT
                    || unary.getOperator() == UnaryExpr.Operator.POSTFIX_DECREMENT
                    || unary.getOperator() == UnaryExpr.Operator.PREFIX_DECREMENT)) {
                return 1;
            }
            return 0;
        }
        if (update instanceof AssignExpr assign
                && assign.getTarget() instanceof NameExpr name
                && name.getNameAsString().equals(varName)
                && (assign.getOperator() == AssignExpr.Operator.PLUS
                || assign.getOperator() == AssignExpr.Operator.MINUS)) {
            if (assign.getValue() instanceof IntegerLiteralExpr literal) {
                try {
                    int value = Integer.parseInt(literal.getValue().replace("_", ""));
                    return value > 0 ? value : 0;
                } catch (NumberFormatException ignored) {
                    return 0;
                }
            }
        }
        return 0;
    }

    private boolean isSimpleIntegerUpdate(Expression update, String varName) {
        if (update instanceof UnaryExpr unary) {
            return unary.getExpression() instanceof NameExpr name && name.getNameAsString().equals(varName)
                    && (unary.getOperator() == UnaryExpr.Operator.POSTFIX_INCREMENT
                    || unary.getOperator() == UnaryExpr.Operator.PREFIX_INCREMENT
                    || unary.getOperator() == UnaryExpr.Operator.POSTFIX_DECREMENT
                    || unary.getOperator() == UnaryExpr.Operator.PREFIX_DECREMENT);
        }
        if (update instanceof AssignExpr assign) {
            return assign.getTarget() instanceof NameExpr name && name.getNameAsString().equals(varName)
                    && (assign.getOperator() == AssignExpr.Operator.PLUS
                    || assign.getOperator() == AssignExpr.Operator.MINUS);
        }
        return false;
    }

    private void emitForEach(ForEachStmt forEachStmt) {
        String loopLabel = takePendingLabel();
        String iterator = emitExpression(forEachStmt.getIterable(), null);
        String variable = name(forEachStmt.getVariable().getVariable(0).getNameAsString());
        out.line((loopLabel == null ? "" : loopLabel + "@ ")
                + "for (" + variable + " in " + iterator + ") {");
        out.indent();
        scope.push();
        KType iterableType = oracle.infer(forEachStmt.getIterable());
        KType elementType = iterableType.element() != null
                ? iterableType.element()
                : types.map(forEachStmt.getVariable().getVariable(0).getType());
        scope.declare(variable, elementType);
        emitStatementBody(forEachStmt.getBody());
        scope.pop();
        out.dedent();
        out.line("}");
    }

    private void emitSwitch(SwitchStmt switchStmt) {
        if (needsFallthrough(switchStmt)) {
            if (isSafeFallthrough(switchStmt)) {
                emitFallthroughSwitch(switchStmt);
                return;
            }
            diagnostics.warn("J2KT-SWITCH-002",
                    "Java switch fallthrough contains control flow that cannot be lowered safely; emitting TODO instead of wrong behaviour.",
                    switchStmt);
            out.line("TODO(\"j2kt: Java switch fallthrough requires manual review\")");
            return;
        }
        String selector = emitExpression(switchStmt.getSelector(), null);
        out.line("when (" + selector + ") {");
        out.indent();

        List<String> pendingLabels = new ArrayList<>();
        boolean fallthroughSeen = false;
        for (SwitchEntry entry : switchStmt.getEntries()) {
            List<String> labels = new ArrayList<>();
            String patternName = null;
            String patternType = null;
            for (Expression label : entry.getLabels()) {
                if (label instanceof com.github.javaparser.ast.expr.TypePatternExpr pattern) {
                    patternName = pattern.getNameAsString();
                    patternType = types.map(pattern.getType()).render();
                    labels.add("is " + patternType);
                } else {
                    labels.add(emitExpression(label, null));
                }
            }
            if (labels.isEmpty()) {
                pendingLabels.add("else");
            } else {
                pendingLabels.addAll(labels);
            }
            if (entry.getStatements().isEmpty()) {
                continue;
            }
            if (fallthroughSeen) {
                diagnostics.warn("J2KT-SWITCH-001",
                        "Java switch fallthrough has no direct Kotlin when equivalent; review this branch.",
                        entry);
                out.line("// TODO(j2kt): Java switch fallthrough");
            }
            out.line(String.join(", ", pendingLabels) + " -> {");
            out.indent();
            if (patternName != null && patternType != null) {
                out.line("val " + name(patternName) + " = " + selector);
            }
            scope.push();
            if (patternName != null && patternType != null) {
                scope.declare(patternName, KType.simple(patternType, patternType, KType.Category.OBJECT));
            }
            List<Statement> statements = entry.getStatements();
            for (Statement statement : statements) {
                if (statement instanceof BreakStmt) {
                    continue;
                }
                emitStatement(statement);
            }
            scope.pop();
            out.dedent();
            out.line("}");
            pendingLabels.clear();
            fallthroughSeen = !endsControlFlow(statements);
        }
        if (!pendingLabels.isEmpty()) {
            out.line(String.join(", ", pendingLabels) + " -> {");
            out.line("}");
        }
        out.dedent();
        out.line("}");
    }

    private boolean needsFallthrough(SwitchStmt switchStmt) {
        List<SwitchEntry> entries = switchStmt.getEntries();
        for (int i = 0; i < entries.size(); i++) {
            SwitchEntry entry = entries.get(i);
            if (entry.getStatements().isEmpty()) {
                continue;
            }
            boolean hasLaterBody = entries.subList(i + 1, entries.size()).stream()
                    .anyMatch(next -> !next.getStatements().isEmpty());
            if (hasLaterBody && !endsControlFlow(entry.getStatements())) {
                return true;
            }
        }
        return false;
    }

    private boolean isSafeFallthrough(SwitchStmt switchStmt) {
        List<SwitchEntry> entries = switchStmt.getEntries();
        for (int i = 0; i < entries.size(); i++) {
            SwitchEntry entry = entries.get(i);
            if (entry.getLabels().isEmpty() && i != entries.size() - 1) {
                // A default in the middle would need a more elaborate state machine.
                return false;
            }
            for (Statement statement : entry.getStatements()) {
                if (containsUnsafeFallthroughControl(statement)) {
                    return false;
                }
            }
        }
        return true;
    }

    private boolean containsUnsafeFallthroughControl(Node node) {
        if (node instanceof ReturnStmt || node instanceof ThrowStmt || node instanceof ContinueStmt) {
            return true;
        }
        if (node instanceof SwitchStmt) {
            return true;
        }
        if (node instanceof ForStmt || node instanceof ForEachStmt
                || node instanceof WhileStmt || node instanceof DoStmt) {
            if (!node.findAll(BreakStmt.class).isEmpty()) {
                return true;
            }
        }
        for (Node child : node.getChildNodes()) {
            if (child instanceof LambdaExpr) {
                continue;
            }
            if (containsUnsafeFallthroughControl(child)) {
                return true;
            }
        }
        return false;
    }

    private void emitFallthroughSwitch(SwitchStmt switchStmt) {
        String selector = emitExpression(switchStmt.getSelector(), null);
        out.line("run switch@ {");
        out.indent();
        out.line("val __switchValue = " + selector);
        out.line("var __fallthrough = false");
        out.line("var __matched = false");

        record Group(List<Expression> labels, List<Statement> statements, boolean isDefault) {
        }
        List<Group> groups = new ArrayList<>();
        List<Expression> pendingLabels = new ArrayList<>();
        boolean pendingDefault = false;
        for (SwitchEntry entry : switchStmt.getEntries()) {
            if (entry.getLabels().isEmpty()) {
                pendingDefault = true;
            } else {
                pendingLabels.addAll(entry.getLabels());
            }
            if (!entry.getStatements().isEmpty()) {
                groups.add(new Group(new ArrayList<>(pendingLabels), new ArrayList<>(entry.getStatements()), pendingDefault));
                pendingLabels.clear();
                pendingDefault = false;
            }
        }

        for (Group group : groups) {
            if (group.isDefault()) {
                out.line("if (__fallthrough || !__matched) {");
            } else {
                List<String> conditions = new ArrayList<>();
                conditions.add("__fallthrough");
                for (Expression label : group.labels()) {
                    conditions.add("__switchValue == " + emitExpression(label, null));
                }
                out.line("if (" + String.join(" || ", conditions) + ") {");
            }
            out.indent();
            if (!group.isDefault()) {
                out.line("__fallthrough = true");
                out.line("__matched = true");
            }
            boolean trailingBreak = !group.statements().isEmpty()
                    && group.statements().get(group.statements().size() - 1) instanceof BreakStmt;
            boolean previousMode = switchFallthroughMode;
            switchFallthroughMode = true;
            for (Statement statement : group.statements()) {
                if (trailingBreak && statement == group.statements().get(group.statements().size() - 1)) {
                    continue;
                }
                emitStatement(statement);
            }
            switchFallthroughMode = previousMode;
            if (trailingBreak) {
                out.line("return@switch");
            }
            out.dedent();
            out.line("}");
        }
        out.dedent();
        out.line("}");
    }

    private boolean endsControlFlow(List<Statement> statements) {
        if (statements.isEmpty()) {
            return false;
        }
        Statement last = statements.get(statements.size() - 1);
        return last instanceof BreakStmt || last instanceof ReturnStmt || last instanceof ThrowStmt
                || last instanceof ContinueStmt;
    }

    private void emitTry(TryStmt tryStmt) {
        if (!tryStmt.getResources().isEmpty()) {
            emitTryWithResources(tryStmt, 0);
            return;
        }
        emitTryCore(tryStmt);
    }

    private void emitTryWithResources(TryStmt tryStmt, int index) {
        if (index >= tryStmt.getResources().size()) {
            emitTryCore(tryStmt);
            return;
        }
        Expression resource = tryStmt.getResources().get(index);
        String name;
        String initializer;
        if (resource instanceof VariableDeclarationExpr declaration && !declaration.getVariables().isEmpty()) {
            VariableDeclarator variable = declaration.getVariable(0);
            name = variable.getNameAsString();
            initializer = variable.getInitializer().map(value -> emitExpression(value, null)).orElse("null");
            out.line("val " + name(name) + " = " + initializer);
        } else {
            name = "resource" + index;
            initializer = emitExpression(resource, null);
            out.line("val " + name + " = " + initializer);
        }
        out.line(name(name) + ".use { " + name(name) + " ->");
        out.indent();
        emitTryWithResources(tryStmt, index + 1);
        out.dedent();
        out.line("}");
    }

    private void emitTryCore(TryStmt tryStmt) {
        if (tryStmt.getCatchClauses().isEmpty() && tryStmt.getFinallyBlock().isEmpty()) {
            emitBlockContents(tryStmt.getTryBlock());
            return;
        }
        out.line("try {");
        out.indent();
        emitBlockContents(tryStmt.getTryBlock());
        out.dedent();
        for (CatchClause catchClause : tryStmt.getCatchClauses()) {
            List<String> catchTypes = new ArrayList<>();
            if (catchClause.getParameter().getType() instanceof com.github.javaparser.ast.type.UnionType union) {
                for (Type type : union.getElements()) {
                    catchTypes.add(types.map(type).render());
                }
            } else {
                catchTypes.add(types.map(catchClause.getParameter().getType()).render());
            }
            for (String catchType : catchTypes) {
                out.line("} catch (" + name(catchClause.getParameter().getNameAsString()) + ": " + catchType + ") {");
                out.indent();
                scope.push();
                scope.declare(catchClause.getParameter().getNameAsString(),
                        types.map(catchClause.getParameter().getType()));
                emitBlockContents(catchClause.getBody());
                scope.pop();
                out.dedent();
            }
        }
        if (tryStmt.getFinallyBlock().isPresent()) {
            out.line("} finally {");
            out.indent();
            emitBlockContents(tryStmt.getFinallyBlock().get());
            out.dedent();
        }
        out.line("}");
    }

    // ---------------------------------------------------------------------
    // Expressions
    // ---------------------------------------------------------------------

    private String emitExpression(Expression expression, KType expected) {
        if (expression == null) {
            return "null";
        }
        if (expression instanceof NameExpr aliasCheck) {
            for (Map<String, String> frame : aliases) {
                String alias = frame.get(aliasCheck.getNameAsString());
                if (alias != null) {
                    return alias;
                }
            }
        }
        if (expression instanceof EnclosedExpr enclosed) {
            return "(" + emitExpression(enclosed.getInner(), expected) + ")";
        }
        if (expression instanceof StringLiteralExpr string) {
            return "\"" + escapeString(string.getValue()) + "\"";
        }
        if (expression instanceof TextBlockLiteralExpr textBlock) {
            return "\"\"\"" + textBlock.getValue().replace("\"\"\"", "\\\"\\\"\\\"") + "\"\"\"";
        }
        if (expression instanceof CharLiteralExpr || expression instanceof BooleanLiteralExpr
                || expression instanceof NullLiteralExpr || expression instanceof LongLiteralExpr
                || expression instanceof DoubleLiteralExpr) {
            return expression.toString();
        }
        if (expression instanceof IntegerLiteralExpr integer) {
            return normalizeIntegerLiteral(integer.getValue());
        }
        if (expression instanceof NameExpr nameExpr) {
            String rawName = nameExpr.getNameAsString();
            if (target == Target.PUMPKIN && scope.lookup(rawName) == null) {
                String pumpkinName = pumpkinTypeName(rawName);
                if (pumpkinName != null) {
                    return pumpkinName;
                }
            }
            return name(rawName);
        }
        if (expression instanceof ThisExpr thisExpr) {
            return thisExpr.getTypeName().map(value -> "this@" + value.asString()).orElse("this");
        }
        if (expression instanceof SuperExpr superExpr) {
            return superExpr.getTypeName().map(value -> "super<" + value.asString() + ">").orElse("super");
        }
        if (expression instanceof FieldAccessExpr field) {
            return emitFieldAccess(field);
        }
        if (expression instanceof MethodCallExpr call) {
            return emitMethodCall(call, expected);
        }
        if (expression instanceof ObjectCreationExpr creation) {
            return emitObjectCreation(creation);
        }
        if (expression instanceof ArrayCreationExpr arrayCreation) {
            return emitArrayCreation(arrayCreation);
        }
        if (expression instanceof ArrayInitializerExpr initializer) {
            return emitArrayInitializer(initializer, expected);
        }
        if (expression instanceof ArrayAccessExpr access) {
            return emitExpression(access.getName(), null) + "[" + emitExpression(access.getIndex(), KType.INT) + "]";
        }
        if (expression instanceof AssignExpr assign) {
            return emitAssign(assign);
        }
        if (expression instanceof BinaryExpr binary) {
            return emitBinary(binary);
        }
        if (expression instanceof UnaryExpr unary) {
            return emitUnary(unary);
        }
        if (expression instanceof ConditionalExpr conditional) {
            String condition = emitExpression(conditional.getCondition(), KType.BOOLEAN);
            String thenValue = emitExpression(conditional.getThenExpr(), expected);
            String elseValue = emitExpression(conditional.getElseExpr(), expected);
            return "if (" + condition + ") " + thenValue + " else " + elseValue;
        }
        if (expression instanceof CastExpr cast) {
            KType type = types.map(cast.getType(), !cast.getType().isPrimitiveType());
            String value = emitExpression(cast.getExpression(), null);
            KType sourceType = oracle.infer(cast.getExpression());
            if (!cast.getType().isPrimitiveType()) {
                return value + " as " + type.render();
            }
            if (type.isNumeric() && sourceType.isNumeric()) {
                return coerce(value, sourceType, type);
            }
            return value + " as " + type.render();
        }
        if (expression instanceof InstanceOfExpr instanceOf) {
            String negated = "";
            String value = emitExpression(instanceOf.getExpression(), null) + " is " + types.map(instanceOf.getType()).render();
            return negated + value;
        }
        if (expression instanceof LambdaExpr lambda) {
            return emitLambda(lambda);
        }
        if (expression instanceof MethodReferenceExpr reference) {
            return emitMethodReference(reference);
        }
        if (expression instanceof ClassExpr classExpr) {
            return types.map(classExpr.getType()).render() + "::class";
        }
        if (expression instanceof com.github.javaparser.ast.expr.TypeExpr typeExpr) {
            return types.map(typeExpr.getType()).render() + "::class";
        }
        if (expression instanceof SwitchExpr switchExpr) {
            return emitSwitchExpression(switchExpr);
        }
        if (expression instanceof VariableDeclarationExpr declaration) {
            if (declaration.getVariables().size() == 1) {
                VariableDeclarator variable = declaration.getVariable(0);
                return "val " + name(variable.getNameAsString()) + " = "
                        + variable.getInitializer().map(value -> emitExpression(value, null)).orElse("null");
            }
        }
        if (expression instanceof com.github.javaparser.ast.expr.TypeExpr) {
            return expression.toString();
        }
        return expression.toString();
    }

    private KType serverType() {
        return KType.simple("Server", "pumpkin.Server.Server", KType.Category.OBJECT);
    }

    private KType eventType() {
        return KType.simple("Event", "pumpkin.Event.Event", KType.Category.OBJECT);
    }

    private String emitCommandRegisterCall(MethodCallExpr call) {
        List<String> parts = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            parts.add(emitExpression(call.getArgument(i), null));
        }
        List<KType> handlerTypes = List.of(
                KType.simple("CommandSender", "pumpkin.Command.CommandSender", KType.Category.OBJECT),
                serverType(),
                KType.simple("ConsumedArgs", "pumpkin.Command.ConsumedArgs", KType.Category.OBJECT));
        Expression handler = call.getArgument(3);
        if (handler instanceof LambdaExpr lambda) {
            parts.add(emitLambda(lambda, handlerTypes, value -> "Result.success(" + value + ")"));
        } else if (handler instanceof MethodReferenceExpr reference) {
            String scope = reference.getScope() instanceof com.github.javaparser.ast.expr.TypeExpr typeExpr
                    ? types.map(typeExpr.getType()).render()
                    : emitExpression(reference.getScope(), null);
            String method = name(reference.getIdentifier());
            parts.add("{ sender, server, args -> Result.success(" + scope + "." + method
                    + "(sender, server, args)) }");
        } else {
            parts.add(emitExpression(handler, null));
        }
        return "commands.register(" + String.join(", ", parts) + ")";
    }

    private String emitCommandSuggestionCall(MethodCallExpr call) {
        List<String> parts = new ArrayList<>();
        parts.add(emitExpression(call.getArgument(0), null));
        parts.add(emitLambda((LambdaExpr) call.getArgument(1),
                List.of(
                        KType.simple("CommandSender", "pumpkin.Command.CommandSender", KType.Category.OBJECT),
                        serverType(),
                        KType.simple("SuggestionRequest", "pumpkin.Command.SuggestionRequest", KType.Category.OBJECT)
                )));
        return "commands.onSuggestions(" + String.join(", ", parts) + ")";
    }

    private String emitListenCall(MethodCallExpr call, String scopeText) {
        List<String> parts = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            parts.add(emitExpression(call.getArgument(i), null));
        }
        Expression listener = call.getArgument(4);
        if (listener instanceof LambdaExpr lambda) {
            parts.add(emitLambda(lambda, List.of(serverType(), eventType())));
        } else {
            parts.add(emitExpression(listener, null));
        }
        return (scopeText == null ? "events" : scopeText) + ".listen(" + String.join(", ", parts) + ")";
    }

    private String pumpkinTypeName(String simple) {
        if (target != Target.PUMPKIN) {
            return null;
        }
        return switch (simple) {
            case "PumpkinPlugin" -> imports.ref(options.compatPlugin()
                    ? "plugin.PumpkinCompatPlugin"
                    : "plugin.PumpkinPlugin");
            case "PluginContext" -> imports.ref("plugin.PluginContext");
            case "PluginMetadata" -> imports.ref("plugin.PluginMetadata");
            case "TaskScheduler" -> imports.ref("plugin.PluginTasks");
            case "ScheduledTask" -> imports.ref("plugin.ScheduledTask");
            case "Player" -> imports.ref("pumpkin.Player.Player");
            case "World" -> imports.ref("pumpkin.World.World");
            case "TextComponent" -> imports.ref("pumpkin.Text.TextComponent");
            case "Scoreboard" -> imports.ref("pumpkin.Scoreboard");
            case "RenderType" -> imports.ref("pumpkin.Scoreboard") + ".RenderType";
            case "DisplaySlot" -> imports.ref("pumpkin.Scoreboard") + ".DisplaySlot";
            case "NametagVisibility" -> imports.ref("pumpkin.Scoreboard") + ".NametagVisibility";
            case "CollisionRule" -> imports.ref("pumpkin.Scoreboard") + ".CollisionRule";
            case "TeamSettings" -> imports.ref("pumpkin.Scoreboard") + ".TeamSettings";
            case "JavaDialogs" -> imports.ref("pumpkin.JavaDialogs");
            case "DialogType" -> imports.ref("pumpkin.JavaDialogs") + ".DialogType";
            case "AfterAction" -> imports.ref("pumpkin.JavaDialogs") + ".AfterAction";
            case "LinkType" -> imports.ref("pumpkin.JavaDialogs") + ".LinkType";
            case "DialogBody" -> imports.ref("pumpkin.JavaDialogs") + ".DialogBody";
            case "PlainMessage" -> imports.ref("pumpkin.JavaDialogs") + ".DialogBody.PlainMessage";
            case "ItemBody" -> imports.ref("pumpkin.JavaDialogs") + ".DialogBody.Item";
            case "DialogInputBool" -> imports.ref("pumpkin.JavaDialogs") + ".DialogInputBool";
            case "DialogInputText" -> imports.ref("pumpkin.JavaDialogs") + ".DialogInputText";
            case "DialogInputNumberRange" -> imports.ref("pumpkin.JavaDialogs") + ".DialogInputNumberRange";
            case "DialogInputSingleOption" -> imports.ref("pumpkin.JavaDialogs") + ".DialogInputSingleOption";
            case "DialogInput" -> imports.ref("pumpkin.JavaDialogs") + ".DialogInput";
            case "BoolInput" -> imports.ref("pumpkin.JavaDialogs") + ".DialogInput.Bool";
            case "TextInput" -> imports.ref("pumpkin.JavaDialogs") + ".DialogInput.Text";
            case "NumberRangeInput" -> imports.ref("pumpkin.JavaDialogs") + ".DialogInput.NumberRange";
            case "SingleOptionInput" -> imports.ref("pumpkin.JavaDialogs") + ".DialogInput.SingleOption";
            case "CustomClickAction" -> imports.ref("pumpkin.JavaDialogs") + ".CustomClickAction";
            case "Action" -> imports.ref("pumpkin.JavaDialogs") + ".Action";
            case "OpenUrl" -> imports.ref("pumpkin.JavaDialogs") + ".Action.OpenUrl";
            case "CustomClick" -> imports.ref("pumpkin.JavaDialogs") + ".Action.CustomClick";
            case "ActionButton" -> imports.ref("pumpkin.JavaDialogs") + ".ActionButton";
            case "LinkLabel" -> imports.ref("pumpkin.JavaDialogs") + ".LinkLabel";
            case "BuiltIn" -> imports.ref("pumpkin.JavaDialogs") + ".LinkLabel.BuiltIn";
            case "CustomLabel" -> imports.ref("pumpkin.JavaDialogs") + ".LinkLabel.Custom";
            case "Link" -> imports.ref("pumpkin.JavaDialogs") + ".Link";
            case "Dialog" -> imports.ref("pumpkin.JavaDialogs") + ".Dialog";
            case "Forms" -> imports.ref("pumpkin.Forms");
            case "Form" -> imports.ref("pumpkin.Forms") + ".Form";
            case "SimpleForm" -> imports.ref("pumpkin.Forms") + ".SimpleForm";
            case "ModalForm" -> imports.ref("pumpkin.Forms") + ".ModalForm";
            case "CustomForm" -> imports.ref("pumpkin.Forms") + ".CustomForm";
            case "SimpleFormButton" -> imports.ref("pumpkin.Forms") + ".SimpleFormButton";
            case "FormImage" -> imports.ref("pumpkin.Forms") + ".FormImage";
            case "CustomFormElement" -> imports.ref("pumpkin.Forms") + ".CustomFormElement";
            case "ImageType" -> imports.ref("pumpkin.Forms") + ".ImageType";
            case "Recipe" -> imports.ref("pumpkin.Recipe");
            case "RecipeCategory" -> imports.ref("pumpkin.Recipe") + ".RecipeCategory";
            case "Ingredient" -> imports.ref("pumpkin.Recipe") + ".Ingredient";
            case "ShapedRecipe" -> imports.ref("pumpkin.Recipe") + ".ShapedRecipe";
            case "ShapelessRecipe" -> imports.ref("pumpkin.Recipe") + ".ShapelessRecipe";
            case "CookingRecipe" -> imports.ref("pumpkin.Recipe") + ".CookingRecipe";
            case "CookingType" -> imports.ref("pumpkin.Recipe") + ".CookingType";
            case "RecipeManager" -> imports.ref("pumpkin.Recipe") + ".RecipeManager";
            case "Datapack" -> imports.ref("pumpkin.Datapack");
            case "DatapackInfo" -> imports.ref("pumpkin.Datapack") + ".DatapackInfo";
            case "EnablePosition" -> imports.ref("pumpkin.Datapack") + ".EnablePosition";
            case "DatapackManager" -> imports.ref("pumpkin.Datapack") + ".DatapackManager";
            case "Enchantments" -> imports.ref("pumpkin.Enchantments");
            case "AttributeModifierSlot" -> imports.ref("pumpkin.Enchantments") + ".AttributeModifierSlot";
            case "Enchantment" -> imports.ref("pumpkin.Enchantments") + ".Enchantment";
            case "CustomEnchantment" -> imports.ref("pumpkin.Enchantments") + ".CustomEnchantment";
            case "EnchantmentManager" -> imports.ref("pumpkin.Enchantments") + ".EnchantmentManager";
            case "Display" -> imports.ref("pumpkin.Display");
            case "BillboardMode" -> imports.ref("pumpkin.Display") + ".BillboardMode";
            case "ItemDisplayMode" -> imports.ref("pumpkin.Display") + ".ItemDisplayMode";
            case "TextAlignment" -> imports.ref("pumpkin.Display") + ".TextAlignment";
            case "Vector3f" -> imports.ref("pumpkin.Display") + ".Vector3f";
            case "Quaternionf" -> imports.ref("pumpkin.Display") + ".Quaternionf";
            case "DisplayTransformation" -> imports.ref("pumpkin.Display") + ".DisplayTransformation";
            case "DisplayEntity" -> imports.ref("pumpkin.Display") + ".DisplayEntity";
            case "BlockDisplayEntity" -> imports.ref("pumpkin.Display") + ".BlockDisplayEntity";
            case "ItemDisplayEntity" -> imports.ref("pumpkin.Display") + ".ItemDisplayEntity";
            case "TextDisplayEntity" -> imports.ref("pumpkin.Display") + ".TextDisplayEntity";
            case "Block" -> imports.ref("pumpkin.World") + ".Block";
            case "BlockState" -> imports.ref("pumpkin.World") + ".BlockState";
            case "Mob" -> imports.ref("pumpkin.World") + ".Mob";
            case "LivingEntity" -> imports.ref("pumpkin.World") + ".LivingEntity";
            case "BossBar" -> imports.ref("pumpkin.BossBar");
            case "BossBarColor" -> imports.ref("pumpkin.BossBar") + ".BossBarColor";
            case "BossBarDivision" -> imports.ref("pumpkin.BossBar") + ".BossBarDivision";
            case "Common" -> imports.ref("pumpkin.Common");
            case "EntityTypes" -> imports.ref("pumpkin.EntityTypes");
            case "EntityType" -> imports.ref("pumpkin.EntityTypes") + ".EntityType";
            case "AiGoal" -> imports.ref("plugin.AiGoal");
            case "AiGoalRegistry" -> imports.ref("plugin.AiGoalRegistry");
            case "Gui" -> imports.ref("pumpkin.Gui") + ".Gui";
            case "Screens" -> imports.ref("pumpkin.Screens");
            case "Screen" -> imports.ref("pumpkin.Screens") + ".Screen";
            case "JavaPlayer" -> imports.ref("pumpkin.Player") + ".JavaPlayer";
            case "BedrockPlayer" -> imports.ref("pumpkin.Player") + ".BedrockPlayer";
            case "JavaPackets" -> imports.ref("pumpkin.JavaPackets");
            case "CPlayDisconnect" -> imports.ref("pumpkin.JavaPackets") + ".CPlayDisconnect";
            case "CKeepAlive" -> imports.ref("pumpkin.JavaPackets") + ".CKeepAlive";
            case "CGameEvent" -> imports.ref("pumpkin.JavaPackets") + ".CGameEvent";
            case "CTransfer" -> imports.ref("pumpkin.JavaPackets") + ".CTransfer";
            case "BedrockPackets" -> imports.ref("pumpkin.BedrockPackets");
            case "CDisconnect" -> imports.ref("pumpkin.BedrockPackets") + ".CDisconnect";
            case "CSetTime" -> imports.ref("pumpkin.BedrockPackets") + ".CSetTime";
            case "CModalFormRequest" -> imports.ref("pumpkin.BedrockPackets") + ".CModalFormRequest";
            case "CCustomPayload" -> imports.ref("pumpkin.JavaPackets") + ".ClientboundPacket.CCustomPayload";
            case "Command" -> imports.ref("pumpkin.Command");
            case "CommandSender" -> imports.ref("pumpkin.Command") + ".CommandSender";
            case "CommandNode" -> imports.ref("pumpkin.Command") + ".CommandNode";
            case "ConsumedArgs" -> imports.ref("pumpkin.Command") + ".ConsumedArgs";
            case "Arg" -> imports.ref("pumpkin.Command") + ".Arg";
            case "SuggestionRequest" -> imports.ref("pumpkin.Command") + ".SuggestionRequest";
            case "CommandSuggestions" -> imports.ref("pumpkin.Command") + ".CommandSuggestions";
            case "PermissionLevel" -> imports.ref("pumpkin.Command") + ".PermissionLevel";
            case "IpcRegistry" -> imports.ref("plugin.IpcRegistry");
            case "EventRegistry" -> imports.ref("plugin.EventRegistry");
            case "CommandRegistry" -> imports.ref("plugin.CommandRegistry");
            case "GameMode" -> imports.ref("pumpkin.Common.GameMode");
            case "Hand" -> imports.ref("pumpkin.Common.Hand");
            case "BlockPos" -> imports.ref("pumpkin.Common.BlockPos");
            case "NamedColor" -> imports.ref("pumpkin.Common.NamedColor");
            case "RgbColor" -> imports.ref("pumpkin.Common.RgbColor");
            case "ArgbColor" -> imports.ref("pumpkin.Common.ArgbColor");
            case "Server" -> imports.ref("pumpkin.Server.Server");
            case "ItemStack" -> imports.ref("pumpkin.ItemStack.ItemStack");
            case "Inventory" -> imports.ref("pumpkin.Inventory.Inventory");
            case "PlayerInventory" -> imports.ref("pumpkin.Inventory.PlayerInventory");
            case "Logging" -> imports.ref("pumpkin.Logging");
            case "Event" -> imports.ref("pumpkin.Event");
            case "EventType" -> imports.ref("pumpkin.Event") + ".EventType";
            case "EventPriority" -> imports.ref("pumpkin.Event") + ".EventPriority";
            case "PermissionDefault" -> imports.ref("pumpkin.Permission") + ".PermissionDefault";
            case "Particles" -> imports.ref("pumpkin.Particles");
            case "Particle" -> imports.ref("pumpkin.Particles") + ".Particle";
            case "Sounds" -> imports.ref("pumpkin.Sounds");
            case "Sound" -> imports.ref("pumpkin.Sounds") + ".Sound";
            case "SoundCategory" -> imports.ref("pumpkin.Sounds") + ".SoundCategory";
            default -> null;
        };
    }

    private String emitFieldAccess(FieldAccessExpr field) {
        String scopeText = emitExpression(field.getScope(), null);
        String name = field.getNameAsString();
        KType receiverType = oracle.infer(field.getScope());

        if (receiverType != null) {
            String javaName = receiverType.javaName();
            if (javaName.startsWith("pumpkin.World.Block") && !javaName.startsWith("pumpkin.World.BlockState")) {
                if (name.equals("id") || name.equals("itemId") || name.equals("defaultStateId")) {
                    return scopeText + "." + name + ".toInt()";
                }
                if (name.equals("mapColor")) {
                    return scopeText + ".mapColor.toInt()";
                }
            }
            if (javaName.startsWith("pumpkin.World.BlockState")) {
                if (name.equals("id") || name.equals("blockId")) {
                    return scopeText + "." + name + ".toInt()";
                }
                if (name.equals("luminance") || name.equals("opacity")) {
                    return scopeText + "." + name + ".toInt()";
                }
            }
            if (javaName.startsWith("pumpkin.Enchantments.CustomEnchantment")) {
                if (name.equals("maxLevel") || name.equals("anvilCost") || name.equals("weight")) {
                    return scopeText + "." + name + ".toInt()";
                }
            }
            if (javaName.startsWith("pumpkin.Datapack.DatapackInfo")) {
                if (name.equals("packFormat") || name.equals("recipeCount") || name.equals("functionCount")) {
                    return scopeText + "." + name + ".toInt()";
                }
            }
            if (javaName.startsWith("pumpkin.Display.TextDisplayEntity") && name.equals("textOpacity")) {
                return scopeText + ".textOpacity.toInt()";
            }
        }

        if (receiverType.isArray() && name.equals("length")) {
            return scopeText + ".size";
        }
        if (target == Target.PUMPKIN && scopeText.endsWith("PermissionDefault")) {
            return switch (name) {
                case "ALLOW" -> imports.ref("pumpkin.Permission") + ".PermissionDefault.Allow";
                case "DENY", "NOT_SET" -> imports.ref("pumpkin.Permission") + ".PermissionDefault.Deny";
                case "OP" -> imports.ref("pumpkin.Permission")
                        + ".PermissionDefault.Op(" + imports.ref("pumpkin.Permission")
                        + ".PermissionLevel.THREE)";
                default -> scopeText + "." + name;
            };
        }
        if (isBoxedConstant(scopeText, name)) {
            return boxedConstant(scopeText, name);
        }
        if (scopeText.endsWith("Math") && name.equals("PI")) {
            imports.wildcard("kotlin.math");
            return "PI";
        }
        if (scopeText.endsWith("Math") && name.equals("E")) {
            imports.wildcard("kotlin.math");
            return "E";
        }
        if (name.equals("class")) {
            return scopeText + "::class";
        }
        return scopeText + "." + name(name);
    }

    private boolean isBoxedConstant(String scope, String name) {
        String simple = Imports.simpleName(scope);
        return (simple.equals("Integer") || simple.equals("Long") || simple.equals("Short")
                || simple.equals("Byte") || simple.equals("Double") || simple.equals("Float")
                || simple.equals("Boolean") || simple.equals("Character"))
                && (name.equals("MAX_VALUE") || name.equals("MIN_VALUE") || name.equals("NaN")
                || name.equals("POSITIVE_INFINITY") || name.equals("NEGATIVE_INFINITY")
                || name.equals("TRUE") || name.equals("FALSE"));
    }

    private String boxedConstant(String scope, String name) {
        String simple = Imports.simpleName(scope);
        if (simple.equals("Integer")) return "Int." + name;
        if (simple.equals("Character")) return "Char." + name;
        if (simple.equals("Boolean")) {
            if (name.equals("TRUE")) return "true";
            if (name.equals("FALSE")) return "false";
        }
        return simple + "." + name;
    }

    private String emitMethodCall(MethodCallExpr call, KType expected) {
        String name = call.getNameAsString();
        String scopeText = call.getScope().map(value -> emitExpression(value, null)).orElse(null);
        KType receiverType = call.getScope().map(oracle::infer).orElse(null);

        if (target == Target.PUMPKIN && scopeText != null
                && (scopeText.equals("commands") || scopeText.endsWith(".commands"))
                && name.equals("register") && call.getArguments().size() == 4
                && (call.getArgument(3) instanceof LambdaExpr
                || call.getArgument(3) instanceof MethodReferenceExpr)) {
            return emitCommandRegisterCall(call);
        }
        if (target == Target.PUMPKIN && "commands".equals(scopeText) && name.equals("onSuggestions")
                && call.getArguments().size() == 2
                && call.getArgument(1) instanceof LambdaExpr) {
            return emitCommandSuggestionCall(call);
        }
        if (target == Target.PUMPKIN && scopeText != null
                && (scopeText.equals("events") || scopeText.endsWith(".events"))
                && name.equals("listen") && call.getArguments().size() == 5) {
            return emitListenCall(call, scopeText);
        }
        if (target == Target.PUMPKIN && scopeText != null
                && (scopeText.equals("tasks") || scopeText.endsWith(".tasks"))
                && name.equals("afterTicks") && call.getArguments().size() == 2) {
            String delay = emitExpression(call.getArgument(0), null);
            String action = call.getArgument(1) instanceof LambdaExpr lambda
                    ? emitLambda(lambda, List.of(serverType()))
                    : emitExpression(call.getArgument(1), null);
            return (scopeText == null ? "tasks" : scopeText) + ".afterTicks((" + delay + ").toULong(), " + action + ")";
        }
        if (target == Target.PUMPKIN && scopeText != null
                && (scopeText.equals("tasks") || scopeText.endsWith(".tasks"))
                && name.equals("everyTicks") && call.getArguments().size() == 3) {
            String delay = emitExpression(call.getArgument(0), null);
            String period = emitExpression(call.getArgument(1), null);
            String action = call.getArgument(2) instanceof LambdaExpr lambda
                    ? emitLambda(lambda, List.of(serverType()))
                    : emitExpression(call.getArgument(2), null);
            String compatScope = scopeText == null ? "compatTasks"
                    : scopeText.endsWith(".tasks")
                    ? scopeText.substring(0, scopeText.length() - ".tasks".length()) + ".compatTasks"
                    : "compatTasks";
            return compatScope + ".everyTicks((" + delay + ").toULong(), ("
                    + period + ").toULong(), " + action + ")";
        }

        List<String> args = call.getArguments().stream()
                .map(value -> emitExpression(value, null)).toList();
        String argsText = String.join(", ", args);

        if (target == Target.PUMPKIN && name.equals("asString") && args.isEmpty()) {
            String command = imports.ref("pumpkin.Command");
            return "when (val arg = " + scopeText + ") { is " + command + ".Arg.Simple -> arg.value; is "
                    + command + ".Arg.Msg -> arg.value; else -> \"\" }";
        }

        if (target == Target.PUMPKIN && "ItemStack".equals(scopeText) && name.equals("of")
                && (args.size() == 1 || args.size() == 2)) {
            String count = args.size() == 1 ? "1" : args.get(1);
            return imports.ref("pumpkin.ItemStack.ItemStack") + "(" + args.get(0)
                    + ", (" + count + ").toUByte())";
        }
        String pumpkinCall = mapPumpkinCall(name, scopeText, receiverType, args, call);
        if (pumpkinCall != null) {
            return pumpkinCall;
        }
        if (target == Target.PUMPKIN && scopeText != null && scopeText.endsWith(".value")
                && name.startsWith("set") && name.length() > 3 && args.size() == 1) {
            return scopeText + "." + propertyName(name.substring(3)) + " = " + args.get(0);
        }

        if (call.getScope().isPresent() && call.getScope().get() instanceof MethodCallExpr innerOptional
                && (innerOptional.getNameAsString().equals("map")
                || innerOptional.getNameAsString().equals("flatMap")
                || innerOptional.getNameAsString().equals("filter"))
                && (name.equals("orElse") || name.equals("orElseGet"))) {
            String innerText = emitMethodCall(innerOptional, null);
            String fallback = args.isEmpty() ? "null" : args.get(0);
            if (name.equals("orElseGet")) {
                fallback = fallback + "()";
            }
            return "(" + innerText + " ?: " + fallback + ")";
        }
        boolean pluginScope = scopeText == null || scopeText.equals("this")
                || (receiverType != null && (receiverType.javaName().startsWith("plugin.PumpkinCompatPlugin")
                || receiverType.javaName().startsWith("plugin.PumpkinPlugin")
                || receiverType.javaName().startsWith("pumpkin.PumpkinPlugin")));
        if (target == Target.PUMPKIN && name.equals("getEvents") && args.isEmpty() && pluginScope) {
            return scopeText == null || scopeText.equals("this") ? "events" : scopeText + ".events";
        }
        if (target == Target.PUMPKIN && name.equals("getTasks") && args.isEmpty() && pluginScope) {
            return scopeText == null || scopeText.equals("this") ? "tasks" : scopeText + ".tasks";
        }
        if (target == Target.PUMPKIN && name.equals("getCommands") && args.isEmpty() && pluginScope) {
            return scopeText == null || scopeText.equals("this") ? "commands" : scopeText + ".commands";
        }
        if (target == Target.PUMPKIN && name.equals("getIpc") && args.isEmpty() && pluginScope) {
            return scopeText == null || scopeText.equals("this") ? "ipc" : scopeText + ".ipc";
        }
        if (target == Target.PUMPKIN && name.equals("getAiGoals") && args.isEmpty() && pluginScope) {
            return scopeText == null || scopeText.equals("this") ? "aiGoals" : scopeText + ".aiGoals";
        }
        if (target == Target.PUMPKIN && scopeText != null
                && (scopeText.equals("tasks") || scopeText.endsWith(".tasks"))
                && name.equals("afterTicks") && args.size() == 2) {
            return (scopeText == null ? "tasks" : scopeText) + ".afterTicks((" + args.get(0) + ").toULong(), " + args.get(1) + ")";
        }
        if (target == Target.PUMPKIN && name.equals("sendMessage") && args.size() == 1
                && receiverType != null && receiverType.javaName().startsWith("pumpkin.Player")
                && (oracle.infer(call.getArgument(0)).isString()
                || call.getArgument(0) instanceof StringLiteralExpr)) {
            String text = imports.ref("pumpkin.Text.TextComponent");
            return scopeText + ".sendMessage(" + text + ".text(" + args.get(0) + "))";
        }
        if (target == Target.PUMPKIN && (name.equals("registerCommand")
                || ("commands".equals(scopeText) && name.equals("register")))) {
            diagnostics.warn("J2KT-PUMPKIN-020",
                    "Command registration is not mapped yet; review the callback and args type against commands.register(...).",
                    call);
        }
        if (target == Target.PUMPKIN && scopeText != null && scopeText.equals("System.out")
                && (name.equals("println") || name.equals("print"))) {
            String logging = imports.ref("pumpkin.Logging");
            return logging + ".log(" + logging + ".Level.INFO, "
                    + (args.isEmpty() ? "\"\"" : args.get(0)) + ")";
        }
        if (scopeText != null) {
            String simpleScope = Imports.simpleName(scopeText);
            if (scopeText.equals("System.out")) {
                if (name.equals("println") || name.equals("print")) {
                    return name + "(" + argsText + ")";
                }
                if (name.equals("printf") || name.equals("format")) {
                    diagnostics.warn("J2KT-CALL-001", "System.out.printf has no Kotlin/Wasm equivalent.", call);
                    if (target == Target.PUMPKIN) {
                        return "TODO(\"j2kt/pumpkin: System.out.printf\")";
                    }
                    return "print(String.format(" + argsText + "))";
                }
            }
            if (scopeText.equals("System") && name.equals("arraycopy") && args.size() == 5) {
                return args.get(0) + ".copyInto(" + args.get(2) + ", " + args.get(3) + ", "
                        + args.get(1) + ", " + args.get(1) + " + " + args.get(4) + ")";
            }
            if (scopeText.equals("System") && target == Target.PUMPKIN
                    && (name.equals("currentTimeMillis") || name.equals("nanoTime"))) {
                diagnostics.warn("J2KT-PUMPKIN-021",
                        "System." + name + " is not part of the portable Kotlin/Wasm API; use a Pumpkin time/runtime primitive.",
                        call);
                return "TODO(\"j2kt/pumpkin: System." + name + "\")";
            }
            if (scopeText.equals("System.err")) {
                if (target == Target.PUMPKIN) {
                    return "Logging.log(Logging.Level.ERROR, " + (args.isEmpty() ? "\"\"" : args.get(0)) + ")";
                }
                return "System.err." + name + "(" + argsText + ")";
            }
            if (simpleScope.equals("List") && name.equals("of")) {
                String typeArg = expected != null && expected.element() != null
                        ? "<" + expected.element().render() + ">" : "";
                return (expected != null && expected.isCollection() && expected.mutable()
                        ? "mutableListOf" + typeArg + "(" : "listOf" + typeArg + "(") + argsText + ")";
            }
            if (simpleScope.equals("List") && name.equals("copyOf")) {
                if (args.size() != 1) {
                    return null;
                }
                return args.get(0) + (expected != null && expected.mutable() ? ".toMutableList()" : ".toList()");
            }
            if (simpleScope.equals("Set") && name.equals("of")) {
                String typeArg = expected != null && expected.element() != null
                        ? "<" + expected.element().render() + ">" : "";
                return (expected != null && expected.isCollection() && expected.mutable()
                        ? "mutableSetOf" + typeArg + "(" : "setOf" + typeArg + "(") + argsText + ")";
            }
            if (simpleScope.equals("Map") && name.equals("of")) {
                List<String> pairs = new ArrayList<>();
                for (int i = 0; i + 1 < args.size(); i += 2) {
                    pairs.add(args.get(i) + " to " + args.get(i + 1));
                }
                String typeArgs = expected != null && expected.key() != null && expected.element() != null
                        ? "<" + expected.key().render() + ", " + expected.element().render() + ">" : "";
                return (expected != null && expected.isMap() && expected.mutable()
                        ? "mutableMapOf" + typeArgs + "(" : "mapOf" + typeArgs + "(")
                        + String.join(", ", pairs) + ")";
            }
            if (simpleScope.equals("Optional")) {
                switch (name) {
                    case "of", "ofNullable" -> {
                        return args.isEmpty() ? "null" : args.get(0);
                    }
                    case "empty" -> {
                        return "null";
                    }
                    default -> { }
                }
            }
            if (receiverType != null && receiverType.javaName().equals("java.util.Optional")) {
                switch (name) {
                    case "isPresent" -> { return scopeText + " != null"; }
                    case "isEmpty" -> { return scopeText + " == null"; }
                    case "get" -> { return scopeText; }
                    case "orElse" -> { return "(" + scopeText + " ?: " + (args.isEmpty() ? "null" : args.get(0)) + ")"; }
                    case "orElseGet" -> { return "(" + scopeText + " ?: " + (args.isEmpty() ? "null" : args.get(0) + "()") + ")"; }
                    case "orElseThrow" -> { return "requireNotNull(" + scopeText + ")"; }
                    case "ifPresent", "map", "flatMap" -> {
                        return scopeText + "?.let(" + (args.isEmpty() ? "{}" : args.get(0)) + ")";
                    }
                    case "filter" -> {
                        return scopeText + "?.takeIf(" + (args.isEmpty() ? "{}" : args.get(0)) + ")";
                    }
                    default -> { }
                }
            }
            if (simpleScope.equals("Math")) {
                return mapMathCall(name, args, call);
            }
            if (simpleScope.equals("Objects")) {
                return mapObjectsCall(name, args, call);
            }
            if (simpleScope.equals("Collections")) {
                String mapped = mapCollectionsCall(name, args, call);
                if (mapped != null) {
                    return mapped;
                }
            }
            if (simpleScope.equals("Arrays")) {
                String mapped = mapArraysCall(name, args, call);
                if (mapped != null) {
                    return mapped;
                }
            }
            if (simpleScope.equals("String")) {
                String mapped = mapStringStatic(name, args);
                if (mapped != null) {
                    return mapped;
                }
            }
            if (simpleScope.equals("Integer") || simpleScope.equals("Long") || simpleScope.equals("Short")
                    || simpleScope.equals("Byte") || simpleScope.equals("Double") || simpleScope.equals("Float")
                    || simpleScope.equals("Boolean") || simpleScope.equals("Character")) {
                String mapped = mapBoxedCall(simpleScope, name, args, call);
                if (mapped != null) {
                    return mapped;
                }
            }
            if (simpleScope.equals("UUID")) {
                if (name.equals("randomUUID")) {
                    if (target == Target.PUMPKIN) {
                        return imports.ref("pumpkin.Uuid") + ".generate()";
                    }
                    return "UUID.randomUUID()";
                }
            }
            if (scopeText.equals("Thread") && name.equals("sleep")) {
                diagnostics.warn("J2KT-CONCURRENCY-001",
                        "Thread.sleep has no blocking equivalent in Kotlin/Wasm; use tasks.afterTicks or a suspend delay.", call);
                if (target == Target.PUMPKIN) {
                    return "TODO(\"j2kt/pumpkin: Thread.sleep\")";
                }
                return "Thread.sleep(" + argsText + ")";
            }
        }

        if (receiverType != null && receiverType.category() == KType.Category.FUNCTION && scopeText != null) {
            if (name.equals("apply") || name.equals("accept") || name.equals("test")
                    || name.equals("get") || name.equals("run") || name.equals("call")
                    || name.equals("invoke")) {
                return scopeText + "(" + argsText + ")";
            }
        }
        if (receiverType != null && args.isEmpty() && scopeText != null) {
            Set<String> components = recordComponents.get(Imports.simpleName(receiverType.base()));
            if (components != null && components.contains(name)) {
                return scopeText + "." + name(name);
            }
        }
        if (scopeText != null && args.isEmpty()
                && (name.equals("toUpperCase") || name.equals("toLowerCase"))) {
            return scopeText + (name.equals("toUpperCase") ? ".uppercase()" : ".lowercase()");
        }
        if (receiverType != null && receiverType.isString()) {
            String mapped = mapStringInstance(name, scopeText, args, call);
            if (mapped != null) {
                return mapped;
            }
        }
        if (receiverType != null && receiverType.isCollection()) {
            String mapped = mapCollectionInstance(name, receiverType, scopeText, args, call);
            if (mapped != null) {
                return mapped;
            }
        }
        if (receiverType != null && receiverType.isMap()) {
            String mapped = mapMapInstance(name, receiverType, scopeText, args, call);
            if (mapped != null) {
                return mapped;
            }
        }

        if (args.isEmpty() && (name.equals("getKey") || name.equals("getValue")) && scopeText != null
                && (receiverType == null || !receiverType.javaName().startsWith("pumpkin."))) {
            return scopeText + (name.equals("getKey") ? ".key" : ".value");
        }
        if (name.equals("equals") && args.size() == 1) {
            return emitExpression(call.getScope().orElseThrow(), null) + " == " + args.get(0);
        }
        if (name.equals("hashCode") && args.isEmpty()) {
            return emitExpression(call.getScope().orElseThrow(), null) + ".hashCode()";
        }
        if (name.equals("getClass") && args.isEmpty()) {
            return emitExpression(call.getScope().orElseThrow(), null) + "::class";
        }
        if (name.equals("toString") && args.isEmpty()) {
            return emitExpression(call.getScope().orElseThrow(), null) + ".toString()";
        }

        if (options.convertProperties() && scopeText != null) {
            String property = propertyCall(call, receiverType, name, args);
            if (property != null) {
                return property;
            }
        }

        if (scopeText == null) {
            return name(name) + "(" + argsText + ")";
        }
        return scopeText + "." + name(name) + "(" + argsText + ")";
    }

    private String mapStringStatic(String name, List<String> args) {
        return switch (name) {
            case "valueOf" -> args.isEmpty() ? "\"\"" : args.get(0) + ".toString()";
            case "join" -> args.size() >= 2 ? args.get(1) + ".joinToString(" + args.get(0) + ")" : null;
            case "format" -> {
                if (target == Target.PUMPKIN) {
                    diagnostics.warn("J2KT-PUMPKIN-004",
                            "String.format is not available in Kotlin/Wasm common code; use string templates or Text.TextComponent.", null);
                    yield "TODO(\"j2kt/pumpkin: String.format\")";
                }
                yield "String.format(" + String.join(", ", args) + ")";
            }
            default -> null;
        };
    }

    private String mapStringInstance(String name, String receiver, List<String> args, MethodCallExpr call) {
        return switch (name) {
            case "length" -> receiver + ".length";
            case "charAt" -> receiver + "[" + (args.isEmpty() ? "0" : args.get(0)) + "]";
            case "equals" -> args.size() == 1 ? receiver + " == " + args.get(0) : null;
            case "equalsIgnoreCase" -> args.size() == 1 ? receiver + ".equals(" + args.get(0) + ", ignoreCase = true)" : null;
            case "isEmpty" -> receiver + ".isEmpty()";
            case "isBlank" -> receiver + ".isBlank()";
            case "substring", "trim", "strip", "stripLeading", "stripTrailing", "toLowerCase", "toUpperCase",
                 "replace", "concat", "repeat", "intern" -> receiver + "." + kotlinStringName(name) + "(" + String.join(", ", args) + ")";
            case "toCharArray" -> receiver + ".toCharArray()";
            case "split" -> {
                diagnostics.warn("J2KT-STRING-001",
                        "Java String.split returns String[]; Kotlin split returns List<String>. Add .toTypedArray() if the target expects an array.",
                        call);
                yield receiver + ".split(" + String.join(", ", args) + ")";
            }
            default -> null;
        };
    }

    private String kotlinStringName(String javaName) {
        return switch (javaName) {
            case "toLowerCase" -> "lowercase";
            case "toUpperCase" -> "uppercase";
            case "strip", "stripLeading", "stripTrailing" -> javaName;
            default -> javaName;
        };
    }

    private String mapCollectionInstance(String name, KType receiverType, String receiver, List<String> args, MethodCallExpr call) {
        return switch (name) {
            case "size" -> receiver + ".size";
            case "isEmpty" -> receiver + ".isEmpty()";
            case "get" -> args.size() == 1 ? receiver + "[" + args.get(0) + "]" : null;
            case "set" -> args.size() == 2 ? receiver + "[" + args.get(0) + "] = " + args.get(1) : null;
            case "add" -> receiver + ".add(" + String.join(", ", args) + ")";
            case "addAll" -> receiver + ".addAll(" + String.join(", ", args) + ")";
            case "contains" -> receiver + ".contains(" + String.join(", ", args) + ")";
            case "remove" -> {
                if (args.size() == 1 && isIntExpression(call.getArgument(0))) {
                    yield receiver + ".removeAt(" + args.get(0) + ")";
                }
                yield receiver + ".remove(" + String.join(", ", args) + ")";
            }
            case "removeAt" -> receiver + ".removeAt(" + String.join(", ", args) + ")";
            case "clear" -> receiver + ".clear()";
            case "iterator" -> receiver + ".iterator()";
            default -> null;
        };
    }

    private String mapMapInstance(String name, KType receiverType, String receiver, List<String> args, MethodCallExpr call) {
        return switch (name) {
            case "size" -> receiver + ".size";
            case "isEmpty" -> receiver + ".isEmpty()";
            case "get" -> args.size() == 1 ? receiver + "[" + args.get(0) + "]" : null;
            case "getOrDefault" -> args.size() == 2
                    ? "(if (" + receiver + ".containsKey(" + args.get(0) + ")) "
                    + receiver + "[" + args.get(0) + "] else " + args.get(1) + ")" : null;
            case "put" -> args.size() == 2 ? receiver + "[" + args.get(0) + "] = " + args.get(1) : null;
            case "putAll" -> receiver + ".putAll(" + String.join(", ", args) + ")";
            case "containsKey" -> receiver + ".containsKey(" + String.join(", ", args) + ")";
            case "containsValue" -> receiver + ".containsValue(" + String.join(", ", args) + ")";
            case "remove" -> receiver + ".remove(" + String.join(", ", args) + ")";
            case "keySet" -> receiver + ".keys";
            case "values" -> receiver + ".values";
            case "entrySet" -> receiver + ".entries";
            default -> null;
        };
    }

    /**
     * Maps the friendly Java stub API to the actual generated Pumpkin Kotlin API.
     * The generated bindings differ from the documented helper layer in a few
     * important places, so these mappings are intentionally explicit.
     */
    private String mapPumpkinCall(String name, String scopeText, KType receiverType,
                                  List<String> args, MethodCallExpr call) {
        if (target != Target.PUMPKIN || scopeText == null) {
            return null;
        }
        String scopeSimple = scopeText;
        int scopeDot = scopeSimple.lastIndexOf('.');
        if (scopeDot >= 0) {
            scopeSimple = scopeSimple.substring(scopeDot + 1);
        }
        if (target == Target.PUMPKIN && ("Forms".equals(scopeSimple) || "Forms".equals(scopeText))) {
            String forms = imports.ref("pumpkin.Forms");
            String text = imports.ref("pumpkin.Text.TextComponent");
            switch (name) {
                case "button" -> {
                    if (args.size() == 1) {
                        return forms + ".SimpleFormButton(" + text + ".text(" + args.get(0) + "), null)";
                    }
                    if (args.size() == 2) {
                        return forms + ".SimpleFormButton(" + text + ".text(" + args.get(0) + "), " + args.get(1) + ")";
                    }
                }
                case "image" -> {
                    if (args.size() == 2) {
                        return forms + ".FormImage(" + args.get(0) + ", " + args.get(1) + ")";
                    }
                }
                case "label" -> {
                    if (args.size() == 1) {
                        return forms + ".CustomFormElement.Label(" + text + ".text(" + args.get(0) + "))";
                    }
                }
                case "toggle" -> {
                    if (args.size() == 2) {
                        return forms + ".CustomFormElement.Toggle(" + text + ".text(" + args.get(0) + ") to " + args.get(1) + ")";
                    }
                }
                case "input" -> {
                    if (args.size() == 3) {
                        return forms + ".CustomFormElement.Input(Triple(" + text + ".text(" + args.get(0)
                                + "), " + args.get(1) + ", " + args.get(2) + "))";
                    }
                }
                case "dropdown" -> {
                    if (args.size() == 3) {
                        return forms + ".CustomFormElement.Dropdown(Triple(" + text + ".text(" + args.get(0)
                                + "), " + args.get(1) + ", (" + args.get(2) + ").toUInt()))";
                    }
                }
                case "slider" -> {
                    if (args.size() == 5) {
                        return forms + ".CustomFormElement.Slider(pumpkin.runtime.Tuple5(" + text + ".text("
                                + args.get(0) + "), " + args.get(1) + ", " + args.get(2) + ", "
                                + args.get(3) + ", " + args.get(4) + "))";
                    }
                }
                case "stepSlider" -> {
                    if (args.size() == 3) {
                        return forms + ".CustomFormElement.StepSlider(Triple(" + text + ".text(" + args.get(0)
                                + "), " + args.get(1) + ", (" + args.get(2) + ").toUInt()))";
                    }
                }
                default -> {
                }
            }
        }
        if (target == Target.PUMPKIN && ("Form".equals(scopeSimple) || "Form".equals(scopeText))) {
            String forms = imports.ref("pumpkin.Forms");
            switch (name) {
                case "simple" -> {
                    if (args.size() == 1) {
                        return forms + ".Form.Simple(" + args.get(0) + ")";
                    }
                }
                case "modal" -> {
                    if (args.size() == 1) {
                        return forms + ".Form.Modal(" + args.get(0) + ")";
                    }
                }
                case "custom" -> {
                    if (args.size() == 1) {
                        return forms + ".Form.Custom(" + args.get(0) + ")";
                    }
                }
                default -> {
                }
            }
        }
        if (target == Target.PUMPKIN && "JavaDialogs".equals(scopeSimple)) {
            String dialogs = imports.ref("pumpkin.JavaDialogs");
            String text = imports.ref("pumpkin.Text.TextComponent");
            switch (name) {
                case "message" -> {
                    if (args.size() == 1) {
                        return dialogs + ".DialogBody.PlainMessage(" + args.get(0) + ")";
                    }
                }
                case "itemBody" -> {
                    if (args.size() == 1) {
                        return dialogs + ".DialogBody.Item(" + args.get(0) + ")";
                    }
                }
                case "boolInput" -> {
                    if (args.size() == 2) {
                        return dialogs + ".DialogInput.Bool(" + dialogs + ".DialogInputBool("
                                + text + ".text(" + args.get(0) + "), " + args.get(1) + "))";
                    }
                }
                case "textInput" -> {
                    if (args.size() == 3) {
                        return dialogs + ".DialogInput.Text(" + dialogs + ".DialogInputText("
                                + text + ".text(" + args.get(0) + "), " + text + ".text("
                                + args.get(1) + "), " + args.get(2) + "))";
                    }
                }
                case "numberInput" -> {
                    if (args.size() == 6) {
                        return dialogs + ".DialogInput.NumberRange(" + dialogs + ".DialogInputNumberRange("
                                + text + ".text(" + args.get(0) + "), " + args.get(1) + ", "
                                + args.get(2) + ", " + args.get(3) + ", " + args.get(4) + ", "
                                + args.get(5) + "))";
                    }
                }
                case "optionInput" -> {
                    if (args.size() == 3) {
                        return dialogs + ".DialogInput.SingleOption(" + dialogs + ".DialogInputSingleOption("
                                + text + ".text(" + args.get(0) + "), " + args.get(1)
                                + ".map { " + text + ".text(it) }, (" + args.get(2) + ").toUInt()))";
                    }
                }
                case "openUrl" -> {
                    if (args.size() == 1) {
                        return dialogs + ".Action.OpenUrl(" + args.get(0) + ")";
                    }
                }
                case "customClick" -> {
                    if (args.size() == 1) {
                        return dialogs + ".Action.CustomClick(" + dialogs + ".CustomClickAction("
                                + args.get(0) + ", null))";
                    }
                    if (args.size() == 2) {
                        return dialogs + ".Action.CustomClick(" + dialogs + ".CustomClickAction("
                                + args.get(0) + ", " + args.get(1) + "?.map { it.toUByte() }))";
                    }
                }
                case "button" -> {
                    if (args.size() == 2) {
                        return dialogs + ".ActionButton(" + args.get(0) + ", null, null, " + args.get(1) + ")";
                    }
                    if (args.size() == 4) {
                        return dialogs + ".ActionButton(" + args.get(0) + ", " + args.get(1)
                                + ", " + args.get(2) + "?.toUInt(), " + args.get(3) + ")";
                    }
                }
                case "builtInLink" -> {
                    if (args.size() == 2) {
                        return dialogs + ".Link(" + dialogs + ".LinkLabel.BuiltIn(" + args.get(0)
                                + "), " + args.get(1) + ")";
                    }
                }
                case "customLink" -> {
                    if (args.size() == 2) {
                        return dialogs + ".Link(" + dialogs + ".LinkLabel.Custom(" + args.get(0)
                                + "), " + args.get(1) + ")";
                    }
                }
                default -> {
                }
            }
        }

        if (target == Target.PUMPKIN && "Recipe".equals(scopeSimple)) {
            String recipe = imports.ref("pumpkin.Recipe");
            switch (name) {
                case "item" -> {
                    if (args.size() == 1) {
                        return recipe + ".Ingredient.Item(" + args.get(0) + ")";
                    }
                }
                case "tag" -> {
                    if (args.size() == 1) {
                        return recipe + ".Ingredient.Tag(" + args.get(0) + ")";
                    }
                }
                case "oneOf" -> {
                    if (args.size() == 1) {
                        return recipe + ".Ingredient.OneOf(" + args.get(0) + ")";
                    }
                }
                default -> {
                }
            }
        }
        if (target == Target.PUMPKIN && "Datapack".equals(scopeSimple)) {
            String datapack = imports.ref("pumpkin.Datapack");
            switch (name) {
                case "first" -> {
                    if (args.isEmpty()) {
                        return datapack + ".EnablePosition.First";
                    }
                }
                case "last" -> {
                    if (args.isEmpty()) {
                        return datapack + ".EnablePosition.Last";
                    }
                }
                case "before" -> {
                    if (args.size() == 1) {
                        return datapack + ".EnablePosition.Before(" + args.get(0) + ")";
                    }
                }
                case "after" -> {
                    if (args.size() == 1) {
                        return datapack + ".EnablePosition.After(" + args.get(0) + ")";
                    }
                }
                default -> {
                }
            }
        }

        if (call.getScope().isPresent()
                && call.getScope().get() instanceof MethodCallExpr inventoryCall
                && inventoryCall.getNameAsString().equals("getInventory")) {
            switch (name) {
                case "getItem" -> {
                    if (args.size() == 1) {
                        return scopeText + ".getItem((" + args.get(0) + ").toUInt())";
                    }
                }
                case "setItem" -> {
                    if (args.size() == 2) {
                        return scopeText + ".setItem((" + args.get(0) + ").toUInt(), " + args.get(1) + ")";
                    }
                }
                case "getSize" -> { return scopeText + ".getSize().toInt()"; }
                default -> {
                }
            }
        }
        if (receiverType == null) {
            return null;
        }
        String javaName = receiverType.javaName();

        if (javaName.startsWith("pumpkin.Player.Player")) {
            switch (name) {
                case "sendMessage", "send" -> {
                    if (args.size() != 1) {
                        return null;
                    }
                    String message = args.get(0);
                    if (oracle.infer(call.getArgument(0)).isString()
                            || call.getArgument(0) instanceof StringLiteralExpr) {
                        message = imports.ref("pumpkin.Text.TextComponent") + ".text(" + message + ")";
                    }
                    return scopeText + ".sendSystemMessage(" + message + ", false)";
                }
                case "sendSystemMessage" -> {
                    if (args.size() == 1) {
                        return scopeText + ".sendSystemMessage(" + args.get(0) + ", false)";
                    }
                }
                case "teleport" -> {
                    if (args.size() == 1) {
                        return scopeText + ".teleport(" + args.get(0) + ", null, null, "
                                + scopeText + ".getWorld())";
                    }
                }
                case "getPing" -> {
                    if (args.isEmpty()) {
                        return scopeText + ".getPing().toInt()";
                    }
                }
                default -> {
                }
            }
        }

        if (javaName.startsWith("pumpkin.Server.Server")) {
            switch (name) {
                case "broadcastMessage" -> {
                    if (args.size() == 1) {
                        String message = oracle.infer(call.getArgument(0)).isString()
                                ? args.get(0)
                                : args.get(0) + ".getText()";
                        return scopeText + ".broadcast(" + message + ")";
                    }
                }
                case "getPlayer" -> {
                    if (args.size() == 1) {
                        return scopeText + ".getPlayerByName(" + args.get(0) + ")";
                    }
                }
                case "getPlayers" -> {
                    if (args.isEmpty()) {
                        return scopeText + ".getAllPlayers()";
                    }
                }
                case "getPlayerCount" -> {
                    if (args.isEmpty()) {
                        return scopeText + ".getPlayerCount().toInt()";
                    }
                }
                case "getWorld" -> {
                    if (args.size() == 1) {
                        return scopeText + ".getWorldByName(" + args.get(0) + ")";
                    }
                }
                default -> {
                }
            }
        }

        if (javaName.startsWith("pumpkin.Event.Event.")) {
            if (name.equals("getValue") && args.isEmpty()) {
                return scopeText + ".value";
            }
            if (args.isEmpty() && name.startsWith("get") && name.length() > 3) {
                return scopeText + ".value." + propertyName(name.substring(3));
            }
            if (args.isEmpty() && name.startsWith("is") && name.length() > 2) {
                return scopeText + ".value." + propertyName(name.substring(2));
            }
            if (args.size() == 1 && name.startsWith("set") && name.length() > 3) {
                return scopeText + ".value." + propertyName(name.substring(3)) + " = " + args.get(0);
            }
        }

        if (javaName.startsWith("pumpkin.Event.") && javaName.endsWith("EventData")) {
            if (args.isEmpty() && name.startsWith("get") && name.length() > 3) {
                return scopeText + "." + propertyName(name.substring(3));
            }
            if (args.isEmpty() && name.startsWith("is") && name.length() > 2) {
                return scopeText + "." + propertyName(name.substring(2));
            }
            if (args.size() == 1 && name.startsWith("set") && name.length() > 3) {
                return scopeText + "." + propertyName(name.substring(3)) + " = " + args.get(0);
            }
        }

        if (call.getScope().isPresent()
                && call.getScope().get() instanceof MethodCallExpr inventoryCall
                && inventoryCall.getNameAsString().equals("getInventory")) {
            switch (name) {
                case "getItem" -> {
                    if (args.size() == 1) {
                        return scopeText + ".getItem((" + args.get(0) + ").toUInt())";
                    }
                }
                case "setItem" -> {
                    if (args.size() == 2) {
                        return scopeText + ".setItem((" + args.get(0) + ").toUInt(), " + args.get(1) + ")";
                    }
                }
                case "getSize" -> { return scopeText + ".getSize().toInt()"; }
                default -> {
                }
            }
        }

        if (javaName.startsWith("pumpkin.Gui.Gui")) {
            switch (name) {
                case "getSize" -> { return scopeText + ".getSize().toInt()"; }
                case "setItem" -> {
                    if (args.size() == 2) {
                        return scopeText + ".setItem((" + args.get(0) + ").toUInt(), " + args.get(1) + ")";
                    }
                }
                case "getItem" -> {
                    if (args.size() == 1) {
                        return scopeText + ".getItem((" + args.get(0) + ").toUInt())";
                    }
                }
                default -> {
                }
            }
        }

        if (javaName.startsWith("pumpkin.Inventory.Inventory")
                || javaName.startsWith("pumpkin.Inventory.PlayerInventory")) {
            switch (name) {
                case "getSize" -> { return scopeText + ".getSize().toInt()"; }
                case "getItem" -> {
                    if (args.size() == 1) {
                        return scopeText + ".getItem((" + args.get(0) + ").toUInt())";
                    }
                }
                case "setItem" -> {
                    if (args.size() == 2) {
                        return scopeText + ".setItem((" + args.get(0) + ").toUInt(), " + args.get(1) + ")";
                    }
                }
                case "removeItem" -> {
                    if (args.size() == 1) {
                        return scopeText + ".removeItem((" + args.get(0) + ").toUInt())";
                    }
                }
                case "getSelectedSlot" -> { return scopeText + ".getSelectedSlot().toInt()"; }
                case "setSelectedSlot" -> {
                    if (args.size() == 1) {
                        return scopeText + ".setSelectedSlot((" + args.get(0) + ").toUByte())";
                    }
                }
                default -> {
                }
            }
        }

        if (javaName.startsWith("pumpkin.ItemStack.ItemStack")) {
            switch (name) {
                case "getCount" -> { return scopeText + ".getCount().toInt()"; }
                default -> {
                }
            }
        }

        if (javaName.startsWith("pumpkin.Position") || javaName.startsWith("pumpkin.Common.Position")) {
            if (args.isEmpty()) {
                switch (name) {
                    case "x", "getX" -> { return scopeText + ".first"; }
                    case "y", "getY" -> { return scopeText + ".second"; }
                    case "z", "getZ" -> { return scopeText + ".third"; }
                    default -> {
                    }
                }
            }
        }

        if (javaName.startsWith("pumpkin.World.Entity")) {
            switch (name) {
                case "teleport" -> {
                    if (args.size() == 2) {
                        return scopeText + ".teleport(" + args.get(0) + ", " + args.get(1) + ")";
                    }
                }
                case "remove" -> {
                    if (args.isEmpty()) {
                        return scopeText + ".remove()";
                    }
                }
                default -> {
                }
            }
        }

        if (javaName.startsWith("pumpkin.World.World")) {
            switch (name) {
                case "getTime" -> { return scopeText + ".getTimeOfDay().toLong()"; }
                case "setTime" -> {
                    if (args.size() == 1) {
                        return scopeText + ".setTimeOfDay((" + args.get(0) + ").toULong())";
                    }
                }
                case "setStorm" -> {
                    if (args.size() == 1) {
                        return scopeText + ".setRaining(" + args.get(0) + ")";
                    }
                }
                case "getBlockStateId" -> {
                    if (args.size() == 1) {
                        return scopeText + ".getBlockStateId(" + args.get(0) + ").toInt()";
                    }
                }
                case "setBlockStateId" -> {
                    if (args.size() == 2) {
                        return scopeText + ".setBlockState(" + args.get(0)
                                + ", (" + args.get(1) + ").toUShort(), pumpkin.World.BlockFlags())";
                    }
                }
                case "setBlockByName" -> {
                    if (args.size() == 2) {
                        return scopeText + ".setBlockByName(" + args.get(0) + ", "
                                + args.get(1) + ", pumpkin.World.BlockFlags())";
                    }
                }
                case "spawnParticle" -> {
                    if (args.size() == 5) {
                        return scopeText + ".spawnParticle(" + args.get(0) + ", " + args.get(1)
                                + ", " + args.get(2) + ", (" + args.get(4) + ").toFloat(), "
                                + args.get(3) + ")";
                    }
                }
                case "playSound" -> {
                    if (args.size() == 5) {
                        return scopeText + ".playSound(" + args.get(0) + ", " + args.get(1)
                                + ", " + args.get(2) + ", " + args.get(3) + ", " + args.get(4) + ")";
                    }
                }
                case "playCustomSound" -> {
                    if (args.size() == 5) {
                        return scopeText + ".playCustomSound(" + args.get(0) + ", " + args.get(1)
                                + ", " + args.get(2) + ", " + args.get(3) + ", " + args.get(4) + ")";
                    }
                }
                case "createExplosion" -> {
                    if (args.size() == 3) {
                        return scopeText + ".createExplosion(" + args.get(0) + ", " + args.get(1)
                                + ", " + args.get(2) + ", pumpkin.World.ExplosionInteraction.TNT)";
                    }
                }
                case "getBlockByName" -> {
                    if (args.size() == 1) {
                        return imports.ref("pumpkin.World") + ".getBlockByName(" + args.get(0) + ")";
                    }
                }
                case "getBlockById" -> {
                    if (args.size() == 1) {
                        return imports.ref("pumpkin.World") + ".getBlockById((" + args.get(0) + ").toUShort())";
                    }
                }
                case "getAllBlocks" -> {
                    if (args.isEmpty()) {
                        return imports.ref("pumpkin.World") + ".getAllBlocks()";
                    }
                }
                case "getStatesForBlock" -> {
                    if (args.size() == 1) {
                        return imports.ref("pumpkin.World") + ".getStatesForBlock(" + args.get(0) + ")";
                    }
                }
                case "getStatesForBlockId" -> {
                    if (args.size() == 1) {
                        return imports.ref("pumpkin.World") + ".getStatesForBlockId((" + args.get(0) + ").toUShort())";
                    }
                }
                default -> {
                }
            }
        }

        if (javaName.equals("pumpkin.World.Mob")) {
            switch (name) {
                case "addCustomAiGoal" -> {
                    if (args.size() == 2) {
                        return scopeText + ".addCustomAiGoal((" + args.get(0) + ").toUByte(), ("
                                + args.get(1) + ").toUInt())";
                    }
                }
                case "getHealth" -> { return scopeText + ".asLiving().getHealth()"; }
                case "setHealth" -> {
                    if (args.size() == 1) {
                        return scopeText + ".asLiving().setHealth(" + args.get(0) + ")";
                    }
                }
                case "getMaxHealth" -> { return scopeText + ".asLiving().getMaxHealth()"; }
                case "setMaxHealth" -> {
                    if (args.size() == 1) {
                        return scopeText + ".asLiving().setMaxHealth(" + args.get(0) + ")";
                    }
                }
                default -> {
                }
            }
        }

        if (javaName.startsWith("pumpkin.Display.BlockDisplayEntity")) {
            switch (name) {
                case "getBlockStateId" -> { return scopeText + ".getBlockStateId().toInt()"; }
                case "setBlockStateId" -> {
                    if (args.size() == 1) {
                        return scopeText + ".setBlockStateId((" + args.get(0) + ").toUShort())";
                    }
                }
                default -> {
                }
            }
        }

        if (javaName.startsWith("pumpkin.Display.TextDisplayEntity")) {
            switch (name) {
                case "getTextOpacity" -> { return scopeText + ".getTextOpacity().toInt()"; }
                case "setTextOpacity" -> {
                    if (args.size() == 1) {
                        return scopeText + ".setTextOpacity((" + args.get(0) + ").toByte())";
                    }
                }
                default -> {
                }
            }
        }
        return null;
    }

    private boolean isIntExpression(Expression expression) {
        KType type = oracle.infer(expression);
        return "Int".equals(type.base()) && !type.isNullable();
    }

    private String mapMathCall(String name, List<String> args, MethodCallExpr call) {
        imports.wildcard("kotlin.math");
        return switch (name) {
            case "abs" -> "abs(" + String.join(", ", args) + ")";
            case "max" -> "maxOf(" + String.join(", ", args) + ")";
            case "min" -> "minOf(" + String.join(", ", args) + ")";
            case "pow" -> args.size() == 2 ? args.get(0) + ".pow(" + args.get(1) + ")" : null;
            case "sqrt" -> "sqrt(" + String.join(", ", args) + ")";
            case "cbrt" -> "cbrt(" + String.join(", ", args) + ")";
            case "floor" -> "floor(" + String.join(", ", args) + ")";
            case "ceil" -> "ceil(" + String.join(", ", args) + ")";
            case "round" -> "round(" + String.join(", ", args) + ")";
            case "sin", "cos", "tan", "asin", "acos", "atan", "atan2", "exp", "ln", "log10", "log",
                 "sinh", "cosh", "tanh", "hypot" -> name + "(" + String.join(", ", args) + ")";
            case "random" -> imports.ref("kotlin.random.Random") + ".nextDouble()";
            case "floorDiv" -> args.size() == 2 ? "(" + args.get(0) + " / " + args.get(1) + ")" : null;
            case "floorMod" -> args.size() == 2
                    ? "(((" + args.get(0) + " % " + args.get(1) + ") + " + args.get(1) + ") % " + args.get(1) + ")"
                    : null;
            default -> null;
        };
    }

    private String mapObjectsCall(String name, List<String> args, MethodCallExpr call) {
        return switch (name) {
            case "requireNonNull" -> args.size() == 1
                    ? "requireNotNull(" + args.get(0) + ")"
                    : "requireNotNull(" + args.get(0) + ") { " + args.get(1) + " }";
            case "requireNonNullElse" -> args.size() == 2 ? args.get(0) + " ?: " + args.get(1) : null;
            case "requireNonNullElseGet" -> args.size() == 2 ? args.get(0) + " ?: " + args.get(1) + "()" : null;
            case "equals" -> args.size() == 2 ? args.get(0) + " == " + args.get(1) : null;
            case "deepEquals" -> args.size() == 2 ? args.get(0) + " contentDeepEquals " + args.get(1) : null;
            case "hash" -> "listOf(" + String.join(", ", args) + ").hashCode()";
            case "hashCode" -> args.size() == 1 ? args.get(0) + ".hashCode()" : null;
            case "toString" -> args.size() == 1 ? args.get(0) + ".toString()" : args.get(0) + "?.toString() ?: " + args.get(1);
            case "isNull" -> args.size() == 1 ? args.get(0) + " == null" : null;
            case "nonNull" -> args.size() == 1 ? args.get(0) + " != null" : null;
            default -> null;
        };
    }

    private String mapCollectionsCall(String name, List<String> args, MethodCallExpr call) {
        return switch (name) {
            case "emptyList" -> "mutableListOf()";
            case "emptySet" -> "mutableSetOf()";
            case "emptyMap" -> "mutableMapOf()";
            case "singletonList" -> args.size() == 1 ? "mutableListOf(" + args.get(0) + ")" : null;
            case "singleton" -> args.size() == 1 ? "mutableSetOf(" + args.get(0) + ")" : null;
            case "singletonMap" -> args.size() == 2 ? "mutableMapOf(" + args.get(0) + " to " + args.get(1) + ")" : null;
            case "unmodifiableList", "unmodifiableSet", "unmodifiableCollection" ->
                    args.size() == 1 ? args.get(0) + ".toList()" : null;
            case "unmodifiableMap" -> args.size() == 1 ? args.get(0) + ".toMap()" : null;
            case "sort" -> args.size() == 1 ? args.get(0) + ".sort()" : null;
            case "reverse" -> args.size() == 1 ? args.get(0) + ".reverse()" : null;
            case "shuffle" -> args.size() == 1 ? args.get(0) + ".shuffle()" : null;
            case "max" -> args.size() == 1 ? args.get(0) + ".maxOrNull()" : null;
            case "min" -> args.size() == 1 ? args.get(0) + ".minOrNull()" : null;
            case "frequency" -> args.size() == 2 ? args.get(0) + ".count { it == " + args.get(1) + " }" : null;
            case "swap" -> args.size() == 3 ? "java.util.Collections.swap(" + String.join(", ", args) + ")" : null;
            default -> null;
        };
    }

    private String mapArraysCall(String name, List<String> args, MethodCallExpr call) {
        return switch (name) {
            case "asList" -> "mutableListOf(" + String.join(", ", args) + ")";
            case "toString" -> args.size() == 1 ? args.get(0) + ".contentToString()" : null;
            case "deepToString" -> args.size() == 1 ? args.get(0) + ".contentDeepToString()" : null;
            case "equals" -> args.size() == 2 ? args.get(0) + ".contentEquals(" + args.get(1) + ")" : null;
            case "deepEquals" -> args.size() == 2 ? args.get(0) + ".contentDeepEquals(" + args.get(1) + ")" : null;
            case "sort" -> args.size() == 1 ? args.get(0) + ".sort()" : null;
            case "fill" -> args.size() == 2 ? args.get(0) + ".fill(" + args.get(1) + ")" : null;
            case "copyOf" -> args.size() == 2 ? args.get(0) + ".copyOf(" + args.get(1) + ")" : null;
            case "copyOfRange" -> args.size() == 3
                    ? args.get(0) + ".copyOfRange(" + args.get(1) + ", " + args.get(2) + ")" : null;
            case "stream" -> args.size() == 1 ? args.get(0) + ".asSequence()" : null;
            default -> null;
        };
    }

    private String mapBoxedCall(String scope, String name, List<String> args, MethodCallExpr call) {
        return switch (name) {
            case "parseInt", "parseLong", "parseShort", "parseByte", "parseDouble", "parseFloat" -> {
                if (args.size() == 1) {
                    yield args.get(0) + "." + switch (scope) {
                        case "Integer" -> "toInt()";
                        case "Long" -> "toLong()";
                        case "Short" -> "toShort()";
                        case "Byte" -> "toByte()";
                        case "Double" -> "toDouble()";
                        case "Float" -> "toFloat()";
                        default -> "toInt()";
                    };
                }
                yield null;
            }
            case "valueOf" -> {
                if (args.size() == 1) {
                    yield switch (scope) {
                        case "Integer" -> args.get(0) + ".toInt()";
                        case "Long" -> args.get(0) + ".toLong()";
                        case "Short" -> args.get(0) + ".toShort()";
                        case "Byte" -> args.get(0) + ".toByte()";
                        case "Double" -> args.get(0) + ".toDouble()";
                        case "Float" -> args.get(0) + ".toFloat()";
                        case "Boolean" -> args.get(0) + ".toBoolean()";
                        default -> args.get(0);
                    };
                }
                yield null;
            }
            case "toString" -> args.size() == 1 ? args.get(0) + ".toString()" : null;
            case "compare" -> args.size() == 2 ? args.get(0) + ".compareTo(" + args.get(1) + ")" : null;
            case "isDigit" -> args.size() == 1 ? args.get(0) + ".isDigit()" : null;
            case "isLetter" -> args.size() == 1 ? args.get(0) + ".isLetter()" : null;
            case "isWhitespace" -> args.size() == 1 ? args.get(0) + ".isWhitespace()" : null;
            case "toUpperCase" -> args.size() == 1 ? args.get(0) + ".uppercaseChar()" : null;
            case "toLowerCase" -> args.size() == 1 ? args.get(0) + ".lowercaseChar()" : null;
            case "booleanValue" -> args.isEmpty() ? "this" : null;
            default -> null;
        };
    }

    private String propertyCall(MethodCallExpr call, KType receiverType, String name, List<String> args) {
        if (call.getScope().isEmpty()) {
            return null;
        }
        Expression receiver = call.getScope().orElseThrow();
        if (receiverType != null && "plugin.PluginMetadata".equals(receiverType.javaName())
                && name.equals("getName") && args.isEmpty()) {
            return emitExpression(receiver, null) + ".name";
        }
        if (receiver instanceof NameExpr receiverName
                && !receiverName.getNameAsString().isEmpty()
                && Character.isUpperCase(receiverName.getNameAsString().charAt(0))) {
            // Likely a static call such as Foo.getBar(); do not turn it into Foo.bar.
            return null;
        }
        if (receiverType == null || receiverType.isUnknown()) {
            return null;
        }
        if (receiverType.isCollection() || receiverType.isMap()
                || receiverType.isString() || receiverType.isArray()) {
            return null;
        }
        if (receiverType != null && (receiverType.javaName().startsWith("pumpkin.")
                || receiverType.javaName().startsWith("plugin."))) {
            // Pumpkin's generated bindings expose WIT functions; do not invent properties.
            return null;
        }
        if (name.equals("get") || name.equals("set")) {
            return null;
        }
        Matcher getter = GETTER.matcher(name);
        if (getter.matches() && args.isEmpty() && getter.group(1).equals("get")) {
            String property = propertyName(getter.group(2));
            return emitExpression(receiver, null) + "." + name(property);
        }
        Matcher setter = SETTER.matcher(name);
        if (setter.matches() && args.size() == 1) {
            String property = propertyName(setter.group(1));
            return emitExpression(receiver, null) + "." + name(property) + " = " + args.get(0);
        }
        return null;
    }

    private String propertyName(String suffix) {
        if (suffix.length() > 1 && Character.isUpperCase(suffix.charAt(1))) {
            return suffix;
        }
        return NameUtils.decapitalize(suffix);
    }

    private String emitObjectCreation(ObjectCreationExpr creation) {
        String typeName = creation.getType().getNameAsString();
        String mappedType = types.map(creation.getType()).render();
        List<String> args = creation.getArguments().stream().map(value -> emitExpression(value, null)).toList();
        String argsText = String.join(", ", args);

        if (creation.getAnonymousClassBody().isPresent()) {
            KotlinWriter savedOut = out;
            CommentRenderer savedComments = comments;
            boolean anonymousInterface = creation.getType() instanceof ClassOrInterfaceType classType
                    && types.isInterface(classType);
            String rawType = creation.getType() instanceof ClassOrInterfaceType classType
                    ? types.rawClassName(classType)
                    : mappedType;
            KotlinWriter nested = new KotlinWriter("    ", out.indentLevel());
            out = nested;
            comments = new CommentRenderer(out);
            boolean savedInResultBlock = inResultBlock;
            String savedLambdaLabel = currentLambdaLabel;
            UnaryOperator<String> savedWrapper = currentLambdaReturnWrapper;
            inResultBlock = false;
            currentLambdaLabel = null;
            currentLambdaReturnWrapper = null;
            out.line("object : " + rawType + (anonymousInterface ? "" : "(" + argsText + ")") + " {");
            out.indent();
            for (BodyDeclaration<?> member : creation.getAnonymousClassBody().get()) {
                if (member instanceof MethodDeclaration method) {
                    emitMethod(method, false, false);
                } else if (member instanceof FieldDeclaration field) {
                    emitField(field, false, false);
                }
            }
            out.dedent();
            out.line("}");
            inResultBlock = savedInResultBlock;
            currentLambdaLabel = savedLambdaLabel;
            currentLambdaReturnWrapper = savedWrapper;
            String rendered = out.toString();
            out = savedOut;
            comments = savedComments;
            return rendered.stripLeading();
        }

        if (typeName.equals("ArrayList") || typeName.equals("LinkedList") || typeName.equals("Vector")) {
            if (args.size() == 1 && oracle.infer(creation.getArgument(0)).isCollection()) {
                return args.get(0) + ".toMutableList()";
            }
            if (!args.isEmpty() && !(creation.getArgument(0) instanceof IntegerLiteralExpr)) {
                diagnostics.info("J2KT-COLLECTION-001", "Collection constructor argument was interpreted as a capacity and dropped.", creation);
            }
            return "mutableListOf(" + (args.size() == 1 && creation.getArgument(0) instanceof IntegerLiteralExpr ? "" : argsText) + ")";
        }
        if (typeName.equals("HashSet") || typeName.equals("LinkedHashSet") || typeName.equals("TreeSet")) {
            if (args.size() == 1 && oracle.infer(creation.getArgument(0)).isCollection()) {
                return args.get(0) + ".toMutableSet()";
            }
            return "mutableSetOf(" + argsText + ")";
        }
        if (typeName.equals("HashMap") || typeName.equals("LinkedHashMap") || typeName.equals("TreeMap")) {
            if (args.size() == 1 && oracle.infer(creation.getArgument(0)).isMap()) {
                return args.get(0) + ".toMutableMap()";
            }
            return "mutableMapOf(" + argsText + ")";
        }
        if (typeName.equals("StringBuffer")) {
            return "StringBuilder(" + argsText + ")";
        }
        if (typeName.equals("Object")) {
            return "Any()";
        }
        if (typeName.equals("String") && args.size() == 1) {
            return "String(" + argsText + ")";
        }
        if (typeName.equals("CommandSuggestions") && args.size() == 3) {
            return mappedType + "((" + args.get(0) + ").toUInt(), ("
                    + args.get(1) + ").toUInt(), " + args.get(2) + ")";
        }
        if (typeName.equals("CookingRecipe") && args.size() == 6) {
            return mappedType + "(" + args.get(0) + ", " + args.get(1) + ", " + args.get(2)
                    + ", (" + args.get(3) + ").toUInt(), " + args.get(4) + ", " + args.get(5) + ")";
        }
        if (typeName.equals("CustomEnchantment") && args.size() == 8) {
            return mappedType + "(" + args.get(0) + ", " + args.get(1)
                    + ", (" + args.get(2) + ").toUInt(), (" + args.get(3) + ").toUInt(), "
                    + args.get(4) + ", (" + args.get(5) + ").toUInt(), "
                    + args.get(6) + ", " + args.get(7) + ")";
        }
        if (typeName.equals("ShapedRecipe") && args.size() == 6) {
            return mappedType + "(" + args.get(0) + ", " + args.get(1) + ".toList(), "
                    + args.get(2) + ", " + args.get(3) + ", " + args.get(4) + ", " + args.get(5) + ")";
        }
        if (typeName.equals("ItemStack") && args.size() == 2) {
            return mappedType + "(" + args.get(0) + ", (" + args.get(1) + ").toUByte())";
        }
        if (typeName.equals("CPlayDisconnect") && args.size() == 1) {
            String packets = imports.ref("pumpkin.JavaPackets");
            return packets + ".ClientboundPacket.CPlayDisconnect(" + packets
                    + ".CPlayDisconnect(" + args.get(0) + "))";
        }
        if (typeName.equals("CKeepAlive") && args.size() == 1) {
            String packets = imports.ref("pumpkin.JavaPackets");
            return packets + ".ClientboundPacket.CKeepAlive(" + packets
                    + ".CKeepAlive(" + args.get(0) + "))";
        }
        if (typeName.equals("CGameEvent") && args.size() == 2) {
            String packets = imports.ref("pumpkin.JavaPackets");
            return packets + ".ClientboundPacket.CGameEvent(" + packets
                    + ".CGameEvent((" + args.get(0) + ").toUByte(), " + args.get(1) + "))";
        }
        if (typeName.equals("CTransfer") && args.size() == 2) {
            String packets = imports.ref("pumpkin.JavaPackets");
            return packets + ".ClientboundPacket.CTransfer(" + packets
                    + ".CTransfer(" + args.get(0) + ", " + args.get(1) + "))";
        }
        if (typeName.equals("CDisconnect") && args.size() == 4) {
            String packets = imports.ref("pumpkin.BedrockPackets");
            return packets + ".ClientboundPacket.CDisconnect(" + packets
                    + ".CDisconnect(" + args.get(0) + ", " + args.get(1) + ", "
                    + args.get(2) + ", " + args.get(3) + "))";
        }
        if (typeName.equals("CSetTime") && args.size() == 1) {
            String packets = imports.ref("pumpkin.BedrockPackets");
            return packets + ".ClientboundPacket.CSetTime(" + packets
                    + ".CSetTime(" + args.get(0) + "))";
        }
        if (typeName.equals("CModalFormRequest") && args.size() == 2) {
            String packets = imports.ref("pumpkin.BedrockPackets");
            return packets + ".ClientboundPacket.CModalFormRequest(" + packets
                    + ".CModalFormRequest(" + args.get(0) + ", " + args.get(1) + "))";
        }
        if (typeName.equals("CCustomPayload") && args.size() == 2) {
            String packets = imports.ref("pumpkin.JavaPackets");
            return packets + ".ClientboundPacket.CCustomPayload(" + packets
                    + ".CCustomPayload(" + args.get(0) + ", "
                    + args.get(1) + ".map { it.toUByte() }))";
        }
        if (typeName.equals("Unknown")) {
            return imports.ref("pumpkin.BedrockPackets") + ".ClientboundPacket.Unknown";
        }

        String prefix = creation.getScope().map(scope -> emitExpression(scope, null) + ".").orElse("");
        return prefix + mappedType + "(" + argsText + ")";
    }

    private String emitArrayCreation(ArrayCreationExpr creation) {
        KType type = oracle.infer(creation);
        int levels = creation.getLevels().size();
        if (creation.getInitializer().isPresent()) {
            return emitArrayInitializer(creation.getInitializer().get(), type);
        }
        List<String> dimensions = new ArrayList<>();
        for (var level : creation.getLevels()) {
            dimensions.add(level.getDimension().map(value -> emitExpression(value, KType.INT)).orElse("0"));
        }
        KType component = types.map(creation.getElementType());
        if (levels == 1 && (component.isKotlinPrimitive())) {
            return switch (component.base()) {
                case "Int" -> "IntArray(" + dimensions.get(0) + ")";
                case "Long" -> "LongArray(" + dimensions.get(0) + ")";
                case "Short" -> "ShortArray(" + dimensions.get(0) + ")";
                case "Byte" -> "ByteArray(" + dimensions.get(0) + ")";
                case "Float" -> "FloatArray(" + dimensions.get(0) + ")";
                case "Double" -> "DoubleArray(" + dimensions.get(0) + ")";
                case "Boolean" -> "BooleanArray(" + dimensions.get(0) + ")";
                case "Char" -> "CharArray(" + dimensions.get(0) + ")";
                default -> "arrayOfNulls<" + component.render() + ">(" + dimensions.get(0) + ")";
            };
        }
        if (levels == 1) {
            return "arrayOfNulls<" + component.render() + ">(" + dimensions.get(0) + ")";
        }
        // Multi-dimensional: Array(n) { ... } from the inside out.
        String innermost;
        if (component.isKotlinPrimitive()) {
            innermost = switch (component.base()) {
                case "Int" -> "IntArray";
                case "Long" -> "LongArray";
                case "Short" -> "ShortArray";
                case "Byte" -> "ByteArray";
                case "Float" -> "FloatArray";
                case "Double" -> "DoubleArray";
                case "Boolean" -> "BooleanArray";
                case "Char" -> "CharArray";
                default -> "arrayOfNulls<" + component.render() + ">";
            } + "(" + dimensions.get(levels - 1) + ")";
        } else {
            innermost = "arrayOfNulls<" + component.render() + ">(" + dimensions.get(levels - 1) + ")";
        }
        String result = innermost;
        for (int i = levels - 2; i >= 0; i--) {
            result = "Array(" + dimensions.get(i) + ") { " + result + " }";
        }
        return result;
    }

    private String emitArrayInitializer(ArrayInitializerExpr initializer, KType expected) {
        List<String> values = initializer.getValues().stream()
                .map(value -> emitExpression(value, expected == null ? null : expected.element()))
                .toList();
        String joined = String.join(", ", values);
        if (expected != null && expected.isArray() && expected.element() != null
                && expected.element().isKotlinPrimitive()) {
            return switch (expected.element().base()) {
                case "Int" -> "intArrayOf(" + joined + ")";
                case "Long" -> "longArrayOf(" + joined + ")";
                case "Short" -> "shortArrayOf(" + joined + ")";
                case "Byte" -> "byteArrayOf(" + joined + ")";
                case "Float" -> "floatArrayOf(" + joined + ")";
                case "Double" -> "doubleArrayOf(" + joined + ")";
                case "Boolean" -> "booleanArrayOf(" + joined + ")";
                case "Char" -> "charArrayOf(" + joined + ")";
                default -> "arrayOf(" + joined + ")";
            };
        }
        return "arrayOf(" + joined + ")";
    }

    private String emitAssign(AssignExpr assign) {
        String target = emitExpression(assign.getTarget(), null);
        String value = emitExpression(assign.getValue(), null);
        KType targetType = oracle.infer(assign.getTarget());
        KType valueType = oracle.infer(assign.getValue());
        if (assign.getValue() instanceof MethodCallExpr valueCall
                && (valueCall.getNameAsString().equals("getValue")
                || valueCall.getNameAsString().equals("getKey"))) {
            valueType = valueType.asNullable();
        }
        if (assign.getOperator() == AssignExpr.Operator.ASSIGN) {
            return target + " = " + coerce(value, valueType, targetType);
        }
        if (assign.getOperator() == AssignExpr.Operator.PLUS && targetType.isString()) {
            return target + " += " + value;
        }
        if (assign.getOperator() == AssignExpr.Operator.BINARY_AND
                || assign.getOperator() == AssignExpr.Operator.BINARY_OR
                || assign.getOperator() == AssignExpr.Operator.XOR) {
            String infix = switch (assign.getOperator()) {
                case BINARY_AND -> "and";
                case BINARY_OR -> "or";
                default -> "xor";
            };
            return target + " = (" + target + " " + infix + " " + value + ")";
        }
        if (assign.getOperator() == AssignExpr.Operator.LEFT_SHIFT
                || assign.getOperator() == AssignExpr.Operator.SIGNED_RIGHT_SHIFT
                || assign.getOperator() == AssignExpr.Operator.UNSIGNED_RIGHT_SHIFT) {
            String infix = switch (assign.getOperator()) {
                case LEFT_SHIFT -> "shl";
                case SIGNED_RIGHT_SHIFT -> "shr";
                default -> "ushr";
            };
            return target + " = (" + target + " " + infix + " " + value + ")";
        }
        if (targetType.isNumeric() && valueType.isNullable()
                && (assign.getOperator() == AssignExpr.Operator.PLUS
                || assign.getOperator() == AssignExpr.Operator.MINUS
                || assign.getOperator() == AssignExpr.Operator.MULTIPLY
                || assign.getOperator() == AssignExpr.Operator.DIVIDE
                || assign.getOperator() == AssignExpr.Operator.REMAINDER)) {
            value = value + "!!";
        }
        if (targetType.isNumeric() && valueType.isNumeric()
                && (assign.getOperator() == AssignExpr.Operator.PLUS
                || assign.getOperator() == AssignExpr.Operator.MINUS
                || assign.getOperator() == AssignExpr.Operator.MULTIPLY
                || assign.getOperator() == AssignExpr.Operator.DIVIDE
                || assign.getOperator() == AssignExpr.Operator.REMAINDER)) {
            KType promoted = promoteNumeric(targetType, valueType);
            String op = binaryOperator(assign.getOperator());
            if (targetType.category() == KType.Category.CHAR) {
                if (valueType.category() == KType.Category.CHAR) {
                    return target + " = (" + target + ".code " + op + " " + value + ".code).toChar()";
                }
                return target + " = (" + target + " " + op + " " + value + ")";
            }
            if (targetType.base().equals("Byte") || targetType.base().equals("Short")
                    || !targetType.base().equals(promoted.base())) {
                String combined = target + " " + op + " " + value;
                return target + " = " + coerce(combined, promoted, targetType);
            }
        }
        return target + " " + operator(assign.getOperator()) + " " + value;
    }

    private String binaryOperator(AssignExpr.Operator operator) {
        return switch (operator) {
            case PLUS -> "+";
            case MINUS -> "-";
            case MULTIPLY -> "*";
            case DIVIDE -> "/";
            case REMAINDER -> "%";
            default -> operator.asString();
        };
    }

    private String operator(AssignExpr.Operator operator) {
        return switch (operator) {
            case ASSIGN -> "=";
            case PLUS -> "+=";
            case MINUS -> "-=";
            case MULTIPLY -> "*=";
            case DIVIDE -> "/=";
            case BINARY_AND -> "and=";
            case BINARY_OR -> "or=";
            case XOR -> "xor=";
            case REMAINDER -> "%=";
            case LEFT_SHIFT -> "shl=";
            case SIGNED_RIGHT_SHIFT -> "shr=";
            case UNSIGNED_RIGHT_SHIFT -> "ushr=";
        };
    }

    private String emitBinary(BinaryExpr binary) {
        KType leftType = oracle.infer(binary.getLeft());
        KType rightType = oracle.infer(binary.getRight());
        KType resultType = oracle.infer(binary);
        String left = emitExpression(binary.getLeft(), null);
        String right = emitExpression(binary.getRight(), null);

        BinaryExpr.Operator operator = binary.getOperator();
        if (operator == BinaryExpr.Operator.EQUALS || operator == BinaryExpr.Operator.NOT_EQUALS) {
            boolean identity = options.strictJavaEquality()
                    && leftType.isReferenceType() && rightType.isReferenceType()
                    && !(binary.getLeft() instanceof NullLiteralExpr)
                    && !(binary.getRight() instanceof NullLiteralExpr);
            String op = identity
                    ? (operator == BinaryExpr.Operator.EQUALS ? "===" : "!==")
                    : (operator == BinaryExpr.Operator.EQUALS ? "==" : "!=");
            return maybeParen(left, binary.getLeft(), 8) + " " + op + " " + maybeParen(right, binary.getRight(), 8);
        }

        if (operator == BinaryExpr.Operator.PLUS
                && !(leftType.isString() || rightType.isString())) {
            left = coerceNumericOperand(left, leftType, resultType);
            right = coerceNumericOperand(right, rightType, resultType);
        } else if (isNumericOperator(operator)) {
            left = coerceNumericOperand(left, leftType, resultType);
            right = coerceNumericOperand(right, rightType, resultType);
        }

        if (isBitwiseOperator(operator)) {
            String infix = switch (operator) {
                case BINARY_AND -> "and";
                case BINARY_OR -> "or";
                case XOR -> "xor";
                default -> throw new IllegalStateException();
            };
            if (leftType.isBoolean() && rightType.isBoolean()) {
                return "(" + left + " " + infix + " " + right + ")";
            }
            return "(" + left + " " + infix + " " + right + ")";
        }
        if (isShiftOperator(operator)) {
            String infix = switch (operator) {
                case LEFT_SHIFT -> "shl";
                case SIGNED_RIGHT_SHIFT -> "shr";
                case UNSIGNED_RIGHT_SHIFT -> "ushr";
                default -> throw new IllegalStateException();
            };
            return "(" + left + " " + infix + " " + right + ")";
        }

        String op = switch (operator) {
            case PLUS -> "+";
            case MINUS -> "-";
            case MULTIPLY -> "*";
            case DIVIDE -> "/";
            case REMAINDER -> "%";
            case LESS -> "<";
            case LESS_EQUALS -> "<=";
            case GREATER -> ">";
            case GREATER_EQUALS -> ">=";
            case OR -> "||";
            case AND -> "&&";
            default -> operator.asString();
        };
        int precedence = precedence(operator);
        return maybeParen(left, binary.getLeft(), precedence)
                + " " + op + " "
                + maybeParen(right, binary.getRight(), precedence + (isRightAssociative(operator) ? 0 : 1));
    }

    private KType promoteNumeric(KType left, KType right) {
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

    private String coerceNumericOperand(String expression, KType type, KType target) {
        if (type == null || target == null || !type.isNumeric() || !target.isNumeric()) {
            return expression;
        }
        String value = expression;
        if (type.isNullable() && !target.isNullable()) {
            value = value + "!!";
        }
        if (type.category() == KType.Category.CHAR) {
            return value + ".code";
        }
        if (!type.base().equals(target.base())) {
            return types.numericConversion("(" + value + ")", target);
        }
        return value;
    }

    private boolean isNumericOperator(BinaryExpr.Operator operator) {
        return switch (operator) {
            case PLUS, MINUS, MULTIPLY, DIVIDE, REMAINDER -> true;
            default -> false;
        };
    }

    private boolean isBitwiseOperator(BinaryExpr.Operator operator) {
        return operator == BinaryExpr.Operator.BINARY_AND
                || operator == BinaryExpr.Operator.BINARY_OR
                || operator == BinaryExpr.Operator.XOR;
    }

    private boolean isShiftOperator(BinaryExpr.Operator operator) {
        return operator == BinaryExpr.Operator.LEFT_SHIFT
                || operator == BinaryExpr.Operator.SIGNED_RIGHT_SHIFT
                || operator == BinaryExpr.Operator.UNSIGNED_RIGHT_SHIFT;
    }

    private int precedence(BinaryExpr.Operator operator) {
        return switch (operator) {
            case OR -> 3;
            case AND -> 4;
            case BINARY_OR -> 5;
            case XOR -> 6;
            case BINARY_AND -> 7;
            case EQUALS, NOT_EQUALS -> 8;
            case LESS, LESS_EQUALS, GREATER, GREATER_EQUALS -> 9;
            case LEFT_SHIFT, SIGNED_RIGHT_SHIFT, UNSIGNED_RIGHT_SHIFT -> 10;
            case PLUS, MINUS -> 11;
            case MULTIPLY, DIVIDE, REMAINDER -> 12;
        };
    }

    private boolean isRightAssociative(BinaryExpr.Operator operator) {
        return false;
    }

    private String maybeParen(String rendered, Expression original, int minimum) {
        int precedence = expressionPrecedence(original);
        if (precedence < minimum) {
            return "(" + rendered + ")";
        }
        return rendered;
    }

    private int expressionPrecedence(Expression expression) {
        if (expression instanceof AssignExpr) return 1;
        if (expression instanceof ConditionalExpr) return 2;
        if (expression instanceof BinaryExpr binary) return precedence(binary.getOperator());
        if (expression instanceof InstanceOfExpr) return 9;
        if (expression instanceof UnaryExpr || expression instanceof CastExpr) return 13;
        return 15;
    }

    private String emitUnary(UnaryExpr unary) {
        String value = emitExpression(unary.getExpression(), null);
        return switch (unary.getOperator()) {
            case PLUS -> "+" + value;
            case MINUS -> "-" + value;
            case LOGICAL_COMPLEMENT -> unary.getExpression() instanceof InstanceOfExpr
                    ? "!(" + value + ")"
                    : "!" + value;
            case BITWISE_COMPLEMENT -> "(" + value + ").inv()";
            case PREFIX_INCREMENT -> "++" + value;
            case PREFIX_DECREMENT -> "--" + value;
            case POSTFIX_INCREMENT -> value + "++";
            case POSTFIX_DECREMENT -> value + "--";
        };
    }

    private String emitLambda(LambdaExpr lambda) {
        return emitLambda(lambda, List.of(), null);
    }

    private String emitLambda(LambdaExpr lambda, List<KType> expectedParameters) {
        return emitLambda(lambda, expectedParameters, null);
    }

    private String emitLambda(LambdaExpr lambda, List<KType> expectedParameters,
                              UnaryOperator<String> returnWrapper) {
        List<String> params = new ArrayList<>();
        for (Parameter parameter : lambda.getParameters()) {
            params.add(name(parameter.getNameAsString()));
        }
        String prefix = params.isEmpty() ? "" : String.join(", ", params) + " -> ";

        scope.push();
        for (int i = 0; i < lambda.getParameters().size(); i++) {
            KType type = i < expectedParameters.size()
                    ? expectedParameters.get(i)
                    : KType.unknown("lambda parameter").asNullable();
            scope.declare(lambda.getParameter(i).getNameAsString(), type);
        }

        if (lambda.getBody() instanceof ExpressionStmt expressionStmt) {
            String body = emitExpression(expressionStmt.getExpression(), null);
            if (returnWrapper != null) {
                body = returnWrapper.apply(body);
            }
            String rendered = "{ " + prefix + body + " }";
            scope.pop();
            return rendered;
        }
        if (!(lambda.getBody() instanceof BlockStmt block)) {
            scope.pop();
            return "{ " + prefix + "Unit }";
        }

        String label = "j2ktLambda" + (++lambdaCounter);
        String previousLabel = currentLambdaLabel;
        UnaryOperator<String> previousWrapper = currentLambdaReturnWrapper;
        KotlinWriter savedOut = out;
        CommentRenderer savedComments = comments;
        KotlinWriter nested = new KotlinWriter("    ", out.indentLevel());
        out = nested;
        comments = new CommentRenderer(out);
        currentLambdaLabel = label;
        currentLambdaReturnWrapper = returnWrapper;
        out.line(label + "@ { " + (params.isEmpty() ? "" : String.join(", ", params) + " ->"));
        out.indent();
        emitBlockContents(block);
        out.dedent();
        out.line("}");
        String rendered = out.toString().strip();
        out = savedOut;
        comments = savedComments;
        currentLambdaLabel = previousLabel;
        currentLambdaReturnWrapper = previousWrapper;
        scope.pop();
        return rendered;
    }

    private String emitMethodReference(MethodReferenceExpr reference) {
        String scope = reference.getScope() instanceof com.github.javaparser.ast.expr.TypeExpr typeExpr
                ? types.map(typeExpr.getType()).render()
                : emitExpression(reference.getScope(), null);
        String identifier = reference.getIdentifier();
        if (identifier.equals("new")) {
            return "::" + scope;
        }
        if (identifier.equals("toUpperCase")) {
            identifier = "uppercase";
        } else if (identifier.equals("toLowerCase")) {
            identifier = "lowercase";
        }
        return scope + "::" + name(identifier);
    }

    private String emitSwitchExpression(SwitchExpr switchExpr) {
        String selector = emitExpression(switchExpr.getSelector(), null);
        List<String> branches = new ArrayList<>();
        boolean hasDefault = false;
        boolean hasPattern = false;
        for (SwitchEntry entry : switchExpr.getEntries()) {
            List<String> labels = new ArrayList<>();
            String patternName = null;
            String patternType = null;
            for (Expression label : entry.getLabels()) {
                if (label instanceof com.github.javaparser.ast.expr.TypePatternExpr pattern) {
                    patternName = pattern.getNameAsString();
                    patternType = types.map(pattern.getType()).render();
                    hasPattern = true;
                    labels.add("is " + patternType);
                } else {
                    labels.add(emitExpression(label, null));
                }
            }
            if (labels.isEmpty()) {
                labels.add("else");
                hasDefault = true;
            }
            String value = "";
            List<String> statements = new ArrayList<>();
            scope.push();
            if (patternName != null && patternType != null) {
                scope.declare(patternName, KType.simple(patternType, patternType, KType.Category.OBJECT));
            }
            for (Statement statement : entry.getStatements()) {
                if (statement instanceof ExpressionStmt expressionStmt) {
                    value = emitExpression(expressionStmt.getExpression(), null);
                } else if (statement instanceof YieldStmt yield) {
                    value = emitExpression(yield.getExpression(), null);
                } else if (statement instanceof ReturnStmt returnStmt && returnStmt.getExpression().isPresent()) {
                    value = emitExpression(returnStmt.getExpression().get(), null);
                } else if (!(statement instanceof BreakStmt)) {
                    statements.add(emitStatementAsString(statement));
                }
            }
            scope.pop();
            if (value.isBlank()) {
                value = statements.isEmpty() ? "Unit" : statements.get(statements.size() - 1);
                if (!statements.isEmpty()) {
                    statements.remove(statements.size() - 1);
                }
            }
            if (patternName != null && patternType != null) {
                StringBuilder block = new StringBuilder("run { val ")
                        .append(name(patternName)).append(" = ").append(selector).append("; ");
                for (String statement : statements) {
                    block.append(statement).append("; ");
                }
                block.append(value).append(" }");
                value = block.toString();
            }
            branches.add(String.join(", ", labels) + " -> " + value);
        }
        if (!hasDefault && !hasPattern) {
            branches.add("else -> error(\"Unreachable switch expression\")");
        }
        return "when (" + selector + ") { " + String.join("; ", branches) + " }";
    }

    private String emitStatementAsString(Statement statement) {
        KotlinWriter savedOut = out;
        KotlinWriter nested = new KotlinWriter("    ", 0);
        out = nested;
        emitStatement(statement);
        out = savedOut;
        return nested.toString().trim();
    }

    private String coerce(String expression, KType from, KType to) {
        if (to == null || from == null || from.isUnknown()) {
            return expression;
        }
        if (to.isNumeric() && from.isNumeric() && !to.base().equals(from.base())) {
            String value = expression;
            if (from.isNullable()) {
                value = value + "!!";
            }
            return types.numericConversion("(" + value + ")", to);
        }
        if (to.category() == KType.Category.CHAR && from.category() == KType.Category.NUMBER) {
            return "(" + expression + ").toChar()";
        }
        if (!to.isNullable() && from.isNullable() && !to.isUnit()) {
            return expression + "!!";
        }
        return expression;
    }

    // ---------------------------------------------------------------------
    // Utilities
    // ---------------------------------------------------------------------

    private String visibility(Node node) {
        if (node instanceof com.github.javaparser.ast.nodeTypes.NodeWithModifiers<?> withModifiers) {
            if (withModifiers.hasModifier(Modifier.Keyword.PRIVATE)) return "private";
            if (withModifiers.hasModifier(Modifier.Keyword.PROTECTED)) return "protected";
            if (withModifiers.hasModifier(Modifier.Keyword.PUBLIC)) return "";
            Node parent = node.getParentNode().orElse(null);
            if (parent instanceof ClassOrInterfaceDeclaration declaration && declaration.isInterface()
                    && (node instanceof MethodDeclaration || node instanceof FieldDeclaration
                    || node instanceof TypeDeclaration<?>)) {
                return "";
            }
            return "internal";
        }
        return "";
    }

    private String takePendingLabel() {
        String label = pendingLabel;
        pendingLabel = null;
        return label;
    }

    private String name(String value) {
        return NameUtils.escape(value);
    }

    private String escapeString(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t");
    }

    private String normalizeIntegerLiteral(String value) {
        String normalized = value.replace("_", "");
        if (normalized.startsWith("0") && normalized.length() > 1
                && !normalized.startsWith("0x") && !normalized.startsWith("0X")
                && !normalized.startsWith("0b") && !normalized.startsWith("0B")) {
            try {
                return Integer.toString(Integer.parseInt(normalized, 8));
            } catch (NumberFormatException ignored) {
                return value;
            }
        }
        return value;
    }

    private boolean hasAnnotation(Node node, String simpleName) {
        if (!(node instanceof com.github.javaparser.ast.nodeTypes.NodeWithAnnotations<?> annotated)) {
            return false;
        }
        return annotated.getAnnotations().stream()
                .anyMatch(annotation -> Imports.simpleName(annotation.getNameAsString()).equals(simpleName));
    }

    private boolean isSealed(ClassOrInterfaceDeclaration declaration) {
        return declaration.getModifiers().stream()
                .anyMatch(modifier -> modifier.getKeyword() == Modifier.Keyword.SEALED);
    }
}
