package jerco.gui;

import java.awt.Frame;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JSpinner;
import javax.swing.tree.FixedHeightLayoutCache.VisibleFHTreeStateNodeEnumeration;
import javax.swing.JCheckBox;
import jerco.network.Net;
import jerco.network.NetStructureInfo;
import jerco.network.RegularLattice;
import jerco.network.generators.LeftRightBoundsWrapper;
import net.miginfocom.swing.MigLayout;

class RegularLatticeFrame extends JDialog {
    
    public static void main(String[] args) {
        RegularLatticeFrame frame = new RegularLatticeFrame(null);
        frame.setDefaultCloseOperation JFrame.DISPOSE_ON_CLOSE
        frame.setVisible true;
    }
    
    private JComboBox comboGenerator;
    
    private JSpinner spinnerWidth;
    
    private JSpinner spinnerHeight;
    
    private JCheckBox moreBound;
    
    def boolean approved;
    
    public RegularLatticeFrame(Frame owner) {
        super(owner, true);

        title = "Параметры решетки"
        initComponents();
        pack();
    }

    def initComponents() {
        setLayout new MigLayout("wrap 2, fillx","[right][left]","[]5px[]3px[]10px[nogrid]");
        
        add new JLabel("Структура решетки: ")
        comboGenerator = new JComboBox(new GeneratorComboboxModel());     
        comboGenerator.renderer = new GeneratorComboRender();
        
        add comboGenerator, "width 100%";
        
        add new JLabel("Узлов в одном слое:")
        spinnerWidth = new JSpinner();
        initSpinnerModel spinnerWidth;
        add spinnerWidth, "width 100%"
        
        add new JLabel("Количество слоев:");
        spinnerHeight = new JSpinner();
        initSpinnerModel spinnerHeight;
        add spinnerHeight, "width 100%";
        
        moreBound = new JCheckBox("Дополнителеные границы")
        add moreBound, "span, wrap";
        
        JButton button = new JButton("Отмена")
        button.actionPerformed = onCancelClick;
        add button, "tag cancel, width 30%, growx"
        
        button = new JButton("Создать решетку")
        button.actionPerformed = onOkClick;
        add button, "tag ok, width 60%, growx"
    }

    private void initSpinnerModel(spinner) {
        def model = spinner.model
        model.value = 500;
        model.stepSize = 50;
        model.minimum = 50;
    }
    
    public Net generateNetwork() {
        def structure = new NetStructureInfo();
        structure.generator = comboGenerator.model.selectedItem;
        structure.height = spinnerHeight.model.value
        structure.width = spinnerWidth.model.value
        if (moreBound.isSelected()) 
        { 
        	structure.addWrapper new LeftRightBoundsWrapper()
        }
        return new RegularLattice(structure)
    }

    def onOkClick = {
        approved = true
        visible  = false
    }

    def onCancelClick = {
        approved = false;
        visible = false;
    }

    public String getNetDescription() {
        String.format("%s %dx%d", comboGenerator.selectedItem.name, 
          spinnerWidth.value, spinnerHeight.value)
    }
}
