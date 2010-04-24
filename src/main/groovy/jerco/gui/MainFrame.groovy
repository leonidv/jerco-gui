package jerco.gui

import java.text.DecimalFormat 
import java.text.NumberFormat;

import java.awt.Cursor;
import java.io.File;

import org.jfree.chart.ChartPanel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JFormattedTextField;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JTabbedPane;
import javax.swing.RootPaneContainer;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileNameExtensionFilter;

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

import com.vygovskiy.controls.SwingUtils;
import com.vygovskiy.controls.fileedit.FileEdit;

import net.miginfocom.swing.MigLayout;



class MainFrame extends JFrame {
    final static Logger LOG = LoggerFactory.getLogger(MainFrame.class);

    final static DEBUG_FILE = "/home/leonidv/Документы/" +
    		"Аспирантура/Программы/matrix.xls";

    final static String DEFAULT_TEMPLATE = "templates/graphviz.ftl"
    
    private JButton loadFileButton;
    
    private String exportName
    
    def BigDecimal pMin
    def BigDecimal pMax
    def BigDecimal pStep
    
    def Integer experimentsCount
    
    private JFreeChart chartPc;
    private JFreeChart chartPa;
    private JFreeChart chartMaxSize;
    private JFreeChart chartClustersCount;
    private JFreeChart chartClusterMeanSize;
    
    private JLabel networkInformation
    
    private JButton buttonRun;
    
    private File chooserDirectory = new File(".")
    
    private JCheckBox exportData;
    
    private FileEdit templateFileEdit
    private FileEdit exportFileEdit
    
    private Net net;
    
    public MainFrame() {
        super("Модель доступности ИОИ");
        initComponents();
        setSize 750, 600

        LOG.info "MainFrame is created"
    }
    
    private void initComponents() {
        this.setLayout new MigLayout()
               
        JButton buttonLoadNet = new JButton("Загрузить структуры из файла")
        buttonLoadNet.actionPerformed = loadNet
        
        JButton buttonGenerateNet = new JButton("Сгенерировать регулярную структуру")
        buttonGenerateNet.actionPerformed = generateNet
        
        networkInformation = new JLabel("Укажите обрабатываемую структуру")
        
        networkInformation.setHorizontalAlignment SwingConstants.CENTER
        
        this.add buttonLoadNet, "width 48%:48%, growx"
        this.add buttonGenerateNet, "width 48%:48%, growx, wrap"
        this.add networkInformation, "span, wrap, width 100%"
        
        JTabbedPane tabbedPane = new JTabbedPane()
        this.add tabbedPane, "h pref:pref:pref, wrap, span, width 98%"
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
              title:"Максимальное количество узлов в кластере", yLabel:"Количество узлов"
            ])
        chartTabbedPane.addTab "Наибольший кластер", new ChartPanel(chartMaxSize)

        chartClusterMeanSize = createChart([
              title:"Среднее количество узлов в кластере", yLabel:"количество узлов"
            ])

        chartTabbedPane.addTab "Средний размер", new ChartPanel(chartClusterMeanSize)
        
        chartClustersCount = createChart([
              title:"Количество образовавшихся кластеров", yLabel:"Количество кластеров"
            ])
        chartTabbedPane.addTab "Количество кластеров", new ChartPanel(chartClustersCount)


        
        this.add chartTabbedPane, "span, h 100%, w 100%"
    }

    /**
     * Создает поле выбора файла.
     *  
     * @param defaultFileNames список имен по умолчанию. Сначала проверяет 
     * первое имя и если файл существует, становится по умолчанию. Потом 
     * следующее и т.д.
     * 
     * @return
     */
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

    def loadNet = {
        JFileChooser chooser = new JFileChooser(chooserDirectory);
        FileNameExtensionFilter filter = 
                new FileNameExtensionFilter("Файлы Excel", "xls");
        chooser.fileFilter = filter;

        
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            changeCursor Cursor.WAIT_CURSOR ;
            
            def file = chooser.selectedFile;
            
            LOG.info "Selected file: ${file}"
            
            net = new NetImpl(new ExcelReader(file))
            networkInformation.text = "Считывается файл. Ждите..."            

            networkInformation.text = file.absolutePath;
            exportName = file.name.split("\\.")[0]

            chooserDirectory = file.parentFile
            
            LOG.info "Network with ${net.size()} elements is loaded"
            
            
            changeCursor Cursor.DEFAULT_CURSOR;
        }
        
    }
    
    private void changeCursor(def cursorType) {
        setCursor(Cursor.getPredefinedCursor(cursorType));
    }
    
    def generateNet = {
        RegularLatticeFrame frame = new RegularLatticeFrame(this);
        SwingUtils.center frame
        frame.setVisible true
        if (frame.approved) {
            networkInformation.text = "Генерация решетки. Ждите..."
            changeCursor Cursor.WAIT_CURSOR;
            
            net = frame.generateNetwork()
            networkInformation.text = frame.netDescription
            exportName = frame.netDescription
            
            changeCursor Cursor.DEFAULT_CURSOR
        }
    }
    
    def runScenario = { ae ->
        buttonRun.enabled = false
        
        final PercolationThresholdScenario scenario = 
            new PercolationThresholdScenario(pMin:pMin, pMax:pMax, pStep:pStep);
        scenario.experimentsCount = experimentsCount
        scenario.net = net
        scenario.frame = this;
        scenario.monitor = new JProgressMonitor(this, false)
        scenario.export = exportData.selected
        scenario.exportFileName = exportName
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
        plotResult chartPc, scenario.pCrititcal
        plotResult chartPa, scenario.pAvailability
        plotResult chartMaxSize, scenario.maxSize
        plotResult chartClusterMeanSize, scenario.meanSize
        plotResult chartClustersCount, scenario.clustersCount
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
        XYSeries series = new XYSeries(exportName)
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