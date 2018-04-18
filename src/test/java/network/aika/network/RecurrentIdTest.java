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


import network.aika.Document;
import network.aika.Model;
import network.aika.neuron.INeuron;
import network.aika.neuron.Neuron;
import network.aika.neuron.Synapse;
import network.aika.neuron.activation.Activation;
import network.aika.neuron.*;
import network.aika.neuron.activation.Range.Relation;
import network.aika.neuron.activation.Range;
import network.aika.neuron.activation.Selector;
import org.junit.Assert;
import org.junit.Test;

import java.util.*;

import static network.aika.neuron.activation.Range.Operator.*;


/**
 *
 * @author Lukas Molzberger
 */
public class RecurrentIdTest {

    @Test
    public void testABPattern() {
        Model m = new Model();

        Neuron inA = m.createNeuron("A");
        Neuron inB = m.createNeuron("B");


        INeuron outC = Neuron.init(m.createNeuron("C"),
                0.001,
                INeuron.Type.EXCITATORY,
                new Synapse.Builder()
                        .setNeuron(inA)
                        .setWeight(1.0)
                        .setRecurrent(false)
                        .setBias(-1.0)
                        .setRelativeRid(0)
                        .setRangeMatch(Relation.EQUALS)
                        .setRangeOutput(true),
                new Synapse.Builder()
                        .setNeuron(inB)
                        .setWeight(1.0)
                        .setRecurrent(false)
                        .setBias(-1.0)
                        .setRelativeRid(1)
                        .setRangeMatch(Relation.EQUALS)
                        .setRangeOutput(true)
        ).get();


        Document doc = m.createDocument("aaaaaaaaaa", 0);

        inA.addInput(doc, 0, 1, 20);
        inB.addInput(doc, 0, 1, 21);

        Activation outC1 = Selector.get(doc, outC, 20, new Range(0, 1), Relation.CONTAINS);

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
                new Synapse.Builder()
                        .setNeuron(inA)
                        .setWeight(1.0)
                        .setRecurrent(false)
                        .setBias(-1.0)
                        .setRelativeRid(3)
                        .setRangeMatch(Relation.EQUALS)
                        .setRangeOutput(true),
                new Synapse.Builder()
                        .setNeuron(inB)
                        .setWeight(1.0)
                        .setRecurrent(false)
                        .setBias(-1.0)
                        .setRelativeRid(0)
                        .setRangeMatch(Relation.EQUALS)
                        .setRangeOutput(true),
                new Synapse.Builder()
                        .setNeuron(inC)
                        .setWeight(1.0)
                        .setRecurrent(false)
                        .setBias(-1.0)
                        .setRelativeRid(6)
                        .setRangeMatch(Relation.EQUALS)
                        .setRangeOutput(true)
        ).get();

        Document doc = m.createDocument("aaaaaaaaaa", 0);

        inA.addInput(doc, 0, 1, 13);
        inB.addInput(doc, 0, 1, 10);
        inC.addInput(doc, 0, 1, 16);

        Activation outD1 = Selector.get(doc, outD, 10, new Range(0, 1), Relation.EQUALS);

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

        String[] testWords = {"Hüttenheim"};

        for (String word : testWords) {
            List<Synapse.Builder> inputs = new ArrayList<>();
            for (int i = 0; i < word.length(); i++) {
                char c = word.toLowerCase().charAt(i);

                Neuron rec = chars.get(c);
                if (rec != null) {
                    boolean begin = i == 0;
                    boolean end = i + 1 == word.length();
                    inputs.add(
                            new Synapse.Builder()
                                .setNeuron(rec)
                                .setWeight(begin || end ? 2.0 : 1.0)
                                .setRecurrent(false)
                                .setBias(begin || end ? -2.0 : -1.0)
                                .setRelativeRid(i)
                                .setRangeMatch(begin ? EQUALS : LESS_THAN_EQUAL, end ? EQUALS : GREATER_THAN_EQUAL)
                                .setRangeOutput(begin, end)
                    );
                }
            }

            Neuron n = Neuron.init(m.createNeuron("PATTERN"), 0.5, INeuron.Type.EXCITATORY, inputs);

            System.out.println(n.get().node.get().logicToString());

            Document doc = m.createDocument("abc Hüttenheim cba", 0);

            for (int i = 0; i < doc.length(); i++) {
                char c = doc.getContent().toLowerCase().charAt(i);

                Neuron rec = chars.get(c);
                if (rec != null) {
                    Range r = new Range(i, doc.length());

                    rec.addInput(doc,
                            new Activation.Builder()
                                    .setRange(r)
                                    .setRelationalId(i)
                    );
                }
            }

            assert n.get().getActivations(doc).size() >= 1;
        }
    }
}