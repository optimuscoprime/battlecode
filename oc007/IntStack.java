package oc007;

public class IntStack {

	private int numItems;
	private int capacity;
	private int[] items;

	private static final int DEFAULT_CAPACITY = 10;

	public IntStack() {
		this(DEFAULT_CAPACITY);
	}

	public IntStack(int initialCapacity) {
		this.capacity = initialCapacity;
		this.items = new int[initialCapacity];
		this.numItems = 0;
	}

	public void push(int item) {
		if (numItems >= capacity) {
			capacity *= 2;
			int[] newItems = new int[capacity];
			for (int i=0; i < numItems; i++) {
				newItems[i] = items[i];
			}
			items = newItems;
		}

		items[numItems] = item;
		numItems++;
	}

	public int pop() {
		int itemToReturn;

		if (numItems > 0) {
			itemToReturn = items[numItems-1];
			numItems--;			
		} else {
			throw new RuntimeException("Can't pop an empty stack");
		}

		return itemToReturn;
	}
}
