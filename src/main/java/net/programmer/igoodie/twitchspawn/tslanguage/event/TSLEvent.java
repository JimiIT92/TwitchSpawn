package net.programmer.igoodie.twitchspawn.tslanguage.event;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import net.programmer.igoodie.twitchspawn.TwitchSpawn;
import net.programmer.igoodie.twitchspawn.tslanguage.EventArguments;
import net.programmer.igoodie.twitchspawn.tslanguage.TSLFlowNode;
import net.programmer.igoodie.twitchspawn.tslanguage.action.TSLAction;
import net.programmer.igoodie.twitchspawn.tslanguage.predicate.TSLPredicate;

import java.util.*;

public class TSLEvent implements TSLFlowNode {

    protected String name;
    protected List<TSLFlowNode> nextNodes;

    public TSLEvent(String name) {
        this.name = name;
        this.nextNodes = new LinkedList<>();
    }

    public List<TSLFlowNode> getNextNodes() {
        return nextNodes;
    }

    public String getName() {
        return name;
    }

    public List<TSLAction> getActions() {
        List<TSLAction> actions = new LinkedList<>();

        nodeLoop:
        for (TSLFlowNode nextNode : nextNodes) {
            TSLFlowNode node = nextNode;

            while (!(node instanceof TSLAction)) {
                if (node instanceof TSLPredicate) {
                    node = ((TSLPredicate) node).getNext();
                } else { // Not possible
                    continue nodeLoop;
                }
            }

            actions.add((TSLAction) node);
        }

        return actions;
    }

    @Override
    public TSLFlowNode chain(TSLFlowNode next) {
        nextNodes.add(next);
        return next;
    }

    @Override
    public boolean process(EventArguments args) {
        Iterator<TSLFlowNode> iterator = nextNodes.iterator();
        boolean success = false;

        while (iterator.hasNext()) {
            success = iterator.next().process(args);
            if (success) break; // Stop if handled once
        }

        return success;
    }

}

