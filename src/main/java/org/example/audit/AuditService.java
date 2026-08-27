package org.example.audit;

import java.util.UUID;

/**
 * DEPRECATED: AuditService moved to org.example.notifications.AuditService
 */
@Deprecated
public class AuditService {
    public void recordAuditEvent() {
        throw new UnsupportedOperationException("Use org.example.notifications.AuditService");
    }
}
