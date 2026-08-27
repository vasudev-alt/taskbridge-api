package org.example.projects;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.audit.AuditEventType;
import org.example.audit.AuditService;
import org.example.notifications.NotificationEventType;
import org.example.notifications.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Enhanced ProjectService with audit logging and notification capabilities.
 * Integrates with AuditService and NotificationService to track all project changes.
 */
@Service
public class ProjectService {

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private AuditService auditService;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ProjectTeamService projectTeamService;

    /**
     * Creates a new project and records audit log + sends notifications.
     *
     * @param project the project to create
     * @param actorUserId the user creating the project
     * @param actorOrganisation the organisation
     * @param actorIpAddress the IP address of the actor
     * @return the created project
     */
    public Project createProject(
            Project project,
            UUID actorUserId,
            String actorOrganisation,
            String actorIpAddress
    ) {
        Project savedProject = projectRepository.save(project);

        // Record audit log
        JsonNode newState = objectMapper.valueToTree(savedProject);
        auditService.recordAuditEvent(
                AuditEventType.PROJECT_CREATED,
                "PROJECT",
                savedProject.getId(),
                actorUserId,
                actorOrganisation,
                actorIpAddress,
                null, // no previous state for creation
                newState
        );

        // Notify team members
        notifyTeamMembers(
                savedProject.getId(),
                NotificationEventType.PROJECT_CREATED,
                "Project '" + savedProject.getName() + "' has been created"
        );

        return savedProject;
    }

    /**
     * Updates project status and records audit log + sends notifications.
     *
     * @param projectId the project ID
     * @param status the new status
     * @param actorUserId the user updating the project
     * @param actorOrganisation the organisation
     * @param actorIpAddress the IP address of the actor
     * @return the updated project
     */
    public Optional<Project> updateProjectStatus(
            UUID projectId,
            String status,
            UUID actorUserId,
            String actorOrganisation,
            String actorIpAddress
    ) {
        Optional<Project> projectOpt = projectRepository.findById(projectId);
        projectOpt.ifPresent(project -> {
            // Capture previous state before update
            JsonNode previousState = objectMapper.valueToTree(project);

            project.setStatus(status);
            Project updatedProject = projectRepository.save(project);

            // Record audit log
            JsonNode newState = objectMapper.valueToTree(updatedProject);
            auditService.recordAuditEvent(
                    AuditEventType.PROJECT_STATUS_UPDATED,
                    "PROJECT",
                    projectId,
                    actorUserId,
                    actorOrganisation,
                    actorIpAddress,
                    previousState,
                    newState
            );

            // Notify team members
            notifyTeamMembers(
                    projectId,
                    NotificationEventType.PROJECT_STATUS_UPDATED,
                    "Project status has been updated to '" + status + "'"
            );
        });

        return projectOpt;
    }

    /**
     * Deletes a project and records audit log + sends notifications.
     *
     * @param projectId the project ID
     * @param actorUserId the user deleting the project
     * @param actorOrganisation the organisation
     * @param actorIpAddress the IP address of the actor
     */
    public void deleteProject(
            UUID projectId,
            UUID actorUserId,
            String actorOrganisation,
            String actorIpAddress
    ) {
        Optional<Project> projectOpt = projectRepository.findById(projectId);
        projectOpt.ifPresent(project -> {
            // Capture state before deletion
            JsonNode previousState = objectMapper.valueToTree(project);

            // Notify team members BEFORE deletion
            notifyTeamMembers(
                    projectId,
                    NotificationEventType.PROJECT_DELETED,
                    "Project '" + project.getName() + "' has been deleted"
            );

            // Record audit log
            auditService.recordAuditEvent(
                    AuditEventType.PROJECT_DELETED,
                    "PROJECT",
                    projectId,
                    actorUserId,
                    actorOrganisation,
                    actorIpAddress,
                    previousState,
                    null // no new state after deletion
            );

            // Delete the project
            projectRepository.deleteById(projectId);
        });
    }

    /**
     * Reopens a project milestone and records audit log + sends notifications.
     * This is a new event type added as per the scope change requirement.
     *
     * @param projectId the project ID
     * @param actorUserId the user reopening the milestone
     * @param actorOrganisation the organisation
     * @param actorIpAddress the IP address of the actor
     * @return the updated project
     */
    public Optional<Project> reopenProjectMilestone(
            UUID projectId,
            UUID actorUserId,
            String actorOrganisation,
            String actorIpAddress
    ) {
        Optional<Project> projectOpt = projectRepository.findById(projectId);
        projectOpt.ifPresent(project -> {
            // Capture previous state
            JsonNode previousState = objectMapper.valueToTree(project);

            // Update status to indicate reopened
            project.setStatus("REOPENED");
            Project updatedProject = projectRepository.save(project);

            // Record audit log with IP address
            JsonNode newState = objectMapper.valueToTree(updatedProject);
            auditService.recordAuditEvent(
                    AuditEventType.MILESTONE_REOPENED,
                    "PROJECT",
                    projectId,
                    actorUserId,
                    actorOrganisation,
                    actorIpAddress,
                    previousState,
                    newState
            );

            // Notify team members
            notifyTeamMembers(
                    projectId,
                    NotificationEventType.MILESTONE_REOPENED,
                    "Project milestone has been reopened"
            );
        });

        return projectOpt;
    }

    /**
     * Retrieves all projects by team.
     *
     * @param team the team name
     * @return list of projects for the team
     */
    public List<Project> getProjectsByTeam(String team) {
        return projectRepository.findByTeam(team);
    }

    /**
     * Notifies all team members of a project about an event.
     *
     * @param projectId the project ID
     * @param eventType the notification event type
     * @param message the notification message
     */
    private void notifyTeamMembers(
            UUID projectId,
            NotificationEventType eventType,
            String message
    ) {
        List<UUID> teamMembers = projectTeamService.getTeamMembersByProjectId(projectId);
        for (UUID memberId : teamMembers) {
            notificationService.createNotification(
                    memberId,
                    eventType,
                    projectId,
                    message
            );
        }
    }
}
