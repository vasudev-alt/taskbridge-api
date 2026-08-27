package org.example.projects;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class ProjectService {

    @Autowired
    private ProjectRepository projectRepository;

    public Project createProject(Project project) {
        return projectRepository.save(project);
    }

    public Optional<Project> updateProjectStatus(UUID projectId, String status) {
        Optional<Project> project = projectRepository.findById(projectId);
        project.ifPresent(p -> {
            p.setStatus(status);
            projectRepository.save(p);
        });
        return project;
    }

    public List<Project> getProjectsByTeam(String team) {
        return projectRepository.findByTeam(team);
    }

    public void deleteProject(UUID projectId) {
        projectRepository.deleteById(projectId);
    }
}
