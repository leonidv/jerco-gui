package jerco.scenario;

import java.math.BigDecimal;
import java.util.HashMap;


import jerco.gui.JProgressMonitor 
import jerco.gui.MainFrame 
import jerco.network.Net;

class PercolationThresholdScenario implements Runnable {
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
            if (monitor.isCanceled) {
                break
            }
            
            pCrititcal.put p, result.pCritical
            pAvailability.put p, result.pAvailability
            maxSize.put p, result.maximumSize
        }
        monitor.dispose()
        frame.onExperimentFinished(this)
    }
    
    /**
     * Возвращает количество возникновений перколяционного кластера.
     * @return
     */
    private Result experiment(p) {
        int percolationCounter = 0;
        int percolationClusterCount = 0;
        int maximumSize = 0;
        
        for (i in 1..experimentsCount) {
            net.reset()
            net.infect p

            if (net.hasPercolationCluster()) {
                percolationCounter++
                percolationClusterCount += 
                    (net.percolationClusters.reverse()[0].size() / net.size())
            }
            
            def clusters = net.clusters
            if (!clusters.isEmpty()) {
                clusters.reverse()
                maximumSize += clusters[0].size()
            }
            
            monitor.inc()
            if (monitor.isCanceled) {
                return;
            }
            
        }
        return new Result(
                pCritical: percolationCounter / experimentsCount,
                pAvailability: percolationClusterCount / experimentsCount,
                maximumSize: maximumSize / experimentsCount
        )
        
    }
}

def class Result {
    def pCritical
    def pAvailability
    def maximumSize
}