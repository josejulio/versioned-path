package ga.josejulio.annotation.processor.codemodel.toolkit;

import com.sun.codemodel.CodeWriter;
import com.sun.codemodel.JPackage;

import javax.annotation.processing.Filer;
import javax.tools.JavaFileObject;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

public class AnnotationProcessingCodeWriter extends CodeWriter {
    private final Filer filer;
    private final Map<String, OutputStream> openedOutputStreams = new HashMap<>();

    public AnnotationProcessingCodeWriter(Filer filer) {
        this.filer = filer;
    }

    @Override
    public OutputStream openBinary(JPackage jPackage, String name) throws IOException {
        final int DOT_JAVA_LENGTH = 5; // ".java".length()

        String className = jPackage.name().isBlank() ? name : jPackage.name() + "." + name.substring(0, name.length() - DOT_JAVA_LENGTH);

        if (!openedOutputStreams.containsKey(className)) {
            JavaFileObject javaFileObject = filer.createSourceFile(className);
            openedOutputStreams.put(
                    className,
                    javaFileObject.openOutputStream()
            );
        }

        return openedOutputStreams.get(className);
    }

    @Override
    public void close() throws IOException {
        for (OutputStream outputStream: openedOutputStreams.values()) {
            outputStream.flush();
            outputStream.close();
        }
    }
}
