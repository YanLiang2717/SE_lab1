package com.texttograph.model;

public class Edge {
    private Node source;
    private Node target;
    private int weight;

    public Edge(Node source, Node target, int weight) {
        this.source = source;
        this.target = target;
        this.weight = weight;
    }

    public Node getSource() {
        return source;
    }

    public Node getTarget() {
        return target;
    }

    public int getWeight() {
        return weight;
    }

    public void incrementWeight() {
        weight++;
    }

    @Override
    public String toString() {
        System.out.println("a change");
        return source + " -> " + target + " [weight=" + weight + "]";
    }
}