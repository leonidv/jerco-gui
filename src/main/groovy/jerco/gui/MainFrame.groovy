package jerco.gui

import java.math.BigDecimal;
import java.text.DecimalFormat 
import java.text.NumberFormat;

import org.jfree.chart.ChartPanel;
import javax.swing.JButton;
import javax.swing.JFormattedTextField;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JSeparator;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentListener;

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
    
    def BigDecimal pMin
    def BigDecimal pMax
    def BigDecimal pStep
    
    def Integer experimentsCount
    
    private XYSeriesCollection dataset;
    
    private JFreeChart chart;
    
    private Net net;
    
    public MainFrame() {
        super("Модель доступности ИОИ");
        initComponents();
        setSize 400, 600
    }
    
    def private initComponents() {
        this.setLayout new MigLayout()
        
        if ((new File(DEBUG_FILE).exists() )) {
            fileEdit = new FileEdit(DEBUG_FILE)
        } else {
            fileEdit = new FileEdit();
        }
        
        fileEdit.buttonText = "Загрузить"
        
        this.add fileEdit, "north, gap 2px 2px 2px 10px"
        
        this.add new JSeparator(JSeparator.HORIZONTAL), "span, w 100%"
               
        this.add new JLabel("P min")
        def input = createDecimalInput("pMin", new ValueRange(min:0, max:1, init:0))
        this.add input, "w 100%";

        input = createDecimalInput("pMax", new ValueRange(min:0, max:1, init:1))
        this.add new JLabel("P max")
        this.add input, "w 100%"
        
        this.add new JLabel("P шаг");
        input =  createDecimalInput("pStep", new ValueRange(min:1E-6, max:1E-2, init:1E-4))
        this.add input, "wrap, w 100%"
        
        this.add new JLabel("N эксп");
        input = createIntegerInput ("ExperimentsCount", new ValueRange(min:100, max:10000, init:1000))
        this.add input, "wrap, growx"

        
        JButton buttonRun = new JButton("Запустить моделирование")
        buttonRun.actionPerformed = loadNetwork
        this.add buttonRun, "span, growx"
        
        
        dataset = new XYSeriesCollection();
        chart = 
        ChartFactory.createXYLineChart("Вероятность образования " +
        		"перколяционного кластера", 
        "p заражения", "p кластера", dataset, PlotOrientation.VERTICAL,
        false, false, false)
        
        this.add new ChartPanel(chart), "south"
    }
    
    def loadNetwork = { ae ->
        println "Selected file: ${fileEdit.file}"
        LOG.trace("Selected file: ${fileEdit.file}")
        net = new NetImpl(new ExcelReader(fileEdit.getFile()))
        println "Network with ${net.size()} elements is loaded"
        
        PercolationThresholdScenario scenario = 
            new PercolationThresholdScenario(pMin:pMin, pMax:pMax, pStep:pStep, 
                    net:net);
        scenario.net = net
        scenario.run()

        println "Experiment is finished"
        XYSeries dataPc = new XYSeries(1);
        scenario.pCrititcal.each {
            dataPc.add it.key, it.value
        }             
        
        XYSeries dataPa = new XYSeries(2)
        scenario.pAvailability.each { 
            dataPa.add it.key, it.value
        }
        dataset.addSeries dataPc
        dataset.addSeries dataPa
        
        assert chart != null
        chart.getXYPlot().dataset = dataset
        
    };

    def createDecimalInput(String field, ValueRange valueRange) {
        DecimalFormat format = NumberFormat.getNumberInstance() as DecimalFormat 
        format.maximumFractionDigits = 6
        
        JFormattedTextField result = new JFormattedTextField(format)
        result.setValue valueRange.init
        
        this."set${field}" valueRange.init
        
        def bind = {
            String text = result.getText()
            def BigDecimal value;
            if ((text == null) || (text.isEmpty())) {
                value = valueRange.min
            } else {
                value = new BigDecimal(text.replace( ',', '.'))
            }
                        
            if (value < valueRange.min) {
                value = valueRange.min.toDouble()
                SwingUtilities.invokeLater({result.setValue(value)})                
                return
            } 
            if (value > valueRange.max) {
                value = valueRange.max.toDouble()
                SwingUtilities.invokeLater({result.setValue(value)})                
                return                
            }
            
            this."set${field}" value
            println "${field} → ${value}"
        }
        
        def onChange = [ 
           changedUpdate: {},
           insertUpdate: bind,
           removeUpdate: bind
        ] as DocumentListener
        
        result.getDocument().addDocumentListener onChange 
        result.actionPerformed = bind 
        result.setHorizontalAlignment SwingConstants.RIGHT;
        return result
    }

    def createIntegerInput(String field, ValueRange valueRange) {
        def format = NumberFormat.getNumberInstance() 
        
        JFormattedTextField result = new JFormattedTextField(format)
        result.setValue valueRange.init
        
        this."set${field}" valueRange.init
        
        def bind = {
            //String text = result.getValue()
            def Integer value = result.value;
            
//            if ((text == null) || (text.isEmpty())) {
//                value = valueRange.min
//            } else {
//                value = new Integer.valueOf i(text.replace( ',', '.'))
//            }
            
            if (value < valueRange.min) {
                value = valueRange.min
                SwingUtilities.invokeLater({result.setValue(value)})                
                return
            } 
            if (value > valueRange.max) {
                value = valueRange.max
                SwingUtilities.invokeLater({result.setValue(value)})                
                return                
            }
            
            this."set${field}" value
            println "${field} → ${value}"
          }
        
        def onChange = [ 
                changedUpdate: {},
                insertUpdate: bind,
                removeUpdate: bind
                ] as DocumentListener
        
        result.getDocument().addDocumentListener onChange 
        result.actionPerformed = bind 
        result.setHorizontalAlignment SwingConstants.RIGHT;
        return result
    }
}


def class ValueRange {
    def init;
    def max;
    def min;
}