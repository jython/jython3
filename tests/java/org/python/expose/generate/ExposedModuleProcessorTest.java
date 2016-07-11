package org.python.expose.generate;

import junit.framework.TestCase;
import org.apache.tools.ant.BuildException;
import org.objectweb.asm.ClassWriter;
import org.python.core.BytecodeLoader;
import org.python.core.PyLong;
import org.python.core.PyModule;
import org.python.core.PyObject;
import org.python.core.PyStringMap;
import org.python.core.PySystemState;
import org.python.core.PyUnicode;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;

import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;

/**
 * Created by isaiah on 7/7/16.
 */
public class ExposedModuleProcessorTest extends TestCase {

    public void setUp() {
        PySystemState.initialize();
    }

    public void testDump() throws Exception {
        InputStream in = getClass().getClassLoader()
                .getResourceAsStream("org/python/modules/SimpleModule.class");
        ExposedModuleProcessor ice = new ExposedModuleProcessor(in);
        assertEquals("hello", ice.getName());
        assertEquals(2, ice.getMethodExposers().size());
        for (MethodExposer me : ice.getMethodExposers()) {
            assertTrue(me instanceof ClassMethodExposer);
        }

        BytecodeLoader.Loader loader = new BytecodeLoader.Loader();
        Class simple_method = null;
        for(MethodExposer exposer : ice.getMethodExposers()) {
            if(exposer.getNames()[0].equals("xxx")) {
                simple_method = exposer.load(loader);
            } else {
                exposer.load(loader);
            }
        }
        Class doctoredSimple = ice.getModuleExposer().load(loader);

        PyModule module = new PyModule("hello", new PyStringMap());
        Method clinit = doctoredSimple.getMethod("clinic", PyModule.class);
        clinit.invoke(null, module);
        assertTrue(module.__findattr__("__name__").__eq__(new PyUnicode("hello")).__bool__());
//        assertTrue(module.__dict__.__finditem__("xxx").__findattr__("__name__").__eq__(new PyUnicode("xxx")).__bool__());
        assertNotNull("const TIMES is not found", module.__findattr__("times"));
        assertTrue(module.__findattr__("times").__eq__(new PyLong(1)).__bool__());
        assertNotNull("const SPACES is not found", module.__findattr__("spaces"));
        assertTrue(module.__findattr__("spaces").__eq__(new PyUnicode("ss")).__bool__());
        module.__findattr__("xxx").__call__();
        PyObject m_yyy = module.__getattr__("yyy");
        assertTrue(m_yyy.__call__(new PyUnicode("aaa")).__eq__(new PyUnicode("aaa")).__bool__());
    }
}
