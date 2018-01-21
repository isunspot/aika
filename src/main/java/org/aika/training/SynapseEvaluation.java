package org.aika.training;

import org.aika.neuron.Activation;
import org.aika.neuron.Synapse;

public interface SynapseEvaluation {


    /**
     * Determines whether a synapse should be created between two neurons during training.
     *
     * @param s  is null if the synapse has not been created yet.
     * @param iAct
     * @param oAct
     * @return
     */
    Result evaluate(Synapse s, Activation iAct, Activation oAct);

    enum DeleteMode {
        NONE,
        DELETE,
        DELETE_IF_SIGN_CHANGES
    }

    class Result {
        public Result(Synapse.Key synapseKey, double significance, DeleteMode deleteMode) {
            this.synapseKey = synapseKey;
            this.significance = significance;
            this.deleteMode = deleteMode;
        }

        public Synapse.Key synapseKey;
        public double significance;
        public DeleteMode deleteMode;
    }
}
