package io.luna.game.model.item;

import com.google.common.collect.Iterables;
import com.google.common.primitives.Ints;
import io.luna.net.msg.out.WidgetItemGroupMessageWriter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;
import static io.luna.game.model.item.ItemContainer.StackPolicy.ALWAYS;
import static io.luna.game.model.item.ItemContainer.StackPolicy.STANDARD;

/**
 * A model representing a group of items.
 *
 * @author lare96 <http://github.com/lare96>
 */
public class ItemContainer implements Iterable<Item> {

    /**
     * A fail-safe iterator for items within this container.
     */
    private final class ItemContainerIterator implements Iterator<Item> {

        /**
         * The current index.
         */
        private int index;

        /**
         * The last index.
         */
        private int lastIndex = -1;

        @Override
        public boolean hasNext() {
            return (index + 1) <= capacity;
        }

        @Override
        public Item next() {
            checkState(index < capacity, "no more elements left to iterate");

            lastIndex = index;
            index++;
            return items[lastIndex];
        }

        @Override
        public void remove() {
            checkState(lastIndex != -1, "can only be called once after 'next'");

            Item oldItem = items[lastIndex];

            items[lastIndex] = null;
            size--;

            fireUpdateEvent(oldItem, null, lastIndex);

            index = lastIndex;
            lastIndex = -1;
        }
    }

    /**
     * An enum representing policies for when to stack items.
     */
    public enum StackPolicy {

        /**
         * Stack stackable items.
         */
        STANDARD,

        /**
         * Stack all items.
         */
        ALWAYS,

        /**
         * Stack no items.
         */
        NEVER
    }

    /**
     * A list of listeners.
     */
    private final List<ItemContainerListener> listeners = new ArrayList<>();

    /**
     * The capacity.
     */
    private final int capacity;

    /**
     * The stack policy.
     */
    private final StackPolicy policy;

    /**
     * The items.
     */
    private final Item[] items;

    /**
     * The size.
     */
    private int size;

    /**
     * If events are being fired.
     */
    private boolean firingEvents = true;

    /**
     * If a bulk operation is in progress.
     */
    private boolean bulkOperation;

    /**
     * Creates a new {@link ItemContainer}.
     *
     * @param capacity The capacity.
     * @param policy The stack policy.
     */
    public ItemContainer(int capacity, StackPolicy policy) {
        this.capacity = capacity;
        this.policy = policy;
        items = new Item[capacity];
    }

    /**
     * This implementation will skip {@code null} values.
     */
    @Override
    public final void forEach(Consumer<? super Item> action) {
        Objects.requireNonNull(action);
        for (int index = 0; index < capacity; index++) {
            Item item = items[index];
            if (item == null) {
                continue;
            }
            action.accept(item);
        }
    }

    @Override
    public final Spliterator<Item> spliterator() {
        return Spliterators.spliterator(items, Spliterator.ORDERED | Spliterator.IMMUTABLE | Spliterator.SIZED);
    }

    @Override
    public final Iterator<Item> iterator() {
        return new ItemContainerIterator();
    }

    /**
     * Returns a sequential stream constructed using {@code spliterator()}. This stream is {@code null}-free.
     */
    public final Stream<Item> stream() {
        return StreamSupport.stream(spliterator(), false).filter(Objects::nonNull);
    }

    /**
     * Attempts to add {@code item} at {@code preferredIndex}. Returns {@code true} if successful.
     */
    public boolean add(Item item, int preferredIndex) {
        checkArgument(preferredIndex >= -1, "invalid index");

        boolean stackable = isStackable(item);
        if (stackable) {
            preferredIndex = computeIndexForId(item.getId()).orElse(-1);
        } else if (preferredIndex != -1) {
            preferredIndex = items[preferredIndex] != null ? -1 : preferredIndex;
        }

        if (preferredIndex == -1) {
            preferredIndex = computeFreeIndex().orElse(-1);
        }

        if (preferredIndex == -1) { /* Not enough space in container. */
            fireCapacityExceededEvent();
            return false;
        }

        if (stackable) {
            Item current = items[preferredIndex];

            if (current == null) {
                items[preferredIndex] = item;
                size++;
            } else {
                items[preferredIndex] = current.createAndIncrement(item.getAmount());
            }

            fireUpdateEvent(current, items[preferredIndex], preferredIndex);
        } else {
            int remaining = computeRemainingSize();
            int until = (remaining > item.getAmount()) ? item.getAmount() : remaining;

            for (int index = 0; index < until; index++) {

                if (items[preferredIndex] != null) {
                    preferredIndex = computeFreeIndex().get();
                }

                Item newItem = new Item(item.getId());
                items[preferredIndex] = newItem;
                size++;

                fireUpdateEvent(null, newItem, preferredIndex++);
            }
        }
        return true;
    }

    /**
     * Attempts to add {@code item}. Returns {@code true} if successful.
     */
    public boolean add(Item item) {
        return add(item, -1);
    }

    /**
     * Attempts to add {@code items}. Returns {@code true} if at least one was added.
     */
    public boolean addAll(Iterable<? extends Item> items) {
        boolean added = false;
        bulkOperation = true;
        try {
            for (Item item : items) {
                if (item == null) {
                    continue;
                }
                if (add(item)) {
                    added = true;
                }
            }
        } finally {
            bulkOperation = false;
        }

        fireUpdateCompletedEvent();
        return added;
    }

    /**
     * Attempts to add {@code items}. Returns {@code true} if at least one was added.
     */
    public boolean addAll(Item... items) {
        return addAll(Arrays.asList(items));
    }

    /**
     * Attempts to remove {@code item} at {@code preferredIndex}. Returns {@code true} if successful.
     */
    public boolean remove(Item item, int preferredIndex) {
        checkArgument(preferredIndex >= -1, "invalid index identifier");

        boolean stackable = isStackable(item);
        if (stackable) {
            preferredIndex = computeIndexForId(item.getId()).orElse(-1);
        } else {
            preferredIndex = preferredIndex == -1 ? computeIndexForId(item.getId()).orElse(-1) : preferredIndex;

            if (preferredIndex != -1 && items[preferredIndex] == null) {
                preferredIndex = -1;
            }
        }

        if (preferredIndex == -1) { // Item isn't present within this container.
            return false;
        }

        if (stackable) {
            Item current = items[preferredIndex];
            if (current.getAmount() > item.getAmount()) {
                items[preferredIndex] = current.createAndDecrement(item.getAmount());
            } else {
                items[preferredIndex] = null;
                size--;
            }

            fireUpdateEvent(current, items[preferredIndex], preferredIndex);
        } else {
            int until = computeAmountForId(item.getId());
            until = (item.getAmount() > until) ? until : item.getAmount();

            for (int index = 0; index < until; index++) {
                preferredIndex = (items[preferredIndex] != null && items[preferredIndex].getId() == item.getId()) ?
                    preferredIndex : computeIndexForId(item.getId()).orElse(-1);

                Item oldItem = items[preferredIndex];
                items[preferredIndex] = null;
                size--;

                fireUpdateEvent(oldItem, null, preferredIndex++);
            }
        }
        return true;
    }

    /**
     * Attempts to remove {@code item}. Returns {@code true} if successful.
     */
    public boolean remove(Item item) {
        return remove(item, -1);
    }

    /**
     * Attempts to remove {@code items}. Returns {@code true} if at least one was removed.
     */
    public boolean removeAll(Iterable<? extends Item> items) {
        boolean removed = false;
        bulkOperation = true;
        try {
            for (Item item : items) {
                if (item == null) {
                    continue;
                }
                if (remove(item)) {
                    removed = true;
                }
            }
        } finally {
            bulkOperation = false;
        }

        fireUpdateCompletedEvent();
        return removed;
    }

    /**
     * Attempts to remove {@code items}. Returns {@code true} if at least one was removed.
     */
    public boolean removeAll(Item... items) {
        return removeAll(Arrays.asList(items));
    }

    /**
     * Computes the next free index.
     */
    public final Optional<Integer> computeFreeIndex() {
        for (int index = 0; index < capacity; index++) {
            if (items[index] == null) {
                return Optional.of(index);
            }
        }
        return Optional.empty();
    }

    /**
     * Computes the next index that {@code id} is found in.
     */
    public final Optional<Integer> computeIndexForId(int id) {
        for (int index = 0; index < capacity; index++) {
            Item item = items[index];
            if (item != null && item.getId() == id) {
                return Optional.of(index);
            }
        }
        return Optional.empty();
    }

    /**
     * Computes the total quantity of items with {@code id}.
     */
    public final int computeAmountForId(int id) {
        int amount = 0;
        for (Item item : items) {
            if (item == null || item.getId() != id) {
                continue;
            }
            amount += item.getAmount();
        }
        return amount;
    }

    /**
     * Computes the identifier at {@code index}.
     */
    public final Optional<Integer> computeIdForIndex(int index) {
        return retrieve(index).map(Item::getId);
    }

    /**
     * Computes the amount at {@code index}.
     */
    public final int computeAmountForIndex(int index) {
        return retrieve(index).map(Item::getAmount).orElse(0);
    }

    /**
     * Computes the amount of indexes required to hold {@code forItems}.
     */
    public final int computeSize(Item... forItems) {
        int size = 0;
        for (Item item : forItems) {
            boolean stackable = isStackable(item);
            if (stackable) {
                int index = computeIndexForId(item.getId()).orElse(-1);
                if (index == -1) {
                    size++;
                    continue;
                }

                Item existing = items[index];
                if ((existing.getAmount() + item.getAmount()) <= 0) {
                    size++;
                }
            } else {
                size += item.getAmount();
            }
        }
        return size;
    }

    /**
     * Computes the remaining amount of free indexes.
     */
    public final int computeRemainingSize() {
        return capacity - size;
    }

    /**
     * Replaces the first occurrence of {@code oldId} with {@code newId}. Returns {@code true} if successful.
     */
    public final boolean replace(int oldId, int newId) {
        Optional<Integer> oldIndex = computeIndexForId(oldId);
        if (!oldIndex.isPresent()) {
            return false;
        }
        int index = oldIndex.get();

        Item oldItem = items[index];
        Item newItem = new Item(newId);

        checkState(!isStackable(oldItem) && !isStackable(newItem), "use add(Item) and remove(Item) instead");

        items[index] = null;
        items[index] = newItem;
        fireUpdateEvent(oldItem, newItem, index);
        return true;
    }

    /**
     * Replaces all occurrences of {@code oldId} with {@code newId}. Returns {@code true} if at least one
     * was replaced.
     */
    public final boolean replaceAll(int oldId, int newId) {
        boolean replaced = false;
        bulkOperation = true;
        try {
            while (replace(oldId, newId)) {
                replaced = true;
            }
        } finally {
            bulkOperation = false;
        }
        fireUpdateCompletedEvent();
        return replaced;
    }

    /**
     * Determines if there is enough space for {@code items} to be added.
     */
    public final boolean hasCapacityFor(Item... items) {
        int expectedSize = computeSize(items);
        return computeRemainingSize() >= expectedSize;
    }

    /**
     * Determines if an item with {@code id} is present.
     */
    public final boolean contains(int id) {
        return computeIndexForId(id).isPresent();
    }

    /**
     * Determines if all items with {@code ids} are present.
     */
    public final boolean containsAll(int... ids) {
        return containsAllIds(Ints.asList(ids));
    }

    /**
     * Determines if all items with {@code ids} are present.
     */
    public final boolean containsAllIds(Iterable<? extends Integer> ids) {
        for (int id : ids) {
            if (!contains(id)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Determines if any items with {@code ids} are present.
     */
    public final boolean containsAny(int... ids) {
        return containsAnyIds(Ints.asList(ids));
    }

    /**
     * Determines if any items with {@code ids} are present.
     */
    public final boolean containsAnyIds(Iterable<? extends Integer> ids) {
        for (int id : ids) {
            if (contains(id)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Determines if {@code item} is present.
     */
    public final boolean contains(Item item) {
        return stream().anyMatch(it -> it.getId() == item.getId() && it.getAmount() >= item.getAmount());
    }

    /**
     * Determines if all {@code items} are present.
     */
    public final boolean containsAll(Item... items) {
        return containsAll(Arrays.asList(items));
    }

    /**
     * Determines if all {@code items} are present.
     */
    public final boolean containsAll(Iterable<? extends Item> items) {
        for (Item item : items) {
            if (!contains(item)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Determines if any {@code items} are present.
     */
    public final boolean containsAny(Item... items) {
        return containsAny(Arrays.asList(items));
    }

    /**
     * Determines if any {@code items} are present.
     */
    public final boolean containsAny(Iterable<? extends Item> items) {
        for (Item item : items) {
            if (contains(item)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Determines if {@code item} will stack when added.
     */
    public final boolean isStackable(Item item) {
        return (policy == STANDARD && item.getItemDef().isStackable()) || policy == ALWAYS;
    }

    /**
     * Returns a message that will display these items on {@code widget}.
     */
    public final WidgetItemGroupMessageWriter constructRefresh(int widget) {
        return new WidgetItemGroupMessageWriter(widget, items);
    }

    /**
     * Swaps the items on {@code firstIndex} and {@code secondIndex}.
     */
    public final void swap(int firstIndex, int secondIndex) {
        bulkOperation = true;
        try {
            swapOperation(firstIndex, secondIndex);
        } finally {
            bulkOperation = false;
        }
        fireUpdateCompletedEvent();
    }

    /**
     * Inserts the item on {@code oldIndex} on {@code newIndex}. May shift items to the left or right to
     * accommodate the insertion.
     */
    public final void insert(int oldIndex, int newIndex) {
        bulkOperation = true;
        try {
            if (newIndex > oldIndex) {
                for (int index = oldIndex; index < newIndex; index++) {
                    swapOperation(index, index + 1);
                }
            } else if (oldIndex > newIndex) {
                for (int index = oldIndex; index > newIndex; index--) {
                    swapOperation(index, index - 1);
                }
            }
        } finally {
            bulkOperation = false;
        }
        fireUpdateCompletedEvent();
    }

    /**
     * Swaps the items on {@code firstIndex} and {@code secondIndex}.
     */
    private void swapOperation(int firstIndex, int secondIndex) {
        checkArgument(firstIndex >= 0 && firstIndex < capacity, "firstIndex out of range");
        checkArgument(secondIndex >= 0 && secondIndex < capacity, "secondIndex out of range");

        Item itemOld = items[firstIndex];
        Item itemNew = items[secondIndex];

        items[firstIndex] = itemNew;
        items[secondIndex] = itemOld;

        fireUpdateEvent(itemOld, items[firstIndex], firstIndex);
        fireUpdateEvent(itemNew, items[secondIndex], secondIndex);
    }

    /**
     * Shifts all items to the left.
     */
    public final void shift() { /* TODO: fire events? */
        Item[] newItems = new Item[capacity];
        int newIndex = 0;

        for (Item item : items) {
            if (item == null) {
                continue;
            }
            newItems[newIndex++] = item;
        }

        setItems(newItems);
    }

    /**
     * Sets the backing array to {@code newItems} (shallow copy).
     */
    public final void setItems(Item[] newItems) { /* TODO: fire events? */
        checkArgument(newItems.length <= capacity, "newItems.length must be <= capacity");

        System.arraycopy(newItems, 0, items, 0, capacity);
    }

    /**
     * Sets the backing array to {@code newItems} (deep copy).
     */
    public final void setItems(IndexedItem[] newItems) { /* TODO: fire events? */
        Arrays.fill(items, null);
        size = 0;
        for (IndexedItem item : newItems) {
            items[item.getIndex()] = new Item(item.getId(), item.getAmount());
            size++;
        }
    }

    /**
     * Returns a shallow copy of the array of items.
     */
    public final Item[] toArray() {
        return Arrays.copyOf(items, items.length);
    }

    /**
     * Returns the backing array as an array of indexed items.
     */
    public final IndexedItem[] toIndexedArray() {
        List<IndexedItem> indexedItems = new LinkedList<>();
        for (int index = 0; index < capacity; index++) {
            Item item = items[index];
            if (item == null) {
                continue;
            }
            indexedItems.add(new IndexedItem(index, item));
        }
        return Iterables.toArray(indexedItems, IndexedItem.class);
    }

    /**
     * Sets {@code index} to {@code item}.
     */
    public final void set(int index, Item item) {
        boolean indexFree = items[index] == null;
        boolean removingItem = item == null;

        if (indexFree && !removingItem) {
            size++;
        } else if (!indexFree && removingItem) {
            size--;
        }

        Item oldItem = items[index];
        items[index] = item;

        fireUpdateEvent(oldItem, items[index], index);
    }

    /**
     * Retrieves the item at {@code index}.
     */
    public final Optional<Item> retrieve(int index) {
        if (index == -1 || index >= items.length)
            return Optional.empty();
        return Optional.ofNullable(items[index]);
    }

    /**
     * Gets the item at {@code index}.
     */
    public final Item get(int index) {
        return retrieve(index).orElse(null);
    }

    /**
     * Determines if {@code index} is occupied.
     */
    public final boolean occupied(int index) {
        return retrieve(index).isPresent();
    }

    /**
     * Determines if all {@code indexes} are occupied.
     */
    public final boolean allOccupied(int... indexes) {
        for (int index : indexes) {
            if (!occupied(index)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Determines if any {@code indexes} are occupied.
     */
    public final boolean anyOccupied(int... indexes) {
        for (int index : indexes) {
            if (occupied(index)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Determines if {@code index} is free.
     */
    public final boolean free(int index) {
        return !occupied(index);
    }

    /**
     * Determines if all {@code indexes} are free.
     */
    public final boolean allFree(int... indexes) {
        return !allOccupied(indexes);
    }

    /**
     * Determines if any {@code indexes} are free.
     */
    public final boolean anyFree(int... indexes) {
        return !anyOccupied(indexes);
    }

    /**
     * Removes all items.
     */
    public final void clear() {
        bulkOperation = true;
        try {
            for (int index = 0; index < capacity; index++) {
                Item old = items[index];
                items[index] = null;

                fireUpdateEvent(old, null, index);
                size--;
            }
        } finally {
            bulkOperation = false;

        }
        fireUpdateCompletedEvent();
    }

    /**
     * Adds a listener. Returns {@code true} if successful.
     */
    public final boolean addListener(ItemContainerListener listener) {
        return listeners.add(listener);
    }

    /**
     * Removes a listener. Returns {@code true} if successful.
     */
    public final boolean removeListener(ItemContainerListener listener) {
        return listeners.remove(listener);
    }

    /**
     * Fires an update or single update event.
     */
    public final void fireUpdateEvent(Item oldItem, Item newItem, int index) {
        Optional<Item> oldOptional = Optional.ofNullable(oldItem);
        Optional<Item> newOptional = Optional.ofNullable(newItem);

        if (firingEvents) {
            for (ItemContainerListener listener : listeners) {
                if (bulkOperation) {
                    listener.onBulkUpdate(this, oldOptional, newOptional, index);
                } else {
                    listener.onSingleUpdate(this, oldOptional, newOptional, index);
                }
            }
        }
    }

    /**
     * Fires a bulk update completed event.
     */
    public final void fireUpdateCompletedEvent() {
        if (firingEvents) {
            listeners.forEach(listener -> listener.onBulkUpdateCompleted(this));
        }
    }

    /**
     * Fires a capacity exceeded event.
     */
    public final void fireCapacityExceededEvent() {
        if (firingEvents) {
            listeners.forEach(listener -> listener.onCapacityExceeded(this));
        }
    }

    /**
     * @return {@code true} if events are being fired, {@code false} otherwise.
     */
    public boolean isFiringEvents() {
        return firingEvents;
    }

    /**
     * Sets the value for {@link #firingEvents}.
     */
    public void setFiringEvents(boolean firingEvents) {
        this.firingEvents = firingEvents;
    }

    /**
     * @return The size.
     */
    public final int getSize() {
        return size;
    }

    /**
     * @return The capacity.
     */
    public final int getCapacity() {
        return capacity;
    }

    /**
     * @return The stack policy.
     */
    public final StackPolicy getPolicy() {
        return policy;
    }
}
