/**
 * Pravin Kitchens & Interiors - Employee Session Lifecycle & Auto-Logout Manager
 *
 * Enforces the strict 8:00 PM IST (Asia/Kolkata) auto-logout rule on the frontend.
 * - Schedules an exact timer for 8:00 PM IST.
 * - Runs periodic background checks (every 15 seconds).
 * - Reacts immediately when tab becomes visible or receives focus (e.g. laptop wake-up).
 * - Prevents Back-button bfcache bypass by forcing reload on pageshow if persisted.
 * - Automatically redirects to login with ?expired when the deadline is reached.
 */
(function () {
    'use strict';

    // Prevent duplicate initializations
    if (window.__employeeSessionTimerActive) {
        return;
    }
    window.__employeeSessionTimerActive = true;

    var deadlineEpochMs = null;
    var timerId = null;
    var checkIntervalId = null;
    var isLoggingOut = false;

    // Helper: calculate local fallback deadline for 8:00 PM IST (UTC+05:30)
    function calculateLocalIstDeadlineMs() {
        var now = new Date();
        // Convert to IST offset (+330 minutes)
        var utc = now.getTime() + (now.getTimezoneOffset() * 60000);
        var istOffsetMs = 5.5 * 3600000;
        var istNow = new Date(utc + istOffsetMs);

        var istCutoff = new Date(istNow);
        istCutoff.setHours(20, 0, 0, 0); // 8:00:00 PM IST

        var diffMs = istCutoff.getTime() - istNow.getTime();
        return diffMs > 0 ? Date.now() + diffMs : Date.now();
    }

    function triggerAutoLogout() {
        if (isLoggingOut) return;
        isLoggingOut = true;

        if (timerId) clearTimeout(timerId);
        if (checkIntervalId) clearInterval(checkIntervalId);

        // Attempt graceful logout request to invalidate session on server
        try {
            var csrfMeta = document.querySelector('meta[name="_csrf"]');
            var csrfHeader = document.querySelector('meta[name="_csrf_header"]');
            var headers = { 'X-Requested-With': 'XMLHttpRequest' };
            if (csrfMeta && csrfHeader) {
                headers[csrfHeader.getAttribute('content')] = csrfMeta.getAttribute('content');
            }

            fetch('/logout', {
                method: 'POST',
                headers: headers,
                credentials: 'same-origin'
            }).finally(function () {
                window.location.replace('/login?expired');
            });
        } catch (e) {
            window.location.replace('/login?expired');
        }
    }

    function checkDeadline() {
        if (deadlineEpochMs !== null && Date.now() >= deadlineEpochMs) {
            triggerAutoLogout();
        }
    }

    function syncWithServer() {
        fetch('/employee/session-status', {
            method: 'GET',
            headers: { 'Accept': 'application/json' },
            credentials: 'same-origin'
        })
            .then(function (res) {
                if (res.status === 401 || res.status === 403) {
                    triggerAutoLogout();
                    return null;
                }
                return res.json();
            })
            .then(function (data) {
                if (!data) return;

                if (data.expired === true || data.authenticated === false) {
                    triggerAutoLogout();
                    return;
                }

                if (typeof data.remainingMillis === 'number') {
                    if (data.remainingMillis <= 0) {
                        triggerAutoLogout();
                        return;
                    }
                    deadlineEpochMs = Date.now() + data.remainingMillis;
                    scheduleTimer(data.remainingMillis);
                }
            })
            .catch(function () {
                // If endpoint check fails, rely on local IST deadline
                if (deadlineEpochMs === null) {
                    deadlineEpochMs = calculateLocalIstDeadlineMs();
                    var remaining = Math.max(0, deadlineEpochMs - Date.now());
                    scheduleTimer(remaining);
                }
            });
    }

    function scheduleTimer(remainingMillis) {
        if (timerId) clearTimeout(timerId);

        if (remainingMillis <= 0) {
            triggerAutoLogout();
            return;
        }

        // Set timeout for the remaining duration
        // Note: setTimeout max 32-bit int is ~24.8 days (~2147483647 ms), remaining is at most 24 hours.
        timerId = setTimeout(function () {
            triggerAutoLogout();
        }, remainingMillis);
    }

    // Initialize synchronization
    syncWithServer();

    // Check periodically every 15 seconds
    checkIntervalId = setInterval(function () {
        checkDeadline();
    }, 15000);

    // Re-sync with server every 5 minutes
    setInterval(function () {
        if (!isLoggingOut) {
            syncWithServer();
        }
    }, 300000);

    // When tab becomes active or window gains focus (e.g., computer wakes from sleep)
    document.addEventListener('visibilitychange', function () {
        if (!document.hidden) {
            checkDeadline();
            syncWithServer();
        }
    });

    window.addEventListener('focus', function () {
        checkDeadline();
        syncWithServer();
    });

    // Prevent Back/Forward Cache (bfcache) from showing stale authenticated state after logout
    window.addEventListener('pageshow', function (event) {
        if (event.persisted) {
            window.location.reload();
        } else {
            checkDeadline();
        }
    });
})();
