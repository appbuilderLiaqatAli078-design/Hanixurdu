<?php
declare(strict_types=1);

/**
 * HaniX Urdu — Shahkot Admin Panel Extension Module (`shahkot.php`, `add_premium.php`, `unlock_vip.php`)
 *
 * HOW TO INCLUDE IN EXISTING `shahkot.php`:
 * Add at the bottom of your authenticated admin dashboard in `shahkot.php`:
 *   <?php require_once __DIR__ . '/shahkot_vip_bunny_module.php'; ?>
 *
 * SECURITY:
 * Requires an active admin session (`$_SESSION['admin_logged_in'] === true`) and CSRF token verification.
 *
 * MATCHES REAL `premium_users` SCHEMA (`add_premium.php` & `unlock_vip.php`):
 * - `premium_users (id, email UNIQUE, expiry_date DATETIME)`
 */

if (session_status() === PHP_SESSION_NONE) {
    session_start();
}

$isShahkotAdmin = !empty($_SESSION['admin_logged_in'])
    || !empty($_SESSION['is_admin'])
    || !empty($_SESSION['shahkot_admin']);

if (!$isShahkotAdmin) {
    http_response_code(403);
    echo '<div style="padding:16px;background:#3b0d0d;color:#ffdada;border:1px solid #ff5a5a;border-radius:8px;">'
        . 'Access Denied: Shahkot Admin authentication is required.</div>';
    return;
}

if (empty($_SESSION['shahkot_csrf_token'])) {
    $_SESSION['shahkot_csrf_token'] = bin2hex(random_bytes(32));
}
$csrfToken = (string) $_SESSION['shahkot_csrf_token'];

require_once __DIR__ . '/../public_html/api/v1/config/Config.php';
$pdo = \HaniX\Api\Config\Config::getPdo();

$flashMessage = '';
$flashError = '';

if ($_SERVER['REQUEST_METHOD'] === 'POST' && isset($_POST['shahkot_action'])) {
    $submittedCsrf = (string) ($_POST['csrf_token'] ?? '');
    if (!hash_equals($csrfToken, $submittedCsrf)) {
        $flashError = 'Invalid security token (CSRF). Please reload Shahkot Admin and try again.';
    } else {
        $action = (string) $_POST['shahkot_action'];

        try {
            if ($action === 'activate_or_extend_vip') {
                $targetEmail = strtolower(trim((string) ($_POST['email'] ?? '')));
                $daysToAdd = max(1, min(3650, (int) ($_POST['days'] ?? 30)));

                if ($targetEmail === '' || !filter_var($targetEmail, FILTER_VALIDATE_EMAIL)) {
                    throw new RuntimeException('Please provide a valid user email address.');
                }

                // Check if user already has a future expiry_date in premium_users (matching unlock_vip.php)
                $stmt = $pdo->prepare('SELECT id, expiry_date FROM premium_users WHERE LOWER(email) = LOWER(:email) ORDER BY expiry_date DESC LIMIT 1');
                $stmt->execute([':email' => $targetEmail]);
                $existing = $stmt->fetch();

                $baseTimestamp = time();
                if ($existing && !empty($existing['expiry_date'])) {
                    $existingTs = strtotime((string) $existing['expiry_date']);
                    if ($existingTs !== false && $existingTs > $baseTimestamp) {
                        $baseTimestamp = $existingTs;
                    }
                }

                $newExpiry = date('Y-m-d H:i:s', $baseTimestamp + ($daysToAdd * 86400));

                $iStmt = $pdo->prepare('
                    INSERT INTO premium_users (email, expiry_date)
                    VALUES (:email, :exp)
                    ON DUPLICATE KEY UPDATE expiry_date = VALUES(expiry_date)
                ');
                $iStmt->execute([':email' => $targetEmail, ':exp' => $newExpiry]);

                $flashMessage = sprintf(
                    'VIP Unlocked for <strong>%s</strong> until <strong>%s</strong> (+%d days).',
                    htmlspecialchars($targetEmail, ENT_QUOTES, 'UTF-8'),
                    htmlspecialchars($newExpiry, ENT_QUOTES, 'UTF-8'),
                    $daysToAdd
                );
            } elseif ($action === 'revoke_vip') {
                $targetEmail = strtolower(trim((string) ($_POST['email'] ?? '')));
                if ($targetEmail === '') {
                    throw new RuntimeException('Please provide a valid user email address.');
                }

                $stmt = $pdo->prepare('DELETE FROM premium_users WHERE LOWER(email) = LOWER(:email)');
                $stmt->execute([':email' => $targetEmail]);

                $flashMessage = sprintf(
                    'VIP revoked for <strong>%s</strong>. Account immediately reverted to FREE.',
                    htmlspecialchars($targetEmail, ENT_QUOTES, 'UTF-8')
                );
            } elseif ($action === 'save_bunny_mapping') {
                $contentType = strtoupper(trim((string) ($_POST['content_type'] ?? 'MOVIE'))) === 'EPISODE' ? 'EPISODE' : 'MOVIE';
                $contentId = (int) ($_POST['content_id'] ?? 0);
                $bunnyLibraryId = trim((string) ($_POST['bunny_library_id'] ?? ''));
                $bunnyVideoId = trim((string) ($_POST['bunny_video_id'] ?? ''));
                $isVipStream = isset($_POST['is_vip_stream']) ? 1 : 0;

                if ($contentId <= 0 || $bunnyVideoId === '') {
                    throw new RuntimeException('Content ID and Bunny Stream Video GUID are required.');
                }

                $stmt = $pdo->prepare('
                    INSERT INTO bunny_video_mappings
                        (content_type, content_id, bunny_library_id, bunny_video_id, is_vip_stream, updated_at)
                    VALUES
                        (:ctype, :cid, :lib, :vid, :vip, NOW())
                    ON DUPLICATE KEY UPDATE
                        bunny_library_id = VALUES(bunny_library_id),
                        bunny_video_id   = VALUES(bunny_video_id),
                        is_vip_stream    = VALUES(is_vip_stream),
                        updated_at       = NOW()
                ');
                $stmt->execute([
                    ':ctype' => $contentType,
                    ':cid'   => $contentId,
                    ':lib'   => $bunnyLibraryId,
                    ':vid'   => $bunnyVideoId,
                    ':vip'   => $isVipStream,
                ]);

                $flashMessage = sprintf(
                    'Saved Bunny Stream GUID (%s) for %s #%d.',
                    htmlspecialchars($bunnyVideoId, ENT_QUOTES, 'UTF-8'),
                    $contentType,
                    $contentId
                );
            }
        } catch (Throwable $e) {
            $flashError = $e->getMessage();
        }
    }
}

$searchQuery = trim((string) ($_GET['shahkot_user_q'] ?? ''));
$userResults = [];
if ($searchQuery !== '') {
    $like = '%' . $searchQuery . '%';
    $uStmt = $pdo->prepare('
        SELECT u.id, u.email, MAX(p.expiry_date) AS vip_expiry
        FROM users u
        LEFT JOIN premium_users p ON LOWER(p.email) = LOWER(u.email)
        WHERE u.email LIKE :q OR CAST(u.id AS CHAR) = :exact
        GROUP BY u.id, u.email
        ORDER BY u.id DESC
        LIMIT 30
    ');
    $uStmt->execute([':q' => $like, ':exact' => $searchQuery]);
    $userResults = $uStmt->fetchAll();
}
?>

<section style="margin:24px 0;padding:24px;background:#10131C;color:#F5F2EB;border:1px solid #2A3142;border-radius:14px;font-family:system-ui,-apple-system,sans-serif;">
    <h2 style="margin-top:0;color:#E50914;">HaniX Urdu — Shahkot VIP &amp; Bunny Stream Control Center</h2>
    <p style="color:#A0A8BC;font-size:14px;">
        Uses your existing <code>premium_users (email, expiry_date)</code> table from <code>add_premium.php</code> &amp; <code>unlock_vip.php</code>.
    </p>

    <?php if ($flashMessage !== ''): ?>
        <div style="padding:12px 16px;margin-bottom:16px;background:#103828;color:#75F0B6;border:1px solid #25855A;border-radius:8px;">
            <?= $flashMessage ?>
        </div>
    <?php endif; ?>

    <?php if ($flashError !== ''): ?>
        <div style="padding:12px 16px;margin-bottom:16px;background:#3B1219;color:#FF9DA7;border:1px solid #9B2C3B;border-radius:8px;">
            <?= htmlspecialchars($flashError, ENT_QUOTES, 'UTF-8') ?>
        </div>
    <?php endif; ?>

    <!-- 1. Quick VIP Unlock by Email (Matches add_premium.php / unlock_vip.php) -->
    <div style="margin-top:20px;padding:16px;background:#171B26;border-radius:10px;">
        <h3 style="margin-top:0;color:#F5F2EB;">1. Quick Unlock / Extend VIP by Email</h3>
        <form method="post" style="display:flex;gap:10px;flex-wrap:wrap;align-items:center;">
            <input type="hidden" name="csrf_token" value="<?= htmlspecialchars($csrfToken, ENT_QUOTES, 'UTF-8') ?>">
            <input type="hidden" name="shahkot_action" value="activate_or_extend_vip">
            <input type="email" name="email" required placeholder="User Registered Email (e.g. user@gmail.com)"
                   style="flex:2;min-width:240px;padding:10px 12px;border-radius:8px;border:1px solid #2E3648;background:#0B0E14;color:#F5F2EB;">
            <select name="days" style="flex:1;min-width:160px;padding:10px 12px;border-radius:8px;border:1px solid #2E3648;background:#0B0E14;color:#F5F2EB;">
                <option value="30">1 Month (30 Days)</option>
                <option value="180">6 Months (180 Days)</option>
                <option value="365">1 Year (365 Days)</option>
            </select>
            <button type="submit" style="padding:10px 18px;border-radius:8px;border:none;background:#E50914;color:#fff;font-weight:700;cursor:pointer;">
                Unlock VIP Access
            </button>
        </form>
    </div>

    <!-- 2. Search Users & Manage FREE / VIP Membership -->
    <div style="margin-top:20px;padding:16px;background:#171B26;border-radius:10px;">
        <h3 style="margin-top:0;color:#F5F2EB;">2. Search Users &amp; Manage FREE / VIP Membership</h3>
        <form method="get" style="display:flex;gap:8px;flex-wrap:wrap;margin-bottom:16px;">
            <input type="text" name="shahkot_user_q" value="<?= htmlspecialchars($searchQuery, ENT_QUOTES, 'UTF-8') ?>"
                   placeholder="Search by email or user ID..."
                   style="flex:1;min-width:240px;padding:10px 12px;border-radius:8px;border:1px solid #2E3648;background:#0B0E14;color:#F5F2EB;">
            <button type="submit" style="padding:10px 18px;border-radius:8px;border:none;background:#E2B049;color:#0B0E14;font-weight:700;cursor:pointer;">
                Search Users
            </button>
        </form>

        <?php if (!empty($userResults)): ?>
            <table style="width:100%;border-collapse:collapse;font-size:14px;">
                <thead>
                <tr style="border-bottom:1px solid #2A3142;text-align:left;color:#A0A8BC;">
                    <th style="padding:8px;">ID</th>
                    <th style="padding:8px;">Email</th>
                    <th style="padding:8px;">Current Tier</th>
                    <th style="padding:8px;">VIP Expiry</th>
                    <th style="padding:8px;">Actions</th>
                </tr>
                </thead>
                <tbody>
                <?php foreach ($userResults as $u):
                    $exp = (string) ($u['vip_expiry'] ?? '');
                    $isActiveVip = ($exp !== '' && strtotime($exp) > time());
                    ?>
                    <tr style="border-bottom:1px solid #1E2433;">
                        <td style="padding:8px;">#<?= (int) $u['id'] ?></td>
                        <td style="padding:8px;"><?= htmlspecialchars((string) ($u['email'] ?? ''), ENT_QUOTES, 'UTF-8') ?></td>
                        <td style="padding:8px;">
                            <?php if ($isActiveVip): ?>
                                <span style="padding:3px 8px;border-radius:6px;background:#E2B049;color:#0B0E14;font-weight:700;">VIP</span>
                            <?php else: ?>
                                <span style="padding:3px 8px;border-radius:6px;background:#252B3B;color:#A0A8BC;">FREE</span>
                            <?php endif; ?>
                        </td>
                        <td style="padding:8px;"><?= $exp !== '' ? htmlspecialchars($exp, ENT_QUOTES, 'UTF-8') : '—' ?></td>
                        <td style="padding:8px;display:flex;gap:6px;flex-wrap:wrap;">
                            <form method="post" style="display:inline;">
                                <input type="hidden" name="csrf_token" value="<?= htmlspecialchars($csrfToken, ENT_QUOTES, 'UTF-8') ?>">
                                <input type="hidden" name="shahkot_action" value="activate_or_extend_vip">
                                <input type="hidden" name="email" value="<?= htmlspecialchars((string) ($u['email'] ?? ''), ENT_QUOTES, 'UTF-8') ?>">
                                <input type="hidden" name="days" value="30">
                                <button type="submit" style="padding:6px 10px;border-radius:6px;border:none;background:#1E7E52;color:#fff;cursor:pointer;">
                                    +30 Days VIP
                                </button>
                            </form>
                            <form method="post" style="display:inline;">
                                <input type="hidden" name="csrf_token" value="<?= htmlspecialchars($csrfToken, ENT_QUOTES, 'UTF-8') ?>">
                                <input type="hidden" name="shahkot_action" value="revoke_vip">
                                <input type="hidden" name="email" value="<?= htmlspecialchars((string) ($u['email'] ?? ''), ENT_QUOTES, 'UTF-8') ?>">
                                <button type="submit" style="padding:6px 10px;border-radius:6px;border:none;background:#9B2C3B;color:#fff;cursor:pointer;">
                                    Revoke to FREE
                                </button>
                            </form>
                        </td>
                    </tr>
                <?php endforeach; ?>
                </tbody>
            </table>
        <?php endif; ?>
    </div>
</section>
