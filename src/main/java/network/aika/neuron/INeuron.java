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
package network.aika.neuron;


import network.aika.*;
import network.aika.lattice.OrNode;
import network.aika.neuron.activation.Activation;
import network.aika.neuron.activation.Linker;
import network.aika.neuron.activation.Range;
import network.aika.neuron.activation.SearchNode;
import network.aika.*;
import network.aika.neuron.activation.*;
import network.aika.lattice.InputNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;


/**
 * The {@code INeuron} class represents a internal neuron implementation in Aikas neural network and is connected to other neurons through
 * input synapses and output synapses. The activation value of a neuron is calculated by computing the weighted sum
 * (input act. value * synapse weight) of the input synapses, adding the bias to it and sending the resulting value
 * through a transfer function (the upper part of tanh).
 * <p>
 * <p>The neuron does not store its activations by itself. The activation objects are stored within the
 * logic nodes. To access the activations of this neuron simply use the member variable {@code node} or use
 * the method {@code getFinalActivations(Document doc)} to ge the final activations of this neuron.
 *
 * @author Lukas Molzberger
 */
public class INeuron extends AbstractNode<Neuron, Activation> implements Comparable<INeuron> {

    public static boolean ALLOW_WEAK_NEGATIVE_WEIGHTS = false;

    private static final Logger log = LoggerFactory.getLogger(INeuron.class);

    public static double WEIGHT_TOLERANCE = 0.001;
    public static double TOLERANCE = 0.000001;

    public String label;
    public Type type;

    public enum Type {
        EXCITATORY,
        INHIBITORY,
        META
    }

    public String outputText;

    public volatile double bias;
    public volatile double biasDelta;
    public volatile double biasSum;
    public volatile double biasSumDelta;

    public volatile double metaBias = 0.0;


    public volatile double posDirSum;
    public volatile double negDirSum;
    public volatile double negRecSum;
    public volatile double posRecSum;

    public volatile double requiredSum;
    public volatile int numDisjunctiveSynapses = 0;

    public Writable statistic;

    public ActivationFunction activationFunction = ActivationFunction.RECTIFIED_SCALED_LOGISTIC_SIGMOID;


    // A synapse is stored only in one direction, depending on the synapse weight.
    public TreeMap<Synapse, Synapse> inputSynapses = new TreeMap<>(Synapse.INPUT_SYNAPSE_COMP);
    public TreeMap<Synapse, Synapse> outputSynapses = new TreeMap<>(Synapse.OUTPUT_SYNAPSE_COMP);

    public Provider<InputNode> outputNode;

    public Provider<OrNode> node;


    public ReadWriteLock lock = new ReadWriteLock();


    public ThreadState[] threads;

    /**
     * The {@code ThreadState} is a thread local data structure containing the activations of a single document for
     * a specific logic node.
     */
    public static class ThreadState {
        public long lastUsed;

        public TreeMap<Range, Activation> activations;
        public TreeMap<Range, Activation> activationsEnd;
        public int minLength = Integer.MAX_VALUE;
        public int maxLength = 0;

        public ThreadState() {
            activations = new TreeMap<>(Range.BEGIN_COMP);
            activationsEnd = new TreeMap<>(Range.END_COMP);
        }
    }


    public ThreadState getThreadState(int threadId, boolean create) {
        ThreadState th = threads[threadId];
        if (th == null) {
            if (!create) return null;

            th = new ThreadState();
            threads[threadId] = th;
        }
        th.lastUsed = provider.model.docIdCounter.get();
        return th;
    }



    private INeuron() {
    }


    public INeuron(Model m) {
        this(m, null);
    }


    public INeuron(Model m, String label) {
        this(m, label, null);
    }


    public INeuron(Model m, String label, String outputText) {
        this.label = label;
        this.outputText = outputText;

        if(m.neuronStatisticFactory != null) {
            statistic = m.neuronStatisticFactory.createStatisticObject();
        }

        threads = new ThreadState[m.numberOfThreads];

        provider = new Neuron(m, this);

        OrNode node = new OrNode(m);

        node.neuron = provider;
        this.node = node.provider;

        setModified();
    }

    /**
     * Propagate an input activation into the network.
     *
     * @param doc   The current document
     * @param input
     */
    public Activation addInput(Document doc, Activation.Builder input) {
        assert input.range.begin <= input.range.end;

        Activation act = getThreadState(doc.threadId, true).activations.get(input.range);
        if(act == null) {
            act = new Activation(doc.activationIdCounter++, doc, node.get(doc));
            act.range = input.range;
        }

        register(act);

        Activation.State s = new Activation.State(input.value, 0.0, input.fired, SearchNode.Weight.ZERO);
        act.rounds.set(0, s);
        act.inputValue = input.value;
        act.upperBound = input.value;
        act.lowerBound = input.value;

        act.inputDecision = SearchNode.Decision.SELECTED;
        act.finalDecision = act.inputDecision;
        act.setDecision(act.inputDecision, doc.visitedCounter++);


        act.setTargetValue(input.targetValue);

        doc.inputNeuronActivations.add(act);
        doc.finallyActivatedNeurons.add(act.getINeuron());

        propagate(act);

        doc.propagate();

        return act;
    }


    // TODO
    public void remove() {

        clearActivations();

        for (Synapse s : inputSynapses.values()) {
            INeuron in = s.input.get();
            in.provider.lock.acquireWriteLock();
            in.provider.inMemoryOutputSynapses.remove(s);
            in.provider.lock.releaseWriteLock();
        }

        provider.lock.acquireReadLock();
        for (Synapse s : provider.inMemoryOutputSynapses.values()) {
            INeuron out = s.output.get();
            out.lock.acquireWriteLock();
            out.inputSynapses.remove(s);
            out.lock.releaseWriteLock();
        }
        provider.lock.releaseReadLock();
    }


    public void propagate(Activation act) {
        Document doc = act.doc;
        outputNode.get(doc).addActivation(act);
    }


    public Collection<Activation> getActivations(Document doc) {
        ThreadState th = getThreadState(doc.threadId, false);
        if (th == null) return Collections.EMPTY_LIST;
        return th.activations.values();
    }


    public synchronized Activation getFirstActivation(Document doc) {
        ThreadState th = getThreadState(doc.threadId, false);
        if (th == null || th.activations.isEmpty()) return null;
        return th.activations.firstEntry().getValue();
    }


    public void clearActivations() {
        for (int i = 0; i < provider.model.numberOfThreads; i++) {
            clearActivations(i);
        }
    }


    public void clearActivations(Document doc) {
        clearActivations(doc.threadId);
    }


    public void clearActivations(int threadId) {
        ThreadState th = getThreadState(threadId, false);
        if (th == null) return;
        th.activations.clear();

        if (th.activationsEnd != null) th.activationsEnd.clear();
    }


    public int compareTo(INeuron n) {
        if (provider.id < n.provider.id) return -1;
        else if (provider.id > n.provider.id) return 1;
        else return 0;
    }


    @Override
    public void write(DataOutput out) throws IOException {
        out.writeBoolean(true);

        out.writeBoolean(label != null);
        if(label != null) {
            out.writeUTF(label);
        }

        out.writeBoolean(type != null);
        if(type != null) {
            out.writeUTF(type.name());
        }

        out.writeBoolean(outputText != null);
        if(outputText != null) {
            out.writeUTF(outputText);
        }

        out.writeBoolean(statistic != null);
        if(statistic != null) {
            statistic.write(out);
        }

        out.writeDouble(bias);
        out.writeDouble(biasSum);
        out.writeDouble(posDirSum);
        out.writeDouble(negDirSum);
        out.writeDouble(negRecSum);
        out.writeDouble(posRecSum);
        out.writeDouble(requiredSum);
        out.writeInt(numDisjunctiveSynapses);

        out.writeUTF(activationFunction.name());

        out.writeInt(outputNode.id);

        out.writeBoolean(node != null);
        if (node != null) {
            out.writeInt(node.id);
        }

        for (Synapse s : inputSynapses.values()) {
            if (s.input != null) {
                out.writeBoolean(true);
                s.write(out);
            }
        }
        out.writeBoolean(false);
        for (Synapse s : outputSynapses.values()) {
            if (s.output != null) {
                out.writeBoolean(true);
                s.write(out);
            }
        }
        out.writeBoolean(false);
    }


    @Override
    public void readFields(DataInput in, Model m) throws IOException {
        if(in.readBoolean()) {
            label = in.readUTF();
        }

        if(in.readBoolean()) {
            type = Type.valueOf(in.readUTF());
        }

        if(in.readBoolean()) {
            outputText = in.readUTF();
        }

        if(in.readBoolean()) {
            statistic = m.neuronStatisticFactory.createStatisticObject();
            statistic.readFields(in, m);
        }

        bias = in.readDouble();
        biasSum = in.readDouble();
        posDirSum = in.readDouble();
        negDirSum = in.readDouble();
        negRecSum = in.readDouble();
        posRecSum = in.readDouble();
        requiredSum = in.readDouble();
        numDisjunctiveSynapses = in.readInt();

        activationFunction = ActivationFunction.valueOf(in.readUTF());

        outputNode = m.lookupNodeProvider(in.readInt());

        if (in.readBoolean()) {
            Integer nId = in.readInt();
            node = m.lookupNodeProvider(nId);
        }

        while (in.readBoolean()) {
            Synapse syn = Synapse.read(in, m);

            inputSynapses.put(syn, syn);
        }

        while (in.readBoolean()) {
            Synapse syn = Synapse.read(in, m);

            outputSynapses.put(syn, syn);
        }
    }


    @Override
    public void suspend() {
        for (Synapse s : inputSynapses.values()) {
            s.input.removeInMemoryOutputSynapse(s);
        }
        for (Synapse s : outputSynapses.values()) {
            s.output.removeInMemoryInputSynapse(s);
        }

        provider.lock.acquireReadLock();
        for (Synapse s : provider.inMemoryInputSynapses.values()) {
            if(!s.isConjunction) {
                s.input.removeInMemoryOutputSynapse(s);
            }
        }
        for (Synapse s : provider.inMemoryOutputSynapses.values()) {
            if(s.isConjunction) {
                s.output.removeInMemoryInputSynapse(s);
            }
        }
        provider.lock.releaseReadLock();
    }


    @Override
    public void reactivate() {
        provider.lock.acquireReadLock();
        for (Synapse s : provider.inMemoryInputSynapses.values()) {
            if(!s.isConjunction) {
                s.input.addInMemoryOutputSynapse(s);
            }
        }
        for (Synapse s : provider.inMemoryOutputSynapses.values()) {
            if(s.isConjunction) {
                s.output.addInMemoryInputSynapse(s);
            }
        }
        provider.lock.releaseReadLock();

        for (Synapse s : inputSynapses.values()) {
            s.input.addInMemoryOutputSynapse(s);
            if (!s.input.isSuspended()) {
                s.output.addInMemoryInputSynapse(s);
            }
        }
        for (Synapse s : outputSynapses.values()) {
            s.output.addInMemoryInputSynapse(s);
            if (!s.output.isSuspended()) {
                s.input.addInMemoryOutputSynapse(s);
            }
        }
    }

    public void setBias(double b) {
        double newBiasDelta = b - bias;
        biasSumDelta += newBiasDelta - biasDelta;
        biasDelta = newBiasDelta;
    }


    public void changeBias(double bd) {
        biasDelta += bd;
        biasSumDelta += bd;
    }


    public double getNewBiasSum() {
        return biasSum + biasSumDelta;
    }


    public void register(Activation act) {
        Document doc = act.doc;
        INeuron.ThreadState th = act.node.neuron.get().getThreadState(doc.threadId, true);

        if (th.activations.isEmpty()) {
            doc.activatedNeurons.add(act.node.neuron.get());
        }

        th.minLength = Math.min(th.minLength, act.range.length());
        th.maxLength = Math.max(th.maxLength, act.range.length());

        th.activations.put(act.range, act);

        TreeMap<Range, Activation> actEnd = th.activationsEnd;
        if (actEnd != null) actEnd.put(act.range, act);

        Document.ActKey ak = new Document.ActKey(act.range, act.node);
        if (act.range.begin != Integer.MIN_VALUE) {
            doc.activationsByRangeBegin.put(ak, act);
        }
        if (act.range.end != Integer.MAX_VALUE) {
            doc.activationsByRangeEnd.put(ak, act);
        }

        doc.addedActivations.add(act);

        Linker.link(act);
    }


    public static boolean update(Model m, int threadId, Document doc, Neuron pn, Double bias, Collection<Synapse> modifiedSynapses) {
        INeuron n = pn.get();

        if(bias != null) {
            n.setBias(bias);
        }

        // s.link requires an updated n.biasSumDelta value.
        modifiedSynapses.forEach(s -> s.link());

        return Converter.convert(threadId, doc, n, modifiedSynapses);
    }


    public static INeuron readNeuron(DataInput in, Neuron p) throws IOException {
        INeuron n = new INeuron();
        n.provider = p;
        n.threads = new ThreadState[p.model.numberOfThreads];
        n.readFields(in, p.model);
        return n;
    }


    public String toString() {
        return label;
    }


    public String toStringWithSynapses() {
        SortedSet<Synapse> is = new TreeSet<>((s1, s2) -> {
            int r = Double.compare(s2.weight, s1.weight);
            if (r != 0) return r;
            return Integer.compare(s1.input.id, s2.input.id);
        });

        is.addAll(inputSynapses.values());

        StringBuilder sb = new StringBuilder();
        sb.append(toString());
        sb.append("<");
        sb.append("B:");
        sb.append(Utils.round(biasSum));
        for (Synapse s : is) {
            sb.append(", ");
            sb.append(Utils.round(s.weight));
            sb.append(":");
            sb.append(s.input.toString());
        }
        sb.append(">");
        return sb.toString();
    }


    /**
     * {@code getFinalActivations} is a convenience method to retrieve all activations of the given neuron that
     * are part of the final interpretation. Before calling this method, the {@code doc.process()} needs to
     * be called first.
     *
     * @param doc The current document
     * @return A collection with all final activations of this neuron.
     */
    public Stream<Activation> getFinalActivationsStream(Document doc) {
        return getActivations(doc).stream().filter(act -> act.isFinalActivation());
    }


    public Collection<Activation> getFinalActivations(Document doc) {
        return getFinalActivationsStream(doc).collect(Collectors.toList());
    }

}