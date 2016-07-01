package org.python.expose.generate;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Set;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.util.GlobPatternMapper;
import org.apache.tools.ant.util.SourceFileScanner;
import org.objectweb.asm.ClassWriter;
import org.python.core.Py;
import org.python.core.Options;
import org.python.util.GlobMatchingTask;

public class ExposeTask extends GlobMatchingTask {
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
            String[] files = getDirectoryScanner(srcDir).getIncludedFiles();
            filter(srcDir, files);
        }
        process(toExpose);
    }

    protected void filter(File srcDir, String[] files) {
        GlobPatternMapper m = new GlobPatternMapper();
        m.setFrom("*.class");
        m.setTo("*.class");
        SourceFileScanner sfs = new SourceFileScanner(this);
        for (File file : sfs.restrictAsFiles(files, srcDir, destDir, m)) {
            toExpose.add(file);
        }
    }

    public void process(Set<File> toExpose) throws BuildException {
        if (toExpose.size() > 1) {
            log("Exposing " + toExpose.size() + " classes");
        } else if (toExpose.size() == 1) {
            log("Exposing 1 class");
        }

        // Quiet harmless unbootstrapped warnings during the expose process
        int verbose = Options.verbose;
        Options.verbose = Py.ERROR;
        try {
            expose(toExpose);
        } finally {
            Options.verbose = verbose;
        }
    }

    private void expose(Set<File> toExpose) {
        for (File f : toExpose) {
            ExposedTypeProcessor etp;
            try {
                etp = new ExposedTypeProcessor(new FileInputStream(f));
            } catch (IOException e) {
                throw new BuildException("Unable to read '" + f + "' to expose it", e);
            } catch (InvalidExposingException iee) {
                throw new BuildException(iee.getMessage());
            }
            for (MethodExposer exposer : etp.getMethodExposers()) {
                generate(exposer);
            }
            for (DescriptorExposer exposer : etp.getDescriptorExposers()) {
                generate(exposer);
            }
            if (etp.getNewExposer() != null) {
                generate(etp.getNewExposer());
            }
            generate(etp.getTypeExposer());
            write(etp.getExposedClassName(), etp.getBytecode());
        }
    }

    private void generate(Exposer exposer) {
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
        exposer.generate(writer);
        write(exposer.getClassName(), writer.toByteArray());
    }

    private void write(String destClass, byte[] newClassfile) {
        File dest = new File(destDir, destClass.replace('.', '/') + ".class");
        dest.getParentFile().mkdirs();// TODO - check for success
        FileOutputStream out = null;
        try {
            out = new FileOutputStream(dest);
            out.write(newClassfile);
        } catch (IOException e) {
            throw new BuildException("Unable to write to '" + dest + "'", e);
        } finally {
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e) {
                    // Le sigh...
                }
            }
        }
    }
}
