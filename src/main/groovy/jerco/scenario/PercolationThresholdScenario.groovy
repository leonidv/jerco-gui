package jerco.scenario;

import java.math.BigDecimal;
import java.util.HashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import jerco.gui.JProgressMonitor 
import jerco.gui.MainFrame 
import jerco.network.Cluster 
import jerco.network.Net;

class PercolationThresholdScenario implements Runnable {
    final static Logger LOG = LoggerFactory.getLogger(PercolationThresholdScenario.class);
    
    def Net net;
    
    def pMin = 0.0;
    def pMax = 1.0;    
    def pStep = 0.001;
    
    def experimentsCount = 1000;
    
    def Map<BigDecimal,BigDecimal> pCrititcal;
    def Map<BigDecimal,BigDecimal> pAvailability;
    def Map<BigDecimal,Integer> maxSize;
    
    def  JProgressMonitor monitor
    def  MainFrame frame
    
    private int progress = 0;
    
    def void run() {
        pCrititcal = new HashMap<BigDecimal, BigDecimal>();
        pAvailability = new HashMap<BigDecimal, BigDecimal>();
        maxSize = new HashMap<BigDecimal, Integer>()
        
        def p = pMin;
        
        def totalExperiments = (pMax-pMin)/pStep*experimentsCount
        monitor.maximum = totalExperiments.intValue()
        monitor.visible = true;
        
        while (p < 1) {
            p += pStep
            Result result = experiment(p)
            
            LOG.info "p = ${p}, ${result}"
            
            pCrititcal.put p, result.pCritical
            pAvailability.put p, result.pAvailability
            maxSize.put p, result.maximumSize
            
            if (monitor.isCanceled) {
                break
            }
            
        }
        monitor.dispose()
        frame.onExperimentFinished(this)
    }
    
    /**
     * Возвращает количество возникновений перколяционного кластера.
     * @return
     */
    private Result experiment(p) {
        int percolationClusterCounter = 0;
        BigDecimal availabilityCounter = 0.0;
        int maximumSize = 0;
        
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
                clusters = clusters.reverse()
                LOG.debug("p = ${p}, clusters found: ${clusters.size()}, " +
                        "maximum cluster: ${clusters[0].size()}");
                
                maximumSize += clusters[0].size()
            }
            
            monitor.inc()
            if (monitor.isCanceled) {
                return;
            }
            
        }
        return new Result(
        pCritical: percolationClusterCounter / experimentsCount,
        pAvailability: availabilityCounter / experimentsCount,
        maximumSize: maximumSize / experimentsCount
        )
        
    }
}

def class Result {
    def pCritical
    def pAvailability
    def maximumSize
    
    @Override
    public String toString() {
        "pCritical = ${pCritical}, pAvailability = ${pAvailability}, " +
        "maxSize = ${maximumSize}"
    }    
    
    
}