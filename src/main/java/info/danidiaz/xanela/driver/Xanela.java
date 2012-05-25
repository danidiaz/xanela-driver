package info.danidiaz.xanela.driver;

import java.awt.Component;
import java.awt.Point;
import java.awt.Window;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractButton;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.RootPaneContainer;
import javax.swing.SwingUtilities;
import javax.swing.text.JTextComponent;

import org.msgpack.MessagePackable;
import org.msgpack.Packer;

public class Xanela implements MessagePackable {
    
    private List<Component> componentArray = new ArrayList<Component>();
    
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
    
    private void writeWindowArray(Packer packer, Window warray[]) throws IOException {
        packer.packArray(countVisible(warray));
        for (int i=0;i<warray.length;i++) {
            Window w = warray[i];
            if (warray[i].isVisible()) {
                packer.packArray(2);
                
                String title = "";
                if (w instanceof JFrame) {
                    title = ((JFrame)w).getTitle();
                } else if (w instanceof JDialog) {
                    title = ((JDialog)w).getTitle();                                    
                }
                packer.packArray(3);
                packer.pack(title);
                packer.packArray(2);
                packer.pack((int)w.getHeight());
                packer.pack((int)w.getWidth());
                
                RootPaneContainer rpc = (RootPaneContainer)w;
                writeComponent(packer, (JComponent) rpc.getContentPane(),w);
                               
                writeWindowArray(packer, w.getOwnedWindows());
            }        
        }
    }
    
    private void writeComponent(Packer packer, JComponent c, Component coordBase) throws IOException {
        
        int componentId = componentArray.size();
        componentArray.add(c);
        
        packer.packArray(2);
        packer.packArray(7);

        packer.packArray(2);
        Point posInWindow = SwingUtilities.convertPoint(c, c.getX(), c.getY(), coordBase);
        packer.pack((int)posInWindow.getX());
        packer.pack((int)posInWindow.getY());
        
        packer.packArray(2);
        packer.pack((int)c.getHeight());
        packer.pack((int)c.getWidth());
        
        writePotentiallyNullString(packer,c.getName());
        writePotentiallyNullString(packer,c.getToolTipText());
        
        if (c instanceof AbstractButton) {
            writePotentiallyNullString(packer,((AbstractButton)c).getText());
        } else if (c instanceof JLabel) {
            writePotentiallyNullString(packer,((JLabel)c).getText());
        } else if (c instanceof JTextComponent) {
            writePotentiallyNullString(packer,((JTextComponent)c).getText());
        } else {
            packer.packNil();
        }

        packer.pack(c.isEnabled());        
        
        writeComponentType(packer, c, coordBase);
        
        Component children[] = c.getComponents();
        packer.packArray(countVisible(children));
        for (int i=0;i<children.length;i++) {
            if (children[i].isVisible()) {
                writeComponent(packer, (JComponent)children[i],coordBase);
            }
        }
    }
    
    private static void writeComponentType(Packer packer, JComponent c, Component coordBase) throws IOException {
        packer.packArray(2);
        if (c instanceof JPanel) {
            packer.pack((int)1);
            packer.pack("foo");
        } else if (c instanceof JButton) {
            packer.pack((int)2);
            packer.pack(((JButton)c).getText());
        } else if (c instanceof JTextField) {
            packer.pack((int)3);
            packer.pack(((JTextField)c).getText());
        } else {
            packer.pack((int)4);
            packer.pack("foo");
        }
            
    }
    
    private static void writePotentiallyNullString(Packer packer, String s) throws IOException {
        if (s==null) {
            packer.packNil();
        } else {
            packer.pack(s);
        }
    }

    public void click(int buttonId) {
        try {
            final JButton button = (JButton)componentArray.get(buttonId);
            
            SwingUtilities.invokeAndWait(new Runnable() {
                
                @Override
                public void run() {
                    button.doClick();
                }
            });
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }                    
    }
}
