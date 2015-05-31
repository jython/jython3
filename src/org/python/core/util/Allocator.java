package org.python.core.util;

public interface Allocator<T> {
    public T allocate(int size);
}
