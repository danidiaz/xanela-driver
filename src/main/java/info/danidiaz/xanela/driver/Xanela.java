package info.danidiaz.xanela.driver;

import java.awt.Component;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Window;
import java.awt.event.MouseEvent;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.AbstractButton;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JLayeredPane;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JRootPane;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.ListCellRenderer;
import javax.swing.RootPaneContainer;
import javax.swing.SwingUtilities;
import javax.swing.text.JTextComponent;

import org.msgpack.MessagePackable;
import org.msgpack.packer.Packer;

import com.sun.org.apache.bcel.internal.generic.GETSTATIC;
import com.sun.org.apache.xpath.internal.operations.Bool;

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
    
    private void writeComponentType( int xanelaid, Packer packer, 
                int componentId,
                JComponent c, 
                Component coordBase 
            ) throws IOException 
    {
        packer.write((int)xanelaid);
        
        if (c instanceof JPanel) {
            packer.write((int)1);
        } else if (c instanceof JToggleButton || c instanceof JCheckBoxMenuItem || c instanceof JRadioButtonMenuItem) {
            packer.write((int)2);
            packer.write((int)componentId);
            packer.write(((AbstractButton)c).isSelected());                 
        } else if (c instanceof AbstractButton) { // normal button, not toggle button
            packer.write((int)3);
            packer.write((int)componentId);
        } else if (c instanceof JTextField ) {
            packer.write((int)4);
            JTextField textField = (JTextField) c;
            if (textField.isEditable()) {
                packer.write((int)componentId);
            } else {
                packer.writeNil();
            }
        } else if (c instanceof JLabel) {
            
            packer.write((int)5);
            
        } else if (c instanceof JComboBox) {
            
            packer.write((int)6);
            packer.write((int)componentId);

            JComboBox comboBox = (JComboBox)c;
            ListCellRenderer renderer = comboBox.getRenderer();
            JList dummyJList = new JList();

            if (comboBox.getSelectedIndex()==-1) {
                packer.writeNil();
            } else {
                JComponent cell = (JComponent)renderer.getListCellRendererComponent(dummyJList, 
                                comboBox.getModel().getElementAt(comboBox.getSelectedIndex()), 
                                comboBox.getSelectedIndex(), 
                                false, 
                                false
                            );
                writeComponent(xanelaid, packer, cell, coordBase);
            }                          
                       
        } else if (c instanceof JList) {
            packer.write((int)7);
            JList list = (JList) c;
            ListCellRenderer renderer = list.getCellRenderer();
            
            packer.writeArrayBegin((int)list.getModel().getSize());
            for (int rowid=0; rowid<list.getModel().getSize(); rowid++) {
                packer.write((int)xanelaid);
                packer.write((int)componentId);
                packer.write((int)rowid);
                packer.write((int)0);
                JComponent cell = (JComponent)renderer.getListCellRendererComponent(list, 
                        list.getModel().getElementAt(rowid), 
                        rowid, 
                        false, 
                        false
                    );
                writeComponent(xanelaid, packer, cell, coordBase);
            }
            packer.writeArrayEnd();
            
        } else if (c instanceof JPopupMenu) {
            
            packer.write((int)8);
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

    public void toggle(final int buttonId, final boolean targetState) {

        final AbstractButton button = (AbstractButton)componentArray.get(buttonId);
        
        SwingUtilities.invokeLater(new Runnable() {
            
            @Override
            public void run() {
                if (button.isSelected() != targetState) {
                    //button.doClick();
                    java.awt.Toolkit.getDefaultToolkit().getSystemEventQueue().postEvent(
                            new MouseEvent( button, 
                                        MouseEvent.MOUSE_ENTERED, 
                                        0, 
                                        0, // modifiers 
                                        button.getWidth()/2, // x 
                                        button.getHeight()/2, // y
                                        0, 
                                        true                        
                                    ));
                    java.awt.Toolkit.getDefaultToolkit().getSystemEventQueue().postEvent(
                            new MouseEvent( button, 
                                        MouseEvent.MOUSE_PRESSED, 
                                        0, 
                                        MouseEvent.BUTTON1_MASK, // modifiers 
                                        button.getWidth()/2, // x 
                                        button.getHeight()/2, // y
                                        0, 
                                        true                        
                                    )); 
                    java.awt.Toolkit.getDefaultToolkit().getSystemEventQueue().postEvent(
                            new MouseEvent( button, 
                                        MouseEvent.MOUSE_RELEASED, 
                                        0, 
                                        MouseEvent.BUTTON1_MASK, // modifiers 
                                        button.getWidth()/2, // x 
                                        button.getHeight()/2, // y
                                        0, 
                                        true                        
                                    ));                         
                    java.awt.Toolkit.getDefaultToolkit().getSystemEventQueue().postEvent(
                            new MouseEvent( button, 
                                        MouseEvent.MOUSE_CLICKED, 
                                        0, 
                                        MouseEvent.BUTTON1_MASK, // modifiers 
                                        button.getWidth()/2, // x 
                                        button.getHeight()/2, // y
                                        0, 
                                        true                        
                                    ));                    
                }
            }
        });                 
    }
    
    public void click(int buttonId) {

        final AbstractButton button = (AbstractButton)componentArray.get(buttonId);
        
        SwingUtilities.invokeLater(new Runnable() {
            
            @Override
            public void run() {
                // button.doClick();
                java.awt.Toolkit.getDefaultToolkit().getSystemEventQueue().postEvent(
                        new MouseEvent( button, 
                                    MouseEvent.MOUSE_ENTERED, 
                                    0, 
                                    0, // modifiers 
                                    button.getWidth()/2, // x 
                                    button.getHeight()/2, // y
                                    0, 
                                    true                        
                                ));
                java.awt.Toolkit.getDefaultToolkit().getSystemEventQueue().postEvent(
                        new MouseEvent( button, 
                                    MouseEvent.MOUSE_PRESSED, 
                                    0, 
                                    MouseEvent.BUTTON1_MASK, // modifiers 
                                    button.getWidth()/2, // x 
                                    button.getHeight()/2, // y
                                    0, 
                                    true                        
                                )); 
                java.awt.Toolkit.getDefaultToolkit().getSystemEventQueue().postEvent(
                        new MouseEvent( button, 
                                    MouseEvent.MOUSE_RELEASED, 
                                    0, 
                                    MouseEvent.BUTTON1_MASK, // modifiers 
                                    button.getWidth()/2, // x 
                                    button.getHeight()/2, // y
                                    0, 
                                    true                        
                                ));                         
                java.awt.Toolkit.getDefaultToolkit().getSystemEventQueue().postEvent(
                        new MouseEvent( button, 
                                    MouseEvent.MOUSE_CLICKED, 
                                    0, 
                                    MouseEvent.BUTTON1_MASK, // modifiers 
                                    button.getWidth()/2, // x 
                                    button.getHeight()/2, // y
                                    0, 
                                    true                        
                                )); 
            }
        });                 
    }

    public void clickCombo(int buttonId) {

        final JComboBox button = (JComboBox)componentArray.get(buttonId);
        
        SwingUtilities.invokeLater(new Runnable() {
            
            @Override
            public void run() {
                button.showPopup();
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
    
    public void selectCell(final int componentid, final int rowid, final int columnid) {

        final Component component = componentArray.get(componentid);
        
        SwingUtilities.invokeLater(new Runnable() {
            
            @Override
            public void run() {
                try {
                    if (component instanceof JList) {
                        JList list = (JList) component;

                        list.ensureIndexIsVisible(rowid);
                        Rectangle bounds = list.getCellBounds(rowid, rowid);
                        java.awt.Toolkit.getDefaultToolkit().getSystemEventQueue().postEvent(
                                new MouseEvent( list, 
                                            MouseEvent.MOUSE_PRESSED, 
                                            0, 
                                            MouseEvent.BUTTON1_MASK, // modifiers 
                                            bounds.x + bounds.width/2, // x 
                                            bounds.y + bounds.height/2, // y
                                            0, 
                                            true                        
                                        )); 
                        java.awt.Toolkit.getDefaultToolkit().getSystemEventQueue().postEvent(
                                new MouseEvent( list, 
                                            MouseEvent.MOUSE_RELEASED, 
                                            0, 
                                            MouseEvent.BUTTON1_MASK, // modifiers 
                                            bounds.x + bounds.width/2, // x 
                                            bounds.y + bounds.height/2, // y
                                            0, 
                                            true                        
                                        ));                         
                        java.awt.Toolkit.getDefaultToolkit().getSystemEventQueue().postEvent(
                                new MouseEvent( list, 
                                            MouseEvent.MOUSE_CLICKED, 
                                            0, 
                                            MouseEvent.BUTTON1_MASK, // modifiers 
                                            bounds.x + bounds.width/2, // x 
                                            bounds.y + bounds.height/2, // y
                                            0, 
                                            true                        
                                        )); 
                        //list.setSelectedIndex(rowid);
/*                        Method m = JList.class.getDeclaredMethod("fireSelectionValueChanged", 
                                Integer.TYPE,Integer.TYPE,Boolean.TYPE);
                        m.setAccessible(true);
                        m.invoke(list, 0, list.getModel().getSize()-1, false);
*/                                
                    }
                } /*catch (NoSuchMethodException e) {
                    e.printStackTrace();
                } catch (InvocationTargetException e) {
                    e.printStackTrace();
                } catch (IllegalArgumentException e) {                 
                    e.printStackTrace();
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }*/ finally {}
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
