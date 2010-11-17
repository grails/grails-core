package org.codehaus.groovy.grails.web.mapping;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.commons.lang.StringUtils;

import com.googlecode.concurrentlinkedhashmap.ConcurrentLinkedHashMap;
import com.googlecode.concurrentlinkedhashmap.Weigher;

/**
 * Implements caching layer for UrlCreator
 * 
 * The "weight" of the cache is the estimated number of characters all cache entries will consume in memory.
 * The estimate is not accurate. It's just used as a hard limit for limiting the cache size.
 * 
 * You can tune the maximum weight of the cache by setting "grails.urlcreator.cache.maxsize" in Config.groovy.
 * The default value is 160000 .
 *  
 * @author Lari Hotari
 * @since 1.3.5
 */
public class UrlCreatorCache {
	private final ConcurrentMap<ReverseMappingKey, CachingUrlCreator> cacheMap;
    private enum CachingUrlCreatorWeigher implements Weigher<CachingUrlCreator> {
    	INSTANCE;
    	public int weightOf(CachingUrlCreator cachingUrlCreator) {
    		return cachingUrlCreator.weight() + 1;
    	}
    }	
	
	public UrlCreatorCache(int maxSize) {
		cacheMap = new ConcurrentLinkedHashMap.Builder<ReverseMappingKey, CachingUrlCreator>()
        .maximumWeightedCapacity(maxSize)
        .weigher(CachingUrlCreatorWeigher.INSTANCE)
        .build();
	}
	
	public void clear() {
		cacheMap.clear();
	}
	
	public ReverseMappingKey createKey(String controller, String action, Map params) {
		return new ReverseMappingKey(controller, action, params);
	}
	
	public UrlCreator lookup(ReverseMappingKey key) {
		return cacheMap.get(key);
	}
	
	public UrlCreator putAndDecorate(ReverseMappingKey key, UrlCreator delegate) {
		CachingUrlCreator cachingUrlCreator=new CachingUrlCreator(delegate, key.weight() * 2);
		CachingUrlCreator prevCachingUrlCreator=cacheMap.putIfAbsent(key, cachingUrlCreator);
		if(prevCachingUrlCreator != null) {
			return prevCachingUrlCreator;
		} else {
			return cachingUrlCreator;
		}
	}

	private class CachingUrlCreator implements UrlCreator {
		private UrlCreator delegate;
		private ConcurrentHashMap<UrlCreatorKey, String> cache=new ConcurrentHashMap<UrlCreatorKey, String>();
		private final int weight;
		
		public CachingUrlCreator(UrlCreator delegate, int weight) {
			this.delegate=delegate;
			this.weight=weight;
		}

		public int weight() {
			return weight;
		}

		public String createRelativeURL(String controller, String action,
				Map parameterValues, String encoding, String fragment) {
			UrlCreatorKey key=new UrlCreatorKey(controller, action, parameterValues, encoding, fragment, 0);
			String url=cache.get(key);
			if(url==null) {
				url=delegate.createRelativeURL(controller, action, parameterValues,	encoding, fragment);
				cache.put(key, url);
			}
			return url;
		}

		public String createRelativeURL(String controller, String action,
				Map parameterValues, String encoding) {
			UrlCreatorKey key=new UrlCreatorKey(controller, action, parameterValues, encoding, null, 0);
			String url=cache.get(key);
			if(url==null) {
				url=delegate.createRelativeURL(controller, action, parameterValues,	encoding);
				cache.put(key, url);
			}
			return url;
		}

		public String createURL(String controller, String action,
				Map parameterValues, String encoding, String fragment) {
			UrlCreatorKey key=new UrlCreatorKey(controller, action, parameterValues, encoding, fragment, 1);
			String url=cache.get(key);
			if(url==null) {
				url=delegate.createURL(controller, action, parameterValues,	encoding, fragment);
				cache.put(key, url);
			}
			return url;
		}

		public String createURL(String controller, String action,
				Map parameterValues, String encoding) {
			UrlCreatorKey key=new UrlCreatorKey(controller, action, parameterValues, encoding, null, 1);
			String url=cache.get(key);
			if(url==null) {
				url=delegate.createURL(controller, action, parameterValues,	encoding);
				cache.put(key, url);
			}
			return url;
		}

		// don't cache these methods at all
		
		public String createURL(Map parameterValues, String encoding,
				String fragment) {
			return delegate.createURL(parameterValues, encoding, fragment);
		}

		public String createURL(Map parameterValues, String encoding) {
			return delegate.createURL(parameterValues, encoding);
		}
	}
	
	public static class ReverseMappingKey {
		protected final String controller;
		protected final String action;
		protected final String[] paramKeys;
		protected final String[] paramValues;
		
		public ReverseMappingKey(String controller, String action, Map<Object,Object> params) {
			this.controller=controller;
			this.action=action;
			if(params != null) {
				paramKeys=new String[params.size()];
				paramValues=new String[params.size()];
				int i=0;
				for(Map.Entry entry : params.entrySet()) {
					paramKeys[i]=String.valueOf(entry.getKey());
					String value=null;
					if(entry.getValue() instanceof CharSequence) {
						value=String.valueOf(entry.getValue());
					} else if(entry.getValue() instanceof Collection) {
						StringUtils.join((Collection)entry.getValue(), ',');
					} else if (entry.getValue() instanceof Object[]) {
						StringUtils.join((Object[])entry.getValue(), ',');
					} else {
						value=String.valueOf(entry.getValue());
					}
					paramValues[i]=value;
					i++;
				}
			} else {
				paramKeys=new String[0];
				paramValues=new String[0];
			}
		}

		public int weight() {
			int weight=0;
			weight += (controller != null) ? controller.length() : 0;
			weight += (action != null) ? action.length() : 0;
			for(int i=0; i < paramKeys.length;i++) {
				weight += (paramKeys[i] != null) ? paramKeys[i].length() : 0;
			}
			for(int i=0; i < paramValues.length;i++) {
				weight += (paramValues[i] != null) ? paramValues[i].length() : 0;
			}
			return weight;
		}
		
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result
					+ ((action == null) ? 0 : action.hashCode());
			result = prime * result
					+ ((controller == null) ? 0 : controller.hashCode());
			result = prime * result + Arrays.hashCode(paramKeys);
			result = prime * result + Arrays.hashCode(paramValues);
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			ReverseMappingKey other = (ReverseMappingKey) obj;
			if (action == null) {
				if (other.action != null)
					return false;
			} else if (!action.equals(other.action))
				return false;
			if (controller == null) {
				if (other.controller != null)
					return false;
			} else if (!controller.equals(other.controller))
				return false;
			if (!Arrays.equals(paramKeys, other.paramKeys))
				return false;
			if (!Arrays.equals(paramValues, other.paramValues))
				return false;
			return true;
		}

		@Override
		public String toString() {
			return "UrlCreatorCache.ReverseMappingKey [action=" + action + ", controller="
					+ controller + ", paramKeys=" + Arrays.toString(paramKeys)
					+ ", paramValues=" + Arrays.toString(paramValues) + "]";
		}
	}
	
	private static class UrlCreatorKey extends ReverseMappingKey {
		protected final String encoding;
		protected final String fragment;
		protected final int urlType;
		
		public UrlCreatorKey(String controller, String action,
				Map<Object, Object> params, String encoding, String fragment, int urlType) {
			super(controller, action, params);
			this.encoding=(encoding != null) ? encoding.toLowerCase() : null;
			this.fragment=fragment;
			this.urlType=urlType;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = super.hashCode();
			result = prime * result
					+ ((encoding == null) ? 0 : encoding.hashCode());
			result = prime * result
					+ ((fragment == null) ? 0 : fragment.hashCode());
			result = prime * result + urlType;
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (!super.equals(obj))
				return false;
			if (getClass() != obj.getClass())
				return false;
			UrlCreatorKey other = (UrlCreatorKey) obj;
			if (encoding == null) {
				if (other.encoding != null)
					return false;
			} else if (!encoding.equals(other.encoding))
				return false;
			if (fragment == null) {
				if (other.fragment != null)
					return false;
			} else if (!fragment.equals(other.fragment))
				return false;
			if (urlType != other.urlType)
				return false;
			return true;
		}

		@Override
		public String toString() {
			return "UrlCreatorCache.UrlCreatorKey [encoding=" + encoding + ", fragment="
					+ fragment + ", urlType=" + urlType + ", action=" + action
					+ ", controller=" + controller + ", paramKeys="
					+ Arrays.toString(paramKeys) + ", paramValues="
					+ Arrays.toString(paramValues) + "]";
		}
	}
}
