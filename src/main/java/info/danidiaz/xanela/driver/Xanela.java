package info.danidiaz.xanela.driver;

import java.awt.Component;
import java.awt.Point;
import java.awt.Window;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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
    
    private List<Component> componentArray = new ArrayList<Component>();
    private List<JMenuItem> menuArray = new ArrayList<JMenuItem>();
    
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
        
        packer.write((int)xid);
        
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

        JMenuBar menubar = null;
        if (w instanceof JFrame) {
            menubar = ((JFrame)w).getJMenuBar();
        } else if (w instanceof JDialog) {
            menubar = ((JDialog)w).getJMenuBar();                                    
        }
        if (menubar==null) {
            packer.writeNil();
        } else {
            writeMenuBar(xid, packer, menubar);
        }
        
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
        
        
        RootPaneContainer rpc = (RootPaneContainer)w;
        writeComponent(xid, packer, (JComponent) rpc.getContentPane(),w);                                                               
        
        writeWindowArray(xid, packer, w.getOwnedWindows());
    }
    
    private void writeMenuBar(int xid, Packer packer, JMenuBar menubar) throws IOException {
        packer.writeArrayBegin(menubar.getMenuCount());
        for (int i=0; i<menubar.getMenuCount();i++) {
            writeMenuItem(xid, packer,menubar.getMenu(i),Collections.<Integer>emptyList());
        }
        packer.writeArrayEnd();
    }


    private void writeMenuItem(int xid, Packer packer, JMenuItem menuItem, List<Integer> idList) throws IOException {
                
        int componentId = menuArray.size();
        menuArray.add(menuItem);
        List<Integer> newIdList = new ArrayList<Integer>(idList);
        newIdList.add(componentId);
        
        packer.write((int)xid);
        
        writePotentiallyNullString(packer,menuItem.getName());
        packer.write(menuItem.getText());
        if (menuItem instanceof JCheckBoxMenuItem) {
            packer.write(((JCheckBoxMenuItem)menuItem).isSelected());
        } else if (menuItem instanceof JRadioButtonMenuItem) {
            packer.write(((JRadioButtonMenuItem)menuItem).isSelected());
        } else {
            packer.writeNil();
        }
        packer.write(menuItem.isEnabled());
        
        packer.writeArrayBegin(newIdList.size());
        for (Integer i:newIdList) {
            packer.write(i);
        }
        packer.writeArrayEnd();
        
        if (menuItem instanceof JMenu) {
            JMenu menu = (JMenu) menuItem;
            packer.writeArrayBegin(menu.getMenuComponentCount());
            for (int i=0;i<menu.getMenuComponentCount();i++) {
                writeMenuItem(xid, packer, (JMenuItem)menu.getMenuComponent(i),newIdList);
            }
            packer.writeArrayEnd();    
        } else {
            packer.writeArrayBegin(0);
            packer.writeArrayEnd();
        }
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

    public void clickMenu(final int menuPath[]) {       
        for (int i=0;i<menuPath.length;i++) {
            final JMenuItem menuItem = menuArray.get(menuPath[i]);
            
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    menuItem.doClick();
                }                       
            });
        }

    }
    
    public void rightClick(final int cId) {
        
        System.out.println("entering right-click");
        
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
}
