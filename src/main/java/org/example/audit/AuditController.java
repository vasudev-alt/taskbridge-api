package org.example.audit;

import jakarta.servlet.http.HttpServletRequest;

/**
 * DEPRECATED: AuditController moved to org.example.notifications.AuditController
 * Please update imports to use org.example.notifications.AuditController and remove this file.
 */
@Deprecated
public class AuditController {

    // Deprecated stub to avoid immediate breakage. Controller implementation moved.
    public void recordAuditEvent(HttpServletRequest request, Object auditEventRequest) {
        throw new UnsupportedOperationException("Use org.example.notifications.AuditController.recordAuditEvent");
    }
}
