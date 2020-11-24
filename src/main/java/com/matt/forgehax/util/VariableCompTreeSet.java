package com.matt.forgehax.util;

import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;


/*
 * This is just a class to make reorganizing the children commands in the Command class ez. Let me know if you know any better way
 * -TheAlphaEpsilon
 * 
 * tonio made it ugly but now you can sort them in the code yay
 */
public class VariableCompTreeSet<E> extends AbstractSet<E> { // sets are not sorted but whatever
	
    private transient List<E> original = new ArrayList<E>();
    private transient List<E> view; // dem lists f me

    private Comparator<E> comp = null;

    public VariableCompTreeSet() {
        super();
        updateView();
    }

    private void updateView() {
        view = original.stream() // lmao maybe use cloneable data struct
                    .collect(Collectors.toList());
        if (comp != null) {
            view.sort(comp);
        }
    }

    public void reorganizeBasedOn(Comparator<E> comp) { 
        this.comp = comp;
        updateView();
    }
    
    public Iterator<E> iterator() {
        return view.iterator();
    }

    public int size() {
        return original.size();
    }

    public boolean isEmpty() {
        return original.isEmpty();
    }

    public boolean contains(Object o) {
        return original.contains(o);
    }

    public boolean add(E e) {
        if (original.contains(e)) return true;
        boolean val = original.add(e);
        if (val) updateView();
        return val;
    }
    
    public boolean remove(Object o) {
        boolean val = original.remove(o);
        if (val) updateView();
        return val;
    }

    public void clear() {
        original.clear();
        updateView();
    }
}
