
import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.GroupLayout;
import java.lang.foreign.Linker;
import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.VarHandle;

public class Reproducer {

    private static final int TIOCGWINSZ;

    // Window sizes.
    // @see <a href="http://man7.org/linux/man-pages/man4/tty_ioctl.4.html">IOCTL_TTY(2) man-page</a>
    static class winsize {
        static final GroupLayout LAYOUT;
        private static final VarHandle ws_col;
        private static final VarHandle ws_row;

        static {
            LAYOUT = MemoryLayout.structLayout(
                    ValueLayout.JAVA_SHORT.withName("ws_row"),
                    ValueLayout.JAVA_SHORT.withName("ws_col"),
                    ValueLayout.JAVA_SHORT,
                    ValueLayout.JAVA_SHORT);
            ws_row = LAYOUT.varHandle(MemoryLayout.PathElement.groupElement("ws_row"));
            ws_col = LAYOUT.varHandle(MemoryLayout.PathElement.groupElement("ws_col"));
        }

        private final MemorySegment seg;

        winsize() {
            seg = Arena.ofAuto().allocate(LAYOUT);
        }

        winsize(short ws_col, short ws_row) {
            this();
            ws_col(ws_col);
            ws_row(ws_row);
        }

        MemorySegment segment() {
            return seg;
        }

        short ws_col() {
            return (short) ws_col.get(seg);
        }

        void ws_col(short col) {
            ws_col.set(seg, col);
        }

        short ws_row() {
            return (short) ws_row.get(seg);
        }

        void ws_row(short row) {
            ws_row.set(seg, row);
        }
    }

    static {
        String osName = System.getProperty("os.name");
        if (osName.startsWith("Linux")) {
            String arch = System.getProperty("os.arch");
            boolean isMipsPpcOrSparc = arch.equals("mips")
                    || arch.equals("mips64")
                    || arch.equals("mipsel")
                    || arch.equals("mips64el")
                    || arch.startsWith("ppc")
                    || arch.startsWith("sparc");
            TIOCGWINSZ = isMipsPpcOrSparc ? 0x40087468 : 0x00005413;
        } else if (osName.startsWith("Solaris") || osName.startsWith("SunOS")) {
            int _TIOC = ('T' << 8);
            TIOCGWINSZ = (_TIOC | 104);
        } else if (osName.startsWith("Mac") || osName.startsWith("Darwin")) {
            TIOCGWINSZ = 0x40087468;
        } else if (osName.startsWith("FreeBSD")) {
            TIOCGWINSZ = 0x40087468;
        } else {
            throw new UnsupportedOperationException();
        }
    }

    static void main(String[] args) throws Throwable {
        Linker linker = Linker.nativeLinker();
        // https://man7.org/linux/man-pages/man2/ioctl.2.html
        MethodHandle ioctl = linker.downcallHandle(
                linker.defaultLookup().find("ioctl").get(),
                FunctionDescriptor.of(
                        ValueLayout.JAVA_INT, ValueLayout.JAVA_INT, ValueLayout.JAVA_LONG, ValueLayout.ADDRESS));
        // https://man7.org/linux/man-pages/man3/openpty.3.html
        MethodHandle openpty = linker.downcallHandle(
                linker.defaultLookup().find("openpty").get(),
                FunctionDescriptor.of(
                        ValueLayout.JAVA_INT,
                        ValueLayout.ADDRESS,
                        ValueLayout.ADDRESS,
                        ValueLayout.ADDRESS,
                        ValueLayout.ADDRESS,
                        ValueLayout.ADDRESS));

        MemorySegment buf = Arena.ofAuto().allocate(64);
        MemorySegment master = Arena.ofAuto().allocate(ValueLayout.JAVA_INT);
        MemorySegment slave = Arena.ofAuto().allocate(ValueLayout.JAVA_INT);

        System.out.println("Opening pty...");

        int res = (int) openpty.invoke(master, slave, buf, MemorySegment.NULL, MemorySegment.NULL);

        byte[] str = buf.toArray(ValueLayout.JAVA_BYTE);
        int len = 0;
        while (str[len] != 0) {
            len++;
        }
        String device = new String(str, 0, len);
        int masterFd = master.get(ValueLayout.JAVA_INT, 0);
        int slaveFd = slave.get(ValueLayout.JAVA_INT, 0);

        System.out.println("Sucessfully opened pty (" + device + ", " + masterFd + ", " + slaveFd + ")");

        System.out.println("Getting pty size...");

        winsize ws = new winsize();
        res = (int) ioctl.invoke(masterFd, (long) TIOCGWINSZ, ws.segment());

        System.out.println("Sucessfully got pty size(" + ws.ws_col() + ", " + ws.ws_row() + ")");
    }
}
