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
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.ProgressMonitor;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentListener;

import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jerco.network.ExcelReader;
import jerco.network.Net;
import jerco.network.NetImpl;
import jerco.scenario.PercolationThresholdScenario;

import com.sun.org.apache.xerces.internal.impl.dtd.models.CMStateSet;
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
    
    private JFreeChart chartPc;
    private JFreeChart chartPa;
    private JFreeChart chartMaxSize;
    
    private Net net;
    
    private JButton buttonRun;
    
    public MainFrame() {
        super("Модель доступности ИОИ");
        initComponents();
        setSize 400, 600

        LOG.info "MainFrame is created"
    }
    
    private void initComponents() {
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
        input =  createDecimalInput("pStep", new ValueRange(min:1E-6, max:1E-2, init:1E-3))
        this.add input, "wrap, w 100%"
        
        this.add new JLabel("N эксп");
        input = createIntegerInput ("ExperimentsCount", new ValueRange(min:100, max:10000, init:1000))
        this.add input, "wrap, growx"

        
        buttonRun = new JButton("Запустить моделирование")
        buttonRun.actionPerformed = loadNetwork
        this.add buttonRun, "span, growx"
        
        JTabbedPane chartTabbedPane = new JTabbedPane();
        
        chartPc = createChart([
              title:"Сохранения связи между КУ",yLabel:"p связанности",
            ])
        chartTabbedPane.addTab "Связанность КУ", new ChartPanel(chartPc)
        
        chartPa = createChart([
              title:"Доступность", yLabel:"p доступности"
            ])
        chartTabbedPane.addTab "Доступность", new ChartPanel(chartPa)
        
        chartMaxSize = createChart([
              title:"Максимальный кластер", yLabel:"Количество узлов"
            ])
        chartTabbedPane.addTab "Наибольший кластер", new ChartPanel(chartMaxSize)

        
        this.add chartTabbedPane, "span, h 100%, w 100%"
    }

    private JFreeChart createChart(params) {
        if (params.xLabel == null) {
            params.put "xLabel", "p устойчивости"
        }

        if (params.dataset == null) {
            params.put "dataset", new XYSeriesCollection()
        }
        
        return ChartFactory.createXYLineChart(params.title, 
            params.xLabel, params.yLabel, params.dataset, 
            PlotOrientation.VERTICAL,
            false, false, false)
    }
    
    def loadNetwork = { ae ->
        buttonRun.enabled = false
        LOG.info "Selected file: ${fileEdit.file}"
        net = new NetImpl(new ExcelReader(fileEdit.getFile()))
        LOG.info "Network with ${net.size()} elements is loaded"
               
        final PercolationThresholdScenario scenario = 
            new PercolationThresholdScenario(pMin:pMin, pMax:pMax, pStep:pStep);
        scenario.net = net
        scenario.frame = this;
        scenario.monitor = new JProgressMonitor(this, false)
        
        SwingUtilities.invokeLater {
            new ScenarioRunner(scenario:scenario).start();
        } as Runnable; 
        
    };
    
    
    public void onExperimentFinished(scenario) {
        LOG.info "Experiment is finished"
        SwingUtilities.invokeLater {
            buttonRun.enabled = true
        } as Runnable
        plotResult chartPc, scenario.pCrititcal;
        plotResult chartPa, scenario.pAvailability;
        plotResult chartMaxSize, scenario.maxSize
    }
    
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
            LOG.debug "${field} → ${value}"
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
            def Integer value = result.value;
                      
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
            LOG.debug "${field} → ${value}"
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

    private void plotResult(JFreeChart chart, Map map) {
        XYSeries series = new XYSeries(map.hashCode())
        map.each { 
            series.add it.key, it.value
        }
        chart.plot.dataset.addSeries series
    }
}


def class ValueRange {
    def init;
    def max;
    def min;
}

def class ScenarioRunner extends Thread {
    def scenario;

    @Override
    public void run() {
        scenario.run();
    }
    
}