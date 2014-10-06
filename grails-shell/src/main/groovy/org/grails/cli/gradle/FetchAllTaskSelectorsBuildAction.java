package org.grails.cli.gradle;
import java.io.File;
import java.io.Serializable;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.gradle.tooling.BuildAction;
import org.gradle.tooling.BuildController;
import org.gradle.tooling.model.Task;
import org.gradle.tooling.model.TaskSelector;
import org.gradle.tooling.model.gradle.BasicGradleProject;
import org.gradle.tooling.model.gradle.BuildInvocations;
import org.grails.cli.gradle.FetchAllTaskSelectorsBuildAction.AllTasksModel;

public class FetchAllTaskSelectorsBuildAction implements BuildAction<AllTasksModel> {
    private static final long serialVersionUID = 1L;
    private final String currentProjectPath;
    
    public FetchAllTaskSelectorsBuildAction(File currentProjectDir) {
        this.currentProjectPath = currentProjectDir.getAbsolutePath();
    }

    public AllTasksModel execute(BuildController controller) {
        AllTasksModel model = new AllTasksModel();
        Map<String, Set<String>> allTaskSelectors = new LinkedHashMap<String, Set<String>>();
        model.allTaskSelectors = allTaskSelectors;
        Map<String, Set<String>> allTasks = new LinkedHashMap<String, Set<String>>();
        model.allTasks = allTasks;
        Map<String, String> projectPaths = new HashMap<String, String>();
        model.projectPaths = projectPaths;
        for (BasicGradleProject project: controller.getBuildModel().getProjects()) {
            BuildInvocations entryPointsForProject = controller.getModel(project, BuildInvocations.class);
            Set<String> selectorNames = new LinkedHashSet<String>();
            for (TaskSelector selector : entryPointsForProject.getTaskSelectors()) {
                selectorNames.add(selector.getName());
            }
            allTaskSelectors.put(project.getName(), selectorNames);
            
            Set<String> taskNames = new LinkedHashSet<String>();
            for (Task task : entryPointsForProject.getTasks()) {
                taskNames.add(task.getName());
            }
            allTasks.put(project.getName(), taskNames);
            
            projectPaths.put(project.getName(), project.getPath());
            if(project.getProjectDirectory().getAbsolutePath().equals(currentProjectPath)) {
                model.currentProject = project.getName();
            }
        }
        return model;
    }
    
    public static class AllTasksModel implements Serializable {
        private static final long serialVersionUID = 1L;
        public Map<String, Set<String>> allTasks;
        public Map<String, Set<String>> allTaskSelectors;
        public Map<String, String> projectPaths;
        public String currentProject;
    }
}