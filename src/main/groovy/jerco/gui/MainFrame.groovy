package jerco.gui

import org.jfree.chart.ChartPanel;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JSeparator;
import javax.swing.JTextField;

import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jerco.network.ExcelReader;
import jerco.network.Net;
import jerco.network.NetImpl;
import jerco.scenario.PercolationThresholdScenario;

import com.vygovskiy.controls.fileedit.FileEdit;

import net.miginfocom.swing.MigLayout;



class MainFrame extends JFrame {
    final static Logger LOG = LoggerFactory.getLogger(MainFrame.class);

    final static DEBUG_FILE = "/home/leonidv/Документы/Аспирантура/" +
    		"Программы/jerco-api/src/test/resources/star.xls"
    
    private JButton loadFileButton;
    
    private FileEdit fileEdit
    
    private XYSeriesCollection dataset;
    
    private JFreeChart chart;
    
    private Net net;
    
    public MainFrame() {
        super("Модель доступности ИОИ");
        initComponents();
        setSize 400, 600
    }
    
    def private initComponents() {
        this.setLayout new MigLayout("","[fill, grow]")
        
        if ((new File(DEBUG_FILE).exists() )) {
            fileEdit = new FileEdit(DEBUG_FILE)
        } else {
            fileEdit = new FileEdit();
        }
        
        fileEdit.setButtonText "Загрузить"
        fileEdit.fileSelected = loadNetwork
        
        this.add fileEdit, "wrap, span"
        
        this.add new JSeparator(), "wrap, span"
        
        this.add new JLabel("P min");
        this.add new JTextField(), "growx";
        this.add new JLabel("P max");
        this.add new JTextField();
        this.add new JLabel("P шаг");
        this.add new JTextField(), "wrap"

        
        
        dataset = new XYSeriesCollection();
        chart = 
        ChartFactory.createXYLineChart("Вероятность образования " +
        		"перколяционного кластера", 
        "p заражения", "p кластера", dataset, PlotOrientation.VERTICAL,
        false, false, false)
        
        this.add new ChartPanel(chart)
    }
    
    def loadNetwork = { ae ->
        println "Selected file: ${ae.selectedFileName}"
        LOG.trace("Selected file: ${ae.selectedFileName}")
        net = new NetImpl(new ExcelReader(fileEdit.getFile()))
        
        PercolationThresholdScenario scenario = new PercolationThresholdScenario();
        scenario.net = net
        scenario.run()

        println "Experiment is finished"
        XYSeries data = new XYSeries(1);
        
        scenario.result.each {
            data.add it.key, it.value
        }               
        dataset.addSeries data
        assert chart != null
        chart.getXYPlot().dataset = dataset
        
        println "Network with ${net.size()} elements is loaded"
    };

    
}
