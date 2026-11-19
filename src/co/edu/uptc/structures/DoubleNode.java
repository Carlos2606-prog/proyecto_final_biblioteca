package co.edu.uptc.structures;

public class DoubleNode<E> {
	private E value;
	private DoubleNode<E> left;
	private DoubleNode<E> right;

	public DoubleNode(E value) {
		this.value = value;
		this.left = null;
		this.right = null;
	}

	public E getValue() {
		return value;
	}

	public void setValue(E value) {
		this.value = value;
	}

	public DoubleNode<E> getLeft() {
		return left;
	}

	public void setLeft(DoubleNode<E> previous) {
		this.left = previous;
	}

	public DoubleNode<E> getRight() {
		return right;
	}

	public void setRight(DoubleNode<E> next) {
		this.right = next;
	}

	@Override
	public String toString() {
		return "DoubleNode [value=" + value + ", next=" + right + "]";
	}

}