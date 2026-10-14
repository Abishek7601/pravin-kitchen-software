package com.example.pravin_quotation.security;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.*;

/**
 * Service managing employee session lifecycles and the strict 8:00 PM IST auto-logout rule.
 *
 * Rules:
 * 1. Every employee session must expire at exactly 8:00 PM IST (Asia/Kolkata) on the day they log in.
 * 2. This deadline cannot be extended by user activity, page refreshes, or new requests.
 * 3. Any login or access attempt at or after 8:00 PM IST is rejected as expired.
 * 4. Once expired, the session is invalidated, SecurityContext is cleared, and cookies are deleted.
 */
@Service
public class EmployeeSessionService {

    public static final ZoneId ZONE_IST = ZoneId.of("Asia/Kolkata");
    public static final LocalTime CUTOFF_TIME_IST = LocalTime.of(20, 0, 0); // 8:00 PM IST

    public static final String ATTR_EXPIRY_INSTANT = "EMPLOYEE_EXPIRY_INSTANT";
    public static final String ATTR_EXPIRY_EPOCH_MS = "EMPLOYEE_EXPIRY_EPOCH_MS";
    public static final String ATTR_LOGIN_DATE = "EMPLOYEE_LOGIN_DATE";

    private Clock clock;

    public EmployeeSessionService() {
        this.clock = Clock.system(ZONE_IST);
    }

    @Autowired(required = false)
    public void setClock(Clock clock) {
        if (clock != null) {
            this.clock = clock;
        }
    }

    public Clock getClock() {
        return this.clock;
    }

    /**
     * Get current date and time in Asia/Kolkata timezone according to the configured clock.
     */
    public ZonedDateTime nowIst() {
        return ZonedDateTime.now(clock).withZoneSameInstant(ZONE_IST);
    }

    /**
     * Calculates the exact 8:00 PM IST deadline for today.
     */
    public ZonedDateTime getTodayDeadlineIst() {
        LocalDate today = nowIst().toLocalDate();
        return today.atTime(CUTOFF_TIME_IST).atZone(ZONE_IST);
    }

    /**
     * Returns true if the current time is 8:00 PM IST or later on the current day.
     */
    public boolean isPast8PmIst() {
        ZonedDateTime now = nowIst();
        ZonedDateTime deadline = getTodayDeadlineIst();
        return !now.isBefore(deadline);
    }

    /**
     * Calculates the remaining milliseconds until 8:00 PM IST today.
     * Returns 0 if already at or past 8:00 PM IST.
     */
    public long getRemainingMillis() {
        ZonedDateTime now = nowIst();
        ZonedDateTime deadline = getTodayDeadlineIst();
        if (!now.isBefore(deadline)) {
            return 0L;
        }
        return Duration.between(now, deadline).toMillis();
    }

    /**
     * Initialize employee session attributes with the 8:00 PM IST deadline.
     */
    public void initializeEmployeeSession(HttpSession session) {
        if (session == null) {
            return;
        }
        ZonedDateTime deadline = getTodayDeadlineIst();
        Instant expiryInstant = deadline.toInstant();
        LocalDate loginDate = nowIst().toLocalDate();

        session.setAttribute(ATTR_LOGIN_DATE, loginDate);
        session.setAttribute(ATTR_EXPIRY_INSTANT, expiryInstant);
        session.setAttribute(ATTR_EXPIRY_EPOCH_MS, expiryInstant.toEpochMilli());
    }

    /**
     * Checks if the given employee session has expired according to the 8:00 PM IST rule.
     */
    public boolean isSessionExpired(HttpSession session) {
        if (session == null) {
            return true;
        }

        // Rule: If current time in IST is >= 8:00 PM, session is expired
        if (isPast8PmIst()) {
            return true;
        }

        // Rule: Check login date stored in session
        Object dateObj = session.getAttribute(ATTR_LOGIN_DATE);
        if (dateObj instanceof LocalDate loginDate) {
            LocalDate today = nowIst().toLocalDate();
            if (!today.equals(loginDate)) {
                // Session from a previous day is expired
                return true;
            }
        }

        // Rule: Check exact expiry instant stored in session
        Object expiryObj = session.getAttribute(ATTR_EXPIRY_INSTANT);
        if (expiryObj instanceof Instant expiryInstant) {
            Instant now = clock.instant();
            if (!now.isBefore(expiryInstant)) {
                return true;
            }
        } else {
            // If before 8:00 PM IST and expiry not yet set on session, lazily initialize for today
            initializeEmployeeSession(session);
        }

        return false;
    }

    /**
     * Invalidate session, clear SecurityContext, and clear JSESSIONID cookie.
     */
    public void invalidateEmployeeSession(HttpServletRequest request, HttpServletResponse response) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            try {
                session.invalidate();
            } catch (IllegalStateException ignored) {
                // Already invalidated
            }
        }

        SecurityContextHolder.clearContext();

        if (response != null) {
            Cookie cookie = new Cookie("JSESSIONID", "");
            cookie.setPath("/");
            cookie.setMaxAge(0);
            cookie.setHttpOnly(true);
            response.addCookie(cookie);

            // Add standard cache control headers to prevent browser back-button caching
            response.setHeader("Cache-Control", "no-cache, no-store, max-age=0, must-revalidate");
            response.setHeader("Pragma", "no-cache");
            response.setDateHeader("Expires", 0);
        }
    }
}
