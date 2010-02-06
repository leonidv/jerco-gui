package jerco.gui

import java.text.DecimalFormat 
import java.text.NumberFormat;

import java.io.File;

import org.jfree.chart.ChartPanel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFormattedTextField;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JTabbedPane;
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

import jerco.network.Net;
import jerco.network.NetImpl;
import jerco.network.io.ExcelReader 
import jerco.network.io.FreemakerWriter;
import jerco.scenario.PercolationThresholdScenario;

import com.vygovskiy.controls.fileedit.FileEdit;

import net.miginfocom.swing.MigLayout;



class MainFrame extends JFrame {
    final static Logger LOG = LoggerFactory.getLogger(MainFrame.class);

    final static DEBUG_FILE = "/home/leonidv/Документы/" +
    		"Аспирантура/Программы/matrix.xls";

    final static String DEFAULT_TEMPLATE = "templates/graphviz.ftl"
    
    private JButton loadFileButton;
    
    private FileEdit fileEdit
    
    def BigDecimal pMin
    def BigDecimal pMax
    def BigDecimal pStep
    
    def Integer experimentsCount
    
    private JFreeChart chartPc;
    private JFreeChart chartPa;
    private JFreeChart chartMaxSize;
    
    private JButton buttonRun;
    
    private JCheckBox exportData;
    
    private FileEdit templateFileEdit
    private FileEdit exportFileEdit
    
    
    public MainFrame() {
        super("Модель доступности ИОИ");
        initComponents();
        setSize 700, 600

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
        
        JTabbedPane tabbedPane = new JTabbedPane()
        this.add tabbedPane, "north, h pref:pref:pref"
        tabbedPane.addTab "Моделирование", createScenarioPanel()
        tabbedPane.addTab "Экспорт структуры", createExportPanel()
        
        JTabbedPane chartTabbedPane = new JTabbedPane();
        
        chartPc = createChart([
              title:"Сохранения связи между КУ", yLabel:"p связанности",
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

    private FileEdit createFileEdit(String ... defaultFileNames) {
        FileEdit fileEdit = null;
        for (String fileName : defaultFileNames) {
            File f = new File(fileName);
            if (f.isFile()) {
                fileEdit = new FileEdit(f.absolutePath);
                break
            }
            
            if (f.isDirectory()) {
                fileEdit = new FileEdit(f);
                break;
            }
        }
        
        if (fileEdit == null) {
            fileEdit = new FileEdit(new File("."));
        }
        fileEdit.buttonText = "Выбрать"
               
        return fileEdit
    }
    
    /**
     * Создает панель настроек сценария.
     * @return
     */
    private JPanel createScenarioPanel() {
        def rows = "[][fill,100%]20px[][fill,100%]20px[][fill,100%]"
        JPanel scenarioPanel = new JPanel(new MigLayout("",rows));
        
        scenarioPanel.add new JLabel("P min")
        def input = createDecimalInput("pMin", new ValueRange(min:0, max:1, init:0))
        scenarioPanel.add input
        
        input = createDecimalInput("pMax", new ValueRange(min:0, max:1, init:1))
        scenarioPanel.add new JLabel("P max")
        scenarioPanel.add input
        
        scenarioPanel.add new JLabel("P шаг");
        input =  createDecimalInput("pStep", new ValueRange(min:1E-6, max:1E-2, init:1E-3))
        scenarioPanel.add input, "wrap"
                
        scenarioPanel.add new JLabel("N эксп");
        input = createIntegerInput ("ExperimentsCount", new ValueRange(min:100, max:10000, init:1000))
        scenarioPanel.add input
        
        exportData = new JCheckBox("Экспортировать значения")
        exportData.toolTipText = "Сохранять все вычисленные значения в файл"
        scenarioPanel.add exportData, "span, wrap"
        
        
        buttonRun = new JButton("Запустить моделирование")
        buttonRun.actionPerformed = runScenario
        scenarioPanel.add buttonRun, "south"
        
        return scenarioPanel
    }
    
    private JPanel createExportPanel() {
        JPanel exportPanel = new JPanel(new MigLayout())
        
        exportPanel.add new JLabel("Шаблон: ")
        
        templateFileEdit = createFileEdit(
            "src/main/distribute/templates/graphviz.ftl",
            "templates/graphviz.ftl")
        exportPanel.add templateFileEdit, "w 100%, wrap"
        
        exportPanel.add new JLabel("Результат: ")
        exportFileEdit = createFileEdit()
        exportPanel.add exportFileEdit, "w 100%, wrap"
        
        JButton exportButton = new JButton("Экспортировать структуру сети");
        exportButton.actionPerformed = exportStructure
        exportPanel.add exportButton, "south"
        
        return exportPanel
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

    private Net loadNet() {
        LOG.info "Selected file: ${fileEdit.file}"
        Net net = new NetImpl(new ExcelReader(fileEdit.getFile()))
        LOG.info "Network with ${net.size()} elements is loaded"
        return net;
    }
    
    def runScenario = { ae ->
        buttonRun.enabled = false
        Net net = loadNet()
        
        final PercolationThresholdScenario scenario = 
            new PercolationThresholdScenario(pMin:pMin, pMax:pMax, pStep:pStep);
        scenario.experimentsCount = experimentsCount
        scenario.net = net
        scenario.frame = this;
        scenario.monitor = new JProgressMonitor(this, false)
        scenario.export = exportData.selected
        if (scenario.export) {
            String exportFileName = fileEdit.file.name
            
            exportFileName = exportFileName.split("\\.")[0]+"_"
            exportFileName += new Date().format("yyyy-MM-dd_HH-mm-ss");
            exportFileName += ".data"
                                                         
            scenario.exportFileName = exportFileName
            LOG.info "Experiments data will be exported to [${exportFileName}]"
        }
        
        SwingUtilities.invokeLater {
            new ScenarioRunner(scenario:scenario).start();
        } as Runnable; 
        
    }
    
    def exportStructure = {
        Net net = loadNet()
        
        FreemakerWriter exporter = new FreemakerWriter(net);
        
        String templateFileName = templateFileEdit.getFile().absolutePath
        exporter.loadTemplate templateFileEdit.getFile().absolutePath
        LOG.info "Template is loaded: "+templateFileName
        
        String resultFileName = exportFileEdit.getFile().absolutePath
        exporter.write exportFileEdit.getFile().absolutePath
        LOG.info "Net is exported: "+resultFileName
    }
    
    public void onExperimentFinished(scenario) {
        LOG.info "Experiment is finished"
        SwingUtilities.invokeLater { buttonRun.enabled = true } as Runnable
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
        XYSeries series = new XYSeries(fileEdit.file.name)
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