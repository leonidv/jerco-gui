package jerco.gui;

import java.awt.Component;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JList;
import javax.swing.ListCellRenderer;

class GeneratorComboRender implements ListCellRenderer {
    private DefaultListCellRenderer render = new DefaultListCellRenderer();
    @Override
    public Component getListCellRendererComponent(JList list, Object value,
            int index, boolean isSelected, boolean cellHasFocus) {
        
        return render.getListCellRendererComponent(list, value.name, index, isSelected, cellHasFocus);
    }

}
