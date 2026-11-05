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
    public static final LocalTime MORNING_START_TIME_IST = LocalTime.of(6, 0, 0); // 6:00 AM IST

    public static final String ATTR_EXPIRY_INSTANT = "EMPLOYEE_EXPIRY_INSTANT";
    public static final String ATTR_EXPIRY_EPOCH_MS = "EMPLOYEE_EXPIRY_EPOCH_MS";
    public static final String ATTR_LOGIN_DATE = "EMPLOYEE_LOGIN_DATE";
    public static final String ATTR_AFTER_HOURS_LOGIN = "EMPLOYEE_AFTER_HOURS_LOGIN";

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
     * Returns true if current time is after 8:00 PM IST (20:00) until morning working hours (06:00).
     */
    public boolean isAfterHours() {
        ZonedDateTime now = nowIst();
        LocalTime time = now.toLocalTime();
        return !time.isBefore(CUTOFF_TIME_IST) || time.isBefore(MORNING_START_TIME_IST);
    }

    /**
     * Returns true if the current time is 8:00 PM IST or later on the current day (or overnight after-hours).
     */
    public boolean isPast8PmIst() {
        return isAfterHours();
    }

    /**
     * Calculates the remaining milliseconds until 8:00 PM IST today for normal sessions,
     * or until morning reset for after-hours sessions.
     */
    public long getRemainingMillis(HttpSession session) {
        if (session != null && Boolean.TRUE.equals(session.getAttribute(ATTR_AFTER_HOURS_LOGIN))) {
            Object expiryObj = session.getAttribute(ATTR_EXPIRY_INSTANT);
            if (expiryObj instanceof Instant expiryInstant) {
                Instant now = clock.instant();
                if (!now.isBefore(expiryInstant)) {
                    return 0L;
                }
                return Duration.between(now, expiryInstant).toMillis();
            }
            return 3600000L;
        }

        ZonedDateTime now = nowIst();
        ZonedDateTime deadline = getTodayDeadlineIst();
        if (!now.isBefore(deadline)) {
            return 0L;
        }
        return Duration.between(now, deadline).toMillis();
    }

    public long getRemainingMillis() {
        return getRemainingMillis(null);
    }

    /**
     * Initialize employee session attributes with the 8:00 PM IST deadline (for normal sessions)
     * or morning reset deadline (for after-hours sessions).
     */
    public void initializeEmployeeSession(HttpSession session) {
        if (session == null) {
            return;
        }
        LocalDate today = nowIst().toLocalDate();
        ZonedDateTime deadline;
        if (isAfterHours()) {
            ZonedDateTime nextMorning = (nowIst().toLocalTime().isBefore(MORNING_START_TIME_IST))
                    ? today.atTime(MORNING_START_TIME_IST).atZone(ZONE_IST)
                    : today.plusDays(1).atTime(MORNING_START_TIME_IST).atZone(ZONE_IST);
            deadline = nextMorning;
            session.setAttribute(ATTR_AFTER_HOURS_LOGIN, true);
        } else {
            deadline = getTodayDeadlineIst();
            session.setAttribute(ATTR_AFTER_HOURS_LOGIN, false);
        }

        Instant expiryInstant = deadline.toInstant();
        session.setAttribute(ATTR_LOGIN_DATE, today);
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

        boolean isAfterHoursSession = Boolean.TRUE.equals(session.getAttribute(ATTR_AFTER_HOURS_LOGIN));
        LocalDate today = nowIst().toLocalDate();
        Object dateObj = session.getAttribute(ATTR_LOGIN_DATE);

        if (isAfterHoursSession) {
            // An after-hours session was verified with Super Admin OTP.
            // It expires when normal working hours start the next morning or by expiry instant.
            if (dateObj instanceof LocalDate loginDate) {
                if (!today.equals(loginDate) && !isAfterHours()) {
                    return true;
                }
            }
            Object expiryObj = session.getAttribute(ATTR_EXPIRY_INSTANT);
            if (expiryObj instanceof Instant expiryInstant) {
                Instant now = clock.instant();
                if (!now.isBefore(expiryInstant)) {
                    return true;
                }
            }
            return false;
        }

        // --- NORMAL WORKING HOURS SESSION (Started before 8:00 PM) ---
        // Rule: If current time in IST is >= 8:00 PM IST, normal session is expired
        if (isAfterHours()) {
            return true;
        }

        // Rule: Check login date stored in session
        if (dateObj instanceof LocalDate loginDate) {
            if (!today.equals(loginDate)) {
                // Session from a previous day is expired
                return true;
            }
        }

        // Rule: Check exact expiry instant stored in session (today at 8:00 PM IST)
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
