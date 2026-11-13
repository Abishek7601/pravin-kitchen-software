package com.example.pravin_quotation.security;

import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.HttpSessionEvent;
import jakarta.servlet.http.HttpSessionListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks active employee HTTP sessions and automatically invalidates all of them at exactly 8:00 PM IST.
 * Ensures Admin and Super Admin sessions are NEVER invalidated.
 */
@Component
public class ActiveEmployeeSessionRegistry implements HttpSessionListener {

    private static final Logger log = LoggerFactory.getLogger(ActiveEmployeeSessionRegistry.class);

    public static final String ATTR_EMPLOYEE_SESSION = "IS_ACTIVE_EMPLOYEE_SESSION";
    public static final String ATTR_EMPLOYEE_ID = "ACTIVE_EMPLOYEE_ID";

    private final Map<String, HttpSession> activeEmployeeSessions = new ConcurrentHashMap<>();

    @Override
    public void sessionCreated(HttpSessionEvent se) {
        // Session created; will be registered as employee session only upon employee authentication
    }

    @Override
    public void sessionDestroyed(HttpSessionEvent se) {
        if (se != null && se.getSession() != null) {
            activeEmployeeSessions.remove(se.getSession().getId());
        }
    }

    /**
     * Registers an authenticated Employee session.
     */
    public void registerEmployeeSession(HttpSession session, Long employeeId) {
        if (session == null) {
            return;
        }
        try {
            session.setAttribute(ATTR_EMPLOYEE_SESSION, Boolean.TRUE);
            if (employeeId != null) {
                session.setAttribute(ATTR_EMPLOYEE_ID, employeeId);
            }
            activeEmployeeSessions.put(session.getId(), session);
            log.debug("Registered active employee session [{}] for employeeId [{}]", session.getId(), employeeId);
        } catch (IllegalStateException e) {
            log.warn("Cannot register already invalidated session: {}", e.getMessage());
        }
    }

    /**
     * Unregisters an employee session (e.g. on manual logout).
     */
    public void unregisterEmployeeSession(HttpSession session) {
        if (session != null) {
            activeEmployeeSessions.remove(session.getId());
        }
    }

    /**
     * Returns the number of currently tracked active employee sessions.
     */
    public int getActiveEmployeeSessionCount() {
        return activeEmployeeSessions.size();
    }

    /**
     * Scheduled job running at exactly 8:00 PM every day in Asia/Kolkata timezone.
     * Automatically invalidates all active Employee sessions.
     * Admin and Super Admin sessions are strictly preserved and never touched.
     */
    @Scheduled(cron = "0 0 20 * * ?", zone = "Asia/Kolkata")
    public void invalidateAllEmployeeSessionsAt8Pm() {
        log.info("Executing 8:00 PM IST auto-logout schedule. Active employee sessions to invalidate: {}",
                activeEmployeeSessions.size());

        for (Map.Entry<String, HttpSession> entry : activeEmployeeSessions.entrySet()) {
            String sessionId = entry.getKey();
            HttpSession session = entry.getValue();

            try {
                // Safety check: ensure this is explicitly an employee session
                Object isEmp = session.getAttribute(ATTR_EMPLOYEE_SESSION);
                if (Boolean.TRUE.equals(isEmp)) {
                    session.invalidate();
                    log.info("Invalidated employee session [{}] at 8:00 PM IST", sessionId);
                }
            } catch (IllegalStateException e) {
                // Already invalidated
            } catch (Exception e) {
                log.warn("Error invalidating employee session [{}]: {}", sessionId, e.getMessage());
            }
        }

        activeEmployeeSessions.clear();
    }
}

