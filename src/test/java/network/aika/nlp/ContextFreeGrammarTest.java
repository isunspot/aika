package network.aika.nlp;



//http://ccl.pku.edu.cn/doubtfire/NLP/Parsing/Introduction/Grammars%20and%20Parsing.htm


import network.aika.ActivationFunction;
import network.aika.Document;
import network.aika.Model;
import network.aika.neuron.INeuron;
import network.aika.neuron.Neuron;
import network.aika.neuron.Synapse;
import network.aika.neuron.activation.Range;
import network.aika.neuron.activation.SearchNode;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;

import static network.aika.neuron.Synapse.Builder.OUTPUT;
import static network.aika.neuron.activation.Range.Relation.BEGIN_TO_END_EQUALS;
import static network.aika.neuron.activation.Range.Relation.OVERLAPS;

public class ContextFreeGrammarTest {


    Model m;

    Neuron I;

    Neuron S;
    Neuron NP;
    Neuron VP;

    Neuron NP_ART_ADJ_N;
    Neuron NP_ART_N;
    Neuron NP_ADJ_N;

    Neuron VP_AUX_V_NP;
    Neuron VP_V_NP;

    Neuron ART;
    Neuron N;
    Neuron ADJ;
    Neuron V;
    Neuron AUX;

    Map<String, Neuron> dictionary = new TreeMap<>();


    @Before
    public void init() {
        m = new Model();

        I = m.createNeuron("Inhibitory");

        S = m.createNeuron("Sentence");
        NP = m.createNeuron("Noun Phrase");
        VP = m.createNeuron("Verb Phrase");

        NP_ART_ADJ_N = m.createNeuron("NP <- ART ADJ N");
        NP_ART_N = m.createNeuron("NP <- ART N");
        NP_ADJ_N = m.createNeuron("NP <- ADJ N");

        VP_AUX_V_NP = m.createNeuron("VP <- AUX V NP");
        VP_V_NP = m.createNeuron("VP <- V NP");

        ART = m.createNeuron("Article");
        N = m.createNeuron("Noun");
        ADJ = m.createNeuron("Adjective");
        V = m.createNeuron("Verb");
        AUX = m.createNeuron("Auxiliary");


        for(Neuron n: new Neuron[] {I, NP, VP}) {
            Neuron.init(n, 0.0, ActivationFunction.LIMITED_RECTIFIED_LINEAR_UNIT, INeuron.Type.INHIBITORY, INeuron.LogicType.DISJUNCTIVE);
        }

        for(Neuron n: new Neuron[] {ART, N, ADJ, V, AUX}) {
            Neuron.init(n, 0.0, ActivationFunction.RECTIFIED_HYPERBOLIC_TANGENT, INeuron.Type.EXCITATORY, INeuron.LogicType.DISJUNCTIVE,
                    new Synapse.Builder()
                            .setNeuron(I)
                            .setWeight(-50.0)
                            .setBias(0.0)
                            .setRecurrent(true)
                            .addRangeRelation(OVERLAPS, OUTPUT)
            );
        }


        initOrNeuron(I, NP, VP, ART, N, ADJ, V, AUX);
        initOrNeuron(NP, NP_ART_ADJ_N, NP_ART_N, NP_ADJ_N);
        initOrNeuron(VP, VP_AUX_V_NP, VP_V_NP);


        initAndNeuron(S, 6.0, NP, VP);

        initAndNeuron(NP_ART_ADJ_N, 9.0, ART, ADJ, N);
        initAndNeuron(NP_ART_N, 6.0, ART, N);
        initAndNeuron(NP_ADJ_N, 6.0, ADJ, N);

        initAndNeuron(VP_AUX_V_NP, 9.0, AUX, V, NP);
        initAndNeuron(VP_V_NP, 6.0, V, NP);


        initWord("the", ART);
        initWord("large", ADJ);
        initWord("can", AUX, N, V);
        initWord("hold", N, V);
        initWord("water", N, V);

    }


    private void initWord(String word, Neuron... wordTypes) {
        Neuron wordN = m.createNeuron("W-" + word);

        dictionary.put(word, wordN);

        for (Neuron wordType : wordTypes) {
            wordType.addSynapse(
                    new Synapse.Builder()
                            .setNeuron(wordN)
                            .setWeight(3.0)
                            .setBias(0.0)
                            .setRangeOutput(true)
            );
        }
    }


    private void initOrNeuron(Neuron orN, Neuron... inputs) {
        for (Neuron n : inputs) {
            orN.addSynapse(
                    new Synapse.Builder()
                            .setNeuron(n)
                            .setWeight(1.0)
                            .setRangeOutput(true)
            );
        }
    }


    private void initAndNeuron(Neuron andN, double weight, Neuron... inputs) {
        ArrayList<Synapse.Builder> synapses = new ArrayList<>();

        for(int i = 0; i < inputs.length; i++) {
            boolean begin = i == 0;
            boolean end = i + 1 == inputs.length;

            Synapse.Builder s = new Synapse.Builder()
                        .setSynapseId(i)
                        .setNeuron(inputs[i])
                        .setWeight(10.0)
                        .setBias(-10.0)
                        .setIdentity(true)
                        .setRangeOutput(begin, end);

            if(!begin) {
                s = s.addRangeRelation(BEGIN_TO_END_EQUALS, i - 1);
            }
            synapses.add(s);
        }

        synapses.add(
                new Synapse.Builder()
                    .setNeuron(I)
                    .setWeight(-100.0)
                    .setBias(0.0)
                    .setRecurrent(true)
                    .addRangeRelation(OVERLAPS, OUTPUT)
        );

        Neuron.init(andN, weight, ActivationFunction.RECTIFIED_HYPERBOLIC_TANGENT, INeuron.Type.EXCITATORY, INeuron.LogicType.CONJUNCTIVE, synapses);
    }


    public Document parse(String txt) {
        Document doc = m.createDocument(txt);

        int i = 0;
        for(String word: txt.split(" ")) {
            int j = i + word.length();
            Neuron wn = dictionary.get(word);
            if(wn != null) {
                wn.addInput(doc, i, j + 1);
            }

            i = j + 1;
        }

        doc.process();

        return doc;
    }


    @Test
    public void parseSentence() {
        Document doc = parse("the large can can hold the water ");

        System.out.println(doc.activationsToString(true, true, true));

        Assert.assertNotNull(S.getActivation(doc, new Range(0, 33), true));
    }
}