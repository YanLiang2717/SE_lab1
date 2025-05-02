package com.texttograph.model;

public class Node {
    private String word;

    public Node(String word) {
        System.out.println("this is a change");
        this.word = word.toLowerCase(); // 不区分大小写
    }

    public String getWord() {
        return word;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Node node = (Node) obj;
        return word.equals(node.word);
    }

    @Override
    public int hashCode() {
        return word.hashCode();
    }

    @Override
    public String toString() {
        return word;
    }
}