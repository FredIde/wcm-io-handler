/*
 * #%L
 * wcm.io
 * %%
 * Copyright (C) 2014 wcm.io
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package io.wcm.handler.url.suffix;

import static io.wcm.handler.url.suffix.impl.UrlSuffixUtil.KEY_VALUE_DELIMITER;
import static io.wcm.handler.url.suffix.impl.UrlSuffixUtil.decodeKey;
import static io.wcm.handler.url.suffix.impl.UrlSuffixUtil.decodeResourcePathPart;
import static io.wcm.handler.url.suffix.impl.UrlSuffixUtil.decodeValue;
import static io.wcm.handler.url.suffix.impl.UrlSuffixUtil.splitSuffix;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.osgi.annotation.versioning.ProviderType;

import com.day.cq.commons.Filter;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.PageManager;
import com.google.common.base.Function;
import com.google.common.collect.Lists;

/**
 * Parses suffixes from Sling URLs build with {@link SuffixBuilder}.
 */
@ProviderType
public final class SuffixParser {

  private final SlingHttpServletRequest request;

  /**
   * Create a {@link SuffixParser} with the default {@link SuffixStateKeepingStrategy} (which discards all existing
   * suffix state when constructing a new suffix)
   * @param request Sling request
   */
  public SuffixParser(SlingHttpServletRequest request) {
    this.request = request;
  }

  /**
   * Create a {@link SuffixParser} that keeps only the suffix parts matched by the given filter when constructing
   * a new suffix
   * @param request Sling request
   * @param suffixPartFilter the filter that is called for each suffix part
   */
  // TODO: Public wcm.io API should not depend on com.day.cq.commons.Filter - create a own interface instead
  public SuffixParser(SlingHttpServletRequest request, Filter<String> suffixPartFilter) {
    this.request = request;
  }

  /**
   * Extract the value of a named suffix part from this request's suffix
   * @param key key of the suffix part
   * @param clazz Type expected for return value.
   *          Only String, Boolean, Integer, Long are supported.
   * @param <T> Parameter type.
   * @return the value of that named parameter (or the default value if not used)
   */
  @SuppressWarnings("unchecked")
  public <T> T get(String key, Class<T> clazz) {
    if (clazz == String.class) {
      return (T)getString(key, (String)null);
    }
    if (clazz == Boolean.class) {
      return (T)(Boolean)getBoolean(key, false);
    }
    if (clazz == Integer.class) {
      return (T)(Integer)getInt(key, 0);
    }
    if (clazz == Long.class) {
      return (T)(Long)getLong(key, 0L);
    }
    throw new IllegalArgumentException("Unsupported type: " + clazz.getName());
  }

  /**
   * Extract the value of a named suffix part from this request's suffix
   * @param key key of the suffix part
   * @param defaultValue the default value to return if suffix part not set.
   *          Only String, Boolean, Integer, Long are supported.
   * @param <T> Parameter type.
   * @return the value of that named parameter (or the default value if not used)
   */
  @SuppressWarnings("unchecked")
  public <T> T get(String key, T defaultValue) {
    if (defaultValue instanceof String || defaultValue == null) {
      return (T)getString(key, (String)defaultValue);
    }
    if (defaultValue instanceof Boolean) {
      return (T)(Boolean)getBoolean(key, (Boolean)defaultValue);
    }
    if (defaultValue instanceof Integer) {
      return (T)(Integer)getInt(key, (Integer)defaultValue);
    }
    if (defaultValue instanceof Long) {
      return (T)(Long)getLong(key, (Long)defaultValue);
    }
    throw new IllegalArgumentException("Unsupported type: " + defaultValue.getClass().getName());
  }

  private String getString(String key, String defaultValue) {
    String value = findSuffixPartByKey(key);
    if (value == null) {
      return defaultValue;
    }
    return value;
  }

  private boolean getBoolean(String key, boolean defaultValue) {
    String value = findSuffixPartByKey(key);
    if (value == null) {
      return defaultValue;
    }

    // value must match exactly "true" or "false"
    if ("true".equals(value)) {
      return true;
    }
    if ("false".equals(value)) {
      return false;
    }

    // invalid boolean value - return default
    return defaultValue;
  }

  private int getInt(String key, int defaultValue) {
    String value = findSuffixPartByKey(key);
    if (value == null) {
      return defaultValue;
    }
    return NumberUtils.toInt(value, defaultValue);
  }

  private long getLong(String key, long defaultValue) {
    String value = findSuffixPartByKey(key);
    if (value == null) {
      return defaultValue;
    }
    return NumberUtils.toLong(value, defaultValue);
  }

  /**
   * Extract the value of a named suffix part from this request's suffix
   * @param key key of the suffix part
   * @return the value of that named parameter (or null if not used)
   */
  private String findSuffixPartByKey(String key) {
    for (String part : splitSuffix(request.getRequestPathInfo().getSuffix())) {
      if (part.indexOf(KEY_VALUE_DELIMITER) >= 0) {
        String partKey = decodeKey(part);
        if (partKey.equals(key)) {
          String value = decodeValue(part);
          return value;
        }
      }
    }
    return null;
  }

  /**
   * Get a resource within the current page by interpreting the suffix as a JCR path relative to this page's jcr:content
   * node
   * @return the Resource or null if no such resource exists
   */
  public Resource getResource() {
    return getResource((Filter<Resource>)null, (Resource)null);
  }

  /**
   * Parse the suffix as resource paths and return the first resource that exists
   * @param baseResource the suffix path is relative to this resource path (null for current page's jcr:content node)
   * @return the resource or null if no such resource was selected by suffix
   */
  public Resource getResource(Resource baseResource) {
    return getResource((Filter<Resource>)null, baseResource);
  }

  /**
   * Parse the suffix as resource paths, return the first resource from the suffix (relativ to the current page's
   * content) that matches the given filter.
   * @param filter a filter that selects only the resource you're interested in.
   * @return the resource or null if no such resource was selected by suffix
   */
  public Resource getResource(Filter<Resource> filter) {
    return getResource(filter, (Resource)null);
  }

  /**
   * Get the first item returned by {@link #getResources(Filter, Resource)} or null if list is empty
   * @param filter the resource filter
   * @param baseResource the suffix path is relative to this resource path (null for current page's jcr:content node)
   * @return the first {@link Resource} or null
   */
  public Resource getResource(Filter<Resource> filter, Resource baseResource) {
    List<Resource> suffixResources = getResources(filter, baseResource);
    if (suffixResources.isEmpty()) {
      return null;
    }
    else {
      return suffixResources.get(0);
    }
  }

  /**
   * Get the resources within the current page selected in the suffix of the URL
   * @return a list containing the Resources
   */
  public List<Resource> getResources() {
    return getResources((Filter<Resource>)null, (Resource)null);
  }

  /**
   * Get the resources selected in the suffix of the URL
   * @param baseResource the suffix path is relative to this resource path (null for current page's jcr:content node)
   * @return a list containing the Resources
   */
  public List<Resource> getResources(Resource baseResource) {
    return getResources((Filter<Resource>)null, baseResource);
  }

  /**
   * Get the resources selected in the suffix of the URL
   * @param filter optional filter to select only specific resources
   * @return a list containing the Resources
   */
  public List<Resource> getResources(Filter<Resource> filter) {
    return getResources(filter, (Resource)null);
  }

  /**
   * Get the resources selected in the suffix of the URL
   * @param filter optional filter to select only specific resources
   * @param baseResource the suffix path is relative to this resource path (null for current page's jcr:content node)
   * @return a list containing the Resources
   */
  public List<Resource> getResources(Filter<Resource> filter, Resource baseResource) {

    // resolve base path or fallback to current page's content if not specified
    Resource baseResourceToUse = baseResource;
    if (baseResourceToUse == null) {
      PageManager pageManager = request.getResourceResolver().adaptTo(PageManager.class);
      Page currentPage = pageManager.getContainingPage(request.getResource());
      if (currentPage != null) {
        baseResourceToUse = currentPage.getContentResource();
      }
      else {
        baseResourceToUse = request.getResource();
      }
    }
    return getResourcesWithBaseResource(filter, baseResourceToUse);
  }

  /**
   * Parse the suffix as page paths and return the first page that exists with a page path relative
   * to the current page path.
   * @return the page or null if no such page was selected by suffix
   */
  public Page getPage() {
    return getPage((Filter<Page>)null, (Page)null);
  }

  /**
   * Parse the suffix as page paths and return the first page that exists.
   * @param basePage the suffix page is relative to this page path (null for current page)
   * @return the page or null if no such page was selected by suffix
   */
  public Page getPage(Page basePage) {
    return getPage((Filter<Page>)null, basePage);
  }

  /**
   * Parse the suffix as page paths, return the first page from the suffix (relativ to the current page) that matches the given filter.
   *
   * @param filter a filter that selects only the page you're interested in.
   * @return the page or null if no such page was selected by suffix
   */
  public Page getPage(Filter<Page> filter) {
    return getPage(filter, (Page)null);
  }

  /**
   * Get the first item returned by {@link #getPages(Filter, Page)} or null if list is empty
   *
   * @param filter the resource filter
   * @param basePage the suffix path is relative to this page path (null for current page)
   * @return the first {@link Page} or null
   */
  public Page getPage(Filter<Page> filter, Page basePage) {
    List<Page> suffixPages = getPages(filter, basePage);
    if (suffixPages.isEmpty()) {
      return null;
    }
    else {
      return suffixPages.get(0);
    }
  }

  /**
   * Get the pages selected in the suffix of the URL with page paths relative
   * to the current page path.
   * @return a list containing the Pages
   */
  public List<Page> getPages() {
    return getPages((Filter<Page>)null, (Page)null);
  }

  /**
   * Get the pages selected in the suffix of the URL with page paths relative
   * to the current page path.
   * @param filter optional filter to select only specific pages
   * @return a list containing the Pages
   */
  public List<Page> getPages(Filter<Page> filter) {
    return getPages(filter, (Page)null);
  }

  /**
   * Get the pages selected in the suffix of the URL
   * @param filter optional filter to select only specific pages
   * @param basePage the suffix path is relative to this page path (null for current page)
   * @return a list containing the Pages
   */
  public List<Page> getPages(final Filter<Page> filter, final Page basePage) {
    Resource baseResourceToUse = null;

    // detect pages page to use
    if (basePage == null) {
      PageManager pageManager = request.getResourceResolver().adaptTo(PageManager.class);
      Page currentPage = pageManager.getContainingPage(request.getResource());
      if (currentPage != null) {
        baseResourceToUse = currentPage.adaptTo(Resource.class);
      }
    }
    else {
      baseResourceToUse = basePage.adaptTo(Resource.class);
    }

    // filter pages (as resources)
    Filter<Resource> resourceFilter = new Filter<Resource>() {
      @Override
      public boolean includes(Resource resource) {
        Page page = resource.adaptTo(Page.class);
        if (page == null) {
          return false;
        }
        if (filter == null) {
          return true;
        }
        return filter.includes(page);
      }
    };
    List<Resource> resources = getResourcesWithBaseResource(resourceFilter, baseResourceToUse);

    // convert resources back to pages
    return Lists.transform(resources, new Function<Resource, Page>() {
      @Override
      public Page apply(Resource resource) {
        return resource.adaptTo(Page.class);
      }
    });
  }

  private List<Resource> getResourcesWithBaseResource(Filter<Resource> filter, Resource baseResource) {
    // split the suffix to extract the paths of the selected components
    String[] suffixParts = splitSuffix(request.getRequestPathInfo().getSuffix());

    // iterate over all parts and gather those resources
    List<Resource> selectedResources = new ArrayList<>();
    for (String path : suffixParts) {

      // if path contains the key/value-delimiter then don't try to resolve it as a content path
      if (StringUtils.contains(path, KEY_VALUE_DELIMITER)) {
        continue;
      }

      String decodedPath = decodeResourcePathPart(path);

      // lookup the resource specified by the path (which is relative to the current page's content resource)
      Resource resource = request.getResourceResolver().getResource(baseResource, decodedPath);
      if (resource == null) {
        // no resource found with given path, continue with next path in suffix
        continue;
      }

      // if a filter is given - check
      if (filter == null || filter.includes(resource)) {
        selectedResources.add(resource);
      }

    }

    return selectedResources;
  }

}
