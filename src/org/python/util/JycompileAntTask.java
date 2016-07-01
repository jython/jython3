package org.python.util;

import org.apache.tools.ant.BuildException;
import org.python.Version;
import org.python.core.PyException;
import org.python.core.PySystemState;
import org.python.core.imp;
import org.python.modules._py_compile;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.Set;

/**
 * Compiles all python files in a directory to bytecode, and writes them to another directory,
 * possibly the same one.
 */
public class JycompileAntTask extends GlobMatchingTask {
    @Override
    public void execute() throws BuildException {
        checkParameters();
        toExpose.clear();

        for (String srcEntry : src.list()) {
            File srcDir = getProject().resolveFile(srcEntry);
            if (!srcDir.exists()) {
                throw new BuildException("srcdir '" + srcDir.getPath() + "' does not exist!",
                                         getLocation());
            }
            File[] files = srcDir.listFiles();
            filter(files);
        }
        process(toExpose);
    }

    private void filter(File[] files) {
        for (File src : files) {
            if (src.isDirectory()) {
                if (!src.toPath().endsWith(imp.PY_CACHE) && !src.toPath().endsWith("test")) {
                    filter(src.listFiles());
                }
                continue;
            }
            if (!src.getName().endsWith(".py")) {
                continue;
            }
            Path classPath = src.toPath().resolveSibling(
                    Paths.get(imp.PY_CACHE, src.getName().substring(0, src.getName().length() - 3) + Version.PY_CACHE_TAG + ".class"));
            File classFile = classPath.toFile();
            if (classFile.exists() && classFile.lastModified() > src.lastModified())
                continue;
            toExpose.add(src);
        }
    }

    public void process(Set<File> toCompile) throws BuildException {
        if (toCompile.size() == 0) {
            return;
        } else if (toCompile.size() > 1) {
            log("Compiling " + toCompile.size() + " files");
        } else if (toCompile.size() == 1) {
            log("Compiling 1 file");
        }
        Properties props = new Properties();
        props.setProperty(PySystemState.PYTHON_CACHEDIR_SKIP, "true");
        PySystemState.initialize(System.getProperties(), props);
        for (File src : toCompile) {
            try {
                String name = _py_compile.getModuleName(src);
                String compiledFilePath = name.replace('.', File.separatorChar);
                Path classPath = Paths.get(compiledFilePath);
                if (src.getName().endsWith("__init__.py")) {
                    classPath = classPath.resolve(Paths.get(imp.PY_CACHE, "__init__"));
                } else {
                    Path cache = Paths.get(imp.PY_CACHE, classPath.getFileName().toString());
                    if (classPath.getParent() == null) {
                        classPath = cache;
                    } else {
                        classPath = classPath.getParent().resolve(cache);
                    }
                }
                File compiled = Paths.get(destDir.toString(),
                        classPath.getParent().toString(),
                        classPath.getFileName().toString() + Version.PY_CACHE_TAG + ".class").toFile();
                compile(src, compiled, name);
            } catch (RuntimeException e) {
                log("Could not compile " + src);
                throw e;
            }
        }
    }

    /**
     * Compiles the python file <code>src</code> to bytecode filling in <code>moduleName</code> a
     * its name, and stores it in <code>compiled</code>. This is called by process for every file
     * that's compiled, so subclasses can override this method to affect or track the compilation.
     */
    protected void compile(File src, File compiled, String moduleName) {
        byte[] bytes;
        try {
            bytes = imp.compileSource(moduleName, src);
        } catch (PyException pye) {
            pye.printStackTrace();
            throw new BuildException("Compile failed; see the compiler error output for details.");
        }
        File dir = compiled.getParentFile();
        if (!dir.exists() && !compiled.getParentFile().mkdirs()) {
            throw new BuildException("Unable to make directory for compiled file: " + compiled);
        }
        imp.cacheCompiledSource(src.getAbsolutePath(), compiled.getAbsolutePath(), bytes);
    }
}
