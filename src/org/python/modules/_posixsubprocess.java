package org.python.modules;

import jnr.constants.platform.Fcntl;
import jnr.posix.POSIX;
import jnr.posix.SpawnAttribute;
import jnr.posix.SpawnFileAction;
import jnr.posix.util.Platform;
import org.python.core.ArgParser;
import org.python.core.Py;
import org.python.core.PyBytes;
import org.python.core.PyDictionary;
import org.python.core.PyList;
import org.python.core.PyLong;
import org.python.core.PyObject;
import org.python.core.PySequence;
import org.python.expose.ExposedFunction;
import org.python.expose.ExposedModule;
import org.python.modules._io.PyFileIO;
import org.python.modules.posix.PosixModule;
import org.python.util.FilenoUtil;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.Channel;
import java.nio.channels.Pipe;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ExposedModule(doc = "A POSIX helper for the subprocess module.")
public class _posixsubprocess {
    private static final POSIX posix = PosixModule.getPOSIX();

    // subprocess_fork_exec
//    public static final PyObject fork_exec(PyObject process_args, PyObject executable_list,
//                                     PyObject close_fds, PyObject fds_to_keep, PyObject cwd, PyObject env,
//                                     PyObject p2cread, PyObject p2cwrite, PyObject c2pread, PyObject c2pwrite,
//                                     PyObject errread, PyObject errwrite, PyObject errpipe_read, PyObject errpipe_write,
//                                     PyObject restore_signals, PyObject call_setsid, PyObject preexec_fn) {
    @ExposedFunction
    public static final PyObject fork_exec(PyObject[] args, String[] keywords) {
        ArgParser ap = new ArgParser("fork_exec", args, keywords, "args", "executable", "close_fds",
                "fds_to_keep", "cwd", "env", "p2cread", "p2cwrite", "c2pread", "c2pwrite",
                "errread", "errwrite", "errpipe_read", "errpipe_write", "restore_signals", "call_setsid", "preexec_fn");
        PyObject mOne = new PyLong(-1);
        PyObject processArgs = ap.getPyObject(0);
        String[] pbArgs = new String[processArgs.__len__()];
        String cwd = ap.getString(4);
        PyObject env = ap.getPyObject(5);
        PyObject p2cread = ap.getPyObject(7);
        PyObject c2pwrite = ap.getPyObject(9);
        PyObject errwrite = ap.getPyObject(11);
        PyObject errpipeWrite = ap.getPyObject(13);
        int i = 0;
        for (PyObject arg : processArgs.asIterable()) {
            pbArgs[i++] = arg.toString();
        }
        ProcessBuilder pb = new ProcessBuilder(pbArgs);
        Map<String, String> environ = pb.environment();
        Map<?, ?> map;
        if (env instanceof PyDictionary) {
            map = ((PyDictionary) env).getMap();
        } else {
            map = new HashMap<>();
        }
        for (Object key : map.keySet()) {
            environ.put((String) key, (String) map.get(key));
        }
        if (cwd != null) {
            pb.directory(new File(cwd));
        }
        if (p2cread.equals(mOne)) {
            pb.redirectInput(ProcessBuilder.Redirect.INHERIT);
        } else {
            pb.redirectInput(new File(p2cread.toString()));
        }
        if (c2pwrite.equals(mOne)) {
            pb.redirectOutput(ProcessBuilder.Redirect.INHERIT);
        } else {
            pb.redirectOutput(new File(c2pwrite.toString()));
        }
        if (errwrite.equals(mOne)) {
            pb.redirectError(ProcessBuilder.Redirect.INHERIT);
        } else {
            pb.redirectError(new File(errwrite.toString()));
        }
//        pb.inheritIO();
        try {
            Process process = pb.start();
            return Py.java2py(process);
        } catch (IOException e) {
            ((PyFileIO) errpipeWrite).getRawIO().write(ByteBuffer.wrap(e.getMessage().getBytes()));
            return Py.None;
        }
//        PyList argv = new PyList(process_args);
//        PyList envp;
////        String[] command = new String[argv.size() + 1];
////        System.arraycopy(argv.toArray(), 0, command, 1, argv.size());
////        command[0] = exec_array.__getitem__(0).asString();
//        ExecArg eargp = new ExecArg();
//        PyObject m1 = Py.newLong(-1);
//
//        if (p2cread != m1) {
//            eargp.fileActions.add(SpawnFileAction.dup(FilenoUtil.filenoFrom(p2cread), 0));
//            eargp.fileActions.add(SpawnFileAction.close(FilenoUtil.filenoFrom(p2cwrite)));
//        }
//        if (c2pread != m1) {
//            eargp.fileActions.add(SpawnFileAction.dup(FilenoUtil.filenoFrom(c2pwrite), 1));
//            eargp.fileActions.add(SpawnFileAction.close(FilenoUtil.filenoFrom(c2pread)));
//        }
//        if (errread != m1) {
//            eargp.fileActions.add(SpawnFileAction.dup(FilenoUtil.filenoFrom(errwrite), 2));
//            eargp.fileActions.add(SpawnFileAction.close(FilenoUtil.filenoFrom(errread)));
//        }
//
//        POSIX posix = PosixModule.getPOSIX();
//        eargp.command_name = (PyBytes) exec_array.__getitem__(0);
//        long status = posix.posix_spawnp(
//                eargp.command_name.asString(),
//                eargp.fileActions,
//                Arrays.asList((String[]) argv.toArray(new String[0])),
//                Collections.EMPTY_LIST);
//
//        return Py.newLong(status);
    }
}
