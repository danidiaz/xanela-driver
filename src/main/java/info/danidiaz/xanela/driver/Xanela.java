package info.danidiaz.xanela.driver;

import java.awt.Window;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import org.msgpack.MessagePackable;
import org.msgpack.Packer;

public class Xanela implements MessagePackable {
    @Override
    public void messagePack(final Packer packer) throws IOException {
        
        try {
            SwingUtilities.invokeAndWait(new Runnable() {
                
                @Override
                public void run() {
                    try {
                        Window warray[] = Window.getOwnerlessWindows();
                        int visibleCount = 0;
                        for (int i=0;i<warray.length;i++) {                            
                            if (warray[i].isVisible()) {
                                visibleCount++;
                            }
                        }
                        packer.packArray(visibleCount);
                        for (int i=0;i<warray.length;i++) {
                            Window w = warray[i];
                            if (warray[i].isVisible()) {
                                String title = "";
                                if (w instanceof JFrame) {
                                    title = ((JFrame)w).getTitle();
                                } else if (w instanceof JDialog) {
                                    title = ((JDialog)w).getTitle();                                    
                                }
                                packer.packArray(2);
                                packer.pack(title);
                                packer.packArray(2);
                                packer.pack((int)w.getHeight());
                                packer.pack((int)w.getWidth());
                            }
                        }                                                                                                   
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }            
    }
    
}
