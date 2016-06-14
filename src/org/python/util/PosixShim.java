package org.python.util;

import jnr.posix.util.Platform;

/**
 * Created by isaiah on 6/14/16.
 */
public class PosixShim {
    public interface WaitMacros {
        boolean WIFEXITED(long status);
        boolean WIFSIGNALED(long status);
        int WTERMSIG(long status);
        int WEXITSTATUS(long status);
        int WSTOPSIG(long status);
        boolean WIFSTOPPED(long status);
        boolean WCOREDUMP(long status);
    }

    public static class BSDWaitMacros implements WaitMacros {
        public final long _WSTOPPED = 0177;

        // Only confirmed on Darwin
        public final long WCOREFLAG = 0200;

        public long _WSTATUS(long status) {
            return status & _WSTOPPED;
        }

        public boolean WIFEXITED(long status) {
            return _WSTATUS(status) == 0;
        }

        public boolean WIFSIGNALED(long status) {
            return _WSTATUS(status) != _WSTOPPED && _WSTATUS(status) != 0;
        }

        public int WTERMSIG(long status) {
            return (int)_WSTATUS(status);
        }

        public int WEXITSTATUS(long status) {
            // not confirmed on all platforms
            return (int)((status >>> 8) & 0xFF);
        }

        public int WSTOPSIG(long status) {
            return (int)(status >>> 8);
        }

        public boolean WIFSTOPPED(long status) {
            return _WSTATUS(status) == _WSTOPPED && WSTOPSIG(status) != 0x13;
        }

        public boolean WCOREDUMP(long status) {
            return (status & WCOREFLAG) != 0;
        }
    }

    public static class LinuxWaitMacros implements WaitMacros {
        private int __WAIT_INT(long status) { return (int)status; }

        private int __W_EXITCODE(int ret, int sig) { return (ret << 8) | sig; }
        private int __W_STOPCODE(int sig) { return (sig << 8) | 0x7f; }
        private static int __W_CONTINUED = 0xffff;
        private static int __WCOREFLAG = 0x80;

        /* If WIFEXITED(STATUS), the low-order 8 bits of the status.  */
        private int __WEXITSTATUS(long status) { return (int)((status & 0xff00) >> 8); }

        /* If WIFSIGNALED(STATUS), the terminating signal.  */
        private int __WTERMSIG(long status) { return (int)(status & 0x7f); }

        /* If WIFSTOPPED(STATUS), the signal that stopped the child.  */
        private int __WSTOPSIG(long status) { return __WEXITSTATUS(status); }

        /* Nonzero if STATUS indicates normal termination.  */
        private boolean __WIFEXITED(long status) { return __WTERMSIG(status) == 0; }

        /* Nonzero if STATUS indicates termination by a signal.  */
        private boolean __WIFSIGNALED(long status) {
            return ((status & 0x7f) + 1) >> 1 > 0;
        }

        /* Nonzero if STATUS indicates the child is stopped.  */
        private boolean __WIFSTOPPED(long status) { return (status & 0xff) == 0x7f; }

        /* Nonzero if STATUS indicates the child dumped core.  */
        private boolean __WCOREDUMP(long status) { return (status & __WCOREFLAG) != 0; }

        /* Macros for constructing status values.  */
        public int WEXITSTATUS(long status) { return __WEXITSTATUS (__WAIT_INT (status)); }
        public int WTERMSIG(long status) { return __WTERMSIG(__WAIT_INT(status)); }
        public int WSTOPSIG(long status) { return __WSTOPSIG(__WAIT_INT(status)); }
        public boolean WIFEXITED(long status) { return __WIFEXITED(__WAIT_INT(status)); }
        public boolean WIFSIGNALED(long status) { return __WIFSIGNALED(__WAIT_INT(status)); }
        public boolean WIFSTOPPED(long status) { return __WIFSTOPPED(__WAIT_INT(status)); }
        public boolean WCOREDUMP(long status) { return __WCOREDUMP(__WAIT_INT(status)); }
    }

    public static final WaitMacros WAIT_MACROS;
    static {
        if (Platform.IS_BSD) {
            WAIT_MACROS = new BSDWaitMacros();
        } else {
            // need other platforms
            WAIT_MACROS = new LinuxWaitMacros();
        }
    }

}
