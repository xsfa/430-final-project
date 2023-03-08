/*
 * This is a template of QueueNode.java. Change this file name into QueueNode.java
 * and implement the logic.
 */

import java.util.*;

public class QueueNode {
    private Vector<Integer> tidQueue; // maintains a list of child TIDs who called wakeup( ).

    public QueueNode( ) {
	// Implement this constructor.
    }

    public synchronized int sleep( ) {
	// If tidQueue has nothing, call wait( ).
	// Otherwise, get one child TID from tidQueue.
	// return it.
    }

    public synchronized void wakeup( int tid ) {
	// Add this child TID to tidQueue.
	// Notify the parent.
    }
}
