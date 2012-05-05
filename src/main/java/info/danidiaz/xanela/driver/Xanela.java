package info.danidiaz.xanela.driver;

import java.awt.Component;
import java.awt.Window;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.RootPaneContainer;
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
                        writeWindowArray(packer, warray);
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
    
    private static int countVisible(Component[] warray) {
        int visibleCount = 0;
        for (int i=0;i<warray.length;i++) {                            
            if (warray[i].isVisible()) {
                visibleCount++;
            }
        }
        return visibleCount;
    }
    
    private static void writeWindowArray(Packer packer, Window warray[]) throws IOException {
        packer.packArray(countVisible(warray));
        for (int i=0;i<warray.length;i++) {
            Window w = warray[i];
            if (warray[i].isVisible()) {
                String title = "";
                if (w instanceof JFrame) {
                    title = ((JFrame)w).getTitle();
                } else if (w instanceof JDialog) {
                    title = ((JDialog)w).getTitle();                                    
                }
                packer.packArray(4);
                packer.pack(title);
                packer.packArray(2);
                packer.pack((int)w.getHeight());
                packer.pack((int)w.getWidth());
                
                RootPaneContainer rpc = (RootPaneContainer)w;
                writeComponent(packer, rpc.getContentPane());
                
                if (w instanceof JFrame) {
                    System.out.println(((JFrame)w).getContentPane().getClass().getName());
                } else if (w instanceof JDialog) {
                    System.out.println(((JDialog)w).getContentPane().getClass().getName());                                   
                }                
                writeWindowArray(packer, w.getOwnedWindows());
            }        
        }
    }
    
    private static void writeComponent(Packer packer, Component c) throws IOException {
        JComponent jc = (JComponent) c;
        
        packer.packArray(3);
        
        packer.pack(c.getClass().getName());
        
        packer.packArray(2);
        packer.pack((int)c.getHeight());
        packer.pack((int)c.getWidth());
        
        Component children[] = jc.getComponents();
        packer.packArray(countVisible(children));
        for (int i=0;i<children.length;i++) {
            if (children[i].isVisible()) {
                writeComponent(packer, children[i]);
            }
        }
    }

}
