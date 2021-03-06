/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package network.aika.network;

import network.aika.ActivationFunction;
import network.aika.Document;
import network.aika.Model;
import network.aika.neuron.Neuron;
import network.aika.neuron.Synapse;
import network.aika.neuron.activation.Range;
import network.aika.neuron.activation.Range.Relation;
import network.aika.neuron.INeuron;
import network.aika.neuron.activation.SearchNode;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

/**
 *
 * @author Lukas Molzberger
 */
public class InterpretationSearchTest {


    @Test
    public void testJoergZimmermann() {
        Model m = new Model();

        Neuron wJoerg = m.createNeuron("W-Joerg");
        Neuron wZimmermann = m.createNeuron("W-Zimmermann");

        Neuron eJoergForename = m.createNeuron("E-Joerg (Forename)");
        Neuron eJoergSurname = m.createNeuron("E-Joerg (Surname)");
        Neuron eZimmermannSurname = m.createNeuron("E-Zimmermann (Surname)");
        Neuron eZimmermannCompany = m.createNeuron("E-Zimmermann (Company)");

        Neuron suppr = m.createNeuron("SUPPR");

        Neuron.init(eJoergSurname, 5.0, INeuron.Type.EXCITATORY, INeuron.LogicType.CONJUNCTIVE,
                new Synapse.Builder()
                        .setSynapseId(0)
                        .setNeuron(wJoerg)
                        .setWeight(10.0)
                        .setBias(-10.0)
                        .addRangeRelation(Relation.EQUALS, 1)
                        .setRangeOutput(true),
                new Synapse.Builder()
                        .setSynapseId(1)
                        .setNeuron(suppr)
                        .setWeight(-60.0)
                        .setBias(0.0)
                        .setRecurrent(true)
                        .setRangeOutput(false)
        );
        Neuron.init(eZimmermannCompany, 5.0, INeuron.Type.EXCITATORY, INeuron.LogicType.CONJUNCTIVE,
                new Synapse.Builder()
                        .setSynapseId(0)
                        .setNeuron(wZimmermann)
                        .setWeight(10.0)
                        .setBias(-10.0)
                        .addRangeRelation(Relation.EQUALS, 1)
                        .setRangeOutput(true),
                new Synapse.Builder()
                        .setSynapseId(1)
                        .setNeuron(suppr)
                        .setWeight(-60.0)
                        .setBias(0.0)
                        .setRecurrent(true)
                        .setRangeOutput(false)
        );

        Neuron.init(eJoergForename, 6.0, INeuron.Type.EXCITATORY, INeuron.LogicType.CONJUNCTIVE,
                new Synapse.Builder()
                        .setSynapseId(0)
                        .setNeuron(wJoerg)
                        .setWeight(10.0)
                        .setBias(-10.0)
                        .addRangeRelation(Relation.END_TO_BEGIN_EQUALS, 1)
                        .setRangeOutput(true),
                new Synapse.Builder()
                        .setSynapseId(1)
                        .setNeuron(eZimmermannSurname)
                        .setWeight(10.0)
                        .setBias(-10.0)
                        .setRecurrent(true)
                        .setRangeOutput(false),
                new Synapse.Builder()
                        .setSynapseId(2)
                        .setNeuron(suppr)
                        .setWeight(-60.0)
                        .setBias(0.0)
                        .setRecurrent(true)
                        .addRangeRelation(Relation.EQUALS, 0)
                        .setRangeOutput(false)
        );

        Neuron.init(eZimmermannSurname, 6.0, INeuron.Type.EXCITATORY, INeuron.LogicType.CONJUNCTIVE,
                new Synapse.Builder()
                        .setSynapseId(0)
                        .setNeuron(wZimmermann)
                        .setWeight(10.0)
                        .setBias(-10.0)
                        .addRangeRelation(Relation.BEGIN_TO_END_EQUALS, 1)
                        .setRangeOutput(true),
                new Synapse.Builder()
                        .setSynapseId(1)
                        .setNeuron(eJoergForename)
                        .setWeight(10.0)
                        .setBias(-10.0)
                        .setRecurrent(true)
                        .setRangeOutput(false),
                new Synapse.Builder()
                        .setSynapseId(2)
                        .setNeuron(suppr)
                        .setWeight(-60.0)
                        .setBias(0.0)
                        .setRecurrent(true)
                        .addRangeRelation(Relation.EQUALS, 0)
                        .setRangeOutput(false)
        );


        Neuron.init(suppr, 0.0, INeuron.Type.INHIBITORY, INeuron.LogicType.CONJUNCTIVE,
                new Synapse.Builder()
                        .setSynapseId(0)
                        .setNeuron(eJoergForename)
                        .setWeight(10.0)
                        .setBias(0.0)
                        .setRangeOutput(true),
                new Synapse.Builder()
                        .setSynapseId(1)
                        .setNeuron(eJoergSurname)
                        .setWeight(10.0)
                        .setBias(0.0)
                        .setRangeOutput(true),
                new Synapse.Builder()
                        .setSynapseId(2)
                        .setNeuron(eZimmermannCompany)
                        .setWeight(10.0)
                        .setBias(0.0)
                        .setRangeOutput(true),
                new Synapse.Builder()
                        .setSynapseId(3)
                        .setNeuron(eZimmermannSurname)
                        .setWeight(10.0)
                        .setBias(0.0)
                        .setRangeOutput(true)
        );

        Document doc = m.createDocument("Joerg Zimmermann");

        wJoerg.addInput(doc, 0, 6);
        wZimmermann.addInput(doc, 6, 16);

        doc.process();

        System.out.println(doc.activationsToString());

        Assert.assertTrue(eZimmermannCompany.getActivations(doc, true).isEmpty());
        Assert.assertFalse(eZimmermannSurname.getActivations(doc, true).isEmpty());

        doc.clearActivations();

        doc = m.createDocument("Joerg Zimmermann Joerg Zimmermann");

        wJoerg.addInput(doc, 0, 6);
        wZimmermann.addInput(doc, 6, 17);
        wJoerg.addInput(doc, 17, 23);
        wZimmermann.addInput(doc, 23, 33);

        doc.process();

        System.out.println(doc.activationsToString());

        Assert.assertEquals(0, eZimmermannCompany.getActivations(doc, true).size());
        Assert.assertEquals(2, eZimmermannSurname.getActivations(doc, true).size());

        doc.clearActivations();
    }


    @Test
    public void testBackwardReferencingSynapses() {
        Model m = new Model();

        Neuron inA = m.createNeuron("IN A");
        Neuron inB = m.createNeuron("IN B");

        Neuron inhib = m.createNeuron("INHIB");

        Neuron nF = m.createNeuron("F");

        Neuron nC = Neuron.init(m.createNeuron("C"), 6.0, INeuron.Type.EXCITATORY, INeuron.LogicType.CONJUNCTIVE,
                new Synapse.Builder()
                        .setSynapseId(0)
                        .setNeuron(inA)
                        .setWeight(10.0)
                        .setBias(-10.0)
                        .setRangeOutput(true),
                new Synapse.Builder()
                        .setSynapseId(1)
                        .setNeuron(inhib)
                        .setWeight(-100.0)
                        .setBias(0.0)
                        .setRecurrent(true)
                        .addRangeRelation(Relation.EQUALS, 0)
        );

        Neuron nD = Neuron.init(m.createNeuron("D"), 5.0, INeuron.Type.EXCITATORY, INeuron.LogicType.CONJUNCTIVE,
                new Synapse.Builder()
                        .setSynapseId(0)
                        .setNeuron(inA)
                        .setWeight(10.0)
                        .setBias(-10.0)
                        .setRangeOutput(true),
                new Synapse.Builder()
                        .setSynapseId(1)
                        .setNeuron(nF)
                        .setWeight(2.0)
                        .setBias(0.0)
                        .addRangeRelation(Relation.NONE, 0),
                new Synapse.Builder()
                        .setSynapseId(2)
                        .setNeuron(inhib)
                        .setWeight(-100.0)
                        .setBias(0.0)
                        .setRecurrent(true)
                        .addRangeRelation(Relation.EQUALS, 0)
        );

        Neuron nE = Neuron.init(m.createNeuron("E"), 5.0, INeuron.Type.EXCITATORY, INeuron.LogicType.CONJUNCTIVE,
                new Synapse.Builder()
                        .setSynapseId(0)
                        .setNeuron(inB)
                        .setWeight(10.0)
                        .setBias(-10.0)
                        .setRangeOutput(true),
                new Synapse.Builder()
                        .setSynapseId(1)
                        .setNeuron(inhib)
                        .setWeight(-100.0)
                        .setBias(0.0)
                        .setRecurrent(true)
                        .addRangeRelation(Relation.EQUALS, 0)
        );

        Neuron.init(nF, 6.0, INeuron.Type.EXCITATORY, INeuron.LogicType.CONJUNCTIVE,
                new Synapse.Builder()
                        .setSynapseId(0)
                        .setNeuron(inB)
                        .setWeight(10.0)
                        .setBias(-10.0)
                        .setRangeOutput(true),
                new Synapse.Builder()
                        .setSynapseId(1)
                        .setNeuron(inhib)
                        .setWeight(-100.0)
                        .setBias(0.0)
                        .setRecurrent(true)
                        .addRangeRelation(Relation.EQUALS, 0)
        );


        Neuron.init(inhib, 0.0, INeuron.Type.INHIBITORY, INeuron.LogicType.DISJUNCTIVE,
                new Synapse.Builder()
                        .setSynapseId(0)
                        .setNeuron(nC)
                        .setWeight(10.0)
                        .setBias(0.0)
                        .setRangeOutput(true),
                new Synapse.Builder()
                        .setSynapseId(1)
                        .setNeuron(nD)
                        .setWeight(10.0)
                        .setBias(0.0)
                        .setRangeOutput(true),
                new Synapse.Builder()
                        .setSynapseId(2)
                        .setNeuron(nE)
                        .setWeight(10.0)
                        .setBias(0.0)
                        .setRangeOutput(true),
                new Synapse.Builder()
                        .setSynapseId(3)
                        .setNeuron(nF)
                        .setWeight(10.0)
                        .setBias(0.0)
                        .setRangeOutput(true)
        );


        Document doc = m.createDocument("aaaa bbbb ");

        inA.addInput(doc, 0, 5);
        inB.addInput(doc, 5, 10);

        doc.process();

        System.out.println(doc.activationsToString(true, true, true));

        Assert.assertFalse(nD.getActivations(doc, true).isEmpty());
    }


    @Ignore
    @Test
    public void testReversePositiveFeedbackSynapse() {
        Model m = new Model();

        Neuron inA = m.createNeuron("IN A");
        Neuron inB = m.createNeuron("IN B");

        Neuron inhib = m.createNeuron("INHIB");
        Neuron.init(inhib, 0.0, INeuron.Type.INHIBITORY, INeuron.LogicType.DISJUNCTIVE);


        Neuron nE = m.createNeuron("E");
        Neuron nF = m.createNeuron("F");


        Neuron nC = Neuron.init(m.createNeuron("C"), 6.0, INeuron.Type.EXCITATORY, INeuron.LogicType.CONJUNCTIVE,
                new Synapse.Builder()
                        .setSynapseId(0)
                        .setNeuron(inA)
                        .setWeight(10.0)
                        .setBias(-10.0)
                        .setRangeOutput(true),
                new Synapse.Builder()
                        .setSynapseId(1)
                        .setNeuron(inhib)
                        .setWeight(-100.0)
                        .setBias(0.0)
                        .setRecurrent(true)
                        .addRangeRelation(Relation.EQUALS, 0)
        );

        Neuron nD = Neuron.init(m.createNeuron("D"), 5.0, INeuron.Type.EXCITATORY, INeuron.LogicType.CONJUNCTIVE,
                new Synapse.Builder()
                        .setSynapseId(0)
                        .setNeuron(inA)
                        .setWeight(10.0)
                        .setBias(-10.0)
                        .setRangeOutput(true),
                new Synapse.Builder()
                        .setSynapseId(1)
                        .setNeuron(inhib)
                        .setWeight(-100.0)
                        .setBias(0.0)
                        .setRecurrent(true)
                        .addRangeRelation(Relation.EQUALS, 0)
        );

        inhib.addSynapse(
                new Synapse.Builder()
                        .setSynapseId(0)
                        .setNeuron(nC)
                        .setWeight(10.0)
                        .setBias(0.0)
                        .setRangeOutput(true)
        );

        inhib.addSynapse(
                new Synapse.Builder()
                        .setSynapseId(1)
                        .setNeuron(nD)
                        .setWeight(10.0)
                        .setBias(0.0)
                        .setRangeOutput(true)
        );



        // Create the document even though the model is not yet complete

        Document doc = m.createDocument("aaaa bbbb ");

//        inA.addInput(doc, 0, 5);
//        inB.addInput(doc, 5, 10);

//        doc.process();

        System.out.println(doc.activationsToString(true, true, true));


        // Complete the model

        nD.addSynapse(doc,
                new Synapse.Builder()
                        .setSynapseId(2)
                        .setNeuron(nF)
                        .setWeight(2.0)
                        .setBias(0.0)
                        .addRangeRelation(Range.Relation.NONE, 0)
        );

        Neuron.init(doc, nE, 5.0, INeuron.Type.EXCITATORY, INeuron.LogicType.CONJUNCTIVE,
                new Synapse.Builder()
                        .setSynapseId(0)
                        .setNeuron(inB)
                        .setWeight(10.0)
                        .setBias(-10.0)
                        .setRangeOutput(true),
                new Synapse.Builder()
                        .setSynapseId(1)
                        .setNeuron(inhib)
                        .setWeight(-100.0)
                        .setBias(0.0)
                        .setRecurrent(true)
                        .addRangeRelation(Relation.EQUALS, 0)
        );

        Neuron.init(doc, nF, 6.0, INeuron.Type.EXCITATORY, INeuron.LogicType.CONJUNCTIVE,
                new Synapse.Builder()
                        .setSynapseId(0)
                        .setNeuron(inB)
                        .setWeight(10.0)
                        .setBias(-10.0)
                        .setRangeOutput(true),
                new Synapse.Builder()
                        .setSynapseId(1)
                        .setNeuron(inhib)
                        .setWeight(-100.0)
                        .setBias(0.0)
                        .setRecurrent(true)
                        .addRangeRelation(Relation.EQUALS, 0)
        );

        inhib.addSynapse(
                doc,
                new Synapse.Builder()
                        .setSynapseId(2)
                        .setNeuron(nE)
                        .setWeight(10.0)
                        .setBias(0.0)
                        .setRangeOutput(true)
        );

        inhib.addSynapse(
                doc,
                new Synapse.Builder()
                        .setSynapseId(3)
                        .setNeuron(nF)
                        .setWeight(10.0)
                        .setBias(0.0)
                        .setRangeOutput(true)
        );

        inA.addInput(doc, 0, 5);
        inB.addInput(doc, 5, 10);

        doc.propagate();
        doc.process();

        System.out.println(doc.activationsToString(true, true, true));

        Assert.assertFalse(nD.getActivations(doc, true).isEmpty());
    }


    @Test
    public void testAvoidUnnecessaryExcludeSteps() {
        Model m = new Model();

        Neuron in = m.createNeuron("IN");

        Neuron inhib = m.createNeuron("INHIB");
        Neuron out = m.createNeuron("OUT");


        Neuron.init(inhib, 0.0, ActivationFunction.LIMITED_RECTIFIED_LINEAR_UNIT, INeuron.Type.INHIBITORY, INeuron.LogicType.DISJUNCTIVE,
                new Synapse.Builder()
                        .setSynapseId(0)
                        .setNeuron(out)
                        .setWeight(1.0)
                        .setBias(0.0)
                        .setRangeOutput(true)
        );

        Neuron.init(out, 5.0, ActivationFunction.RECTIFIED_HYPERBOLIC_TANGENT, INeuron.Type.EXCITATORY, INeuron.LogicType.CONJUNCTIVE,
                new Synapse.Builder()
                        .setSynapseId(0)
                        .setNeuron(in)
                        .setWeight(20.0)
                        .setBias(-20.0)
                        .setRangeOutput(true, false),
                new Synapse.Builder()
                        .setSynapseId(1)
                        .setNeuron(in)
                        .setWeight(20.0)
                        .setBias(-20.0)
                        .setRangeOutput(false, true)
                        .addRangeRelation(Relation.BEGIN_TO_END_EQUALS, 0),
                new Synapse.Builder()
                        .setSynapseId(2)
                        .setNeuron(out)
                        .setWeight(-100.0)
                        .setBias(0.0)
                        .setRecurrent(true)
                        .addRangeRelation(Relation.OVERLAPS, Synapse.Builder.OUTPUT)
                        .setRangeOutput(false)
        );


        Document doc = m.createDocument("aaaa");
        in.addInput(doc, 0, 1);
        in.addInput(doc, 1, 2);
        in.addInput(doc, 2, 3);
        in.addInput(doc, 3, 4);


        SearchNode.COMPUTE_SOFT_MAX = true;
        SearchNode.OPTIMIZE_SEARCH = false;

        doc.process();

        System.out.println(doc.activationsToString(true, true, true));

        Assert.assertEquals(13, doc.searchNodeIdCounter);
    }
}
