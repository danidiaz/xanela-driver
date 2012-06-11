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
import javax.swing.JToggleButton;
import javax.swing.RootPaneContainer;
import javax.swing.SwingUtilities;
import javax.swing.text.JTextComponent;

import org.msgpack.MessagePackable;
import org.msgpack.packer.Packer;

public class Xanela {
    
    private List<Component> componentArray = new ArrayList<Component>();
    
    public void buildAndWrite(final Packer packer) throws IOException {
        
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
        packer.writeArrayBegin(countVisible(warray));
        for (int i=0;i<warray.length;i++) {
            Window w = warray[i];
            if (w.isVisible()) {
                writeWindow(packer,w);
            }        
        }
        packer.writeArrayEnd();
    }
    
    private void writeWindow(Packer packer, Window w) throws IOException {
        String title = "";
        if (w instanceof JFrame) {
            title = ((JFrame)w).getTitle();
        } else if (w instanceof JDialog) {
            title = ((JDialog)w).getTitle();                                    
        }

        packer.write(title);
        packer.writeArrayBegin(2);
        {
            packer.write((int)w.getHeight());
            packer.write((int)w.getWidth());
        }
        packer.writeArrayEnd();

        
        RootPaneContainer rpc = (RootPaneContainer)w;
        writeComponent(packer, (JComponent) rpc.getContentPane(),w);                                                               
        
        writeWindowArray(packer, w.getOwnedWindows());
    }
    
    
    private void writeComponent(Packer packer, JComponent c, Component coordBase) throws IOException {
        
        int componentId = componentArray.size();
        componentArray.add(c);
        
        packer.writeArrayBegin(2);
        {
            Point posInWindow = SwingUtilities.convertPoint(c, c.getX(), c.getY(), coordBase);
            packer.write((int)posInWindow.getX());
            packer.write((int)posInWindow.getY());
        }
        packer.writeArrayEnd();
        
        packer.writeArrayBegin(2);
        {
            packer.write((int)c.getHeight());
            packer.write((int)c.getWidth());
        }
        packer.writeArrayEnd();
        
        writePotentiallyNullString(packer,c.getName());
        writePotentiallyNullString(packer,c.getToolTipText());
        
        if (c instanceof AbstractButton) {
            writePotentiallyNullString(packer,((AbstractButton)c).getText());
        } else if (c instanceof JLabel) {
            writePotentiallyNullString(packer,((JLabel)c).getText());
        } else if (c instanceof JTextComponent) {
            writePotentiallyNullString(packer,((JTextComponent)c).getText());
        } else {
            packer.writeNil();
        }

        packer.write(c.isEnabled());        
        
        writeComponentType(packer, componentId, c, coordBase);
        
        Component children[] = c.getComponents();
        packer.writeArrayBegin(countVisible(children));
        for (int i=0;i<children.length;i++) {
            if (children[i].isVisible()) {
                writeComponent(packer, (JComponent)children[i],coordBase);
            }
        }
        packer.writeArrayEnd();
    }
    
    private static void writeComponentType( Packer packer, 
                int componentId,
                JComponent c, 
                Component coordBase 
            ) throws IOException 
    {
        if (c instanceof JPanel) {
            packer.write((int)1);
        } else if (c instanceof AbstractButton) {
            packer.write((int)2);
            packer.write((int)componentId);
            if (c instanceof JToggleButton) {
                packer.write(((JToggleButton)c).isSelected());
            } else {
                packer.writeNil();
            }
        } else if (c instanceof JTextField) {
            packer.write((int)3);
            JTextField textField = (JTextField) c;
            if (textField.isEditable()) {
                packer.write((int)componentId);
            } else {
                packer.writeNil();
            }
        } else if (c instanceof JLabel) {
            
            packer.write((int)4);
        } else {
            packer.write((int)77);
            packer.write(c.getClass().getName());
        }
    }
    
    private static void writePotentiallyNullString(Packer packer, String s) throws IOException {
        if (s==null) {
            packer.writeNil();
        } else {
            packer.write(s);
        }
    }

    public void click(int buttonId) {

        final JButton button = (JButton)componentArray.get(buttonId);
        
        SwingUtilities.invokeLater(new Runnable() {
            
            @Override
            public void run() {
                button.doClick();
            }
        });                 
    }
    
    public void setTextField(int cId, final String text) {

        final JTextField textField = (JTextField)componentArray.get(cId);
        
        SwingUtilities.invokeLater(new Runnable() {
            
            @Override
            public void run() {
                textField.setText(text);
            }
        });                 
    }

}
