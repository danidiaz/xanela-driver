package info.danidiaz.xanela.driver;

import java.awt.Window;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

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
                                packer.packArray(3);
                                packer.pack(w.getClass().getName());
                                packer.pack((int)w.getHeight());
                                packer.pack((int)w.getWidth());
                            }
                        }                        
                        
/*                        packer.packArray(2);
                        packer.pack("fim fam fum");                            
                        packer.packArray(3);
                        packer.pack((int)1);
                        packer.pack((int)2);
                        packer.pack((int)3);*/                                                    
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
