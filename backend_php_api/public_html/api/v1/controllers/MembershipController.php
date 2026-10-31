<?php
declare(strict_types=1);

namespace HaniX\Api\Controllers;

use HaniX\Api\Helpers\ResponseHelper;
use HaniX\Api\Middleware\AuthMiddleware;
use PDO;

/**
 * Membership Controller (`check_premium.php`, `add_premium.php`, `unlock_vip.php`, `index.php`, `series.php`)
 *
 * Strictly TWO membership tiers: FREE and VIP.
 * Includes the real JazzCash & WhatsApp activation instructions from `index.php` and `series.php`.
 */
final class MembershipController
{
    public static function status(PDO $pdo): never
    {
        $auth = AuthMiddleware::requireAuth($pdo);
        $membership = $auth['membership'];

        ResponseHelper::success([
            'membership'             => $membership['membership'], // Strictly 'FREE' or 'VIP'
            'tier'                   => $membership['membership'],
            'isVip'                  => (bool) $membership['isVip'],
            'is_vip'                 => (bool) $membership['isVip'],
            'planName'               => $membership['planName'],
            'vipExpiry'              => $membership['vipExpiry'],
            'expiry_date'            => $membership['vipExpiry'],
            'vipExpiryEpochSeconds'  => $membership['vipExpiryEpochSeconds'],
            'remainingDays'          => $membership['remainingDays'] ?? 0,
            'allowOfflineDownload'   => (bool) $membership['isVip'],
            'paymentInfo'            => self::paymentInfo(),
        ]);
    }

    public static function plans(): never
    {
        ResponseHelper::success([
            [
                'id'             => 'free_plan',
                'planType'       => 'FREE',
                'name'           => 'FREE Membership',
                'durationDays'   => 3650,
                'priceFormatted' => 'Rs. 0 / Forever',
                'features'       => [
                    'Watch all FREE Urdu Dubbed & Urdu Subtitled movies and series',
                    'Full catalog search, Mind Games Quiz, and Dubbing Studio Auditions',
                    'Standard user profile and watch history',
                ],
                'paymentInfo'    => self::paymentInfo(),
            ],
            [
                'id'             => 'vip_1_month',
                'planType'       => 'VIP',
                'name'           => 'VIP — 1 Month Plan',
                'durationDays'   => 30,
                'priceFormatted' => 'Rs. 149 / 1 Month (or Rs. 250 Full Access)',
                'features'       => [
                    'Unlock all VIP-only Urdu Dubbed & Subtitled Movies and Series',
                    'Ad-free HD streaming across Main Server & Server 2',
                    'Send JazzCash screenshot on WhatsApp (+92 316 0450074) for instant activation',
                ],
                'paymentInfo'    => self::paymentInfo(),
            ],
            [
                'id'             => 'vip_6_months',
                'planType'       => 'VIP',
                'name'           => 'VIP — 6 Months Plan (Popular)',
                'durationDays'   => 180,
                'priceFormatted' => 'Rs. 599 / 6 Months',
                'features'       => [
                    'Save Rs. 300 with 6 Months continuous VIP access',
                    'All Premium Movies, Series, and Hanix Originals unlocked',
                    'Priority WhatsApp support & VIP offline download access',
                ],
                'paymentInfo'    => self::paymentInfo(),
            ],
            [
                'id'             => 'vip_1_year',
                'planType'       => 'VIP',
                'name'           => 'VIP — 1 Year Plan',
                'durationDays'   => 365,
                'priceFormatted' => 'Rs. 999 / 1 Year',
                'features'       => [
                    'Best Value — Save Rs. 800 for a full year of VIP access',
                    'Full HD streaming + all upcoming Premium releases included',
                    'Direct WhatsApp VIP support (+92 316 0450074)',
                ],
                'paymentInfo'    => self::paymentInfo(),
            ],
        ]);
    }

    public static function paymentInfo(): array
    {
        return [
            'method'              => 'JazzCash',
            'accountName'         => 'Muhammad Hasnain',
            'accountNumber'       => '03294806457',
            'whatsappNumber'      => '+923160450074',
            'whatsappSupportUrl'  => 'https://wa.me/923160450074',
            'whatsappChannelUrl'  => 'https://whatsapp.com/channel/0029VbEGTSzFcowGUw5qFb3q',
            'instructions'        => 'Send payment via JazzCash to Muhammad Hasnain (03294806457) and share the screenshot with your registered email on WhatsApp (+92 316 0450074) for instant VIP activation.',
        ];
    }
}
