package grails.build.logging;

import jline.ConsoleReader;
import jline.Terminal;
import jline.WindowsTerminal;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public final class PatchedJLineWindowsTerminal extends WindowsTerminal {
    private static final int ENABLE_PROCESSED_INPUT = 1;
    private static final int ENABLE_WINDOW_INPUT = 8;
    private final ConsoleReader reader;

    public PatchedJLineWindowsTerminal(ConsoleReader reader) {
        super();
        this.reader = reader;
    }

    public static Terminal setupTerminal(ConsoleReader reader) throws Exception {
        final Terminal terminal = new PatchedJLineWindowsTerminal(reader);
        terminal.initializeTerminal();
        return terminal;
    }

    @Override
    public void initializeTerminal() throws Exception {
        super.initializeTerminal();
        setConsoleModeHacked(getConsoleModeHacked() | ENABLE_PROCESSED_INPUT | ENABLE_WINDOW_INPUT);
        fixCtrlC(reader);
    }

    @Override
    public boolean isANSISupported() {
        return true;
    }

    private Integer getConsoleModeHacked() {
        return (Integer) invokePrivateMethod(WindowsTerminal.class, this, "getConsoleMode", null);
    }

    private void setConsoleModeHacked(Integer mode) {
        invokePrivateMethod(WindowsTerminal.class, this, "setConsoleMode", new Object[]{mode});
    }

    private void fixCtrlC(ConsoleReader reader) {
        if (reader == null) {
            return;
        }

        // hack to workaround JLine bug - see https://issues.apache.org/jira/browse/GERONIMO-3978 for source of fix
        try {
            Field f = ConsoleReader.class.getDeclaredField("keybindings");
            f.setAccessible(true);
            short[] keybindings = (short[]) f.get(reader);
            if (keybindings[3] == Terminal.INSERT) {
                keybindings[3] = Terminal.CTRL_C;
            }
        } catch (Exception ignored) {
            // shouldn't happen
        }

        // CTRL+C before interactive mode is running
        reader.addTriggeredAction(Terminal.CTRL_C, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                System.exit(0);
            }
        });
    }

    private Object invokePrivateMethod(Class clazz, Object o, String methodName, Object[] params) {
        final Method methods[] = clazz.getDeclaredMethods();
        for (int i = 0; i < methods.length; ++i) {
            if (methodName.equals(methods[i].getName())) {
                try {
                    methods[i].setAccessible(true);
                    return methods[i].invoke(o, params);
                } catch (IllegalAccessException ex) {
                    throw new RuntimeException("IllegalAccessException accessing " + methodName);
                } catch (InvocationTargetException ite) {
                    throw new RuntimeException("InvocationTargetException accessing " + methodName);
                }
            }
        }
        throw new RuntimeException("Method '" + methodName + "' not found");
    }

}
