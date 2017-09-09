package org.aika.network;


import org.aika.Input;
import org.aika.Model;
import org.aika.Provider;
import org.aika.corpus.Document;
import org.aika.corpus.Range.Operator;
import org.aika.corpus.Range.Mapping;
import org.aika.neuron.INeuron;
import org.junit.Test;

public class TestNextWord {

    @Test
    public void testMatchTheWord() {
        Model m = new Model(null, 1);

        Provider<INeuron> inA = m.createNeuron("A");
        Provider<INeuron> inB = m.createNeuron("B");

        Provider<INeuron> abN = m.initAndNeuron(m.createNeuron("AB"), 0.5,
                new Input()
                        .setNeuron(inB)
                        .setWeight(10.0f)
                        .setMinInput(0.95f)
                        .setEndRangeMatch(Operator.EQUALS)
                        .setEndRangeOutput(true),
                new Input()
                        .setNeuron(inA)
                        .setWeight(10.0f)
                        .setMinInput(0.95f)
                        .setStartRangeMapping(Mapping.END)
                        .setStartRangeMatch(Operator.EQUALS)
                        .setStartRangeOutput(true)
        );

        Document doc = m.createDocument("aaaa bbbb  ", 0);

        Document.APPLY_DEBUG_OUTPUT = true;
        inA.get().addInput(doc, 0, 5);
        inB.get().addInput(doc, 5, 10);

        System.out.println(doc.neuronActivationsToString(true, false, true));
    }
}
