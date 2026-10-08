import vn.dtinh.messenger.RegistrationPolicy;
public class RegistrationPolicyTest {
    static void check(boolean value, String message) { if (!value) throw new AssertionError(message); }
    public static void main(String[] args) {
        RegistrationPolicy p = new RegistrationPolicy();
        check(p.begin(0, null), "Missing login must remain retryable");
        check(!p.begin(1, "A"), "One request at a time");
        check(p.finish(100, null), "No session retries");
        check(p.delay(100) == 20000, "Initial backoff");
        check(!p.begin(101, null), "Cooldown prevents a request storm");
        check(p.begin(20100, "A"), "Login enables registration");
        check(p.finish(20200, "A"), "No token retries even without an exception");
        long t = 40200;
        for (long delay : new long[]{60000, 120000, 300000, 300000}) {
            check(p.begin(t, "A"), "Retry after cooldown");
            check(p.finish(t, "A"), "Transient failure stays retryable");
            check(p.delay(t) == delay, "Backoff reaches a five-minute cap");
            t += delay;
        }
        check(p.begin(t, "A"), "Final retry starts");
        p.tokenReady("A");
        check(!p.finish(t, "A"), "Token handoff stops retrying");
        check(!p.begin(t + 1000000, "A"), "Completed user does not re-register");
        check(p.begin(t + 1, "B"), "Account switch is not blocked by previous success");
        p.tokenReady("B");
        check(!p.finish(t + 2, "B"), "Second account completes independently");
        check(p.begin(t + 3, "A"), "Switching back can register the previous account again");
        System.out.println("PASS: login readiness, concurrency, cooldown, capped retries, token handoff and account switches");
    }
}
