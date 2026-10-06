package com.github.darksoulq.visage.util;

import com.github.darksoulq.visage.VisageConfig;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.function.Function;

public class Octree<N> implements Iterable<N> {

    private final int maxDepth;
    private final int maxEntries;
    private BoundingBox bounds;
    private final int depth;
    private final Function<N, BoundingBox> entryStrategy;
    private final boolean storeOutOfBoundsEntries;

    private final Set<N> entries = new CopyOnWriteArraySet<>();
    private final Set<N> outOfBoundsEntries = new CopyOnWriteArraySet<>();
    private Octree<N>[] children = null;

    public Octree(BoundingBox bounds, int depth, Function<N, BoundingBox> entryStrategy) {
        this(VisageConfig.OCTREE_MAX_DEPTH, VisageConfig.OCTREE_MAX_ENTRIES, bounds, depth, entryStrategy, false);
    }

    public Octree(int maxDepth, int maxEntries, BoundingBox bounds, int depth, Function<N, BoundingBox> entryStrategy, boolean storeOutOfBoundsEntries) {
        this.maxDepth = maxDepth;
        this.maxEntries = maxEntries;
        this.bounds = bounds;
        this.depth = depth;
        this.entryStrategy = entryStrategy;
        this.storeOutOfBoundsEntries = storeOutOfBoundsEntries;
    }

    public boolean insert(N entry) {
        BoundingBox entryBounds = entryStrategy.apply(entry);
        if (!bounds.overlaps(entryBounds)) {
            if (storeOutOfBoundsEntries) outOfBoundsEntries.add(entry);
            return false;
        }

        if (children != null) {
            for (Octree<N> child : children) {
                if (child.bounds.overlaps(entryBounds)) {
                    return child.insert(entry);
                }
            }
        }

        if (entries.size() < maxEntries || depth >= maxDepth) {
            entries.add(entry);
            return true;
        }

        subdivide();
        return insert(entry);
    }

    public boolean remove(N entry) {
        BoundingBox entryBounds = entryStrategy.apply(entry);
        if (!bounds.overlaps(entryBounds)) {
            if (storeOutOfBoundsEntries) outOfBoundsEntries.remove(entry);
            return false;
        }

        if (entries.remove(entry)) {
            return true;
        }

        if (children != null) {
            for (Octree<N> child : children) {
                if (child.bounds.overlaps(entryBounds)) {
                    if (child.remove(entry)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public void clear() {
        entries.clear();
        outOfBoundsEntries.clear();
        if (children != null) {
            for (Octree<N> child : children) child.clear();
            children = null;
        }
    }

    @SuppressWarnings("unchecked")
    private void subdivide() {
        Vector min = bounds.getMin();
        Vector max = bounds.getMax();
        Vector center = min.clone().add(max).multiply(0.5);

        children = new Octree[8];
        for (int i = 0; i < 8; i++) {
            int dx = (i >> 2) & 1;
            int dy = (i >> 1) & 1;
            int dz = i & 1;
            Vector childMin = new Vector(
                dx == 0 ? min.getX() : center.getX(),
                dy == 0 ? min.getY() : center.getY(),
                dz == 0 ? min.getZ() : center.getZ()
            );
            Vector childMax = new Vector(
                dx == 0 ? center.getX() : max.getX(),
                dy == 0 ? center.getY() : max.getY(),
                dz == 0 ? center.getZ() : max.getZ()
            );
            children[i] = new Octree<>(maxDepth, maxEntries, BoundingBox.of(childMin, childMax), depth + 1, entryStrategy, storeOutOfBoundsEntries);
        }

        List<N> toTransfer = new ArrayList<>(entries);
        entries.clear();
        for (N entry : toTransfer) {
            for (Octree<N> child : children) {
                if (child.bounds.overlaps(entryStrategy.apply(entry))) {
                    child.insert(entry);
                    break;
                }
            }
        }
    }

    public List<N> query(BoundingBox range) {
        List<N> result = new ArrayList<>();
        query(range, result);
        return result;
    }

    public void query(BoundingBox range, List<N> result) {
        if (!bounds.overlaps(range)) return;

        for (N entry : entries) {
            if (range.overlaps(entryStrategy.apply(entry))) {
                result.add(entry);
            }
        }

        if (children != null) {
            for (Octree<N> child : children) {
                child.query(range, result);
            }
        }
    }

    @Override
    public Iterator<N> iterator() {
        return new OctreeIterator<>(this);
    }

    private static class OctreeIterator<N> implements Iterator<N> {
        private final List<Octree<N>> stack = new ArrayList<>();
        private Iterator<N> currentEntries = null;

        public OctreeIterator(Octree<N> root) {
            stack.add(root);
        }

        @Override
        public boolean hasNext() {
            while (true) {
                if (currentEntries != null && currentEntries.hasNext()) {
                    return true;
                }
                if (stack.isEmpty()) {
                    return false;
                }
                Octree<N> node = stack.remove(stack.size() - 1);
                currentEntries = node.entries.iterator();
                if (node.children != null) {
                    for (Octree<N> child : node.children) {
                        stack.add(child);
                    }
                }
            }
        }

        @Override
        public N next() {
            if (!hasNext()) throw new NoSuchElementException();
            return currentEntries.next();
        }
    }
}