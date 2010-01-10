package jerco;

import org.jvnet.substance.skin.SubstanceBusinessBlueSteelLookAndFeel;

import com.vygovskiy.controls.SwingUtils;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import jerco.gui.MainFrame;


SwingUtilities.invokeLater {
    JFrame.setDefaultLookAndFeelDecorated(true);
    UIManager.setLookAndFeel(new SubstanceBusinessBlueSteelLookAndFeel());
    
    def mainFrame = new MainFrame();
    mainFrame.setDefaultCloseOperation JFrame.EXIT_ON_CLOSE
    SwingUtils.center mainFrame
    mainFrame.setVisible true        
} as Runnable;

