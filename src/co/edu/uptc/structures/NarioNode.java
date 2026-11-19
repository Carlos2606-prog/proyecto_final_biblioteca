package co.edu.uptc.structures;

import java.util.ArrayList;
import java.util.List;

public class NarioNode<T> {

    private T data;
    private List<NarioNode<T>> children;

    public NarioNode(T data) {
        this.data = data;
        this.children = new ArrayList<>();
    }

    public boolean add(NarioNode<T> son) {
        return this.children.add(son);
    }

    public T getData() {
        return this.data;
    }

    public List<NarioNode<T>> getChildren() {
        return this.children;
    }
}