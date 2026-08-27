package org.example.projects;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/projects")
public class ProjectController {

    private final ProjectService service;

    @Autowired
    public ProjectController(ProjectService service) {
        this.service = service;
    }

    @PostMapping
    public Project createProject(@RequestBody Project project) {
        return service.createProject(project);
    }

    @PatchMapping("/{id}/status")
    public Optional<Project> updateProjectStatus(@PathVariable Long id, @RequestParam String status) {
        return service.updateProjectStatus(id, status);
    }

    @GetMapping
    public List<Project> getProjectsByTeam(@RequestParam String teamId) {
        return service.getProjectsByTeam(teamId);
    }

    @DeleteMapping("/{id}")
    public void deleteProject(@PathVariable Long id) {
        service.deleteProject(id);
    }
}
