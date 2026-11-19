package co.edu.uptc.structures;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

public class NarioTree<T> {
    private NarioNode<T> root;
    private Comparator<T> comparator;

    public NarioTree(T valueRoot, Comparator<T> comparator) {
        this.root = new NarioNode<>(valueRoot);
        this.comparator = comparator;
    }

    public boolean add(T parent, T son) {
        if (search(son) != null) {
            return false;
        }

        NarioNode<T> parentNode = search(parent);
        if (parentNode == null) {
            return false;
        }

        return parentNode.add(new NarioNode<>(son));
    }

    public boolean remove(T parent, T son) {
        NarioNode<T> parentNode = search(parent);
        if (parentNode == null) {
            return false;
        }

        List<NarioNode<T>> children = parentNode.getChildren();
        for (int i = 0; i < children.size(); i++) {
            if (comparator.compare(children.get(i).getData(), son) == 0) {
                children.remove(i);
                return true;
            }
        }

        return false;
    }

    public NarioNode<T> search(T value) {
        return search(root, value);
    }

    private NarioNode<T> search(NarioNode<T> current, T value) {
        if (current == null) {
            return null;
        }

        if (comparator.compare(current.getData(), value) == 0) {
            return current;
        }

        for (NarioNode<T> child : current.getChildren()) {
            NarioNode<T> result = search(child, value);
            if (result != null) {
                return result;
            }
        }

        return null;
    }

    public Iterator<T> iterator() {
        List<T> elements = new ArrayList<>();
        preOrder(root, elements);
        return elements.iterator();
    }

    private void preOrder(NarioNode<T> current, List<T> elements) {
        if (current != null) {
            elements.add(current.getData());
            for (NarioNode<T> child : current.getChildren()) {
                preOrder(child, elements);
            }
        }
    }
}