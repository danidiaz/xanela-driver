package info.danidiaz.xanela.driver;

import java.awt.Component;
import java.awt.Point;
import java.awt.Window;
import java.awt.event.MouseEvent;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.AbstractButton;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JLayeredPane;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JRootPane;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.RootPaneContainer;
import javax.swing.SwingUtilities;
import javax.swing.text.JTextComponent;

import org.msgpack.MessagePackable;
import org.msgpack.packer.Packer;

public class Xanela {
    
    private ImageBin imageBin;

    private List<Window> windowArray = new ArrayList<Window>();
    private Map<Window,BufferedImage> windowImageMap = new HashMap<Window,BufferedImage>();
    
    private List<Component> componentArray = new ArrayList<Component>();
    
    boolean dirty = false;
    
    public Xanela(Xanela xanela) {
        this.imageBin = xanela==null?new ImageBin():xanela.obtainImageBin();
    }
    public void buildAndWrite(final int xid, final Packer packer) throws IOException {
        
        try {
            SwingUtilities.invokeAndWait(new Runnable() {
                
                @Override
                public void run() {
                    try {
                        Window warray[] = Window.getOwnerlessWindows();
                        writeWindowArray(xid, packer, warray);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } finally {
            this.imageBin.flush();
        }
    }
    
    private static int countShowing(Component[] warray) {
        int visibleCount = 0;
        for (int i=0;i<warray.length;i++) {                            
            if (warray[i].isShowing()) {
                visibleCount++;
            }
        }
        return visibleCount;
    }
    
    private void writeWindowArray(int xid, Packer packer, Window warray[]) throws IOException {
        packer.writeArrayBegin(countShowing(warray));
        for (int i=0;i<warray.length;i++) {
            Window w = warray[i];
            if (w.isShowing()) {
                writeWindow(xid, packer,w);
            }        
        }
        packer.writeArrayEnd();
    }
    
    private void writeWindow(int xid, Packer packer, Window w) throws IOException {
        
        int windowId = windowArray.size();
        windowArray.add(w);
        BufferedImage image = imageBin.obtainImage(w.getSize());
        w.paint(image.getGraphics());
        windowImageMap.put(w, image);
        
        packer.write((int)xid);
        packer.write((int)windowId);
        
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
        
        writeMenuBar(xid, packer, w);
        
        writePopupLayer(xid,packer,w);
                        
        RootPaneContainer rpc = (RootPaneContainer)w;
        writeComponent(xid, packer, (JComponent) rpc.getContentPane(),w);                                                               
        
        writeWindowArray(xid, packer, w.getOwnedWindows());
    }
    
    private void writeMenuBar(int xid, Packer packer, Window w) throws IOException {        
        JMenuBar menubar = null;
        if (w instanceof JFrame) {
            menubar = ((JFrame)w).getJMenuBar();
        } else if (w instanceof JDialog) {
            menubar = ((JDialog)w).getJMenuBar();                                    
        }
        if (menubar==null) {
            packer.writeArrayBegin(0);
            packer.writeArrayEnd();
        } else {
            packer.writeArrayBegin(menubar.getMenuCount());
            for (int i=0; i<menubar.getMenuCount();i++) {
                writeComponent(xid, packer,menubar.getMenu(i),w);
            }
            packer.writeArrayEnd();

        }                
    }
    
    private void writePopupLayer(int xid, Packer packer, Window w) throws IOException {
        Component[] popupLayerArray = new Component[] {};
        if (w instanceof JFrame) {
            popupLayerArray = ((JFrame)w).getLayeredPane().getComponentsInLayer(JLayeredPane.POPUP_LAYER);
        } else if (w instanceof JDialog) {
            popupLayerArray = ((JDialog)w).getLayeredPane().getComponentsInLayer(JLayeredPane.POPUP_LAYER);                                    
        }
        packer.writeArrayBegin(countShowing(popupLayerArray));        
        for (int i=0;i<popupLayerArray.length;i++) {
            JComponent c = (JComponent) popupLayerArray[i];
            if (c.isShowing()) {
                writeComponent(xid, packer, c, w);    
            }
        }
        packer.writeArrayEnd();
    }
        
    private void writeComponent(int xid, Packer packer, JComponent c, Component coordBase) throws IOException {
        
        int componentId = componentArray.size();
        componentArray.add(c);
        
        packer.write((int)xid);
        packer.write((int)componentId);
        
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
        
        writeComponentType(xid, packer, componentId, c, coordBase);
        
        Component children[] = c.getComponents();
        packer.writeArrayBegin(countShowing(children));
        for (int i=0;i<children.length;i++) {
            if (children[i].isShowing()) {
                writeComponent(xid, packer, (JComponent)children[i],coordBase);
            }
        }
        packer.writeArrayEnd();
    }
    
    private static void writeComponentType( int xid, Packer packer, 
                int componentId,
                JComponent c, 
                Component coordBase 
            ) throws IOException 
    {
        packer.write((int)xid);
        
        if (c instanceof JPanel) {
            packer.write((int)1);
        } else if (c instanceof AbstractButton) {
            packer.write((int)2);
            packer.write((int)componentId);
            if (c instanceof JToggleButton || 
                    c instanceof JCheckBoxMenuItem || 
                    c instanceof JRadioButtonMenuItem) {
                packer.write(((AbstractButton)c).isSelected());
            } else {
                packer.writeNil();
            }
        } else if (c instanceof JTextField ) {
            packer.write((int)3);
            JTextField textField = (JTextField) c;
            if (textField.isEditable()) {
                packer.write((int)componentId);
            } else {
                packer.writeNil();
            }
        } else if (c instanceof JLabel) {
            
            packer.write((int)4);
            
        } else if (c instanceof JPopupMenu) {
            
            packer.write((int)5);
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

        final AbstractButton button = (AbstractButton)componentArray.get(buttonId);
        
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
    
    public void rightClick(final int cId) {
                
        final JComponent button = (JComponent)componentArray.get(cId);
        System.out.println(button.getClass());
        System.out.println(button.toString());
        java.awt.Toolkit.getDefaultToolkit().getSystemEventQueue().postEvent(
                new MouseEvent( button, 
                            MouseEvent.MOUSE_RELEASED, 
                            0, 
                            MouseEvent.BUTTON3_MASK, // modifiers 
                            0, // x 
                            0, // y
                            0, 
                            true                        
                        ));                    
    }
           
    public BufferedImage getWindowImage(final int windowId) {
       Window window = windowArray.get(windowId);
       return windowImageMap.get(window);
    }

    public void closeWindow(final int windowId) {
        Window window = windowArray.get(windowId);
        
        java.awt.Toolkit.getDefaultToolkit().getSystemEventQueue().postEvent(
                    new WindowEvent(window, WindowEvent.WINDOW_CLOSING) 
                );
     }
    
    private void setDirty() {
        this.dirty = true;
    }
    
    private ImageBin obtainImageBin() {
        setDirty();
        return new ImageBin(windowImageMap.values());
    }
}
