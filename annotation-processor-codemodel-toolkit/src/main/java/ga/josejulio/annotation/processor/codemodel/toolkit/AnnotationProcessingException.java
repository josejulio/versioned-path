package ga.josejulio.annotation.processor.codemodel.toolkit;

public class AnnotationProcessingException extends RuntimeException {

    public AnnotationProcessingException(String message) {
        super(message);
    }

    public AnnotationProcessingException(String message, Exception cause) {
        super(message, cause);
    }
}
