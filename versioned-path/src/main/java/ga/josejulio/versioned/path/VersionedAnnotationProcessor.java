package ga.josejulio.versioned.path;

import ga.josejulio.annotation.processor.codemodel.toolkit.AnnotationProcessingCodeWriter;
import ga.josejulio.annotation.processor.codemodel.toolkit.AnnotationProcessingException;
import ga.josejulio.annotation.processor.codemodel.toolkit.AnnotationProcessorToolkit;
import com.sun.codemodel.JAnnotatable;
import com.sun.codemodel.JCodeModel;
import com.sun.codemodel.JDefinedClass;
import com.sun.codemodel.JMethod;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HEAD;
import jakarta.ws.rs.OPTIONS;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class VersionedAnnotationProcessor extends AbstractProcessor {

    private static final List<Class<? extends Annotation>> bannedAnnotations = List.of(
            Path.class,
            GET.class,
            POST.class,
            PUT.class,
            PATCH.class,
            DELETE.class,
            OPTIONS.class,
            HEAD.class
    );

    private static final List<Class<? extends Annotation>> IGNORED_ANNOTATIONS = List.of(
            VersionedPath.class,
            VersionedMethod.class
    );

    private static final String VERSION_REPLACEMENT = "$version";

    private AnnotationProcessorToolkit annotationProcessorToolkit;
    private JCodeModel codeModel;
    private Messager messager;
    private Filer filer;
    private Elements elements;
    private Types types;
    private final Set<String> processedClasses = new HashSet<>();

    @Override
    public synchronized void init(ProcessingEnvironment environment) {
        init(new JCodeModel(), environment.getElementUtils(), environment.getMessager(), environment.getFiler(), environment.getTypeUtils());
    }

    void init(JCodeModel codeModel, Elements elements, Messager messager, Filer filer, Types types) {
        this.elements = elements;
        this.messager = messager;
        this.filer = filer;
        this.types = types;
        this.setCodeModel(codeModel);
    }

    void setCodeModel(JCodeModel codeModel) {
        this.codeModel = codeModel;
        this.annotationProcessorToolkit = new AnnotationProcessorToolkit(codeModel, types);
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnvironment) {
        for (TypeElement annotation : annotations) {
            Set<? extends Element> annotatedElements = roundEnvironment.getElementsAnnotatedWith(annotation);
            for (Element element : annotatedElements) {
                // Only triggered by class elements
                if (element.getKind().equals(ElementKind.CLASS)) {
                    processedClasses.add(element.asType().toString());
                    processClass(element);
                } else {
                    if (!processedClasses.contains(element.getEnclosingElement().asType().toString())) {
                        messager.printMessage(
                                Diagnostic.Kind.ERROR,
                                "Container class (" +
                                        element.getEnclosingElement().asType().toString() +
                                        ") of element ("
                                        + element.getSimpleName().toString() +
                                        ") does not have the @VersionedPath annotation.",
                                element
                        );
                    }
                }
            }
        }

        try {
            codeModel.build(new AnnotationProcessingCodeWriter(filer));
        } catch (IOException ex) {
            messager.printMessage(Diagnostic.Kind.ERROR, "Error writing source files: " + ex);
        }

        setCodeModel(new JCodeModel());
        return true;
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return Set.of(VersionedPath.class.getCanonicalName());
    }

    List<JDefinedClass> processClass(Element classElement) {
        List<JDefinedClass> created = new ArrayList<>();
        checkForBannedAnnotations(classElement);

        List<ExecutableElement> methodsWithVersionedPath = collectVersionedMethods(classElement);
        validate(methodsWithVersionedPath);

        VersionedPath versionedPath = classElement.getAnnotation(VersionedPath.class);
        Version classVersion = getClassVersion(classElement);
        Set<Version> versions = collectVersions(classVersion, methodsWithVersionedPath);

        PackageElement classPackage = elements.getPackageOf(classElement);
        String packageName = classPackage.getQualifiedName().toString();
        String className = classElement.asType().toString().substring(packageName.length() + 1)
                // Inner classes use "." in their class names.
                .replace(".", "_");

        for (Version version: versions) {
            try {
                String name = packageName + "."  + className + "V" + version.toMinorVersionString().replace(".", "_");
                JDefinedClass klass = annotationProcessorToolkit.extendFromClass(name, classElement);
                created.add(klass);

                annotationProcessorToolkit.copyAnnotations(classElement, klass, IGNORED_ANNOTATIONS);

                annotatePath(klass, versionedPath, version);

                List<ExecutableElement> targetMethods = collectMethodsForTargetVersion(methodsWithVersionedPath, version, classVersion);

                for (ExecutableElement method : targetMethods) {
                    JMethod jMethod = annotationProcessorToolkit.overrideMethod(klass, method, IGNORED_ANNOTATIONS);
                    annotatePath(jMethod, method.getAnnotation(VersionedPath.class), version);
                    jMethod.annotate(toRestMethodAnnotation(getHttpMethod(method)));
                }
            } catch (AnnotationProcessingException annotationProcessingException) {
                messager.printMessage(Diagnostic.Kind.ERROR, annotationProcessingException.getMessage());
            }
        }

        return created;
    }

    private void checkForBannedAnnotations(Element element) {
        // Todo: @VersionedMethod can be mixed with @Path
        for (Class<? extends Annotation> annotation: bannedAnnotations) {
            if (element.getAnnotation(annotation) != null) {
                messager.printMessage(Diagnostic.Kind.ERROR, "Annotation %s not allowed in %s".formatted(annotation, element.getSimpleName().toString()));
            }
        }
    }

    private List<ExecutableElement> collectVersionedMethods(Element classElement) {
        return classElement
                .getEnclosedElements()
                .parallelStream()
                .filter(element -> element.getKind().equals(ElementKind.METHOD))
                .filter(element -> element.getAnnotation(VersionedPath.class) != null || element.getAnnotation(VersionedMethod.class) != null)
                .map(element -> (ExecutableElement) element)
                .collect(Collectors.toList());
    }

    private List<ExecutableElement> collectMethodsForTargetVersion(List<ExecutableElement> methods, Version targetVersion, Version defaultVersion) {
        Map<String, ExecutableElement> targetMethods = new HashMap<>();

        for (ExecutableElement method : methods) {
            Version methodVersion = getVersion(method, defaultVersion);

            if (methodVersion.compareTo(targetVersion) <= 0) {
                String key = computeKeyForMethod(method);
                ExecutableElement presentExecutableElement = targetMethods.get(key);

                if (presentExecutableElement == null) {
                    targetMethods.put(key, method);
                } else {
                    Version presentVersion = getVersion(presentExecutableElement, defaultVersion);

                    if (presentVersion.equals(methodVersion)) {
                        throw new RuntimeException("Multiple methods for the same endpoint/version: " + key);
                    }

                    if (presentVersion.compareTo(methodVersion) < 0) {
                        targetMethods.put(key, method);
                    }
                }
            }
        }

        return List.copyOf(targetMethods.values());
    }

    private void validate(List<ExecutableElement> elements) {
        for (Element enclosedElement: elements) {
            // Error: Method is not public
            if (!enclosedElement.getModifiers().contains(Modifier.PUBLIC)) {
                messager.printMessage(Diagnostic.Kind.ERROR, "Methods are required to be public: " + enclosedElement.getSimpleName());
            }

            // Error: Method is final
            if (enclosedElement.getModifiers().contains(Modifier.FINAL)) {
                messager.printMessage(Diagnostic.Kind.ERROR, "Final methods are not allowed: " + enclosedElement.getSimpleName());
            }

            checkForBannedAnnotations(enclosedElement);
        }
    }

    private Set<Version> collectVersions(Version classVersion, Collection<ExecutableElement> versionedElementMethods) {
        Set<Version> versionSet = new HashSet<>();
        versionSet.add(classVersion);

        for (Element element: versionedElementMethods) {
            VersionedPath versionedPath  = element
                    .getAnnotation(VersionedPath.class);

            String methodVersion = versionedPath != null ? versionedPath.sinceVersion() : "";

            versionSet.add(methodVersion.isBlank() ? classVersion : new Version(methodVersion));
        }

        return versionSet;
    }

    private Version getVersion(Element element, Version defaultVersion) {
        VersionedPath versionedPath = element.getAnnotation(VersionedPath.class);
        if (versionedPath == null || versionedPath.sinceVersion().isBlank()) {
            return defaultVersion;
        }

        return new Version(versionedPath.sinceVersion());
    }

    private Version getClassVersion(Element classElement) {
        String sinceClassVersion = classElement.getAnnotation(VersionedPath.class).sinceVersion();
        if (sinceClassVersion.isBlank()) {
            messager.printMessage(Diagnostic.Kind.ERROR, "`sinceVersion` is required for class elements: " + classElement.asType().toString());
            sinceClassVersion = "1.0";
        }

        return new Version(sinceClassVersion);
    }

    private String computeKeyForMethod(ExecutableElement method) {
        VersionedPath versionedPath = method.getAnnotation(VersionedPath.class);
        String path = versionedPath != null ? versionedPath.path() : "";
        return "%s_%s".formatted(getHttpMethod(method).name(), path);
    }

    private VersionedMethod.HttpMethod getHttpMethod(Element element) {
        VersionedMethod versionedMethod = element.getAnnotation(VersionedMethod.class);
        if (versionedMethod == null) {
            return VersionedMethod.HttpMethod.GET;
        }

        return versionedMethod.value();
    }

    private Class<? extends Annotation> toRestMethodAnnotation(VersionedMethod.HttpMethod versionedMethod) {
        return switch (versionedMethod) {
            case GET -> GET.class;
            case PUT -> PUT.class;
            case HEAD -> HEAD.class;
            case DELETE -> DELETE.class;
            case OPTIONS -> OPTIONS.class;
            case PATCH -> PATCH.class;
            case POST -> POST.class;
        };
    }

    private void annotatePath(JAnnotatable annotatable, VersionedPath versionedPath, Version version) {
        if (versionedPath != null) {
            annotatable
                    .annotate(Path.class)
                    .param("value", versionedPath.path().replace(VERSION_REPLACEMENT, version.toMinorVersionString()));
        }
    }
}
