/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package filemanagerLogic.fileStructure;

import filemanagerGUI.FileManagerLB;
import filemanagerLogic.Enums.Identity;
import filemanagerLogic.*;
import java.io.File;
import java.nio.file.*;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.function.Predicate;
import javafx.beans.property.*;
import javafx.util.Callback;
import lt.lb.commons.ArrayOp;
import lt.lb.commons.threads.RepeatableTask;
import utility.ErrorReport;
import utility.PathStringCommands;

/**
 *
 * @author Lemmin
 */
public class ExtPath {

    public static Predicate<ExtPath> EXISTS = path -> (ArrayOp.any(
                                                       Predicate.isEqual(path.getIdentity()), Identity.FILE, Identity.FOLDER, Identity.LINK)
                                                       && Files.exists(path.toPath()));

    public static final Comparator<String> COMPARE_SIZE_STRING = (String f1, String f2) -> {
        if (f1.isEmpty() || f2.isEmpty()) {
            return f1.compareTo(f2);
        }
        return ExtPath.extractSize(f1).compareTo(ExtPath.extractSize(f2));
    };

    public static Double extractSize(String s) {
        Long multiplier = Enums.DATA_SIZE.B.size;
        if (s.startsWith("(B)")) {
            s = s.replace("(B) ", "");
        } else if (s.startsWith("(KB)")) {
            s = s.replace("(KB) ", "");
            multiplier = Enums.DATA_SIZE.KB.size;
        } else if (s.startsWith("(MB)")) {
            s = s.replace("(MB) ", "");
            multiplier = Enums.DATA_SIZE.MB.size;
        } else if (s.startsWith("(GB)")) {
            s = s.replace("(GB) ", "");
            multiplier = Enums.DATA_SIZE.GB.size;
        } else {
            try {
                return Double.parseDouble(s);
            } catch (NumberFormatException ex) {
                ErrorReport.report(ex);
            }
            return 0d;
        }
        return Double.parseDouble(s) * multiplier;
    }

    public static Predicate<ExtPath> IS_FOLDER = new Predicate<ExtPath>() {
        @Override
        public boolean test(ExtPath t) {
            return t.getIdentity().equals(Enums.Identity.FOLDER);
        }
    };

    public static Predicate<ExtPath> IS_FILE = new Predicate<ExtPath>() {
        @Override
        public boolean test(ExtPath t) {
            return t.getIdentity().equals(Enums.Identity.FILE);
        }
    };

    public static Predicate<ExtPath> IS_NOT_DISABLED = new Predicate<ExtPath>() {
        @Override
        public boolean test(ExtPath t) {
            return !t.isDisabled.get();
        }
    };

    private Path path;
    private final String absolutePath;
    private long size = -1;
    private long lastModified = -1;
    public BooleanProperty isVirtual;
    public BooleanProperty isAbsoluteRoot;
    public BooleanProperty isDisabled;
    public StringProperty propertyName;
    public StringProperty propertyType;
    public LongProperty propertySize;
    public LongProperty propertyLastModified;
    public StringProperty propertyDate;
    public StringProperty propertySizeAuto;
    public LongProperty readyToUpdate;

    boolean sizeTaskComplete = true;
    boolean dateTaskComplete = true;
    private Runnable getSizeTask = new RepeatableTask(new Callable() {
        @Override
        public Object call() throws Exception {
//                Log.write("getSizeTask" ,absolutePath);
            size = Files.size(toPath());
            propertySize.set(size);
            sizeTaskComplete = true;
            return null;
        }
    });

    private Runnable getDateTask = new RepeatableTask(new Callable() {
        @Override
        public Void call() throws Exception {
//                Log.write("getDateTask ",absolutePath);
            lastModified = Files.getLastModifiedTime(toPath()).toMillis();
            propertyLastModified.set(lastModified);
            dateTaskComplete = true;
            return null;
        }
    });

    public ExtPath(String str, Object... optional) {
        str = str.trim();
        if (str.endsWith(File.separator)) {
            str = str.substring(0, str.length() - 1);
        }
        this.absolutePath = str;
        init();
        if (optional.length > 0) {
            this.path = (Path) optional[0];
        }

    }

    public Path toPath() {
        if (this.path == null) {
            this.path = Paths.get(this.getAbsoluteDirectory());
        }
        return this.path;
//        return Paths.get(this.getAbsoluteDirectory());
    }

    private void init() {
        this.propertyName = new SimpleStringProperty(this.getName(true));
        this.propertyType = new SimpleStringProperty(this.getIdentity().toString());
        this.isDisabled = new SimpleBooleanProperty(false);

        this.propertySize = new SimpleLongProperty() {
            @Override
            public long get() {
                if (sizeTaskComplete) {
                    sizeTaskComplete = false;
                    TaskFactory.mainExecutor.submit(getSizeTask);
                }

                return size;

            }
        };
        this.propertyLastModified = new SimpleLongProperty() {
            @Override
            public long get() {
                if (dateTaskComplete) {
                    dateTaskComplete = false;
                    TaskFactory.mainExecutor.submit(getDateTask);
                }

                return lastModified;

            }
        };
        this.propertyDate = new SimpleStringProperty() {
            @Override
            public String get() {
                if (propertyLastModified.get() == -1) {
                    return "LOADING";
                }
                return new SimpleDateFormat("YYYY-MM-dd HH:mm:ss").format(Date.from(Instant.ofEpochMilli(propertyLastModified.get())));
            }
        };
        this.propertySizeAuto = new SimpleStringProperty() {
            @Override
            public String get() {
                if (propertySize.get() == -1) {
                    return "LOADING";
                }
                String stringSize = propertySize.asString().get();

                Double size = Double.valueOf(stringSize);
                String sizeType = "B";
                if (size >= 1024) {
                    size = size / 1024;
                    sizeType = "KB";
                }
                if (size >= 1024) {
                    size = size / 1024;
                    sizeType = "MB";
                }
                if (size >= 1024) {
                    size = size / 1024;
                    sizeType = "GB";
                }
                stringSize = String.valueOf(size);
                int indexOf = stringSize.indexOf('.');

                return ("(" + sizeType + ") " + stringSize.substring(0, Math.min(stringSize.length(), indexOf + 3))); //To change body of generated methods, choose Tools | Templates.
            }

        };
        this.isAbsoluteRoot = new SimpleBooleanProperty(false);
        this.isVirtual = new SimpleBooleanProperty(getIdentity().equals(Identity.VIRTUAL));
    }

    public Collection<ExtPath> getListRecursive(boolean applyDisable) {
        ArrayList<ExtPath> list = new ArrayList<>();
        if (!applyDisable || !this.isDisabled.get()) {
            list.add(this);
        }
        return list;
    }

    public Collection<ExtPath> getListRecursive(Predicate<ExtPath> predicate) {
        ArrayDeque<ExtPath> list = new ArrayDeque<>();
        if (predicate.test(this)) {
            list.add(this);
        }
        return list;
    }

    public void collectRecursive(Predicate<ExtPath> predicate, Callback<ExtPath, Void> call) {
        if (predicate.test(this)) {
            call.call(this);
        }
    }

    public boolean isRoot() {
        return FileManagerLB.getRootSet().contains(this.getAbsoluteDirectory());
    }

    public String getAbsoluteDirectory() {
        return this.getAbsolutePath();
    }

    public String getAbsolutePath() {
        return absolutePath;
    }

    public LocationInRoot getMapping() {
        return new LocationInRoot(this.getAbsoluteDirectory());
    }

    public Enums.Identity getIdentity() {
        return Enums.Identity.FILE;
    }

    public void setIsAbsoluteRoot(boolean b) {
        this.isAbsoluteRoot.set(b);
    }

    public boolean isNotWriteable() {
        return (this.isAbsoluteRoot.get() || (this.equals(FileManagerLB.VirtualFolders)));
    }

    public long size() {
        long get = this.propertySize.get();
        if (get == -1) {
            try {
                get = Files.size(toPath());
            } catch (Exception ex) {
                ErrorReport.report(ex);
            }
        }
        return get;
    }

    public long lastModified() {
        long get = this.propertyLastModified.get();
        if (get == -1) {
            try {
                get = Files.getLastModifiedTime(toPath()).toMillis();
            } catch (Exception ex) {
                ErrorReport.report(ex);
            }
        }
        return get;
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 17 * hash + Objects.hashCode(this.absolutePath);
        return hash;
    }

    @Override
    public boolean equals(Object e) {
        if (e == null) {
            return false;
        }
        if (!(e instanceof ExtPath)) {
            return false;
        } else {
            ExtPath p = (ExtPath) e;
            return this.absolutePath.equals(p.getAbsolutePath());
        }

    }

    public String getName(boolean extension) {
        return new PathStringCommands(this.absolutePath).getName(extension);
    }

    public String getExtension() {
        return new PathStringCommands(this.absolutePath).getExtension();
    }

    public String getParent(int timesToGoUp) {
        return new PathStringCommands(this.absolutePath).getParent(timesToGoUp);
    }

    public String relativeFrom(String possibleParent) {
        return new PathStringCommands(absolutePath).relativePathFrom(possibleParent);
    }

    public String relativeTo(String possibleChild) {
        return new PathStringCommands(absolutePath).relativePathTo(possibleChild);
    }

    public PathStringCommands getPathCommands() {
        return new PathStringCommands(absolutePath);
    }

    @Override
    public String toString() {
        return this.getAbsolutePath();
    }
}
