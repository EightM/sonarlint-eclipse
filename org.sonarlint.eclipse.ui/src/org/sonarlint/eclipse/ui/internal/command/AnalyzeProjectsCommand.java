/*
 * SonarLint for Eclipse
 * Copyright (C) 2015 SonarSource
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
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonarlint.eclipse.ui.internal.command;

import java.util.List;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.sonarlint.eclipse.core.internal.SonarLintNature;
import org.sonarlint.eclipse.core.internal.jobs.AnalyzeProjectJob;
import org.sonarlint.eclipse.core.internal.jobs.AnalyzeProjectRequest;
import org.sonarlint.eclipse.ui.internal.SonarLintUiPlugin;
import org.sonarlint.eclipse.ui.internal.views.issues.IssuesView;

public class AnalyzeProjectsCommand extends AbstractProjectsCommand {

  @Override
  public Object execute(ExecutionEvent event) throws ExecutionException {

    List<IProject> selectedProjects = findSelectedProjects(event);
    if (selectedProjects.isEmpty()) {
      findProjectOfSelectedEditor(event, selectedProjects);
    }

    if (!selectedProjects.isEmpty()) {
      runAnalysisJob(selectedProjects);
    }

    return null;
  }

  private void runAnalysisJob(List<IProject> selectedProjects) {
    for (IProject project : selectedProjects) {
      if (!SonarLintNature.hasSonarLintNature(project)) {
        break;
      }
      AnalyzeProjectJob job = new AnalyzeProjectJob(new AnalyzeProjectRequest(project, null));
      job.schedule();
      showIssuesViewAfterJobSuccess(job);
    }
  }

  protected void showIssuesViewAfterJobSuccess(Job job) {
    // Display issues view after analysis is completed
    job.addJobChangeListener(new JobChangeAdapter() {
      @Override
      public void done(IJobChangeEvent event) {
        if (Status.OK_STATUS == event.getResult()) {
          Display.getDefault().asyncExec(new Runnable() {
            @Override
            public void run() {
              IWorkbenchWindow iw = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
              try {
                iw.getActivePage().showView(IssuesView.ID, null, IWorkbenchPage.VIEW_VISIBLE);
              } catch (PartInitException e) {
                SonarLintUiPlugin.getDefault().getLog().log(new Status(Status.ERROR, SonarLintUiPlugin.PLUGIN_ID, Status.OK, "Unable to open Issues View", e));
              }
            }
          });
        }
      }
    });
    job.schedule();
  }

}