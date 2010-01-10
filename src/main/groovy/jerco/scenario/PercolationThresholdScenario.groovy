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
    
    def Map<BigDecimal,BigDecimal> result;
    
    def void run() {
        result = new HashMap<BigDecimal, BigDecimal>();
        def p = pMin;
        
        while (p < 1) {
            print p + ": "
            p += pStep
            result.put p, experiment(p)
        }
    }
    
    /**
     * Возвращает количество возникновений перколяционного кластера.
     * @return
     */
    private BigDecimal experiment(p) {
        int percolationCounter = 0;
        for (i in 1..experimentsCount) {
            print i
            net.reset()
            net.infect p
            if (net.hasPercolationCluster()) {
                percolationCounter++
            }
        }
        println()
        println(percolationCounter / experimentsCount)
        return percolationCounter / experimentsCount
    }
}
