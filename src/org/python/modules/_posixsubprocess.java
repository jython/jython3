package org.python.modules;

import jnr.constants.platform.Fcntl;
import jnr.posix.POSIX;
import jnr.posix.SpawnAttribute;
import jnr.posix.SpawnFileAction;
import jnr.posix.util.Platform;
import org.python.core.ClassDictInit;
import org.python.core.Py;
import org.python.core.PyBytes;
import org.python.core.PyList;
import org.python.core.PyObject;
import org.python.core.PyUnicode;
import org.python.modules.posix.PosixModule;
import org.python.util.FilenoUtil;

import java.io.IOException;
import java.nio.channels.Channel;
import java.nio.channels.Pipe;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Created by isaiah on 6/12/16.
 */
public class _posixsubprocess implements ClassDictInit {
    public static final PyUnicode __doc__ = new PyUnicode("A POSIX helper for the subprocess module.");

    private static final POSIX posix = PosixModule.getPOSIX();

    public static void classDictInit(PyObject dict) {
        dict.__setitem__("__name__", new PyUnicode("_posixsubprocess"));
        dict.__setitem__("__doc__", __doc__);

        // Hide from Python
        dict.__setitem__("classDictInit", null);
    }

    // subprocess_fork_exec
    public static PyObject fork_exec(PyObject process_args, PyObject executable_list,
                                     boolean close_fds, PyObject fds_to_keep, PyObject cwd, PyObject env,
                                     PyObject p2cread, PyObject p2cwrite, PyObject c2pread, PyObject c2pwrite,
                                     PyObject errread, PyObject errwrite, PyObject errpipe_read, PyObject errpipe_write,
                                     boolean restore_signals, boolean call_setsid, PyObject preexec_fn) throws IOException {
        PyList exec_array = new PyList(executable_list), argv = new PyList(process_args), envp;
//        String[] command = new String[argv.size() + 1];
//        System.arraycopy(argv.toArray(), 0, command, 1, argv.size());
//        command[0] = exec_array.__getitem__(0).asString();
        ExecArg eargp = new ExecArg();
        PyObject m1 = Py.newLong(-1);

        if (p2cread != m1) {
            eargp.fileActions.add(SpawnFileAction.dup(FilenoUtil.filenoFrom(p2cread), 0));
            eargp.fileActions.add(SpawnFileAction.close(FilenoUtil.filenoFrom(p2cwrite)));
        }
        if (c2pread != m1) {
            eargp.fileActions.add(SpawnFileAction.dup(FilenoUtil.filenoFrom(c2pwrite), 1));
            eargp.fileActions.add(SpawnFileAction.close(FilenoUtil.filenoFrom(c2pread)));
        }
        if (errread != m1) {
            eargp.fileActions.add(SpawnFileAction.dup(FilenoUtil.filenoFrom(errwrite), 2));
            eargp.fileActions.add(SpawnFileAction.close(FilenoUtil.filenoFrom(errread)));
        }

        POSIX posix = PosixModule.getPOSIX();
        eargp.command_name = (PyBytes) exec_array.__getitem__(0);
        long status = posix.posix_spawnp(
                eargp.command_name.asString(),
                eargp.fileActions,
                Arrays.asList((String[]) argv.toArray(new String[0])),
                Collections.EMPTY_LIST);

        return Py.newLong(status);
    }

//    private static ExecArg newExecArg(PyObject prog, PyList args) {
//        ExecArg eargp = new ExecArg();
//        Channel[] mainPipe = null, secondPipe = null;
//        if ((secondPipe = pipe()) == null) {
//            throw Py.SystemError(String.format("cannot spawn %s", prog));
//        }
//        if ((mainPipe = pipe()) == null) {
//            try {secondPipe[1].close();} catch (IOException ioe) {}
//            try {secondPipe[0].close();} catch (IOException ioe) {}
////            throw Py.SystemError(posix.errno, prog.toString());
//            throw Py.SystemError(String.format("cannot spawn %s", prog));
//        }
//
//        if (eargp != null) prepareStdioRedirects(mainPipe, secondPipe, eargp);
//        return eargp;
//    }
//
//    private void prepareStdioRedirects(Channel[] readPipe, Channel[] writePipe, ExecArg eargp) {
//        // We insert these redirects directly into fd_dup2 so that chained redirection can be
//        // validated and set up properly by the execargFixup logic.
//        // The closes do not appear to be part of MRI's logic (they close the fd before exec/spawn),
//        // so rather than using execargAddopt we do them directly here.
//        eargp.fd_dup2 = new PyList();
//
//        if (readPipe != null) {
//            // dup our read pipe's write end into stdout
//            int readPipeWriteFD = FilenoUtil.filenoFrom(readPipe[1]);
//            eargp.fd_dup2 = checkExecRedirect1(eargp.fd_dup2, 1, readPipeWriteFD);
//            eargp.fd_dup2.__add__()
//
//            // close the other end of the pipe in the child
//            int readPipeReadFD = FilenoUtil.filenoFrom(readPipe[0]);
//            eargp.fileActions.add(SpawnFileAction.close(readPipeReadFD));
//        }
//
//        if (writePipe != null) {
//            // dup our write pipe's read end into stdin
//            int writePipeReadFD = FilenoUtil.filenoFrom(writePipe[0]);
//            eargp.fd_dup2 = checkExecRedirect1(eargp.fd_dup2, 0, writePipeReadFD);
//
//            // close the other end of the pipe in the child
//            int writePipeWriteFD = FilenoUtil.filenoFrom(writePipe[1]);
//            eargp.fileActions.add(SpawnFileAction.close(writePipeWriteFD));
//        }
//    }

    // MRI: check_exec_redirect_fd
//    static PyObject checkExecRedirectFd(PyObject v, boolean iskey) {
//        PyObject tmp;
//        int fd;
//        if (v instanceof PyLong) {
//            fd = RubyNumeric.fix2int(v);
//        }
//        else if (v instanceof PyBytes) {
//            String id = v.toString();
//            if (id.equals("in"))
//                fd = 0;
//            else if (id.equals("out"))
//                fd = 1;
//            else if (id.equals("err"))
//                fd = 2;
//            else
//                throw Py.TypeError("wrong exec redirect");
//        }
//        else if (!(tmp = TypeConverter.convertToTypeWithCheck(v, runtime.getIO(), "to_io")).isNil()) {
//            OpenFile fptr;
//            fptr = ((RubyIO)tmp).getOpenFileChecked();
//            if (fptr.tiedIOForWriting != null)
//                throw runtime.newArgumentError("duplex IO redirection");
//            fd = fptr.fd().bestFileno();
//        }
//        else {
//            throw Py.TypeError("wrong exec redirect");
//        }
//        if (fd < 0) {
//            throw Py.TypeError("negative file descriptor");
//        }
//        else if (Platform.IS_WINDOWS && fd >= 3 && iskey) {
//            throw Py.TypeError("wrong file descriptor (" + fd + ")");
//        }
//        return Py.newLong(fd);
//    }
//
//    // MRI: check_exec_redirect1
//    static PyObject checkExecRedirect1(PyObject ary, int key, int param) {
//        if (ary == null) {
//            ary = new PyList();
//        }
//        if ((key instanceof PyList)) {
//            int i, n=0;
//            for (i = 0 ; i < ((PyList)key).size(); i++) {
//                PyObject v = ((PyList)key).eltOk(i);
//                PyObject fd = checkExecRedirectFd(v, param != Py.None);
//                ((PyList)ary).push(new PyList(fd, param));
//                n++;
//            }
//        } else {
//            PyObject fd = checkExecRedirectFd(key, param != Py.None);
//            ((PyList)ary).push(new PyList(fd, param));
//        }
//        return ary;
//    }


    public static Channel[] pipe() {
        try {
            Pipe pipe = Pipe.open();
            Channel source = pipe.source(), sink = pipe.sink();

            if (posix.isNative() && !Platform.IS_WINDOWS) {
                // set cloexec if possible
                int read = FilenoUtil.filenoFrom(source);
                int write = FilenoUtil.filenoFrom(sink);
                setCloexec(read, true);
                setCloexec(write, true);
            }

            return new Channel[]{source, sink};
        } catch (IOException ioe) {
            return null;
        }
    }

    class FcntlLibrary {
        private static final int FD_CLOEXEC = 1;
    }

    public static int setCloexec(int fd, boolean cloexec) {
        int ret = posix.fcntl(fd, Fcntl.F_GETFD);
        if (ret == -1) {
//            errno = Errno.valueOf(posix.errno());
            return -1;
        }
        if (
                (cloexec && (ret & FcntlLibrary.FD_CLOEXEC) == FcntlLibrary.FD_CLOEXEC)
                || (!cloexec && (ret & FcntlLibrary.FD_CLOEXEC) == 0)) {
            return 0;
        }
        ret = cloexec ?
                ret | FcntlLibrary.FD_CLOEXEC :
                ret & ~FcntlLibrary.FD_CLOEXEC;
        ret = posix.fcntlInt(fd, Fcntl.F_SETFD, ret);
//        if (ret == -1) errno = Errno.valueOf(posix.errno());
        return ret;
    }


    /**
     * usage
     self.pid = _posixsubprocess.fork_exec(
         args, executable_list,
         close_fds, sorted(fds_to_keep), cwd, env_list,
         p2cread, p2cwrite, c2pread, c2pwrite,
         errread, errwrite,
         errpipe_read, errpipe_write,
         restore_signals, start_new_session, preexec_fn)
     */

    private static class run_exec_dup2_fd_pair {
        int oldfd;
        int newfd;
        int older_index;
        int num_newer;
    }

    public static class ExecArg {
        boolean use_shell;
        PyBytes command_name;
        PyBytes command_abspath; /* full path string or nil */
        String[] argv;
        List<byte[]> argv_buf;
        PyObject redirect_fds;
        String[] envp_str;
        List<String> envp_buf;
        run_exec_dup2_fd_pair[] dup2_tmpbuf;
        int flags;
        long pgroup_pgid = -1; /* asis(-1), new pgroup(0), specified pgroup (0<V). */
        PyObject rlimit_limits; /* null or [[rtype, softlim, hardlim], ...] */
        int umask_mask;
        int uid;
        int gid;
        PyObject fd_dup2;
        PyObject fd_close;
        PyObject fd_open;
        PyObject fd_dup2_child;
        int close_others_maxhint;
        PyList env_modification; /* null or [[k1,v1], ...] */
        String chdir_dir;
        List<SpawnFileAction> fileActions = new ArrayList<>();
        List<SpawnAttribute> attributes = new ArrayList<>();

        boolean pgroup_given() {
            return (flags & 0x1) != 0;
        }

        boolean umask_given() {
            return (flags & 0x2) != 0;
        }

        boolean unsetenv_others_given() {
            return (flags & 0x4) != 0;
        }

        boolean unsetenv_others_do() {
            return (flags & 0x8) != 0;
        }

        boolean close_others_given() {
            return (flags & 0x10) != 0;
        }

        boolean close_others_do() {
            return (flags & 0x20) != 0;
        }

        boolean chdir_given() {
            return (flags & 0x40) != 0;
        }

        boolean new_pgroup_given() {
            return (flags & 0x80) != 0;
        }

        boolean new_pgroup_flag() {
            return (flags & 0x100) != 0;
        }

        boolean uid_given() {
            return (flags & 0x200) != 0;
        }

        boolean gid_given() {
            return (flags & 0x400) != 0;
        }

        void pgroup_given_set() {
            flags |= 0x1;
        }

        void umask_given_set() {
            flags |= 0x2;
        }

        void unsetenv_others_given_set() {
            flags |= 0x4;
        }

        void unsetenv_others_do_set() {
            flags |= 0x8;
        }

        void close_others_given_set() {
            flags |= 0x10;
        }

        void close_others_do_set() {
            flags |= 0x20;
        }

        void chdir_given_set() {
            flags |= 0x40;
        }

        void new_pgroup_given_set() {
            flags |= 0x80;
        }

        void new_pgroup_flag_set() {
            flags |= 0x100;
        }

        void uid_given_set() {
            flags |= 0x200;
        }

        void gid_given_set() {
            flags |= 0x400;
        }

        void pgroup_given_clear() {
            flags &= ~0x1;
        }

        void umask_given_clear() {
            flags &= ~0x2;
        }

        void unsetenv_others_given_clear() {
            flags &= ~0x4;
        }

        void unsetenv_others_do_clear() {
            flags &= ~0x8;
        }

        void close_others_given_clear() {
            flags &= ~0x10;
        }

        void close_others_do_clear() {
            flags &= ~0x20;
        }

        void chdir_given_clear() {
            flags &= ~0x40;
        }

        void new_pgroup_given_clear() {
            flags &= ~0x80;
        }

        void new_pgroup_flag_clear() {
            flags &= ~0x100;
        }

        void uid_given_clear() {
            flags &= ~0x200;
        }

        void gid_given_clear() {
            flags &= ~0x400;
        }
    }
}
