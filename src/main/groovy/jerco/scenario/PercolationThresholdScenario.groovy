package jerco.scenario;

import java.math.BigDecimal;
import java.util.HashMap;

import jerco.network.Net;

class PercolationThresholdScenario implements Runnable {
    def Net net;
    
    def pMin = 0.0;
    def pMax = 1.0;    
    def pStep = 0.001;
    
    def experimentsCount = 1000;
    
    def Map<BigDecimal,BigDecimal> pCrititcal;
    def Map<BigDecimal,BigDecimal> pAvailability;
    
    def void run() {
        pCrititcal = new HashMap<BigDecimal, BigDecimal>();
        pAvailability = new HashMap<BigDecimal, BigDecimal>();
        
        def p = pMin;
        
        while (p < 1) {
            p += pStep
            Result result = experiment(p)
            pCrititcal.put p, result.pCritical
            pAvailability.put p, result.pAvailability
        }
    }
    
    /**
     * Возвращает количество возникновений перколяционного кластера.
     * @return
     */
    private Result experiment(p) {
        int percolationCounter = 0;
        int percolationClusterSize = 0;
        
        for (i in 1..experimentsCount) {
            net.reset()
            net.infect p
            if (net.hasPercolationCluster()) {
                percolationCounter++
                percolationClusterSize += 
                    (net.percolationClusters[0].size() / net.size())
            }
        }
        return new Result(
                pCritical: percolationCounter / experimentsCount,
                pAvailability: percolationClusterSize / experimentsCount
        )
        
    }
}

def class Result {
    def pCritical
    def pAvailability
}