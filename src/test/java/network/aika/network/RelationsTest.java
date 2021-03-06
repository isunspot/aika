package network.aika.network;


import network.aika.Document;
import network.aika.Model;
import network.aika.neuron.Neuron;
import network.aika.neuron.Synapse;
import network.aika.neuron.INeuron;
import network.aika.neuron.activation.Activation;
import network.aika.neuron.activation.Range;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static network.aika.neuron.relation.InstanceRelation.Type.COMMON_ANCESTOR;
import static network.aika.neuron.relation.InstanceRelation.Type.CONTAINED_IN;
import static network.aika.neuron.activation.Range.Operator.*;


public class RelationsTest {


    @Test
    public void testRangeRelation() {
        Model m = new Model();

        Neuron inA = m.createNeuron("A");
        Neuron inB = m.createNeuron("B");


        INeuron outC = Neuron.init(m.createNeuron("C"),
                5.0,
                INeuron.Type.EXCITATORY,
                INeuron.LogicType.CONJUNCTIVE,
                new Synapse.Builder()
                        .setSynapseId(0)
                        .setNeuron(inA)
                        .setWeight(10.0)
                        .setRecurrent(false)
                        .setBias(-10.0)
                        .addRangeRelation(Range.Relation.EQUALS, 1)
                        .setRangeOutput(true),
                new Synapse.Builder()
                        .setSynapseId(1)
                        .setNeuron(inB)
                        .setWeight(10.0)
                        .setRecurrent(false)
                        .setBias(-10.0)
                        .setRangeOutput(true)
        ).get();


        Document doc = m.createDocument("aaaaaaaaaa", 0);

        inA.addInput(doc, 0, 1);
        inB.addInput(doc, 0, 1);

        Activation outC1 = outC.getActivation(doc, new Range(0, 1), false);

        System.out.println(doc.activationsToString(false, false, true));

        Assert.assertNotNull(outC1);
    }


    @Test
    public void testInstanceRelation() {
        Model m = new Model();

        Neuron inA = m.createNeuron("A");

        INeuron inB = Neuron.init(m.createNeuron("B"),
                5.0,
                INeuron.Type.EXCITATORY,
                INeuron.LogicType.CONJUNCTIVE,
                new Synapse.Builder()
                        .setSynapseId(0)
                        .setNeuron(inA)
                        .setWeight(10.0)
                        .setRecurrent(false)
                        .setIdentity(true)
                        .setBias(-10.0)
                        .setRangeOutput(true)
        ).get();

        INeuron outC = Neuron.init(m.createNeuron("C"),
                5.0,
                INeuron.Type.EXCITATORY,
                INeuron.LogicType.CONJUNCTIVE,
                new Synapse.Builder()
                        .setSynapseId(0)
                        .setNeuron(inA)
                        .setWeight(10.0)
                        .setRecurrent(false)
                        .setBias(-10.0)
                        .addInstanceRelation(CONTAINED_IN, 1)
                        .setRangeOutput(true),
                new Synapse.Builder()
                        .setSynapseId(1)
                        .setNeuron(inB.provider)
                        .setWeight(10.0)
                        .setRecurrent(false)
                        .setBias(-10.0)
                        .setRangeOutput(true)
        ).get();


        Document doc = m.createDocument("aaaaaaaaaa", 0);

        inA.addInput(doc, 0, 1);

        Activation outC1 = outC.getActivation(doc, new Range(0, 1), false);

        System.out.println(doc.activationsToString(false, false, true));

        Assert.assertNotNull(outC1);
    }


    @Test
    public void testABCPattern() {
        Model m = new Model();

        Neuron inA = m.createNeuron("A");
        Neuron inB = m.createNeuron("B");
        Neuron inC = m.createNeuron("C");

        INeuron outD = Neuron.init(m.createNeuron("D"),
                0.001,
                INeuron.Type.EXCITATORY,
                INeuron.LogicType.CONJUNCTIVE,
                new Synapse.Builder()
                        .setSynapseId(0)
                        .setNeuron(inA)
                        .setWeight(1.0)
                        .setRecurrent(false)
                        .setBias(-1.0)
                        .addRangeRelation(Range.Relation.END_TO_BEGIN_EQUALS, 1)
                        .setRangeOutput(true, false),
                new Synapse.Builder()
                        .setSynapseId(1)
                        .setNeuron(inB)
                        .setWeight(1.0)
                        .setRecurrent(false)
                        .setBias(-1.0)
                        .addRangeRelation(Range.Relation.END_TO_BEGIN_EQUALS, 2)
                        .setRangeOutput(false),
                new Synapse.Builder()
                        .setSynapseId(2)
                        .setNeuron(inC)
                        .setWeight(1.0)
                        .setRecurrent(false)
                        .setBias(-1.0)
                        .setRangeOutput(false, true)
        ).get();

        Document doc = m.createDocument("aaaaaaaaaa", 0);

        inA.addInput(doc, 0, 1);
        inB.addInput(doc, 1, 2);
        inC.addInput(doc, 2, 3);

        Activation outD1 = outD.getActivation(doc, new Range(0, 3), false);

        Assert.assertNotNull(outD1);
    }


    @Test
    public void testHuettenheim() {
        Model m = new Model();

        HashMap<Character, Neuron> chars = new HashMap<>();
        for (char c = 'a'; c <= 'z'; c++) {
            Neuron rec = m.createNeuron("IN-" + c);
            chars.put(c, rec);
        }

        String word = "Huettenheim";


        List<Synapse.Builder> inputs = new ArrayList<>();
        for (int i = 0; i < word.length(); i++) {
            char c = word.toLowerCase().charAt(i);

            Neuron rec = chars.get(c);
            if (rec != null) {
                boolean begin = i == 0;
                boolean end = i + 1 == word.length();

                Synapse.Builder s = new Synapse.Builder()
                        .setSynapseId(i)
                        .setNeuron(rec)
                        .setWeight(begin || end ? 2.0 : 1.0)
                        .setRecurrent(false)
                        .setBias(begin || end ? -2.0 : -1.0)
                        .setRangeOutput(begin, end);

                if(!end) {
                    s = s.addRangeRelation(Range.Relation.END_TO_BEGIN_EQUALS, i + 1);
                } else {
                    s = s.addRangeRelation(Range.Relation.create(NONE, NONE, NONE, GREATER_THAN), 0);
                }

                inputs.add(s);
            }
        }

        Neuron n = Neuron.init(m.createNeuron("PATTERN"), 0.5, INeuron.Type.EXCITATORY, INeuron.LogicType.CONJUNCTIVE, inputs);

        System.out.println(n.get().node.get().logicToString());

        Document doc = m.createDocument("abc Huettenheim cba", 0);

        for (int i = 0; i < doc.length(); i++) {
            char c = doc.getContent().toLowerCase().charAt(i);

            Neuron inputNeuron = chars.get(c);
            if (inputNeuron != null) {
                inputNeuron.addInput(doc, new Range(i, i + 1));
            }
        }

        System.out.println(doc.activationsToString(false, true, true));

        assert n.get().getActivations(doc, false).size() >= 1;
    }



    @Test
    public void testInstanceRelation1() {
        Model m = new Model();
        Document doc = m.createDocument("aaaaaaaaaa", 0);

        Neuron inA = m.createNeuron("A");

        Neuron nB = Neuron.init(m.createNeuron("B"), 5.0, INeuron.Type.EXCITATORY, INeuron.LogicType.CONJUNCTIVE,
                new Synapse.Builder()
                        .setSynapseId(0)
                        .setNeuron(inA)
                        .setWeight(10.0)
                        .setBias(-10.0)
                        .setRecurrent(false)
                        .setIdentity(true)
                        .setRangeOutput(true)
        );

        Neuron nC = Neuron.init(m.createNeuron("C"), 5.0, INeuron.Type.EXCITATORY, INeuron.LogicType.CONJUNCTIVE,
                new Synapse.Builder()
                        .setSynapseId(0)
                        .setNeuron(inA)
                        .setWeight(10.0)
                        .setBias(-10.0)
                        .setRecurrent(false)
                        .setIdentity(true)
                        .setRangeOutput(true)
        );

        Neuron nD = Neuron.init(m.createNeuron("D"), 5.0, INeuron.Type.EXCITATORY, INeuron.LogicType.CONJUNCTIVE,
                new Synapse.Builder()
                        .setSynapseId(0)
                        .setNeuron(nB)
                        .setWeight(10.0)
                        .setBias(-10.0)
                        .setRecurrent(false)
                        .setRangeOutput(true)
                        .setSynapseId(0)
                        .addInstanceRelation(COMMON_ANCESTOR, 1),
                new Synapse.Builder()
                        .setSynapseId(1)
                        .setNeuron(nC)
                        .setWeight(10.0)
                        .setBias(-10.0)
                        .setRecurrent(false)
                        .setRangeOutput(false)
                        .setSynapseId(1)
                        .addInstanceRelation(COMMON_ANCESTOR, 0)
        );


        inA.addInput(doc, 0, 1);

        doc.process();

        Assert.assertFalse(nD.getActivations(doc, true).isEmpty());
    }


    @Test
    public void testInstanceRelation2() {
        Model m = new Model();
        Document doc = m.createDocument("aaaaaaaaaa", 0);

        Neuron inA = m.createNeuron("A");

        Neuron inB = m.createNeuron("B");


        Neuron nC = Neuron.init(m.createNeuron("C"), 5.0, INeuron.Type.EXCITATORY, INeuron.LogicType.CONJUNCTIVE,
                new Synapse.Builder()
                        .setSynapseId(0)
                        .setNeuron(inA)
                        .setWeight(10.0)
                        .setBias(-10.0)
                        .setRecurrent(false)
                        .setRangeOutput(true)
                        .setSynapseId(0)
                        .addInstanceRelation(COMMON_ANCESTOR, 1),
                new Synapse.Builder()
                        .setSynapseId(1)
                        .setNeuron(inB)
                        .setWeight(10.0)
                        .setBias(-10.0)
                        .setRecurrent(false)
                        .setRangeOutput(false)
                        .setSynapseId(1)
                        .addInstanceRelation(COMMON_ANCESTOR, 0)
        );


        inA.addInput(doc, 0, 1);
        inB.addInput(doc, 1, 2);

        doc.process();

        Assert.assertTrue(nC.getActivations(doc, true).isEmpty());

    }
}
