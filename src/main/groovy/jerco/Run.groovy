package jerco;

import jerco.network.RegularLattice;

import com.sun.org.apache.bcel.internal.generic.NEW;

import org.jvnet.substance.skin.SubstanceBusinessBlueSteelLookAndFeel;

import com.vygovskiy.controls.SwingUtils;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.UIManager; 
import javax.xml.bind.Marshaller;

import javax.xml.bind.Marshaller;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;

import jerco.gui.MainFrame;
import jerco.network.Net;
import jerco.network.NetStructureInfo;
import jerco.network.generators.KagomeGenerator;
import jerco.network.generators.NetGenerator;
import jerco.network.generators.RectGenerator;
import jerco.scenario.PercolationThresholdScenario;


if (args.size() > 0) {
    runCli()
} else {
    runGui();
}

def runGui() {
    SwingUtilities.invokeLater {
        JFrame.setDefaultLookAndFeelDecorated(true);
        UIManager.setLookAndFeel(new SubstanceBusinessBlueSteelLookAndFeel());
        
        def mainFrame = new MainFrame();
        mainFrame.setDefaultCloseOperation JFrame.EXIT_ON_CLOSE
        SwingUtils.center mainFrame
        mainFrame.setVisible true        
    } as Runnable;
}

def runCli() {
    CliBuilder cliBuilder = new CliBuilder()
    cliBuilder.usage = "--properties settings.xml --structure [rect|kagome] --size N"
    cliBuilder.with {
        p longOpt: "properties", "file with scenario run settings", args: 1, required: true
        s longOpt: "structure", "structure of network: rect (rectangle) or kagome", args: 1, required: true
        n longOpt: "size", "size of network, integer", args: 1, required: true
    }
    
    
    cli = cliBuilder.parse(args)
    if (cli == null) {
        return;
    }
    
    def structure = cli.getOptionValue("s")
    def int size = cli.getOptionValue("n").toInteger()
    
    def String settingsFile = cli.getOptionValue("p")
    
    JAXBContext context = JAXBContext.newInstance(PercolationThresholdScenario.class)
    Unmarshaller um = context.createUnmarshaller();
    PercolationThresholdScenario scenario = um.unmarshal(new File(settingsFile))
    scenario.exportFileName += "${structure}-${size}x${size}"
    scenario.export = true
    
    Marshaller m = context.createMarshaller();
    m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);    
    m.marshal(scenario , new PrintWriter(System.out))
    
    def NetGenerator generator;
    switch (structure) {
        case "rect": 
            generator = new RectGenerator();
            break;
        case "kagome":
            generator = new KagomeGenerator();
            break;
    }    
    
    NetStructureInfo netStructure = new NetStructureInfo()
    netStructure.width = size;
    netStructure.height = size;
    netStructure.generator = generator;
    
    
    print "generate network, please wait..."
    long start = System.currentTimeMillis();
    Net net = new RegularLattice();
    net.generate netStructure;
    long end = System.currentTimeMillis();
    println "ok, done in ${(end-start)/1000}s"    
    
    scenario.net = net;
    scenario.run()
    
}