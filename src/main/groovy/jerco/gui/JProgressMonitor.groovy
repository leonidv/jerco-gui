package jerco.gui;

import java.awt.BorderLayout;
import java.awt.Dialog;
import java.awt.Frame 

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;

import com.vygovskiy.controls.SwingUtils;


class JProgressMonitor extends JDialog {
    def maximum;
    def isCanceled;
    
    private JProgressBar progressBar
    
    public JProgressMonitor(Frame owner, boolean modal) {
        super(owner, modal);
        title = "Моделирование"
        setSize 700, 75;
        SwingUtils.center this
            
        layout = new BorderLayout()
            
        progressBar = new JProgressBar()
        
        add progressBar, BorderLayout.CENTER;

        JButton cancelButton = new JButton("Прервать моделирование")
        add cancelButton, BorderLayout.SOUTH;

        cancelButton.actionPerformed = {
            isCanceled = true 
            dispose()
        };
    }

    public void setMaximum(int value) {
        SwingUtilities.invokeLater {
            progressBar.setMaximum value
        } as Runnable
    }
    
    public void inc() {
        SwingUtilities.invokeLater {
            progressBar.value++
        } as Runnable;
    }
}
