package org.python.expose.generate;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.objectweb.asm.ClassWriter;
import org.python.modules.Setup;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Created by isaiah on 7/11/16.
 */
public class ModuleExposeTask extends Task {
    public File getDestDir() {
        return destDir;
    }

    public void setDestDir(File destDir) {
        this.destDir = destDir;
    }

    protected File destDir;

    @Override
    public void execute() throws BuildException {
        for (String mod : Setup.newbuiltinModules) {
            InputStream in = getClass().getClassLoader()
                    .getResourceAsStream(className(mod).replace('.', '/') + ".class");
            ExposedModuleProcessor ice = new ExposedModuleProcessor(in);
            for(MethodExposer exposer : ice.getMethodExposers()) {
                generate(exposer);
            }
            generate(ice.getModuleExposer());
        }
    }

    private String className(String name) {
        String classname;
        String modname;

        int colon = name.indexOf(':');
        if (colon != -1) {
            // name:fqclassname
            modname = name.substring(0, colon).trim();
            classname = name.substring(colon + 1, name.length()).trim();
            if (classname.equals("null")) {
                // name:null, i.e. remove it
                classname = null;
            }
        } else {
            modname = name.trim();
            classname = "org.python.modules." + modname;
        }
        return classname;
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
