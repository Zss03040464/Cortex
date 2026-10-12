import java.lang.reflect.*;
import java.io.*;
import android.graphics.PixelFormat;
import android.media.ImageReader;
import android.os.*;
import android.view.Surface;
import android.hardware.display.VirtualDisplayConfig;

public class HeadlessDisplayServer {
    public static void main(String[] args) {
        try {
            int width = args.length > 0 ? Integer.parseInt(args[0]) : 1080;
            int height = args.length > 1 ? Integer.parseInt(args[1]) : 2400;
            int dpi = args.length > 2 ? Integer.parseInt(args[2]) : 420;

            Class<?> smClass = Class.forName("android.os.ServiceManager");
            Method getService = smClass.getMethod("getService", String.class);
            IBinder binder = (IBinder) getService.invoke(null, "display");

            Class<?> stubClass = Class.forName("android.hardware.display.IDisplayManager$Stub");
            Method asInterface = stubClass.getMethod("asInterface", IBinder.class);
            Object dm = asInterface.invoke(null, binder);

            ImageReader reader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2);
            Surface surface = reader.getSurface();

            // Flags:
            // PUBLIC = 1
            // OWN_CONTENT_ONLY = 8
            // TRUSTED = 1024
            // OWN_DISPLAY_GROUP = 2048
            // Note: Explicitly NO SHOULD_SHOW_SYSTEM_DECORATIONS (512)
            int flags = 1 | 8 | 1024 | 2048;

            VirtualDisplayConfig.Builder builder = new VirtualDisplayConfig.Builder("headless_vd", width, height, dpi);
            builder.setSurface(surface);
            builder.setFlags(flags);
            VirtualDisplayConfig config = builder.build();

            Class<?> cbInterface = Class.forName("android.hardware.display.IVirtualDisplayCallback");
            Binder cbBinder = new Binder();
            Object cbProxy = Proxy.newProxyInstance(
                cbInterface.getClassLoader(),
                new Class<?>[] { cbInterface },
                (proxy, method, mArgs) -> method.getName().equals("asBinder") ? cbBinder : null
            );

            Method createVD = null;
            for (Method m : dm.getClass().getMethods()) {
                if (m.getName().equals("createVirtualDisplay")) {
                    createVD = m;
                    break;
                }
            }

            int displayId = (Integer) createVD.invoke(dm, config, cbProxy, null, "com.android.shell");

            // CRITICAL: Explicitly set DISPLAY_IME_POLICY_HIDE (2) on this display
            // This prevents InputMethodManager from falling back to Display 0 and popping up the keyboard on the physical screen!
            try {
                IBinder wmBinder = (IBinder) getService.invoke(null, "window");
                Class<?> wmStubClass = Class.forName("android.view.IWindowManager$Stub");
                Method wmAsInterface = wmStubClass.getMethod("asInterface", IBinder.class);
                Object wm = wmAsInterface.invoke(null, wmBinder);
                Method setDisplayImePolicy = null;
                for (Method m : wm.getClass().getMethods()) {
                    if (m.getName().equals("setDisplayImePolicy")) {
                        setDisplayImePolicy = m;
                        break;
                    }
                }
                if (setDisplayImePolicy != null) {
                    setDisplayImePolicy.invoke(wm, displayId, 2); // 2 = DISPLAY_IME_POLICY_HIDE
                    System.out.println("DISPLAY_IME_POLICY set to HIDE (2) for display " + displayId);
                }
            } catch (Throwable t) {
                System.err.println("Warning: Failed to set DISPLAY_IME_POLICY_HIDE: " + t.getMessage());
            }

            File pidFile = new File("/data/local/tmp/headless_display.pid");
            File idFile = new File("/data/local/tmp/headless_display.id");

            int myPid = android.os.Process.myPid();
            try (FileWriter fw = new FileWriter(pidFile)) { fw.write(String.valueOf(myPid)); }
            try (FileWriter fw = new FileWriter(idFile)) { fw.write(String.valueOf(displayId)); }

            System.out.println("HEADLESS_DISPLAY_READY: id=" + displayId + " pid=" + myPid);
            System.out.flush();

            // Register shutdown hook
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try {
                    Method releaseVD = dm.getClass().getMethod("releaseVirtualDisplay", cbInterface);
                    releaseVD.invoke(dm, cbProxy);
                    pidFile.delete();
                    idFile.delete();
                    System.out.println("HEADLESS_DISPLAY_CLEANED");
                } catch (Exception ignored) {}
            }));

            // Keep process alive until interrupted
            while (true) {
                Thread.sleep(10000);
            }
        } catch (Throwable t) {
            t.printStackTrace();
            System.exit(1);
        }
    }
}
