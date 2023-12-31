package ga.josejulio.versioned.path;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(value = {ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface VersionedPath {
    String path(); // /api/foobar/$version/endpoints
    String sinceVersion() default ""; // Version number
    // Todo: Add removedOn version
}
