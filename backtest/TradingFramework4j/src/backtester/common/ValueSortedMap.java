package backtester.common;

import java.util.AbstractList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.AbstractMap.SimpleImmutableEntry;

/**
 * A sorted map sorted by values instead of keys. This class is not thread-safe.
 */
public class ValueSortedMap<K, V extends Comparable<? super V>> implements Map<K, V> {
	private long insertionCounter = 0;

	private final Map<K, ValueSortedMapValue<V>> innerHashMap = new HashMap<K, ValueSortedMapValue<V>>();
	private final SortedSet<ValueSortedMapValue<V>> sortedSet = new TreeSet<ValueSortedMapValue<V>>();

	private final List<V> valuesView = new AbstractList<V>() {
		@Override
		public Iterator<V> iterator() {
			return new Iterator<V>() {
				private final Iterator<ValueSortedMapValue<V>> i = sortedSet.iterator();

				@Override
				public boolean hasNext() {
					return i.hasNext();
				}

				@Override
				public V next() {
					return i.next().getHeldValue();
				}

				@Override
				public void remove() {
					throw new UnsupportedOperationException();
				}
			};
		}

		@Override
		public int size() {
			return ValueSortedMap.this.size();
		}

		@Override
		public V get(final int index) {
			if (index >= size()) {
				throw new IndexOutOfBoundsException(index + " greater than or equal to" + size());
			}
			final Iterator<V> iterator = iterator();
			V item = iterator.next();
			for (int i = 0; i < index; i++) {
				item = iterator.next();
			}

			return item;
		}
	};

	public void clear() {
		innerHashMap.clear();
		sortedSet.clear();
	}

	public boolean containsKey(final Object key) {
		return innerHashMap.containsKey(key);
	}

	public boolean containsValue(final Object value) {
		return valuesView.contains(value);
	}

	public V get(final Object key) {
		final ValueSortedMapValue<V> value = innerHashMap.get(key);
		return value == null ? null : value.getHeldValue();
	}

	public boolean isEmpty() {
		return innerHashMap.isEmpty();
	}
	
	public V put(final K key, final V value) {
		if (value == null) {
			throw new IllegalArgumentException("Null values not allowed");
		}

		final ValueSortedMapValue<V> oldValue = removeImpl(key);

		final ValueSortedMapValue<V> wrappedValue;
		if (oldValue != null) {
			wrappedValue = new ValueSortedMapValue<V>(oldValue.insertionOrder, value);
		} else {
			wrappedValue = new ValueSortedMapValue<V>(insertionCounter++, value);
		}
		innerHashMap.put(key, wrappedValue);
		sortedSet.add(wrappedValue);
		return oldValue == null ? null : oldValue.getHeldValue();
	}

	public void putAll(final Map<? extends K, ? extends V> m) {
		for (final Entry<? extends K, ? extends V> entry : m.entrySet()) {
			put(entry.getKey(), entry.getValue());
		}
	}

	public V remove(final Object key) {
		final ValueSortedMapValue<V> value = removeImpl(key);
		if (value != null) {
			return value.getHeldValue();
		}

		return null;
	}

	private ValueSortedMapValue<V> removeImpl(final Object key) {
		final ValueSortedMapValue<V> value = innerHashMap.remove(key);
		if (value != null) {
			sortedSet.remove(value);
		}

		return value;
	}

	public int size() {
		return innerHashMap.size();
	}

	public Set<Entry<K, V>> entrySet() {
		final Set<Entry<K, V>> valueSet = new HashSet<Entry<K, V>>();
		for (final Entry<K, ValueSortedMapValue<V>> entry : innerHashMap.entrySet()) {
			valueSet.add(new SimpleImmutableEntry<K, V>(entry.getKey(), entry.getValue().getHeldValue()));
		}

		return valueSet;
	}

	public Set<K> keySet() {
		return innerHashMap.keySet();
	}

	/**
	 * Returns a sorted list of values, first sorted by their comparator, then by their insertion time
	 */
	public List<V> values() {
		return valuesView;
	}

	private static class ValueSortedMapValue<HELD_CLASS extends Comparable<? super HELD_CLASS>> implements
			Comparable<ValueSortedMapValue<HELD_CLASS>> {
		private final Long insertionOrder;
		private final HELD_CLASS heldValue;

		public ValueSortedMapValue(final Long insertionOrder, final HELD_CLASS heldValue) {
			this.insertionOrder = insertionOrder;
			this.heldValue = heldValue;
		}

		@Override
		public int compareTo(final ValueSortedMapValue<HELD_CLASS> o) {
			final int basicCompare = heldValue.compareTo(o.heldValue);
			if (basicCompare == 0) {
				return insertionOrder.compareTo(o.insertionOrder);
			}
			return basicCompare;
		}

		protected HELD_CLASS getHeldValue() {
			return heldValue;
		}

		@Override
		public String toString() {
			return heldValue.toString();
		}
	}

	@Override
	public String toString() {
		return innerHashMap.toString();
	}
}
