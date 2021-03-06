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
package network.aika;

import network.aika.lattice.Node;
import network.aika.lattice.NodeActivation;
import network.aika.neuron.INeuron;
import network.aika.neuron.Neuron;

import java.io.DataInput;
import java.io.IOException;

/**
 *
 * @author Lukas Molzberger
 */
public abstract class AbstractNode<P extends Provider<? extends AbstractNode>, A extends NodeActivation> implements Writable {

    public volatile int lastUsedDocumentId = 0;

    public volatile boolean modified;

    public P provider;

    /**
     * Propagate an activation to the next node or the next neuron that is depending on the current node.
     *
     * @param act
     */
    public abstract void propagate(A act);

    public void setModified() {
        modified = true;
    }

    public void suspend() {}

    public void reactivate() {}

    public static <P extends Provider> AbstractNode read(DataInput in, P p) throws IOException {
        AbstractNode n;
        if(in.readBoolean()) {
            n = INeuron.readNeuron(in, (Neuron) p);
        } else {
            n = Node.readNode(in, p);
        }
        return n;
    }

}
