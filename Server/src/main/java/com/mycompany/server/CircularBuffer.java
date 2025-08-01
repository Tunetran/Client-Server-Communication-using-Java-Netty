/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.server;

/**
 *
 * @author VT
 */
import java.util.LinkedList;

public class CircularBuffer<E> extends LinkedList<E> {

    private final int maxSize;

    public CircularBuffer(int maxSize) {
        this.maxSize = maxSize;
    }

    @Override
    public boolean add(E e) {
        if (size() >= maxSize) {
            removeFirst();
        }
        return super.add(e);
    }
}
