package network.aika.network;


import network.aika.Document;
import network.aika.Model;
import network.aika.neuron.Neuron;
import network.aika.neuron.Synapse;
import network.aika.neuron.activation.Range;
import network.aika.neuron.activation.Range.Mapping;
import network.aika.neuron.INeuron;
import org.junit.Test;

public class TestNextWord {

    @Test
    public void testMatchTheWord() {
        Model m = new Model(null, 1);

        Neuron inA = m.createNeuron("A");
        Neuron inB = m.createNeuron("B");

        Neuron abN = Neuron.init(m.createNeuron("AB"), 5.0, INeuron.Type.EXCITATORY,
                new Synapse.Builder()
                        .setNeuron(inB)
                        .setWeight(10.0)
                        .setBias(-9.5)
                        .setRangeMatch(Range.Relation.END_EQUALS)
                        .setRangeOutput(false, true),
                new Synapse.Builder()
                        .setNeuron(inA)
                        .setWeight(10.0)
                        .setBias(-9.5)
                        .setRangeMatch(Range.Relation.BEGIN_EQUALS)
                        .setRangeOutput(Mapping.END, Mapping.NONE)
        );

        Document doc = m.createDocument("aaaa bbbb  ", 0);

        inA.addInput(doc, 0, 5);
        inB.addInput(doc, 5, 10);

        System.out.println(doc.activationsToString(false, false, true));
    }
}