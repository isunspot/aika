package network.aika.network;

import network.aika.Document;
import network.aika.Model;
import network.aika.neuron.INeuron;
import network.aika.neuron.Neuron;
import network.aika.neuron.Synapse;
import network.aika.neuron.activation.Activation;
import network.aika.neuron.activation.Range;
import org.junit.Assert;
import org.junit.Test;

public class PassiveInputNeuronTest {



    @Test
    public void testPassiveInputNeuron() {
        Model m = new Model();
        Document doc = m.createDocument("aaaaaaaaaa", 0);

        Neuron inA = m.createNeuron("A");

        Neuron inB = m.createNeuron("B");
        Neuron.registerPassiveInputNeuron(inB, (s, oAct) -> 1.0);

        Neuron out = Neuron.init(m.createNeuron("OUT"), 5.0, INeuron.Type.EXCITATORY, INeuron.LogicType.CONJUNCTIVE,
                new Synapse.Builder()
                        .setSynapseId(0)
                        .setNeuron(inA)
                        .setWeight(10.0)
                        .setBias(-10.0)
                        .setRangeOutput(true),
                new Synapse.Builder()
                        .setSynapseId(1)
                        .setNeuron(inB)
                        .setWeight(10.0)
                        .setBias(-10.0)
                        .addRangeRelation(Range.Relation.EQUALS, 0)
        );


        inA.addInput(doc,
                new Activation.Builder()
                        .setRange(0, 1)
        );

        Activation outAct = out.getActivation(doc, new Range(0, 1), false);

        System.out.println(doc.activationsToString(false, true, true));

        doc.process();

        Assert.assertTrue(outAct.isFinalActivation());
    }
}
