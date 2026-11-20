package co.edu.uptc.structures;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.List;


public class BinaryTree<T> {

    private DoubleNode<T> root;
    private final Comparator<T> comparator;
    private int size;

    public BinaryTree(Comparator<T> comparator) {
        this.comparator = comparator;
    }


    public boolean add(T value) {
        if (value == null) {
            throw new IllegalArgumentException("No se permiten valores nulos");
        }
        if (root == null) {
            root = new DoubleNode<>(value);
            size++;
            return true;
        }
        DoubleNode<T> current = root;
        while (true) {
            int cmp = comparator.compare(value, current.getValue());
            if (cmp == 0) {
                return false;
            }
            if (cmp < 0) {
                if (current.getLeft() == null) {
                    current.setLeft(new DoubleNode<>(value));
                    size++;
                    return true;
                }
                current = current.getLeft();
            } else {
                if (current.getRight() == null) {
                    current.setRight(new DoubleNode<>(value));
                    size++;
                    return true;
                }
                current = current.getRight();
            }
        }
    }

   
    public boolean remove(T value) {
        if (value == null || !contains(value)) {
            return false;
        }
        root = remove(root, value);
        size--;
        return true;
    }

    private DoubleNode<T> remove(DoubleNode<T> node, T value) {
        if (node == null) {
            return null;
        }
        int cmp = comparator.compare(value, node.getValue());
        if (cmp < 0) {
            node.setLeft(remove(node.getLeft(), value));
        } else if (cmp > 0) {
            node.setRight(remove(node.getRight(), value));
        } else {
           
            if (node.getLeft() == null) {
                return node.getRight();
            }
            if (node.getRight() == null) {
                return node.getLeft();
            }
            
            DoubleNode<T> min = node.getRight();
            while (min.getLeft() != null) {
                min = min.getLeft();
            }
            node.setValue(min.getValue());
            node.setRight(removeMin(node.getRight()));
        }
        return node;
    }

    private DoubleNode<T> removeMin(DoubleNode<T> node) {
        if (node.getLeft() == null) {
            return node.getRight();
        }
        node.setLeft(removeMin(node.getLeft()));
        return node;
    }

    
    public T find(T value) {
        if (value == null) {
            return null;
        }
        DoubleNode<T> current = root;
        while (current != null) {
            int cmp = comparator.compare(value, current.getValue());
            if (cmp == 0) {
                return current.getValue();
            }
            current = cmp < 0 ? current.getLeft() : current.getRight();
        }
        return null;
    }

    public boolean contains(T value) {
        return find(value) != null;
    }

    public boolean isEmpty() {
        return root == null;
    }

    public int size() {
        return size;
    }

    public void clear() {
        root = null;
        size = 0;
    }

    
    public List<T> inOrder() {
        List<T> result = new ArrayList<>(size);
        Deque<DoubleNode<T>> stack = new ArrayDeque<>();
        DoubleNode<T> current = root;
        while (current != null || !stack.isEmpty()) {
            while (current != null) {
                stack.push(current);
                current = current.getLeft();
            }
            current = stack.pop();
            result.add(current.getValue());
            current = current.getRight();
        }
        return result;
    }
}