package jerco.gui;

import javax.swing.ComboBoxModel;
import javax.swing.event.ListDataListener;

import jerco.network.generators.KagomeGenerator;
import jerco.network.generators.NetGenerator;
import jerco.network.generators.RectGenerator;

class GeneratorComboboxModel implements ComboBoxModel {
    
    private List<NetGenerator> generators = [
       new RectGenerator(),  new KagomeGenerator()
    ] 
    
    private NetGenerator selectedItem = generators[0];
    
    
    public NetGenerator getSelectedItem() {
        return selectedItem;
    }
    
    @Override
    public void setSelectedItem(Object anItem) {
        selectedItem = anItem
        
    }
    @Override
    public void addListDataListener(ListDataListener l) {
 
    }

    @Override
    public Object getElementAt(int index) {
        return generators[index];
    }

    @Override
    public int getSize() {
        return generators.size();
    }

    @Override
    public void removeListDataListener(ListDataListener l) {

    }

}
