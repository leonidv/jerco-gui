package jerco.scenario;

import java.math.BigDecimal;
import java.util.HashMap;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import jerco.gui.JProgressMonitor 
import jerco.gui.MainFrame 
import jerco.network.Cluster 
import jerco.network.Net;

@XmlAccessorType( XmlAccessType.NONE )
@XmlRootElement(name="scenario")
public class PercolationThresholdScenario implements Runnable {
    final static Logger LOG = LoggerFactory.getLogger(PercolationThresholdScenario.class);
    
    def Net net;
    
    @XmlElement
    def BigDecimal pMin = 0.0;
    @XmlElement
    def BigDecimal pMax = 1.0;
    @XmlElement
    def BigDecimal pStep = 0.001;
    
    @XmlElement
    def int experimentsCount = 1000;
    
    def Map<BigDecimal,BigDecimal> pCrititcal;
    def Map<BigDecimal,BigDecimal> pAvailability;
    def Map<BigDecimal,Integer> maxSize;
    def Map<BigDecimal,BigDecimal> meanSize;
    def Map<BigDecimal,BigDecimal> clustersCount;
    
    def  JProgressMonitor monitor
    def  MainFrame frame
    
    def boolean export = false
    
    @XmlElement
    def String exportFileName = ""
    
    def File filePc
    def File filePa
    def File fileClusterMaximumSize
    def File fileClustersCount
    def File fileClusterMeanSize
    
    private int progress = 0;
    
    def void run() {
        pCrititcal = new HashMap<BigDecimal, BigDecimal>();
        pAvailability = new HashMap<BigDecimal, BigDecimal>();
        maxSize = new HashMap<BigDecimal, Integer>()
        meanSize = new HashMap<BigDecimal, BigDecimal>();
        clustersCount = new HashMap<BigDecimal, BigDecimal>();
        
        
        def p = pMin;
        
        def totalExperiments = (pMax-pMin)/pStep*experimentsCount
        if (monitor) {
            monitor.maximum = totalExperiments.intValue()
            monitor.visible = true;
        }
        
        if (export) {
            String date = new Date().format("yyyy-MM-dd_HH-mm-ss");
            String prefix = "${date}-${exportFileName}-${experimentsCount}"
            filePc = new File(prefix + "-pc.dat")
            filePa = new File(prefix + "-pa.dat")
            fileClusterMaximumSize = new File(prefix + "-clusters-max-size.dat");
            fileClustersCount = new File(prefix+"-clusters-counts.dat")
            fileClusterMeanSize = new File(prefix+"-clusters-mean-size.dat")
        }
        
        while (p < 1) {
            p += pStep
            Result result = experiment(p)
            
            LOG.info "p = ${p}, ${result}"
            
            pCrititcal.put p, result.pCritical
            pAvailability.put p, result.pAvailability
            maxSize.put p, result.clusterMaximumSize
            meanSize.put p, result.clustersMeanSize
            clustersCount.put p, result.clustersCount 
            
            if (monitor) {
                if (monitor.isCanceled) {
                    break
                }
            }
            
        }
        
        if (monitor) {
            monitor.dispose()
            frame.onExperimentFinished(this)
        }
    }
    
    private Result experiment(p) {
        int percolationClusterCounter = 0;
        BigDecimal availabilityCounter = 0.0;
        int maximumSize = 0;
        
        int clustersCountBuffer = 0;
        BigDecimal meanSizeBuffer = 0;
        
        if (export) {
            fileClusterMaximumSize.append "${p}"
            fileClustersCount.append "${p}"
            fileClusterMeanSize.append "${p}"
        }
        
        for (i in 1..experimentsCount) {
            net.reset()
            net.infect p
            
            if (net.hasPercolationCluster()) {
                percolationClusterCounter++   
                
                List<Cluster> clusters = net.percolationClusters.reverse()
                BigDecimal pA = clusters[0].size() / net.size();                
                LOG.debug "pAvailability = ${pA}"
                
                availabilityCounter += pA;                 
                
                LOG.debug "availabilityCounter = ${availabilityCounter}"
            }
            
            def clusters = net.clusters
            
            if (!clusters.isEmpty()) {
                clustersCountBuffer += clusters.size()
                meanSizeBuffer += meanSize(clusters);
                
                clusters = clusters.reverse()                               
                def maximum = clusters[0].size()                
                maximumSize += maximum
                
                if (export) {
                    fileClusterMaximumSize.append " ${maximumSize}"
                    fileClustersCount.append " ${clusters.size()}"  
                    fileClusterMeanSize.append " ${meanSize(clusters)}"
                }
                
            }
            
            if (monitor) {
                monitor.inc()
                if (monitor.isCanceled) {
                    return;
                }
            }
            
        }
        
        
        def result = new Result(
                pCritical: percolationClusterCounter / experimentsCount,
                pAvailability: availabilityCounter / experimentsCount,
                clustersCount: clustersCountBuffer / experimentsCount,
                clusterMaximumSize: maximumSize/ experimentsCount,
                clustersMeanSize: meanSizeBuffer / experimentsCount
                )
        
        if (export) {
            filePc.append "${p} ${result.pCritical}\n"
            filePa.append "${p} ${result.pAvailability}\n"
            fileClusterMaximumSize.append "\n"
            fileClustersCount.append "\n"
            fileClusterMeanSize.append "\n"
        }
        
        return result;
    }
    
    BigDecimal meanSize (Collection<Cluster> clusters) {
        int buff = 0;
        clusters.each{ buff += it.size() }
        return buff/clusters.size()
    }
}

def class Result {
    def pCritical
    def pAvailability
    def clusterMaximumSize
    def clustersCount
    def clustersMeanSize
    
    @Override
    public String toString() {
        "pCritical = ${pCritical}, pAvailability = ${pAvailability}, " +
        "maxSize = ${clusterMaximumSize}"
    }    
    
    
}