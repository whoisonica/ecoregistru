package ro.ecoregistru.security;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import io.github.bucket4j.TimeMeter;
import io.github.bucket4j.local.LocalBucketBuilder;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * P0.3 — the brake on the three public doors.
 *
 * <p>In memory, on purpose: one dyno, and a limiter that needs Redis to start is a limiter that
 * gets turned off. If the app ever runs on two dynos the counts split in two, which halves the
 * brake but does not break it — the note is here so nobody discovers that as a surprise.
 *
 * <p>Each {@link Rule} is a capacity and a window, and each {@code (rule, key)} pair gets its own
 * token bucket. The key is an IP for the flood case and an email for the „one account, many IPs"
 * case; {@link #tryConsume} answers with the seconds to wait, so the caller can put a real
 * {@code Retry-After} on the 429 instead of a guess.
 */
@Component
public class RateLimiter {

    /** A named quota. The name is what shows up in the log line when it trips. */
    public record Rule(String name, long capacity, Duration window) {}

    // Login is limited twice, and the two counts are deliberately different things.
    //
    // Per IP counts *every* attempt: it is the brake on a script working through a password list,
    // and it has to sit above what an office behind one NAT does on a Monday morning — and above
    // what the e2e suite does, which is ~14 logins in ~7 minutes from one address.
    public static final Rule LOGIN_PER_IP = new Rule("login/ip", 60, Duration.ofMinutes(5));

    // Per email counts only the attempts that FAILED (see AuthenticationService). Counting the
    // successful ones would lock out the one person who knows their password — a real office, or
    // the e2e suite, which signs in as admin@demo.ro about ten times a run. Ten wrong passwords in
    // a quarter of an hour is not a person who mistyped.
    public static final Rule LOGIN_PER_EMAIL = new Rule("login/email", 10, Duration.ofMinutes(15));

    // Password reset sends mail through our own Gmail credentials. A flood here does not just
    // spam a stranger's inbox: it gets the account suspended, and with it every invite and every
    // deadline alert the product sends.
    public static final Rule RESET_PER_IP = new Rule("reset/ip", 10, Duration.ofHours(1));
    public static final Rule RESET_PER_EMAIL = new Rule("reset/email", 3, Duration.ofHours(1));

    // The intake form is the only public write in the application. Ten an hour from one address is
    // far above a real prospect (who submits once) and far below a script filling the table.
    public static final Rule INTAKE_PER_IP = new Rule("intake/ip", 10, Duration.ofHours(1));

    /**
     * Above this many live keys the map is trimmed of idle buckets. Without a bound, an attacker
     * rotating IPs would be filling a map rather than being stopped by it. A bucket back at full
     * capacity has no memory worth keeping — dropping it is the same as never having seen the key.
     */
    private static final int MAX_KEYS = 20_000;

    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();
    private final TimeMeter timeMeter;

    public RateLimiter() {
        this(TimeMeter.SYSTEM_MILLISECONDS);
    }

    /** Tests hand in a clock they can move, so a window can expire without waiting for it. */
    RateLimiter(TimeMeter timeMeter) {
        this.timeMeter = timeMeter;
    }

    /**
     * @return {@code 0} when the request is allowed, otherwise the whole seconds to wait before
     *     the next token is available (never less than 1, so {@code Retry-After: 0} is impossible).
     */
    public long tryConsume(Rule rule, String key) {
        if (key == null || key.isBlank()) {
            return 0; // Nothing to count against. Refusing here would break the request, not a flood.
        }
        Bucket bucket = buckets.computeIfAbsent(rule.name() + "|" + key, k -> newBucket(rule));
        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);
        if (probe.isConsumed()) {
            return 0;
        }
        long seconds = probe.getNanosToWaitForRefill() / 1_000_000_000L;
        return Math.max(1, seconds);
    }

    /**
     * Give a token back, for an attempt that turned out not to be one — a login with the right
     * password. Without this the per-email quota would count people who know their password, and
     * the eleventh honest sign-in of the morning would be refused.
     *
     * <p>Adding to a full bucket is a no-op in bucket4j, so a refund can never raise the ceiling.
     */
    public void refund(Rule rule, String key) {
        if (key == null || key.isBlank()) {
            return;
        }
        Bucket bucket = buckets.get(rule.name() + "|" + key);
        if (bucket != null) {
            bucket.addTokens(1);
        }
    }

    private Bucket newBucket(Rule rule) {
        if (buckets.size() > MAX_KEYS) {
            evictIdle();
        }
        LocalBucketBuilder builder = Bucket.builder()
                .addLimit(Bandwidth.builder()
                        .capacity(rule.capacity())
                        .refillIntervally(rule.capacity(), rule.window())
                        .build());
        return builder.withCustomTimePrecision(timeMeter).build();
    }

    private void evictIdle() {
        buckets.entrySet().removeIf(e -> e.getValue().getAvailableTokens() >= capacityOf(e.getKey()));
    }

    /** The rule name is the part of the key before the '|', and every rule is one of the five. */
    private long capacityOf(String key) {
        String name = key.substring(0, Math.max(0, key.indexOf('|')));
        for (Rule rule : new Rule[] {
                LOGIN_PER_IP, LOGIN_PER_EMAIL, RESET_PER_IP, RESET_PER_EMAIL, INTAKE_PER_IP}) {
            if (rule.name().equals(name)) {
                return rule.capacity();
            }
        }
        return Long.MAX_VALUE; // Unknown rule: never counted as idle, so never dropped by mistake.
    }
}
