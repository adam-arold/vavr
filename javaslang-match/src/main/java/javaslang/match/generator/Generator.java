/*     / \____  _    _  ____   ______  / \ ____  __    _______
 *    /  /    \/ \  / \/    \ /  /\__\/  //    \/  \  //  /\__\   JΛVΛSLΛNG
 *  _/  /  /\  \  \/  /  /\  \\__\\  \  //  /\  \ /\\/ \ /__\ \   Copyright 2014-2016 Javaslang, http://javaslang.io
 * /___/\_/  \_/\____/\_/  \_/\__\/__/\__\_/  \_//  \__/\_____/   Licensed under the Apache License, Version 2.0
 */
package javaslang.match.generator;

import javaslang.match.annotation.Unapply;
import javaslang.match.model.ClassModel;
import javaslang.match.model.MethodModel;
import javaslang.match.model.TypeParameterModel;

import javax.annotation.processing.Messager;
import javax.tools.Diagnostic;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;

// ------------
// TODO: check generic (return) type parameters (-> e.g. wildcard is not allowed)
// TODO: detect collisions / retry with different generic type arg names (starting with _1, _2, ...)
// ------------

/**
 * Code generator for structural pattern matching patterns.
 *
 * @author Daniel Dietrich
 * @since 2.0.0
 */
public class Generator {

    private Generator() {
    }

    // ENTRY POINT: Expands one @Patterns class
    public static Optional<String> generate(String derivedClassName, ClassModel classModel, Messager messager) {
        List<MethodModel> methodModels = getMethods(classModel, messager);
        if (methodModels.isEmpty()) {
            messager.printMessage(Diagnostic.Kind.WARNING, "No @Unapply methods found.", classModel.getTypeElement());
            return Optional.empty();
        } else {
            final String _package = classModel.getPackageName();
            final ImportManager im = ImportManager.forClass(classModel);
            final String methods = generate(im, classModel, methodModels);
            final String result = (_package.isEmpty() ? "" : "package " + _package + ";\n\n") +
                    im.getImports() +
                    "\n\n// GENERATED BY JAVASLANG <<>> derived from " + classModel.getFullQualifiedName() + "\n\n" +
                    "public final class " + derivedClassName + " {\n\n" +
                    "    private " + derivedClassName + "() {\n" +
                    "    }\n\n" +
                    methods +
                    "}\n";
            return Optional.of(result);
        }
    }

    // Expands the @Unapply methods of a @Patterns class
    private static String generate(ImportManager im, ClassModel classModel, List<MethodModel> methodModels) {
        final StringBuilder builder = new StringBuilder();
        for (MethodModel methodModel : methodModels) {
            generate(im, classModel, methodModel, builder);
            builder.append("\n");
        }
        return builder.toString();
    }

    // Expands one @Unapply method
    private static void generate(ImportManager im, ClassModel classModel, MethodModel methodModel, StringBuilder builder) {
        final String paramTypeName = im.getType(methodModel.getParameter(0).getType());
        final String name = methodModel.getName();
        final int arity = Integer.parseInt(methodModel.getReturnType().getClassName().substring("Tuple".length()));
        final String body;
        if (arity == 0) {
            body = pattern0(im) + ".of(" + paramTypeName + ".class)";
        } else {
            final String args = IntStream.rangeClosed(1, arity).mapToObj(i -> "p" + i).collect(joining(", "));
            final String unapplyRef = classModel.getClassName() + "::" + name;
            body = String.format("%s.of(%s, %s, %s)", patternN(im, arity), paramTypeName + ".class", args, unapplyRef);
        }
        final String returnType = getReturnType(im, methodModel, arity);
        final String method;
        if (methodModel.hasTypeParameters()) {
            final String generics = getGenerics(im, methodModel);
            final String params = getParams(im, arity);
            method = String.format("%s %s %s(%s) {\n        return %s;\n    }", generics, returnType, name, params, body);
        } else {
            method = String.format("final %s %s = %s;", returnType, name, body);
        }
        builder.append("    public static ").append(method).append("\n");
    }

    // Expands the generics part of a method declaration
    private static String getGenerics(ImportManager im, MethodModel methodModel) {
        final List<TypeParameterModel> typeParameters = methodModel.getTypeParameters();
        final List<TypeParameterModel> returnTypeArgs = methodModel.getReturnType().getTypeParameters();
        if (typeParameters.size() + returnTypeArgs.size() == 0) {
            return "";
        } else {
            final List<String> result = new ArrayList<>();
            result.addAll(typeParameters.stream()
                    .map(typeParameterModel -> mapToName(im, typeParameterModel))
                    .collect(toList()));
            for (int i = 0; i < returnTypeArgs.size(); i++) {
                final String returnTypeArg = mapToName(im, returnTypeArgs.get(i));
                result.add("_" + (i + 1) + " extends " + returnTypeArg);
            }
            return result.stream().collect(joining(", ", "<", ">"));
        }
    }

    private static String mapToName(ImportManager im, TypeParameterModel typeParameterModel) {
        if (typeParameterModel.isType()) {
            return im.getType(typeParameterModel.asType());
        } else if (typeParameterModel.isTypeVar()) {
            return typeParameterModel.toString();
        } else {
            throw new IllegalStateException("Unhandled type parameter: " + typeParameterModel.toString());
        }
    }

    // Expands the return type of a method declaration
    private static String getReturnType(ImportManager im, MethodModel methodModel, int arity) {
        final String type = im.getType(methodModel.getParameter(0).getType());
        if (arity == 0) {
            return pattern0(im) + "<" + type + ">";
        } else {
            final List<String> resultTypes = new ArrayList<>();
            final int typeParameterCount = methodModel.getReturnType().getTypeParameterCount();
            resultTypes.add(type);
            for (int i = 0; i < typeParameterCount; i++) {
                resultTypes.add("_" + (i + 1));
            }
            return patternN(im, arity) + "<" + resultTypes.stream().collect(joining(", ")) + ">";
        }
    }

    private static String getParams(ImportManager im, int arity) {
        final String patternType = im.getType("javaslang", "API.Match.Pattern");
        return IntStream.rangeClosed(1, arity).mapToObj(i -> patternType + "<_" + i + ", ?> p" + i).collect(joining(", "));
    }

    // returns all @Unapply methods of a @Patterns class
    private static List<MethodModel> getMethods(ClassModel classModel, Messager messager) {
        return classModel.getMethods().stream()
                .filter(method -> method.isAnnotatedWith(Unapply.class) && Unapply.Checker.isValid(method.getExecutableElement(), messager))
                .collect(toList());
    }

    private static String pattern0(ImportManager im) {
        return im.getType("javaslang", "API.Match.Pattern0");
    }

    private static String patternN(ImportManager im, int arity) {
        return im.getType("javaslang", "API.Match.Pattern" + arity);
    }
}
