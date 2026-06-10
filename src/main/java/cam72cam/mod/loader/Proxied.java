package cam72cam.mod.loader;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface Proxied {
    /**
     * Used for referring implementation class, a <code>NoClassDefError</code> will be raised if the target class is not present.
     */
    String target();

    /**
     * If the class is annotated, set this to false on methods to disable proxy.
     * TODO
     */
    boolean enabled() default true;
}
