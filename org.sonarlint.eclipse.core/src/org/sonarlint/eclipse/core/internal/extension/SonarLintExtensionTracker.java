/*
 * SonarLint for Eclipse
 * Copyright (C) 2015-2017 SonarSource SA
 * sonarlint@sonarsource.com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonarlint.eclipse.core.internal.extension;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.dynamichelpers.ExtensionTracker;
import org.eclipse.core.runtime.dynamichelpers.IExtensionChangeHandler;
import org.eclipse.core.runtime.dynamichelpers.IExtensionTracker;
import org.eclipse.core.runtime.dynamichelpers.IFilter;
import org.sonarlint.eclipse.core.SonarLintLogger;
import org.sonarlint.eclipse.core.configurator.ProjectConfigurator;
import org.sonarlint.eclipse.core.resource.ISonarLintFileFilter;
import org.sonarlint.eclipse.core.resource.ISonarLintProjectsProvider;

public class SonarLintExtensionTracker implements IExtensionChangeHandler {

  private static final String CONFIGURATOR_EP = "org.sonarlint.eclipse.core.projectConfigurators"; //$NON-NLS-1$
  private static final String PROJECTSPROVIDER_EP = "org.sonarlint.eclipse.core.projectsProvider"; //$NON-NLS-1$
  private static final String FILEFILTER_EP = "org.sonarlint.eclipse.core.fileFilter"; //$NON-NLS-1$
  private static final String ATTR_CLASS = "class"; //$NON-NLS-1$

  private ExtensionTracker tracker;
  private Collection<ProjectConfigurator> configurators = new ArrayList<>();
  private Collection<ISonarLintProjectsProvider> projectsProviders = new ArrayList<>();
  private Collection<ISonarLintFileFilter> fileFilters = new ArrayList<>();

  public void start() {
    IExtensionRegistry reg = Platform.getExtensionRegistry();
    tracker = new ExtensionTracker(reg);
    IExtensionPoint[] allEps = new IExtensionPoint[] {
      reg.getExtensionPoint(CONFIGURATOR_EP),
      reg.getExtensionPoint(PROJECTSPROVIDER_EP),
      reg.getExtensionPoint(FILEFILTER_EP)};
    IFilter filter = ExtensionTracker.createExtensionPointFilter(allEps);
    tracker.registerHandler(this, filter);
    for (IExtensionPoint ep : allEps) {
      for (IExtension ext : ep.getExtensions()) {
        addExtension(tracker, ext);
      }
    }
  }

  public void close() {
    if (tracker != null) {
      tracker.close();
      tracker = null;
    }
  }

  @Override
  public void addExtension(IExtensionTracker tracker, IExtension extension) {
    IConfigurationElement[] configs = extension.getConfigurationElements();
    for (final IConfigurationElement element : configs) {
      try {
        Object instance;
        switch (extension.getExtensionPointUniqueIdentifier()) {
          case CONFIGURATOR_EP:
            instance = addConfigurator(element);
            break;
          case PROJECTSPROVIDER_EP:
            instance = addProjectsProvider(element);
            break;
          case FILEFILTER_EP:
            instance = addFileFilter(element);
            break;
          default:
            throw new IllegalStateException("Unexpected extension point: " + extension.getExtensionPointUniqueIdentifier());
        }

        // register association between object and extension with the tracker
        tracker.registerObject(extension, instance, IExtensionTracker.REF_WEAK);
      } catch (CoreException e) {
        SonarLintLogger.get().error("Unable to load one SonarLint extension", e);
      }
    }
  }

  private Object addConfigurator(IConfigurationElement element) throws CoreException {
    ProjectConfigurator instance = (ProjectConfigurator) element.createExecutableExtension(ATTR_CLASS);
    configurators.add(instance);
    return instance;
  }

  private Object addProjectsProvider(IConfigurationElement element) throws CoreException {
    ISonarLintProjectsProvider instance = (ISonarLintProjectsProvider) element.createExecutableExtension(ATTR_CLASS);
    projectsProviders.add(instance);
    return instance;
  }

  private Object addFileFilter(IConfigurationElement element) throws CoreException {
    ISonarLintFileFilter instance = (ISonarLintFileFilter) element.createExecutableExtension(ATTR_CLASS);
    fileFilters.add(instance);
    return instance;
  }

  @Override
  public void removeExtension(IExtension extension, Object[] objects) {
    // stop using objects associated with the removed extension
    switch (extension.getExtensionPointUniqueIdentifier()) {
      case CONFIGURATOR_EP:
        configurators.removeAll(Arrays.asList(objects));
        break;
      case PROJECTSPROVIDER_EP:
        projectsProviders.removeAll(Arrays.asList(objects));
        break;
      case FILEFILTER_EP:
        fileFilters.removeAll(Arrays.asList(objects));
        break;
      default:
        throw new IllegalStateException("Unexpected extension point: " + extension.getExtensionPointUniqueIdentifier());
    }

  }

  public Collection<ProjectConfigurator> getConfigurators() {
    return configurators;
  }

  public Collection<ISonarLintProjectsProvider> getProjectsProviders() {
    return projectsProviders;
  }

  public Collection<ISonarLintFileFilter> getFileFilters() {
    return fileFilters;
  }

}
