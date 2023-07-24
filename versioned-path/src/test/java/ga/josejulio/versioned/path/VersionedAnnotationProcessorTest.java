package ga.josejulio.versioned.path;

import com.karuslabs.elementary.junit.Labels;
import com.karuslabs.elementary.junit.Tools;
import com.karuslabs.elementary.junit.ToolsExtension;
import com.karuslabs.elementary.junit.annotations.Introspect;
import com.karuslabs.elementary.junit.annotations.Label;
import com.sun.codemodel.JAnnotatable;
import com.sun.codemodel.JAnnotationUse;
import com.sun.codemodel.JCodeModel;
import com.sun.codemodel.JDefinedClass;
import com.sun.codemodel.JMethod;
import com.sun.codemodel.writer.SingleStreamCodeWriter;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import javax.lang.model.element.Element;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@ExtendWith(ToolsExtension.class)
@Introspect
public class VersionedAnnotationProcessorTest {

    private static final String CASE_SAMPLE_CLASS = "SAMPLE_CLASS";

    @VersionedPath(sinceVersion = "1.0", path = "/api/$version/")
    @Label(CASE_SAMPLE_CLASS)
    class Sample {

        @VersionedMethod(value = VersionedMethod.HttpMethod.GET)
        public int common() {
            return 5;
        }

        @POST
        @Path("shared")
        public float shared() {
            return 3.14f;
        }

        @VersionedPath(sinceVersion = "1.0", path = "stuff")
        @VersionedMethod(VersionedMethod.HttpMethod.PUT)
        public int v1Stuff() {
            return 1;
        }

        @VersionedPath(sinceVersion = "2.0", path = "stuff")
        @VersionedMethod(VersionedMethod.HttpMethod.PUT)
        public float v2Stuff() {
            return 2.0f;
        }

        @VersionedPath(sinceVersion = "3.1", path = "stuff")
        @VersionedMethod(VersionedMethod.HttpMethod.PUT)
        public double v3Stuff(int param) {
            return 3.0;
        }

    }

    @Test
    public void classWithMultipleVersionsTest(Labels labels) throws IOException {
        JCodeModel codeModel = new JCodeModel();
        VersionedAnnotationProcessor versionedAnnotationProcessor = new VersionedAnnotationProcessor();
        versionedAnnotationProcessor.init(codeModel, Tools.elements(), Tools.messager(), Tools.filer(), Tools.types());

        Element element = labels.get(CASE_SAMPLE_CLASS);
        assertNotNull(element);

        List<JDefinedClass> createdClasses = versionedAnnotationProcessor.processClass(element);

        assertEquals(3, createdClasses.size());

        assertNotNull(codeModel.directClass("ga.josejulio.versioned.path.VersionedAnnotationProcessorTest_SampleV1_0"));
        assertNotNull(codeModel.directClass("ga.josejulio.versioned.path.VersionedAnnotationProcessorTest_SampleV2_0"));
        assertNotNull(codeModel.directClass("ga.josejulio.versioned.path.VersionedAnnotationProcessorTest_SampleV3_1"));

        // V1 Class
        {
            JDefinedClass v1Class = getClassNamed(createdClasses, "ga.josejulio.versioned.path.VersionedAnnotationProcessorTest_SampleV1_0");
            JAnnotationUse annotation = getAnnotation(v1Class, Path.class);
            assertEquals("/api/1.0/", annotation.getAnnotationMembers().get("value").toString());

            assertEquals(2, v1Class.methods().size());

            JMethod method = getMethodNamed(v1Class.methods(), "v1Stuff");
            assertNotNull(method);
            assertEquals(codeModel.INT.fullName(), method.type().fullName());
            annotation = getAnnotation(method, Path.class);
            assertEquals("stuff", annotation.getAnnotationMembers().get("value").toString());
            assertNotNull(getAnnotation(method, PUT.class));

            method = getMethodNamed(v1Class.methods(), "common");
            assertNotNull(method);
            assertEquals(codeModel.INT.fullName(), method.type().fullName());
            getAnnotation(method, Path.class, false);
            assertNotNull(getAnnotation(method, GET.class));
        }

        // V2 Class
        {
            JDefinedClass v2Class = getClassNamed(createdClasses, "ga.josejulio.versioned.path.VersionedAnnotationProcessorTest_SampleV2_0");
            JAnnotationUse annotation = getAnnotation(v2Class, Path.class);
            assertEquals("/api/2.0/", annotation.getAnnotationMembers().get("value").toString());

            assertEquals(2, v2Class.methods().size());

            JMethod method = getMethodNamed(v2Class.methods(), "v2Stuff");
            assertNotNull(method);
            assertEquals(codeModel.FLOAT.fullName(), method.type().fullName());
            annotation = getAnnotation(method, Path.class);
            assertEquals("stuff", annotation.getAnnotationMembers().get("value").toString());
            assertNotNull(getAnnotation(method, PUT.class));

            method = getMethodNamed(v2Class.methods(), "common");
            assertNotNull(method);
            assertEquals(codeModel.INT.fullName(), method.type().fullName());
            getAnnotation(method, Path.class, false);
            assertNotNull(getAnnotation(method, GET.class));
        }

        // V3.1 Class
        {
            JDefinedClass v3Class = getClassNamed(createdClasses, "ga.josejulio.versioned.path.VersionedAnnotationProcessorTest_SampleV3_1");
            JAnnotationUse annotation = getAnnotation(v3Class, Path.class);
            assertEquals("/api/3.1/", annotation.getAnnotationMembers().get("value").toString());

            assertEquals(2, v3Class.methods().size());

            JMethod method = getMethodNamed(v3Class.methods(), "v3Stuff");
            assertNotNull(method);
            assertEquals(codeModel.DOUBLE.fullName(), method.type().fullName());
            annotation = getAnnotation(method, Path.class);
            assertEquals("stuff", annotation.getAnnotationMembers().get("value").toString());
            assertNotNull(getAnnotation(method, PUT.class));

            method = getMethodNamed(v3Class.methods(), "common");
            assertNotNull(method);
            assertEquals(codeModel.INT.fullName(), method.type().fullName());
            getAnnotation(method, Path.class, false);
            assertNotNull(getAnnotation(method, GET.class));
        }

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        codeModel.build(new SingleStreamCodeWriter(byteArrayOutputStream));
        String result = byteArrayOutputStream.toString(StandardCharsets.UTF_8);

        assertEquals(IOUtils.toString(getClass().getResourceAsStream("/source/SampleClass")), result);
    }

    private JDefinedClass getClassNamed(Collection<JDefinedClass> classList, String name) {
        return classList
                .stream()
                .filter(klass -> klass.fullName().equals(name))
                .findFirst()
                .get();
    }

    private JMethod getMethodNamed(Collection<JMethod> methods, String name) {
        return methods
                .stream()
                .filter(method -> method.name().equals(name))
                .findFirst()
                .get();
    }

    private JAnnotationUse getAnnotation(JAnnotatable annotatable, Class<? extends Annotation> annotationClass) {
        return getAnnotation(annotatable, annotationClass, true);
    }

    private JAnnotationUse getAnnotation(JAnnotatable annotatable, Class<? extends Annotation> annotationClass, boolean isPresent) {
        Optional<JAnnotationUse> annotation = annotatable
                .annotations()
                .stream()
                .filter(a -> a.getAnnotationClass().fullName().equals(annotationClass.getCanonicalName()))
                .findFirst();

        assertEquals(isPresent, annotation.isPresent());
        if (!isPresent) {
            return null;
        }

        return annotation.get();
    }



}
