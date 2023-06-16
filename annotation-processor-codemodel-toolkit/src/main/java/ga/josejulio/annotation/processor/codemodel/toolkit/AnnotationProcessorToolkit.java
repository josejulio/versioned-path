package ga.josejulio.annotation.processor.codemodel.toolkit;

import com.sun.codemodel.JAnnotatable;
import com.sun.codemodel.JAnnotationUse;
import com.sun.codemodel.JClass;
import com.sun.codemodel.JClassAlreadyExistsException;
import com.sun.codemodel.JCodeModel;
import com.sun.codemodel.JDefinedClass;
import com.sun.codemodel.JExpr;
import com.sun.codemodel.JInvocation;
import com.sun.codemodel.JMethod;
import com.sun.codemodel.JMod;
import com.sun.codemodel.JType;
import com.sun.codemodel.JVar;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Types;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.stream.Collectors;

public class AnnotationProcessorToolkit {

    private final JCodeModel codeModel;
    private final Types types;

    public AnnotationProcessorToolkit(JCodeModel codeModel, Types types) {
        this.codeModel = codeModel;
        this.types = types;
    }

    public JDefinedClass extendFromClass(String newFullClassName, Element element) {
        if (!element.getKind().isClass()) {
            throw new AnnotationProcessingException("Unable to extend from non class: " + element.getSimpleName());
        }

        try {
            JDefinedClass klass = codeModel._class(newFullClassName);
            klass._extends(
                    (JClass) toJType(element)
            );

            return klass;
        } catch (JClassAlreadyExistsException classAlreadyExistsException) {
            throw new AnnotationProcessingException("Class already exists:" + newFullClassName, classAlreadyExistsException);
        }
    }

    public JMethod overrideMethod(JDefinedClass klass, ExecutableElement method, Collection<Class<? extends Annotation>> ignoredAnnotations) {
        JType returnValue = toJType(method.getReturnType());

        JMethod jMethod = klass
                .method(toMods(method.getModifiers()), returnValue, method.getSimpleName().toString());
        jMethod.annotate(Override.class);

        copyAnnotations(method, jMethod, ignoredAnnotations);
        List<JVar> parameters = copyParameters(method, jMethod, ignoredAnnotations);

        JInvocation superCall = JExpr._super().invoke(method.getSimpleName().toString());

        for (JVar parameter: parameters) {
            superCall.arg(parameter);
        }

        if (returnValue.equals(codeModel.VOID)) {
            jMethod.body().add(superCall);
        } else {
            jMethod.body()._return(superCall);
        }

        return jMethod;
    }

    public void copyAnnotations(Element from, JAnnotatable target, Collection<Class<? extends Annotation>> ignoredAnnotations) {
        List<? extends AnnotationMirror> annotations = from.getAnnotationMirrors();

        annotation_processing:
        for (AnnotationMirror annotationMirror: annotations) {

            for (Class<?> ignoredAnnotation: ignoredAnnotations) {
                if (annotationMirror.getAnnotationType().asElement().asType().toString().equals(ignoredAnnotation.getCanonicalName())) {
                    continue annotation_processing;
                }
            }

            JAnnotationUse jAnnotationUse = target.annotate(codeModel.directClass(annotationMirror.getAnnotationType().asElement().asType().toString()));

            for (Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> entry: annotationMirror.getElementValues().entrySet()) {
                String key = entry.getKey().getSimpleName().toString();
                writeAnnotationValue(new AnnotationParamWriter(key, jAnnotationUse), entry.getValue());
            }
        }
    }

    public List<JVar> copyParameters(ExecutableElement method, JMethod jMethod, Collection<Class<? extends Annotation>> ignoredAnnotations) {
        List<JVar> parameters = new ArrayList<>();
        for (VariableElement parameter: method.getParameters()) {
            JVar jvar = jMethod.param(
                    toJType(parameter.asType()),
                    parameter.getSimpleName().toString()
            );

            copyAnnotations(parameter, jvar, ignoredAnnotations);
            parameters.add(jvar);
        }

        return parameters;
    }

    public int toMods(Set<Modifier> modifiers) {
        int mods = 0x0;
        for (Modifier modifier: modifiers) {
            mods |= toMod(modifier);
        }
        return mods;
    }

    public int toMod(Modifier modifier) {
        return switch (modifier) {
            case PUBLIC -> JMod.PUBLIC;
            case PROTECTED -> JMod.PROTECTED;
            case PRIVATE -> JMod.PRIVATE;
            case FINAL -> JMod.FINAL;
            case STATIC -> JMod.STATIC;
            case ABSTRACT -> JMod.ABSTRACT;
            case NATIVE -> JMod.NATIVE;
            case SYNCHRONIZED -> JMod.SYNCHRONIZED;
            case TRANSIENT -> JMod.TRANSIENT;
            case VOLATILE -> JMod.VOLATILE;
            case DEFAULT, NON_SEALED -> 0;
            case SEALED, STRICTFP -> throw new AnnotationProcessingException("Modifier not supported: " + modifier);
        };
    }

    public JType toJType(TypeMirror typeMirror) {
        if (typeMirror instanceof DeclaredType declaredType) {
            return toJType(declaredType);
        }

        return switch (typeMirror.getKind()) {
            case CHAR -> codeModel.CHAR;
            case BYTE -> codeModel.BYTE;
            case INT -> codeModel.INT;
            case LONG -> codeModel.LONG;
            case VOID -> codeModel.VOID;
            case FLOAT -> codeModel.FLOAT;
            case SHORT -> codeModel.SHORT;
            case DOUBLE -> codeModel.DOUBLE;
            case BOOLEAN -> codeModel.BOOLEAN;
            default -> throw new AnnotationProcessingException("Unexpected type mirror: " + typeMirror);
        };
    }

    public JType toJType(DeclaredType declaredType) {
        JClass jClass = codeModel.ref(types.erasure(declaredType).toString());

        if (declaredType.getTypeArguments().size() > 0) {
            return jClass.narrow(declaredType
                    .getTypeArguments()
                    .stream()
                    .map(typedArgument -> (JClass) toJType(typedArgument))
                    .collect(Collectors.toList())
            );
        }

        return jClass;
    }

    public JType toJType(AnnotationMirror annotationMirror) {
        return toJType(annotationMirror.getAnnotationType());
    }

    public JType toJType(Element element) {
        return toJType(element.asType());
    }

    public Class<?> toClass(TypeMirror typeMirror) {
        if (typeMirror instanceof DeclaredType declaredType) {
            return toClass(declaredType);
        }

        return switch (typeMirror.getKind()) {
            case CHAR -> char.class;
            case BYTE -> byte.class;
            case INT -> int.class;
            case LONG -> long.class;
            case VOID -> void.class;
            case FLOAT -> float.class;
            case SHORT -> short.class;
            case DOUBLE -> double.class;
            case BOOLEAN -> boolean.class;
            default -> throw new AnnotationProcessingException("Unexpected type mirror: " + typeMirror);
        };
    }

    public <T> Class<T> toClass(DeclaredType declaredType) {
        return (Class<T>) toClass(declaredType.asElement());
    }

    public Class<?> toClass(AnnotationMirror annotationMirror) {
        return toClass(annotationMirror.getAnnotationType());
    }

    public Class<?> toClass(Element element) {
        if (!isClass(element)) {
            return toClass(element.asType());
        }

        Stack<String> stack = new Stack<>();
        stack.push(element.getSimpleName().toString());

        while (!element.getKind().equals(ElementKind.PACKAGE)) {
            element = element.getEnclosingElement();

            if (isClass(element)) {
                stack.push("$");
                stack.push(element.getSimpleName().toString());
            } else {
                stack.push(".");
                stack.push(element.asType().toString());
            }
        }

        StringBuilder builder = new StringBuilder();
        while (!stack.empty()) {
            builder.append(stack.pop());
        }

        String name = builder.toString();

        try {
            return Class.forName(name);
        } catch (ClassNotFoundException classNotFoundException) {
            throw new AnnotationProcessingException("Class for name: %s not found.".formatted(name), classNotFoundException);
        }
    }

    private boolean isClass(Element element) {
        return element.getKind().isClass() || element.getKind().equals(ElementKind.ANNOTATION_TYPE) || element.getKind().isInterface();
    }

    private void writeAnnotationValue(AnnotationParamWriter annotationParamWriter, AnnotationValue annotationValue) {
        Object value = annotationValue.getValue();

        if (value instanceof Number number) {
            annotationParamWriter.writeNumber(number);
        } else if (value instanceof String) {
            annotationParamWriter.writeString(value.toString());
        } else if (value instanceof TypeMirror typeMirror) {
            annotationParamWriter.writeJType(toJType(typeMirror));
        } else if (value instanceof VariableElement variableElement) {
            annotationParamWriter.writeEnum(Enum.valueOf((Class<? extends Enum>) toClass(variableElement), variableElement.getSimpleName().toString()));
        } else if (value instanceof AnnotationMirror annotationMirror) {
            writeAnnotationMirror(annotationParamWriter, annotationMirror);
        } else if (value instanceof List<?>) {
            AnnotationParamWriter arrayWriter = annotationParamWriter.writeArray();
            for (AnnotationValue val: (List<? extends AnnotationValue>) value) {
                writeAnnotationValue(arrayWriter, val);
            }
        } else {
            throw new UnsupportedOperationException("Unsupported value type of AnnotationValue:" + value);
        }
    }

    private void writeAnnotationMirror(AnnotationParamWriter writer, AnnotationMirror annotationMirror) {
        Class<? extends Annotation> klass = toClass(annotationMirror.getAnnotationType());
        JAnnotationUse annotation = writer.writeClass(klass);

        for (Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> entry: annotationMirror.getElementValues().entrySet()) {
            String key = entry.getKey().getSimpleName().toString();
            writeAnnotationValue(new AnnotationParamWriter(key, annotation), entry.getValue());
        }
    }

}
