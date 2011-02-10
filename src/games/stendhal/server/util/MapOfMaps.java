/***************************************************************************
 *                   (C) Copyright 2010-2011 - Stendhal                    *
 ***************************************************************************
 ***************************************************************************
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *                                                                         *
 ***************************************************************************/
package games.stendhal.server.util;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * A Map which contains maps
 *
 * @author hendrik
 * @param <K> type of primary key
 * @param <V> type of secondary key
 * @param <W> type of value
 */
public class MapOfMaps<K, V, W> implements Map<K, Map<V, W>> {
        private Map<K, Map<V, W>> maps = new HashMap<K, Map<V, W>>();

        public void clear() {
                maps.clear();
        }

        public boolean containsKey(Object key) {
                return maps.containsKey(key);
        }

        public boolean containsValue(Object value) {
                for (Map<V, W> map : maps.values()) {
                        if (map.containsValue(value)) {
                                return true;
                        }
                }
                return false;
        }

        public Set<java.util.Map.Entry<K, Map<V, W>>> entrySet() {
                return maps.entrySet();
        }

        public Map<V, W> get(Object key) {
                return maps.get(key);
        }

        public W get(Object key, Object subKey) {
                Map<V, W> temp = maps.get(key);
                if (temp == null) {
                        return null;
                }
                return temp.get(subKey);
        }

        public boolean isEmpty() {
                return maps.isEmpty();
        }

        public Set<K> keySet() {
                return maps.keySet();
        }

        public Map<V, W> put(K key, Map<V, W> value) {
                return maps.put(key, value);
        }

        public void putAll(Map<? extends K, ? extends Map<V, W>> m) {
                maps.putAll(m);
        }

        public Map<V, W> remove(Object key) {
                return maps.remove(key);
        }

        public int size() {
                return maps.size();
        }

        public Collection<Map<V, W>> values() {
                return maps.values();
        }

        @Override
        public String toString() {
                return maps.toString();
        }

        /**
         * adds an entry to the map
         *
         * @param key    primary key
         * @param subKey secondary key
         * @param value  value
         * @return old value
         */
        public W put(K key, V subKey, W value) {
                Map<V, W> map = maps.get(key);
                if (map == null) {
                        map = new HashMap<V, W>();
                        maps.put(key, map);
                }
                return map.put(subKey, value);
        }

}