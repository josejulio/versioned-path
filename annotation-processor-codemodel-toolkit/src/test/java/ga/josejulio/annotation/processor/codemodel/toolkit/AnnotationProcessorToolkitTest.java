package ga.josejulio.annotation.processor.codemodel.toolkit;

import com.karuslabs.elementary.junit.Cases;
import com.karuslabs.elementary.junit.Tools;
import com.karuslabs.elementary.junit.ToolsExtension;
import com.karuslabs.elementary.junit.annotations.Case;
import com.karuslabs.elementary.junit.annotations.Introspect;
import com.sun.codemodel.JAnnotationUse;
import com.sun.codemodel.JClass;
import com.sun.codemodel.JCodeModel;
import com.sun.codemodel.JDefinedClass;
import com.sun.codemodel.JMethod;
import com.sun.codemodel.JType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(ToolsExtension.class)
@Introspect
public class AnnotationProcessorToolkitTest {

    private static final String CASE_SAMPLE_TEST_CLASS = "CASE_SAMPLE_TEST_CLASS";
    private static final String CASE_FOO_METHOD = "CASE_FOO_METHOD";
    private static final String CASE_INNER_CLASS = "CASE_INNER_CLASS";
    private static final String CASE_ATTRIBUTE_GENERIC = "CASE_ATTRIBUTE_GENERIC";
    private static final String CASE_INTEGER_METHOD = "CASE_INTEGER_METHOD";

    private final JCodeModel codeModel = new JCodeModel();
    AnnotationProcessorToolkit toolkit = new AnnotationProcessorToolkit(
            codeModel,
            Tools.types()
    );

    @Case(CASE_SAMPLE_TEST_CLASS)
    class SampleTest {

        @Case(CASE_ATTRIBUTE_GENERIC)
        private final Map<String, Map<String, SampleTest>> map = new HashMap<>();

        @Deprecated
        @Case(CASE_INNER_CLASS)
        class InnerSampleTest extends SampleTest {
            public void bar(int xyz) {

            }

            @Override
            public int foo(float param0, Object param1, SampleTest param2) {
                return 8;
            }
        }

        @Deprecated
        @Case(CASE_FOO_METHOD)
        public int foo(float param0, Object param1, SampleTest param2) {
            return 5;
        }

        @Case(CASE_INTEGER_METHOD)
        public Integer integer() {
            return 2;
        }

    }

    @Test
    void toClassTest(Cases cases) {
        Element element = cases.one(CASE_SAMPLE_TEST_CLASS);
        assertNotNull(element);

        Class<?> klass = toolkit.toClass(element);
        assertEquals(SampleTest.class, klass);

        final Element fooMethodElement = cases.one(CASE_FOO_METHOD);
        assertNotNull(fooMethodElement);
        assertThrows(AnnotationProcessingException.class, () -> toolkit.toClass(fooMethodElement));

        // Enclosing element is the class
        assertEquals(SampleTest.class, toolkit.toClass(fooMethodElement.getEnclosingElement()));

        ExecutableElement executableElement = (ExecutableElement) fooMethodElement;

        // Lets try with the type of the method
        TypeMirror returnType = executableElement.getReturnType();
        assertNotNull(returnType);
        assertEquals(int.class, toolkit.toClass(returnType));

        // And the params
        List<? extends VariableElement> params = executableElement.getParameters();
        assertNotNull(params);
        assertEquals(3, params.size());

        assertEquals(float.class, toolkit.toClass(params.get(0)));
        assertEquals(Object.class, toolkit.toClass(params.get(1)));
        assertEquals(SampleTest.class, toolkit.toClass(params.get(2)));

        // Lets try with an Integer object
        final Element integerMethodElement = cases.one(CASE_INTEGER_METHOD);
        ExecutableElement executableIntegerElement = (ExecutableElement) integerMethodElement;
        returnType = executableIntegerElement.getReturnType();
        assertNotNull(returnType);
        assertEquals(Integer.class, toolkit.toClass(returnType));

        // And the params
        params = executableIntegerElement.getParameters();
        assertNotNull(params);
        assertEquals(0, params.size());
    }

    @Test
    void toClassInnerClassTest(Cases cases) {
        Element element = cases.one(CASE_INNER_CLASS);
        assertNotNull(element);

        Class<?> klass = toolkit.toClass(element);
        assertEquals(SampleTest.InnerSampleTest.class, klass);
    }

    @Test
    void toClassAnnotationTest(Cases cases) {
        Element element = cases.one(CASE_INNER_CLASS);
        assertNotNull(element);

        List <? extends AnnotationMirror> annotations = element.getAnnotationMirrors();
        assertEquals(2, annotations.size());

        assertEquals(Deprecated.class, toolkit.toClass(annotations.get(0)));
        assertEquals(Case.class, toolkit.toClass(annotations.get(1)));
    }

    @Test
    void toClassGeneric(Cases cases) {
        Element element = cases.one(CASE_ATTRIBUTE_GENERIC);
        assertNotNull(element);

        assertEquals(Map.class, toolkit.toClass(element));
    }

    @Test
    void toJTypeTest(Cases cases) {
        Element element = cases.one(CASE_SAMPLE_TEST_CLASS);
        assertNotNull(element);

        JType jType = toolkit.toJType(element);
        assertJClass(jType, SampleTest.class);

        final Element fooMethodElement = cases.one(CASE_FOO_METHOD);
        assertNotNull(fooMethodElement);
        assertThrows(AnnotationProcessingException.class, () -> toolkit.toJType(fooMethodElement));

        // Enclosing element is the class
        jType = toolkit.toJType(fooMethodElement.getEnclosingElement());
        assertJClass(jType, SampleTest.class);

        ExecutableElement executableElement = (ExecutableElement) fooMethodElement;

        // Lets try with the type of the method
        TypeMirror returnType = executableElement.getReturnType();
        assertNotNull(returnType);
        assertEquals(codeModel.INT, toolkit.toJType(returnType));

        // And the params
        List<? extends VariableElement> params = executableElement.getParameters();
        assertNotNull(params);
        assertEquals(3, params.size());

        assertEquals(codeModel.FLOAT, toolkit.toJType(params.get(0)));
        assertJClass(toolkit.toJType(params.get(1)), Object.class);
        assertJClass(toolkit.toJType(params.get(2)), SampleTest.class);

        // Lets try with an Integer object
        final Element integerMethodElement = cases.one(CASE_INTEGER_METHOD);
        ExecutableElement executableIntegerElement = (ExecutableElement) integerMethodElement;
        returnType = executableIntegerElement.getReturnType();
        assertNotNull(returnType);
        assertJClass(toolkit.toJType(returnType), Integer.class);

        // And the params
        params = executableIntegerElement.getParameters();
        assertNotNull(params);
        assertEquals(0, params.size());
    }

    @Test
    void toJTypeInnerClassTest(Cases cases) {
        Element element = cases.one(CASE_INNER_CLASS);
        assertNotNull(element);

        JType jType = toolkit.toJType(element);
        assertJClass(jType, SampleTest.InnerSampleTest.class);
    }

    @Test
    void toJTypeAnnotationTest(Cases cases) {
        Element element = cases.one(CASE_INNER_CLASS);
        assertNotNull(element);

        List <? extends AnnotationMirror> annotations = element.getAnnotationMirrors();
        assertEquals(2, annotations.size());

        assertJClass(toolkit.toJType(annotations.get(0)), Deprecated.class);
        assertJClass(toolkit.toJType(annotations.get(1)), Case.class);
    }

    @Test
    void toJTypeGeneric(Cases cases) {
        Element element = cases.one(CASE_ATTRIBUTE_GENERIC);
        assertNotNull(element);

        assertJClass(toolkit.toJType(element), new ClassDefinitionAssert(
                Map.class,
                new ClassDefinitionAssert(String.class),
                new ClassDefinitionAssert(Map.class, new ClassDefinitionAssert(String.class), new ClassDefinitionAssert(SampleTest.class))
        ));
    }

    @Test
    void extendClassSuperClassTest(Cases cases) {
        Element element = cases.one(CASE_SAMPLE_TEST_CLASS);
        assertNotNull(element);

        JDefinedClass klass = toolkit.extendFromClass("com.redhat.extendClassSuperClassTest", element);

        assertJClass(klass._extends(), SampleTest.class);
    }

    @Test
    void extendClassInnerClassTest(Cases cases) {
        Element element = cases.one(CASE_INNER_CLASS);
        assertNotNull(element);

        JDefinedClass klass = toolkit.extendFromClass("com.redhat.extendClassInnerClassTest", element);
        assertJClass(klass._extends(), SampleTest.InnerSampleTest.class);
    }

    @Test
    void overrideMethodTest(Cases cases) {
        Element element = cases.one(CASE_SAMPLE_TEST_CLASS);
        assertNotNull(element);

        JDefinedClass klass = toolkit.extendFromClass("com.redhat.extendClassSuperClassTest", element);
        assertTrue(klass.methods().isEmpty());

        Element fooMethod = cases.one(CASE_FOO_METHOD);
        assertNotNull(fooMethod);

        JMethod overridenMethod = toolkit.overrideMethod(klass, (ExecutableElement) fooMethod, List.of());

        assertEquals(1, klass.methods().size());
        assertEquals(klass.methods().stream().findFirst().get(), overridenMethod);

        // Return type
        assertEquals(codeModel.INT, overridenMethod.type());

        // Parameters
        assertEquals(3, overridenMethod.params().size());

        assertEquals("param0", overridenMethod.params().get(0).name());
        assertEquals(codeModel.FLOAT, overridenMethod.params().get(0).type());

        assertEquals("param1", overridenMethod.params().get(1).name());
        assertEquals(codeModel.ref(Object.class).fullName(), overridenMethod.params().get(1).type().fullName());

        assertEquals("param2", overridenMethod.params().get(2).name());
        assertEquals(codeModel.ref(SampleTest.class).fullName(), overridenMethod.params().get(2).type().fullName());

        // Annotations
        List<JAnnotationUse> annotations = List.copyOf(overridenMethod.annotations());
        assertEquals(3, annotations.size());
        assertEquals(codeModel.ref(Override.class).fullName(), annotations.get(0).getAnnotationClass().fullName());
        assertEquals(codeModel.ref(Deprecated.class).fullName(), annotations.get(1).getAnnotationClass().fullName());
        assertEquals(codeModel.ref(Case.class).fullName(), annotations.get(2).getAnnotationClass().fullName());
        assertEquals(1, annotations.get(2).getAnnotationMembers().size());
        assertEquals(CASE_FOO_METHOD, annotations.get(2).getAnnotationMembers().get("value").toString());
    }

    private void assertJClass(JType jType, Class<?> target) {
        assertJClass(jType, new ClassDefinitionAssert(target));
    }

    private void assertJClass(JType jType, ClassDefinitionAssert expectedClass) {
        String name = expectedClass.getFullName();

        assertEquals(name, jType.fullName());
        assertTrue(jType instanceof JClass);
        JClass jClass = (JClass) jType;
        assertEquals(expectedClass.params.length, jClass.getTypeParameters().size());

        for (int i = 0; i < expectedClass.params.length; ++i) {
            assertJClass(
                    jClass.getTypeParameters().get(i),
                    expectedClass.params[i]
            );
        }
    }

    private record ClassDefinitionAssert(
            Class<?> target,
            ClassDefinitionAssert... params
    ) {

        public String getFullName() {
                String name = target.getCanonicalName();
                if (params.length > 0) {
                    name += "<";
                    name += Arrays.stream(params).map(ClassDefinitionAssert::getFullName).collect(Collectors.joining(","));
                    name += ">";
                }

                return name;
            }
        }

}
