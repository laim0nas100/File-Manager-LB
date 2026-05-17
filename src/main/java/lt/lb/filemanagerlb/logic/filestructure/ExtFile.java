package lt.lb.filemanagerlb.logic.filestructure;

import java.io.File;
import java.util.Objects;

/**
 *
 * @author Lemmin
 */
public class ExtFile extends File {

    public final ExtPath path;

    public ExtFile(ExtPath path) {
        super(Objects.requireNonNull(path).getAbsolutePath());
        this.path = path;
    }
}
